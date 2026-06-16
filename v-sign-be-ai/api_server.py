"""
V-Sign AI REST API server.

Run:
    python -m uvicorn api_server:app --host 0.0.0.0 --port 8000
"""

from __future__ import annotations

import base64
import asyncio
import json
import logging
import os
import time
from contextlib import asynccontextmanager
from pathlib import Path
from typing import Any, List

import cv2
import mediapipe as mp
import numpy as np
import torch
from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from feature_engineering import normalize_landmarks
from model_v2 import DEFAULT_TARGET_FRAMES, RAW_FEATURE_SIZE, SignLanguageBranchBiLSTMAttention, prepare_sequence


logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger(__name__)

MODEL_DIR = Path("models")
MODEL_PATH = MODEL_DIR / "sign_language_cnn_lstm.pth"
CLASSES_PATH = MODEL_DIR / "classes.npy"
CONFIG_PATH = MODEL_DIR / "model_config.json"
API_VERSION = os.getenv("VSIGN_AI_API_VERSION", "2.0.0")
MODEL_VERSION = os.getenv("VSIGN_AI_MODEL_VERSION", "branch-bilstm-attention-v2")
LABEL_VERSION = os.getenv("VSIGN_AI_LABEL_VERSION", "mvp-3-units-20260609")
MAX_LANDMARK_FRAMES = int(os.getenv("VSIGN_AI_MAX_LANDMARK_FRAMES", "120"))
MAX_JSON_BODY_BYTES = int(os.getenv("VSIGN_AI_MAX_JSON_BODY_BYTES", str(512 * 1024)))
PREDICT_TIMEOUT_SECONDS = float(os.getenv("VSIGN_AI_PREDICT_TIMEOUT_SECONDS", "10"))
MAX_CONCURRENT_PREDICTIONS = max(1, int(os.getenv("VSIGN_AI_MAX_CONCURRENT_PREDICTIONS", "1")))
ENABLE_LEGACY_FRAME_PREDICT = os.getenv("VSIGN_AI_ENABLE_LEGACY_FRAME_PREDICT", "true").lower() == "true"
FORBIDDEN_RAW_FIELDS = {"frames", "frame", "image", "images", "video", "videos", "base64", "jpeg", "jpg", "png"}
prediction_semaphore = asyncio.Semaphore(MAX_CONCURRENT_PREDICTIONS)


class AppState:
    model: SignLanguageBranchBiLSTMAttention | None = None
    classes: np.ndarray | None = None
    device: torch.device | None = None
    holistic = None
    mp_holistic = None
    model_config: dict | None = None


state = AppState()


def load_model_config() -> dict:
    config = {
        "architecture": "branch_bilstm_attention_v2",
        "target_frames": DEFAULT_TARGET_FRAMES,
        "include_velocity": True,
    }
    if CONFIG_PATH.exists():
        with CONFIG_PATH.open("r", encoding="utf-8") as file:
            config.update(json.load(file))
    return config


def empty_prev_landmarks() -> dict[str, list[float]]:
    return {"pose": [0.0] * 132, "left": [0.0] * 63, "right": [0.0] * 63}


def extract_landmarks_from_frame(results, prev_landmarks: dict[str, list[float]]) -> tuple[list[float], bool]:
    landmarks: list[float] = []

    if results.pose_landmarks:
        pose = [
            value
            for landmark in results.pose_landmarks.landmark
            for value in (landmark.x, landmark.y, landmark.z, landmark.visibility)
        ]
        prev_landmarks["pose"] = pose
    else:
        pose = prev_landmarks["pose"]
    landmarks.extend(pose)

    if results.left_hand_landmarks:
        left = [
            value
            for landmark in results.left_hand_landmarks.landmark
            for value in (landmark.x, landmark.y, landmark.z)
        ]
        prev_landmarks["left"] = left
    else:
        left = prev_landmarks["left"]
    landmarks.extend(left)

    if results.right_hand_landmarks:
        right = [
            value
            for landmark in results.right_hand_landmarks.landmark
            for value in (landmark.x, landmark.y, landmark.z)
        ]
        prev_landmarks["right"] = right
    else:
        right = prev_landmarks["right"]
    landmarks.extend(right)

    hands_detected = bool(results.left_hand_landmarks or results.right_hand_landmarks)
    return landmarks, hands_detected


def apply_ema_smoothing(current: list[float], previous_smoothed: list[float] | None, alpha: float = 0.7):
    if previous_smoothed is None:
        return current
    return [alpha * c + (1.0 - alpha) * p for c, p in zip(current, previous_smoothed)]


def process_frames(frames_bgr: List[np.ndarray]) -> dict:
    prev_landmarks = empty_prev_landmarks()
    prev_smoothed = None
    sequence: list[list[float]] = []
    hands_frame_count = 0

    for frame in frames_bgr:
        image_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        image_rgb.flags.writeable = False
        results = state.holistic.process(image_rgb)
        image_rgb.flags.writeable = True

        raw_features, hands_detected = extract_landmarks_from_frame(results, prev_landmarks)
        smoothed = apply_ema_smoothing(raw_features, prev_smoothed, alpha=0.7)
        prev_smoothed = smoothed
        normalized = normalize_landmarks(smoothed).tolist()
        sequence.append(normalized)

        if hands_detected:
            hands_frame_count += 1

    if len(sequence) < 5:
        return {"status": "error", "message": "Too few frames (minimum 5 required)"}
    if hands_frame_count == 0:
        return {"status": "no_hands", "message": "No hands detected in any frame"}

    config = state.model_config or load_model_config()
    prepared = prepare_sequence(
        np.asarray(sequence, dtype=np.float32),
        target_frames=int(config.get("target_frames", DEFAULT_TARGET_FRAMES)),
        include_velocity=bool(config.get("include_velocity", True)),
    )
    input_tensor = torch.tensor(prepared, dtype=torch.float32).unsqueeze(0).to(state.device)

    with torch.no_grad():
        output = state.model(input_tensor)
        probabilities = torch.softmax(output, dim=1).squeeze(0)
        confidence, predicted_idx = torch.max(probabilities, 0)

    label = str(state.classes[predicted_idx.item()])
    top3_values, top3_indices = torch.topk(probabilities, min(3, len(state.classes)))
    top3 = [
        {"label": str(state.classes[i.item()]), "confidence": round(v.item(), 4)}
        for i, v in zip(top3_indices, top3_values)
    ]

    return {
        "status": "ok",
        "label": label,
        "confidence": round(float(confidence.item()), 4),
        "top3": top3,
        "frames_processed": len(sequence),
        "hands_detected_frames": hands_frame_count,
        "model_version": MODEL_VERSION,
        "label_version": LABEL_VERSION,
    }


def classify_landmark_sequence(sequence: list[list[float]], hands_detected_frames: int | None = None) -> dict:
    if len(sequence) < 5:
        return {"status": "error", "message": "Too few frames (minimum 5 required)"}
    if hands_detected_frames is not None and hands_detected_frames <= 0:
        return {"status": "no_hands", "message": "No hands detected in any frame"}

    config = state.model_config or load_model_config()
    prepared = prepare_sequence(
        np.asarray(sequence, dtype=np.float32),
        target_frames=int(config.get("target_frames", DEFAULT_TARGET_FRAMES)),
        include_velocity=bool(config.get("include_velocity", True)),
    )
    input_tensor = torch.tensor(prepared, dtype=torch.float32).unsqueeze(0).to(state.device)

    with torch.no_grad():
        output = state.model(input_tensor)
        probabilities = torch.softmax(output, dim=1).squeeze(0)
        confidence, predicted_idx = torch.max(probabilities, 0)

    label = str(state.classes[predicted_idx.item()])
    top3_values, top3_indices = torch.topk(probabilities, min(3, len(state.classes)))
    top3 = [
        {"label": str(state.classes[i.item()]), "confidence": round(v.item(), 4)}
        for i, v in zip(top3_indices, top3_values)
    ]

    return {
        "status": "ok",
        "label": label,
        "confidence": round(float(confidence.item()), 4),
        "top3": top3,
        "frames_processed": len(sequence),
        "hands_detected_frames": hands_detected_frames,
        "model_version": MODEL_VERSION,
        "label_version": LABEL_VERSION,
    }


async def classify_landmark_sequence_limited(sequence: list[list[float]], hands_detected_frames: int | None = None) -> dict:
    try:
        async with prediction_semaphore:
            return await asyncio.wait_for(
                asyncio.to_thread(classify_landmark_sequence, sequence, hands_detected_frames),
                timeout=PREDICT_TIMEOUT_SECONDS,
            )
    except asyncio.TimeoutError as exc:
        raise HTTPException(status_code=504, detail="AI prediction timed out") from exc


def reject_raw_payload_fields(payload: dict[str, Any]) -> None:
    lowered_keys = {str(key).lower() for key in payload.keys()}
    forbidden = sorted(lowered_keys.intersection(FORBIDDEN_RAW_FIELDS))
    if forbidden:
        raise HTTPException(status_code=400, detail=f"Raw frame/video fields are not accepted: {', '.join(forbidden)}")


async def read_json_object(request: Request) -> dict[str, Any]:
    try:
        payload = await request.json()
    except ValueError as exc:
        raise HTTPException(status_code=400, detail="Request body must be valid JSON") from exc

    if not isinstance(payload, dict):
        raise HTTPException(status_code=400, detail="Request body must be a JSON object")
    return payload


def validate_landmark_sequence(payload: dict[str, Any]) -> tuple[list[list[float]], int | None]:
    reject_raw_payload_fields(payload)
    sequence = payload.get("sequence")
    if not isinstance(sequence, list):
        raise HTTPException(status_code=400, detail="sequence must be an array of frames")
    if len(sequence) < 5:
        raise HTTPException(status_code=400, detail="Need at least 5 landmark frames")
    if len(sequence) > MAX_LANDMARK_FRAMES:
        raise HTTPException(status_code=400, detail=f"Too many landmark frames (max {MAX_LANDMARK_FRAMES})")

    validated: list[list[float]] = []
    for frame_index, frame in enumerate(sequence):
        if not isinstance(frame, list):
            raise HTTPException(status_code=400, detail=f"Frame {frame_index} must be an array")
        if len(frame) != RAW_FEATURE_SIZE:
            raise HTTPException(status_code=400, detail=f"Frame {frame_index} must have exactly {RAW_FEATURE_SIZE} values")
        values: list[float] = []
        for value_index, value in enumerate(frame):
            if not isinstance(value, (int, float)) or not np.isfinite(value):
                raise HTTPException(status_code=400, detail=f"Invalid numeric value at frame {frame_index}, index {value_index}")
            if abs(float(value)) > 10:
                raise HTTPException(status_code=400, detail=f"Out-of-bounds landmark value at frame {frame_index}, index {value_index}")
            values.append(float(value))
        validated.append(values)

    hands_detected_frames = payload.get("hands_detected_frames")
    if hands_detected_frames is None:
        return validated, None
    if not isinstance(hands_detected_frames, int) or hands_detected_frames < 0 or hands_detected_frames > len(validated):
        raise HTTPException(status_code=400, detail="hands_detected_frames must be between 0 and sequence length")
    return validated, hands_detected_frames


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Loading AI model and MediaPipe...")

    state.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    state.classes = np.load(CLASSES_PATH, allow_pickle=True)
    state.model_config = load_model_config()
    state.model = SignLanguageBranchBiLSTMAttention(
        num_classes=len(state.classes),
        include_velocity=bool(state.model_config.get("include_velocity", True)),
    ).to(state.device)
    state.model.load_state_dict(torch.load(MODEL_PATH, map_location=state.device))
    state.model.eval()

    state.mp_holistic = mp.solutions.holistic
    state.holistic = state.mp_holistic.Holistic(
        static_image_mode=False,
        model_complexity=1,
        min_detection_confidence=0.5,
        min_tracking_confidence=0.5,
    )

    logger.info(
        "Model loaded | Device: %s | Architecture: %s | Classes: %s",
        state.device,
        state.model_config.get("architecture"),
        state.classes,
    )
    yield

    state.holistic.close()
    logger.info("Server shutdown - resources released.")


app = FastAPI(
    title="V-Sign AI API",
    description="Sign language recognition API",
    version=API_VERSION,
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


class FramesRequest(BaseModel):
    frames: List[str]


class PredictResponse(BaseModel):
    status: str
    label: str | None = None
    confidence: float | None = None
    top3: list | None = None
    frames_processed: int | None = None
    hands_detected_frames: int | None = None
    message: str | None = None
    inference_ms: float | None = None
    model_version: str | None = None
    label_version: str | None = None


@app.get("/health")
def health_check():
    return {
        "status": "healthy",
        "service": "v-sign-ai",
        "api_version": API_VERSION,
        "model_version": MODEL_VERSION,
        "label_version": LABEL_VERSION,
        "model_loaded": state.model is not None,
        "device": str(state.device),
        "architecture": state.model_config.get("architecture") if state.model_config else None,
        "target_frames": state.model_config.get("target_frames") if state.model_config else None,
        "num_classes": len(state.classes) if state.classes is not None else 0,
        "raw_feature_size": RAW_FEATURE_SIZE,
        "max_landmark_frames": MAX_LANDMARK_FRAMES,
        "max_json_body_bytes": MAX_JSON_BODY_BYTES,
        "predict_timeout_seconds": PREDICT_TIMEOUT_SECONDS,
        "max_concurrent_predictions": MAX_CONCURRENT_PREDICTIONS,
    }


@app.get("/version")
def version():
    return {
        "service": "v-sign-ai",
        "api_version": API_VERSION,
        "model_version": MODEL_VERSION,
        "label_version": LABEL_VERSION,
        "legacy_frame_predict_enabled": ENABLE_LEGACY_FRAME_PREDICT,
        "predict_timeout_seconds": PREDICT_TIMEOUT_SECONDS,
        "max_concurrent_predictions": MAX_CONCURRENT_PREDICTIONS,
    }


@app.get("/classes")
def get_classes():
    if state.classes is None:
        raise HTTPException(status_code=503, detail="Model not loaded")
    return {"classes": state.classes.tolist(), "count": len(state.classes)}


@app.middleware("http")
async def enforce_json_body_limit(request: Request, call_next):
    if request.method == "POST":
        content_length = request.headers.get("content-length")
        if content_length and int(content_length) > MAX_JSON_BODY_BYTES:
            raise HTTPException(status_code=413, detail="Request body too large")
    return await call_next(request)


@app.post("/predict-landmarks", response_model=PredictResponse)
async def predict_landmarks(request: Request):
    if state.model is None:
        raise HTTPException(status_code=503, detail="Model not loaded yet")

    payload = await read_json_object(request)
    sequence, hands_detected_frames = validate_landmark_sequence(payload)
    started = time.perf_counter()
    result = await classify_landmark_sequence_limited(sequence, hands_detected_frames)
    result["inference_ms"] = round((time.perf_counter() - started) * 1000, 1)

    logger.info(
        "Predict landmarks: %s (%s) | %s frames | %sms | model=%s labels=%s",
        result.get("label", "N/A"),
        result.get("confidence", 0),
        len(sequence),
        result["inference_ms"],
        result.get("model_version"),
        result.get("label_version"),
    )
    return result


@app.post("/predict", response_model=PredictResponse)
def predict(request: FramesRequest):
    if not ENABLE_LEGACY_FRAME_PREDICT:
        raise HTTPException(status_code=404, detail="Legacy frame prediction endpoint is disabled")
    if state.model is None:
        raise HTTPException(status_code=503, detail="Model not loaded yet")
    if len(request.frames) < 5:
        raise HTTPException(status_code=400, detail="Need at least 5 frames")
    if len(request.frames) > 500:
        raise HTTPException(status_code=400, detail="Too many frames (max 500)")

    frames_bgr = []
    for index, frame_b64 in enumerate(request.frames):
        try:
            image_bytes = base64.b64decode(frame_b64)
            image_array = np.frombuffer(image_bytes, dtype=np.uint8)
            image = cv2.imdecode(image_array, cv2.IMREAD_COLOR)
            if image is None:
                raise ValueError("Could not decode image")
            frames_bgr.append(image)
        except Exception as exc:
            raise HTTPException(status_code=400, detail=f"Invalid frame at index {index}: {exc}")

    started = time.perf_counter()
    result = process_frames(frames_bgr)
    result["inference_ms"] = round((time.perf_counter() - started) * 1000, 1)

    logger.info(
        "Predict: %s (%s) | %s frames | %sms",
        result.get("label", "N/A"),
        result.get("confidence", 0),
        len(request.frames),
        result["inference_ms"],
    )
    return result

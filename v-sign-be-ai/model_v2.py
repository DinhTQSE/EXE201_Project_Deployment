"""
Shared V-Sign model and sequence preprocessing.
"""

from __future__ import annotations

import numpy as np
import torch
import torch.nn as nn


RAW_FEATURE_SIZE = 258
POSE_SIZE = 132
LEFT_HAND_SIZE = 63
RIGHT_HAND_SIZE = 63
DEFAULT_TARGET_FRAMES = 60


def resample_sequence(sequence: np.ndarray, target_frames: int = DEFAULT_TARGET_FRAMES) -> np.ndarray:
    sequence = np.asarray(sequence, dtype=np.float32)
    if sequence.ndim != 2:
        raise ValueError(f"Expected 2D sequence, got shape {sequence.shape}")
    if sequence.shape[0] == 0:
        raise ValueError("Cannot resample an empty sequence.")
    if sequence.shape[0] == target_frames:
        return sequence.astype(np.float32)
    if sequence.shape[0] == 1:
        return np.repeat(sequence, target_frames, axis=0).astype(np.float32)

    source_positions = np.linspace(0.0, 1.0, sequence.shape[0], dtype=np.float32)
    target_positions = np.linspace(0.0, 1.0, target_frames, dtype=np.float32)
    resampled = np.empty((target_frames, sequence.shape[1]), dtype=np.float32)
    for feature_index in range(sequence.shape[1]):
        resampled[:, feature_index] = np.interp(
            target_positions,
            source_positions,
            sequence[:, feature_index],
        )
    return resampled


def append_velocity(sequence: np.ndarray) -> np.ndarray:
    sequence = np.asarray(sequence, dtype=np.float32)
    velocity = np.zeros_like(sequence, dtype=np.float32)
    if sequence.shape[0] > 1:
        velocity[1:] = sequence[1:] - sequence[:-1]
    return np.concatenate([sequence, velocity], axis=1).astype(np.float32)


def prepare_sequence(
    sequence: np.ndarray,
    target_frames: int = DEFAULT_TARGET_FRAMES,
    include_velocity: bool = True,
) -> np.ndarray:
    sequence = np.asarray(sequence, dtype=np.float32)
    if sequence.shape[1] != RAW_FEATURE_SIZE:
        raise ValueError(f"Expected {RAW_FEATURE_SIZE} features, got {sequence.shape[1]}")
    sequence = resample_sequence(sequence, target_frames=target_frames)
    if include_velocity:
        sequence = append_velocity(sequence)
    return sequence.astype(np.float32)


class ConvBranch(nn.Module):
    def __init__(self, input_size: int, hidden_size: int = 64, dropout: float = 0.2):
        super().__init__()
        self.net = nn.Sequential(
            nn.Conv1d(input_size, hidden_size, kernel_size=3, padding=1),
            nn.BatchNorm1d(hidden_size),
            nn.ReLU(),
            nn.Dropout(dropout),
            nn.Conv1d(hidden_size, hidden_size, kernel_size=3, padding=1),
            nn.BatchNorm1d(hidden_size),
            nn.ReLU(),
        )

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        return self.net(x.transpose(1, 2)).transpose(1, 2)


class TemporalAttention(nn.Module):
    def __init__(self, input_size: int, attention_size: int = 128):
        super().__init__()
        self.score = nn.Sequential(
            nn.Linear(input_size, attention_size),
            nn.Tanh(),
            nn.Linear(attention_size, 1),
        )

    def forward(self, sequence: torch.Tensor) -> tuple[torch.Tensor, torch.Tensor]:
        weights = torch.softmax(self.score(sequence), dim=1)
        context = torch.sum(sequence * weights, dim=1)
        return context, weights.squeeze(-1)


class SignLanguageBranchBiLSTMAttention(nn.Module):
    def __init__(
        self,
        num_classes: int,
        branch_hidden: int = 64,
        lstm_hidden: int = 128,
        dropout: float = 0.4,
        include_velocity: bool = True,
    ):
        super().__init__()
        multiplier = 2 if include_velocity else 1
        self.include_velocity = include_velocity
        self.input_size = RAW_FEATURE_SIZE * multiplier
        self.pose_branch = ConvBranch(POSE_SIZE * multiplier, branch_hidden)
        self.left_branch = ConvBranch(LEFT_HAND_SIZE * multiplier, branch_hidden)
        self.right_branch = ConvBranch(RIGHT_HAND_SIZE * multiplier, branch_hidden)
        merged_size = branch_hidden * 3
        self.lstm = nn.LSTM(
            input_size=merged_size,
            hidden_size=lstm_hidden,
            num_layers=1,
            batch_first=True,
            bidirectional=True,
        )
        self.dropout = nn.Dropout(dropout)
        self.attention = TemporalAttention(lstm_hidden * 2)
        self.classifier = nn.Sequential(
            nn.Linear(lstm_hidden * 2, 128),
            nn.ReLU(),
            nn.Dropout(dropout),
            nn.Linear(128, num_classes),
        )

    def split_features(self, x: torch.Tensor) -> tuple[torch.Tensor, torch.Tensor, torch.Tensor]:
        if self.include_velocity:
            position = x[:, :, :RAW_FEATURE_SIZE]
            velocity = x[:, :, RAW_FEATURE_SIZE:]
            pose = torch.cat([position[:, :, :POSE_SIZE], velocity[:, :, :POSE_SIZE]], dim=2)
            left_start = POSE_SIZE
            left_end = POSE_SIZE + LEFT_HAND_SIZE
            left = torch.cat([position[:, :, left_start:left_end], velocity[:, :, left_start:left_end]], dim=2)
            right = torch.cat([position[:, :, left_end:], velocity[:, :, left_end:]], dim=2)
        else:
            pose = x[:, :, :POSE_SIZE]
            left_start = POSE_SIZE
            left_end = POSE_SIZE + LEFT_HAND_SIZE
            left = x[:, :, left_start:left_end]
            right = x[:, :, left_end:]
        return pose, left, right

    def forward(self, x: torch.Tensor, lengths: torch.Tensor | None = None) -> torch.Tensor:
        pose, left, right = self.split_features(x)
        merged = torch.cat(
            [self.pose_branch(pose), self.left_branch(left), self.right_branch(right)],
            dim=2,
        )
        sequence, _ = self.lstm(merged)
        sequence = self.dropout(sequence)
        context, _ = self.attention(sequence)
        return self.classifier(context)

# V-Sign AI Model Improvement Plan

Muc tieu: tang do chinh xac thuc te cua model nhieu hon, giam false positive, va lam pipeline train/inference nhat quan.

## Phase 1 - Chuan hoa data dau vao

### 1. Cat raw video thanh tung sequence rieng

Hien tai moi file trong `raw_videos/` co the chua nhieu lan lap dong tac. Can cat thanh tung clip, moi clip = 1 lan thuc hien gesture hoan chinh.

Output de xuat:

```text
segmented_videos/
  ca_phe/
    ca_phe_01_seq001.mp4
    ca_phe_01_seq002.mp4
  da/
    da_01_seq001.mp4
```

Ly do:

- Moi `video_id` trong CSV se tuong ung dung 1 gesture.
- Train/validation tach sequence ro rang hon.
- De loai bo doan thua: chuan bi, nghi tay, sai dong tac.

### 2. Viet lai extraction pipeline cho training

Training extraction phai giong inference:

1. Doc frame goc, khong flip truoc khi MediaPipe detect.
2. Extract `258` landmarks:
   - pose: `33 * 4`
   - left hand: `21 * 3`
   - right hand: `21 * 3`
3. Neu mat landmark tam thoi thi dung landmark frame truoc.
4. EMA smoothing voi `alpha=0.7`.
5. Normalize landmarks bang Nose lam goc.
6. Ghi CSV: `video_id + 258 features + label`.

Tieu chi xong:

- Script extract xu ly `segmented_videos/<label>/*.mp4`.
- Data train va inference cung chung helper function.
- Khong con khac biet zero-fill vs interpolation.

### 3. Them class `idle` / `none`

Them cac clip khong phai gesture:

- dung yen
- tay ngoai frame
- chuyen dong tay khong co nghia
- chuan bi truoc/sau gesture

Ly do: model hien tai bi ep chon 1 trong 6 label, nen de false positive.

## Phase 2 - Tang robust cho data

### 4. Landmark augmentation

Augment tren CSV/sequence thay vi video:

- gaussian noise nho vao toa do
- scale nhe quanh goc Nose
- temporal crop
- resample speed nhanh/cham
- drop random frame
- mask tam thoi mot ban tay trong vai frame

Tieu chi xong:

- Co script tao `data_augmented/`.
- Moi sequence goc tao duoc 3-5 bien the.
- Validation tren test set that khong giam.

### 5. Split theo session/person

Khong chi random split theo sequence. Can co manifest:

```text
video_id,label,source_video,subject,session,split
```

De xuat:

- train: subject/session chinh
- val: cung vocab nhung sequence khac
- test: session hoac nguoi khac

Tieu chi xong:

- Bao cao confusion matrix tren test set.
- Khong danh gia chi bang val random.

## Phase 3 - Nang cap feature/model

### 6. Resampling sequence ve do dai co dinh

Chuan hoa moi gesture thanh `60` hoac `90` frames.

Ly do:

- Train on dinh hon.
- Batch don gian hon.
- Model hoc nhieu ve hinh dang chuyen dong thay vi do dai video.

De xuat bat dau voi `60` frames.

### 7. Them velocity features

Voi moi frame:

```text
delta_t = landmarks_t - landmarks_t-1
```

Input moi frame co the la:

```text
258 position + 258 velocity = 516 features
```

Ly do: ngon ngu ky hieu phu thuoc rat nhieu vao huong va toc do chuyen dong.

### 8. Tach branch pose/left/right

Thay vi dua thang `258` features vao mot Conv1D, tach:

- pose branch: `132`
- left hand branch: `63`
- right hand branch: `63`

Moi branch co Conv1D rieng, sau do concat lai.

Ly do:

- Pose va hand co y nghia khac nhau.
- Hand movement nen duoc model uu tien hon pose noise.

### 9. BiLSTM + Attention

Vi prediction duoc thuc hien sau khi gesture ket thuc, model co the nhin ca qua khu va tuong lai trong sequence.

Kien truc de xuat:

```text
Input sequence
  -> pose/left/right Conv1D branches
  -> concat
  -> BiLSTM
  -> temporal attention
  -> classifier
```

Attention giup model tap trung vao frame quan trong nhat cua gesture, thay vi chi dung hidden state cuoi.

## Phase 4 - Danh gia va deploy

### 10. Metrics bat buoc

Can co script evaluation sinh:

- accuracy
- per-class precision/recall/F1
- confusion matrix
- top-3 accuracy
- latency inference trung binh

### 11. Dieu kien replace model cu

Chi replace `models/sign_language_cnn_lstm.pth` khi:

- test accuracy tang hoac false positive giam ro
- inference API van load model thanh cong
- `/classes` dung label
- test webcam/API voi 6 label hien tai khong regression nang

## Trang thai trien khai hien tai

Da them/sua cac file:

```text
V-Sign-AI-Build/ai-engine/manual_video_cutter.py
V-Sign-AI-Build/ai-engine/extract_features.py
V-Sign-AI-Build/ai-engine/create_split_manifest.py
V-Sign-AI-Build/ai-engine/augment_landmarks.py
V-Sign-AI-Build/ai-engine/model_v2.py
V-Sign-AI-Build/ai-engine/train_lstm.py
V-Sign-AI-Build/ai-engine/evaluate_model.py
V-Sign-AI-Build/ai-engine/inference.py
V-Sign-AI-Build/v-sign-be-ai/model_v2.py
V-Sign-AI-Build/v-sign-be-ai/api_server.py
```

Nhung muc da duoc code hoa:

- Cat raw video thanh clip tung gesture.
- Extract pipeline khop inference: interpolation, EMA, normalize.
- Ho tro label `idle`/`none` bang cach them thu muc/clip label moi.
- Tao split manifest train/val/test co group theo source.
- Landmark augmentation.
- Resampling sequence ve fixed length.
- Velocity features.
- Pose/left/right branches.
- BiLSTM + Attention.
- Evaluation report: accuracy, top-3, classification report, confusion matrix, latency.
- Inference webcam va API da load model v2.

## Thu tu chay pipeline moi

1. Cat raw videos thanh segmented clips.
2. Viet lai extraction tu segmented clips, dung chung pipeline voi inference.
3. Train lai model hien tai tren data sach.
4. Them `idle/none`.
5. Them resampling + velocity.
6. Nang model len branch Conv1D + BiLSTM + Attention.

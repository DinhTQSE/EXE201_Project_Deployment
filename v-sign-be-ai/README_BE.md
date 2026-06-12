# V-Sign AI Service — Hướng dẫn cho Backend Developer

## Cấu trúc thư mục nhận được

```
v-sign-be-ai/
├── api_server.py          ← File server chính
├── feature_engineering.py ← Dependency (không sửa)
├── model_v2.py            ← Kiến trúc model + sequence preprocessing
├── requirements.txt       ← Danh sách thư viện
└── models/
    ├── sign_language_cnn_lstm.pth   ← Model AI đã train
    ├── classes.npy                  ← Danh sách từ/labels
    └── model_config.json            ← Cấu hình target_frames/velocity/architecture
```

---

## Yêu cầu môi trường

- **Python 3.11** (khuyến nghị) — tải tại https://www.python.org/downloads/release/python-3119/
- **Không cần GPU** — chạy được trên CPU thường

---

## Cài đặt & Chạy server (lần đầu)

```bash
# Bước 1: Vào thư mục
cd v-sign-be-ai

# Bước 2: Tạo môi trường ảo
py -3.11 -m venv venv

# Bước 3: Kích hoạt venv
# Windows:
.\venv\Scripts\Activate.ps1
# macOS/Linux:
source venv/bin/activate

# Bước 4: Cài thư viện (chỉ cần làm 1 lần, mất ~5-10 phút)
pip install -r requirements.txt

# Bước 5: Chạy server
python -m uvicorn api_server:app --host 0.0.0.0 --port 8000
```

## Chạy lại server (từ lần 2 trở đi)

```bash
.\venv\Scripts\Activate.ps1
python -m uvicorn api_server:app --host 0.0.0.0 --port 8000
```

Server sẵn sàng khi thấy:
```
INFO: Application startup complete.
INFO: Uvicorn running on http://0.0.0.0:8000
```

---

## API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/health` | Kiểm tra server còn sống |
| GET | `/classes` | Danh sách từ model hỗ trợ |
| POST | `/predict` | **Nhận diện cử chỉ từ frames** |

### Swagger UI (test API trực tiếp trên browser)
```
http://localhost:8000/docs
```

---

## Cách gọi `/predict`

### Request
```json
POST /predict
Content-Type: application/json

{
  "frames": [
    "base64_encoded_image_1",
    "base64_encoded_image_2",
    "..."
  ]
}
```

- Mỗi frame là ảnh JPEG/PNG encode thành **base64 string**
- Gửi **tối thiểu 15 frames**, lý tưởng 30–60 frames của **1 cử chỉ hoàn chỉnh**
- Frames phải theo **đúng thứ tự thời gian**
- Nên resize ảnh về **640×480** trước khi encode để giảm kích thước request

### Response
```json
{
  "status": "ok",
  "label": "ca_phe",
  "confidence": 0.9823,
  "top3": [
    { "label": "ca_phe", "confidence": 0.9823 },
    { "label": "den",    "confidence": 0.0102 },
    { "label": "sua",    "confidence": 0.0075 }
  ],
  "frames_processed": 45,
  "hands_detected_frames": 38,
  "inference_ms": 312.5
}
```

| Field | Ý nghĩa |
|-------|---------|
| `status` | `"ok"` / `"no_hands"` / `"error"` |
| `label` | Từ được nhận diện |
| `confidence` | Độ tin cậy (0.0 → 1.0) |
| `top3` | Top 3 dự đoán, dùng để debug |
| `inference_ms` | Thời gian xử lý (milliseconds) |

---

## Ví dụ code gọi API

### JavaScript (Axios)
```javascript
import axios from 'axios';

const AI_SERVER = 'http://localhost:8000';

/**
 * Nhận diện cử chỉ từ mảng HTMLImageElement hoặc canvas frames
 * @param {HTMLCanvasElement[]} canvasFrames - Mảng canvas frames từ camera
 * @returns {Promise<Object>} Kết quả nhận diện
 */
async function recognizeGesture(canvasFrames) {
  // Encode frames thành base64
  const frames = canvasFrames.map(canvas =>
    canvas.toDataURL('image/jpeg', 0.8)
          .replace('data:image/jpeg;base64,', '')
  );

  const response = await axios.post(`${AI_SERVER}/predict`, { frames });
  return response.data;
}

// Sử dụng
const result = await recognizeGesture(myFrames);
if (result.status === 'ok' && result.confidence > 0.7) {
  console.log(`Nhận diện: ${result.label} (${(result.confidence * 100).toFixed(0)}%)`);
}
```

### Java (OkHttp + Gson) — ví dụ Spring Boot
```java
// Thêm dependency: com.squareup.okhttp3:okhttp, com.google.code.gson:gson

import okhttp3.*;
import com.google.gson.*;
import java.util.List;
import java.util.Base64;

public class VSignAIClient {
    private static final String AI_SERVER = "http://localhost:8000";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    public String predict(List<byte[]> frameBytes) throws Exception {
        // Encode frames thành base64
        List<String> frames = frameBytes.stream()
            .map(b -> Base64.getEncoder().encodeToString(b))
            .toList();

        String json = gson.toJson(new RequestBody(frames));
        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));

        Request request = new Request.Builder()
            .url(AI_SERVER + "/predict")
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            return response.body().string(); // Parse JSON theo nhu cầu
        }
    }

    record RequestBody(List<String> frames) {}
}
```

---

## Lưu ý quan trọng

> **Mỗi request = 1 cử chỉ hoàn chỉnh.**
> Frontend cần tự quyết định khi nào user bắt đầu và kết thúc cử chỉ,
> rồi gom tất cả frames lại gửi 1 lần.

**Workflow khuyến nghị:**
1. User nhấn nút "Bắt đầu ký hiệu" → Frontend bắt đầu lưu frames
2. User nhấn "Xong" (hoặc tự động detect dừng) → Gửi frames lên `/predict`
3. Hiển thị kết quả cho user

---

## Cập nhật model (khi AI team thêm từ mới)

AI team sẽ giao 2 file mới:
```
models/sign_language_cnn_lstm.pth   ← Thay thế file cũ
models/classes.npy                  ← Thay thế file cũ
models/model_config.json            ← Thay thế file cũ
```

Quy trình:
1. Dừng server (`CTRL+C`)
2. Replace 2 file trong `models/`
3. Chạy lại server
4. Gọi `GET /classes` để xác nhận danh sách từ mới

**Không cần sửa bất kỳ dòng code nào.**

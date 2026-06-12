# V-SIGN — Use Case Descriptions

> **Version:** 1.0 | **Author:** BA Team | **Date:** 2026-05-14
> **Convention:** Basic Flow mô tả đường đi hạnh phúc (happy path). Alternative Flow (AF) và Exception Flow (EF) đánh số theo bước gốc.

---

## UC-01: Học bài với AI (AI Practice Quiz)

| Attribute | Detail |
|---|---|
| **Use Case ID** | UC-01 |
| **Use Case Name** | AI Practice Quiz — Thực hành ký hiệu trước camera |
| **Actor chính** | Premium User |
| **Actor phụ** | MediaPipe (Client-side AI Engine), Spring Boot API |
| **Mức độ** | User Goal |

### Pre-conditions
1. User đã đăng nhập và có `account_type = PREMIUM`.
2. User đã hoàn thành Lesson video tương ứng (`User_Progress.status = COMPLETED`).
3. Chapter chứa bài học có `is_premium = TRUE` — đã được mở khóa.
4. Thiết bị của User có camera và trình duyệt hỗ trợ `getUserMedia API` (Chrome/Edge/Firefox).
5. MediaPipe Hands WASM bundle đã được tải về client (lazy-load khi vào trang Quiz).

### Basic Flow

| Bước | Actor | Hành động |
|---|---|---|
| 1 | User | Chọn bài học → click **"Bắt đầu Luyện tập AI"**. |
| 2 | System (FE) | Kiểm tra `account_type` và `is_premium` của Chapter qua API. Kết quả: hợp lệ → tiếp tục. |
| 3 | System (FE) | Load trang Quiz, hiển thị nút **"Cấp quyền Camera"** và hướng dẫn ánh sáng. |
| 4 | User | Click **"Cấp quyền Camera"** → trình duyệt hiển thị popup xin quyền. User chấp thuận. |
| 5 | System (FE) | Khởi tạo `MediaPipe Hands` hoàn toàn trên client (WASM). Stream video từ `getUserMedia()` được đưa vào MediaPipe **tại trình duyệt — không có frame nào gửi lên server**. |
| 6 | System (FE) | Hiển thị câu hỏi đầu tiên: ảnh/text mô tả ký hiệu cần thực hiện + countdown timer (5 giây). |
| 7 | User | Thực hiện ký hiệu tay trước camera. |
| 8 | MediaPipe (Client) | Phát hiện bàn tay, trích xuất **21 Hand Landmarks** (tọa độ x, y, z) theo thời gian thực ≤ 200ms mỗi frame. |
| 9 | System (FE) | So sánh vector Landmark với model classification đã được tải về client. Xác định `predicted_sign` và `confidence_score`. |
| 10 | System (FE) | Nếu `confidence_score ≥ 0.85` trong ≥ 3 frame liên tiếp → xác nhận kết quả. |
| 11 | System (FE) | Hiển thị phản hồi tức thì: ✅ Đúng / ❌ Sai + animation. Chuyển câu tiếp theo. |
| 12 | System (FE) | Sau khi hết câu, gọi `POST /api/quiz-attempts` gửi lên server: `{ quiz_id, score, correct_answers, total_questions }`. |
| 13 | System (FE) | Gọi `POST /api/ai-log` gửi log tổng hợp: `{ attempt_id, question_id, predicted_sign, confidence_score, is_correct }` — **chỉ metadata, không có ảnh/video**. |
| 14 | System (BE) | Lưu `Quiz_Attempts` và `AI_Attempts_Log`. Nếu `score ≥ passing_score` → cập nhật `User_Progress.status = COMPLETED`. |
| 15 | System (BE) | Trigger XP: ghi vào `XP_Logs` với `source_type = QUIZ_COMPLETE`, kiểm tra Streak, kiểm tra điều kiện Badge. |
| 16 | System (FE) | Hiển thị màn hình kết quả: Điểm số, XP nhận được, gợi ý bài tiếp theo. |

### Alternative Flows

**AF-4a — User từ chối cấp quyền Camera:**
- Bước 4: User click "Chặn" trên popup quyền trình duyệt.
- System hiển thị thông báo: *"Camera là bắt buộc cho bài tập AI. Hãy cấp quyền trong Settings trình duyệt."*
- Cung cấp link hướng dẫn theo từng trình duyệt. Quiz bị treo ở màn hình này.

**AF-9a — Không phát hiện bàn tay (no hand detected):**
- Bước 9: MediaPipe không trả về Landmark (vùng ảnh tối hoặc tay ngoài khung).
- Overlay hiển thị: *"Không phát hiện bàn tay. Hãy đưa tay vào khung hình."*
- Countdown timer tạm dừng. Sau 10 giây không phát hiện → hệ thống tự bỏ qua câu hỏi, tính là sai.

**AF-9b — Ánh sáng không đủ (low confidence):**
- Bước 9: Liên tục nhận `confidence_score < 0.85` sau 5 giây.
- Hiển thị cảnh báo: *"Khó nhận diện — thử tăng ánh sáng hoặc đổi nền."*
- User có thể click **"Bỏ qua câu này"** (tối đa 2 lần/quiz).

**AF-2a — User Basic truy cập Chapter Premium:**
- Bước 2: API trả về `403 Forbidden` do `is_premium = TRUE` và `account_type = BASIC`.
- System điều hướng về màn hình **Paywall**: hiển thị lợi ích Premium + nút "Nâng cấp ngay".

### Post-conditions
- `Quiz_Attempts` và `AI_Attempts_Log` được lưu đầy đủ.
- `User_Progress.status` cập nhật nếu đạt điểm.
- `XP_Logs` ghi nhận XP, `Streaks.last_activity_date` được cập nhật.
- User thấy kết quả và có thể tiếp tục bài tiếp theo.

---

## UC-02: Nâng cấp tài khoản Premium (Thanh toán QR)

| Attribute | Detail |
|---|---|
| **Use Case ID** | UC-02 |
| **Use Case Name** | Upgrade to Premium — Thanh toán QR MoMo/ZaloPay |
| **Actor chính** | Basic User |
| **Actor phụ** | Payment Gateway (MoMo / ZaloPay), Spring Boot Webhook Handler |
| **Mức độ** | User Goal |

### Pre-conditions
1. User đã đăng nhập với `account_type = BASIC`.
2. User chưa có Subscription `ACTIVE` nào.
3. Server Spring Boot có endpoint Webhook đã đăng ký với nhà cung cấp thanh toán.

### Basic Flow

| Bước | Actor | Hành động |
|---|---|---|
| 1 | User | Nhấn "Nâng cấp Premium" (từ Paywall hoặc menu Profile). |
| 2 | System (FE) | Hiển thị màn hình chọn gói: **Monthly 49.000đ / Yearly 399.000đ**. |
| 3 | User | Chọn gói Monthly → chọn phương thức: **MoMo** hoặc **ZaloPay**. |
| 4 | System (FE) | Gọi `POST /api/payments/create-order` với `{ plan_type, provider }`. |
| 5 | System (BE) | Tạo bản ghi `Payment_Transactions` với `status = PENDING`. Gọi API Payment Gateway để lấy QR code / deep link. |
| 6 | System (BE) | Trả về `{ qr_code_url, provider_transaction_id, expires_in: 300s }`. |
| 7 | System (FE) | Hiển thị QR code + countdown 5 phút + logo nhà cung cấp. |
| 8 | User | Mở app MoMo/ZaloPay, quét QR, xác nhận thanh toán. |
| 9 | Payment Gateway | Gửi Webhook `POST /api/payments/webhook` về server với `{ provider_transaction_id, status: "SUCCESS", ... }`. |
| 10 | System (BE) | **Xác thực chữ ký Webhook** (HMAC signature). Tìm `Payment_Transactions` theo `provider_transaction_id`. |
| 11 | System (BE) | Cập nhật `Payment_Transactions.status = SUCCESS`. Tạo `Subscriptions` mới với `status = ACTIVE`, `end_date = NOW() + 30 days`. |
| 12 | System (BE) | Cập nhật `Users.account_type = PREMIUM`. |
| 13 | System (FE) | Polling `GET /api/payments/status/{transaction_id}` mỗi 3 giây. Nhận `SUCCESS` → hiển thị màn hình chúc mừng. |
| 14 | System (FE) | Refresh JWT token (hoặc re-fetch user profile) để cập nhật quyền ngay lập tức. |

### Alternative Flows

**AF-8a — User không quét QR trong 5 phút (timeout):**
- Bước 7: Countdown về 0.
- System đánh dấu `Payment_Transactions.status = FAILED` (timeout).
- Hiển thị: *"Phiên thanh toán đã hết hạn."* + nút **"Tạo lại mã QR"**.
- Bản ghi cũ được giữ lại để audit.

**AF-9a — Webhook không đến (mạng gián đoạn):**
- Bước 9: Server không nhận được Webhook trong 10 phút.
- Polling FE nhận `PENDING` liên tục → sau 10 phút hiển thị: *"Thanh toán đang xử lý. Chúng tôi sẽ thông báo qua email khi hoàn tất."*
- System có **Scheduled Job** kiểm tra lại trạng thái giao dịch mỗi 15 phút.

**AF-10a — Xác thực chữ ký Webhook thất bại:**
- Bước 10: HMAC signature không khớp → Server trả `400 Bad Request`.
- Ghi log cảnh báo bảo mật. **Không** cập nhật bất kỳ dữ liệu nào.
- Alert cho Admin qua email/Slack.

**AF-11a — Thanh toán thất bại (người dùng hủy / số dư không đủ):**
- Bước 9: Webhook trả `status = FAILED`.
- Cập nhật `Payment_Transactions.status = FAILED`.
- FE hiển thị: *"Thanh toán không thành công. Kiểm tra số dư hoặc thử phương thức khác."*
- `Users.account_type` **không thay đổi** — đảm bảo không nâng cấp nhầm.

### Post-conditions
- `Payment_Transactions` có `status = SUCCESS` với đầy đủ `provider_transaction_id`.
- `Subscriptions` mới được tạo với `status = ACTIVE`.
- `Users.account_type = PREMIUM`.
- User có thể truy cập toàn bộ Chapter `is_premium = TRUE` ngay lập tức.

---

## UC-03: Tra cứu từ điển VSL

| Attribute | Detail |
|---|---|
| **Use Case ID** | UC-03 |
| **Use Case Name** | VSL Dictionary Lookup — Tra cứu từ điển ngôn ngữ ký hiệu |
| **Actor chính** | Guest User, Basic User, Premium User |
| **Mức độ** | Supporting Goal |

> **Quyết định nghiệp vụ:** Từ điển là tính năng **công khai** — không yêu cầu đăng nhập và không phân biệt Basic/Premium, nhằm tăng giá trị miễn phí và thu hút người dùng mới.

### Pre-conditions
1. Hệ thống hoạt động bình thường.
2. Bảng `Dictionary` đã có dữ liệu.
3. *(Không yêu cầu đăng nhập)*

### Basic Flow

| Bước | Actor | Hành động |
|---|---|---|
| 1 | User | Vào mục **"Từ điển VSL"** từ navigation. |
| 2 | System (FE) | Gọi `GET /api/dictionary/categories` → hiển thị danh sách danh mục (Chào hỏi, Gia đình, Số đếm, …). |
| 3 | User | Chọn một danh mục *(ví dụ: "Cảm xúc")*. |
| 4 | System (FE) | Gọi `GET /api/dictionary?category=Cảm xúc&page=0&size=20` → hiển thị grid các từ với thumbnail. |
| 5 | User | Click vào từ cụ thể *(ví dụ: "Vui vẻ")*. |
| 6 | System (FE) | Hiển thị trang chi tiết: tên ký hiệu, mô tả, độ khó, và trình phát video minh họa từ S3. |
| 7 | User | Xem video → click **"Luyện tập ngay"** *(tùy chọn)*. |
| 8 | System (FE) | Nếu User chưa đăng nhập → redirect tới trang Login. Nếu đã đăng nhập → navigate tới bài học liên quan (nếu có). |

### Alternative Flows

**AF-2a — Tìm kiếm theo từ khóa:**
- Bước 1: User nhập từ khóa vào thanh Search trên trang Từ điển.
- FE gọi `GET /api/dictionary/search?keyword=xin+chào`.
- Hiển thị kết quả matching theo `sign_name`. Nếu không có kết quả → *"Chưa có từ này trong từ điển. Bạn muốn đề xuất?"*

**AF-6a — Video không tải được:**
- Bước 6: S3 URL trả lỗi hoặc timeout.
- Hiển thị placeholder ảnh + thông báo: *"Video tạm thời không khả dụng."*
- Log lỗi tự động gửi về monitoring.

**AF-4a — Danh mục chưa có nội dung:**
- Bước 4: API trả về mảng rỗng.
- Hiển thị: *"Danh mục này đang được cập nhật. Quay lại sau nhé!"*

### Post-conditions
- User đã xem được video minh họa ký hiệu.
- Nếu đã đăng nhập, có thể chuyển thẳng sang bài học liên quan.
- Không có dữ liệu nào được ghi vào DB (chỉ đọc).

---

## UC-04: Duy trì Streak và nhận XP

| Attribute | Detail |
|---|---|
| **Use Case ID** | UC-04 |
| **Use Case Name** | Daily Streak Maintenance & XP Reward |
| **Actor chính** | Basic User, Premium User |
| **Actor phụ** | System Scheduler (Cron Job) |
| **Mức độ** | Sub-function (trigger bởi UC-01 hoặc hoàn thành Lesson) |

### Định nghĩa nghiệp vụ Streak (Business Rules — BR)

> ⚠️ **Múi giờ chuẩn:** Toàn bộ hệ thống sử dụng **UTC+7 (Giờ Việt Nam)** làm chuẩn cho mọi tính toán Streak. Timestamp gốc lưu UTC, nhưng logic ngày tháng Streak tính theo UTC+7.

| Rule | Mô tả |
|---|---|
| **BR-1** | Một ngày học = một hoạt động hợp lệ trong khoảng `00:00:00 – 23:59:59 UTC+7`. |
| **BR-2** | Streak tăng 1 khi `last_activity_date = TODAY(UTC+7) - 1 day` tại thời điểm hoạt động mới. |
| **BR-3** | Streak **giữ nguyên** (không tăng, không giảm) nếu `last_activity_date = TODAY(UTC+7)` (đã học hôm nay rồi). |
| **BR-4** | Streak **reset về 1** nếu `last_activity_date < TODAY(UTC+7) - 1 day` (bỏ qua ≥ 1 ngày). |
| **BR-5** | Học lúc 23:59 UTC+7 hoàn toàn hợp lệ — ngày được tính theo **ngày bắt đầu hoạt động** tại múi giờ UTC+7. |
| **BR-6** | XP Streak Bonus chỉ được cộng khi Streak **tăng** (BR-2), không cộng khi giữ nguyên (BR-3). |
| **BR-7** | Cron Job chạy lúc **00:05 UTC+7 mỗi ngày** để kiểm tra và reset Streak nếu cần — nhưng tác động thực tế chỉ xảy ra lần tiếp theo User hoạt động (lazy reset). |

### Pre-conditions
1. User đã đăng nhập.
2. User vừa hoàn thành một Lesson hoặc Quiz (`User_Progress.status = COMPLETED` hoặc `Quiz_Attempts.status = COMPLETED`).
3. Bảng `Streaks` đã có bản ghi cho User (tạo lần đầu khi User hoàn thành bài học đầu tiên).

### Basic Flow (Streak Tăng — Happy Path)

| Bước | Actor | Hành động |
|---|---|---|
| 1 | System (BE) | Nhận event hoàn thành bài học/quiz từ UC-01 hoặc Learning flow. |
| 2 | System (BE) | Đọc `Streaks` của User: lấy `last_activity_date` và `current_streak`. |
| 3 | System (BE) | Tính `today_vn = NOW() converted to UTC+7, lấy phần DATE`. |
| 4 | System (BE) | So sánh `last_activity_date` với `today_vn` theo BR-1 đến BR-4. |
| 5 | System (BE) | Kết quả: `last_activity_date = today_vn - 1 day` → **Streak tăng**: `current_streak += 1`. Cập nhật `longest_streak` nếu cần. |
| 6 | System (BE) | Cập nhật `last_activity_date = today_vn`. |
| 7 | System (BE) | Tính XP Streak Bonus: `streak_xp = base_xp × streak_multiplier`. |

**Bảng XP Multiplier theo mốc Streak:**

| Streak (ngày) | Multiplier | Tổng XP bonus |
|---|---|---|
| 1 – 6 | ×1.0 | Không bonus |
| 7 – 13 | ×1.2 | +20% |
| 14 – 29 | ×1.5 | +50% |
| 30+ | ×2.0 | +100% |

| Bước | Actor | Hành động |
|---|---|---|
| 8 | System (BE) | Ghi `XP_Logs`: `{ source_type: LESSON_COMPLETE, xp_earned: lesson_xp }`. |
| 9 | System (BE) | Ghi `XP_Logs`: `{ source_type: STREAK_BONUS, xp_earned: streak_bonus_xp }` (nếu có). |
| 10 | System (BE) | Kiểm tra điều kiện Badge: `STREAK_DAYS` — nếu `current_streak` đạt mốc (7, 14, 30, …) → ghi `User_Badges`. |
| 11 | System (BE) | Cập nhật `Leaderboard_Snapshots` (xếp hạng tuần/tháng hiện tại). |
| 12 | System (FE) | Hiển thị animation: 🔥 Streak ngày X + XP earned + Badge mới (nếu có). |

### Alternative Flows

**AF-4a — User đã học hôm nay rồi (Streak giữ nguyên — BR-3):**
- Bước 4: `last_activity_date = today_vn`.
- Không tăng Streak, không cộng Streak Bonus XP.
- Vẫn ghi XP bài học bình thường vào `XP_Logs`.
- FE hiển thị: *"Bạn đã học hôm nay! Streak hiện tại: 🔥 X ngày."*

**AF-4b — User bỏ học ≥ 1 ngày (Streak Reset — BR-4):**
- Bước 4: `last_activity_date < today_vn - 1 day`.
- `current_streak` reset về `1` (ngày hôm nay được tính là ngày 1 mới).
- `longest_streak` **giữ nguyên** — không bị ảnh hưởng.
- FE hiển thị: *"Streak đã bị reset. Hôm nay là ngày 1 — hãy duy trì chuỗi mới!"*
- *Lưu ý UX:* Cân nhắc tính năng **"Streak Freeze"** (dùng XP để bảo vệ Streak — roadmap v2).

**AF-4c — Học xuyên nửa đêm (23:59 → 00:01):**
- Tình huống: User bắt đầu bài học lúc `23:55 UTC+7`, hoàn thành lúc `00:03 UTC+7 ngày hôm sau`.
- Quyết định: **Lấy thời điểm hoàn thành** (`completed_at`) để tính ngày.
- Ngày được tính là ngày hôm sau (ngày mới) → BR được áp dụng bình thường.
- Không có edge case bất lợi cho User.

**AF-Cron — Hệ thống tự kiểm tra Streak lúc 00:05 UTC+7:**
- Cron Job chạy: SELECT tất cả User có `last_activity_date < today_vn - 1`.
- **Không reset ngay** trong DB để tránh confusion — chỉ log cảnh báo.
- Reset thực tế diễn ra lần tiếp theo User hoạt động (lazy evaluation — BR-4).
- Lý do: Tránh User bị reset trước khi họ kịp đăng nhập trong ngày.

### Post-conditions
- `Streaks.current_streak` và `longest_streak` được cập nhật chính xác.
- `Streaks.last_activity_date = today_vn`.
- `XP_Logs` có đủ bản ghi Lesson XP + Streak Bonus XP (nếu có).
- `User_Badges` cập nhật nếu đạt mốc Streak milestone.
- `Leaderboard_Snapshots` phản ánh tổng XP mới nhất.
- User thấy thông báo trực quan về Streak và XP ngay trên màn hình.

---

## Ma trận quyền truy cập Use Cases

| Use Case | Guest | Basic | Premium |
|---|---|---|---|
| UC-01: AI Practice Quiz | ❌ | ❌ (Chapter premium bị chặn) | ✅ |
| UC-01: AI Practice (Chapter 1 miễn phí) | ❌ | ✅ | ✅ |
| UC-02: Nâng cấp Premium | ❌ | ✅ | ➖ (đã có) |
| UC-03: Tra cứu Từ điển | ✅ | ✅ | ✅ |
| UC-04: Streak & XP | ❌ | ✅ | ✅ |

---

## Luồng Freemium Gate (Business Logic tổng quát)

```
User click vào Lesson/Quiz
        │
        ▼
[API Check] Chapter.is_premium == TRUE?
        │
    YES ─┤
        │   account_type == PREMIUM?
        │       │
        │    YES └──► ✅ Cho vào
        │    NO  └──► ❌ Redirect Paywall (UC-02)
        │
    NO  └──► ✅ Cho vào (miễn phí)
```

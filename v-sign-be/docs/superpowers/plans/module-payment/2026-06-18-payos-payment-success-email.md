# Implementation Plan - PayOS Payment Success Email Notification

**Ngày tạo:** 2026-06-18
**Module:** `payment`
**Trạng thái:** Ready to Implement

Tài liệu này mô tả kế hoạch chi tiết để tích hợp tính năng **gửi email thông báo thành công khi người dùng thanh toán gói dịch vụ** thông qua PayOS.

---

## 1. Mục tiêu

Sau khi người dùng thanh toán thành công (thông qua PayOS Webhook hoặc Return URL), hệ thống sẽ tự động gửi một email xác nhận giao dịch đến địa chỉ email của người dùng, bao gồm thông tin gói dịch vụ, số tiền và thời hạn sử dụng.

---

## 2. Phân tích code hiện tại

Logic kích hoạt gói dịch vụ cho người dùng (`upgradeUserTier`) đang nằm ở **2 điểm**:

| File | Method | Kích hoạt khi |
|---|---|---|
| `PayOSWebhookService.java` | `upgradeUserTier()` | PayOS Webhook gửi code `00` (PAID) |
| `PayOSPaymentService.java` | `upgradeUserTier()` | FE gọi Return URL endpoint và trạng thái là PAID |

Cả 2 điểm này cần được tích hợp email notification.

---

## 3. Các thay đổi cần thực hiện

### A. `PayOSWebhookService.java`

**Inject `EmailService`** và gọi `sendPaymentSuccessEmail()` trong method `upgradeUserTier()` ngay sau khi lưu `UserTierEntity` vào database.

```java
// Inject EmailService (via constructor)
private final EmailService emailService;

// Sau dòng userTierRepository.save(userTier);
emailService.sendPaymentSuccessEmail(
    order.getUser().getEmail(),
    order.getUser().getFullName(),
    order.getTier().getTitle(),
    order.getAmount(),
    order.getOrderCode(),
    userTier.getStartTime(),
    userTier.getEndTime()
);
```

### B. `PayOSPaymentService.java`

Tương tự, **inject `EmailService`** và gọi `sendPaymentSuccessEmail()` trong method `upgradeUserTier()` cùng logic như trên.

### C. `EmailService.java`

Thêm method chuyên biệt `sendPaymentSuccessEmail()` để tạo nội dung email HTML phù hợp:

```java
@Async
public void sendPaymentSuccessEmail(
    String to,
    String fullName,
    String tierName,
    Integer amount,
    long orderCode,
    LocalDateTime startTime,
    LocalDateTime endTime
) { ... }
```

---

## 4. Thiết kế Email HTML

**Subject:** `[V-Sign] Xác nhận thanh toán thành công - Gói ${tierName}`

**Nội dung email bao gồm:**

| Phần | Nội dung |
|---|---|
| Lời chào | Xin chào `${fullName}` |
| Thông báo | Thanh toán gói `${tierName}` thành công |
| Bảng chi tiết | Mã đơn hàng, Gói dịch vụ, Số tiền, Ngày bắt đầu, Ngày hết hạn |
| Ghi chú | Liên hệ hỗ trợ nếu có vấn đề |

---

## 5. Verification Plan

### Automated Tests
- Cập nhật `PayOSWebhookServiceTest.java` để verify `emailService.sendPaymentSuccessEmail()` được gọi với đúng tham số khi webhook code `00` được nhận.
- Cập nhật `PayOSPaymentServiceTest.java` để verify email được trigger trong flow return URL.

### Manual Verification
1. Khởi động Backend với cấu hình SMTP đầy đủ trong `secretKey.properties`.
2. Thực hiện thanh toán thử qua giao diện Frontend hoặc gọi trực tiếp PayOS return endpoint.
3. Kiểm tra log Backend và hòm thư email người dùng để xác nhận email được gửi thành công.

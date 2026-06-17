package com.vsign.backend.common.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:V-Sign <noreply@vsign.com>}")
    private String fromAddress;

    @Async
    public void sendEmail(String to, String subject, String bodyHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(bodyHtml, true);
            
            mailSender.send(message);
            log.info("Email successfully sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}. Reason: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendPaymentSuccessEmail(
            String to,
            String fullName,
            String tierName,
            Integer amount,
            long orderCode,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String formattedAmount = NumberFormat.getNumberInstance(new Locale("vi", "VN"))
                    .format(amount) + " VNĐ";
            String subject = "[V-Sign] Xác nhận thanh toán thành công - Gói " + tierName;
            String body = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;\">"
                    + "<div style=\"background: linear-gradient(135deg, #f97316, #fb923c); padding: 24px; text-align: center;\">"
                    + "<h1 style=\"color: white; margin: 0; font-size: 24px;\">🎉 Thanh Toán Thành Công!</h1>"
                    + "</div>"
                    + "<div style=\"padding: 24px;\">"
                    + "<p style=\"font-size: 16px;\">Xin chào <strong>" + fullName + "</strong>,</p>"
                    + "<p>Cảm ơn bạn đã nâng cấp lên <strong>V-Sign " + tierName + "</strong>. "
                    + "Gói dịch vụ của bạn đã được kích hoạt thành công!</p>"
                    + "<table style=\"width: 100%; border-collapse: collapse; margin: 20px 0;\">"
                    + "<tr style=\"background-color: #f9fafb;\"><td style=\"padding: 10px 14px; border: 1px solid #e0e0e0; font-weight: bold;\">Mã đơn hàng</td>"
                    + "<td style=\"padding: 10px 14px; border: 1px solid #e0e0e0;\">#" + orderCode + "</td></tr>"
                    + "<tr><td style=\"padding: 10px 14px; border: 1px solid #e0e0e0; font-weight: bold;\">Gói dịch vụ</td>"
                    + "<td style=\"padding: 10px 14px; border: 1px solid #e0e0e0;\">" + tierName + "</td></tr>"
                    + "<tr style=\"background-color: #f9fafb;\"><td style=\"padding: 10px 14px; border: 1px solid #e0e0e0; font-weight: bold;\">Số tiền</td>"
                    + "<td style=\"padding: 10px 14px; border: 1px solid #e0e0e0; color: #16a34a; font-weight: bold;\">" + formattedAmount + "</td></tr>"
                    + "<tr><td style=\"padding: 10px 14px; border: 1px solid #e0e0e0; font-weight: bold;\">Ngày bắt đầu</td>"
                    + "<td style=\"padding: 10px 14px; border: 1px solid #e0e0e0;\">" + (startTime != null ? startTime.format(fmt) : "-") + "</td></tr>"
                    + "<tr style=\"background-color: #f9fafb;\"><td style=\"padding: 10px 14px; border: 1px solid #e0e0e0; font-weight: bold;\">Ngày hết hạn</td>"
                    + "<td style=\"padding: 10px 14px; border: 1px solid #e0e0e0;\">" + (endTime != null ? endTime.format(fmt) : "-") + "</td></tr>"
                    + "</table>"
                    + "<p>Nếu bạn có bất kỳ thắc mắc nào, vui lòng liên hệ đội hỗ trợ của chúng tôi.</p>"
                    + "<p style=\"color: #6b7280; font-size: 13px;\">Email này được gửi tự động, vui lòng không trả lời trực tiếp.</p>"
                    + "</div>"
                    + "<div style=\"background-color: #f3f4f6; padding: 16px; text-align: center; color: #6b7280; font-size: 12px;\">"
                    + "© 2026 V-Sign. All rights reserved."
                    + "</div>"
                    + "</div>";
            sendEmail(to, subject, body);
        } catch (Exception e) {
            log.error("Failed to send payment success email to: {}. Reason: {}", to, e.getMessage());
        }
    }
}


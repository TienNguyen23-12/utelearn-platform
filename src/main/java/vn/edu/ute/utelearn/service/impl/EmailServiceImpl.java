package vn.edu.ute.utelearn.service.impl;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import vn.edu.ute.utelearn.service.EmailService;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username:utelearn.work@gmail.com}")
    private String fromEmail;

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        if (to == null || to.isBlank()) {
            log.warn("[EmailService] Bỏ qua gửi email do địa chỉ người nhận rỗng.");
            return;
        }
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            String sender = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail.trim() : "utelearn.work@gmail.com";
            helper.setFrom(sender, "UTELearn Platform");
            helper.setTo(to.trim());
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            
            javaMailSender.send(message);
            log.info("[EmailService] Đã gửi email thành công tới: {}", to);
        } catch (Exception e) {
            log.error("[EmailService] Không thể gửi email tới {}: {}", to, e.getMessage());
        }
    }
}

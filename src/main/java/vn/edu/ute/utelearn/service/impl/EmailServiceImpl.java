package vn.edu.ute.utelearn.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import vn.edu.ute.utelearn.config.UtelearnProperties;
import vn.edu.ute.utelearn.dto.OrderItemDTO;
import vn.edu.ute.utelearn.entity.Order;
import vn.edu.ute.utelearn.service.EmailService;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;
    private final UtelearnProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    @Override
    public void sendOfflinePaymentInstructionEmail(Order order) {
        if (order == null || order.getUser() == null || order.getUser().getEmail() == null || order.getUser().getEmail().isBlank()) {
            return;
        }

        String recipientEmail = order.getUser().getEmail().trim();
        String studentName = order.getUser().getFullName() != null ? order.getUser().getFullName() : order.getUser().getUsername();
        String subject = "[UTELearn] Hướng dẫn thanh toán đơn hàng Offline #" + order.getOrderCode();

        List<OrderItemDTO> items = deserializeItems(order.getItems());
        StringBuilder itemsHtml = new StringBuilder();
        for (OrderItemDTO item : items) {
            itemsHtml.append("<tr>")
                    .append("<td style=\"padding: 10px; border-bottom: 1px solid #e2e8f0; color: #1e293b;\"><strong>").append(item.getName()).append("</strong></td>")
                    .append("<td style=\"padding: 10px; border-bottom: 1px solid #e2e8f0; text-align: right; color: #0284c7; font-weight: bold;\">")
                    .append(item.getPrice() != null ? java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(item.getPrice()) + " VNĐ" : "0 VNĐ")
                    .append("</td>")
                    .append("</tr>");
        }

        String htmlBody = "<div style=\"font-family: Arial, sans-serif; max-width: 650px; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; background-color: #ffffff;\">"
                + "<div style=\"background: linear-gradient(135deg, #1e40af, #3b82f6); padding: 24px; text-align: center; color: #ffffff;\">"
                + "<h2 style=\"margin: 0; font-size: 24px;\">UTE<span style=\"color: #93c5fd;\">Learn</span> Platform</h2>"
                + "<p style=\"margin: 6px 0 0; font-size: 14px; opacity: 0.9;\">Hướng dẫn thanh toán đơn hàng Offline tại Quầy thu ngân</p>"
                + "</div>"
                + "<div style=\"padding: 24px;\">"
                + "<p style=\"font-size: 15px; color: #334155; margin-top: 0;\">Xin chào <strong>" + studentName + "</strong>,</p>"
                + "<p style=\"font-size: 14px; color: #475569; line-height: 1.6;\">Bạn vừa tạo đơn hàng thanh toán offline trên hệ thống UTELearn. Vui lòng xem thông tin chi tiết và hoàn tất nộp tiền theo hướng dẫn dưới đây:</p>"
                + "<div style=\"background-color: #f8fafc; border: 1px solid #cbd5e1; border-radius: 8px; padding: 16px; margin: 20px 0;\">"
                + "<div style=\"display: flex; justify-content: space-between; margin-bottom: 8px;\">"
                + "<span style=\"color: #64748b; font-size: 13px;\">Mã đơn hàng:</span> "
                + "<strong style=\"color: #dc2626; font-size: 18px; font-family: monospace;\">" + order.getOrderCode() + "</strong>"
                + "</div>"
                + "<div style=\"display: flex; justify-content: space-between; margin-bottom: 8px;\">"
                + "<span style=\"color: #64748b; font-size: 13px;\">Tổng tiền thanh toán:</span> "
                + "<strong style=\"color: #16a34a; font-size: 18px;\">" + order.getFormattedTotalAmount() + "</strong>"
                + "</div>"
                + "<div style=\"display: flex; justify-content: space-between;\">"
                + "<span style=\"color: #64748b; font-size: 13px;\">Thời hạn thanh toán:</span> "
                + "<strong style=\"color: #d97706; font-size: 13px;\">" + order.getFormattedExpiresAt() + " (Trong vòng 48h)</strong>"
                + "</div>"
                + "</div>"
                + "<table style=\"width: 100%; border-collapse: collapse; margin-bottom: 20px; font-size: 14px;\">"
                + "<thead><tr style=\"background-color: #f1f5f9;\">"
                + "<th style=\"padding: 10px; text-align: left; border-bottom: 2px solid #cbd5e1;\">Sản phẩm đăng ký</th>"
                + "<th style=\"padding: 10px; text-align: right; border-bottom: 2px solid #cbd5e1;\">Số tiền</th>"
                + "</tr></thead>"
                + "<tbody>" + itemsHtml + "</tbody>"
                + "</table>"
                + "<div style=\"background-color: #eff6ff; border-left: 4px solid #3b82f6; padding: 16px; border-radius: 4px; margin-bottom: 20px;\">"
                + "<h4 style=\"margin: 0 0 8px; color: #1e3a8a; font-size: 15px;\">Địa điểm và cách thức thanh toán:</h4>"
                + "<p style=\"margin: 4px 0; font-size: 13px; color: #1e40af;\"><strong>1. Quầy thu ngân:</strong> Phòng Kế hoạch - Tài chính (A1-01), Trường ĐH Sư phạm Kỹ thuật TP.HCM</p>"
                + "<p style=\"margin: 4px 0; font-size: 13px; color: #1e40af;\"><strong>2. Địa chỉ:</strong> Số 1 Võ Văn Ngân, P. Linh Chiểu, TP. Thủ Đức, TP. Hồ Chí Minh</p>"
                + "<p style=\"margin: 4px 0; font-size: 13px; color: #1e40af;\"><strong>3. Thời gian làm việc:</strong> Thứ 2 - Thứ 6 (07:30 - 16:30)</p>"
                + "<p style=\"margin: 4px 0; font-size: 13px; color: #1e40af;\"><strong>4. Hình thức chấp nhận:</strong> Tiền mặt hoặc Quét mã chuyển khoản trực tiếp tại quầy</p>"
                + "</div>"
                + "<div style=\"background-color: #fef2f2; border: 1px solid #fecaca; border-radius: 8px; padding: 14px; margin-bottom: 20px;\">"
                + "<h4 style=\"margin: 0 0 6px; color: #991b1b; font-size: 14px;\">&#9888; LƯU Ý BẢO VỆ QUYỀN LỢI & CHỐNG THẤT THOÁT:</h4>"
                + "<ul style=\"margin: 0; padding-left: 20px; font-size: 13px; color: #7f1d1d; line-height: 1.5;\">"
                + "<li>Học viên xuất trình chính xác <strong>Mã đơn hàng: " + order.getOrderCode() + "</strong> cho thu ngân tại quầy.</li>"
                + "<li><strong>Yêu cầu thu ngân in Phiếu biên nhận hoặc kiểm tra Email Biên nhận xác nhận kích hoạt thành công</strong> ngay tại quầy trước khi rời đi.</li>"
                + "<li>Khóa học sẽ tự động được mở trên tài khoản của bạn ngay khi thu ngân bấm xác nhận trên hệ thống.</li>"
                + "</ul>"
                + "</div>"
                + "<p style=\"font-size: 13px; color: #64748b; line-height: 1.5;\">Nếu có bất kỳ thắc mắc nào, vui lòng liên hệ Ban Đào tạo qua Hotline: (028) 38968641 hoặc gửi email tới support@utelearn.edu.vn.</p>"
                + "</div>"
                + "<div style=\"background-color: #f8fafc; border-top: 1px solid #e2e8f0; padding: 16px; text-align: center; font-size: 12px; color: #94a3b8;\">"
                + "UTELearn Platform &bull; Đại học Sư phạm Kỹ thuật TP.HCM &bull; Hệ thống đào tạo trực tuyến"
                + "</div>"
                + "</div>";

        sendHtmlEmail(recipientEmail, subject, htmlBody);
    }

    @Override
    public void sendPaymentReceiptEmail(Order order) {
        if (order == null || order.getUser() == null || order.getUser().getEmail() == null || order.getUser().getEmail().isBlank()) {
            return;
        }

        String recipientEmail = order.getUser().getEmail().trim();
        String studentName = order.getUser().getFullName() != null ? order.getUser().getFullName() : order.getUser().getUsername();
        String subject = "[UTELearn] Biên nhận thanh toán thành công đơn hàng #" + order.getOrderCode();

        List<OrderItemDTO> items = deserializeItems(order.getItems());
        StringBuilder itemsHtml = new StringBuilder();
        for (OrderItemDTO item : items) {
            itemsHtml.append("<tr>")
                    .append("<td style=\"padding: 10px; border-bottom: 1px solid #e2e8f0; color: #1e293b;\"><strong>").append(item.getName()).append("</strong></td>")
                    .append("<td style=\"padding: 10px; border-bottom: 1px solid #e2e8f0; text-align: right; color: #16a34a; font-weight: bold;\">")
                    .append(item.getPrice() != null ? java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(item.getPrice()) + " VNĐ" : "0 VNĐ")
                    .append("</td>")
                    .append("</tr>");
        }

        String baseUrl = properties.getRender() != null ? properties.getRender().getKeepAliveUrl() : "https://utelearn-platform.onrender.com";
        String targetUrl = baseUrl + "/home";
        if (!items.isEmpty()) {
            OrderItemDTO first = items.get(0);
            if ("COHORT".equalsIgnoreCase(first.getType()) && first.getCohortId() != null) {
                targetUrl = baseUrl + (first.getCourseSlug() != null
                        ? "/courses/" + first.getCourseSlug() + "/modules/" + first.getCohortId() + "/learn"
                        : "/cohorts/" + first.getCohortId() + "/learn");
            } else if ("COURSE".equalsIgnoreCase(first.getType()) && first.getCourseSlug() != null) {
                targetUrl = baseUrl + "/courses/" + first.getCourseSlug();
            }
        }

        String htmlBody = "<div style=\"font-family: Arial, sans-serif; max-width: 650px; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden; background-color: #ffffff;\">"
                + "<div style=\"background: linear-gradient(135deg, #15803d, #22c55e); padding: 24px; text-align: center; color: #ffffff;\">"
                + "<h2 style=\"margin: 0; font-size: 24px;\">&#10004; BIÊN NHẬN THANH TOÁN ĐIỆN TỬ</h2>"
                + "<p style=\"margin: 6px 0 0; font-size: 14px; opacity: 0.9;\">UTELearn Platform &bull; Xác nhận thanh toán thành công</p>"
                + "</div>"
                + "<div style=\"padding: 24px;\">"
                + "<p style=\"font-size: 15px; color: #334155; margin-top: 0;\">Kính gửi học viên <strong>" + studentName + "</strong>,</p>"
                + "<p style=\"font-size: 14px; color: #475569; line-height: 1.6;\">Hệ thống UTELearn xin thông báo đơn hàng của bạn đã được xác nhận thanh toán thành công. Toàn bộ quyền truy cập bài học và tài liệu đã được kích hoạt ngay tức thì.</p>"
                + "<div style=\"background-color: #f8fafc; border: 1px solid #cbd5e1; border-radius: 8px; padding: 16px; margin: 20px 0;\">"
                + "<div style=\"display: flex; justify-content: space-between; margin-bottom: 8px;\">"
                + "<span style=\"color: #64748b; font-size: 13px;\">Mã đơn hàng:</span> "
                + "<strong style=\"color: #1e293b; font-size: 16px; font-family: monospace;\">" + order.getOrderCode() + "</strong>"
                + "</div>"
                + "<div style=\"display: flex; justify-content: space-between; margin-bottom: 8px;\">"
                + "<span style=\"color: #64748b; font-size: 13px;\">Mã biên lai / giao dịch:</span> "
                + "<strong style=\"color: #0284c7; font-size: 14px; font-family: monospace;\">" + (order.getTransactionId() != null ? order.getTransactionId() : "N/A") + "</strong>"
                + "</div>"
                + "<div style=\"display: flex; justify-content: space-between; margin-bottom: 8px;\">"
                + "<span style=\"color: #64748b; font-size: 13px;\">Phương thức thanh toán:</span> "
                + "<strong style=\"color: #334155; font-size: 13px;\">" + order.getPaymentMethodDisplayName() + "</strong>"
                + "</div>"
                + "<div style=\"display: flex; justify-content: space-between; margin-bottom: 8px;\">"
                + "<span style=\"color: #64748b; font-size: 13px;\">Người xác nhận / Thu ngân:</span> "
                + "<strong style=\"color: #334155; font-size: 13px;\">" + (order.getConfirmedBy() != null ? order.getConfirmedBy() : "Hệ thống tự động") + "</strong>"
                + "</div>"
                + "<div style=\"display: flex; justify-content: space-between; margin-bottom: 8px;\">"
                + "<span style=\"color: #64748b; font-size: 13px;\">Thời gian thanh toán:</span> "
                + "<strong style=\"color: #334155; font-size: 13px;\">" + order.getFormattedCreatedAt() + "</strong>"
                + "</div>"
                + "<div style=\"display: flex; justify-content: space-between; border-top: 1px dashed #cbd5e1; padding-top: 8px; margin-top: 8px;\">"
                + "<span style=\"color: #64748b; font-size: 14px; font-weight: bold;\">Tổng số tiền đã nộp:</span> "
                + "<strong style=\"color: #16a34a; font-size: 20px;\">" + order.getFormattedTotalAmount() + "</strong>"
                + "</div>"
                + "</div>"
                + "<table style=\"width: 100%; border-collapse: collapse; margin-bottom: 24px; font-size: 14px;\">"
                + "<thead><tr style=\"background-color: #f1f5f9;\">"
                + "<th style=\"padding: 10px; text-align: left; border-bottom: 2px solid #cbd5e1;\">Khóa học / Module kích hoạt</th>"
                + "<th style=\"padding: 10px; text-align: right; border-bottom: 2px solid #cbd5e1;\">Học phí</th>"
                + "</tr></thead>"
                + "<tbody>" + itemsHtml + "</tbody>"
                + "</table>"
                + "<div style=\"text-align: center; margin: 30px 0;\">"
                + "<a href=\"" + targetUrl + "\" style=\"background-color: #2563eb; color: #ffffff; text-decoration: none; padding: 14px 32px; border-radius: 50px; font-weight: bold; font-size: 15px; display: inline-block;\">BẮT ĐẦU VÀO HỌC NGAY &rarr;</a>"
                + "</div>"
                + "<div style=\"background-color: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 8px; padding: 12px; margin-bottom: 16px; font-size: 13px; color: #166534;\">"
                + "<strong>Xác thực đối soát:</strong> Biên nhận này là chứng từ điện tử có giá trị chính thức ghi nhận học phí giữa Học viên và Nhà trường. Dữ liệu được đồng bộ trực tiếp vào sổ cái đối soát của Phòng Kế toán."
                + "</div>"
                + "<p style=\"font-size: 13px; color: #64748b; line-height: 1.5;\">Chúc bạn có những trải nghiệm học tập tuyệt vời tại UTELearn Platform!</p>"
                + "</div>"
                + "<div style=\"background-color: #f8fafc; border-top: 1px solid #e2e8f0; padding: 16px; text-align: center; font-size: 12px; color: #94a3b8;\">"
                + "UTELearn Platform &bull; Đại học Sư phạm Kỹ thuật TP.HCM &bull; Số 1 Võ Văn Ngân, TP. Thủ Đức, TP.HCM"
                + "</div>"
                + "</div>";

        sendHtmlEmail(recipientEmail, subject, htmlBody);
    }

    private List<OrderItemDTO> deserializeItems(String itemsJson) {
        if (itemsJson == null || itemsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(itemsJson, new TypeReference<List<OrderItemDTO>>() {});
        } catch (Exception e) {
            log.error("[EmailService] Lỗi parse items JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}

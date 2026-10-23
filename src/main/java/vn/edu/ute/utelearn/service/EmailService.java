package vn.edu.ute.utelearn.service;

import vn.edu.ute.utelearn.entity.Order;

public interface EmailService {
    void sendHtmlEmail(String to, String subject, String htmlBody);
    void sendOfflinePaymentInstructionEmail(Order order);
    void sendPaymentReceiptEmail(Order order);
}

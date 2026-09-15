package vn.edu.ute.utelearn.service;

public interface EmailService {
    void sendHtmlEmail(String to, String subject, String htmlBody);
}

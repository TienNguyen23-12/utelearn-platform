package vn.edu.ute.utelearn;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.ute.utelearn.config.UtelearnProperties;
import vn.edu.ute.utelearn.entity.Order;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.EmailService;
import vn.edu.ute.utelearn.service.impl.EmailServiceImpl;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    private UtelearnProperties properties;
    private EmailService emailService;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        properties = new UtelearnProperties();
        properties.getRender().setKeepAliveUrl("https://utelearn.edu.vn");

        emailService = new EmailServiceImpl(javaMailSender, properties);
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@utelearn.edu.vn");

        sampleUser = User.builder()
                .id(1L)
                .username("student_nguyen")
                .fullName("Nguyen Van B")
                .email("student_nguyen@utelearn.edu.vn")
                .build();
    }

    @Test
    void testSendHtmlEmail_Success() {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendHtmlEmail("test@ute.edu.vn", "Tiêu đề test", "<b>Nội dung test</b>");

        verify(javaMailSender, times(1)).createMimeMessage();
        verify(javaMailSender, times(1)).send(mimeMessage);
    }

    @Test
    void testSendHtmlEmail_BlankOrNullEmail_DoesNotSend() {
        emailService.sendHtmlEmail(null, "Tiêu đề", "Nội dung");
        emailService.sendHtmlEmail("   ", "Tiêu đề", "Nội dung");

        verify(javaMailSender, never()).createMimeMessage();
        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testSendHtmlEmail_HandlesExceptionGracefully() {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP connection timeout")).when(javaMailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() ->
                emailService.sendHtmlEmail("error@ute.edu.vn", "Tiêu đề", "<p>Nội dung</p>")
        );
        verify(javaMailSender, times(1)).send(mimeMessage);
    }

    @Test
    void testSendOfflinePaymentInstructionEmail_Success() {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        Order order = Order.builder()
                .id(101L)
                .orderCode("UTE101202")
                .user(sampleUser)
                .totalAmount(new BigDecimal("450000"))
                .status("PENDING")
                .paymentMethod("OFFLINE_CASH")
                .expiresAt(Instant.now().plusSeconds(172800))
                .items("[{\"type\":\"COHORT\",\"name\":\"Lớp Java Spring Boot\",\"price\":450000}]")
                .build();

        emailService.sendOfflinePaymentInstructionEmail(order);

        verify(javaMailSender, times(1)).send(mimeMessage);
    }

    @Test
    void testSendOfflinePaymentInstructionEmail_NullOrderOrEmail_DoesNotSend() {
        emailService.sendOfflinePaymentInstructionEmail(null);

        Order orderWithoutUser = Order.builder().orderCode("UTE001").build();
        emailService.sendOfflinePaymentInstructionEmail(orderWithoutUser);

        User userWithoutEmail = User.builder().username("no_email").build();
        Order orderWithoutEmail = Order.builder().orderCode("UTE002").user(userWithoutEmail).build();
        emailService.sendOfflinePaymentInstructionEmail(orderWithoutEmail);

        verify(javaMailSender, never()).createMimeMessage();
    }

    @Test
    void testSendPaymentReceiptEmail_Cohort_Success() {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        Order order = Order.builder()
                .id(201L)
                .orderCode("UTE201303")
                .user(sampleUser)
                .totalAmount(new BigDecimal("300000"))
                .status("SUCCESS")
                .paymentMethod("VIETQR_SEPAY")
                .transactionId("TX998877")
                .createdAt(Instant.now())
                .items("[{\"type\":\"COHORT\",\"cohortId\":15,\"courseSlug\":\"java-core\",\"name\":\"Module Java Cơ Bản\",\"price\":300000}]")
                .build();

        emailService.sendPaymentReceiptEmail(order);

        verify(javaMailSender, times(1)).send(mimeMessage);
    }

    @Test
    void testSendPaymentReceiptEmail_Course_Success() {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        Order order = Order.builder()
                .id(202L)
                .orderCode("UTE202404")
                .user(sampleUser)
                .totalAmount(new BigDecimal("990000"))
                .status("SUCCESS")
                .paymentMethod("VIETQR_SEPAY")
                .transactionId("TX112233")
                .createdAt(Instant.now())
                .items("[{\"type\":\"COURSE\",\"courseSlug\":\"fullstack-spring-react\",\"name\":\"Toàn bộ khóa Fullstack\",\"price\":990000}]")
                .build();

        emailService.sendPaymentReceiptEmail(order);

        verify(javaMailSender, times(1)).send(mimeMessage);
    }

    @Test
    void testSendPaymentReceiptEmail_WalletDeposit_Success() {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        Order order = Order.builder()
                .id(203L)
                .orderCode("UTE203505")
                .user(sampleUser)
                .totalAmount(new BigDecimal("500000"))
                .status("SUCCESS")
                .paymentMethod("VIETQR_SEPAY")
                .transactionId("WALLET_DEP_5566")
                .createdAt(Instant.now())
                .items("[{\"type\":\"WALLET_DEPOSIT\",\"name\":\"Nạp tiền ví học tập\",\"price\":500000}]")
                .build();

        emailService.sendPaymentReceiptEmail(order);

        verify(javaMailSender, times(1)).send(mimeMessage);
    }

    @Test
    void testSendPaymentReceiptEmail_NullOrderOrEmail_DoesNotSend() {
        emailService.sendPaymentReceiptEmail(null);

        Order orderWithoutUser = Order.builder().orderCode("UTE003").build();
        emailService.sendPaymentReceiptEmail(orderWithoutUser);

        User userBlankEmail = User.builder().email("   ").build();
        Order orderBlankEmail = Order.builder().orderCode("UTE004").user(userBlankEmail).build();
        emailService.sendPaymentReceiptEmail(orderBlankEmail);

        verify(javaMailSender, never()).createMimeMessage();
    }

    @Test
    void testSendPaymentReceiptEmail_WithDiscount_Success() {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        Order order = Order.builder()
                .id(204L)
                .orderCode("UTE204606")
                .user(sampleUser)
                .totalAmount(new BigDecimal("700000"))
                .discountCode("GIAM30K")
                .discountAmount(new BigDecimal("300000"))
                .status("SUCCESS")
                .paymentMethod("VIETQR_SEPAY")
                .transactionId("TX_DISC_123")
                .createdAt(Instant.now())
                .items("[{\"type\":\"COURSE\",\"name\":\"Khoá học React Spring\",\"price\":1000000}]")
                .build();

        emailService.sendPaymentReceiptEmail(order);

        verify(javaMailSender, times(1)).send(mimeMessage);
    }

    @Test
    void testSendOfflinePaymentInstructionEmail_WithDiscount_Success() {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        Order order = Order.builder()
                .id(102L)
                .orderCode("UTE102303")
                .user(sampleUser)
                .totalAmount(new BigDecimal("350000"))
                .discountCode("DISCOUNT50")
                .discountAmount(new BigDecimal("50000"))
                .status("PENDING")
                .paymentMethod("OFFLINE_COUNTER")
                .expiresAt(Instant.now().plusSeconds(172800))
                .items("[{\"type\":\"COHORT\",\"name\":\"Module Java Nâng Cao\",\"price\":400000}]")
                .build();

        emailService.sendOfflinePaymentInstructionEmail(order);

        verify(javaMailSender, times(1)).send(mimeMessage);
    }
}

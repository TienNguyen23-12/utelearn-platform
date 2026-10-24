package vn.edu.ute.utelearn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import vn.edu.ute.utelearn.config.UtelearnProperties;
import vn.edu.ute.utelearn.controller.api.SepayWebhookApiController;
import vn.edu.ute.utelearn.dao.CohortMemberRepository;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.CohortScheduleRepository;
import vn.edu.ute.utelearn.dao.CourseRepository;
import vn.edu.ute.utelearn.dao.EnrollmentRepository;
import vn.edu.ute.utelearn.dao.LessonRepository;
import vn.edu.ute.utelearn.dao.OrderRepository;
import vn.edu.ute.utelearn.dao.UserRepository;
import vn.edu.ute.utelearn.dto.SepayWebhookDTO;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.CohortMember;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.Order;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.EmailService;
import vn.edu.ute.utelearn.service.OrderService;
import vn.edu.ute.utelearn.service.impl.OrderServiceImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderFeatureTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CohortRepository cohortRepository;

    @Mock
    private CohortMemberRepository cohortMemberRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CohortScheduleRepository cohortScheduleRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private vn.edu.ute.utelearn.service.AuthService authService;

    @Mock
    private org.springframework.ui.Model model;

    @Mock
    private org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes;

    private UtelearnProperties properties;
    private OrderService orderService;
    private vn.edu.ute.utelearn.controller.OrderController orderController;

    private User sampleUser;
    private Course sampleCourse;
    private Cohort sampleCohort;

    @BeforeEach
    void setUp() {
        properties = new UtelearnProperties();
        properties.getPayment().setBankId("MB");
        properties.getPayment().setAccountNo("0901234567");
        properties.getPayment().setAccountName("DAI HOC SU PHAM KY THUAT TPHCM");
        properties.getPayment().setTemplate("compact");
        properties.getPayment().setSepayApiKey("TEST_SEPAY_API_KEY");

        orderService = new OrderServiceImpl(
                orderRepository,
                cohortRepository,
                cohortMemberRepository,
                courseRepository,
                cohortScheduleRepository,
                lessonRepository,
                enrollmentRepository,
                userRepository,
                properties,
                emailService
        );

        orderController = new vn.edu.ute.utelearn.controller.OrderController(orderService, authService);

        sampleUser = User.builder()
                .id(1L)
                .username("student1")
                .fullName("Nguyen Van A")
                .email("student1@utelearn.edu.vn")
                .balance(new BigDecimal("500000"))
                .build();

        sampleCourse = Course.builder()
                .id(10L)
                .title("Lập trình Web Cơ bản")
                .slug("lap-trinh-web-co-ban")
                .price(new BigDecimal("1000000"))
                .code("CRS-WEB")
                .build();

        sampleCohort = Cohort.builder()
                .id(101L)
                .name("Module 1: HTML5 và CSS3")
                .code("MOD-WEB-01")
                .price(new BigDecimal("400000"))
                .maxCapacity(50)
                .currentEnrolled(10)
                .course(sampleCourse)
                .build();
    }

    @Test
    void testCreateCohortOrder_GeneratesPendingOrderWithVietQr() {
        when(cohortRepository.findById(101L)).thenReturn(Optional.of(sampleCohort));
        when(cohortMemberRepository.existsByCohort_IdAndUser_Id(101L, 1L)).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order order = orderService.createCohortOrder(sampleUser, 101L, "VIETQR_SEPAY");

        assertNotNull(order);
        assertTrue(order.getOrderCode().startsWith("UTE"));
        assertEquals("PENDING", order.getStatus());
        assertEquals(new BigDecimal("400000"), order.getTotalAmount());
        assertEquals("VIETQR_SEPAY", order.getPaymentMethod());
        assertTrue(order.getItems().contains("MOD-WEB-01"));
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testCreateCohortOrder_CohortFullThrowsIllegalStateException() {
        Cohort fullCohort = Cohort.builder()
                .id(109L)
                .name("Module Full")
                .code("MOD-FULL")
                .price(new BigDecimal("200000"))
                .maxCapacity(20)
                .currentEnrolled(20)
                .course(sampleCourse)
                .build();

        when(cohortRepository.findById(109L)).thenReturn(Optional.of(fullCohort));
        when(cohortMemberRepository.existsByCohort_IdAndUser_Id(109L, 1L)).thenReturn(false);

        assertThrows(IllegalStateException.class, () ->
                orderService.createCohortOrder(sampleUser, 109L, "VIETQR_SEPAY"));
    }

    @Test
    void testCreateFreeCohortOrder_DirectlySuccessAndEnrolls() {
        Cohort freeCohort = Cohort.builder()
                .id(102L)
                .name("Module Free Introduction")
                .code("MOD-FREE-01")
                .price(BigDecimal.ZERO)
                .maxCapacity(100)
                .currentEnrolled(0)
                .course(sampleCourse)
                .build();

        when(cohortRepository.findById(102L)).thenReturn(Optional.of(freeCohort));
        when(cohortMemberRepository.existsByCohort_IdAndUser_Id(102L, 1L)).thenReturn(false);
        when(cohortMemberRepository.countByCohort_Id(102L)).thenReturn(1L);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order order = orderService.createFreeCohortOrder(sampleUser, 102L);

        assertNotNull(order);
        assertEquals("SUCCESS", order.getStatus());
        assertEquals("FREE", order.getPaymentMethod());
        assertEquals(BigDecimal.ZERO, order.getTotalAmount());
        assertNotNull(order.getPaidAt());
        verify(cohortMemberRepository, times(1)).save(any(CohortMember.class));
        verify(cohortRepository, times(1)).save(freeCohort);
    }

    @Test
    void testCreateCourseOrder_WithUpsellCalculation() {
        Cohort c1 = Cohort.builder().id(101L).price(new BigDecimal("400000")).code("M1").name("Module 1").build();
        Cohort c2 = Cohort.builder().id(102L).price(new BigDecimal("400000")).code("M2").name("Module 2").build();
        Cohort c3 = Cohort.builder().id(103L).price(new BigDecimal("400000")).code("M3").name("Module 3").build();

        when(courseRepository.findBySlug("lap-trinh-web-co-ban")).thenReturn(Optional.of(sampleCourse));
        when(cohortRepository.findByCourseIdAndStatusIn(eq(10L), any()))
                .thenReturn(List.of(c1, c2, c3));

        CohortMember cm1 = CohortMember.builder().user(sampleUser).cohort(c1).build();
        when(cohortMemberRepository.findByCohort_Course_IdAndUser_Id(10L, 1L)).thenReturn(List.of(cm1));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order order = orderService.createCourseOrder(sampleUser, "lap-trinh-web-co-ban", "VIETQR_SEPAY");

        assertNotNull(order);
        assertEquals("PENDING", order.getStatus());
        assertEquals(new BigDecimal("600000"), order.getTotalAmount());
        assertTrue(order.getItems().contains("\"isUpgrade\":true"));
    }

    @Test
    void testCreateCourseOrder_WithSelectedCohortSubset() {
        Cohort c1 = Cohort.builder().id(101L).price(new BigDecimal("400000")).code("M1").name("Module 1").build();
        Cohort c2 = Cohort.builder().id(102L).price(new BigDecimal("400000")).code("M2").name("Module 2").build();
        Cohort c3 = Cohort.builder().id(103L).price(new BigDecimal("400000")).code("M3").name("Module 3").build();

        when(courseRepository.findBySlug("lap-trinh-web-co-ban")).thenReturn(Optional.of(sampleCourse));
        when(cohortRepository.findByCourseIdAndStatusIn(eq(10L), any()))
                .thenReturn(List.of(c1, c2, c3));

        CohortMember cm1 = CohortMember.builder().user(sampleUser).cohort(c1).build();
        when(cohortMemberRepository.findByCohort_Course_IdAndUser_Id(10L, 1L)).thenReturn(List.of(cm1));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order order = orderService.createCourseOrder(sampleUser, "lap-trinh-web-co-ban", List.of(103L), "VIETQR_SEPAY");

        assertNotNull(order);
        assertEquals("PENDING", order.getStatus());
        assertEquals(new BigDecimal("400000"), order.getTotalAmount());
        assertTrue(order.getItems().contains("Module 3"));
        assertTrue(order.getItems().contains("[103]"));
    }

    @Test
    void testFulfillOrder_IdempotentAndEnrollsStudent() {
        Order pendingOrder = Order.builder()
                .id(50L)
                .orderCode("UTE123456")
                .user(sampleUser)
                .totalAmount(new BigDecimal("400000"))
                .status("PENDING")
                .items("[{\"type\":\"COHORT\",\"cohortId\":101,\"cohortCode\":\"MOD-WEB-01\",\"name\":\"Module 1\",\"price\":400000}]")
                .build();

        when(orderRepository.findByOrderCode("UTE123456")).thenReturn(Optional.of(pendingOrder));
        when(cohortRepository.findById(101L)).thenReturn(Optional.of(sampleCohort));
        when(cohortMemberRepository.existsByCohort_IdAndUser_Id(101L, 1L)).thenReturn(false);
        when(cohortMemberRepository.countByCohort_Id(101L)).thenReturn(11L);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order fulfilled = orderService.fulfillOrder("UTE123456", "TXN_SEPAY_999", "VIETQR_SEPAY");

        assertNotNull(fulfilled);
        assertEquals("SUCCESS", fulfilled.getStatus());
        assertEquals("TXN_SEPAY_999", fulfilled.getTransactionId());
        assertEquals("VIETQR_SEPAY", fulfilled.getPaymentMethod());
        assertNotNull(fulfilled.getPaidAt());
        verify(cohortMemberRepository, times(1)).save(any(CohortMember.class));
        verify(cohortRepository, times(1)).save(sampleCohort);

        Order secondCall = orderService.fulfillOrder("UTE123456", "TXN_DUPLICATE", "VIETQR_SEPAY");
        assertEquals("SUCCESS", secondCall.getStatus());
        verify(cohortMemberRepository, times(1)).save(any(CohortMember.class));
    }

    @Test
    void testProcessSepayWebhook_SuccessWithRegexOrderCodeExtraction() {
        Order pendingOrder = Order.builder()
                .id(50L)
                .orderCode("UTE789012")
                .user(sampleUser)
                .totalAmount(new BigDecimal("400000"))
                .status("PENDING")
                .items("[{\"type\":\"COHORT\",\"cohortId\":101,\"name\":\"Module 1\",\"price\":400000}]")
                .build();

        when(orderRepository.findByOrderCode("UTE789012")).thenReturn(Optional.of(pendingOrder));
        when(cohortRepository.findById(101L)).thenReturn(Optional.of(sampleCohort));
        when(cohortMemberRepository.existsByCohort_IdAndUser_Id(101L, 1L)).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SepayWebhookDTO payload = SepayWebhookDTO.builder()
                .id(9999L)
                .gateway("MBBank")
                .accountNumber("0901234567")
                .content("MBVCB.12345 UTE789012 Nguyen Van A chuyen tien")
                .transferAmount(new BigDecimal("400000"))
                .referenceCode("FT2429699999")
                .build();

        boolean processed = orderService.processSepayWebhook(payload, "Apikey TEST_SEPAY_API_KEY");

        assertTrue(processed);
        assertEquals("SUCCESS", pendingOrder.getStatus());
        assertEquals("FT2429699999", pendingOrder.getTransactionId());
    }

    @Test
    void testProcessSepayWebhook_InvalidApiKeyOrShortAmountFails() {
        SepayWebhookDTO payload = SepayWebhookDTO.builder()
                .id(9999L)
                .content("UTE789012")
                .transferAmount(new BigDecimal("100000"))
                .build();

        boolean invalidKey = orderService.processSepayWebhook(payload, "Apikey WRONG_KEY");
        assertFalse(invalidKey);

        Order pendingOrder = Order.builder()
                .orderCode("UTE789012")
                .totalAmount(new BigDecimal("400000"))
                .status("PENDING")
                .build();
        when(orderRepository.findByOrderCode("UTE789012")).thenReturn(Optional.of(pendingOrder));

        boolean underpaid = orderService.processSepayWebhook(payload, "Apikey TEST_SEPAY_API_KEY");
        assertFalse(underpaid);
        assertEquals("PENDING", pendingOrder.getStatus());
    }

    @Test
    void testManualConfirmPayment_ByAdminOrFinance() {
        Order pendingOrder = Order.builder()
                .id(77L)
                .orderCode("UTE555666")
                .user(sampleUser)
                .totalAmount(new BigDecimal("400000"))
                .status("PENDING")
                .items("[{\"type\":\"COHORT\",\"cohortId\":101,\"name\":\"Module 1\",\"price\":400000}]")
                .build();

        when(orderRepository.findById(77L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.findByOrderCode("UTE555666")).thenReturn(Optional.of(pendingOrder));
        when(cohortRepository.findById(101L)).thenReturn(Optional.of(sampleCohort));
        when(cohortMemberRepository.existsByCohort_IdAndUser_Id(101L, 1L)).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order confirmed = orderService.manualConfirmPayment(77L, "finance_officer");

        assertNotNull(confirmed);
        assertEquals("SUCCESS", confirmed.getStatus());
        assertEquals("MANUAL_ADMIN", confirmed.getPaymentMethod());
        assertEquals("MANUAL_BY_finance_officer", confirmed.getTransactionId());
        verify(cohortMemberRepository, times(1)).save(any(CohortMember.class));
    }

    @Test
    void testCancelOrder_SuccessAndCannotCancelPaidOrder() {
        Order pendingOrder = Order.builder()
                .id(88L)
                .orderCode("UTE888999")
                .status("PENDING")
                .build();

        when(orderRepository.findById(88L)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order cancelled = orderService.cancelOrder(88L, "Khách hàng đổi ý");
        assertEquals("CANCELLED", cancelled.getStatus());
        assertTrue(cancelled.getTransactionId().contains("Khách hàng đổi ý"));

        Order paidOrder = Order.builder()
                .id(89L)
                .orderCode("UTE111222")
                .status("SUCCESS")
                .build();
        when(orderRepository.findById(89L)).thenReturn(Optional.of(paidOrder));

        assertThrows(IllegalStateException.class, () -> orderService.cancelOrder(89L, "Thử hủy"));
    }

    @Test
    void testGenerateVietQrUrl() {
        String qrUrl = orderService.generateVietQrUrl("UTE123456", new BigDecimal("500000"));

        assertNotNull(qrUrl);
        assertTrue(qrUrl.startsWith("https://qr.sepay.vn/img?acc=0901234567&bank=MB&amount=500000&des=UTE123456&template=compact"));
    }

    @Test
    void testSepayWebhookApiController_StatusPolling() {
        Order successOrder = Order.builder()
                .orderCode("UTE333444")
                .user(sampleUser)
                .status("SUCCESS")
                .totalAmount(new BigDecimal("400000"))
                .items("[{\"type\":\"COHORT\",\"cohortId\":101,\"courseSlug\":\"lap-trinh-web-co-ban\",\"name\":\"M1\",\"price\":400000}]")
                .build();

        when(orderRepository.findByOrderCode("UTE333444")).thenReturn(Optional.of(successOrder));

        SepayWebhookApiController controller = new SepayWebhookApiController(orderService);
        ResponseEntity<Map<String, Object>> response = controller.checkOrderStatus("UTE333444");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("UTE333444", response.getBody().get("orderCode"));
        assertEquals("SUCCESS", response.getBody().get("status"));
        assertEquals(true, response.getBody().get("isPaid"));
        assertTrue(response.getBody().get("redirectUrl").toString().contains("learn"));
    }

    @Test
    void testCreateDepositOrder_GeneratesPendingDepositOrder() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order deposit = orderService.createDepositOrder(sampleUser, new BigDecimal("200000"));

        assertNotNull(deposit);
        assertTrue(deposit.getOrderCode().startsWith("UTENAP"));
        assertEquals("PENDING", deposit.getStatus());
        assertEquals(new BigDecimal("200000"), deposit.getTotalAmount());
        assertEquals("VIETQR_SEPAY", deposit.getPaymentMethod());
        assertTrue(deposit.getItems().contains("WALLET_DEPOSIT"));
    }

    @Test
    void testFulfillOrder_WithWalletDeposit_CreditsUserBalance() {
        Order depositOrder = Order.builder()
                .id(60L)
                .orderCode("UTENAP555666")
                .user(sampleUser)
                .totalAmount(new BigDecimal("200000"))
                .status("PENDING")
                .items("[{\"type\":\"WALLET_DEPOSIT\",\"price\":200000}]")
                .build();

        when(orderRepository.findByOrderCode("UTENAP555666")).thenReturn(Optional.of(depositOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order fulfilled = orderService.fulfillOrder("UTENAP555666", "TXN_DEP_999", "VIETQR_SEPAY");

        assertNotNull(fulfilled);
        assertEquals("SUCCESS", fulfilled.getStatus());
        assertEquals(new BigDecimal("700000"), sampleUser.getBalance());
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    void testPayWithWallet_DeductsBalanceAndEnrolls() {
        when(cohortRepository.findById(101L)).thenReturn(Optional.of(sampleCohort));
        when(cohortMemberRepository.existsByCohort_IdAndUser_Id(101L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order order = orderService.payWithWallet(sampleUser, 101L);

        assertNotNull(order);
        assertEquals("SUCCESS", order.getStatus());
        assertEquals("WALLET", order.getPaymentMethod());
        assertEquals(new BigDecimal("100000"), sampleUser.getBalance());
        verify(userRepository, times(1)).save(sampleUser);
        verify(cohortMemberRepository, times(1)).save(any(CohortMember.class));
    }

    @Test
    void testPayWithWallet_InsufficientBalance_ThrowsException() {
        sampleUser.setBalance(new BigDecimal("50000"));
        when(cohortRepository.findById(101L)).thenReturn(Optional.of(sampleCohort));
        when(cohortMemberRepository.existsByCohort_IdAndUser_Id(101L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        assertThrows(IllegalStateException.class, () -> orderService.payWithWallet(sampleUser, 101L));
    }

    @Test
    void testShowOrderHistory_AuthenticatedUser_ReturnsOrdersIndexView() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(sampleUser));
        when(orderRepository.findByUserOrderByCreatedAtDesc(sampleUser)).thenReturn(Collections.emptyList());
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        String view = orderController.showOrderHistory("ALL", null, null, null, null, null, model, redirectAttributes);

        assertEquals("orders/index", view);
        verify(model, times(1)).addAttribute(eq("orders"), any());
        verify(model, times(1)).addAttribute("totalOrders", 0);
        verify(model, times(1)).addAttribute("completedCount", 0L);
        verify(model, times(1)).addAttribute("pendingCount", 0L);
        verify(model, times(1)).addAttribute("currentUser", sampleUser);
        verify(model, times(1)).addAttribute(eq("userBalance"), any());
        verify(model, times(1)).addAttribute(eq("formattedUserBalance"), any());
    }

    @Test
    void testShowOrderHistory_Unauthenticated_RedirectsToLogin() {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.empty());

        String view = orderController.showOrderHistory("ALL", null, null, null, null, null, model, redirectAttributes);

        assertEquals("redirect:/login", view);
    }

    @Test
    void testShowOrderHistory_WithFilter_FiltersCorrectly() {
        Order o1 = Order.builder().id(1L).orderCode("UTE111").totalAmount(new BigDecimal("100000")).status("SUCCESS").items("[]").build();
        Order o2 = Order.builder().id(2L).orderCode("UTE222").totalAmount(new BigDecimal("500000")).status("PENDING").items("[]").build();
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(sampleUser));
        when(orderRepository.findByUserOrderByCreatedAtDesc(sampleUser)).thenReturn(List.of(o1, o2));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        String view = orderController.showOrderHistory("SUCCESS", "UTE111", null, null, new BigDecimal("50000"), new BigDecimal("200000"), model, redirectAttributes);

        assertEquals("orders/index", view);
        verify(model, times(1)).addAttribute("filteredCount", 1);
    }

    @Test
    void testShowOrderDetail_Owner_ReturnsDetailView() {
        Order sampleOrder = Order.builder()
                .id(99L)
                .orderCode("UTE123456")
                .user(sampleUser)
                .totalAmount(new BigDecimal("400000"))
                .status("SUCCESS")
                .paymentMethod("VIETQR")
                .items("[]")
                .build();

        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(sampleUser));
        when(orderRepository.findByOrderCode("UTE123456")).thenReturn(Optional.of(sampleOrder));

        String view = orderController.showOrderDetail("UTE123456", model, redirectAttributes);

        assertEquals("orders/detail", view);
        verify(model, times(1)).addAttribute("order", sampleOrder);
        verify(model, times(1)).addAttribute(eq("orderDTO"), any());
        verify(model, times(1)).addAttribute("currentUser", sampleUser);
    }

    @Test
    void testFulfillOrder_ExpiredOrderThrowsExceptionAndMarksCancelled() {
        Order expiredOrder = Order.builder()
                .id(105L)
                .orderCode("UTE999111")
                .user(sampleUser)
                .totalAmount(new BigDecimal("200000"))
                .status("PENDING")
                .expiresAt(java.time.Instant.now().minus(2, java.time.temporal.ChronoUnit.HOURS))
                .build();

        when(orderRepository.findByOrderCode("UTE999111")).thenReturn(Optional.of(expiredOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(IllegalStateException.class, () ->
                orderService.fulfillOrder("UTE999111", "TXN_LATE", "VIETQR_TEST"));

        assertEquals("CANCELLED", expiredOrder.getStatus());
        verify(orderRepository, times(1)).save(expiredOrder);
    }

    @Test
    void testProcessSepayWebhook_ExpiredOrderFailsAndCancels() {
        Order expiredOrder = Order.builder()
                .id(106L)
                .orderCode("UTE999222")
                .user(sampleUser)
                .totalAmount(new BigDecimal("300000"))
                .status("PENDING")
                .expiresAt(java.time.Instant.now().minus(1, java.time.temporal.ChronoUnit.HOURS))
                .build();

        when(orderRepository.findByOrderCode("UTE999222")).thenReturn(Optional.of(expiredOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SepayWebhookDTO payload = SepayWebhookDTO.builder()
                .id(8888L)
                .content("Chuyen tien UTE999222")
                .transferAmount(new BigDecimal("300000"))
                .build();

        boolean processed = orderService.processSepayWebhook(payload, "Apikey TEST_SEPAY_API_KEY");

        assertFalse(processed);
        assertEquals("CANCELLED", expiredOrder.getStatus());
        verify(orderRepository, times(1)).save(expiredOrder);
    }

    @Test
    void testPayCourseWithWallet_Success_DeductsBalanceAndEnrolls() {
        sampleUser.setBalance(new BigDecimal("1000000"));
        when(courseRepository.findBySlug("web-dev")).thenReturn(Optional.of(sampleCourse));
        when(cohortRepository.findByCourseIdAndStatusIn(eq(10L), any())).thenReturn(List.of(sampleCohort));
        when(cohortMemberRepository.findByCohort_Course_IdAndUser_Id(10L, 1L)).thenReturn(Collections.emptyList());
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order order = orderService.payCourseWithWallet(sampleUser, "web-dev");

        assertNotNull(order);
        assertEquals("SUCCESS", order.getStatus());
        assertEquals("WALLET", order.getPaymentMethod());
        assertEquals(BigDecimal.ZERO, sampleUser.getBalance());
        verify(userRepository, times(1)).save(sampleUser);
        verify(cohortMemberRepository, times(1)).save(any(CohortMember.class));
        verify(emailService, times(1)).sendPaymentReceiptEmail(any(Order.class));
    }

    @Test
    void testCreateCashierOrder_OfflinePending_SendsInstructionEmail() {
        when(userRepository.findByEmail("student1@utelearn.edu.vn")).thenReturn(Optional.of(sampleUser));
        when(cohortRepository.findById(101L)).thenReturn(Optional.of(sampleCohort));
        when(cohortMemberRepository.existsByCohort_IdAndUser_Id(101L, 1L)).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order order = orderService.createCashierOrder(
                "student1@utelearn.edu.vn",
                "COHORT",
                101L,
                null,
                "OFFLINE_CASH",
                false,
                "Thu tiền tại phòng A1-01",
                "cashier01"
        );

        assertNotNull(order);
        assertEquals("PENDING", order.getStatus());
        assertEquals("OFFLINE_COUNTER", order.getPaymentMethod());
        assertNotNull(order.getExpiresAt());
        verify(emailService, times(1)).sendOfflinePaymentInstructionEmail(any(Order.class));
    }

    @Test
    void testCashierConfirmPayment_Success_EnrollsAndSendsReceiptEmail() {
        Order pendingOrder = Order.builder()
                .id(901L)
                .orderCode("UTE901001")
                .user(sampleUser)
                .totalAmount(new BigDecimal("400000"))
                .status("PENDING")
                .items("[{\"type\":\"COHORT\",\"cohortId\":101,\"name\":\"Module 1\",\"price\":400000}]")
                .build();

        when(orderRepository.findById(901L)).thenReturn(Optional.of(pendingOrder));
        when(cohortRepository.findById(101L)).thenReturn(Optional.of(sampleCohort));
        when(cohortMemberRepository.existsByCohort_IdAndUser_Id(101L, 1L)).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order confirmed = orderService.cashierConfirmPayment(901L, "cashier_hoa", "OFFLINE_CASH", "Đã thu tiền mặt đủ");

        assertNotNull(confirmed);
        assertEquals("SUCCESS", confirmed.getStatus());
        assertEquals("cashier_hoa", confirmed.getConfirmedBy());
        assertEquals("PT-901", confirmed.getTransactionId());
        verify(cohortMemberRepository, times(1)).save(any(CohortMember.class));
        verify(emailService, times(1)).sendPaymentReceiptEmail(any(Order.class));
    }
    @Test
    void testCreateCashierMultiOrder_Success_MultipleItems() {
        when(userRepository.findByEmail("student1@utelearn.edu.vn")).thenReturn(Optional.of(sampleUser));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(sampleCourse));
        when(cohortRepository.findByCourseIdAndStatusIn(eq(10L), any())).thenReturn(List.of(sampleCohort));
        when(cohortMemberRepository.existsByCohort_IdAndUser_Id(101L, 1L)).thenReturn(false);
        when(cohortRepository.findById(101L)).thenReturn(Optional.of(sampleCohort));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order order = orderService.createCashierMultiOrder(
                "student1@utelearn.edu.vn",
                List.of(10L),
                List.of(101L),
                "OFFLINE_CASH",
                true,
                "Thu tại quầy gộp",
                "cashier01"
        );

        assertNotNull(order);
        assertEquals("SUCCESS", order.getStatus());
        assertEquals("PT-" + order.getId(), order.getTransactionId());
        assertEquals(new BigDecimal("1400000"), order.getTotalAmount());
        verify(emailService, times(1)).sendPaymentReceiptEmail(any(Order.class));
    }

    @Test
    void testGetAvailableProductsForStudent_FiltersEnrolledAndPending() {
        when(userRepository.findByEmail("student1@utelearn.edu.vn")).thenReturn(Optional.of(sampleUser));

        CohortMember enrolledCm = CohortMember.builder().user(sampleUser).cohort(sampleCohort).build();
        when(cohortMemberRepository.findByUserOrderByEnrolledAtDesc(sampleUser)).thenReturn(List.of(enrolledCm));

        Order pendingOrder = Order.builder()
                .id(777L)
                .orderCode("UTE777888")
                .status("PENDING")
                .expiresAt(Instant.now().plusSeconds(3600))
                .items("[{\"type\":\"COURSE\",\"courseId\":10,\"name\":\"Lập trình Web\"}]")
                .build();
        when(orderRepository.findByUserOrderByCreatedAtDesc(sampleUser)).thenReturn(List.of(pendingOrder));

        when(courseRepository.findAll()).thenReturn(List.of(sampleCourse));
        when(cohortRepository.findAll()).thenReturn(List.of(sampleCohort));

        Map<String, Object> result = orderService.getAvailableProductsForStudent("student1@utelearn.edu.vn");

        assertNotNull(result);
        List<?> availableCourses = (List<?>) result.get("availableCourses");
        List<?> availableCohorts = (List<?>) result.get("availableCohorts");

        assertTrue(availableCourses.isEmpty());
        assertTrue(availableCohorts.isEmpty());
    }
}
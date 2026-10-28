package vn.edu.ute.utelearn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.ute.utelearn.dao.CartItemRepository;
import vn.edu.ute.utelearn.dao.CohortMemberRepository;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.CourseRepository;
import vn.edu.ute.utelearn.dto.CartSummaryDTO;
import vn.edu.ute.utelearn.entity.*;
import vn.edu.ute.utelearn.service.DiscountService;
import vn.edu.ute.utelearn.service.OrderService;
import vn.edu.ute.utelearn.service.impl.CartServiceImpl;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CohortRepository cohortRepository;

    @Mock
    private CohortMemberRepository cohortMemberRepository;

    @Mock
    private DiscountService discountService;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private CartServiceImpl cartService;

    private User sampleUser;
    private Course sampleCourse;
    private Cohort sampleCohort;
    private CartItem sampleCourseCartItem;
    private CartItem sampleCohortCartItem;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .username("student_tien")
                .email("student_tien@utelearn.edu.vn")
                .build();

        sampleCourse = Course.builder()
                .id(10L)
                .title("Lập trình Web nâng cao")
                .slug("web-adv")
                .price(new BigDecimal("500000"))
                .build();

        sampleCohort = Cohort.builder()
                .id(101L)
                .name("Chuyên đề Spring Boot")
                .code("SPRING-01")
                .price(new BigDecimal("200000"))
                .course(sampleCourse)
                .build();

        sampleCourseCartItem = CartItem.builder()
                .id(501L)
                .user(sampleUser)
                .itemType("COURSE")
                .course(sampleCourse)
                .build();

        sampleCohortCartItem = CartItem.builder()
                .id(502L)
                .user(sampleUser)
                .itemType("COHORT")
                .cohort(sampleCohort)
                .build();
    }

    @Test
    void testGetCartItems_ReturnsUserItems() {
        when(cartItemRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(sampleCourseCartItem, sampleCohortCartItem));

        List<CartItem> items = cartService.getCartItems(sampleUser);

        assertEquals(2, items.size());
        verify(cartItemRepository, times(1)).findByUserIdOrderByCreatedAtDesc(1L);
    }

    @Test
    void testGetCartItemCount_ReturnsCount() {
        when(cartItemRepository.countByUserId(1L)).thenReturn(2L);

        long count = cartService.getCartItemCount(sampleUser);

        assertEquals(2L, count);
    }

    @Test
    void testAddToCart_Course_Success() {
        when(courseRepository.findById(10L)).thenReturn(Optional.of(sampleCourse));
        when(cohortRepository.findByCourseIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(sampleCohort));
        when(cohortMemberRepository.existsByCohort_IdAndUser_Id(101L, 1L)).thenReturn(false);
        when(cartItemRepository.findByUserIdAndCourseId(1L, 10L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CartItem item = cartService.addToCart(sampleUser, "COURSE", 10L);

        assertNotNull(item);
        assertEquals("COURSE", item.getItemType());
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
    }

    @Test
    void testAddToCart_Course_AlreadyEnrolled_ThrowsException() {
        when(courseRepository.findById(10L)).thenReturn(Optional.of(sampleCourse));
        when(cohortRepository.findByCourseIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(sampleCohort));
        when(cohortMemberRepository.existsByCohort_IdAndUser_Id(101L, 1L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> cartService.addToCart(sampleUser, "COURSE", 10L));
    }

    @Test
    void testAddToCart_Cohort_Success() {
        when(cohortRepository.findById(101L)).thenReturn(Optional.of(sampleCohort));
        when(cohortMemberRepository.existsByCohort_IdAndUser_Id(101L, 1L)).thenReturn(false);
        when(cartItemRepository.findByUserIdAndCohortId(1L, 101L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CartItem item = cartService.addToCart(sampleUser, "COHORT", 101L);

        assertNotNull(item);
        assertEquals("COHORT", item.getItemType());
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
    }

    @Test
    void testAddToCart_ExistingInCart_ReturnsExisting() {
        when(courseRepository.findById(10L)).thenReturn(Optional.of(sampleCourse));
        when(cohortRepository.findByCourseIdOrderByCreatedAtAsc(10L)).thenReturn(Collections.emptyList());
        when(cartItemRepository.findByUserIdAndCourseId(1L, 10L)).thenReturn(Optional.of(sampleCourseCartItem));

        CartItem item = cartService.addToCart(sampleUser, "COURSE", 10L);

        assertEquals(501L, item.getId());
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    void testRemoveFromCart_Success() {
        when(cartItemRepository.findById(501L)).thenReturn(Optional.of(sampleCourseCartItem));

        cartService.removeFromCart(sampleUser, 501L);

        verify(cartItemRepository, times(1)).delete(sampleCourseCartItem);
    }

    @Test
    void testClearCart_Success() {
        cartService.clearCart(sampleUser);

        verify(cartItemRepository, times(1)).deleteByUserId(1L);
    }

    @Test
    void testGetCartSummary_WithoutDiscount() {
        when(cartItemRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(sampleCourseCartItem, sampleCohortCartItem));

        CartSummaryDTO summary = cartService.getCartSummary(sampleUser, null, null);

        assertNotNull(summary);
        assertEquals(2, summary.getItems().size());
        assertEquals(new BigDecimal("700000"), summary.getOriginalTotal());
        assertEquals(BigDecimal.ZERO, summary.getDiscountAmount());
        assertEquals(new BigDecimal("700000"), summary.getFinalTotal());
    }

    @Test
    void testGetCartSummary_WithValidDiscount() {
        Discount discount = Discount.builder()
                .code("GIAM20")
                .name("Giảm 20%")
                .discountType("PERCENTAGE")
                .discountValue(new BigDecimal("20"))
                .active(true)
                .build();

        when(cartItemRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(sampleCourseCartItem));
        when(discountService.findValidDiscountByCode(eq("GIAM20"), any())).thenReturn(Optional.of(discount));

        CartSummaryDTO summary = cartService.getCartSummary(sampleUser, List.of(501L), "GIAM20");

        assertNotNull(summary);
        assertEquals(new BigDecimal("500000"), summary.getOriginalTotal());
        assertEquals(new BigDecimal("100000.00"), summary.getDiscountAmount());
        assertEquals(new BigDecimal("400000.00"), summary.getFinalTotal());
        assertTrue(summary.isDiscountValid());
    }

    @Test
    void testCheckoutCart_Success_CreatesMultiItemOrderAndClearsItems() {
        when(cartItemRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(sampleCourseCartItem, sampleCohortCartItem));
        when(cohortRepository.findByCourseIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(sampleCohort));

        Order expectedOrder = Order.builder()
                .id(999L)
                .orderCode("UTE999111")
                .status("PENDING")
                .paymentMethod("VIETQR_SEPAY")
                .totalAmount(new BigDecimal("700000"))
                .build();

        when(orderService.createMultiItemOrder(eq(sampleUser), anyList(), isNull(), eq(BigDecimal.ZERO), eq("VIETQR_SEPAY"), any()))
                .thenReturn(expectedOrder);

        Order result = cartService.checkoutCart(sampleUser, List.of(501L, 502L), null, "VIETQR_SEPAY", "Đơn gộp");

        assertNotNull(result);
        assertEquals("UTE999111", result.getOrderCode());
        verify(orderService, times(1)).createMultiItemOrder(eq(sampleUser), anyList(), isNull(), eq(BigDecimal.ZERO), eq("VIETQR_SEPAY"), any());
        verify(cartItemRepository, times(1)).deleteAll(anyList());
    }
    @Test
    void testCheckoutCart_BundlesMultipleCohortsIntoCourse_Success() {
        Cohort cohort2 = Cohort.builder()
                .id(102L)
                .name("Chuyen de Microservices")
                .code("SPRING-02")
                .price(new BigDecimal("350000"))
                .course(sampleCourse)
                .build();

        CartItem cohortCartItem2 = CartItem.builder()
                .id(503L)
                .user(sampleUser)
                .itemType("COHORT")
                .cohort(cohort2)
                .build();

        when(cartItemRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(sampleCohortCartItem, cohortCartItem2));
        when(courseRepository.findById(10L))
                .thenReturn(Optional.of(sampleCourse));
        when(cohortRepository.findByCourseIdOrderByCreatedAtAsc(10L))
                .thenReturn(List.of(sampleCohort, cohort2));
        when(cohortMemberRepository.existsByCohort_IdAndUser_Id(anyLong(), eq(1L)))
                .thenReturn(false);

        Order bundledOrder = Order.builder()
                .id(1001L)
                .orderCode("UTE_BUNDLE_01")
                .status("PENDING")
                .paymentMethod("VIETQR_SEPAY")
                .totalAmount(new BigDecimal("500000"))
                .build();

        when(orderService.createMultiItemOrder(eq(sampleUser), anyList(), isNull(), eq(BigDecimal.ZERO), eq("VIETQR_SEPAY"), any()))
                .thenReturn(bundledOrder);

        Order result = cartService.checkoutCart(sampleUser, List.of(502L, 503L), null, "VIETQR_SEPAY", "Don gop 2 module thanh tron khoa");

        assertNotNull(result);
        assertEquals("UTE_BUNDLE_01", result.getOrderCode());
        assertEquals(new BigDecimal("500000"), result.getTotalAmount());
        verify(orderService, times(1)).createMultiItemOrder(eq(sampleUser), argThat(items -> items.size() == 1 && "COURSE".equals(items.get(0).getType())), isNull(), eq(BigDecimal.ZERO), eq("VIETQR_SEPAY"), any());
        verify(cartItemRepository, times(1)).deleteAll(anyList());
    }
}

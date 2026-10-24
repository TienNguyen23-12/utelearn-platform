package vn.edu.ute.utelearn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.ute.utelearn.dao.DiscountRepository;
import vn.edu.ute.utelearn.dto.DiscountDTO;
import vn.edu.ute.utelearn.entity.Discount;
import vn.edu.ute.utelearn.service.impl.DiscountServiceImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DiscountServiceTest {

    @Mock
    private DiscountRepository discountRepository;

    @InjectMocks
    private DiscountServiceImpl discountService;

    private Discount samplePercentageDiscount;
    private Discount sampleFixedDiscount;

    @BeforeEach
    void setUp() {
        samplePercentageDiscount = Discount.builder()
                .id(1L)
                .code("GIAM20")
                .name("Giảm 20% đơn hàng")
                .discountType("PERCENTAGE")
                .discountValue(new BigDecimal("20"))
                .maxDiscountAmount(new BigDecimal("100000"))
                .minOrderAmount(new BigDecimal("200000"))
                .usageLimit(100)
                .usedCount(10)
                .active(true)
                .build();

        sampleFixedDiscount = Discount.builder()
                .id(2L)
                .code("SALE50K")
                .name("Giảm 50.000 VNĐ")
                .discountType("FIXED_AMOUNT")
                .discountValue(new BigDecimal("50000"))
                .minOrderAmount(new BigDecimal("100000"))
                .usageLimit(50)
                .usedCount(5)
                .active(true)
                .build();
    }

    @Test
    void testGetAllDiscounts_ReturnsList() {
        when(discountRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(samplePercentageDiscount, sampleFixedDiscount));

        List<Discount> result = discountService.getAllDiscounts();

        assertEquals(2, result.size());
        verify(discountRepository, times(1)).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void testGetDiscountById_Found() {
        when(discountRepository.findById(1L)).thenReturn(Optional.of(samplePercentageDiscount));

        Optional<Discount> result = discountService.getDiscountById(1L);

        assertTrue(result.isPresent());
        assertEquals("GIAM20", result.get().getCode());
    }

    @Test
    void testFindValidDiscountByCode_Valid_ReturnsDiscount() {
        when(discountRepository.findByCodeIgnoreCase("GIAM20")).thenReturn(Optional.of(samplePercentageDiscount));

        Optional<Discount> result = discountService.findValidDiscountByCode("GIAM20", new BigDecimal("500000"));

        assertTrue(result.isPresent());
        assertEquals("GIAM20", result.get().getCode());
    }

    @Test
    void testFindValidDiscountByCode_UnderMinOrder_ReturnsEmpty() {
        when(discountRepository.findByCodeIgnoreCase("GIAM20")).thenReturn(Optional.of(samplePercentageDiscount));

        Optional<Discount> result = discountService.findValidDiscountByCode("GIAM20", new BigDecimal("100000"));

        assertTrue(result.isEmpty());
    }

    @Test
    void testFindValidDiscountByCode_Inactive_ReturnsEmpty() {
        samplePercentageDiscount.setActive(false);
        when(discountRepository.findByCodeIgnoreCase("GIAM20")).thenReturn(Optional.of(samplePercentageDiscount));

        Optional<Discount> result = discountService.findValidDiscountByCode("GIAM20", new BigDecimal("500000"));

        assertTrue(result.isEmpty());
    }

    @Test
    void testCalculateDiscount_PercentageWithMaxCap() {
        BigDecimal orderAmount = new BigDecimal("1000000");
        BigDecimal calculated = samplePercentageDiscount.calculateDiscount(orderAmount);

        assertEquals(new BigDecimal("100000"), calculated);
    }

    @Test
    void testCalculateDiscount_FixedAmount() {
        BigDecimal orderAmount = new BigDecimal("300000");
        BigDecimal calculated = sampleFixedDiscount.calculateDiscount(orderAmount);

        assertEquals(new BigDecimal("50000"), calculated);
    }

    @Test
    void testCalculateDiscount_CannotExceedOrderTotal() {
        Discount bigDiscount = Discount.builder()
                .code("BIG")
                .discountType("FIXED_AMOUNT")
                .discountValue(new BigDecimal("500000"))
                .active(true)
                .build();

        BigDecimal orderAmount = new BigDecimal("200000");
        BigDecimal calculated = bigDiscount.calculateDiscount(orderAmount);

        assertEquals(new BigDecimal("200000"), calculated);
    }

    @Test
    void testCreateDiscount_Success() {
        DiscountDTO dto = DiscountDTO.builder()
                .code("NEWYEAR")
                .name("Mừng năm mới")
                .discountType("PERCENTAGE")
                .discountValue(new BigDecimal("15"))
                .active(true)
                .build();

        when(discountRepository.existsByCodeIgnoreCase("NEWYEAR")).thenReturn(false);
        when(discountRepository.save(any(Discount.class))).thenAnswer(inv -> inv.getArgument(0));

        Discount created = discountService.createDiscount(dto, "admin");

        assertNotNull(created);
        assertEquals("NEWYEAR", created.getCode());
        assertEquals("PERCENTAGE", created.getDiscountType());
        verify(discountRepository, times(1)).save(any(Discount.class));
    }

    @Test
    void testCreateDiscount_DuplicateCode_ThrowsException() {
        DiscountDTO dto = DiscountDTO.builder()
                .code("GIAM20")
                .discountValue(new BigDecimal("20"))
                .build();

        when(discountRepository.existsByCodeIgnoreCase("GIAM20")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> discountService.createDiscount(dto, "admin"));
    }

    @Test
    void testCreateDiscount_PercentageOver100_ThrowsException() {
        DiscountDTO dto = DiscountDTO.builder()
                .code("OVER100")
                .discountType("PERCENTAGE")
                .discountValue(new BigDecimal("105"))
                .build();

        when(discountRepository.existsByCodeIgnoreCase("OVER100")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> discountService.createDiscount(dto, "admin"));
    }

    @Test
    void testToggleActive_FlipsState() {
        when(discountRepository.findById(1L)).thenReturn(Optional.of(samplePercentageDiscount));
        when(discountRepository.save(any(Discount.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean state = discountService.toggleActive(1L);

        assertFalse(state);
        verify(discountRepository, times(1)).save(samplePercentageDiscount);
    }

    @Test
    void testIncrementUsage_IncreasesUsedCount() {
        when(discountRepository.findByCodeIgnoreCase("GIAM20")).thenReturn(Optional.of(samplePercentageDiscount));
        when(discountRepository.save(any(Discount.class))).thenAnswer(inv -> inv.getArgument(0));

        discountService.incrementUsage("GIAM20");

        assertEquals(11, samplePercentageDiscount.getUsedCount());
        verify(discountRepository, times(1)).save(samplePercentageDiscount);
    }

    @Test
    void testDeleteDiscount_Success() {
        when(discountRepository.existsById(1L)).thenReturn(true);

        discountService.deleteDiscount(1L);

        verify(discountRepository, times(1)).deleteById(1L);
    }
}

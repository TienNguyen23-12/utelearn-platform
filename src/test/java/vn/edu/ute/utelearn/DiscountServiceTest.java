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
    void testCreateDiscount_Success() {
        DiscountDTO dto = DiscountDTO.builder()
                .code("NEWCODE")
                .name("Mã mới")
                .discountType("PERCENTAGE")
                .discountValue(new BigDecimal("10"))
                .maxDiscountAmount(new BigDecimal("50000"))
                .minOrderAmount(BigDecimal.ZERO)
                .usageLimit(20)
                .active(true)
                .build();

        when(discountRepository.existsByCodeIgnoreCase("NEWCODE")).thenReturn(false);
        when(discountRepository.save(any(Discount.class))).thenAnswer(inv -> inv.getArgument(0));

        Discount created = discountService.createDiscount(dto, "admin");

        assertNotNull(created);
        assertEquals("NEWCODE", created.getCode());
        verify(discountRepository, times(1)).save(any(Discount.class));
    }

    @Test
    void testCreateDiscount_DuplicateCode_ThrowsException() {
        DiscountDTO dto = DiscountDTO.builder()
                .code("GIAM20")
                .discountValue(new BigDecimal("10"))
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
        when(discountRepository.incrementUsageAtomic("GIAM20")).thenReturn(1);

        discountService.incrementUsage("GIAM20");

        verify(discountRepository, times(1)).incrementUsageAtomic("GIAM20");
    }

    @Test
    void testIncrementUsage_ExceededLimit_ThrowsException() {
        when(discountRepository.incrementUsageAtomic("GIAM20")).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> discountService.incrementUsage("GIAM20"));
        verify(discountRepository, times(1)).incrementUsageAtomic("GIAM20");
    }

    @Test
    void testDeleteDiscount_Success() {
        when(discountRepository.existsById(1L)).thenReturn(true);

        discountService.deleteDiscount(1L);

        verify(discountRepository, times(1)).deleteById(1L);
    }
}

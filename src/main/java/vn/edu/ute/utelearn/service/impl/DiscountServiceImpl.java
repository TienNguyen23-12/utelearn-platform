package vn.edu.ute.utelearn.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.utelearn.dao.DiscountRepository;
import vn.edu.ute.utelearn.dto.DiscountDTO;
import vn.edu.ute.utelearn.entity.Discount;
import vn.edu.ute.utelearn.service.DiscountService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountServiceImpl implements DiscountService {

    private final DiscountRepository discountRepository;

    @Override
    public List<Discount> getAllDiscounts() {
        return discountRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public Optional<Discount> getDiscountById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return discountRepository.findById(id);
    }

    @Override
    public Optional<Discount> findValidDiscountByCode(String code, BigDecimal currentAmount) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        Optional<Discount> discountOpt = discountRepository.findByCodeIgnoreCase(code.trim());
        if (discountOpt.isEmpty()) {
            return Optional.empty();
        }
        Discount discount = discountOpt.get();
        if (!discount.isValid(currentAmount)) {
            return Optional.empty();
        }
        return Optional.of(discount);
    }

    @Override
    @Transactional
    public Discount createDiscount(DiscountDTO dto, String createdBy) {
        if (dto.getCode() == null || dto.getCode().isBlank()) {
            throw new IllegalArgumentException("Mã giảm giá không được để trống.");
        }
        String cleanCode = dto.getCode().trim().toUpperCase();
        if (discountRepository.existsByCodeIgnoreCase(cleanCode)) {
            throw new IllegalArgumentException("Mã giảm giá '" + cleanCode + "' đã tồn tại.");
        }
        if (dto.getDiscountValue() == null || dto.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá trị giảm giá phải lớn hơn 0.");
        }

        String type = (dto.getDiscountType() != null && !dto.getDiscountType().isBlank())
                ? dto.getDiscountType().trim().toUpperCase()
                : "PERCENTAGE";

        if ("PERCENTAGE".equalsIgnoreCase(type) && dto.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Tỷ lệ giảm giá theo phần trăm không được vượt quá 100%.");
        }

        Instant startInstant = parseDateString(dto.getStartDateStr());
        Instant endInstant = parseDateString(dto.getEndDateStr());

        if (startInstant != null && endInstant != null && endInstant.isBefore(startInstant)) {
            throw new IllegalArgumentException("Ngày kết thúc không được trước ngày bắt đầu.");
        }

        Discount discount = Discount.builder()
                .code(cleanCode)
                .name(dto.getName() != null && !dto.getName().isBlank() ? dto.getName().trim() : cleanCode)
                .discountType(type)
                .discountValue(dto.getDiscountValue())
                .maxDiscountAmount(dto.getMaxDiscountAmount())
                .minOrderAmount(dto.getMinOrderAmount() != null ? dto.getMinOrderAmount() : BigDecimal.ZERO)
                .usageLimit(dto.getUsageLimit())
                .usedCount(0)
                .startDate(startInstant)
                .endDate(endInstant)
                .active(dto.isActive())
                .createdBy(createdBy)
                .build();

        return discountRepository.save(discount);
    }

    @Override
    @Transactional
    public Discount updateDiscount(Long id, DiscountDTO dto) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã giảm giá ID: " + id));

        if (dto.getCode() != null && !dto.getCode().isBlank()) {
            String cleanCode = dto.getCode().trim().toUpperCase();
            if (!cleanCode.equalsIgnoreCase(discount.getCode()) && discountRepository.existsByCodeIgnoreCase(cleanCode)) {
                throw new IllegalArgumentException("Mã giảm giá '" + cleanCode + "' đã tồn tại.");
            }
            discount.setCode(cleanCode);
        }

        if (dto.getName() != null && !dto.getName().isBlank()) {
            discount.setName(dto.getName().trim());
        }

        if (dto.getDiscountType() != null && !dto.getDiscountType().isBlank()) {
            discount.setDiscountType(dto.getDiscountType().trim().toUpperCase());
        }

        if (dto.getDiscountValue() != null) {
            if (dto.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Giá trị giảm giá phải lớn hơn 0.");
            }
            if ("PERCENTAGE".equalsIgnoreCase(discount.getDiscountType()) && dto.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Tỷ lệ giảm giá theo phần trăm không được vượt quá 100%.");
            }
            discount.setDiscountValue(dto.getDiscountValue());
        }

        discount.setMaxDiscountAmount(dto.getMaxDiscountAmount());
        discount.setMinOrderAmount(dto.getMinOrderAmount() != null ? dto.getMinOrderAmount() : BigDecimal.ZERO);
        discount.setUsageLimit(dto.getUsageLimit());

        if (dto.getStartDateStr() != null) {
            discount.setStartDate(parseDateString(dto.getStartDateStr()));
        }
        if (dto.getEndDateStr() != null) {
            discount.setEndDate(parseDateString(dto.getEndDateStr()));
        }

        if (discount.getStartDate() != null && discount.getEndDate() != null && discount.getEndDate().isBefore(discount.getStartDate())) {
            throw new IllegalArgumentException("Ngày kết thúc không được trước ngày bắt đầu.");
        }

        discount.setActive(dto.isActive());
        return discountRepository.save(discount);
    }

    @Override
    @Transactional
    public void deleteDiscount(Long id) {
        if (!discountRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy mã giảm giá ID: " + id);
        }
        discountRepository.deleteById(id);
    }

    @Override
    @Transactional
    public boolean toggleActive(Long id) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã giảm giá ID: " + id));
        discount.setActive(!discount.isActive());
        discountRepository.save(discount);
        return discount.isActive();
    }

    @Override
    @Transactional
    public void incrementUsage(String code) {
        if (code == null || code.isBlank()) {
            return;
        }
        discountRepository.findByCodeIgnoreCase(code.trim()).ifPresent(d -> {
            d.setUsedCount((d.getUsedCount() != null ? d.getUsedCount() : 0) + 1);
            discountRepository.save(d);
        });
    }

    private Instant parseDateString(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(dateStr.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            return ldt.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        } catch (Exception e1) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(dateStr.trim() + "T00:00");
                return ldt.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
            } catch (Exception e2) {
                return null;
            }
        }
    }
}

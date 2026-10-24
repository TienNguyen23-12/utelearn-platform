package vn.edu.ute.utelearn.service;

import vn.edu.ute.utelearn.dto.DiscountDTO;
import vn.edu.ute.utelearn.entity.Discount;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface DiscountService {

    List<Discount> getAllDiscounts();

    Optional<Discount> getDiscountById(Long id);

    Optional<Discount> findValidDiscountByCode(String code, BigDecimal currentAmount);

    Discount createDiscount(DiscountDTO dto, String createdBy);

    Discount updateDiscount(Long id, DiscountDTO dto);

    void deleteDiscount(Long id);

    boolean toggleActive(Long id);

    void incrementUsage(String code);
}

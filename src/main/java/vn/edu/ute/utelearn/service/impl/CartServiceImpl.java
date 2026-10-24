package vn.edu.ute.utelearn.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.utelearn.dao.CartItemRepository;
import vn.edu.ute.utelearn.dao.CohortMemberRepository;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.CourseRepository;
import vn.edu.ute.utelearn.dto.CartSummaryDTO;
import vn.edu.ute.utelearn.dto.OrderItemDTO;
import vn.edu.ute.utelearn.entity.*;
import vn.edu.ute.utelearn.service.CartService;
import vn.edu.ute.utelearn.service.DiscountService;
import vn.edu.ute.utelearn.service.OrderService;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final CourseRepository courseRepository;
    private final CohortRepository cohortRepository;
    private final CohortMemberRepository cohortMemberRepository;
    private final DiscountService discountService;
    private final OrderService orderService;

    @Override
    public List<CartItem> getCartItems(User user) {
        if (user == null) {
            return Collections.emptyList();
        }
        return cartItemRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Override
    public long getCartItemCount(User user) {
        if (user == null) {
            return 0;
        }
        return cartItemRepository.countByUserId(user.getId());
    }

    @Override
    @Transactional
    public CartItem addToCart(User user, String itemType, Long itemId) {
        if (user == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để thêm vào giỏ hàng.");
        }
        if (itemType == null || itemId == null) {
            throw new IllegalArgumentException("Thông tin sản phẩm không hợp lệ.");
        }

        String type = itemType.trim().toUpperCase();

        if ("COURSE".equals(type)) {
            Course course = courseRepository.findById(itemId)
                    .orElseThrow(() -> new IllegalArgumentException("Khóa học không tồn tại."));

            List<Cohort> cohorts = cohortRepository.findByCourseIdOrderByCreatedAtAsc(course.getId());
            boolean isAllEnrolled = !cohorts.isEmpty() && cohorts.stream()
                    .allMatch(c -> cohortMemberRepository.existsByCohort_IdAndUser_Id(c.getId(), user.getId()));
            if (isAllEnrolled) {
                throw new IllegalStateException("Bạn đã sở hữu toàn bộ khóa học này rồi.");
            }

            Optional<CartItem> existing = cartItemRepository.findByUserIdAndCourseId(user.getId(), course.getId());
            if (existing.isPresent()) {
                return existing.get();
            }

            CartItem cartItem = CartItem.builder()
                    .user(user)
                    .itemType("COURSE")
                    .course(course)
                    .build();
            return cartItemRepository.save(cartItem);
        }

        if ("COHORT".equals(type)) {
            Cohort cohort = cohortRepository.findById(itemId)
                    .orElseThrow(() -> new IllegalArgumentException("Module không tồn tại."));

            if (cohortMemberRepository.existsByCohort_IdAndUser_Id(cohort.getId(), user.getId())) {
                throw new IllegalStateException("Bạn đã ghi danh vào module này rồi.");
            }

            Optional<CartItem> existing = cartItemRepository.findByUserIdAndCohortId(user.getId(), cohort.getId());
            if (existing.isPresent()) {
                return existing.get();
            }

            CartItem cartItem = CartItem.builder()
                    .user(user)
                    .itemType("COHORT")
                    .cohort(cohort)
                    .build();
            return cartItemRepository.save(cartItem);
        }

        throw new IllegalArgumentException("Loại sản phẩm không hỗ trợ: " + itemType);
    }

    @Override
    @Transactional
    public void removeFromCart(User user, Long cartItemId) {
        if (user == null || cartItemId == null) {
            return;
        }
        cartItemRepository.findById(cartItemId).ifPresent(item -> {
            if (item.getUser() != null && item.getUser().getId().equals(user.getId())) {
                cartItemRepository.delete(item);
            }
        });
    }

    @Override
    @Transactional
    public void clearCart(User user) {
        if (user == null) {
            return;
        }
        cartItemRepository.deleteByUserId(user.getId());
    }

    @Override
    public CartSummaryDTO getCartSummary(User user, List<Long> selectedItemIds, String discountCode) {
        if (user == null) {
            return CartSummaryDTO.builder().build();
        }

        List<CartItem> allItems = cartItemRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        Set<Long> selectedSet = (selectedItemIds != null && !selectedItemIds.isEmpty())
                ? new HashSet<>(selectedItemIds)
                : allItems.stream().map(CartItem::getId).collect(Collectors.toSet());

        List<CartSummaryDTO.CartItemDTO> itemDTOs = new ArrayList<>();
        BigDecimal originalTotal = BigDecimal.ZERO;

        for (CartItem item : allItems) {
            boolean isSelected = selectedSet.contains(item.getId());
            BigDecimal price = item.getPrice();

            if (isSelected) {
                originalTotal = originalTotal.add(price != null ? price : BigDecimal.ZERO);
            }

            Long specificItemId = "COURSE".equalsIgnoreCase(item.getItemType()) && item.getCourse() != null
                    ? item.getCourse().getId()
                    : (item.getCohort() != null ? item.getCohort().getId() : null);

            itemDTOs.add(CartSummaryDTO.CartItemDTO.builder()
                    .id(item.getId())
                    .itemType(item.getItemType())
                    .itemId(specificItemId)
                    .title(item.getName())
                    .codeOrSlug(item.getSlugOrCode())
                    .price(price)
                    .formattedPrice(item.getFormattedPrice())
                    .imageUrl(item.getImageUrl())
                    .selected(isSelected)
                    .build());
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        String discountMsg = null;
        boolean discountValid = false;

        if (discountCode != null && !discountCode.isBlank()) {
            Optional<Discount> discountOpt = discountService.findValidDiscountByCode(discountCode.trim(), originalTotal);
            if (discountOpt.isPresent()) {
                Discount discount = discountOpt.get();
                discountAmount = discount.calculateDiscount(originalTotal);
                discountMsg = "Áp dụng mã '" + discount.getCode() + "' thành công: " + discount.getName();
                discountValid = true;
            } else {
                discountMsg = "Mã giảm giá '" + discountCode.trim().toUpperCase() + "' không hợp lệ, đã hết hạn hoặc chưa đạt giá trị đơn tối thiểu.";
            }
        }

        BigDecimal finalTotal = originalTotal.subtract(discountAmount);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        return CartSummaryDTO.builder()
                .items(itemDTOs)
                .originalTotal(originalTotal)
                .discountAmount(discountAmount)
                .finalTotal(finalTotal)
                .discountCode(discountValid ? discountCode.trim().toUpperCase() : null)
                .discountMessage(discountMsg)
                .discountValid(discountValid)
                .build();
    }

    @Override
    @Transactional
    public Order checkoutCart(User user, List<Long> selectedItemIds, String discountCode, String paymentMethod, String note) {
        if (user == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để thanh toán.");
        }

        List<CartItem> allItems = cartItemRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        Set<Long> selectedSet = (selectedItemIds != null && !selectedItemIds.isEmpty())
                ? new HashSet<>(selectedItemIds)
                : allItems.stream().map(CartItem::getId).collect(Collectors.toSet());

        List<CartItem> selectedItems = allItems.stream()
                .filter(item -> selectedSet.contains(item.getId()))
                .collect(Collectors.toList());

        if (selectedItems.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một sản phẩm trong giỏ hàng để tạo đơn hàng.");
        }

        List<OrderItemDTO> orderItems = new ArrayList<>();
        BigDecimal originalTotal = BigDecimal.ZERO;

        for (CartItem item : selectedItems) {
            BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
            originalTotal = originalTotal.add(price);

            if ("COURSE".equalsIgnoreCase(item.getItemType()) && item.getCourse() != null) {
                Course course = item.getCourse();
                List<Cohort> availableCohorts = cohortRepository.findByCourseIdOrderByCreatedAtAsc(course.getId());
                List<Long> cohortIds = availableCohorts.stream().map(Cohort::getId).collect(Collectors.toList());

                orderItems.add(OrderItemDTO.builder()
                        .type("COURSE")
                        .courseId(course.getId())
                        .courseSlug(course.getSlug())
                        .name("Khóa học: " + course.getTitle())
                        .price(price)
                        .isUpgrade(false)
                        .cohortIds(cohortIds)
                        .build());
            } else if ("COHORT".equalsIgnoreCase(item.getItemType()) && item.getCohort() != null) {
                Cohort cohort = item.getCohort();
                orderItems.add(OrderItemDTO.builder()
                        .type("COHORT")
                        .courseId(cohort.getCourse() != null ? cohort.getCourse().getId() : null)
                        .courseSlug(cohort.getCourse() != null ? cohort.getCourse().getSlug() : null)
                        .cohortId(cohort.getId())
                        .cohortCode(cohort.getCode())
                        .name("Module: " + cohort.getName())
                        .price(price)
                        .isUpgrade(false)
                        .cohortIds(List.of(cohort.getId()))
                        .build());
            }
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        String validDiscountCode = null;

        if (discountCode != null && !discountCode.isBlank()) {
            Optional<Discount> discountOpt = discountService.findValidDiscountByCode(discountCode.trim(), originalTotal);
            if (discountOpt.isPresent()) {
                Discount discount = discountOpt.get();
                discountAmount = discount.calculateDiscount(originalTotal);
                validDiscountCode = discount.getCode();
                discountService.incrementUsage(validDiscountCode);
            }
        }

        Order order = orderService.createMultiItemOrder(user, orderItems, validDiscountCode, discountAmount, paymentMethod, note);
        cartItemRepository.deleteAll(selectedItems);
        return order;
    }
}

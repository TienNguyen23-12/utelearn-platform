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
    @Transactional(readOnly = true)
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

            if (course.getCreatedBy() != null && course.getCreatedBy().getId().equals(user.getId())) {
                throw new IllegalStateException("Bạn là giảng viên của khóa học này nên không thể tự mua khóa học của chính mình.");
            }

            List<Cohort> cohorts = cohortRepository.findByCourseIdOrderByCreatedAtAsc(course.getId());
            boolean isAllEnrolled = !cohorts.isEmpty() && cohorts.stream()
                    .allMatch(c -> cohortMemberRepository.existsByCohort_IdAndUser_Id(c.getId(), user.getId()));
            if (isAllEnrolled) {
                throw new IllegalStateException("Bạn đã sở hữu toàn bộ khóa học này rồi.");
            }

            List<CartItem> userCohortsInCart = cartItemRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                    .filter(ci -> "COHORT".equalsIgnoreCase(ci.getItemType()) && ci.getCohort() != null && ci.getCohort().getCourse() != null && ci.getCohort().getCourse().getId().equals(course.getId()))
                    .toList();
            for (CartItem ci : userCohortsInCart) {
                cartItemRepository.delete(ci);
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

            boolean isOwner = (cohort.getCreatedBy() != null && cohort.getCreatedBy().getId().equals(user.getId()))
                    || (cohort.getCourse() != null && cohort.getCourse().getCreatedBy() != null && cohort.getCourse().getCreatedBy().getId().equals(user.getId()));
            if (isOwner) {
                throw new IllegalStateException("Bạn là giảng viên của module này nên không thể tự mua sản phẩm của chính mình.");
            }

            if (cohortMemberRepository.existsByCohort_IdAndUser_Id(cohort.getId(), user.getId())) {
                throw new IllegalStateException("Bạn đã ghi danh vào module này rồi.");
            }

            if (cohort.getCourse() != null) {
                List<Cohort> courseCohorts = cohortRepository.findByCourseIdOrderByCreatedAtAsc(cohort.getCourse().getId());
                boolean isCourseEnrolled = !courseCohorts.isEmpty() && courseCohorts.stream()
                        .allMatch(c -> cohortMemberRepository.existsByCohort_IdAndUser_Id(c.getId(), user.getId()));
                if (isCourseEnrolled) {
                    throw new IllegalStateException("Bạn đã sở hữu toàn bộ khóa học '" + cohort.getCourse().getTitle() + "' chứa module này rồi.");
                }
            }

            if (cohort.getCourse() != null) {
                boolean hasCourseInCart = cartItemRepository.findByUserIdAndCourseId(user.getId(), cohort.getCourse().getId()).isPresent();
                if (hasCourseInCart) {
                    throw new IllegalStateException("Khóa học trọn gói '" + cohort.getCourse().getTitle() + "' chứa module này đã có trong giỏ hàng của bạn rồi. Không cần thêm module lẻ.");
                }
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
    @Transactional(readOnly = true)
    public CartSummaryDTO getCartSummary(User user, List<Long> selectedItemIds, String discountCode) {
        if (user == null) {
            return CartSummaryDTO.builder().build();
        }

        List<CartItem> rawItems = cartItemRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<CartItem> allItems = new ArrayList<>();

        Set<Long> courseIdsInCart = rawItems.stream()
                .filter(i -> "COURSE".equalsIgnoreCase(i.getItemType()) && i.getCourse() != null)
                .map(i -> i.getCourse().getId())
                .collect(Collectors.toSet());

        for (CartItem item : rawItems) {
            boolean alreadyEnrolled = false;
            if ("COURSE".equalsIgnoreCase(item.getItemType()) && item.getCourse() != null) {
                List<Cohort> cohorts = cohortRepository.findByCourseIdOrderByCreatedAtAsc(item.getCourse().getId());
                alreadyEnrolled = !cohorts.isEmpty() && cohorts.stream()
                        .allMatch(c -> cohortMemberRepository.existsByCohort_IdAndUser_Id(c.getId(), user.getId()));
            } else if ("COHORT".equalsIgnoreCase(item.getItemType()) && item.getCohort() != null) {
                alreadyEnrolled = cohortMemberRepository.existsByCohort_IdAndUser_Id(item.getCohort().getId(), user.getId());
            }
            if (alreadyEnrolled) {
                cartItemRepository.delete(item);
            } else {
                allItems.add(item);
            }
        }

        Set<Long> selectedSet = (selectedItemIds != null && !selectedItemIds.isEmpty())
                ? new HashSet<>(selectedItemIds)
                : allItems.stream().map(CartItem::getId).collect(Collectors.toSet());

        Map<Long, List<CartItem>> courseIdToCohortItems = new HashMap<>();
        for (CartItem item : allItems) {
            if (selectedSet.contains(item.getId()) && "COHORT".equalsIgnoreCase(item.getItemType()) && item.getCohort() != null && item.getCohort().getCourse() != null) {
                courseIdToCohortItems.computeIfAbsent(item.getCohort().getCourse().getId(), k -> new ArrayList<>()).add(item);
            }
        }

        BigDecimal totalBundleSavings = BigDecimal.ZERO;
        List<String> bundleMessages = new ArrayList<>();
        Map<Long, String> itemBundleNotes = new HashMap<>();

        for (Map.Entry<Long, List<CartItem>> entry : courseIdToCohortItems.entrySet()) {
            Long courseId = entry.getKey();
            List<CartItem> cohortItemsInCart = entry.getValue();
            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null) continue;

            BigDecimal coursePrice = course.getPrice();
            if (coursePrice == null || coursePrice.compareTo(BigDecimal.ZERO) <= 0) continue;

            List<Cohort> allCohortsOfCourse = cohortRepository.findByCourseIdOrderByCreatedAtAsc(courseId);
            List<Cohort> unEnrolledCohorts = allCohortsOfCourse.stream()
                    .filter(c -> !cohortMemberRepository.existsByCohort_IdAndUser_Id(c.getId(), user.getId()))
                    .collect(Collectors.toList());

            BigDecimal sumOfCohorts = cohortItemsInCart.stream()
                    .map(CartItem::getPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            boolean formsFullCourse = (cohortItemsInCart.size() >= unEnrolledCohorts.size() && !unEnrolledCohorts.isEmpty())
                    || (sumOfCohorts.compareTo(coursePrice) >= 0);

            if (formsFullCourse && sumOfCohorts.compareTo(coursePrice) > 0) {
                BigDecimal savings = sumOfCohorts.subtract(coursePrice);
                totalBundleSavings = totalBundleSavings.add(savings);
                String note = "Gói trọn bộ: Tiết kiệm " + java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(savings) + " VNĐ";
                for (CartItem ci : cohortItemsInCart) {
                    itemBundleNotes.put(ci.getId(), note);
                }
                bundleMessages.add("Đã gom " + cohortItemsInCart.size() + " module thành gói trọn khóa '" + course.getTitle() + "': " + java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(coursePrice) + " VNĐ (Tiết kiệm " + java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(savings) + " VNĐ)");
            }
        }

        List<CartSummaryDTO.CartItemDTO> itemDTOs = new ArrayList<>();
        BigDecimal originalTotal = BigDecimal.ZERO;

        for (CartItem item : allItems) {
            boolean isSelected = selectedSet.contains(item.getId());
            BigDecimal price = item.getPrice();
            if ("COURSE".equalsIgnoreCase(item.getItemType()) && item.getCourse() != null && (price == null || price.compareTo(BigDecimal.ZERO) <= 0)) {
                List<Cohort> cohorts = cohortRepository.findByCourseIdOrderByCreatedAtAsc(item.getCourse().getId());
                price = cohorts.stream().map(Cohort::getPrice).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            }

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
                    .bundleNote(itemBundleNotes.get(item.getId()))
                    .build());
        }

        BigDecimal subtotalAfterBundle = originalTotal.subtract(totalBundleSavings);
        if (subtotalAfterBundle.compareTo(BigDecimal.ZERO) < 0) {
            subtotalAfterBundle = BigDecimal.ZERO;
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        String discountMsg = null;
        boolean discountValid = false;

        if (discountCode != null && !discountCode.isBlank()) {
            Optional<Discount> discountOpt = discountService.findValidDiscountByCode(discountCode.trim(), subtotalAfterBundle);
            if (discountOpt.isPresent()) {
                Discount discount = discountOpt.get();
                discountAmount = discount.calculateDiscount(subtotalAfterBundle);
                discountMsg = "Áp dụng mã '" + discount.getCode() + "' thành công: " + discount.getName();
                discountValid = true;
            } else {
                discountMsg = "Mã giảm giá '" + discountCode.trim().toUpperCase() + "' không hợp lệ, đã hết hạn hoặc chưa đạt giá trị đơn tối thiểu.";
            }
        }

        BigDecimal finalTotal = subtotalAfterBundle.subtract(discountAmount);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        String finalBundleMessage = !bundleMessages.isEmpty() ? String.join(" | ", bundleMessages) : null;

        return CartSummaryDTO.builder()
                .items(itemDTOs)
                .originalTotal(originalTotal)
                .bundleSavings(totalBundleSavings)
                .discountAmount(discountAmount)
                .finalTotal(finalTotal)
                .bundleMessage(finalBundleMessage)
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

        Map<Long, List<CartItem>> courseIdToCohortItems = new HashMap<>();
        Set<Long> bundledCohortItemIds = new HashSet<>();
        List<OrderItemDTO> orderItems = new ArrayList<>();

        for (CartItem item : selectedItems) {
            if ("COHORT".equalsIgnoreCase(item.getItemType()) && item.getCohort() != null && item.getCohort().getCourse() != null) {
                courseIdToCohortItems.computeIfAbsent(item.getCohort().getCourse().getId(), k -> new ArrayList<>()).add(item);
            }
        }

        for (Map.Entry<Long, List<CartItem>> entry : courseIdToCohortItems.entrySet()) {
            Long courseId = entry.getKey();
            List<CartItem> cohortItemsInCart = entry.getValue();
            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null) continue;

            BigDecimal coursePrice = course.getPrice();
            if (coursePrice == null || coursePrice.compareTo(BigDecimal.ZERO) <= 0) continue;

            List<Cohort> allCohortsOfCourse = cohortRepository.findByCourseIdOrderByCreatedAtAsc(courseId);
            List<Cohort> unEnrolledCohorts = allCohortsOfCourse.stream()
                    .filter(c -> !cohortMemberRepository.existsByCohort_IdAndUser_Id(c.getId(), user.getId()))
                    .collect(Collectors.toList());

            BigDecimal sumOfCohorts = cohortItemsInCart.stream()
                    .map(CartItem::getPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            boolean formsFullCourse = (cohortItemsInCart.size() >= unEnrolledCohorts.size() && !unEnrolledCohorts.isEmpty())
                    || (sumOfCohorts.compareTo(coursePrice) >= 0);

            if (formsFullCourse && sumOfCohorts.compareTo(coursePrice) > 0) {
                List<Long> cohortIds = cohortItemsInCart.stream().map(ci -> ci.getCohort().getId()).collect(Collectors.toList());
                for (CartItem ci : cohortItemsInCart) {
                    bundledCohortItemIds.add(ci.getId());
                }
                orderItems.add(OrderItemDTO.builder()
                        .type("COURSE")
                        .courseId(course.getId())
                        .courseSlug(course.getSlug())
                        .name("Khóa học: " + course.getTitle() + " (Ưu đãi theo lộ trình)")
                        .price(coursePrice)
                        .isUpgrade(false)
                        .cohortIds(cohortIds)
                        .build());
            }
        }

        for (CartItem item : selectedItems) {
            if (bundledCohortItemIds.contains(item.getId())) {
                continue;
            }
            BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
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

        BigDecimal originalTotal = orderItems.stream()
                .map(OrderItemDTO::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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

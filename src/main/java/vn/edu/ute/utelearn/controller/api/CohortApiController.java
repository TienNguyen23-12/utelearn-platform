package vn.edu.ute.utelearn.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.service.CohortService;
import vn.edu.ute.utelearn.util.ValidationUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/cohorts")
@RequiredArgsConstructor
public class CohortApiController {

    private final CohortService cohortService;
    private final vn.edu.ute.utelearn.dao.CohortMemberRepository cohortMemberRepository;
    private final vn.edu.ute.utelearn.dao.CohortRepository cohortRepository;
    private final vn.edu.ute.utelearn.dao.CartItemRepository cartItemRepository;
    private final vn.edu.ute.utelearn.service.AuthService authService;

    @PostMapping("/{moduleId}/last-lesson/{lessonId}")
    public ResponseEntity<Map<String, Object>> updateLastAccessedLesson(
            @PathVariable Long moduleId,
            @PathVariable Long lessonId
    ) {
        java.util.Optional<vn.edu.ute.utelearn.entity.User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }

        boolean updated = cohortService.updateLastAccessedLesson(moduleId, lessonId, currentUserOpt.get());
        Map<String, Object> response = new HashMap<>();
        response.put("success", updated);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{moduleId}/lessons/{lessonId}/toggle-complete")
    public ResponseEntity<Map<String, Object>> toggleLessonCompletion(
            @PathVariable Long moduleId,
            @PathVariable Long lessonId
    ) {
        java.util.Optional<vn.edu.ute.utelearn.entity.User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, Object> result = cohortService.toggleLessonCompletion(moduleId, lessonId, currentUserOpt.get());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{moduleId}/progress")
    public ResponseEntity<Map<String, Object>> getCohortProgress(@PathVariable Long moduleId) {
        java.util.Optional<vn.edu.ute.utelearn.entity.User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, Object> progress = cohortService.getCohortProgress(moduleId, currentUserOpt.get().getId());
        return ResponseEntity.ok(progress);
    }

    @GetMapping
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Map<String, Object>> getPublicCohorts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size
    ) {
        try {
            String safeQ = ValidationUtils.stripHtmlTags(q);
            String safeCategory = ValidationUtils.stripHtmlTags(category);

            Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
            Page<Cohort> cohortPage = cohortService.searchPublicCohorts(safeQ, safeCategory, pageable);
            java.util.Optional<vn.edu.ute.utelearn.entity.User> currentUserOpt = authService.getCurrentAuthenticatedUser();

            List<Map<String, Object>> cohorts = cohortPage.getContent().stream().map(cohort -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", cohort.getId());
                map.put("code", cohort.getCode());
                map.put("name", cohort.getName());
                map.put("status", cohort.getStatus());
                map.put("visibility", cohort.getVisibility() != null ? cohort.getVisibility() : "PUBLIC");
                java.math.BigDecimal cohortPrice = cohort.getPrice();
                if (cohort.getCourse() != null && cohort.getCourse().getPrice() != null && cohort.getCourse().getPrice().compareTo(java.math.BigDecimal.ZERO) == 0) {
                    cohortPrice = java.math.BigDecimal.ZERO;
                }
                map.put("price", cohortPrice);

                boolean isEnrolled = false;
                boolean isOwner = false;
                boolean isCourseEnrolled = false;
                boolean isCourseInCart = false;
                if (currentUserOpt.isPresent()) {
                    vn.edu.ute.utelearn.entity.User user = currentUserOpt.get();
                    isEnrolled = cohortMemberRepository.existsByCohort_IdAndUser_Id(cohort.getId(), user.getId());
                    isOwner = (cohort.getCreatedBy() != null && cohort.getCreatedBy().getId().equals(user.getId()))
                            || (cohort.getCourse() != null && cohort.getCourse().getCreatedBy() != null && cohort.getCourse().getCreatedBy().getId().equals(user.getId()));
                    if (cohort.getCourse() != null) {
                        List<Cohort> cCohorts = cohortRepository.findByCourseIdOrderByCreatedAtAsc(cohort.getCourse().getId());
                        isCourseEnrolled = !cCohorts.isEmpty() && cCohorts.stream()
                                .allMatch(c -> cohortMemberRepository.existsByCohort_IdAndUser_Id(c.getId(), user.getId()));
                        isCourseInCart = cartItemRepository.existsByUserIdAndCourseId(user.getId(), cohort.getCourse().getId());
                    }
                }
                map.put("isEnrolled", isEnrolled);
                map.put("isOwner", isOwner);
                map.put("isCourseEnrolled", isCourseEnrolled);
                map.put("isCourseInCart", isCourseInCart);
                map.put("currentEnrolled", cohortMemberRepository.countByCohort_Id(cohort.getId()));

                Double avgCohort = cohortMemberRepository.getAverageRatingByCohortId(cohort.getId());
                long countCohort = cohortMemberRepository.countRatingsByCohortId(cohort.getId());
                Double rating = (avgCohort != null && countCohort > 0) ? Math.round(avgCohort * 10.0) / 10.0 : null;
                map.put("rating", rating);
                map.put("ratingCount", countCohort);

                map.put("maxCapacity", cohort.getMaxCapacity());
                map.put("description", cohort.getDescription());
                map.put("objectives", cohort.getObjectives());
                
                String thumb = cohort.getThumbnailUrl();
                if (thumb == null || thumb.isBlank()) {
                    thumb = (cohort.getCourse() != null) ? cohort.getCourse().getThumbnailUrl() : null;
                }
                if (thumb == null || thumb.isBlank()) {
                    thumb = "/images/courses/default-cover.png";
                }
                map.put("thumbnailUrl", thumb);

                String instructorName = "Giảng viên";
                Long creatorId = null;
                if (cohort.getCreatedBy() != null) {
                    creatorId = cohort.getCreatedBy().getId();
                    String name = cohort.getCreatedBy().getFullName();
                    if (name == null || name.isBlank()) {
                        name = cohort.getCreatedBy().getUsername();
                    }
                    if (name != null && !name.isBlank()) {
                        instructorName = name;
                    }
                } else if (cohort.getCourse() != null && cohort.getCourse().getCreatedBy() != null) {
                    creatorId = cohort.getCourse().getCreatedBy().getId();
                    String name = cohort.getCourse().getCreatedBy().getFullName();
                    if (name == null || name.isBlank()) {
                        name = cohort.getCourse().getCreatedBy().getUsername();
                    }
                    if (name != null && !name.isBlank()) {
                        instructorName = name;
                    }
                }
                map.put("creatorId", creatorId);
                map.put("creatorName", instructorName);
                map.put("instructorName", instructorName);

                if (cohort.getCourse() != null) {
                    map.put("courseTitle", cohort.getCourse().getTitle());
                    map.put("courseSlug", cohort.getCourse().getSlug());
                    map.put("categoryName", cohort.getCourse().getCategory() != null ? cohort.getCourse().getCategory().getName() : null);
                }
                return map;
            }).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("content", cohorts);
            response.put("currentPage", cohortPage.getNumber());
            response.put("totalItems", cohortPage.getTotalElements());
            response.put("totalPages", cohortPage.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[CohortApiController] Lỗi khi tải danh sách public cohorts: ", e);
            throw e;
        }
    }
}

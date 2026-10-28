package vn.edu.ute.utelearn.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.utelearn.entity.Category;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.service.CategoryService;
import vn.edu.ute.utelearn.service.CourseService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseApiController {

    private final CourseService courseService;
    private final CategoryService categoryService;
    private final vn.edu.ute.utelearn.service.SectionService sectionService;
    private final vn.edu.ute.utelearn.service.LessonService lessonService;
    private final vn.edu.ute.utelearn.service.AuthService authService;
    private final vn.edu.ute.utelearn.dao.CohortMemberRepository cohortMemberRepository;
    private final vn.edu.ute.utelearn.dao.CohortRepository cohortRepository;
    private final vn.edu.ute.utelearn.dao.CohortScheduleRepository cohortScheduleRepository;
    private final vn.edu.ute.utelearn.dao.CartItemRepository cartItemRepository;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private vn.edu.ute.utelearn.dao.EnrollmentRepository enrollmentRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getCourses(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Course> coursePage = courseService.getPublishedCourses(q, category, level, pageable);
        Optional<vn.edu.ute.utelearn.entity.User> currentUserOpt = authService.getCurrentAuthenticatedUser();

        List<Map<String, Object>> courses = coursePage.getContent().stream().map(course -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", course.getId());
            map.put("code", course.getCode());
            map.put("title", course.getTitle());
            map.put("slug", course.getSlug());
            map.put("headline", course.getHeadline());
            map.put("thumbnailUrl", (course.getThumbnailUrl() != null && !course.getThumbnailUrl().isBlank()) ? course.getThumbnailUrl() : "/images/courses/default-cover.png");
            map.put("level", course.getLevel());
            List<vn.edu.ute.utelearn.entity.Cohort> cohortsForPrice = cohortRepository.findByCourseIdOrderByCreatedAtAsc(course.getId());
            java.math.BigDecimal sumCohorts = cohortsForPrice.stream()
                    .map(vn.edu.ute.utelearn.entity.Cohort::getPrice)
                    .filter(java.util.Objects::nonNull)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            java.math.BigDecimal effectivePrice = (course.getPrice() != null && course.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0)
                    ? course.getPrice()
                    : sumCohorts;
            map.put("price", effectivePrice);
            map.put("categoryName", course.getCategory() != null ? course.getCategory().getName() : null);
            map.put("categorySlug", course.getCategory() != null ? course.getCategory().getSlug() : null);
            map.put("instructorName",
                    course.getCreatedBy() != null ? course.getCreatedBy().getFullName() : "Giảng viên");
            map.put("modulesCount", cohortRepository.countByCourseId(course.getId()));
            map.put("enrolledCount", cohortMemberRepository.countDistinctUsersByCourseId(course.getId()));

            Double avgEnrollment = (enrollmentRepository != null) ? enrollmentRepository.getAverageRatingByCourseId(course.getId()) : null;
            long countEnrollment = (enrollmentRepository != null) ? enrollmentRepository.countRatingsByCourseId(course.getId()) : 0L;
            Double rating = (avgEnrollment != null && countEnrollment > 0) ? Math.round(avgEnrollment * 10.0) / 10.0 : null;
            map.put("rating", rating);
            map.put("ratingCount", countEnrollment);

            List<vn.edu.ute.utelearn.entity.Cohort> cohorts = cohortRepository.findByCourseIdOrderByCreatedAtAsc(course.getId());
            java.math.BigDecimal sumOfCohorts = cohorts.stream()
                    .map(vn.edu.ute.utelearn.entity.Cohort::getPrice)
                    .filter(java.util.Objects::nonNull)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            java.math.BigDecimal originalPrice = null;
            if (course.getPrice() != null && course.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
                if (sumOfCohorts.compareTo(course.getPrice()) > 0) {
                    originalPrice = sumOfCohorts;
                } else {
                    originalPrice = course.getPrice().multiply(new java.math.BigDecimal("1.25"))
                            .divide(new java.math.BigDecimal("10000"), 0, java.math.RoundingMode.UP)
                            .multiply(new java.math.BigDecimal("10000"));
                }
            }
            map.put("originalPrice", originalPrice);
            map.put("sumOfCohorts", sumOfCohorts);

            boolean isEnrolled = false;
            boolean isPartiallyEnrolled = false;
            boolean isOwner = false;
            boolean isCourseInCart = false;
            int userEnrolledCohortsCount = 0;
            if (currentUserOpt.isPresent()) {
                vn.edu.ute.utelearn.entity.User user = currentUserOpt.get();
                isOwner = course.getCreatedBy() != null && course.getCreatedBy().getId().equals(user.getId());
                isCourseInCart = cartItemRepository.existsByUserIdAndCourseId(user.getId(), course.getId());
                if (!cohorts.isEmpty()) {
                    long enrolledCount = cohorts.stream()
                            .filter(c -> cohortMemberRepository.existsByCohort_IdAndUser_Id(c.getId(), user.getId()))
                            .count();
                    userEnrolledCohortsCount = (int) enrolledCount;
                    isEnrolled = (enrolledCount == cohorts.size());
                    isPartiallyEnrolled = (enrolledCount > 0 && enrolledCount < cohorts.size());
                }
            }
            map.put("isEnrolled", isEnrolled);
            map.put("isPartiallyEnrolled", isPartiallyEnrolled);
            map.put("isOwner", isOwner);
            map.put("isCourseInCart", isCourseInCart);
            map.put("userEnrolledCohortsCount", userEnrolledCohortsCount);
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("content", courses);
        response.put("currentPage", coursePage.getNumber());
        response.put("totalItems", coursePage.getTotalElements());
        response.put("totalPages", coursePage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Map<String, Object>>> getCategories() {
        List<Category> categories = categoryService.getRootCategories();
        List<Map<String, Object>> result = categories.stream().map(cat -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", cat.getId());
            map.put("name", cat.getName());
            map.put("slug", cat.getSlug());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{slug}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getCourseDetail(@PathVariable String slug) {
        try {
            Course course = courseService.getCourseBySlug(slug);

            Map<String, Object> map = new HashMap<>();
            map.put("id", course.getId());
            map.put("title", course.getTitle());
            map.put("slug", course.getSlug());
            map.put("headline", course.getHeadline());
            map.put("description", course.getDescription());
            map.put("thumbnailUrl", (course.getThumbnailUrl() != null && !course.getThumbnailUrl().isBlank()) ? course.getThumbnailUrl() : "/images/courses/default-cover.png");
            map.put("level", course.getLevel());
            map.put("objectives", course.getObjectives());
            map.put("requirements", course.getRequirements());
            map.put("categoryName", course.getCategory() != null ? course.getCategory().getName() : null);
            map.put("categorySlug", course.getCategory() != null ? course.getCategory().getSlug() : null);
            map.put("instructorName",
                    course.getCreatedBy() != null ? course.getCreatedBy().getFullName() : "Giảng viên");
            map.put("instructorAvatar", course.getCreatedBy() != null ? course.getCreatedBy().getAvatarUrl() : null);

            boolean isPrivileged = false;
            java.util.Set<Long> unlockedLessonIds = java.util.Collections.emptySet();
            java.util.Optional<vn.edu.ute.utelearn.entity.User> currentUserOpt = authService.getCurrentAuthenticatedUser();
            if (currentUserOpt.isPresent()) {
                vn.edu.ute.utelearn.entity.User user = currentUserOpt.get();
                boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getCode().equals("ADMIN"));
                boolean isModerator = user.getRoles().stream().anyMatch(r -> r.getCode().equals("MODERATOR"));
                boolean isInstructor = course.getCreatedBy() != null && course.getCreatedBy().getId().equals(user.getId());
                isPrivileged = isAdmin || isModerator || isInstructor;
                unlockedLessonIds = cohortScheduleRepository.findUnlockedLessonIdsByUserIdAndCourseId(user.getId(), course.getId());
            }

            List<Map<String, Object>> curriculum = new java.util.ArrayList<>();
            List<vn.edu.ute.utelearn.entity.Section> sections = sectionService.getSectionsByCourseId(course.getId());
            for (vn.edu.ute.utelearn.entity.Section s : sections) {
                Map<String, Object> sectionMap = new HashMap<>();
                Map<String, Object> sDto = new HashMap<>();
                sDto.put("id", s.getId());
                sDto.put("title", s.getTitle());
                sDto.put("cohortId", s.getCohort() != null ? s.getCohort().getId() : null);
                sDto.put("cohortName", s.getCohort() != null ? s.getCohort().getName() : null);
                sectionMap.put("section", sDto);

                List<Map<String, Object>> lessonDtos = new java.util.ArrayList<>();
                for (vn.edu.ute.utelearn.entity.Lesson l : lessonService.getLessonsBySectionId(s.getId())) {
                    if (!isPrivileged && !l.isPublished()) {
                        continue;
                    }
                    Map<String, Object> lDto = new HashMap<>();
                    lDto.put("id", l.getId());
                    lDto.put("title", l.getTitle());
                    lDto.put("lessonType", l.getLessonType());
                    lDto.put("isFreePreview", l.getIsFreePreview());

                    boolean canAccess = isPrivileged || (l.getIsFreePreview() != null && l.getIsFreePreview()) || unlockedLessonIds.contains(l.getId());
                    if (canAccess) {
                        lDto.put("assetUrl", l.getAssetUrl());
                        lDto.put("videoUrl", l.getVideoUrl());
                        lDto.put("documentContent", l.getDocumentContent());
                    } else {
                        lDto.put("assetUrl", null);
                        lDto.put("videoUrl", null);
                        lDto.put("documentContent", null);
                        lDto.put("isLocked", true);
                    }

                    lessonDtos.add(lDto);
                }
                sectionMap.put("lessons", lessonDtos);
                curriculum.add(sectionMap);
            }
            map.put("curriculum", curriculum);

            List<vn.edu.ute.utelearn.entity.Cohort> courseModules = cohortRepository.findByCourseIdOrdered(course.getId()).stream()
                    .filter(c -> c != null && !c.isFullCourseCohort())
                    .toList();
            List<Map<String, Object>> moduleDtos = new java.util.ArrayList<>();
            for (vn.edu.ute.utelearn.entity.Cohort cm : courseModules) {
                Map<String, Object> mDto = new HashMap<>();
                mDto.put("id", cm.getId());
                mDto.put("code", cm.getCode());
                mDto.put("name", cm.getName());
                mDto.put("price", cm.getPrice());
                mDto.put("description", cm.getDescription());
                mDto.put("objectives", cm.getObjectives());
                String moduleThumb = cm.getThumbnailUrl();
                if (moduleThumb == null || moduleThumb.isBlank()) {
                    moduleThumb = (course.getThumbnailUrl() != null && !course.getThumbnailUrl().isBlank())
                            ? course.getThumbnailUrl()
                            : "/images/courses/default-cover.png";
                }
                mDto.put("thumbnailUrl", moduleThumb);
                mDto.put("currentEnrolled", cohortMemberRepository.countByCohort_Id(cm.getId()));
                mDto.put("maxCapacity", cm.getMaxCapacity());
                mDto.put("lessonIds", cohortScheduleRepository.findLessonIdsByCohortId(cm.getId()));
                moduleDtos.add(mDto);
            }
            map.put("modules", moduleDtos);

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error("[CourseApiController] Lỗi khi lấy chi tiết khóa học slug: {}", slug, e);
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{slug}/modules/{moduleId}/learn")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getModuleLearnData(@PathVariable String slug, @PathVariable Long moduleId) {
        try {
            Course course = courseService.getCourseBySlug(slug);
            vn.edu.ute.utelearn.entity.Cohort module = cohortRepository.findById(moduleId).orElse(null);
            if (module == null || !module.getCourse().getId().equals(course.getId())) {
                return ResponseEntity.notFound().build();
            }

            java.util.Optional<vn.edu.ute.utelearn.entity.User> currentUserOpt = authService.getCurrentAuthenticatedUser();
            boolean isAuthorized = false;
            if (currentUserOpt.isPresent()) {
                vn.edu.ute.utelearn.entity.User user = currentUserOpt.get();
                boolean isEnrolled = cohortMemberRepository.existsByCohort_IdAndUser_Id(module.getId(), user.getId());
                boolean hasRole = user.getRoles().stream().anyMatch(r -> r.getCode().equals("ADMIN") ||
                        r.getCode().equals("MODERATOR") ||
                        r.getCode().equals("INSTRUCTOR"));
                isAuthorized = isEnrolled || hasRole;
            }

            if (!isAuthorized) {
                return ResponseEntity.status(403).body(Map.of("message", "Chưa được cấp quyền truy cập module này"));
            }

            boolean isPrivilegedUser = currentUserOpt.isPresent() && currentUserOpt.get().getRoles().stream().anyMatch(r -> r.getCode().equals("ADMIN") ||
                    r.getCode().equals("MODERATOR") ||
                    r.getCode().equals("INSTRUCTOR"));

            List<vn.edu.ute.utelearn.entity.CohortSchedule> schedules = cohortScheduleRepository.findByCohortIdWithDetails(module.getId());
            List<Map<String, Object>> lessons = schedules.stream()
                    .filter(cs -> isPrivilegedUser || cs.getLesson().isPublished())
                    .map(cs -> {
                vn.edu.ute.utelearn.entity.Lesson l = cs.getLesson();
                Map<String, Object> lMap = new HashMap<>();
                lMap.put("id", l.getId());
                lMap.put("title", l.getTitle());
                lMap.put("lessonType", l.getLessonType());
                lMap.put("sectionId", l.getSection() != null ? l.getSection().getId() : null);
                lMap.put("sectionTitle", l.getSection() != null ? l.getSection().getTitle() : null);
                lMap.put("assetUrl", l.getAssetUrl());
                lMap.put("videoUrl", l.getVideoUrl());
                lMap.put("documentContent", l.getDocumentContent());
                lMap.put("deadlineAt", cs.getFormattedDeadlineAt());
                lMap.put("deadlineAtDisplay", cs.getDisplayDeadlineAt());
                lMap.put("isDeadlinePassed", cs.isDeadlinePassed());
                return lMap;
            }).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            Map<String, Object> courseMap = new HashMap<>();
            courseMap.put("id", course.getId());
            courseMap.put("title", course.getTitle());
            courseMap.put("slug", course.getSlug());
            courseMap.put("thumbnailUrl", (course.getThumbnailUrl() != null && !course.getThumbnailUrl().isBlank()) ? course.getThumbnailUrl() : "/images/courses/default-cover.png");
            response.put("course", courseMap);

            Map<String, Object> moduleMap = new HashMap<>();
            moduleMap.put("id", module.getId());
            moduleMap.put("code", module.getCode());
            moduleMap.put("name", module.getName());
            moduleMap.put("description", module.getDescription());
            moduleMap.put("objectives", module.getObjectives());
            String modThumb = module.getThumbnailUrl();
            if (modThumb == null || modThumb.isBlank()) {
                modThumb = (course.getThumbnailUrl() != null && !course.getThumbnailUrl().isBlank())
                        ? course.getThumbnailUrl()
                        : "/images/courses/default-cover.png";
            }
            moduleMap.put("thumbnailUrl", modThumb);
            response.put("module", moduleMap);

            response.put("lessons", lessons);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[CourseApiController] Lỗi khi lấy dữ liệu học module id: {}", moduleId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private vn.edu.ute.utelearn.service.CertificateService certificateService;

    @PostMapping(value = "/review", produces = "application/json")
    public ResponseEntity<?> submitCourseReview(@RequestBody vn.edu.ute.utelearn.dto.CourseReviewRequestDTO req) {
        if (req == null || req.getRating() == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Số sao đánh giá không được để trống"));
        }
        if (certificateService == null) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "Dịch vụ chứng chỉ chưa sẵn sàng"));
        }
        Long userId = null;
        if (authService != null) {
            Optional<vn.edu.ute.utelearn.entity.User> uOpt = authService.getCurrentAuthenticatedUser();
            if (uOpt.isPresent()) {
                userId = uOpt.get().getId();
            }
        }
        try {
            vn.edu.ute.utelearn.dto.CertificateViewDTO cert = certificateService.submitCourseReview(
                    userId, req.getCohortId(), req.getCertCode(), req.getRating(), req.getComment());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đánh giá khóa học thành công! Giấy chứng nhận của bạn đã được kích hoạt.",
                    "data", cert
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("[CourseApiController] Lỗi khi lưu đánh giá khóa học: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "Lỗi hệ thống khi lưu đánh giá"));
        }
    }
}

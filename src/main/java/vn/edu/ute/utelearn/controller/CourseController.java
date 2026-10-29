package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.CourseService;
import vn.edu.ute.utelearn.dao.CohortMemberRepository;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.CohortScheduleRepository;
import vn.edu.ute.utelearn.dao.LessonRepository;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.CohortMember;
import vn.edu.ute.utelearn.entity.CohortSchedule;
import vn.edu.ute.utelearn.entity.Lesson;
import vn.edu.ute.utelearn.entity.Section;
import vn.edu.ute.utelearn.entity.User;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping({"/courses", "/course"})
@RequiredArgsConstructor
public class CourseController {

    private final AuthService authService;
    private final CourseService courseService;
    private final vn.edu.ute.utelearn.service.CohortService cohortService;
    private final CohortMemberRepository cohortMemberRepository;
    private final CohortRepository cohortRepository;
    private final CohortScheduleRepository cohortScheduleRepository;
    private final LessonRepository lessonRepository;
    private final vn.edu.ute.utelearn.service.SectionService sectionService;
    private final vn.edu.ute.utelearn.dao.SectionRepository sectionRepository;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private vn.edu.ute.utelearn.security.RbacHelper rbacHelper;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private vn.edu.ute.utelearn.service.GradebookService gradebookService;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private vn.edu.ute.utelearn.dao.EnrollmentRepository enrollmentRepository;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private vn.edu.ute.utelearn.dao.WishlistRepository wishlistRepository;

    private Course resolveCourse(String slugOrId) {
        if (slugOrId == null || slugOrId.isBlank() || "standalone".equalsIgnoreCase(slugOrId)) {
            return null;
        }
        if (slugOrId.matches("^\\d+$")) {
            try {
                return courseService.getCourseById(Long.parseLong(slugOrId));
            } catch (Exception ignored) {
            }
        }
        try {
            return courseService.getCourseBySlug(slugOrId);
        } catch (Exception e) {
            try {
                Long id = Long.parseLong(slugOrId);
                return courseService.getCourseById(id);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    @GetMapping({"/modules/{moduleId}/learn", "/module/{moduleId}/learn"})
    public String showModuleLearnDirect(@PathVariable Long moduleId) {
        Optional<Cohort> cohortOpt = cohortRepository.findById(moduleId);
        if (cohortOpt.isPresent()) {
            Cohort cohort = cohortOpt.get();
            String courseKey = (cohort.getCourse() != null && cohort.getCourse().getSlug() != null && !cohort.getCourse().getSlug().isBlank())
                    ? cohort.getCourse().getSlug()
                    : "standalone";
            return "redirect:/courses/" + courseKey + "/modules/" + moduleId + "/learn";
        }
        return "redirect:/cohorts";
    }

    @GetMapping({"/modules/{moduleId}", "/module/{moduleId}"})
    public String showModuleDetailDirect(@PathVariable Long moduleId) {
        Optional<Cohort> cohortOpt = cohortRepository.findById(moduleId);
        if (cohortOpt.isPresent()) {
            Cohort cohort = cohortOpt.get();
            String courseKey = (cohort.getCourse() != null && cohort.getCourse().getSlug() != null && !cohort.getCourse().getSlug().isBlank())
                    ? cohort.getCourse().getSlug()
                    : "standalone";
            return "redirect:/courses/" + courseKey + "/modules/" + moduleId;
        }
        return "redirect:/cohorts";
    }

    @GetMapping({"/{courseIdOrSlug}/curriculum"})
    public String redirectToCurriculum(@PathVariable String courseIdOrSlug) {
        Course course = resolveCourse(courseIdOrSlug);
        if (course != null) {
            return "redirect:/instructor/courses/" + course.getId() + "/curriculum";
        }
        try {
            Long cId = Long.parseLong(courseIdOrSlug);
            return "redirect:/instructor/courses/" + cId + "/curriculum";
        } catch (Exception ignored) {
            return "redirect:/instructor/courses";
        }
    }

    public String redirectToCurriculum(Long id) {
        return redirectToCurriculum(String.valueOf(id));
    }

    @GetMapping({"/{courseIdOrSlug}/modules/create", "/{courseIdOrSlug}/module/create"})
    public String redirectToCreateModule(@PathVariable String courseIdOrSlug) {
        Course course = resolveCourse(courseIdOrSlug);
        if (course != null) {
            return "redirect:/instructor/courses/" + course.getId() + "/modules/create";
        }
        try {
            Long cId = Long.parseLong(courseIdOrSlug);
            return "redirect:/instructor/courses/" + cId + "/modules/create";
        } catch (Exception ignored) {
            return "redirect:/instructor/courses";
        }
    }

    public String redirectToCreateModule(Long id) {
        return redirectToCreateModule(String.valueOf(id));
    }

    @GetMapping({"/{courseIdOrSlug}/modules/{moduleId}/edit", "/{courseIdOrSlug}/module/{moduleId}/edit"})
    public String redirectToEditModule(@PathVariable String courseIdOrSlug, @PathVariable Long moduleId) {
        try {
            Optional<Cohort> cohortOpt = cohortRepository.findById(moduleId);
            if (cohortOpt.isPresent() && cohortOpt.get().getCourse() != null) {
                return "redirect:/instructor/courses/" + cohortOpt.get().getCourse().getId() + "/modules/" + moduleId + "/edit";
            }
            Course course = resolveCourse(courseIdOrSlug);
            if (course != null) {
                return "redirect:/instructor/courses/" + course.getId() + "/modules/" + moduleId + "/edit";
            }
        } catch (Exception ignored) {
        }
        return "redirect:/instructor/courses";
    }

    @GetMapping({"/{courseIdOrSlug}/modules/{moduleId}/curriculum", "/{courseIdOrSlug}/module/{moduleId}/curriculum"})
    public String redirectToModuleCurriculum(@PathVariable String courseIdOrSlug, @PathVariable Long moduleId) {
        try {
            Optional<Cohort> cohortOpt = cohortRepository.findById(moduleId);
            if (cohortOpt.isPresent() && cohortOpt.get().getCourse() != null) {
                return "redirect:/instructor/courses/" + cohortOpt.get().getCourse().getId() + "/modules/" + moduleId + "/curriculum";
            }
            Course course = resolveCourse(courseIdOrSlug);
            if (course != null) {
                return "redirect:/instructor/courses/" + course.getId() + "/modules/" + moduleId + "/curriculum";
            }
        } catch (Exception ignored) {
        }
        return "redirect:/instructor/courses";
    }

    @GetMapping({"/{courseIdOrSlug}/modules/{moduleId}/gradebook", "/{courseIdOrSlug}/module/{moduleId}/gradebook"})
    public String redirectToModuleGradebook(@PathVariable String courseIdOrSlug, @PathVariable Long moduleId) {
        return "redirect:/instructor/cohorts/" + moduleId + "/gradebook";
    }

    @GetMapping
    public String showCourseList(Model model) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        currentUserOpt.ifPresent(user -> model.addAttribute("currentUser", user));
        return "courses/index";
    }

    @Transactional
    @GetMapping("/my-courses")
    public String showMyCourses(Model model) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }
        User user = currentUserOpt.get();
        model.addAttribute("currentUser", user);
        vn.edu.ute.utelearn.dto.UserEnrollmentSummaryDTO summary = cohortService.getUserEnrollmentSummary(user);
        model.addAttribute("enrolledCourses", summary.getEnrolledCourses());
        model.addAttribute("enrolledModules", summary.getEnrolledModules());
        model.addAttribute("upsellSuggestions", summary.getUpsellSuggestions());
        model.addAttribute("myCourses", cohortMemberRepository.findByUserOrderByEnrolledAtDesc(user));
        return "courses/my-courses";
    }

    @GetMapping("/{slug}")
    public String showCourseDetail(@PathVariable String slug, Model model) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();

        Course course = resolveCourse(slug);
        if (course == null) {
            return "redirect:/courses";
        }

        boolean isEnrolled = false;
        boolean hasAccessRole = false;
        boolean canEdit = false;
        java.util.Set<Long> enrolledCohortIds = new java.util.HashSet<>();
        List<CohortMember> userMemberships = java.util.Collections.emptyList();
        if (currentUserOpt.isPresent()) {
            User user = currentUserOpt.get();
            model.addAttribute("currentUser", user);
            userMemberships = cohortMemberRepository.findByCohort_Course_IdAndUser_Id(course.getId(), user.getId());
            enrolledCohortIds = userMemberships.stream().map(cm -> cm.getCohort().getId()).collect(java.util.stream.Collectors.toSet());
            isEnrolled = !enrolledCohortIds.isEmpty();
            canEdit = checkCanEdit(user, course, null);
            hasAccessRole = checkHasAccessRole(user, course, null);
        }

        List<Cohort> availableCohorts = cohortRepository.findByCourseIdAndStatusInOrdered(course.getId(),
                List.of("ENROLLING", "UPCOMING", "IN_PROGRESS"))
                .stream()
                .filter(c -> c != null && !c.isFullCourseCohort())
                .toList();
        final java.util.Set<Long> finalEnrolledCohortIds = enrolledCohortIds;
        final boolean finalHasAccessRole = hasAccessRole;
        List<java.util.Map<String, Object>> cohortList = availableCohorts.stream().map(c -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", c.getId());
            map.put("code", c.getCode());
            map.put("name", c.getName());
            map.put("currentEnrolled", cohortMemberRepository.countByCohort_Id(c.getId()));
            map.put("maxCapacity", c.getMaxCapacity());
            map.put("status", c.getStatus());
            map.put("price", c.getPrice());
            map.put("isEnrolled", finalEnrolledCohortIds.contains(c.getId()));
            return map;
        }).collect(java.util.stream.Collectors.toList());

        boolean isAllEnrolled = !availableCohorts.isEmpty() && availableCohorts.stream().allMatch(c -> finalEnrolledCohortIds.contains(c.getId()));
        java.math.BigDecimal sumOfCohorts = availableCohorts.stream()
                .map(Cohort::getPrice)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.math.BigDecimal fullCoursePrice = (course.getPrice() != null && course.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0)
                ? course.getPrice()
                : sumOfCohorts;

        java.math.BigDecimal remainingCohortSum = availableCohorts.stream()
                .filter(c -> !finalEnrolledCohortIds.contains(c.getId()))
                .map(Cohort::getPrice)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.math.BigDecimal remainingCoursePrice = isEnrolled 
                ? remainingCohortSum.min(fullCoursePrice)
                : fullCoursePrice;

        java.math.BigDecimal discountAmount = (sumOfCohorts.compareTo(fullCoursePrice) > 0)
                ? sumOfCohorts.subtract(fullCoursePrice)
                : java.math.BigDecimal.ZERO;

        long unenrolledCount = availableCohorts.stream().filter(c -> !finalEnrolledCohortIds.contains(c.getId())).count();

        List<Cohort> allCourseCohorts = cohortRepository.findByCourseIdOrdered(course.getId())
                .stream()
                .filter(c -> c != null && !c.isFullCourseCohort())
                .toList();

        boolean isCourseCompleted = false;
        java.math.BigDecimal courseAvgGrade = null;
        String courseCertificateUrl = null;
        boolean courseHasReviewed = false;
        vn.edu.ute.utelearn.entity.Enrollment userEnrollment = null;
        int courseCompletedModulesCount = 0;
        int courseTotalModulesCount = allCourseCohorts.size();
        int courseProgressPercent = 0;

        if (currentUserOpt.isPresent()) {
            User user = currentUserOpt.get();
            for (Cohort mod : allCourseCohorts) {
                CohortMember cm = userMemberships.stream()
                        .filter(m -> m.getCohort() != null && m.getCohort().getId().equals(mod.getId()))
                        .findFirst().orElse(null);
                if (cm != null && Boolean.TRUE.equals(cm.getIsPassed())) {
                    courseCompletedModulesCount++;
                }
            }
            courseProgressPercent = (courseTotalModulesCount > 0)
                    ? (courseCompletedModulesCount * 100 / courseTotalModulesCount) : 0;

            if (enrollmentRepository != null) {
                java.util.Optional<vn.edu.ute.utelearn.entity.Enrollment> enrollOpt =
                        enrollmentRepository.findByUser_IdAndCourse_Id(user.getId(), course.getId());
                if (enrollOpt.isPresent()) {
                    userEnrollment = enrollOpt.get();
                    isCourseCompleted = Boolean.TRUE.equals(userEnrollment.getIsCompleted());
                    courseAvgGrade = userEnrollment.getAverageGrade();
                    courseCertificateUrl = userEnrollment.getCertificateUrl();
                    courseHasReviewed = userEnrollment.hasReviewed();
                }
            }
            if (!isCourseCompleted && courseTotalModulesCount > 0 && courseCompletedModulesCount >= courseTotalModulesCount) {
                isCourseCompleted = true;
            }
            if (gradebookService != null) {
                try {
                    vn.edu.ute.utelearn.dto.StudentCourseGradeDTO studentCourseGrade =
                            gradebookService.getStudentCourseGrade(course.getId(), user.getId());
                    model.addAttribute("studentCourseGrade", studentCourseGrade);
                } catch (Exception e) {
                    log.warn("[CourseController] Không thể lấy bảng điểm khóa học {} cho user {}: {}", course.getId(), user.getId(), e.getMessage());
                }
            }
        }
        List<Lesson> allCourseLessons = lessonRepository.findBySectionCourseIdOrderByOrderIndexAsc(course.getId());
        if (!hasAccessRole) {
            allCourseLessons = allCourseLessons.stream().filter(Lesson::isPublished).toList();
        }
        java.util.Map<Long, Lesson> lessonMap = allCourseLessons.stream()
                .collect(java.util.stream.Collectors.toMap(Lesson::getId, l -> l, (a, b) -> a));

        java.util.Set<Long> assignedLessonIds = new java.util.HashSet<>();
        List<java.util.Map<String, Object>> courseModulesData = new java.util.ArrayList<>();
        for (Cohort cohort : allCourseCohorts) {
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", cohort.getId());
            map.put("code", cohort.getCode());
            map.put("name", cohort.getName());
            map.put("status", cohort.getStatus());
            map.put("price", cohort.getPrice());
            map.put("description", cohort.getDescription());
            map.put("objectivesRaw", cohort.getObjectives() != null ? cohort.getObjectives() : "");
            map.put("objectivesList", parseJsonStringList(cohort.getObjectives()));
            map.put("currentEnrolled", cohortMemberRepository.countByCohort_Id(cohort.getId()));
            map.put("maxCapacity", cohort.getMaxCapacity());
            map.put("isEnrolled", finalEnrolledCohortIds.contains(cohort.getId()));
            map.put("isFull", cohort.getMaxCapacity() != null && cohortMemberRepository.countByCohort_Id(cohort.getId()) >= cohort.getMaxCapacity());

            List<Long> modLessonIds = cohortScheduleRepository.findLessonIdsByCohortId(cohort.getId());
            List<Lesson> modLessons = new java.util.ArrayList<>();
            for (Long lid : modLessonIds) {
                if (lessonMap.containsKey(lid)) {
                    modLessons.add(lessonMap.get(lid));
                    assignedLessonIds.add(lid);
                }
            }
            map.put("lessons", modLessons);
            map.put("lessonCount", modLessons.size());
            courseModulesData.add(map);
        }

        java.util.Set<Long> standaloneLessonIds = new java.util.HashSet<>();
        List<Section> courseSections = sectionRepository.findByCourseIdOrderByOrderIndexAsc(course.getId());
        List<java.util.Map<String, Object>> standaloneSectionsData = new java.util.ArrayList<>();
        for (Section sec : courseSections) {
            if (sec.getCohort() == null) {
                List<Lesson> secLessons = lessonRepository.findBySectionIdOrderByOrderIndexAsc(sec.getId())
                        .stream()
                        .filter(l -> !assignedLessonIds.contains(l.getId()))
                        .filter(l -> finalHasAccessRole || l.isPublished())
                        .toList();
                if (!secLessons.isEmpty()) {
                    java.util.Map<String, Object> sMap = new java.util.LinkedHashMap<>();
                    sMap.put("id", sec.getId());
                    sMap.put("title", sec.getTitle());
                    sMap.put("lessons", secLessons);
                    standaloneSectionsData.add(sMap);
                    secLessons.forEach(l -> standaloneLessonIds.add(l.getId()));
                }
            }
        }

        List<Lesson> unassignedLessons = allCourseLessons.stream()
                .filter(l -> !assignedLessonIds.contains(l.getId()) && !standaloneLessonIds.contains(l.getId()))
                .toList();

        log.info("[CourseController] Hiển thị chi tiết khóa học SSR cho slug: {}, id: {}, modules: {}, lessons: {}",
                slug, course.getId(), courseModulesData.size(), allCourseLessons.size());

        model.addAttribute("course", course);
        model.addAttribute("availableCohorts", cohortList);
        model.addAttribute("courseSlug", slug);
        model.addAttribute("isEnrolled", isEnrolled);
        model.addAttribute("isAllEnrolled", isAllEnrolled);
        model.addAttribute("hasEnrolledSome", isEnrolled && !isAllEnrolled);
        model.addAttribute("fullCoursePrice", fullCoursePrice);
        model.addAttribute("sumOfCohorts", sumOfCohorts);
        model.addAttribute("discountAmount", discountAmount);
        model.addAttribute("remainingCoursePrice", remainingCoursePrice);
        model.addAttribute("unenrolledCount", unenrolledCount);
        model.addAttribute("hasAccessRole", hasAccessRole);
        model.addAttribute("canEdit", canEdit);
        model.addAttribute("objectivesList", parseJsonStringList(course.getObjectives()));
        model.addAttribute("requirementsList", parseJsonStringList(course.getRequirements()));
        model.addAttribute("courseModules", courseModulesData);
        model.addAttribute("standaloneSections", standaloneSectionsData);
        model.addAttribute("unassignedLessons", unassignedLessons);
        model.addAttribute("totalLessonsCount", allCourseLessons.size());
        model.addAttribute("totalModulesCount", allCourseCohorts.size());
        model.addAttribute("isCourseCompleted", isCourseCompleted);
        model.addAttribute("courseAvgGrade", courseAvgGrade);
        model.addAttribute("courseCertificateUrl", courseCertificateUrl);
        model.addAttribute("courseHasReviewed", courseHasReviewed);
        model.addAttribute("courseCompletedModulesCount", courseCompletedModulesCount);
        model.addAttribute("courseTotalModulesCount", courseTotalModulesCount);
        model.addAttribute("courseProgressPercent", courseProgressPercent);
        Double avgRatingVal = (enrollmentRepository != null) ? enrollmentRepository.getAverageRatingByCourseId(course.getId()) : null;
        long totalReviewsCount = (enrollmentRepository != null) ? enrollmentRepository.countRatingsByCourseId(course.getId()) : 0L;
        Double courseRating = (avgRatingVal != null && totalReviewsCount > 0) ? Math.round(avgRatingVal * 10.0) / 10.0 : null;
        List<vn.edu.ute.utelearn.entity.Enrollment> courseReviews = (enrollmentRepository != null) ? enrollmentRepository.findReviewsByCourseIdWithUser(course.getId()) : java.util.Collections.emptyList();

        long star5 = courseReviews.stream().filter(r -> r.getRating() != null && r.getRating() == 5).count();
        long star4 = courseReviews.stream().filter(r -> r.getRating() != null && r.getRating() == 4).count();
        long star3 = courseReviews.stream().filter(r -> r.getRating() != null && r.getRating() == 3).count();
        long star2 = courseReviews.stream().filter(r -> r.getRating() != null && r.getRating() == 2).count();
        long star1 = courseReviews.stream().filter(r -> r.getRating() != null && r.getRating() == 1).count();

        java.util.Map<Object, Long> ratingBreakdown = new java.util.HashMap<>();
        ratingBreakdown.put(5, star5);
        ratingBreakdown.put("5", star5);
        ratingBreakdown.put(4, star4);
        ratingBreakdown.put("4", star4);
        ratingBreakdown.put(3, star3);
        ratingBreakdown.put("3", star3);
        ratingBreakdown.put(2, star2);
        ratingBreakdown.put("2", star2);
        ratingBreakdown.put(1, star1);
        ratingBreakdown.put("1", star1);

        boolean inWishlist = false;
        if (currentUserOpt.isPresent() && wishlistRepository != null) {
            inWishlist = wishlistRepository.existsByUser_IdAndCourse_Id(currentUserOpt.get().getId(), course.getId());
        }
        model.addAttribute("courseRating", courseRating);
        model.addAttribute("totalReviewsCount", totalReviewsCount);
        model.addAttribute("courseReviews", courseReviews);
        model.addAttribute("ratingBreakdown", ratingBreakdown);
        model.addAttribute("userEnrollment", userEnrollment);
        model.addAttribute("inWishlist", inWishlist);

        return "courses/detail";
    }

    private List<String> parseJsonStringList(String jsonOrText) {
        if (jsonOrText == null || jsonOrText.isBlank() || "[]".equals(jsonOrText.trim())) {
            return java.util.Collections.emptyList();
        }
        List<String> result = new java.util.ArrayList<>();
        String trimmed = jsonOrText.trim();
        if (trimmed.startsWith("[")) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(trimmed);
                if (root.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode node : root) {
                        if (node.isObject() && node.has("value")) {
                            result.add(node.get("value").asText());
                        } else if (node.isTextual()) {
                            result.add(node.asText());
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (result.isEmpty()) {
            for (String line : trimmed.split("\n")) {
                String clean = line.trim();
                if (!clean.isEmpty()) {
                    result.add(clean);
                }
            }
        }
        return result;
    }

    @GetMapping({"/{slug}/modules", "/{slug}/module"})
    public String redirectToCourseOrCohorts(@PathVariable String slug) {
        if ("standalone".equalsIgnoreCase(slug)) {
            return "redirect:/cohorts";
        }
        return "redirect:/courses/" + slug;
    }

    @GetMapping({"/{slug}/modules/{moduleId}", "/{slug}/module/{moduleId}"})
    public String showModuleDetail(@PathVariable String slug, @PathVariable Long moduleId, Model model) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        currentUserOpt.ifPresent(user -> model.addAttribute("currentUser", user));

        Cohort module = cohortRepository.findById(moduleId).orElse(null);
        if (module == null) {
            return "redirect:/cohorts";
        }

        Course course = resolveCourse(slug);
        if (course == null || "standalone".equalsIgnoreCase(slug) || (module.getCourse() != null && !module.getCourse().getId().equals(course.getId()))) {
            if (module.getCourse() != null) {
                course = module.getCourse();
            } else {
                course = Course.builder()
                        .id(0L)
                        .title(module.getName())
                        .slug("standalone")
                        .code(module.getCode())
                        .price(module.getPrice())
                        .createdBy(module.getCreatedBy())
                        .build();
            }
        }

        boolean isModEnrolled = false;
        boolean hasAccessRole = false;
        boolean canEdit = false;
        if (currentUserOpt.isPresent()) {
            User user = currentUserOpt.get();
            isModEnrolled = cohortMemberRepository.existsByCohort_IdAndUser_Id(module.getId(), user.getId());
            canEdit = checkCanEdit(user, course, module);
            hasAccessRole = checkHasAccessRole(user, course, module);
        }

        long enrolledCount = cohortMemberRepository.countByCohort_Id(module.getId());
        module.setCurrentEnrolled((int) enrolledCount);

        List<Section> cohortSections = sectionService != null
                ? sectionService.getSectionsByCohortId(module.getId())
                : sectionRepository.findSectionsForCohort(module.getId());
        if (cohortSections.isEmpty() && course != null) {
            cohortSections = sectionRepository.findByCourseIdOrderByOrderIndexAsc(course.getId());
        }

        List<CohortSchedule> schedules = cohortScheduleRepository.findByCohortIdWithDetails(module.getId());
        java.util.Set<Long> validSectionIds = cohortSections.stream().map(Section::getId).collect(java.util.stream.Collectors.toSet());
        if (!validSectionIds.isEmpty()) {
            schedules = schedules.stream()
                    .filter(cs -> cs.getLesson() != null && cs.getLesson().getSection() != null && validSectionIds.contains(cs.getLesson().getSection().getId()))
                    .toList();
        }
        List<Lesson> moduleLessons;
        if (!schedules.isEmpty()) {
            moduleLessons = schedules.stream().map(CohortSchedule::getLesson).toList();
        } else {
            moduleLessons = lessonRepository.findBySectionCourseIdOrderByOrderIndexAsc(course.getId());
        }
        if (!hasAccessRole) {
            moduleLessons = moduleLessons.stream().filter(Lesson::isPublished).toList();
        }

        List<java.util.Map<String, Object>> moduleSections = new java.util.ArrayList<>();
        java.util.Map<Long, java.util.Map<String, Object>> sectionMap = new java.util.LinkedHashMap<>();

        for (Section sec : cohortSections) {
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", sec.getId());
            map.put("title", (sec.getTitle() != null && !sec.getTitle().isBlank()) ? sec.getTitle() : "Phần học");
            map.put("orderIndex", sec.getOrderIndex() != null ? sec.getOrderIndex() : 0);
            map.put("lessons", new java.util.ArrayList<Lesson>());
            sectionMap.put(sec.getId(), map);
            moduleSections.add(map);
        }

        java.util.Map<String, Object> fallbackSection = null;
        for (Lesson l : moduleLessons) {
            if (l.getSection() != null && sectionMap.containsKey(l.getSection().getId())) {
                ((List<Lesson>) sectionMap.get(l.getSection().getId()).get("lessons")).add(l);
            } else if (l.getSection() != null) {
                Long secId = l.getSection().getId();
                java.util.Map<String, Object> secData = sectionMap.computeIfAbsent(secId, id -> {
                    java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
                    map.put("id", secId);
                    map.put("title", (l.getSection().getTitle() != null && !l.getSection().getTitle().isBlank())
                            ? l.getSection().getTitle() : "Phần học");
                    map.put("orderIndex", l.getSection().getOrderIndex() != null ? l.getSection().getOrderIndex() : 0);
                    map.put("lessons", new java.util.ArrayList<Lesson>());
                    moduleSections.add(map);
                    return map;
                });
                ((List<Lesson>) secData.get("lessons")).add(l);
            } else {
                if (fallbackSection == null) {
                    fallbackSection = new java.util.LinkedHashMap<>();
                    fallbackSection.put("id", 0L);
                    fallbackSection.put("title", "Nội dung chung");
                    fallbackSection.put("orderIndex", 9999);
                    fallbackSection.put("lessons", new java.util.ArrayList<Lesson>());
                    moduleSections.add(fallbackSection);
                }
                ((List<Lesson>) fallbackSection.get("lessons")).add(l);
            }
        }

        java.util.Map<Long, CohortSchedule> scheduleMap = new java.util.HashMap<>();
        for (CohortSchedule cs : schedules) {
            if (cs.getLesson() != null) {
                scheduleMap.put(cs.getLesson().getId(), cs);
            }
        }

        List<Long> completedLessonIds = java.util.Collections.emptyList();
        if (currentUserOpt.isPresent()) {
            completedLessonIds = cohortService.getCompletedLessonIds(module.getId(), currentUserOpt.get().getId());
        }
        int totalLessonCount = moduleLessons.size();
        int completedCount = completedLessonIds.size();
        int progressPercent = (totalLessonCount > 0) ? (int) Math.min(100, Math.round(((double) completedCount / totalLessonCount) * 100)) : 0;

        List<CohortSchedule> upcomingSchedules = schedules.stream()
                .filter(cs -> cs.getLesson() != null && (cs.isLockedNow() || (cs.getDeadlineAt() != null && !cs.isDeadlinePassed())))
                .sorted((a, b) -> {
                    java.time.Instant tA = a.isLockedNow() ? a.getUnlockAt() : a.getDeadlineAt();
                    java.time.Instant tB = b.isLockedNow() ? b.getUnlockAt() : b.getDeadlineAt();
                    if (tA == null && tB == null) return 0;
                    if (tA == null) return 1;
                    if (tB == null) return -1;
                    return tA.compareTo(tB);
                })
                .limit(5)
                .toList();

        if (currentUserOpt.isPresent()) {
            User user = currentUserOpt.get();
            if (gradebookService != null) {
                try {
                    vn.edu.ute.utelearn.dto.StudentGradeItemDTO studentGrade = gradebookService.getStudentGrade(module.getId(), user.getId());
                    model.addAttribute("studentGrade", studentGrade);
                } catch (Exception e) {
                    log.warn("[CourseController] Không thể lấy điểm học viên {} cho module {}: {}", user.getId(), module.getId(), e.getMessage());
                }
            }
        }

        boolean hasGradedLessons = moduleLessons.stream().anyMatch(l -> {
            String t = (l != null && l.getLessonType() != null) ? l.getLessonType().toUpperCase() : "";
            return "QUIZ".equals(t) || "ASSIGNMENT".equals(t) || "CODE_PRACTICE".equals(t) || "EXAM".equals(t) || "PROJECT".equals(t);
        });

        Double avgRatingVal = cohortMemberRepository.getAverageRatingByCohortId(module.getId());
        long totalReviewsCount = cohortMemberRepository.countRatingsByCohortId(module.getId());
        Double moduleRating = (avgRatingVal != null && totalReviewsCount > 0) ? Math.round(avgRatingVal * 10.0) / 10.0 : null;
        List<CohortMember> moduleReviews = cohortMemberRepository.findReviewsByCohortIdWithUser(module.getId());
        log.info("[CourseController] Module {}: totalReviewsCount={}, moduleReviews size={}", module.getId(), totalReviewsCount, moduleReviews != null ? moduleReviews.size() : 0);

        CohortMember userCohortMember = null;
        if (currentUserOpt.isPresent()) {
            userCohortMember = cohortMemberRepository.findByCohort_IdAndUser_Id(module.getId(), currentUserOpt.get().getId()).orElse(null);
        }

        long star5 = moduleReviews.stream().filter(r -> r.getRating() != null && r.getRating() == 5).count();
        long star4 = moduleReviews.stream().filter(r -> r.getRating() != null && r.getRating() == 4).count();
        long star3 = moduleReviews.stream().filter(r -> r.getRating() != null && r.getRating() == 3).count();
        long star2 = moduleReviews.stream().filter(r -> r.getRating() != null && r.getRating() == 2).count();
        long star1 = moduleReviews.stream().filter(r -> r.getRating() != null && r.getRating() == 1).count();
        java.util.Map<Object, Long> ratingBreakdown = new java.util.HashMap<>();
        ratingBreakdown.put(5, star5);
        ratingBreakdown.put("5", star5);
        ratingBreakdown.put(4, star4);
        ratingBreakdown.put("4", star4);
        ratingBreakdown.put(3, star3);
        ratingBreakdown.put("3", star3);
        ratingBreakdown.put(2, star2);
        ratingBreakdown.put("2", star2);
        ratingBreakdown.put(1, star1);
        ratingBreakdown.put("1", star1);

        model.addAttribute("course", course);
        model.addAttribute("module", module);
        model.addAttribute("moduleLessons", moduleLessons);
        model.addAttribute("moduleSections", moduleSections);
        model.addAttribute("isModEnrolled", isModEnrolled);
        model.addAttribute("hasAccessRole", hasAccessRole);
        model.addAttribute("canEdit", canEdit);
        model.addAttribute("hasGradedLessons", hasGradedLessons);
        model.addAttribute("completedLessonIds", completedLessonIds);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("progressPercent", progressPercent);
        model.addAttribute("scheduleMap", scheduleMap);
        model.addAttribute("upcomingSchedules", upcomingSchedules);
        model.addAttribute("moduleRating", moduleRating);
        model.addAttribute("totalReviewsCount", totalReviewsCount);
        model.addAttribute("moduleReviews", moduleReviews);
        model.addAttribute("userCohortMember", userCohortMember);
        model.addAttribute("ratingBreakdown", ratingBreakdown);
        return "courses/module-detail";
    }

    @PostMapping(value = {"/{slug}/modules/{moduleId}/review", "/modules/{moduleId}/review"}, produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> submitCohortReview(
            @PathVariable(required = false) String slug,
            @PathVariable Long moduleId,
            @RequestBody vn.edu.ute.utelearn.dto.CourseReviewRequestDTO req) {
        if (req == null || req.getRating() == null || req.getRating() < 1 || req.getRating() > 5) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Số sao đánh giá phải từ 1 đến 5 sao"));
        }

        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Vui lòng đăng nhập để gửi đánh giá"));
        }

        User user = currentUserOpt.get();
        CohortMember member = cohortMemberRepository.findByCohort_IdAndUser_Id(moduleId, user.getId()).orElse(null);
        if (member == null) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Bạn cần tham gia đợt học này để có thể gửi đánh giá"));
        }

        member.setRating(req.getRating());
        if (req.getComment() != null) {
            member.setReviewComment(req.getComment().trim());
        }
        member.setReviewedAt(java.time.Instant.now());

        if (Boolean.TRUE.equals(member.getIsPassed()) && (member.getCertificateUrl() == null || member.getCertificateUrl().isBlank())) {
            Cohort c = member.getCohort();
            String code = "UTE-CERT-" + (c != null && c.getCode() != null ? c.getCode() : "MOD") + "-" + user.getId();
            member.setCertificateUrl("/certificates/verify?code=" + code);
        }

        cohortMemberRepository.save(member);
        log.info("[CourseController] Học viên {} đã đánh giá {} sao cho cohort {}", user.getId(), req.getRating(), moduleId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Cảm ơn bạn đã gửi đánh giá cho môn học!"
        ));
    }

    @PostMapping(value = {"/{slug}/review", "/courses/{slug}/review"}, produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> submitCourseReview(
            @PathVariable(required = false) String slug,
            @RequestBody vn.edu.ute.utelearn.dto.CourseReviewRequestDTO req) {
        if (req == null || req.getRating() == null || req.getRating() < 1 || req.getRating() > 5) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Số sao đánh giá phải từ 1 đến 5 sao"));
        }

        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Vui lòng đăng nhập để gửi đánh giá"));
        }

        Course course = resolveCourse(slug);
        if (course == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Khóa học không tồn tại"));
        }

        User user = currentUserOpt.get();
        vn.edu.ute.utelearn.entity.Enrollment enrollment = enrollmentRepository.findByUser_IdAndCourse_Id(user.getId(), course.getId()).orElse(null);
        boolean isCourseCompleted = enrollment != null && Boolean.TRUE.equals(enrollment.getIsCompleted());
        if (!isCourseCompleted) {
            List<Cohort> allCourseCohorts = cohortRepository.findByCourseIdOrdered(course.getId())
                    .stream()
                    .filter(c -> c != null && !c.isFullCourseCohort())
                    .toList();
            if (!allCourseCohorts.isEmpty()) {
                List<CohortMember> memberships = cohortMemberRepository.findByCohort_Course_IdAndUser_Id(course.getId(), user.getId());
                long completedModulesCount = allCourseCohorts.stream()
                        .filter(mod -> memberships.stream().anyMatch(m -> m.getCohort() != null && m.getCohort().getId().equals(mod.getId()) && Boolean.TRUE.equals(m.getIsPassed())))
                        .count();
                if (completedModulesCount >= allCourseCohorts.size()) {
                    isCourseCompleted = true;
                }
            }
        }

        if (!isCourseCompleted) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Bạn cần hoàn thành toàn bộ khóa học để có thể gửi đánh giá"));
        }

        if (enrollment == null) {
            enrollment = vn.edu.ute.utelearn.entity.Enrollment.builder()
                    .user(user)
                    .course(course)
                    .progress(100.0)
                    .isCompleted(true)
                    .build();
        }

        enrollment.setRating(req.getRating());
        if (req.getComment() != null) {
            enrollment.setReviewComment(req.getComment().trim());
        }
        enrollment.setReviewedAt(java.time.Instant.now());

        if (Boolean.TRUE.equals(enrollment.getIsCompleted()) && (enrollment.getCertificateUrl() == null || enrollment.getCertificateUrl().isBlank())) {
            String code = "UTE-CERT-" + (course.getCode() != null ? course.getCode() : "CRS") + "-" + user.getId();
            enrollment.setCertificateUrl("/certificates/verify?code=" + code);
        }

        enrollmentRepository.save(enrollment);
        log.info("[CourseController] Học viên {} đã đánh giá {} sao cho khóa học {}", user.getId(), req.getRating(), course.getId());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Cảm ơn bạn đã gửi đánh giá cho khóa học!"
        ));
    }

    @Transactional
    @org.springframework.web.bind.annotation.RequestMapping(value = {"/{slug}/enroll/{cohortId}", "/enroll/{cohortId}"}, method = {org.springframework.web.bind.annotation.RequestMethod.GET, org.springframework.web.bind.annotation.RequestMethod.POST})
    public String enrollCourse(@PathVariable(required = false) String slug, @PathVariable Long cohortId,
            RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }

        User user = currentUserOpt.get();

        Optional<Cohort> cohortOpt = cohortRepository.findById(cohortId);
        if (cohortOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Lớp học không tồn tại.");
            return "redirect:/cohorts";
        }

        Cohort activeCohort = cohortOpt.get();
        Course course = resolveCourse(slug);
        if (course == null || "standalone".equalsIgnoreCase(slug) || (activeCohort.getCourse() != null && !activeCohort.getCourse().getId().equals(course.getId()))) {
            if (activeCohort.getCourse() != null) {
                course = activeCohort.getCourse();
            } else {
                course = Course.builder()
                        .id(0L)
                        .title(activeCohort.getName())
                        .slug("standalone")
                        .code(activeCohort.getCode())
                        .price(activeCohort.getPrice())
                        .createdBy(activeCohort.getCreatedBy())
                        .build();
            }
        }
        String courseKey = (course != null && course.getSlug() != null && !course.getSlug().isBlank()) ? course.getSlug() : "standalone";
        if (cohortMemberRepository.existsByCohort_IdAndUser_Id(activeCohort.getId(), user.getId())) {
            redirectAttributes.addFlashAttribute("info", "Bạn đã tham gia đợt học này rồi.");
            return "redirect:/courses/" + courseKey + "/modules/" + activeCohort.getId() + "/learn";
        }

        if (List.of("ENROLLING", "UPCOMING", "IN_PROGRESS").contains(activeCohort.getStatus())) {
            if (activeCohort.getCurrentEnrolled() >= activeCohort.getMaxCapacity()) {
                redirectAttributes.addFlashAttribute("error", "Lớp học đã đủ số lượng học viên.");
                return "redirect:/courses/" + courseKey;
            }

            if (activeCohort.getPrice() != null && activeCohort.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
                return "redirect:/checkout/" + cohortId;
            }

            CohortMember cohortMember = CohortMember.builder()
                    .user(user)
                    .cohort(activeCohort)
                    .build();
            cohortMemberRepository.save(cohortMember);

            activeCohort.setCurrentEnrolled((int) cohortMemberRepository.countByCohort_Id(activeCohort.getId()));
            cohortRepository.save(activeCohort);

            redirectAttributes.addFlashAttribute("success", "Tham gia đợt học thành công!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Lớp học này hiện không mở đăng ký.");
            return "redirect:/courses/" + courseKey;
        }

        return "redirect:/courses/" + courseKey + "/modules/" + activeCohort.getId() + "/learn";
    }

    @Transactional
    @org.springframework.web.bind.annotation.RequestMapping(value = "/{slug}/enroll-all", method = {org.springframework.web.bind.annotation.RequestMethod.GET, org.springframework.web.bind.annotation.RequestMethod.POST})
    public String enrollAll(@PathVariable String slug, RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }

        Course course;
        try {
            course = courseService.getCourseBySlug(slug);
        } catch (Exception e) {
            return "redirect:/courses";
        }

        User user = currentUserOpt.get();
        List<Cohort> cohorts = cohortRepository.findByCourseIdAndStatusIn(course.getId(),
                List.of("ENROLLING", "UPCOMING", "IN_PROGRESS"));
        List<Cohort> realModules = cohorts.stream()
                .filter(c -> !(c != null && c.isFullCourseCohort()))
                .collect(java.util.stream.Collectors.toList());
        List<Cohort> availableCohorts = !realModules.isEmpty() ? realModules : cohorts;

        if (availableCohorts.isEmpty()) {
            Cohort defaultCohort = Cohort.builder()
                    .course(course)
                    .name("Toàn bộ khóa học: " + course.getTitle())
                    .code(course.getCode() + "-ALL")
                    .price(course.getPrice() != null ? course.getPrice() : java.math.BigDecimal.ZERO)
                    .status("ENROLLING")
                    .visibility("PRIVATE")
                    .moderationStatus("APPROVED")
                    .maxCapacity(1000)
                    .currentEnrolled(0)
                    .enrollmentStart(java.time.Instant.now())
                    .studyStart(java.time.Instant.now())
                    .build();
            defaultCohort = cohortRepository.save(defaultCohort);
            List<Lesson> lessons = lessonRepository.findBySectionCourseIdOrderByOrderIndexAsc(course.getId());
            for (Lesson l : lessons) {
                CohortSchedule cs = CohortSchedule.builder()
                        .cohort(defaultCohort)
                        .lesson(l)
                        .unlockAt(java.time.Instant.now())
                        .build();
                cohortScheduleRepository.save(cs);
            }
            availableCohorts = List.of(defaultCohort);
        }

        List<CohortMember> userMemberships = cohortMemberRepository.findByCohort_Course_IdAndUser_Id(course.getId(), user.getId());
        java.util.Set<Long> enrolledCohortIds = userMemberships.stream().map(cm -> cm.getCohort().getId()).collect(java.util.stream.Collectors.toSet());

        List<Cohort> unenrolledCohorts = availableCohorts.stream()
                .filter(c -> !enrolledCohortIds.contains(c.getId()))
                .collect(java.util.stream.Collectors.toList());

        if (unenrolledCohorts.isEmpty()) {
            redirectAttributes.addFlashAttribute("info", "Bạn đã tham gia tất cả các module của khóa học này.");
            Long firstModId = availableCohorts.get(0).getId();
            return "redirect:/courses/" + slug + "/modules/" + firstModId + "/learn";
        }

        java.math.BigDecimal sumOfUnenrolled = unenrolledCohorts.stream()
                .map(Cohort::getPrice)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.math.BigDecimal totalPrice = (unenrolledCohorts.size() == availableCohorts.size() 
                && course.getPrice() != null 
                && course.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0)
                ? course.getPrice()
                : (course.getPrice() != null && course.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0
                    ? sumOfUnenrolled.min(course.getPrice())
                    : sumOfUnenrolled);

        if (totalPrice.compareTo(java.math.BigDecimal.ZERO) > 0) {
            return "redirect:/checkout/course/" + slug;
        }

        for (Cohort c : unenrolledCohorts) {
            if (c.getCurrentEnrolled() < c.getMaxCapacity()) {
                CohortMember member = CohortMember.builder()
                        .user(user)
                        .cohort(c)
                        .build();
                cohortMemberRepository.save(member);
                c.setCurrentEnrolled((int) cohortMemberRepository.countByCohort_Id(c.getId()));
                cohortRepository.save(c);
            }
        }

        log.info("[CourseController] Đăng ký miễn phí trọn gói course: {}, user: {}", slug, user.getEmail());
        redirectAttributes.addFlashAttribute("success", "Đã đăng ký toàn bộ khóa học thành công!");
        Long targetId = unenrolledCohorts.get(0).getId();
        return "redirect:/courses/" + slug + "/modules/" + targetId + "/learn";
    }

    @Transactional(readOnly = true)
    @GetMapping({"/{slug}/modules/{moduleId}/learn", "/{slug}/module/{moduleId}/learn"})
    public String showModuleLearn(@PathVariable String slug,
                                  @PathVariable Long moduleId,
                                  @org.springframework.web.bind.annotation.RequestParam(name = "lesson", required = false) Long queryLessonId,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }

        Cohort module = cohortRepository.findById(moduleId).orElse(null);
        if (module == null) {
            return "redirect:/cohorts";
        }

        Course course = resolveCourse(slug);
        if (course == null || "standalone".equalsIgnoreCase(slug) || (module.getCourse() != null && !module.getCourse().getId().equals(course.getId()))) {
            if (module.getCourse() != null) {
                course = module.getCourse();
            } else {
                course = Course.builder()
                        .id(0L)
                        .title(module.getName())
                        .slug("standalone")
                        .code(module.getCode())
                        .price(module.getPrice())
                        .createdBy(module.getCreatedBy())
                        .build();
            }
        }

        User user = currentUserOpt.get();
        boolean isEnrolled = cohortMemberRepository.existsByCohort_IdAndUser_Id(module.getId(), user.getId());
        boolean hasAccessRole = checkHasAccessRole(user, course, module);
        boolean canEdit = checkCanEdit(user, course, module);

        if (!isEnrolled && !hasAccessRole) {
            redirectAttributes.addFlashAttribute("error", "Bạn cần đăng ký module này để vào phòng học.");
            return "redirect:/courses/" + slug + "/modules/" + moduleId;
        }

        List<Section> cohortSections = sectionService != null
                ? sectionService.getSectionsByCohortId(module.getId())
                : sectionRepository.findSectionsForCohort(module.getId());
        List<CohortSchedule> schedules = cohortScheduleRepository.findByCohortIdWithDetails(module.getId());
        java.util.Set<Long> validSectionIds = cohortSections.stream().map(Section::getId).collect(java.util.stream.Collectors.toSet());
        if (!validSectionIds.isEmpty()) {
            schedules = schedules.stream()
                    .filter(cs -> cs.getLesson() != null && cs.getLesson().getSection() != null && validSectionIds.contains(cs.getLesson().getSection().getId()))
                    .toList();
        }
        List<Lesson> rawLessons;
        if (!schedules.isEmpty()) {
            rawLessons = schedules.stream().map(CohortSchedule::getLesson).toList();
        } else {
            rawLessons = lessonRepository.findBySectionCourseIdOrderByOrderIndexAsc(course.getId());
        }
        if (!hasAccessRole) {
            rawLessons = rawLessons.stream().filter(Lesson::isPublished).toList();
        }

        List<Lesson> lessons = new java.util.ArrayList<>(rawLessons);
        lessons.sort((a, b) -> {
            int secOrderA = (a.getSection() != null && a.getSection().getOrderIndex() != null) ? a.getSection().getOrderIndex() : 0;
            int secOrderB = (b.getSection() != null && b.getSection().getOrderIndex() != null) ? b.getSection().getOrderIndex() : 0;
            if (secOrderA != secOrderB) return Integer.compare(secOrderA, secOrderB);

            long secIdA = (a.getSection() != null && a.getSection().getId() != null) ? a.getSection().getId() : 0L;
            long secIdB = (b.getSection() != null && b.getSection().getId() != null) ? b.getSection().getId() : 0L;
            if (secIdA != secIdB) return Long.compare(secIdA, secIdB);

            int lOrderA = a.getOrderIndex() != null ? a.getOrderIndex() : 0;
            int lOrderB = b.getOrderIndex() != null ? b.getOrderIndex() : 0;
            if (lOrderA != lOrderB) return Integer.compare(lOrderA, lOrderB);

            return Long.compare(a.getId() != null ? a.getId() : 0L, b.getId() != null ? b.getId() : 0L);
        });

        java.util.Map<Long, CohortSchedule> scheduleMap = new java.util.HashMap<>();
        for (CohortSchedule cs : schedules) {
            if (cs.getLesson() != null) {
                scheduleMap.put(cs.getLesson().getId(), cs);
            }
        }

        final boolean finalHasAccessRole = hasAccessRole;
        List<java.util.Map<String, Object>> lessonDtos = lessons.stream().map(l -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            CohortSchedule cs = scheduleMap.get(l.getId());
            boolean isLocked = !finalHasAccessRole && cs != null && cs.isLockedNow();

            map.put("id", l.getId());
            map.put("title", l.getTitle() != null ? l.getTitle() : "");
            map.put("lessonType", l.getLessonType() != null ? l.getLessonType() : "VIDEO");
            map.put("durationSeconds", l.getDurationSeconds() != null ? l.getDurationSeconds() : 0);
            map.put("videoUrl", isLocked ? null : l.getVideoUrl());
            map.put("documentContent", isLocked ? null : l.getDocumentContent());
            map.put("assetUrl", isLocked ? null : l.getAssetUrl());
            map.put("sectionId", (l.getSection() != null) ? l.getSection().getId() : null);
            map.put("sectionTitle", (l.getSection() != null && l.getSection().getTitle() != null) ? l.getSection().getTitle() : "Nội dung học tập");
            map.put("sectionOrder", (l.getSection() != null && l.getSection().getOrderIndex() != null) ? l.getSection().getOrderIndex() : 0);
            map.put("orderIndex", l.getOrderIndex() != null ? l.getOrderIndex() : 0);
            map.put("isLocked", isLocked);
            map.put("unlockAt", (cs != null) ? cs.getFormattedUnlockAt() : null);
            map.put("unlockAtDisplay", (cs != null) ? cs.getDisplayUnlockAt() : null);
            map.put("deadlineAt", (cs != null) ? cs.getFormattedDeadlineAt() : null);
            map.put("deadlineAtDisplay", (cs != null) ? cs.getDisplayDeadlineAt() : null);
            map.put("isDeadlinePassed", (cs != null) && cs.isDeadlinePassed());
            return map;
        }).toList();

        List<Long> completedLessonIds = cohortService.getCompletedLessonIds(module.getId(), user.getId());
        int totalLessonCount = lessons.size();
        int completedCount = completedLessonIds.size();
        int progressPercent = (totalLessonCount > 0) ? (int) Math.min(100, Math.round(((double) completedCount / totalLessonCount) * 100)) : 0;

        List<CohortMember> userMemberships = cohortMemberRepository.findByCourseIdAndUserIdWithCohort(course.getId(), user.getId());
        List<java.util.Map<String, Object>> otherEnrolledModules = userMemberships.stream()
                .map(CohortMember::getCohort)
                .filter(c -> !c.getId().equals(module.getId()))
                .map(c -> {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", c.getId());
                    map.put("code", c.getCode() != null ? c.getCode() : "");
                    map.put("name", c.getName() != null ? c.getName() : "");
                    return map;
                })
                .toList();

        Optional<CohortMember> userMemberOpt = cohortMemberRepository.findByCohort_IdAndUser_Id(module.getId(), user.getId());
        Long savedLastLessonId = userMemberOpt
                .map(CohortMember::getLastLesson)
                .map(Lesson::getId)
                .orElse(null);
        Long initialLessonId = queryLessonId != null ? queryLessonId : savedLastLessonId;

        boolean isAdmin = vn.edu.ute.utelearn.util.SecurityUtils.isAdmin(user) || (rbacHelper != null && rbacHelper.hasAnyRole("ADMIN", "ROLE_ADMIN"));
        boolean isInstructor = vn.edu.ute.utelearn.util.SecurityUtils.isInstructor(user) || (rbacHelper != null && rbacHelper.hasAnyRole("INSTRUCTOR", "ROLE_INSTRUCTOR", "TEACHER"));
        boolean isModerator = vn.edu.ute.utelearn.util.SecurityUtils.isModerator(user) || (rbacHelper != null && rbacHelper.hasAnyRole("MODERATOR", "ROLE_MODERATOR"));

        model.addAttribute("currentUser", user);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isInstructor", isInstructor);
        model.addAttribute("isModerator", isModerator);
        model.addAttribute("course", course);
        model.addAttribute("module", module);
        model.addAttribute("moduleLessons", lessons);
        model.addAttribute("lessonDtos", lessonDtos);
        model.addAttribute("completedLessonIds", completedLessonIds);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("progressPercent", progressPercent);
        model.addAttribute("hasAccessRole", hasAccessRole);
        model.addAttribute("canEdit", canEdit);
        model.addAttribute("otherEnrolledModules", otherEnrolledModules);
        model.addAttribute("initialLessonId", initialLessonId);
        userMemberOpt.ifPresent(cm -> model.addAttribute("currentMember", cm));
        return "courses/module-learn";
    }

    @Transactional(readOnly = true)
    @GetMapping({"/{slug}/learn", "/{slug}/learning"})
    public String showCourseContent(@PathVariable String slug,
            @RequestParam(required = false) Long lesson,
            RedirectAttributes redirectAttributes) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return "redirect:/login";
        }

        Course course = resolveCourse(slug);
        if (course == null) {
            return "redirect:/courses";
        }

        User user = currentUserOpt.get();
        List<CohortMember> userMemberships = cohortMemberRepository.findByCourseIdAndUserIdWithCohort(course.getId(), user.getId());
        if (!userMemberships.isEmpty()) {
            CohortMember latestMember = userMemberships.stream()
                    .sorted((a, b) -> {
                        if (a.getLastAccessedAt() == null && b.getLastAccessedAt() == null) {
                            return 0;
                        }
                        if (a.getLastAccessedAt() == null) return 1;
                        if (b.getLastAccessedAt() == null) return -1;
                        return b.getLastAccessedAt().compareTo(a.getLastAccessedAt());
                    })
                    .findFirst()
                    .orElse(userMemberships.get(0));

            Long targetModuleId = latestMember.getCohort().getId();
            Long targetLessonId = (lesson != null) ? lesson : (latestMember.getLastLesson() != null ? latestMember.getLastLesson().getId() : null);
            if (targetLessonId != null) {
                return "redirect:/courses/" + slug + "/modules/" + targetModuleId + "/learn?lesson=" + targetLessonId;
            }
            return "redirect:/courses/" + slug + "/modules/" + targetModuleId + "/learn";
        }

        boolean hasAccessRole = checkHasAccessRole(user, course, null);
        if (hasAccessRole) {
            List<Cohort> cohorts = cohortRepository.findByCourseIdOrderByCreatedAtAsc(course.getId()).stream()
                    .filter(c -> c != null && !c.isFullCourseCohort())
                    .toList();
            if (cohorts.isEmpty()) {
                cohorts = cohortRepository.findByCourseIdOrdered(course.getId()).stream()
                        .filter(c -> c != null && !c.isFullCourseCohort())
                        .toList();
            }
            if (cohorts.isEmpty()) {
                cohorts = cohortRepository.findByCourseIdOrdered(course.getId());
            }
            if (cohorts.isEmpty() && lesson != null) {
                List<CohortSchedule> schedules = cohortScheduleRepository.findByLessonId(lesson);
                if (!schedules.isEmpty() && schedules.get(0).getCohort() != null) {
                    return "redirect:/courses/" + slug + "/modules/" + schedules.get(0).getCohort().getId() + "/learn?lesson=" + lesson;
                }
            }
            if (!cohorts.isEmpty()) {
                String lessonParam = lesson != null ? "?lesson=" + lesson : "";
                return "redirect:/courses/" + slug + "/modules/" + cohorts.get(0).getId() + "/learn" + lessonParam;
            }
        }

        redirectAttributes.addFlashAttribute("info", "Bạn chưa đăng ký module nào của khóa học này.");
        return "redirect:/courses/" + slug;
    }

    @GetMapping(value = {"/{courseIdOrSlug}/grades/json", "/api/{courseIdOrSlug}/grades"}, produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> getStudentCourseGradesJson(@PathVariable("courseIdOrSlug") String courseIdOrSlug) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        if (currentUserOpt.isEmpty()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Vui lòng đăng nhập để xem bảng điểm."));
        }
        User user = currentUserOpt.get();
        Course course = resolveCourse(courseIdOrSlug);
        if (course == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Không tìm thấy khóa học."));
        }
        if (gradebookService == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("success", false, "message", "Dịch vụ bảng điểm tạm thời không khả dụng."));
        }
        try {
            vn.edu.ute.utelearn.dto.StudentCourseGradeDTO grades = gradebookService.getStudentCourseGrade(course.getId(), user.getId());
            return ResponseEntity.ok(grades);
        } catch (Exception e) {
            log.error("[CourseController] Lỗi khi lấy bảng điểm cho user {}: {}", user.getId(), e.getMessage());
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Đã xảy ra lỗi khi lấy bảng điểm: " + e.getMessage()));
        }
    }

    private boolean checkHasAccessRole(User user, Course course, Cohort module) {
        if (user == null) {
            return false;
        }
        if (checkCanEdit(user, course, module)) {
            return true;
        }
        boolean hasRbacRole = rbacHelper != null && rbacHelper.hasAnyRole("MODERATOR", "ROLE_MODERATOR", "INSTRUCTOR", "ROLE_INSTRUCTOR", "TEACHER", "ADMIN", "ROLE_ADMIN");
        boolean hasEntityRole = user.getRoles() != null && user.getRoles().stream().anyMatch(r -> {
            String c = r.getCode();
            String n = r.getName();
            return "MODERATOR".equalsIgnoreCase(c) || "ROLE_MODERATOR".equalsIgnoreCase(c)
                    || "INSTRUCTOR".equalsIgnoreCase(c) || "ROLE_INSTRUCTOR".equalsIgnoreCase(c)
                    || "TEACHER".equalsIgnoreCase(c) || "ADMIN".equalsIgnoreCase(c) || "ROLE_ADMIN".equalsIgnoreCase(c)
                    || "MODERATOR".equalsIgnoreCase(n) || "INSTRUCTOR".equalsIgnoreCase(n);
        });
        return hasRbacRole || hasEntityRole;
    }

    private boolean checkCanEdit(User user, Course course, Cohort module) {
        if (user == null) {
            return false;
        }
        boolean isOwner = (course != null && course.getCreatedBy() != null && course.getCreatedBy().getId().equals(user.getId()))
                || (module != null && module.getCreatedBy() != null && module.getCreatedBy().getId().equals(user.getId()));
        boolean hasAdminRole = (rbacHelper != null && rbacHelper.hasAnyRole("ADMIN", "ROLE_ADMIN"))
                || (user.getRoles() != null && user.getRoles().stream().anyMatch(r ->
                "ADMIN".equalsIgnoreCase(r.getCode()) || "ROLE_ADMIN".equalsIgnoreCase(r.getCode())
                        || "ADMIN".equalsIgnoreCase(r.getName())));
        return isOwner || hasAdminRole;
    }
}

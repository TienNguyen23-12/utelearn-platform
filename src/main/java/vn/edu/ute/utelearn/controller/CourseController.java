package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
import vn.edu.ute.utelearn.entity.User;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.List;

@Slf4j
@Controller
@Transactional
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

    private Course resolveCourse(String slugOrId) {
        if (slugOrId == null || slugOrId.isBlank()) {
            return null;
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
        if (cohortOpt.isPresent() && cohortOpt.get().getCourse() != null) {
            String courseKey = (cohortOpt.get().getCourse().getSlug() != null && !cohortOpt.get().getCourse().getSlug().isBlank())
                    ? cohortOpt.get().getCourse().getSlug()
                    : String.valueOf(cohortOpt.get().getCourse().getId());
            return "redirect:/courses/" + courseKey + "/modules/" + moduleId + "/learn";
        }
        return "redirect:/cohorts";
    }

    @GetMapping({"/modules/{moduleId}", "/module/{moduleId}"})
    public String showModuleDetailDirect(@PathVariable Long moduleId) {
        Optional<Cohort> cohortOpt = cohortRepository.findById(moduleId);
        if (cohortOpt.isPresent() && cohortOpt.get().getCourse() != null) {
            String courseKey = (cohortOpt.get().getCourse().getSlug() != null && !cohortOpt.get().getCourse().getSlug().isBlank())
                    ? cohortOpt.get().getCourse().getSlug()
                    : String.valueOf(cohortOpt.get().getCourse().getId());
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

    @GetMapping
    public String showCourseList(Model model) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        currentUserOpt.ifPresent(user -> model.addAttribute("currentUser", user));
        return "courses/index";
    }

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
        java.util.Set<Long> enrolledCohortIds = new java.util.HashSet<>();
        if (currentUserOpt.isPresent()) {
            User user = currentUserOpt.get();
            model.addAttribute("currentUser", user);
            List<CohortMember> userMemberships = cohortMemberRepository.findByCohort_Course_IdAndUser_Id(course.getId(), user.getId());
            enrolledCohortIds = userMemberships.stream().map(cm -> cm.getCohort().getId()).collect(java.util.stream.Collectors.toSet());
            isEnrolled = !enrolledCohortIds.isEmpty();
            hasAccessRole = user.getRoles().stream().anyMatch(r -> r.getCode().equals("ADMIN") ||
                    r.getCode().equals("MODERATOR") ||
                    r.getCode().equals("INSTRUCTOR"));
        }

        List<Cohort> availableCohorts = cohortRepository.findByCourseIdAndStatusIn(course.getId(),
                List.of("ENROLLING", "UPCOMING"));
        final java.util.Set<Long> finalEnrolledCohortIds = enrolledCohortIds;
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

        model.addAttribute("availableCohorts", cohortList);
        model.addAttribute("courseSlug", slug);
        model.addAttribute("isEnrolled", isEnrolled);
        model.addAttribute("isAllEnrolled", isAllEnrolled);
        model.addAttribute("fullCoursePrice", fullCoursePrice);
        model.addAttribute("sumOfCohorts", sumOfCohorts);
        model.addAttribute("discountAmount", discountAmount);
        model.addAttribute("remainingCoursePrice", remainingCoursePrice);
        model.addAttribute("unenrolledCount", unenrolledCount);
        model.addAttribute("hasAccessRole", hasAccessRole);

        return "courses/detail";
    }

    @GetMapping({"/{slug}/modules/{moduleId}", "/{slug}/module/{moduleId}"})
    public String showModuleDetail(@PathVariable String slug, @PathVariable Long moduleId, Model model) {
        Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
        currentUserOpt.ifPresent(user -> model.addAttribute("currentUser", user));

        Course course = resolveCourse(slug);
        if (course == null) {
            return "redirect:/courses";
        }

        Cohort module = cohortRepository.findById(moduleId).orElse(null);
        if (module == null || (module.getCourse() != null && !module.getCourse().getId().equals(course.getId()))) {
            return "redirect:/courses/" + slug;
        }

        boolean isModEnrolled = false;
        boolean hasAccessRole = false;
        if (currentUserOpt.isPresent()) {
            User user = currentUserOpt.get();
            isModEnrolled = cohortMemberRepository.existsByCohort_IdAndUser_Id(module.getId(), user.getId());
            hasAccessRole = user.getRoles().stream().anyMatch(r -> r.getCode().equals("ADMIN") ||
                    r.getCode().equals("MODERATOR") ||
                    r.getCode().equals("INSTRUCTOR"));
        }

        long enrolledCount = cohortMemberRepository.countByCohort_Id(module.getId());
        module.setCurrentEnrolled((int) enrolledCount);

        List<CohortSchedule> schedules = cohortScheduleRepository.findByCohortIdWithDetails(module.getId());
        List<Lesson> moduleLessons;
        if (!schedules.isEmpty()) {
            moduleLessons = schedules.stream().map(CohortSchedule::getLesson).toList();
        } else {
            moduleLessons = lessonRepository.findBySectionCourseIdOrderByOrderIndexAsc(course.getId());
        }

        List<java.util.Map<String, Object>> moduleSections = new java.util.ArrayList<>();
        java.util.Map<Long, java.util.Map<String, Object>> sectionMap = new java.util.LinkedHashMap<>();
        java.util.Map<String, Object> fallbackSection = null;

        for (Lesson l : moduleLessons) {
            if (l.getSection() != null) {
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

        model.addAttribute("course", course);
        model.addAttribute("module", module);
        model.addAttribute("moduleLessons", moduleLessons);
        model.addAttribute("moduleSections", moduleSections);
        model.addAttribute("isModEnrolled", isModEnrolled);
        model.addAttribute("hasAccessRole", hasAccessRole);
        return "courses/module-detail";
    }

    @PostMapping("/{slug}/enroll/{cohortId}")
    public String enrollCourse(@PathVariable String slug, @PathVariable Long cohortId,
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

        Optional<Cohort> cohortOpt = cohortRepository.findById(cohortId);
        if (cohortOpt.isEmpty() || !cohortOpt.get().getCourse().getId().equals(course.getId())) {
            redirectAttributes.addFlashAttribute("error", "Lớp học không tồn tại hoặc không thuộc khóa học này.");
            return "redirect:/courses/" + slug;
        }

        Cohort activeCohort = cohortOpt.get();
        if (cohortMemberRepository.existsByCohort_IdAndUser_Id(activeCohort.getId(), user.getId())) {
            redirectAttributes.addFlashAttribute("info", "Bạn đã tham gia đợt học này rồi.");
            return "redirect:/courses/" + slug + "/modules/" + activeCohort.getId() + "/learn";
        }

        if (List.of("ENROLLING", "UPCOMING").contains(activeCohort.getStatus())) {
            if (activeCohort.getCurrentEnrolled() >= activeCohort.getMaxCapacity()) {
                redirectAttributes.addFlashAttribute("error", "Lớp học đã đủ số lượng học viên.");
                return "redirect:/courses/" + slug;
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
            return "redirect:/courses/" + slug;
        }

        return "redirect:/courses/" + slug + "/modules/" + activeCohort.getId() + "/learn";
    }

    @PostMapping("/{slug}/enroll-all")
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
        List<Cohort> availableCohorts = cohortRepository.findByCourseIdAndStatusIn(course.getId(),
                List.of("ENROLLING", "UPCOMING"));

        if (availableCohorts.isEmpty()) {
            Cohort defaultCohort = Cohort.builder()
                    .course(course)
                    .name("Toàn bộ khóa học: " + course.getTitle())
                    .code(course.getCode() + "-ALL")
                    .price(course.getPrice() != null ? course.getPrice() : java.math.BigDecimal.ZERO)
                    .status("ENROLLING")
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

        Course course = resolveCourse(slug);
        if (course == null) {
            return "redirect:/courses";
        }

        Cohort module = cohortRepository.findById(moduleId).orElse(null);
        if (module == null || (module.getCourse() != null && !module.getCourse().getId().equals(course.getId()))) {
            return "redirect:/courses/" + slug;
        }

        User user = currentUserOpt.get();
        boolean isEnrolled = cohortMemberRepository.existsByCohort_IdAndUser_Id(module.getId(), user.getId());
        boolean hasAccessRole = user.getRoles().stream().anyMatch(r -> r.getCode().equals("ADMIN") ||
                r.getCode().equals("MODERATOR") ||
                r.getCode().equals("INSTRUCTOR"));

        if (!isEnrolled && !hasAccessRole) {
            redirectAttributes.addFlashAttribute("error", "Bạn cần đăng ký module này để vào phòng học.");
            return "redirect:/courses/" + slug + "/modules/" + moduleId;
        }

        List<CohortSchedule> schedules = cohortScheduleRepository.findByCohortIdWithDetails(module.getId());
        List<Lesson> lessons;
        if (!schedules.isEmpty()) {
            lessons = schedules.stream().map(CohortSchedule::getLesson).toList();
        } else {
            lessons = lessonRepository.findBySectionCourseIdOrderByOrderIndexAsc(course.getId());
        }

        List<java.util.Map<String, Object>> lessonDtos = lessons.stream().map(l -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", l.getId());
            map.put("title", l.getTitle() != null ? l.getTitle() : "");
            map.put("lessonType", l.getLessonType() != null ? l.getLessonType() : "VIDEO");
            map.put("durationSeconds", l.getDurationSeconds() != null ? l.getDurationSeconds() : 0);
            map.put("videoUrl", l.getVideoUrl());
            map.put("documentContent", l.getDocumentContent());
            map.put("assetUrl", l.getAssetUrl());
            map.put("sectionId", (l.getSection() != null) ? l.getSection().getId() : null);
            map.put("sectionTitle", (l.getSection() != null && l.getSection().getTitle() != null) ? l.getSection().getTitle() : "Nội dung học tập");
            return map;
        }).toList();

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

        boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getCode()));
        boolean isInstructor = user.getRoles().stream().anyMatch(r -> "INSTRUCTOR".equals(r.getCode()) || "TEACHER".equals(r.getCode()));
        boolean isModerator = user.getRoles().stream().anyMatch(r -> "MODERATOR".equals(r.getCode()));

        model.addAttribute("currentUser", user);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isInstructor", isInstructor);
        model.addAttribute("isModerator", isModerator);
        model.addAttribute("course", course);
        model.addAttribute("module", module);
        model.addAttribute("moduleLessons", lessons);
        model.addAttribute("lessonDtos", lessonDtos);
        model.addAttribute("otherEnrolledModules", otherEnrolledModules);
        model.addAttribute("initialLessonId", initialLessonId);
        return "courses/module-learn";
    }

    @GetMapping({"/{slug}/learn", "/{slug}/learning"})
    public String showCourseContent(@PathVariable String slug, RedirectAttributes redirectAttributes) {
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
            if (latestMember.getLastLesson() != null) {
                return "redirect:/courses/" + slug + "/modules/" + targetModuleId + "/learn?lesson=" + latestMember.getLastLesson().getId();
            }
            return "redirect:/courses/" + slug + "/modules/" + targetModuleId + "/learn";
        }

        boolean hasAccessRole = user.getRoles().stream().anyMatch(r -> r.getCode().equals("ADMIN") ||
                r.getCode().equals("MODERATOR") ||
                r.getCode().equals("INSTRUCTOR"));
        if (hasAccessRole) {
            List<Cohort> cohorts = cohortRepository.findByCourseIdOrderByCreatedAtAsc(course.getId());
            if (!cohorts.isEmpty()) {
                return "redirect:/courses/" + slug + "/modules/" + cohorts.get(0).getId() + "/learn";
            }
        }

        redirectAttributes.addFlashAttribute("info", "Bạn chưa đăng ký module nào của khóa học này.");
        return "redirect:/courses/" + slug;
    }
}

package vn.edu.ute.utelearn.controller.instructor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.CohortSchedule;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.CourseService;
import vn.edu.ute.utelearn.service.CategoryService;
import vn.edu.ute.utelearn.service.SectionService;
import vn.edu.ute.utelearn.service.LessonService;
import vn.edu.ute.utelearn.service.ModerationService;
import vn.edu.ute.utelearn.service.CohortService;
import vn.edu.ute.utelearn.service.GradebookService;
import vn.edu.ute.utelearn.dto.CourseGradebookDTO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import vn.edu.ute.utelearn.entity.Quiz;
import vn.edu.ute.utelearn.entity.Question;
import vn.edu.ute.utelearn.entity.Assignment;
import vn.edu.ute.utelearn.dao.QuizRepository;
import vn.edu.ute.utelearn.dao.QuestionRepository;
import vn.edu.ute.utelearn.dao.AssignmentRepository;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.CohortScheduleRepository;
import vn.edu.ute.utelearn.dao.LessonRepository;
import vn.edu.ute.utelearn.dto.CourseRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.time.Instant;

@Slf4j
@Controller
@RequestMapping("/instructor/courses")
@RequiredArgsConstructor
public class InstructorCourseController {

    private final CourseService courseService;
    private final CategoryService categoryService;
    private final AuthService authService;
    private final SectionService sectionService;
    private final LessonService lessonService;
    private final ModerationService moderationService;
    private final CohortRepository cohortRepository;
    private final CohortScheduleRepository cohortScheduleRepository;
    private final LessonRepository lessonRepository;
    private final vn.edu.ute.utelearn.dao.SectionRepository sectionRepository;
    private final vn.edu.ute.utelearn.dao.CohortMemberRepository cohortMemberRepository;
    private final CohortService cohortService;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final AssignmentRepository assignmentRepository;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private GradebookService gradebookService;

    @GetMapping
    public String listCourses(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              Model model) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow(() -> new RuntimeException("Unauthorized"));
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        boolean isAdmin = vn.edu.ute.utelearn.util.SecurityUtils.isAdmin(currentUser);
        Page<Course> coursePage = isAdmin
                ? courseService.getAllCourses(pageable)
                : courseService.getCoursesByInstructor(currentUser.getId(), pageable);
        
        model.addAttribute("courses", coursePage.getContent());
        model.addAttribute("currentPage", coursePage.getNumber());
        model.addAttribute("totalPages", coursePage.getTotalPages());
        model.addAttribute("totalItems", coursePage.getTotalElements());
        return "instructor/courses/index";
    }

    @GetMapping("/create")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public String showCreateForm(Model model) {
        model.addAttribute("categories", categoryService.getRootCategories());
        model.addAttribute("isEdit", false);
        return "instructor/courses/form";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public String showEditForm(@PathVariable Long id, Model model) {
        Course course = courseService.getCourseById(id);
        
        String objectivesText = "";
        try {
            List<String> objs = new ObjectMapper().readValue(course.getObjectives(), new TypeReference<List<String>>(){});
            objectivesText = String.join("\n", objs);
        } catch (Exception e) {}

        model.addAttribute("course", course);
        model.addAttribute("courseObjectives", objectivesText);
        model.addAttribute("categories", categoryService.getRootCategories());
        model.addAttribute("isEdit", true);
        return "instructor/courses/form";
    }

    @PostMapping("/create")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public String createCourse(@Valid @ModelAttribute("course") CourseRequestDTO request, 
                               BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("courseObjectives", request.getObjectives());
            model.addAttribute("categories", categoryService.getRootCategories());
            model.addAttribute("isEdit", false);
            return "instructor/courses/form";
        }
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        try {
            courseService.createCourse(request, currentUser.getId());
            log.info("[InstructorCourseController] Tạo khóa học mới: {}, user: {}", request.getTitle(), currentUser.getEmail());
            redirectAttributes.addFlashAttribute("successMessage", "Tạo khóa học thành công");
            return "redirect:/instructor/courses";
        } catch (Exception e) {
            log.warn("[InstructorCourseController] Lỗi khi tạo khóa học: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("courseObjectives", request.getObjectives());
            model.addAttribute("categories", categoryService.getRootCategories());
            model.addAttribute("isEdit", false);
            return "instructor/courses/form";
        }
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public String updateCourse(@PathVariable Long id, @Valid @ModelAttribute("course") CourseRequestDTO request, 
                               BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.getRootCategories());
            model.addAttribute("isEdit", true);
            return "instructor/courses/form";
        }
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        try {
            courseService.updateCourse(id, request, currentUser.getId());
            log.info("[InstructorCourseController] Cập nhật khóa học id: {}, user: {}", id, currentUser.getEmail());
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật khóa học thành công");
            return "redirect:/instructor/courses";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("categories", categoryService.getRootCategories());
            model.addAttribute("isEdit", true);
            return "instructor/courses/form";
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public String deleteCourse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        courseService.deleteCourse(id, currentUser.getId());
        log.info("[InstructorCourseController] Xóa khóa học id: {}, user: {}", id, currentUser.getEmail());
        redirectAttributes.addFlashAttribute("successMessage", "Xóa khóa học thành công");
        return "redirect:/instructor/courses";
    }

    @GetMapping("/{id}/curriculum")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String showCurriculum(@PathVariable Long id, Model model) {
        Course course = courseService.getCourseById(id);
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        
        boolean isOwner = course.getCreatedBy() != null && course.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getCode()) || "ROLE_ADMIN".equals(r.getCode()));
        if (!isOwner && !isAdmin) {
            return "redirect:/instructor/courses";
        }

        List<vn.edu.ute.utelearn.entity.Section> sections = sectionService.getSectionsByCourseId(id);
        List<Cohort> modules = cohortRepository.findByCourseIdOrdered(id).stream()
                .filter(c -> c != null && !c.isFullCourseCohort())
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        Map<Long, List<vn.edu.ute.utelearn.entity.Section>> sectionsByCohort = new LinkedHashMap<>();
        List<vn.edu.ute.utelearn.entity.Section> standaloneSections = new ArrayList<>();
        for (vn.edu.ute.utelearn.entity.Section s : sections) {
            if (s.getCohort() != null) {
                sectionsByCohort.computeIfAbsent(s.getCohort().getId(), k -> new ArrayList<>()).add(s);
            } else {
                standaloneSections.add(s);
            }
        }

        for (Map.Entry<Long, List<vn.edu.ute.utelearn.entity.Section>> entry : sectionsByCohort.entrySet()) {
            boolean alreadyInModules = modules.stream().anyMatch(m -> m.getId().equals(entry.getKey()));
            if (!alreadyInModules) {
                cohortRepository.findById(entry.getKey()).ifPresent(modules::add);
            }
        }

        List<Map<String, Object>> moduleGroups = new ArrayList<>();
        for (Cohort m : modules) {
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("module", m);
            group.put("sections", sectionsByCohort.getOrDefault(m.getId(), Collections.emptyList()));
            moduleGroups.add(group);
        }

        model.addAttribute("course", course);
        model.addAttribute("sections", sections);
        model.addAttribute("modules", modules);
        model.addAttribute("moduleGroups", moduleGroups);
        model.addAttribute("standaloneSections", standaloneSections);
        return "instructor/courses/curriculum";
    }

    @PostMapping("/{id}/submit-review")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public String submitForReview(@PathVariable Long id,
                                  @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
                                  RedirectAttributes redirectAttributes) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        Course course = courseService.getCourseById(id);
        if (!course.getCreatedBy().getId().equals(currentUser.getId())) {
            return "redirect:/instructor/courses";
        }
        
        moderationService.createCourseModerationTask(id, currentUser.getId());
        log.info("[InstructorCourseController] Gửi duyệt khóa học id: {}, user: {}", id, currentUser.getEmail());
        redirectAttributes.addFlashAttribute("successMessage", "Khóa học đã được gửi duyệt thành công. Vui lòng chờ Moderator kiểm duyệt.");
        if (redirectUrl != null && (redirectUrl.startsWith("/instructor/courses/") || redirectUrl.startsWith("/instructor/cohorts/"))) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/instructor/courses";
    }

    @GetMapping("/{id}/modules")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public String listCourseModules(@PathVariable Long id, Model model) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        Course course = courseService.getCourseById(id);
        if (!course.getCreatedBy().getId().equals(currentUser.getId())) {
            return "redirect:/instructor/courses";
        }

        List<Cohort> modules = cohortRepository.findByCourseIdOrderByCreatedAtAsc(id);
        Map<Long, Integer> lessonCountMap = new HashMap<>();
        for (Cohort m : modules) {
            m.setCurrentEnrolled((int) cohortMemberRepository.countByCohort_Id(m.getId()));
            lessonCountMap.put(m.getId(), cohortScheduleRepository.findByCohortId(m.getId()).size());
        }

        model.addAttribute("course", course);
        model.addAttribute("modules", modules);
        model.addAttribute("lessonCountMap", lessonCountMap);
        return "instructor/courses/modules";
    }

    @GetMapping("/{id}/modules/create")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public String showCreateModuleForm(
            @PathVariable Long id,
            @RequestParam(required = false) String keyword,
            Model model) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        Course course = courseService.getCourseById(id);
        boolean isOwner = course.getCreatedBy() != null && course.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getCode()) || "ROLE_ADMIN".equals(r.getCode()));
        if (!isOwner && !isAdmin) {
            return "redirect:/instructor/courses";
        }

        String safeKeyword = vn.edu.ute.utelearn.util.ValidationUtils.stripHtmlTags(keyword);
        List<Cohort> availableModules;
        if (safeKeyword != null && !safeKeyword.isBlank()) {
            availableModules = cohortRepository.searchShareableCohorts(currentUser.getId(), safeKeyword);
        } else {
            availableModules = cohortRepository.findShareableCohorts(currentUser.getId());
        }

        List<Cohort> existingCourseModules = cohortRepository.findByCourseIdOrderByCreatedAtAsc(id);
        java.util.Set<Long> existingModuleIds = existingCourseModules.stream()
                .map(Cohort::getId)
                .collect(java.util.stream.Collectors.toSet());

        model.addAttribute("course", course);
        model.addAttribute("modules", availableModules);
        model.addAttribute("existingModuleIds", existingModuleIds);
        model.addAttribute("keyword", safeKeyword != null ? safeKeyword : "");
        model.addAttribute("currentUser", currentUser);
        return "instructor/courses/module-add";
    }

    @PostMapping("/{id}/modules/add")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    @org.springframework.transaction.annotation.Transactional
    public String addModuleToCourse(
            @PathVariable Long id,
            @RequestParam Long moduleId,
            RedirectAttributes redirectAttributes) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        Course course = courseService.getCourseById(id);
        boolean isOwner = course.getCreatedBy() != null && course.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getCode()) || "ROLE_ADMIN".equals(r.getCode()));
        if (!isOwner && !isAdmin) {
            return "redirect:/instructor/courses";
        }

        Cohort sourceCohort = cohortRepository.findById(moduleId).orElseThrow(() -> new RuntimeException("Module not found"));
        boolean canUse = (sourceCohort.getCreatedBy() != null && sourceCohort.getCreatedBy().getId().equals(currentUser.getId()))
                || "PUBLIC".equalsIgnoreCase(sourceCohort.getVisibility())
                || isAdmin;
        if (!canUse) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền chia sẻ hoặc sử dụng module này.");
            return "redirect:/instructor/courses/" + id + "/modules/create";
        }

        if (sourceCohort.getCourse() != null && sourceCohort.getCourse().getId().equals(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Module này đã thuộc về khóa học hiện tại.");
            return "redirect:/instructor/courses/" + id + "/modules";
        }

        boolean isCreator = sourceCohort.getCreatedBy() != null && sourceCohort.getCreatedBy().getId().equals(currentUser.getId());
        if (sourceCohort.getCourse() == null && isCreator) {
            sourceCohort.setCourse(course);
            cohortRepository.save(sourceCohort);
            List<vn.edu.ute.utelearn.entity.Section> directSections = sectionRepository.findByCohortIdOrderByOrderIndexAsc(sourceCohort.getId());
            for (vn.edu.ute.utelearn.entity.Section s : directSections) {
                if (s.getCourse() == null) {
                    s.setCourse(course);
                    sectionRepository.save(s);
                }
            }
            log.info("[InstructorCourseController] Gán trực tiếp module độc lập id: {} vào course id: {}", sourceCohort.getId(), id);
            courseService.revertToDraftIfPublished(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm module '" + sourceCohort.getName() + "' vào khóa học thành công!");
            return "redirect:/instructor/courses/" + id + "/modules";
        }

        long count = cohortRepository.countByCourseId(id) + 1;
        String newCode = course.getCode() + "-MOD" + count;
        while (cohortRepository.existsByCode(newCode)) {
            count++;
            newCode = course.getCode() + "-MOD" + count;
        }

        String finalThumbnail = sourceCohort.getThumbnailUrl();
        if (finalThumbnail == null || finalThumbnail.trim().isEmpty()) {
            finalThumbnail = (course.getThumbnailUrl() != null && !course.getThumbnailUrl().isBlank())
                    ? course.getThumbnailUrl()
                    : "/images/courses/default-cover.png";
        } else {
            finalThumbnail = finalThumbnail.trim();
        }

        Cohort newCohort = Cohort.builder()
                .course(course)
                .createdBy(currentUser)
                .name(sourceCohort.getName())
                .code(newCode)
                .price(sourceCohort.getPrice() != null ? sourceCohort.getPrice() : java.math.BigDecimal.ZERO)
                .description(sourceCohort.getDescription())
                .objectives(sourceCohort.getObjectives())
                .announcementTitle(sourceCohort.getAnnouncementTitle())
                .announcementContent(sourceCohort.getAnnouncementContent())
                .thumbnailUrl(finalThumbnail)
                .status("ENROLLING")
                .moderationStatus("APPROVED")
                .visibility("COURSE_INTERNAL")
                .maxCapacity(sourceCohort.getMaxCapacity() != null ? sourceCohort.getMaxCapacity() : 100)
                .currentEnrolled(0)
                .enrollmentStart(Instant.now())
                .enrollmentEnd(sourceCohort.getEnrollmentEnd() != null ? sourceCohort.getEnrollmentEnd() : Instant.now().plus(365, java.time.temporal.ChronoUnit.DAYS))
                .studyStart(Instant.now())
                .studyEnd(sourceCohort.getStudyEnd() != null ? sourceCohort.getStudyEnd() : Instant.now().plus(365, java.time.temporal.ChronoUnit.DAYS))
                .gradingPolicy(sourceCohort.getGradingPolicy())
                .processWeight(sourceCohort.getProcessWeight())
                .finalWeight(sourceCohort.getFinalWeight())
                .minPassingGrade(sourceCohort.getMinPassingGrade())
                .minFinalExamScore(sourceCohort.getMinFinalExamScore())
                .build();
        Cohort savedCohort = cohortRepository.save(newCohort);
        log.info("[InstructorCourseController] Nhân bản module id: {} vào course id: {} với mã: {} và visibility=COURSE_INTERNAL", sourceCohort.getId(), id, newCode);

        List<CohortSchedule> sourceSchedules = cohortScheduleRepository.findByCohortId(sourceCohort.getId());
        java.util.Map<Long, CohortSchedule> scheduleByLessonId = sourceSchedules.stream()
                .filter(cs -> cs.getLesson() != null)
                .collect(java.util.stream.Collectors.toMap(cs -> cs.getLesson().getId(), cs -> cs, (a, b) -> a));

        List<vn.edu.ute.utelearn.entity.Section> sourceSections = sectionRepository.findSectionsForCohort(sourceCohort.getId());
        for (vn.edu.ute.utelearn.entity.Section s : sourceSections) {
            vn.edu.ute.utelearn.entity.Section newSection = vn.edu.ute.utelearn.entity.Section.builder()
                    .course(course)
                    .cohort(savedCohort)
                    .title(s.getTitle())
                    .orderIndex(s.getOrderIndex())
                    .build();
            vn.edu.ute.utelearn.entity.Section savedSection = sectionRepository.save(newSection);

            List<vn.edu.ute.utelearn.entity.Lesson> lessons = lessonRepository.findBySectionIdOrderByOrderIndexAsc(s.getId());
            for (vn.edu.ute.utelearn.entity.Lesson l : lessons) {
                if (!scheduleByLessonId.isEmpty() && !scheduleByLessonId.containsKey(l.getId()) && s.getCohort() == null) {
                    continue;
                }
                vn.edu.ute.utelearn.entity.Lesson newLesson = vn.edu.ute.utelearn.entity.Lesson.builder()
                        .section(savedSection)
                        .title(l.getTitle())
                        .lessonType(l.getLessonType())
                        .orderIndex(l.getOrderIndex())
                        .isFreePreview(l.getIsFreePreview())
                        .videoUrl(l.getVideoUrl())
                        .durationSeconds(l.getDurationSeconds())
                        .documentContent(l.getDocumentContent())
                        .assetUrl(l.getAssetUrl())
                        .isPublicForCohorts(l.getIsPublicForCohorts())
                        .status(l.getStatus())
                        .build();
                vn.edu.ute.utelearn.entity.Lesson savedLesson = lessonRepository.save(newLesson);

                CohortSchedule origSchedule = scheduleByLessonId.get(l.getId());
                CohortSchedule newSchedule = CohortSchedule.builder()
                        .cohort(savedCohort)
                        .lesson(savedLesson)
                        .unlockAt(origSchedule != null && origSchedule.getUnlockAt() != null ? origSchedule.getUnlockAt() : Instant.now())
                        .deadlineAt(origSchedule != null ? origSchedule.getDeadlineAt() : null)
                        .showScoreType(origSchedule != null && origSchedule.getShowScoreType() != null ? origSchedule.getShowScoreType() : "IMMEDIATELY")
                        .isScorePublished(origSchedule != null ? origSchedule.getIsScorePublished() : true)
                        .allowReviewAnswers(origSchedule != null ? origSchedule.getAllowReviewAnswers() : true)
                        .build();
                cohortScheduleRepository.save(newSchedule);

                if ("QUIZ".equalsIgnoreCase(l.getLessonType())) {
                    quizRepository.findByLessonId(l.getId()).ifPresent(origQuiz -> {
                        Quiz newQuiz = Quiz.builder()
                                .lesson(savedLesson)
                                .title(origQuiz.getTitle())
                                .durationMinutes(origQuiz.getDurationMinutes())
                                .passingScore(origQuiz.getPassingScore())
                                .maxAttempts(origQuiz.getMaxAttempts())
                                .shuffleQuestions(origQuiz.getShuffleQuestions())
                                .allowReview(origQuiz.getAllowReview())
                                .allowLateSubmission(origQuiz.getAllowLateSubmission())
                                .examRules(origQuiz.getExamRules())
                                .build();
                        Quiz savedQuiz = quizRepository.save(newQuiz);
                        List<Question> questions = questionRepository.findByQuizIdOrderByOrderIndexAscCreatedAtAsc(origQuiz.getId());
                        for (Question q : questions) {
                            Question newQ = Question.builder()
                                    .quiz(savedQuiz)
                                    .content(q.getContent())
                                    .questionType(q.getQuestionType())
                                    .points(q.getPoints())
                                    .explanation(q.getExplanation())
                                    .options(q.getOptions())
                                    .orderIndex(q.getOrderIndex())
                                    .build();
                            questionRepository.save(newQ);
                        }
                    });
                } else if ("ASSIGNMENT".equalsIgnoreCase(l.getLessonType())) {
                    assignmentRepository.findByLessonId(l.getId()).ifPresent(origAssign -> {
                        Assignment newAssign = Assignment.builder()
                                .lesson(savedLesson)
                                .title(origAssign.getTitle())
                                .instruction(origAssign.getInstruction())
                                .attachmentUrl(origAssign.getAttachmentUrl())
                                .maxScore(origAssign.getMaxScore())
                                .passingScore(origAssign.getPassingScore())
                                .allowFileSubmission(origAssign.getAllowFileSubmission())
                                .allowTextSubmission(origAssign.getAllowTextSubmission())
                                .allowedFileTypes(origAssign.getAllowedFileTypes())
                                .maxFileSizeMb(origAssign.getMaxFileSizeMb())
                                .allowLateSubmission(origAssign.getAllowLateSubmission())
                                .build();
                        assignmentRepository.save(newAssign);
                    });
                }
            }
        }

        log.info("[InstructorCourseController] Thêm module {} (id: {}) vào course id: {}", savedCohort.getName(), sourceCohort.getId(), id);
        courseService.revertToDraftIfPublished(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã thêm module '" + sourceCohort.getName() + "' vào khóa học thành công!");
        return "redirect:/instructor/courses/" + id + "/modules";
    }

    @PostMapping("/{id}/modules/create")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public String createModule(
            @PathVariable Long id,
            @RequestParam(required = false) Long moduleId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) java.math.BigDecimal price,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String objectives,
            @RequestParam(required = false) String thumbnailUrl,
            @RequestParam(required = false, defaultValue = "PUBLIC") String visibility,
            @RequestParam(required = false) List<Long> lessonIds,
            RedirectAttributes redirectAttributes) {
        if (moduleId != null) {
            return addModuleToCourse(id, moduleId, redirectAttributes);
        }

        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        Course course = courseService.getCourseById(id);
        if (!course.getCreatedBy().getId().equals(currentUser.getId())) {
            return "redirect:/instructor/courses";
        }

        String finalCode = code;
        if (finalCode == null || finalCode.isBlank()) {
            long count = cohortRepository.countByCourseId(id) + 1;
            finalCode = course.getCode() + "-MOD" + count;
            while (cohortRepository.existsByCode(finalCode)) {
                count++;
                finalCode = course.getCode() + "-MOD" + count;
            }
        } else {
            finalCode = finalCode.trim();
            if (cohortRepository.existsByCode(finalCode)) {
                log.warn("[InstructorCourseController] Mã module trùng: {}", finalCode);
                redirectAttributes.addFlashAttribute("errorMessage", "Mã module/lớp học đã tồn tại");
                return "redirect:/instructor/courses/" + id + "/modules/create";
            }
        }

        String finalThumbnail = thumbnailUrl;
        if (finalThumbnail == null || finalThumbnail.trim().isEmpty()) {
            finalThumbnail = (course.getThumbnailUrl() != null && !course.getThumbnailUrl().isBlank())
                    ? course.getThumbnailUrl()
                    : "/images/courses/default-cover.png";
        } else {
            finalThumbnail = finalThumbnail.trim();
        }

        Cohort cohort = Cohort.builder()
                .course(course)
                .createdBy(currentUser)
                .name(name)
                .code(finalCode)
                .price(price != null ? price : java.math.BigDecimal.ZERO)
                .description(description)
                .objectives(parseObjectives(objectives))
                .thumbnailUrl(finalThumbnail)
                .status("ENROLLING")
                .moderationStatus("APPROVED")
                .visibility(visibility != null && !visibility.isBlank() ? visibility : "PUBLIC")
                .maxCapacity(100)
                .currentEnrolled(0)
                .enrollmentStart(Instant.now())
                .enrollmentEnd(Instant.now().plus(365, java.time.temporal.ChronoUnit.DAYS))
                .studyStart(Instant.now())
                .studyEnd(Instant.now().plus(365, java.time.temporal.ChronoUnit.DAYS))
                .build();
        Cohort savedCohort = cohortRepository.save(cohort);

        if (lessonIds != null && !lessonIds.isEmpty()) {
            for (Long lessonId : lessonIds) {
                lessonRepository.findById(lessonId).ifPresent(lesson -> {
                    CohortSchedule schedule = CohortSchedule.builder()
                            .cohort(savedCohort)
                            .lesson(lesson)
                            .unlockAt(Instant.now())
                            .build();
                    cohortScheduleRepository.save(schedule);
                });
            }
        }

        log.info("[InstructorCourseController] Tạo module mới: {} cho course id: {}", name, id);
        courseService.revertToDraftIfPublished(id);
        redirectAttributes.addFlashAttribute("successMessage", "Tạo Module cho khóa học thành công!");
        return "redirect:/instructor/courses/" + id + "/modules";
    }

    @GetMapping("/{courseId}/modules/{moduleId}/edit")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String showEditModuleForm(@PathVariable Long courseId, @PathVariable Long moduleId, Model model) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        Cohort cohort = cohortRepository.findById(moduleId).orElseThrow(() -> new RuntimeException("Module not found"));
        Long actualCourseId = (cohort.getCourse() != null) ? cohort.getCourse().getId() : courseId;
        if (!actualCourseId.equals(courseId)) {
            return "redirect:/instructor/courses/" + actualCourseId + "/modules/" + moduleId + "/edit";
        }

        Course course = courseService.getCourseById(actualCourseId);
        boolean isOwner = course.getCreatedBy() != null && course.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getCode()) || "ROLE_ADMIN".equals(r.getCode()));
        if (!isOwner && !isAdmin) {
            return "redirect:/instructor/courses";
        }

        List<vn.edu.ute.utelearn.entity.Section> sections = sectionService.getSectionsByCourseId(actualCourseId);
        Map<Long, List<vn.edu.ute.utelearn.entity.Lesson>> sectionLessonsMap = new HashMap<>();
        for (vn.edu.ute.utelearn.entity.Section s : sections) {
            sectionLessonsMap.put(s.getId(), lessonService.getLessonsBySectionId(s.getId()));
        }

        List<Long> selectedLessonIds = cohortScheduleRepository.findByCohortId(moduleId).stream()
                .map(cs -> cs.getLesson().getId())
                .toList();

        String objectivesText = "";
        if (cohort.getObjectives() != null && !cohort.getObjectives().isBlank()) {
            List<String> objs = cohort.getParsedObjectives();
            objectivesText = String.join("\n", objs);
        }

        String creatorName = "Chưa xác định";
        if (cohort.getCreatedBy() != null && cohort.getCreatedBy().getFullName() != null) {
            creatorName = cohort.getCreatedBy().getFullName() + " (" + cohort.getCreatedBy().getUsername() + ")";
        } else if (course.getCreatedBy() != null && course.getCreatedBy().getFullName() != null) {
            creatorName = course.getCreatedBy().getFullName() + " (" + course.getCreatedBy().getUsername() + ")";
        }

        model.addAttribute("course", course);
        model.addAttribute("module", cohort);
        model.addAttribute("moduleObjectives", objectivesText);
        model.addAttribute("creatorName", creatorName);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("sections", sections);
        model.addAttribute("sectionLessonsMap", sectionLessonsMap);
        model.addAttribute("selectedLessonIds", selectedLessonIds);
        model.addAttribute("isEdit", true);
        return "instructor/courses/module-form";
    }

    @PostMapping("/{courseId}/modules/{moduleId}/edit")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public String updateModule(
            @PathVariable Long courseId,
            @PathVariable Long moduleId,
            @RequestParam String name,
            @RequestParam String code,
            @RequestParam(required = false) java.math.BigDecimal price,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String objectives,
            @RequestParam(required = false) String announcementTitle,
            @RequestParam(required = false) String announcementContent,
            @RequestParam(required = false) String thumbnailUrl,
            @RequestParam(required = false, defaultValue = "PUBLIC") String visibility,
            @RequestParam(required = false) List<Long> lessonIds,
            RedirectAttributes redirectAttributes) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        Cohort cohort = cohortRepository.findById(moduleId).orElseThrow(() -> new RuntimeException("Module not found"));
        Long actualCourseId = (cohort.getCourse() != null) ? cohort.getCourse().getId() : courseId;
        Course course = courseService.getCourseById(actualCourseId);
        boolean isOwner = course.getCreatedBy() != null && course.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getCode()) || "ROLE_ADMIN".equals(r.getCode()));
        if (!isOwner && !isAdmin) {
            return "redirect:/instructor/courses";
        }

        if (cohort.getCreatedBy() == null) {
            cohort.setCreatedBy(currentUser);
        }
        String trimmedCode = (code != null) ? code.trim() : "";
        if (!trimmedCode.isEmpty() && !trimmedCode.equals(cohort.getCode()) && cohortRepository.existsByCode(trimmedCode)) {
            log.warn("[InstructorCourseController] Mã module trùng khi cập nhật: {}", trimmedCode);
            redirectAttributes.addFlashAttribute("errorMessage", "Mã module/lớp học đã tồn tại");
            return "redirect:/instructor/courses/" + courseId + "/modules/" + moduleId + "/edit";
        }
        if (!trimmedCode.isEmpty()) {
            cohort.setCode(trimmedCode);
        }
        cohort.setName(name);
        cohort.setPrice(price != null ? price : java.math.BigDecimal.ZERO);
        cohort.setVisibility(visibility != null && !visibility.isBlank() ? visibility : "PUBLIC");
        cohort.setDescription(description);
        cohort.setObjectives(parseObjectives(objectives));
        cohort.setAnnouncementTitle(announcementTitle);
        cohort.setAnnouncementContent(announcementContent);
        String finalThumbnail = thumbnailUrl;
        if (finalThumbnail == null || finalThumbnail.trim().isEmpty()) {
            finalThumbnail = (course.getThumbnailUrl() != null && !course.getThumbnailUrl().isBlank())
                    ? course.getThumbnailUrl()
                    : "/images/courses/default-cover.png";
        } else {
            finalThumbnail = finalThumbnail.trim();
        }
        cohort.setThumbnailUrl(finalThumbnail);
        cohortRepository.save(cohort);

        cohortScheduleRepository.deleteByCohortId(moduleId);
        if (lessonIds != null && !lessonIds.isEmpty()) {
            for (Long lessonId : lessonIds) {
                lessonRepository.findById(lessonId).ifPresent(lesson -> {
                    CohortSchedule schedule = CohortSchedule.builder()
                            .cohort(cohort)
                            .lesson(lesson)
                            .unlockAt(Instant.now())
                            .build();
                    cohortScheduleRepository.save(schedule);
                });
            }
        }

        log.info("[InstructorCourseController] Cập nhật module id: {} của course id: {}", moduleId, actualCourseId);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật Module thành công!");
        return "redirect:/instructor/courses/" + actualCourseId + "/modules";
    }

    @PostMapping("/{courseId}/modules/{moduleId}/delete")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    @org.springframework.transaction.annotation.Transactional
    public String deleteModule(@PathVariable Long courseId, @PathVariable Long moduleId, RedirectAttributes redirectAttributes) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        Long actualCourseId = courseId;
        try {
            java.util.Optional<Cohort> cohortOpt = cohortRepository.findById(moduleId);
            if (cohortOpt.isPresent() && cohortOpt.get().getCourse() != null) {
                actualCourseId = cohortOpt.get().getCourse().getId();
            }
            cohortService.deleteCohort(moduleId, currentUser);
            courseService.revertToDraftIfPublished(actualCourseId);
            log.info("[InstructorCourseController] Xóa module id: {} của course id: {}", moduleId, actualCourseId);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa Module thành công!");
        } catch (Exception e) {
            log.error("[InstructorCourseController] Lỗi khi xóa module id {}: {}", moduleId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa Module: " + e.getMessage());
        }
        return "redirect:/instructor/courses/" + actualCourseId + "/modules";
    }

    @GetMapping("/{courseId}/modules/{moduleId}/delete")
    public String deleteModuleGetRedirect(@PathVariable Long courseId) {
        return "redirect:/instructor/courses/" + courseId + "/modules";
    }

    @GetMapping("/{courseId}/modules/{moduleId}/curriculum")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String showModuleCurriculum(@PathVariable Long courseId, @PathVariable Long moduleId, Model model) {
        Cohort cohort = cohortRepository.findById(moduleId).orElseThrow(() -> new RuntimeException("Module not found"));
        Long actualCourseId = (cohort.getCourse() != null) ? cohort.getCourse().getId() : courseId;
        if (!actualCourseId.equals(courseId)) {
            return "redirect:/instructor/courses/" + actualCourseId + "/modules/" + moduleId + "/curriculum";
        }
        Course course = courseService.getCourseById(actualCourseId);
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        boolean isOwner = course.getCreatedBy() != null && course.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getCode()) || "ROLE_ADMIN".equals(r.getCode()));
        if (!isOwner && !isAdmin) {
            return "redirect:/instructor/courses";
        }
        List<vn.edu.ute.utelearn.entity.Section> sections = sectionService.getSectionsByCohortId(moduleId);
        model.addAttribute("course", course);
        model.addAttribute("module", cohort);
        model.addAttribute("sections", sections);
        return "instructor/courses/module-curriculum";
    }

    @GetMapping("/{courseId}/modules/{moduleId}/gradebook")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public String showModuleGradebook(@PathVariable Long courseId, @PathVariable Long moduleId) {
        return "redirect:/instructor/cohorts/" + moduleId + "/gradebook";
    }

    @GetMapping("/{id}/gradebook")
    public String showCourseGradebook(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        log.info("[InstructorCourseController] Truy cập bảng điểm khóa học id: {}", id);
        User currentUser = authService.getCurrentAuthenticatedUser().orElse(null);
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (gradebookService == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Dịch vụ bảng điểm chưa khả dụng");
            return "redirect:/instructor/courses";
        }

        try {
            CourseGradebookDTO gradebook = gradebookService.getCourseGradebook(id, currentUser.getId());
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("gradebook", gradebook);
            model.addAttribute("course", gradebook.getCourse());
            model.addAttribute("modules", gradebook.getModules());
            model.addAttribute("students", gradebook.getStudents());
            return "instructor/courses/gradebook";
        } catch (Exception e) {
            log.error("[InstructorCourseController] Lỗi khi mở bảng điểm khóa học {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/instructor/courses";
        }
    }

    @PostMapping("/{id}/gradebook/calculate")
    public String calculateCourseGrades(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElse(null);
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (gradebookService == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Dịch vụ bảng điểm chưa khả dụng");
            return "redirect:/instructor/courses";
        }

        try {
            gradebookService.calculateAndSyncCourseGradebook(id, currentUser.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Đã tính toán và đồng bộ điểm số cùng trạng thái hoàn thành khóa học cho toàn bộ học viên!");
        } catch (Exception e) {
            log.error("[InstructorCourseController] Lỗi khi tính điểm khóa học {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tính điểm: " + e.getMessage());
        }
        return "redirect:/instructor/courses/" + id + "/gradebook";
    }

    @GetMapping("/{id}/gradebook/export")
    public ResponseEntity<byte[]> exportCourseGradebook(@PathVariable("id") Long id) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (gradebookService == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        try {
            byte[] excelBytes = gradebookService.exportCourseGradebookToExcel(id, currentUser.getId());
            Course course = courseService.getCourseById(id);
            String courseCode = (course != null && course.getCode() != null) ? course.getCode() : "CRS" + id;
            String fileName = "Bang_Diem_Khoa_Hoc_" + courseCode + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(excelBytes.length);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("[InstructorCourseController] Lỗi xuất Excel bảng điểm khóa học {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String parseObjectives(String objectivesStr) {
        if (objectivesStr == null || objectivesStr.trim().isEmpty()) {
            return "[]";
        }
        if (objectivesStr.trim().startsWith("[")) {
            return objectivesStr.trim();
        }
        List<String> list = java.util.Arrays.stream(objectivesStr.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }
}

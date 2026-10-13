package vn.edu.ute.utelearn.controller;

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
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.CohortScheduleRepository;
import vn.edu.ute.utelearn.dao.LessonRepository;
import vn.edu.ute.utelearn.dto.CourseRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
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

    @GetMapping
    public String listCourses(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              Model model) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow(() -> new RuntimeException("Unauthorized"));
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Course> coursePage = courseService.getCoursesByInstructor(currentUser.getId(), pageable);
        
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
            model.addAttribute("categories", categoryService.getRootCategories());
            model.addAttribute("isEdit", false);
            return "instructor/courses/form";
        }
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        courseService.createCourse(request, currentUser.getId());
        log.info("[InstructorCourseController] Tạo khóa học mới: {}, user: {}", request.getTitle(), currentUser.getEmail());
        redirectAttributes.addFlashAttribute("successMessage", "Tạo khóa học thành công");
        return "redirect:/instructor/courses";
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
        courseService.updateCourse(id, request, currentUser.getId());
        log.info("[InstructorCourseController] Cập nhật khóa học id: {}, user: {}", id, currentUser.getEmail());
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật khóa học thành công");
        return "redirect:/instructor/courses";
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
    public String showCurriculum(@PathVariable Long id, Model model) {
        Course course = courseService.getCourseById(id);
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        
        if (!course.getCreatedBy().getId().equals(currentUser.getId())) {
            return "redirect:/instructor/courses";
        }

        model.addAttribute("course", course);
        model.addAttribute("sections", sectionService.getSectionsByCourseId(id));
        model.addAttribute("modules", cohortRepository.findByCourseIdOrderByCreatedAtAsc(id));
        return "instructor/courses/curriculum";
    }

    @PostMapping("/{id}/submit-review")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public String submitForReview(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        Course course = courseService.getCourseById(id);
        if (!course.getCreatedBy().getId().equals(currentUser.getId())) {
            return "redirect:/instructor/courses";
        }
        
        moderationService.createCourseModerationTask(id, currentUser.getId());
        log.info("[InstructorCourseController] Gửi duyệt khóa học id: {}, user: {}", id, currentUser.getEmail());
        redirectAttributes.addFlashAttribute("successMessage", "Khóa học đã được gửi duyệt thành công. Vui lòng chờ Moderator kiểm duyệt.");
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
            lessonCountMap.put(m.getId(), cohortScheduleRepository.findByCohortId(m.getId()).size());
        }

        model.addAttribute("course", course);
        model.addAttribute("modules", modules);
        model.addAttribute("lessonCountMap", lessonCountMap);
        return "instructor/courses/modules";
    }

    @GetMapping("/{id}/modules/create")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public String showCreateModuleForm(@PathVariable Long id, Model model) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        Course course = courseService.getCourseById(id);
        if (!course.getCreatedBy().getId().equals(currentUser.getId())) {
            return "redirect:/instructor/courses";
        }

        List<vn.edu.ute.utelearn.entity.Section> sections = sectionService.getSectionsByCourseId(id);
        Map<Long, List<vn.edu.ute.utelearn.entity.Lesson>> sectionLessonsMap = new HashMap<>();
        for (vn.edu.ute.utelearn.entity.Section s : sections) {
            sectionLessonsMap.put(s.getId(), lessonService.getLessonsBySectionId(s.getId()));
        }

        model.addAttribute("course", course);
        model.addAttribute("sections", sections);
        model.addAttribute("sectionLessonsMap", sectionLessonsMap);
        model.addAttribute("isEdit", false);
        return "instructor/courses/module-form";
    }

    @PostMapping("/{id}/modules/create")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public String createModule(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) java.math.BigDecimal price,
            @RequestParam(required = false) List<Long> lessonIds,
            RedirectAttributes redirectAttributes) {
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
        }

        Cohort cohort = Cohort.builder()
                .course(course)
                .name(name)
                .code(finalCode)
                .price(price != null ? price : java.math.BigDecimal.ZERO)
                .status("ENROLLING")
                .maxCapacity(100)
                .currentEnrolled(0)
                .enrollmentStart(Instant.now())
                .studyStart(Instant.now())
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
        redirectAttributes.addFlashAttribute("successMessage", "Tạo Module cho khóa học thành công!");
        return "redirect:/instructor/courses/" + id + "/modules";
    }

    @GetMapping("/{courseId}/modules/{moduleId}/edit")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public String showEditModuleForm(@PathVariable Long courseId, @PathVariable Long moduleId, Model model) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        Course course = courseService.getCourseById(courseId);
        if (!course.getCreatedBy().getId().equals(currentUser.getId())) {
            return "redirect:/instructor/courses";
        }

        Cohort cohort = cohortRepository.findById(moduleId).orElseThrow(() -> new RuntimeException("Module not found"));
        List<vn.edu.ute.utelearn.entity.Section> sections = sectionService.getSectionsByCourseId(courseId);
        Map<Long, List<vn.edu.ute.utelearn.entity.Lesson>> sectionLessonsMap = new HashMap<>();
        for (vn.edu.ute.utelearn.entity.Section s : sections) {
            sectionLessonsMap.put(s.getId(), lessonService.getLessonsBySectionId(s.getId()));
        }

        List<Long> selectedLessonIds = cohortScheduleRepository.findByCohortId(moduleId).stream()
                .map(cs -> cs.getLesson().getId())
                .toList();

        model.addAttribute("course", course);
        model.addAttribute("module", cohort);
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
            @RequestParam(required = false) List<Long> lessonIds,
            RedirectAttributes redirectAttributes) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        Course course = courseService.getCourseById(courseId);
        if (!course.getCreatedBy().getId().equals(currentUser.getId())) {
            return "redirect:/instructor/courses";
        }

        Cohort cohort = cohortRepository.findById(moduleId).orElseThrow(() -> new RuntimeException("Module not found"));
        cohort.setName(name);
        cohort.setCode(code);
        cohort.setPrice(price != null ? price : java.math.BigDecimal.ZERO);
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

        log.info("[InstructorCourseController] Cập nhật module id: {} của course id: {}", moduleId, courseId);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật Module thành công!");
        return "redirect:/instructor/courses/" + courseId + "/modules";
    }

    @PostMapping("/{courseId}/modules/{moduleId}/delete")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public String deleteModule(@PathVariable Long courseId, @PathVariable Long moduleId, RedirectAttributes redirectAttributes) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        Course course = courseService.getCourseById(courseId);
        if (!course.getCreatedBy().getId().equals(currentUser.getId())) {
            return "redirect:/instructor/courses";
        }

        cohortScheduleRepository.deleteByCohortId(moduleId);
        cohortRepository.deleteById(moduleId);
        log.info("[InstructorCourseController] Xóa module id: {} của course id: {}", moduleId, courseId);
        redirectAttributes.addFlashAttribute("successMessage", "Xóa Module thành công!");
        return "redirect:/instructor/courses/" + courseId + "/modules";
    }
}

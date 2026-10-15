package vn.edu.ute.utelearn.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.utelearn.dto.LessonRequestDTO;
import vn.edu.ute.utelearn.dto.SectionRequestDTO;
import vn.edu.ute.utelearn.entity.Lesson;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.CohortSchedule;
import vn.edu.ute.utelearn.entity.InstructorAsset;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.LessonService;
import vn.edu.ute.utelearn.service.SectionService;
import vn.edu.ute.utelearn.service.InstructorAssetService;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@Slf4j
@RestController
@RequestMapping("/api/instructor/courses")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstructorCurriculumApiController {

    private final SectionService sectionService;
    private final LessonService lessonService;
    private final AuthService authService;
    private final InstructorAssetService assetService;
    private final vn.edu.ute.utelearn.dao.CohortScheduleRepository cohortScheduleRepository;
    private final vn.edu.ute.utelearn.dao.CohortRepository cohortRepository;

    @PostMapping("/{courseId}/sections")
    @Transactional
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> createSection(@PathVariable Long courseId, @Valid @RequestBody SectionRequestDTO request) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        sectionService.createSection(courseId, request, currentUser.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/modules/{cohortId}/sections")
    @Transactional
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> createModuleSection(@PathVariable Long cohortId, @Valid @RequestBody SectionRequestDTO request) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        Cohort cohort = cohortRepository.findById(cohortId).orElseThrow(() -> new RuntimeException("Module not found"));
        request.setCohortId(cohortId);
        Long courseId = (cohort.getCourse() != null) ? cohort.getCourse().getId() : null;
        sectionService.createSection(courseId, request, currentUser.getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/sections/{sectionId}")
    @Transactional
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> updateSection(@PathVariable Long sectionId, @Valid @RequestBody SectionRequestDTO request) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        sectionService.updateSection(sectionId, request, currentUser.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/sections/{sectionId}")
    @Transactional
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<Void> deleteSection(@PathVariable Long sectionId) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        sectionService.deleteSection(sectionId, currentUser.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{courseId}/full-curriculum")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> getFullCurriculum(
            @PathVariable Long courseId,
            @RequestParam(required = false) Long cohortId) {
        try {
            List<vn.edu.ute.utelearn.entity.Section> sections = sectionService.getSectionsByCourseId(courseId);
            List<Map<String, Object>> result = new ArrayList<>();
            
            for (vn.edu.ute.utelearn.entity.Section s : sections) {
                Map<String, Object> sectionMap = new HashMap<>();
                sectionMap.put("id", s.getId());
                sectionMap.put("title", s.getTitle());
                
                List<Lesson> lessons = lessonService.getLessonsBySectionId(s.getId());
                List<Map<String, Object>> lessonDtos = lessons.stream().map(l -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", l.getId());
                    map.put("title", l.getTitle());
                    map.put("lessonType", l.getLessonType());
                    map.put("isFreePreview", l.getIsFreePreview());

                    if (cohortId != null) {
                        cohortScheduleRepository.findByCohortIdAndLessonId(cohortId, l.getId()).ifPresent(cs -> {
                            map.put("unlockAt", cs.getFormattedUnlockAt());
                            map.put("deadlineAt", cs.getFormattedDeadlineAt());
                            map.put("showScoreType", cs.getShowScoreType());
                        });
                    }

                    List<CohortSchedule> schedules = cohortScheduleRepository.findByLessonId(l.getId());
                    if (!schedules.isEmpty() && schedules.get(0).getCohort() != null) {
                        Cohort c = schedules.get(0).getCohort();
                        map.put("cohortId", c.getId());
                        map.put("cohortCode", c.getCode());
                        map.put("cohortName", c.getName());
                    } else {
                        map.put("cohortId", null);
                        map.put("cohortCode", null);
                        map.put("cohortName", null);
                    }
                    return map;
                }).toList();
                
                sectionMap.put("lessons", lessonDtos);
                result.add(sectionMap);
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Lỗi khi tải toàn bộ giáo trình cho khóa học ID {}: {}", courseId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    @GetMapping("/sections/{sectionId}/lessons")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> getLessons(@PathVariable Long sectionId) {
        try {
            List<Lesson> lessons = lessonService.getLessonsBySectionId(sectionId);
            List<Map<String, Object>> result = lessons.stream().map(l -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", l.getId());
                map.put("title", l.getTitle());
                map.put("lessonType", l.getLessonType());
                map.put("isFreePreview", l.getIsFreePreview());
                map.put("videoUrl", l.getVideoUrl());
                map.put("assetUrl", l.getAssetUrl());
                map.put("documentContent", l.getDocumentContent());
                map.put("isPublicForCohorts", l.getIsPublicForCohorts());
                List<CohortSchedule> schedules = cohortScheduleRepository.findByLessonId(l.getId());
                if (!schedules.isEmpty() && schedules.get(0).getCohort() != null) {
                    Cohort c = schedules.get(0).getCohort();
                    map.put("cohortId", c.getId());
                    map.put("cohortCode", c.getCode());
                    map.put("cohortName", c.getName());
                } else {
                    map.put("cohortId", null);
                    map.put("cohortCode", null);
                    map.put("cohortName", null);
                }
                return map;
            }).toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Lỗi khi tải danh sách bài học của section ID {}: {}", sectionId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    @GetMapping("/modules/{cohortId}/sections")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> getModuleSections(@PathVariable Long cohortId) {
        try {
            List<vn.edu.ute.utelearn.entity.Section> sections = sectionService.getSectionsByCohortId(cohortId);
            List<Map<String, Object>> result = sections.stream().map(s -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", s.getId());
                map.put("title", s.getTitle());
                map.put("orderIndex", s.getOrderIndex());
                return map;
            }).toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Lỗi khi tải danh sách section của module ID {}: {}", cohortId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    @PostMapping("/sections/{sectionId}/lessons")
    @Transactional
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> createLesson(@PathVariable Long sectionId, @Valid @RequestBody LessonRequestDTO request) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        lessonService.createLesson(sectionId, request, currentUser.getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/lessons/{lessonId}")
    @Transactional
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> updateLesson(@PathVariable Long lessonId, @Valid @RequestBody LessonRequestDTO request) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        lessonService.updateLesson(lessonId, request, currentUser.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/lessons/{lessonId}")
    @Transactional
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long lessonId) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        lessonService.deleteLesson(lessonId, currentUser.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/assets")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> getInstructorAssets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        Page<InstructorAsset> assets = assetService.getAssetsByInstructor(
                currentUser, keyword, null, PageRequest.of(page, 50)
        );

        List<java.util.Map<String, Object>> result = assets.stream().map(a -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", a.getId());
            map.put("name", a.getName());
            map.put("assetType", a.getAssetType());
            map.put("cloudinaryUrl", a.getCloudinaryUrl());
            map.put("createdAt", a.getCreatedAt());
            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }
}

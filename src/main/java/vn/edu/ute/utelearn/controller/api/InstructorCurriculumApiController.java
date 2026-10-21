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
import vn.edu.ute.utelearn.entity.Course;
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

import vn.edu.ute.utelearn.service.CloudinaryService;

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
    private final CloudinaryService cloudinaryService;
    private final vn.edu.ute.utelearn.dao.CohortScheduleRepository cohortScheduleRepository;
    private final vn.edu.ute.utelearn.dao.CohortRepository cohortRepository;
    private final vn.edu.ute.utelearn.dao.CourseRepository courseRepository;
    private final vn.edu.ute.utelearn.dao.LessonRepository lessonRepository;

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
                    map.put("status", l.getStatus());

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
                map.put("status", l.getStatus());
                map.put("durationSeconds", l.getDurationSeconds() != null ? l.getDurationSeconds() : 0);
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
        try {
            User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
            Lesson created = lessonService.createLesson(sectionId, request, currentUser.getId());
            return ResponseEntity.ok(Map.of("message", "Tạo bài học thành công", "id", created.getId()));
        } catch (Exception e) {
            log.error("[InstructorCurriculumApiController] Lỗi khi tạo bài học cho section {}: {}", sectionId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Lỗi khi tạo bài học"));
        }
    }

    @PutMapping("/lessons/{lessonId}")
    @Transactional
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> updateLesson(@PathVariable Long lessonId, @Valid @RequestBody LessonRequestDTO request) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
            Lesson updated = lessonService.updateLesson(lessonId, request, currentUser.getId());
            return ResponseEntity.ok(Map.of("message", "Cập nhật bài học thành công", "id", updated.getId()));
        } catch (Exception e) {
            log.error("[InstructorCurriculumApiController] Lỗi khi cập nhật bài học {}: {}", lessonId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Lỗi khi cập nhật bài học"));
        }
    }

    @DeleteMapping("/lessons/{lessonId}")
    @Transactional
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> deleteLesson(@PathVariable Long lessonId) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
            lessonService.deleteLesson(lessonId, currentUser.getId());
            return ResponseEntity.ok(Map.of("message", "Xóa bài học thành công"));
        } catch (Exception e) {
            log.error("[InstructorCurriculumApiController] Lỗi khi xóa bài học {}: {}", lessonId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Lỗi khi xóa bài học"));
        }
    }

    @GetMapping("/videos/detect-duration")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> detectVideoDuration(@RequestParam String url) {
        Integer duration = cloudinaryService.getVideoDuration(url);
        return ResponseEntity.ok(Map.of("durationSeconds", duration != null ? duration : 0));
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

    @PutMapping("/{courseId}/inline")
    @Transactional
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN', 'MODERATOR', 'ROLE_MODERATOR') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> updateCourseInline(
            @PathVariable Long courseId,
            @RequestBody Map<String, Object> payload) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Khóa học không tồn tại"));

        boolean isOwner = course.getCreatedBy() != null && course.getCreatedBy().getId().equals(currentUser.getId());
        boolean hasAdminRole = currentUser.getRoles().stream().anyMatch(r ->
                "ADMIN".equals(r.getCode()) || "ROLE_ADMIN".equals(r.getCode()) ||
                "MODERATOR".equals(r.getCode()) || "ROLE_MODERATOR".equals(r.getCode()));

        if (!isOwner && !hasAdminRole) {
            return ResponseEntity.status(403).body(Map.of("error", "Bạn không có quyền chỉnh sửa khóa học này"));
        }

        if (payload.containsKey("title")) {
            String title = (String) payload.get("title");
            if (title != null && !title.trim().isBlank()) {
                course.setTitle(title.trim());
            }
        }
        if (payload.containsKey("headline")) {
            course.setHeadline((String) payload.get("headline"));
        }
        if (payload.containsKey("description")) {
            course.setDescription((String) payload.get("description"));
        }
        if (payload.containsKey("level")) {
            course.setLevel((String) payload.get("level"));
        }
        if (payload.containsKey("objectives")) {
            Object obj = payload.get("objectives");
            if (obj instanceof String) {
                String str = (String) obj;
                if (str.trim().startsWith("[")) {
                    course.setObjectives(str);
                } else {
                    List<String> list = java.util.Arrays.stream(str.split("\n"))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList();
                    try {
                        course.setObjectives(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(list));
                    } catch (Exception e) {
                        course.setObjectives(str);
                    }
                }
            } else if (obj instanceof List) {
                try {
                    course.setObjectives(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj));
                } catch (Exception e) {
                    log.error("Lỗi parse objectives inline", e);
                }
            }
        }
        if (payload.containsKey("price")) {
            Object p = payload.get("price");
            if (p != null) {
                course.setPrice(new java.math.BigDecimal(p.toString()));
            }
        }

        Course saved = courseRepository.save(course);
        log.info("[InstructorCurriculumApiController] Cập nhật inline khóa học id: {}, user: {}", courseId, currentUser.getEmail());

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", saved.getId());
        resp.put("title", saved.getTitle());
        resp.put("headline", saved.getHeadline());
        resp.put("description", saved.getDescription());
        resp.put("level", saved.getLevel());
        resp.put("objectives", saved.getObjectives());
        resp.put("price", saved.getPrice());
        resp.put("slug", saved.getSlug());
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/modules/{cohortId}/inline")
    @Transactional
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN', 'MODERATOR', 'ROLE_MODERATOR') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> updateCohortInline(
            @PathVariable Long cohortId,
            @RequestBody Map<String, Object> payload) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new RuntimeException("Module không tồn tại"));

        boolean isModuleCreator = cohort.getCreatedBy() != null && cohort.getCreatedBy().getId().equals(currentUser.getId());
        boolean isCourseOwner = cohort.getCourse() != null && cohort.getCourse().getCreatedBy() != null && cohort.getCourse().getCreatedBy().getId().equals(currentUser.getId());
        boolean hasAdminRole = currentUser.getRoles().stream().anyMatch(r ->
                "ADMIN".equals(r.getCode()) || "ROLE_ADMIN".equals(r.getCode()) ||
                "MODERATOR".equals(r.getCode()) || "ROLE_MODERATOR".equals(r.getCode()));

        if (!isCourseOwner && !isModuleCreator && !hasAdminRole) {
            return ResponseEntity.status(403).body(Map.of("error", "Bạn không có quyền chỉnh sửa module này"));
        }

        if (payload.containsKey("name")) {
            String name = (String) payload.get("name");
            if (name != null && !name.trim().isBlank()) {
                cohort.setName(name.trim());
            }
        }
        if (payload.containsKey("code")) {
            String code = (String) payload.get("code");
            if (code != null && !code.trim().isBlank()) {
                cohort.setCode(code.trim());
            }
        }
        if (payload.containsKey("description")) {
            cohort.setDescription((String) payload.get("description"));
        }
        if (payload.containsKey("objectives")) {
            Object obj = payload.get("objectives");
            if (obj != null) {
                String raw = obj.toString().trim();
                if (raw.startsWith("[")) {
                    cohort.setObjectives(raw);
                } else {
                    List<String> list = java.util.Arrays.stream(raw.split("\n"))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList();
                    try {
                        cohort.setObjectives(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(list));
                    } catch (Exception e) {
                        cohort.setObjectives(raw);
                    }
                }
            }
        }
        if (payload.containsKey("price")) {
            Object p = payload.get("price");
            if (p != null) {
                cohort.setPrice(new java.math.BigDecimal(p.toString()));
            }
        }
        if (payload.containsKey("announcementTitle")) {
            cohort.setAnnouncementTitle((String) payload.get("announcementTitle"));
        }
        if (payload.containsKey("announcementContent")) {
            cohort.setAnnouncementContent((String) payload.get("announcementContent"));
        }

        if (cohort.getCourse() == null && "APPROVED".equalsIgnoreCase(cohort.getModerationStatus())) {
            cohort.setModerationStatus("DRAFT");
            log.info("[InstructorCurriculumApiController] Chuyển module độc lập id: {} về DRAFT do sửa inline", cohortId);
        }

        Cohort saved = cohortRepository.save(cohort);
        log.info("[InstructorCurriculumApiController] Cập nhật inline module id: {}, user: {}", cohortId, currentUser.getEmail());

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", saved.getId());
        resp.put("name", saved.getName());
        resp.put("code", saved.getCode());
        resp.put("description", saved.getDescription());
        resp.put("objectives", saved.getParsedObjectives());
        resp.put("price", saved.getPrice());
        resp.put("announcementTitle", saved.getAnnouncementTitle());
        resp.put("announcementContent", saved.getAnnouncementContent());
        resp.put("moderationStatus", saved.getModerationStatus());
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/sections/reorder")
    @Transactional
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> reorderSections(@RequestBody List<Long> sectionIds) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        sectionService.reorderSections(sectionIds, currentUser.getId());
        return ResponseEntity.ok(Map.of("message", "Cập nhật thứ tự chương thành công"));
    }

    @PutMapping("/lessons/reorder")
    @Transactional
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> reorderLessons(@RequestBody Map<String, Object> payload) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        Long sectionId = null;
        if (payload.get("sectionId") != null) {
            sectionId = Long.valueOf(payload.get("sectionId").toString());
        }
        List<?> rawIds = (List<?>) payload.get("lessonIds");
        List<Long> lessonIds = new ArrayList<>();
        if (rawIds != null) {
            for (Object id : rawIds) {
                if (id != null) {
                    lessonIds.add(Long.valueOf(id.toString()));
                }
            }
        }
        lessonService.reorderLessons(sectionId, lessonIds, currentUser.getId());
        return ResponseEntity.ok(Map.of("message", "Cập nhật thứ tự bài học thành công"));
    }

    @PutMapping("/{courseId}/modules/reorder")
    @Transactional
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> reorderModules(@PathVariable Long courseId, @RequestBody List<Long> moduleIds) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        if (moduleIds != null && !moduleIds.isEmpty()) {
            for (int i = 0; i < moduleIds.size(); i++) {
                Long mId = moduleIds.get(i);
                Cohort cohort = cohortRepository.findById(mId).orElse(null);
                if (cohort != null) {
                    cohort.setOrderIndex(i + 1);
                    cohortRepository.save(cohort);
                }
            }
        }
        return ResponseEntity.ok(Map.of("message", "Cập nhật thứ tự module thành công"));
    }
}

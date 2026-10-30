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

import vn.edu.ute.utelearn.service.CohortService;
import vn.edu.ute.utelearn.service.CloudinaryService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

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
    private final CohortService cohortService;
    private final vn.edu.ute.utelearn.service.CourseService courseService;
    private final vn.edu.ute.utelearn.service.ModerationService moderationService;
    private final vn.edu.ute.utelearn.dao.CohortScheduleRepository cohortScheduleRepository;
    private final vn.edu.ute.utelearn.dao.CohortRepository cohortRepository;
    private final vn.edu.ute.utelearn.dao.CourseRepository courseRepository;
    private final vn.edu.ute.utelearn.dao.LessonRepository lessonRepository;

    private boolean canManageCohort(Cohort cohort, User user) {
        if (cohort == null || user == null) {
            return false;
        }
        boolean isModuleCreator = cohort.getCreatedBy() != null && cohort.getCreatedBy().getId().equals(user.getId());
        boolean isCourseOwner = cohort.getCourse() != null && cohort.getCourse().getCreatedBy() != null && cohort.getCourse().getCreatedBy().getId().equals(user.getId());
        boolean hasAdminRole = user.getRoles().stream().anyMatch(r ->
                "ADMIN".equalsIgnoreCase(r.getCode()) || "ROLE_ADMIN".equalsIgnoreCase(r.getCode()));
        return isCourseOwner || isModuleCreator || hasAdminRole;
    }

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
    public ResponseEntity<?> getLessons(
            @PathVariable Long sectionId,
            @RequestParam(required = false) Long cohortId) {
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

                CohortSchedule targetSchedule = null;
                if (cohortId != null) {
                    targetSchedule = cohortScheduleRepository.findByCohortIdAndLessonId(cohortId, l.getId()).orElse(null);
                }
                if (targetSchedule == null) {
                    List<CohortSchedule> schedules = cohortScheduleRepository.findByLessonId(l.getId());
                    if (!schedules.isEmpty()) {
                        targetSchedule = schedules.get(0);
                    }
                }

                if (targetSchedule != null) {
                    map.put("scheduleId", targetSchedule.getId());
                    map.put("unlockAt", targetSchedule.getFormattedUnlockAt());
                    map.put("deadlineAt", targetSchedule.getFormattedDeadlineAt());
                    map.put("displayUnlockAt", targetSchedule.getDisplayUnlockAt());
                    map.put("displayDeadlineAt", targetSchedule.getDisplayDeadlineAt());
                    map.put("isLocked", targetSchedule.isLockedNow());
                    map.put("showScoreType", targetSchedule.getShowScoreType());
                    map.put("allowReviewAnswers", targetSchedule.getAllowReviewAnswers());
                    if (targetSchedule.getCohort() != null) {
                        map.put("cohortId", targetSchedule.getCohort().getId());
                        map.put("cohortCode", targetSchedule.getCohort().getCode());
                        map.put("cohortName", targetSchedule.getCohort().getName());
                    }
                } else {
                    map.put("scheduleId", null);
                    map.put("unlockAt", "");
                    map.put("deadlineAt", "");
                    map.put("displayUnlockAt", "");
                    map.put("displayDeadlineAt", "");
                    map.put("isLocked", false);
                    map.put("showScoreType", "IMMEDIATELY");
                    map.put("allowReviewAnswers", true);
                    map.put("cohortId", null);
                    map.put("cohortCode", null);
                    map.put("cohortName", null);
                }
                return map;
            }).toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[InstructorCurriculumApiController] Lỗi khi tải bài học của section ID {}: {}", sectionId, e.getMessage(), e);
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
        boolean validFormat = vn.edu.ute.utelearn.util.ValidationUtils.isValidVideoUrl(url);
        Integer duration = validFormat ? cloudinaryService.getVideoDuration(url) : null;
        return ResponseEntity.ok(Map.of(
                "durationSeconds", duration != null ? duration : 0,
                "validFormat", validFormat
        ));
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
            map.put("authorName", a.getInstructor() != null ? a.getInstructor().getFullName() : "");
            map.put("isOwner", a.getInstructor() != null && a.getInstructor().getId().equals(currentUser.getId()));
            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/upload-asset", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> uploadAsset(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
            InstructorAsset asset = assetService.uploadAndRegisterAsset(file, currentUser);
            Map<String, Object> res = new HashMap<>();
            res.put("url", asset.getCloudinaryUrl());
            res.put("name", asset.getName());
            res.put("assetType", asset.getAssetType() != null ? asset.getAssetType() : "");
            res.put("fileSizeBytes", asset.getFileSizeBytes() != null ? asset.getFileSizeBytes() : 0L);
            res.put("id", asset.getId());
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            log.error("[InstructorCurriculumApiController] Lỗi upload tài liệu: {}", e.getMessage());
            Map<String, Object> err = new HashMap<>();
            err.put("message", e.getMessage() != null ? e.getMessage() : "Lỗi tải tệp lên máy chủ");
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PutMapping("/{courseId}/inline")
    @Transactional
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> updateCourseInline(
            @PathVariable Long courseId,
            @RequestBody Map<String, Object> payload) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Khóa học không tồn tại"));

        boolean isOwner = course.getCreatedBy() != null && course.getCreatedBy().getId().equals(currentUser.getId());
        boolean hasAdminRole = currentUser.getRoles().stream().anyMatch(r ->
                "ADMIN".equals(r.getCode()) || "ROLE_ADMIN".equals(r.getCode()));

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

        if ("PUBLISHED".equalsIgnoreCase(course.getStatus())) {
            course.setStatus("DRAFT");
            log.info("[InstructorCurriculumApiController] Chuyển khóa học id: {} về DRAFT do cập nhật inline", courseId);
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
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> updateCohortInline(
            @PathVariable Long cohortId,
            @RequestBody Map<String, Object> payload) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new RuntimeException("Module không tồn tại"));

        boolean isModuleCreator = cohort.getCreatedBy() != null && cohort.getCreatedBy().getId().equals(currentUser.getId());
        boolean isCourseOwner = cohort.getCourse() != null && cohort.getCourse().getCreatedBy() != null && cohort.getCourse().getCreatedBy().getId().equals(currentUser.getId());
        boolean hasAdminRole = currentUser.getRoles().stream().anyMatch(r ->
                "ADMIN".equals(r.getCode()) || "ROLE_ADMIN".equals(r.getCode()));

        if (!isCourseOwner && !isModuleCreator && !hasAdminRole) {
            return ResponseEntity.status(403).body(Map.of("error", "Bạn không có quyền chỉnh sửa module này"));
        }

        boolean coreChanged = false;

        if (payload.containsKey("name")) {
            String name = (String) payload.get("name");
            if (name != null && !name.trim().isBlank() && !name.trim().equals(cohort.getName())) {
                cohort.setName(name.trim());
                coreChanged = true;
            }
        }
        if (payload.containsKey("code")) {
            String code = (String) payload.get("code");
            if (code != null && !code.trim().isBlank() && !code.trim().equals(cohort.getCode())) {
                cohort.setCode(code.trim());
                coreChanged = true;
            }
        }
        if (payload.containsKey("description")) {
            String desc = (String) payload.get("description");
            if (!java.util.Objects.equals(desc, cohort.getDescription())) {
                cohort.setDescription(desc);
                coreChanged = true;
            }
        }
        if (payload.containsKey("objectives")) {
            Object obj = payload.get("objectives");
            if (obj != null) {
                String raw = obj.toString().trim();
                String formatted = raw;
                if (!raw.startsWith("[")) {
                    List<String> list = java.util.Arrays.stream(raw.split("\n"))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList();
                    try {
                        formatted = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(list);
                    } catch (Exception e) {
                        formatted = raw;
                    }
                }
                if (!java.util.Objects.equals(formatted, cohort.getObjectives())) {
                    cohort.setObjectives(formatted);
                    coreChanged = true;
                }
            }
        }
        if (payload.containsKey("price")) {
            Object p = payload.get("price");
            if (p != null) {
                java.math.BigDecimal newPrice = new java.math.BigDecimal(p.toString());
                if (cohort.getPrice() == null || cohort.getPrice().compareTo(newPrice) != 0) {
                    cohort.setPrice(newPrice);
                    coreChanged = true;
                }
            }
        }
        if (payload.containsKey("announcementTitle")) {
            cohort.setAnnouncementTitle((String) payload.get("announcementTitle"));
        }
        if (payload.containsKey("announcementContent")) {
            cohort.setAnnouncementContent((String) payload.get("announcementContent"));
        }

        if (coreChanged) {
            cohortService.revertToDraftIfApproved(cohort);
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
            courseService.revertToDraftIfPublished(courseId);
        }
        return ResponseEntity.ok(Map.of("message", "Cập nhật thứ tự module thành công"));
    }

    @GetMapping("/cohorts/{cohortId}/lessons/{lessonId}/schedule")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> getLessonSchedule(
            @PathVariable Long cohortId,
            @PathVariable Long lessonId) {
        try {
            Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("message", "Vui lòng đăng nhập"));
            }
            User currentUser = currentUserOpt.get();
            Cohort cohort = cohortRepository.findById(cohortId).orElse(null);
            if (cohort == null) {
                return ResponseEntity.status(404).body(Map.of("message", "Module không tồn tại"));
            }
            if (!canManageCohort(cohort, currentUser)) {
                return ResponseEntity.status(403).body(Map.of("message", "Bạn không có quyền xem lộ trình của module này"));
            }

            Optional<CohortSchedule> csOpt = cohortScheduleRepository.findByCohortIdAndLessonId(cohortId, lessonId);
            Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
            Map<String, Object> map = new HashMap<>();
            map.put("cohortId", cohortId);
            map.put("lessonId", lessonId);
            map.put("lessonTitle", lesson != null ? lesson.getTitle() : "");
            map.put("lessonType", lesson != null ? lesson.getLessonType() : "");
            if (csOpt.isPresent()) {
                CohortSchedule cs = csOpt.get();
                map.put("scheduleId", cs.getId());
                map.put("unlockAt", cs.getFormattedUnlockAt());
                map.put("deadlineAt", cs.getFormattedDeadlineAt());
                map.put("displayUnlockAt", cs.getDisplayUnlockAt());
                map.put("displayDeadlineAt", cs.getDisplayDeadlineAt());
                map.put("showScoreType", cs.getShowScoreType());
                map.put("allowReviewAnswers", cs.getAllowReviewAnswers());
                map.put("isLocked", cs.isLockedNow());
            } else {
                map.put("scheduleId", null);
                map.put("unlockAt", "");
                map.put("deadlineAt", "");
                map.put("displayUnlockAt", "");
                map.put("displayDeadlineAt", "");
                map.put("showScoreType", "IMMEDIATELY");
                map.put("allowReviewAnswers", true);
                map.put("isLocked", false);
            }
            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error("[InstructorCurriculumApiController] Lỗi khi tải schedule lesson {}: {}", lessonId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi khi tải thông tin lộ trình"));
        }
    }

    @PutMapping("/cohorts/{cohortId}/lessons/{lessonId}/schedule")
    @Transactional
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> updateLessonSchedule(
            @PathVariable Long cohortId,
            @PathVariable Long lessonId,
            @RequestBody Map<String, Object> payload) {
        try {
            Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("message", "Vui lòng đăng nhập"));
            }
            User currentUser = currentUserOpt.get();
            Cohort cohort = cohortRepository.findById(cohortId).orElseThrow(() -> new RuntimeException("Module không tồn tại"));
            if (!canManageCohort(cohort, currentUser)) {
                return ResponseEntity.status(403).body(Map.of("message", "Bạn không có quyền chỉnh sửa lộ trình của module này"));
            }

            Lesson lesson = lessonRepository.findById(lessonId).orElseThrow(() -> new RuntimeException("Bài học không tồn tại"));
            if (cohort.getCourse() != null && lesson.getSection() != null && lesson.getSection().getCourse() != null) {
                if (!cohort.getCourse().getId().equals(lesson.getSection().getCourse().getId())) {
                    return ResponseEntity.status(400).body(Map.of("message", "Bài học không thuộc khóa học của module này"));
                }
            }

            String unlockAtStr = (String) payload.get("unlockAt");
            String deadlineAtStr = (String) payload.get("deadlineAt");
            String showScoreType = (String) payload.get("showScoreType");
            Boolean allowReview = payload.get("allowReviewAnswers") != null ? (Boolean) payload.get("allowReviewAnswers") : true;

            Instant unlockInstant = parseDateTime(unlockAtStr);
            if (unlockInstant == null) {
                unlockInstant = Instant.now();
            }
            Instant deadlineInstant = parseDateTime(deadlineAtStr);

            Optional<CohortSchedule> csOpt = cohortScheduleRepository.findByCohortIdAndLessonId(cohortId, lessonId);
            CohortSchedule cs;
            if (csOpt.isPresent()) {
                cs = csOpt.get();
                cs.setUnlockAt(unlockInstant);
                cs.setDeadlineAt(deadlineInstant);
                if (showScoreType != null && !showScoreType.isBlank()) {
                    cs.setShowScoreType(showScoreType);
                }
                cs.setAllowReviewAnswers(allowReview);
            } else {
                cs = CohortSchedule.builder()
                        .cohort(cohort)
                        .lesson(lesson)
                        .unlockAt(unlockInstant)
                        .deadlineAt(deadlineInstant)
                        .showScoreType(showScoreType != null && !showScoreType.isBlank() ? showScoreType : "IMMEDIATELY")
                        .allowReviewAnswers(allowReview)
                        .build();
            }
            cohortScheduleRepository.save(cs);
            cohortService.revertToDraftIfApproved(cohort);
            log.info("[InstructorCurriculumApiController] Cập nhật lộ trình cho bài học {} trong module {}", lessonId, cohortId);

            Map<String, Object> resp = new HashMap<>();
            resp.put("message", "Cập nhật lộ trình thành công");
            resp.put("unlockAt", cs.getFormattedUnlockAt());
            resp.put("deadlineAt", cs.getFormattedDeadlineAt());
            resp.put("displayUnlockAt", cs.getDisplayUnlockAt());
            resp.put("displayDeadlineAt", cs.getDisplayDeadlineAt());
            resp.put("showScoreType", cs.getShowScoreType());
            resp.put("allowReviewAnswers", cs.getAllowReviewAnswers());
            resp.put("isLocked", cs.isLockedNow());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("[InstructorCurriculumApiController] Lỗi khi cập nhật schedule: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Lỗi khi cập nhật lộ trình"));
        }
    }

    @PostMapping("/cohorts/{cohortId}/unlock-all")
    @Transactional
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> unlockAllCohortLessons(@PathVariable Long cohortId) {
        try {
            Optional<User> currentUserOpt = authService.getCurrentAuthenticatedUser();
            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("message", "Vui lòng đăng nhập"));
            }
            User currentUser = currentUserOpt.get();
            Cohort cohort = cohortRepository.findById(cohortId).orElseThrow(() -> new RuntimeException("Module không tồn tại"));
            if (!canManageCohort(cohort, currentUser)) {
                return ResponseEntity.status(403).body(Map.of("message", "Bạn không có quyền mở bài học của module này"));
            }

            List<CohortSchedule> schedules = cohortScheduleRepository.findByCohortId(cohortId);
            Instant now = Instant.now();
            for (CohortSchedule cs : schedules) {
                cs.setUnlockAt(now);
                cohortScheduleRepository.save(cs);
            }
            cohortService.revertToDraftIfApproved(cohort);
            log.info("[InstructorCurriculumApiController] Đã mở toàn bộ bài học ngay bây giờ cho module {}", cohortId);
            return ResponseEntity.ok(Map.of("message", "Đã mở toàn bộ bài học thành công!"));
        } catch (Exception e) {
            log.error("[InstructorCurriculumApiController] Lỗi khi mở toàn bộ bài học: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Lỗi khi mở toàn bộ bài học"));
        }
    }

    private Instant parseDateTime(String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        try {
            if (str.length() == 16) {
                return LocalDateTime.parse(str).atZone(ZoneId.systemDefault()).toInstant();
            }
            return Instant.parse(str);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(str).atZone(ZoneId.systemDefault()).toInstant();
            } catch (Exception ex) {
                return null;
            }
        }
    }

    @GetMapping("/{courseId}/moderation-status")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> getCourseModerationStatus(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Khóa học không tồn tại"));
        List<Cohort> modules = cohortRepository.findByCourseIdOrdered(courseId).stream()
                .filter(c -> c != null && !c.isFullCourseCohort())
                .toList();
        List<Map<String, Object>> moduleList = modules.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("code", m.getCode());
            map.put("name", m.getName());
            map.put("moderationStatus", m.getModerationStatus());
            return map;
        }).toList();

        Map<String, Object> resp = new HashMap<>();
        resp.put("courseId", course.getId());
        resp.put("courseStatus", course.getStatus());
        resp.put("modules", moduleList);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/modules/{cohortId}/moderation-status")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> getCohortModerationStatus(@PathVariable Long cohortId) {
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new RuntimeException("Module không tồn tại"));
        Map<String, Object> resp = new HashMap<>();
        resp.put("cohortId", cohort.getId());
        resp.put("cohortStatus", cohort.getModerationStatus());
        if (cohort.getCourse() != null) {
            resp.put("courseId", cohort.getCourse().getId());
            resp.put("courseStatus", cohort.getCourse().getStatus());
        }
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{courseId}/submit-review")
    @Transactional
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> submitCourseReviewAjax(@PathVariable Long courseId) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Khóa học không tồn tại"));
        boolean isOwner = course.getCreatedBy() != null && course.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r ->
                "ADMIN".equals(r.getCode()) || "ROLE_ADMIN".equals(r.getCode()));
        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(403).body(Map.of("message", "Bạn không có quyền gửi duyệt khóa học này"));
        }
        moderationService.createCourseModerationTask(courseId, currentUser.getId());
        log.info("[InstructorCurriculumApiController] Gửi duyệt khóa học id: {}, user: {}", courseId, currentUser.getEmail());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Khóa học đã được gửi duyệt thành công. Vui lòng chờ Moderator kiểm duyệt.",
                "courseStatus", "PENDING_MODERATOR"
        ));
    }

    @PostMapping("/modules/{cohortId}/submit-review")
    @Transactional
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'ROLE_INSTRUCTOR', 'ADMIN', 'ROLE_ADMIN') or @rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> submitCohortReviewAjax(@PathVariable Long cohortId) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new RuntimeException("Module không tồn tại"));
        if (!canManageCohort(cohort, currentUser)) {
            return ResponseEntity.status(403).body(Map.of("message", "Bạn không có quyền gửi duyệt module này"));
        }
        moderationService.createCohortModerationTask(cohortId, currentUser.getId());
        log.info("[InstructorCurriculumApiController] Gửi duyệt module id: {}, user: {}", cohortId, currentUser.getEmail());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Module đã được gửi duyệt thành công. Vui lòng chờ Moderator kiểm duyệt.",
                "cohortStatus", "PENDING_MODERATOR"
        ));
    }

}
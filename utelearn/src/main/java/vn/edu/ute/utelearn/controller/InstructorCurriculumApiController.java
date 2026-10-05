package vn.edu.ute.utelearn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.utelearn.dto.LessonRequestDTO;
import vn.edu.ute.utelearn.dto.SectionRequestDTO;
import vn.edu.ute.utelearn.entity.Lesson;
import vn.edu.ute.utelearn.entity.Section;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.entity.InstructorAsset;
import vn.edu.ute.utelearn.service.AuthService;
import vn.edu.ute.utelearn.service.LessonService;
import vn.edu.ute.utelearn.service.SectionService;
import vn.edu.ute.utelearn.service.InstructorAssetService;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@RestController
@RequestMapping("/api/instructor/courses")
@RequiredArgsConstructor
public class InstructorCurriculumApiController {

    private final SectionService sectionService;
    private final LessonService lessonService;
    private final AuthService authService;
    private final InstructorAssetService assetService;

    // --- SECTION APIs ---

    @PostMapping("/{courseId}/sections")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> createSection(@PathVariable Long courseId, @Valid @RequestBody SectionRequestDTO request) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        sectionService.createSection(courseId, request, currentUser.getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/sections/{sectionId}")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> updateSection(@PathVariable Long sectionId, @Valid @RequestBody SectionRequestDTO request) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        sectionService.updateSection(sectionId, request, currentUser.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/sections/{sectionId}")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<Void> deleteSection(@PathVariable Long sectionId) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        sectionService.deleteSection(sectionId, currentUser.getId());
        return ResponseEntity.ok().build();
    }

    // --- LESSON APIs ---

    @GetMapping("/sections/{sectionId}/lessons")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> getLessons(@PathVariable Long sectionId) {
        List<Lesson> lessons = lessonService.getLessonsBySectionId(sectionId);
        // Map to prevent serialization issues
        List<java.util.Map<String, Object>> result = lessons.stream().map(l -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", l.getId());
            map.put("title", l.getTitle());
            map.put("lessonType", l.getLessonType());
            map.put("isFreePreview", l.getIsFreePreview());
            map.put("videoUrl", l.getVideoUrl());
            map.put("assetUrl", l.getAssetUrl());
            map.put("documentContent", l.getDocumentContent());
            map.put("isPublicForCohorts", l.getIsPublicForCohorts());
            return map;
        }).toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/sections/{sectionId}/lessons")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> createLesson(@PathVariable Long sectionId, @Valid @RequestBody LessonRequestDTO request) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        lessonService.createLesson(sectionId, request, currentUser.getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/lessons/{lessonId}")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<?> updateLesson(@PathVariable Long lessonId, @Valid @RequestBody LessonRequestDTO request) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        lessonService.updateLesson(lessonId, request, currentUser.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/lessons/{lessonId}")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long lessonId) {
        User currentUser = authService.getCurrentAuthenticatedUser().orElseThrow();
        lessonService.deleteLesson(lessonId, currentUser.getId());
        return ResponseEntity.ok().build();
    }

    // --- ASSET PICKER API ---

    @GetMapping("/assets")
    @PreAuthorize("@rbac.hasPermission('COURSE_CREATE')")
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

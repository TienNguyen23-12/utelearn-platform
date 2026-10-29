package vn.edu.ute.utelearn.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.utelearn.dao.LessonRepository;
import vn.edu.ute.utelearn.dto.LessonRequestDTO;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.CohortSchedule;
import vn.edu.ute.utelearn.entity.Lesson;
import vn.edu.ute.utelearn.entity.Section;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.CloudinaryService;
import vn.edu.ute.utelearn.service.LessonService;
import vn.edu.ute.utelearn.util.SecurityUtils;
import vn.edu.ute.utelearn.util.ValidationUtils;
import vn.edu.ute.utelearn.service.SectionService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final SectionService sectionService;
    private final vn.edu.ute.utelearn.dao.SectionRepository sectionRepository;
    private final vn.edu.ute.utelearn.dao.CohortRepository cohortRepository;
    private final vn.edu.ute.utelearn.dao.CohortScheduleRepository cohortScheduleRepository;
    private final vn.edu.ute.utelearn.dao.UserRepository userRepository;
    private final vn.edu.ute.utelearn.dao.CourseRepository courseRepository;
    private final CloudinaryService cloudinaryService;

    private boolean isAuthorized(Section section, Long instructorId) {
        if (instructorId == null) {
            return false;
        }
        User user = userRepository.findById(instructorId).orElse(null);
        if (user != null && user.getRoles() != null) {
            boolean hasAdminOrMod = SecurityUtils.hasRole(user, "ADMIN", "MODERATOR");
            if (hasAdminOrMod) {
                return true;
            }
        }
        if (section == null) {
            return false;
        }
        if (section.getCourse() != null && section.getCourse().getCreatedBy() != null && section.getCourse().getCreatedBy().getId().equals(instructorId)) {
            return true;
        }
        if (section.getCohort() != null && section.getCohort().getCreatedBy() != null && section.getCohort().getCreatedBy().getId().equals(instructorId)) {
            return true;
        }
        if (user != null && user.getRoles() != null) {
            boolean isInstructor = user.getRoles().stream().anyMatch(r ->
                    "INSTRUCTOR".equalsIgnoreCase(r.getCode()) || "ROLE_INSTRUCTOR".equalsIgnoreCase(r.getCode()) ||
                    "TEACHER".equalsIgnoreCase(r.getCode()));
            if (isInstructor && ((section.getCourse() != null && section.getCourse().getCreatedBy() == null) ||
                    (section.getCohort() != null && section.getCohort().getCreatedBy() == null))) {
                return true;
            }
        }
        return false;
    }

    private void validateAndNormalizeVideoLesson(LessonRequestDTO request) {
        if (request == null || !"VIDEO".equalsIgnoreCase(request.getLessonType())) {
            return;
        }
        String rawVideoUrl = (request.getVideoUrl() != null && !request.getVideoUrl().isBlank())
                ? request.getVideoUrl().trim()
                : ((request.getAssetUrl() != null && !request.getAssetUrl().isBlank()) ? request.getAssetUrl().trim() : null);
        if (rawVideoUrl == null || rawVideoUrl.isBlank()) {
            log.warn("[LessonService] Từ chối lưu bài giảng video do thiếu URL video");
            throw new IllegalArgumentException("Vui lòng nhập đường dẫn (URL) video cho bài giảng video");
        }
        if (!ValidationUtils.isValidVideoUrl(rawVideoUrl)) {
            log.warn("[LessonService] Từ chối lưu bài giảng video do URL không hợp lệ: {}", rawVideoUrl);
            throw new IllegalArgumentException("Đường dẫn video không hợp lệ hoặc không chứa nội dung video bài giảng");
        }
        request.setVideoUrl(rawVideoUrl);
    }

    @Override
    public List<Lesson> getLessonsBySectionId(Long sectionId) {
        return lessonRepository.findBySectionIdOrderByOrderIndexAsc(sectionId);
    }

    @Override
    public Lesson getLessonById(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài học ID: " + lessonId));
    }

    private Integer resolveDurationSeconds(LessonRequestDTO request) {
        if (request.getDurationSeconds() != null && request.getDurationSeconds() > 0) {
            return request.getDurationSeconds();
        }
        if (cloudinaryService != null && "VIDEO".equalsIgnoreCase(request.getLessonType()) && request.getVideoUrl() != null && !request.getVideoUrl().isBlank()) {
            Integer detected = cloudinaryService.getVideoDuration(request.getVideoUrl());
            if (detected != null && detected > 0) {
                return detected;
            }
        }
        return 0;
    }

    @Override
    @Transactional
    public Lesson createLesson(Long sectionId, LessonRequestDTO request, Long instructorId) {
        Section section = sectionService.getSectionById(sectionId);

        if (!isAuthorized(section, instructorId)) {
            throw new RuntimeException("Bạn không có quyền thêm bài học vào phần này");
        }

        validateAndNormalizeVideoLesson(request);

        Integer durationSeconds = resolveDurationSeconds(request);

        Integer orderIndex = request.getOrderIndex();
        if (orderIndex == null) {
            orderIndex = lessonRepository.findBySectionIdOrderByOrderIndexAsc(sectionId).size() + 1;
        }

        Lesson lesson = Lesson.builder()
                .section(section)
                .title(request.getTitle())
                .lessonType(request.getLessonType())
                .videoUrl(request.getVideoUrl())
                .documentContent(request.getDocumentContent())
                .assetUrl(request.getAssetUrl())
                .durationSeconds(durationSeconds)
                .isFreePreview(request.getIsFreePreview() != null ? request.getIsFreePreview() : false)
                .isPublicForCohorts(request.getIsPublicForCohorts() != null ? request.getIsPublicForCohorts() : false)
                .orderIndex(orderIndex)
                .status("DRAFT")
                .build();

        Lesson savedLesson = lessonRepository.save(lesson);

        Long targetCohortId = request.getCohortId();
        if (targetCohortId == null && section.getCohort() != null) {
            targetCohortId = section.getCohort().getId();
        }

        if (targetCohortId != null) {
            cohortRepository.findById(targetCohortId).ifPresent(cohort -> {
                Instant unlockInstant = parseDateTime(request.getUnlockAt());
                if (unlockInstant == null) {
                    unlockInstant = Instant.now();
                }
                Instant deadlineInstant = parseDateTime(request.getDeadlineAt());
                String scoreType = (request.getShowScoreType() != null && !request.getShowScoreType().isBlank())
                        ? request.getShowScoreType() : "IMMEDIATELY";
                Boolean allowReview = request.getAllowReviewAnswers() != null ? request.getAllowReviewAnswers() : true;

                CohortSchedule schedule = CohortSchedule.builder()
                        .cohort(cohort)
                        .lesson(savedLesson)
                        .unlockAt(unlockInstant)
                        .deadlineAt(deadlineInstant)
                        .showScoreType(scoreType)
                        .allowReviewAnswers(allowReview)
                        .build();
                cohortScheduleRepository.save(schedule);
                revertCohortToDraftIfApproved(cohort);
            });
        }
        if (section != null && section.getCohort() != null) {
            revertCohortToDraftIfApproved(section.getCohort());
        }
        if (section != null && section.getCourse() != null) {
            revertCourseToDraftIfPublished(section.getCourse());
        }

        log.info("[LessonService] Đã tạo thành công bài học ID: {}", savedLesson.getId());
        return savedLesson;
    }

    @Override
    @Transactional
    public Lesson updateLesson(Long lessonId, LessonRequestDTO request, Long instructorId) {
        Lesson lesson = getLessonById(lessonId);
        Section section = lesson.getSection();

        if (!isAuthorized(section, instructorId)) {
            throw new RuntimeException("Bạn không có quyền sửa bài học này");
        }

        validateAndNormalizeVideoLesson(request);

        lesson.setTitle(request.getTitle());
        lesson.setLessonType(request.getLessonType());
        lesson.setVideoUrl(request.getVideoUrl());
        lesson.setDocumentContent(request.getDocumentContent());
        lesson.setAssetUrl(request.getAssetUrl());
        
        if (request.getDurationSeconds() != null && request.getDurationSeconds() > 0) {
            lesson.setDurationSeconds(request.getDurationSeconds());
        } else if ("VIDEO".equalsIgnoreCase(request.getLessonType()) && request.getVideoUrl() != null && !request.getVideoUrl().isBlank()) {
            Integer detected = cloudinaryService.getVideoDuration(request.getVideoUrl());
            if (detected != null && detected > 0) {
                lesson.setDurationSeconds(detected);
            }
        }
        if (request.getIsFreePreview() != null) {
            lesson.setIsFreePreview(request.getIsFreePreview());
        }
        if (request.getIsPublicForCohorts() != null) {
            lesson.setIsPublicForCohorts(request.getIsPublicForCohorts());
        }
        if (request.getOrderIndex() != null) {
            lesson.setOrderIndex(request.getOrderIndex());
        }
        lesson.setStatus("DRAFT");

        Lesson savedLesson = lessonRepository.save(lesson);

        Long targetCohortId = request.getCohortId();
        if (targetCohortId == null && section != null && section.getCohort() != null) {
            targetCohortId = section.getCohort().getId();
        }
        if (targetCohortId != null) {
            Optional<CohortSchedule> scheduleOpt = cohortScheduleRepository.findByCohortIdAndLessonId(targetCohortId, savedLesson.getId());
            if (scheduleOpt.isPresent()) {
                CohortSchedule cs = scheduleOpt.get();
                if (request.getUnlockAt() != null && !request.getUnlockAt().isBlank()) {
                    Instant unlockInstant = parseDateTime(request.getUnlockAt());
                    if (unlockInstant != null) {
                        cs.setUnlockAt(unlockInstant);
                    }
                }
                if (request.getDeadlineAt() != null) {
                    if (request.getDeadlineAt().isBlank()) {
                        cs.setDeadlineAt(null);
                    } else {
                        Instant deadlineInstant = parseDateTime(request.getDeadlineAt());
                        cs.setDeadlineAt(deadlineInstant);
                    }
                }
                if (request.getShowScoreType() != null && !request.getShowScoreType().isBlank()) {
                    cs.setShowScoreType(request.getShowScoreType());
                }
                if (request.getAllowReviewAnswers() != null) {
                    cs.setAllowReviewAnswers(request.getAllowReviewAnswers());
                }
                cohortScheduleRepository.save(cs);
                revertCohortToDraftIfApproved(targetCohortId);
            } else {
                cohortRepository.findById(targetCohortId).ifPresent(cohort -> {
                    Instant unlockInstant = parseDateTime(request.getUnlockAt());
                    if (unlockInstant == null) {
                        unlockInstant = Instant.now();
                    }
                    Instant deadlineInstant = parseDateTime(request.getDeadlineAt());
                    String scoreType = (request.getShowScoreType() != null && !request.getShowScoreType().isBlank())
                            ? request.getShowScoreType() : "IMMEDIATELY";
                    Boolean allowReview = request.getAllowReviewAnswers() != null ? request.getAllowReviewAnswers() : true;

                    CohortSchedule schedule = CohortSchedule.builder()
                            .cohort(cohort)
                            .lesson(savedLesson)
                            .unlockAt(unlockInstant)
                            .deadlineAt(deadlineInstant)
                            .showScoreType(scoreType)
                            .allowReviewAnswers(allowReview)
                            .build();
                    cohortScheduleRepository.save(schedule);
                    revertCohortToDraftIfApproved(cohort);
                });
            }
        }
        if (section != null && section.getCohort() != null) {
            revertCohortToDraftIfApproved(section.getCohort());
        }
        List<CohortSchedule> schedules = cohortScheduleRepository.findByLessonId(lessonId);
        for (CohortSchedule cs : schedules) {
            if (cs.getCohort() != null) {
                revertCohortToDraftIfApproved(cs.getCohort());
            }
        }
        if (section != null && section.getCourse() != null) {
            revertCourseToDraftIfPublished(section.getCourse());
        }

        log.info("[LessonService] Đã cập nhật thành công bài học ID: {}", savedLesson.getId());
        return savedLesson;
    }

    @Override
    @Transactional
    public void deleteLesson(Long lessonId, Long instructorId) {
        Lesson lesson = getLessonById(lessonId);
        Section section = lesson.getSection();

        if (!isAuthorized(section, instructorId)) {
            throw new RuntimeException("Bạn không có quyền xóa bài học này");
        }

        if (lesson.isPublished()) {
            List<CohortSchedule> schedules = cohortScheduleRepository.findByLessonId(lessonId);
            for (CohortSchedule cs : schedules) {
                if (cs.getCohort() != null) {
                    revertCohortToDraftIfApproved(cs.getCohort());
                }
            }
            if (section != null && section.getCohort() != null) {
                revertCohortToDraftIfApproved(section.getCohort());
            }
            if (section != null && section.getCourse() != null) {
                revertCourseToDraftIfPublished(section.getCourse());
            }
        }

        cohortScheduleRepository.deleteByLessonId(lessonId);
        lessonRepository.delete(lesson);
        log.info("[LessonService] Đã xóa thành công bài học ID: {}", lessonId);
    }

    @Override
    @Transactional
    public void reorderLessons(Long sectionId, List<Long> lessonIds, Long instructorId) {
        if (lessonIds == null || lessonIds.isEmpty()) return;
        Section targetSection = (sectionId != null && sectionId > 0) ? sectionRepository.findById(sectionId).orElse(null) : null;
        if (targetSection != null) {
            if (targetSection.getCohort() != null) {
                revertCohortToDraftIfApproved(targetSection.getCohort());
            }
            if (targetSection.getCourse() != null) {
                revertCourseToDraftIfPublished(targetSection.getCourse());
            }
        }
        for (int i = 0; i < lessonIds.size(); i++) {
            Long lId = lessonIds.get(i);
            Lesson l = lessonRepository.findById(lId).orElse(null);
            if (l != null) {
                l.setOrderIndex(i + 1);
                if (targetSection != null) {
                    l.setSection(targetSection);
                }
                lessonRepository.save(l);
            }
        }
    }

    private void revertCohortToDraftIfApproved(Cohort cohort) {
        if (cohort == null) {
            return;
        }
        if ("APPROVED".equalsIgnoreCase(cohort.getModerationStatus())) {
            cohort.setModerationStatus("DRAFT");
            cohortRepository.save(cohort);
            log.info("[LessonService] Chuyển module id: {} về DRAFT do cập nhật bài học", cohort.getId());
        }
        if (cohort.getCourse() != null) {
            revertCourseToDraftIfPublished(cohort.getCourse());
        }
    }

    private void revertCohortToDraftIfApproved(Long cohortId) {
        if (cohortId != null) {
            cohortRepository.findById(cohortId).ifPresent(this::revertCohortToDraftIfApproved);
        }
    }

    private void revertCourseToDraftIfPublished(vn.edu.ute.utelearn.entity.Course course) {
        if (course != null && "PUBLISHED".equalsIgnoreCase(course.getStatus())) {
            course.setStatus("DRAFT");
            courseRepository.save(course);
            log.info("[LessonService] Chuyển khóa học id: {} về DRAFT do cập nhật bài học", course.getId());
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
}

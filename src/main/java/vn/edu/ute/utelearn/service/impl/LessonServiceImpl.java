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
import vn.edu.ute.utelearn.service.SectionService;

import java.util.List;

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
    private final CloudinaryService cloudinaryService;

    private boolean isAuthorized(Section section, Long instructorId) {
        if (instructorId == null) {
            return false;
        }
        User user = userRepository.findById(instructorId).orElse(null);
        if (user != null && user.getRoles() != null) {
            boolean hasAdminOrMod = user.getRoles().stream().anyMatch(r ->
                    "ADMIN".equalsIgnoreCase(r.getCode()) || "ROLE_ADMIN".equalsIgnoreCase(r.getCode()) ||
                    "MODERATOR".equalsIgnoreCase(r.getCode()) || "ROLE_MODERATOR".equalsIgnoreCase(r.getCode()));
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
        if ("VIDEO".equalsIgnoreCase(request.getLessonType()) && request.getVideoUrl() != null && !request.getVideoUrl().isBlank()) {
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
                vn.edu.ute.utelearn.entity.CohortSchedule schedule = vn.edu.ute.utelearn.entity.CohortSchedule.builder()
                        .cohort(cohort)
                        .lesson(savedLesson)
                        .unlockAt(java.time.Instant.now())
                        .build();
                cohortScheduleRepository.save(schedule);
                revertCohortToDraftIfApproved(cohort);
            });
        } else if (section != null && section.getCohort() != null) {
            revertCohortToDraftIfApproved(section.getCohort());
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
            boolean scheduleExists = cohortScheduleRepository.findByCohortIdAndLessonId(targetCohortId, savedLesson.getId()).isPresent();
            if (!scheduleExists) {
                cohortRepository.findById(targetCohortId).ifPresent(cohort -> {
                    vn.edu.ute.utelearn.entity.CohortSchedule schedule = vn.edu.ute.utelearn.entity.CohortSchedule.builder()
                            .cohort(cohort)
                            .lesson(savedLesson)
                            .unlockAt(java.time.Instant.now())
                            .build();
                    cohortScheduleRepository.save(schedule);
                    revertCohortToDraftIfApproved(cohort);
                });
            } else {
                revertCohortToDraftIfApproved(targetCohortId);
            }
        } else if (section != null && section.getCohort() != null) {
            revertCohortToDraftIfApproved(section.getCohort());
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

        if (section != null && section.getCohort() != null) {
            revertCohortToDraftIfApproved(section.getCohort());
        }
        List<CohortSchedule> schedules = cohortScheduleRepository.findByLessonId(lessonId);
        for (CohortSchedule cs : schedules) {
            if (cs.getCohort() != null) {
                revertCohortToDraftIfApproved(cs.getCohort());
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
        if (cohort != null && cohort.getCourse() == null && "APPROVED".equalsIgnoreCase(cohort.getModerationStatus())) {
            cohort.setModerationStatus("DRAFT");
            cohortRepository.save(cohort);
            log.info("[LessonService] Chuyển module độc lập id: {} về DRAFT do cập nhật bài học", cohort.getId());
        }
    }

    private void revertCohortToDraftIfApproved(Long cohortId) {
        if (cohortId != null) {
            cohortRepository.findById(cohortId).ifPresent(this::revertCohortToDraftIfApproved);
        }
    }
}

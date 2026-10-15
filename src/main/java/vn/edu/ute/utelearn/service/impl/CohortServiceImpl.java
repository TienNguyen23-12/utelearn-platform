package vn.edu.ute.utelearn.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.utelearn.dao.CohortRepository;
import vn.edu.ute.utelearn.dao.CohortScheduleRepository;
import vn.edu.ute.utelearn.dao.CourseRepository;
import vn.edu.ute.utelearn.dao.LessonRepository;
import vn.edu.ute.utelearn.dto.CohortRequestDTO;
import vn.edu.ute.utelearn.dto.CohortScheduleRequestDTO;
import vn.edu.ute.utelearn.entity.Cohort;
import vn.edu.ute.utelearn.entity.CohortSchedule;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.CohortService;

import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CohortServiceImpl implements CohortService {

    private final CohortRepository cohortRepository;
    private final CohortScheduleRepository cohortScheduleRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final vn.edu.ute.utelearn.dao.CohortMemberRepository cohortMemberRepository;
    private final vn.edu.ute.utelearn.dao.SectionRepository sectionRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Override
    public Page<Cohort> getAllCohorts(Pageable pageable) {
        return cohortRepository.findAll(pageable);
    }

    @Override
    public Page<Cohort> getCohortsByInstructor(Long instructorId, Pageable pageable) {
        return cohortRepository.findByInstructorId(instructorId, pageable);
    }

    @Override
    public Page<Cohort> searchCohortsByInstructor(Long instructorId, String keyword, String status, Pageable pageable) {
        String safeKeyword = (keyword == null) ? "" : keyword.trim();
        String safeStatus = (status == null) ? "" : status.trim();
        return cohortRepository.searchCohortsByInstructor(instructorId, safeKeyword, safeStatus, pageable);
    }

    @Override
    public Page<Cohort> searchPublicCohorts(String keyword, String categorySlug, Pageable pageable) {
        String safeKeyword = (keyword == null) ? "" : keyword.trim();
        String safeCategory = (categorySlug == null) ? "" : categorySlug.trim();
        return cohortRepository.searchPublicCohorts(safeKeyword, safeCategory, pageable);
    }

    @Override
    public Cohort getCohortById(Long id) {
        return cohortRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cohort not found"));
    }

    @Override
    public Cohort getCohortByIdAndInstructor(Long id, Long instructorId) {
        return cohortRepository.findByIdAndInstructorId(id, instructorId)
                .orElseThrow(() -> new RuntimeException("Cohort not found or unauthorized"));
    }

    @Override
    @Transactional
    public Cohort createCohort(CohortRequestDTO dto, User instructor) {
        Course course = null;
        if (dto.getCourseId() != null) {
            course = courseRepository.findById(dto.getCourseId())
                    .orElseThrow(() -> new RuntimeException("Course not found"));

            if (!course.getCreatedBy().getId().equals(instructor.getId())) {
                throw new RuntimeException("Not authorized to create cohort for this course");
            }
        }

        if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            String prefix = course != null ? course.getCode() : "MOD";
            long count = course != null ? cohortRepository.countByCourseId(course.getId()) : cohortRepository.count();
            String year = String.valueOf(java.time.Year.now().getValue());
            String autoCode = String.format("%s-%s-%02d", prefix, year, count + 1);
            while (cohortRepository.existsByCode(autoCode)) {
                count++;
                autoCode = String.format("%s-%s-%02d", prefix, year, count + 1);
            }
            dto.setCode(autoCode);
        } else if (cohortRepository.existsByCode(dto.getCode())) {
            throw new RuntimeException("Cohort code already exists");
        }

        String finalThumbnail = dto.getThumbnailUrl();
        if (finalThumbnail == null || finalThumbnail.trim().isEmpty()) {
            finalThumbnail = (course != null && course.getThumbnailUrl() != null && !course.getThumbnailUrl().isBlank())
                    ? course.getThumbnailUrl()
                    : "/images/courses/default-cover.png";
        } else {
            finalThumbnail = finalThumbnail.trim();
        }

        Cohort cohort = Cohort.builder()
                .course(course)
                .createdBy(instructor)
                .code(dto.getCode())
                .name(dto.getName())
                .enrollmentStart(dto.getEnrollmentStart() != null ? dto.getEnrollmentStart().atZone(ZoneId.systemDefault()).toInstant() : null)
                .enrollmentEnd(dto.getEnrollmentEnd() != null ? dto.getEnrollmentEnd().atZone(ZoneId.systemDefault()).toInstant() : null)
                .studyStart(dto.getStudyStart() != null ? dto.getStudyStart().atZone(ZoneId.systemDefault()).toInstant() : null)
                .studyEnd(dto.getStudyEnd() != null ? dto.getStudyEnd().atZone(ZoneId.systemDefault()).toInstant() : null)
                .maxCapacity(dto.getMaxCapacity())
                .price(dto.getPrice())
                .status(dto.getStatus())
                .visibility(dto.getVisibility() != null && !dto.getVisibility().isBlank() ? dto.getVisibility() : "PUBLIC")
                .thumbnailUrl(finalThumbnail)
                .description(dto.getDescription())
                .objectives(dto.getObjectives())
                .build();

        Cohort savedCohort = cohortRepository.save(cohort);
        saveSchedules(savedCohort, dto.getSchedules());
        return savedCohort;
    }

    @Override
    @Transactional
    public Cohort updateCohort(Long id, CohortRequestDTO dto, User instructor) {
        Cohort cohort = getCohortByIdAndInstructor(id, instructor.getId());

        if (!cohort.getCode().equals(dto.getCode()) && cohortRepository.existsByCode(dto.getCode())) {
            throw new RuntimeException("Cohort code already exists");
        }

        if (cohort.getCreatedBy() == null) {
            cohort.setCreatedBy(instructor);
        }
        if (dto.getCourseId() != null) {
            if (cohort.getCourse() == null || !cohort.getCourse().getId().equals(dto.getCourseId())) {
                Course course = courseRepository.findById(dto.getCourseId())
                        .orElseThrow(() -> new RuntimeException("Course not found"));
                if (!course.getCreatedBy().getId().equals(instructor.getId())) {
                    throw new RuntimeException("Not authorized to assign this course");
                }
                cohort.setCourse(course);
            }
        } else {
            cohort.setCourse(null);
        }

        cohort.setCode(dto.getCode());
        cohort.setName(dto.getName());
        if (dto.getEnrollmentStart() != null) cohort.setEnrollmentStart(dto.getEnrollmentStart().atZone(ZoneId.systemDefault()).toInstant()); else cohort.setEnrollmentStart(null);
        if (dto.getEnrollmentEnd() != null) cohort.setEnrollmentEnd(dto.getEnrollmentEnd().atZone(ZoneId.systemDefault()).toInstant()); else cohort.setEnrollmentEnd(null);
        if (dto.getStudyStart() != null) cohort.setStudyStart(dto.getStudyStart().atZone(ZoneId.systemDefault()).toInstant()); else cohort.setStudyStart(null);
        if (dto.getStudyEnd() != null) cohort.setStudyEnd(dto.getStudyEnd().atZone(ZoneId.systemDefault()).toInstant()); else cohort.setStudyEnd(null);
        cohort.setMaxCapacity(dto.getMaxCapacity());
        cohort.setPrice(dto.getPrice());
        cohort.setStatus(dto.getStatus());
        cohort.setVisibility(dto.getVisibility() != null && !dto.getVisibility().isBlank() ? dto.getVisibility() : "PUBLIC");
        cohort.setDescription(dto.getDescription());
        cohort.setObjectives(dto.getObjectives());

        String finalThumbnail = dto.getThumbnailUrl();
        if (finalThumbnail == null || finalThumbnail.trim().isEmpty()) {
            finalThumbnail = (cohort.getCourse() != null && cohort.getCourse().getThumbnailUrl() != null && !cohort.getCourse().getThumbnailUrl().isBlank())
                    ? cohort.getCourse().getThumbnailUrl()
                    : "/images/courses/default-cover.png";
        } else {
            finalThumbnail = finalThumbnail.trim();
        }
        cohort.setThumbnailUrl(finalThumbnail);

        Cohort savedCohort = cohortRepository.save(cohort);
        if (dto.getSchedules() != null && !dto.getSchedules().isEmpty()) {
            boolean hasValidSchedule = dto.getSchedules().stream().anyMatch(s -> s.getUnlockAt() != null);
            if (hasValidSchedule) {
                cohortScheduleRepository.deleteByCohortId(savedCohort.getId());
                saveSchedules(savedCohort, dto.getSchedules());
            }
        }
        return savedCohort;
    }

    private void saveSchedules(Cohort cohort, List<CohortScheduleRequestDTO> scheduleDTOs) {
        if (scheduleDTOs == null || scheduleDTOs.isEmpty()) return;
        for (CohortScheduleRequestDTO req : scheduleDTOs) {
            if (req.getUnlockAt() == null) continue; // Skip if no unlock time
            lessonRepository.findById(req.getLessonId()).ifPresent(lesson -> {
                CohortSchedule schedule = CohortSchedule.builder()
                        .cohort(cohort)
                        .lesson(lesson)
                        .unlockAt(req.getUnlockAt().atZone(ZoneId.systemDefault()).toInstant())
                        .deadlineAt(req.getDeadlineAt() != null ? req.getDeadlineAt().atZone(ZoneId.systemDefault()).toInstant() : null)
                        .showScoreType(req.getShowScoreType())
                        .allowReviewAnswers(req.getAllowReviewAnswers() != null ? req.getAllowReviewAnswers() : false)
                        .isScorePublished(true)
                        .build();
                cohortScheduleRepository.save(schedule);
            });
        }
    }

    @Override
    @Transactional
    public void deleteCohort(Long id, User instructor) {
        Cohort cohort = cohortRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Module"));

        boolean isOwner = (cohort.getCreatedBy() != null && cohort.getCreatedBy().getId().equals(instructor.getId()))
                || (cohort.getCourse() != null && cohort.getCourse().getCreatedBy() != null && cohort.getCourse().getCreatedBy().getId().equals(instructor.getId()));
        boolean isAdmin = instructor.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getCode()) || "ROLE_ADMIN".equals(r.getCode()));
        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Bạn không có quyền xóa Module này");
        }

        long enrolledCount = cohortMemberRepository.countByCohort_Id(id);
        if (enrolledCount > 0) {
            throw new RuntimeException("Không thể xóa Module đã có " + enrolledCount + " học viên đăng ký!");
        }

        List<vn.edu.ute.utelearn.entity.Section> sections = sectionRepository.findByCohortIdOrderByOrderIndexAsc(id);
        for (vn.edu.ute.utelearn.entity.Section section : sections) {
            List<vn.edu.ute.utelearn.entity.Lesson> lessons = lessonRepository.findBySectionIdOrderByOrderIndexAsc(section.getId());
            for (vn.edu.ute.utelearn.entity.Lesson lesson : lessons) {
                cohortScheduleRepository.deleteByLessonId(lesson.getId());
                jdbcTemplate.update("DELETE FROM coding_problems WHERE lesson_id = ?", lesson.getId());
                jdbcTemplate.update("DELETE FROM quizzes WHERE lesson_id = ?", lesson.getId());
                lessonRepository.delete(lesson);
            }
            sectionRepository.delete(section);
        }

        cohortScheduleRepository.deleteByCohortId(id);
        jdbcTemplate.update("DELETE FROM cohort_messages WHERE cohort_id = ?", id);
        jdbcTemplate.update("DELETE FROM quiz_submissions WHERE cohort_id = ?", id);
        jdbcTemplate.update("DELETE FROM coding_submissions WHERE cohort_id = ?", id);
        jdbcTemplate.update("UPDATE moderation_tasks SET cohort_id = NULL WHERE cohort_id = ?", id);
        jdbcTemplate.update("DELETE FROM cohort_members WHERE cohort_id = ?", id);

        cohortRepository.delete(cohort);
    }

    @Override
    public List<CohortSchedule> getCohortSchedules(Long cohortId) {
        return cohortScheduleRepository.findByCohortId(cohortId);
    }

    @Override
    @Transactional
    public void updateSchedules(Long cohortId, List<CohortSchedule> schedules, User instructor) {
        Cohort cohort = getCohortByIdAndInstructor(cohortId, instructor.getId());
        for (CohortSchedule schedule : schedules) {
            schedule.setCohort(cohort);
            cohortScheduleRepository.save(schedule);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public vn.edu.ute.utelearn.dto.UserEnrollmentSummaryDTO getUserEnrollmentSummary(User user) {
        List<vn.edu.ute.utelearn.entity.CohortMember> allMemberships = cohortMemberRepository.findByUserOrderByEnrolledAtDesc(user);

        java.util.Map<Long, List<vn.edu.ute.utelearn.entity.CohortMember>> courseMembershipsMap = new java.util.LinkedHashMap<>();
        java.util.Map<Long, Course> courseMap = new java.util.LinkedHashMap<>();
        for (vn.edu.ute.utelearn.entity.CohortMember cm : allMemberships) {
            if (cm.getCohort() != null && cm.getCohort().getCourse() != null) {
                Course c = cm.getCohort().getCourse();
                courseMembershipsMap.computeIfAbsent(c.getId(), k -> new java.util.ArrayList<>()).add(cm);
                courseMap.putIfAbsent(c.getId(), c);
            }
        }

        List<vn.edu.ute.utelearn.dto.UserEnrollmentSummaryDTO.EnrolledCourseItem> enrolledCourses = new java.util.ArrayList<>();
        List<vn.edu.ute.utelearn.entity.CohortMember> enrolledModules = new java.util.ArrayList<>();

        for (java.util.Map.Entry<Long, List<vn.edu.ute.utelearn.entity.CohortMember>> entry : courseMembershipsMap.entrySet()) {
            Long courseId = entry.getKey();
            List<vn.edu.ute.utelearn.entity.CohortMember> memberships = entry.getValue();
            Course course = courseMap.get(courseId);

            List<Cohort> allCohortsOfCourse = cohortRepository.findByCourseIdOrderByCreatedAtAsc(courseId);
            java.util.Set<Long> userEnrolledCohortIds = memberships.stream()
                    .map(cm -> cm.getCohort().getId())
                    .collect(java.util.stream.Collectors.toSet());

            boolean hasAllCohort = memberships.stream().anyMatch(cm -> 
                cm.getCohort().getCode() != null && cm.getCohort().getCode().endsWith("-ALL")
            );

            boolean isAllCohortsEnrolled = !allCohortsOfCourse.isEmpty() && 
                allCohortsOfCourse.stream().allMatch(c -> userEnrolledCohortIds.contains(c.getId()));

            boolean isFullCourse = hasAllCohort || isAllCohortsEnrolled;

            if (isFullCourse) {
                java.time.Instant earliestEnrolledAt = memberships.stream()
                        .map(vn.edu.ute.utelearn.entity.CohortMember::getEnrolledAt)
                        .filter(java.util.Objects::nonNull)
                        .min(java.util.Comparator.naturalOrder())
                        .orElse(java.time.Instant.now());

                enrolledCourses.add(vn.edu.ute.utelearn.dto.UserEnrollmentSummaryDTO.EnrolledCourseItem.builder()
                        .course(course)
                        .enrolledAt(earliestEnrolledAt)
                        .totalModules(allCohortsOfCourse.size())
                        .enrolledModulesCount(memberships.size())
                        .build());
            } else {
                enrolledModules.addAll(memberships);
            }
        }

        return vn.edu.ute.utelearn.dto.UserEnrollmentSummaryDTO.builder()
                .enrolledCourses(enrolledCourses)
                .enrolledModules(enrolledModules)
                .build();
    }

    @Override
    @Transactional
    public boolean updateLastAccessedLesson(Long cohortId, Long lessonId, User user) {
        if (cohortId == null || lessonId == null || user == null) {
            return false;
        }

        java.util.Optional<vn.edu.ute.utelearn.entity.CohortMember> memberOpt = cohortMemberRepository.findByCohort_IdAndUser_Id(cohortId, user.getId());
        if (memberOpt.isEmpty()) {
            return false;
        }

        java.util.Optional<vn.edu.ute.utelearn.entity.Lesson> lessonOpt = lessonRepository.findById(lessonId);
        if (lessonOpt.isEmpty()) {
            return false;
        }

        vn.edu.ute.utelearn.entity.CohortMember member = memberOpt.get();
        member.setLastLesson(lessonOpt.get());
        member.setLastAccessedAt(java.time.Instant.now());
        cohortMemberRepository.save(member);
        log.info("[CohortService] Cập nhật bài học gần nhất cho user {} tại cohort {}: lesson {}", user.getId(), cohortId, lessonId);
        return true;
    }
}

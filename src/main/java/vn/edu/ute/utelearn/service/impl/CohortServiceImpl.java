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
import vn.edu.ute.utelearn.entity.CohortMember;
import vn.edu.ute.utelearn.entity.CohortSchedule;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.Lesson;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.service.CohortService;
import vn.edu.ute.utelearn.util.SecurityUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

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
    private final vn.edu.ute.utelearn.dao.LessonCompletionRepository lessonCompletionRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private vn.edu.ute.utelearn.dao.EnrollmentRepository enrollmentRepository;

    @Override
    public Page<Cohort> getAllCohorts(Pageable pageable) {
        syncCohortStatuses();
        return cohortRepository.findAll(pageable);
    }

    @Override
    public Page<Cohort> getCohortsByInstructor(Long instructorId, Pageable pageable) {
        syncCohortStatuses();
        return cohortRepository.findByInstructorId(instructorId, pageable);
    }

    @Override
    public Page<Cohort> searchCohortsByInstructor(Long instructorId, String keyword, String status, Pageable pageable) {
        syncCohortStatuses();
        String safeKeyword = (keyword == null) ? "" : keyword.trim();
        String safeStatus = (status == null) ? "" : status.trim();
        return cohortRepository.searchCohortsByInstructor(instructorId, safeKeyword, safeStatus, pageable);
    }

    @Override
    public Page<Cohort> searchAllCohorts(String keyword, String status, Pageable pageable) {
        syncCohortStatuses();
        String safeKeyword = (keyword == null) ? "" : keyword.trim();
        String safeStatus = (status == null) ? "" : status.trim();
        return cohortRepository.searchAllCohorts(safeKeyword, safeStatus, pageable);
    }

    @Override
    public Page<Cohort> searchPublicCohorts(String keyword, String categorySlug, Pageable pageable) {
        syncCohortStatuses();
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

        String initialModerationStatus = (course == null) ? "DRAFT" : "APPROVED";

        if ("WEIGHTED_GRADING".equalsIgnoreCase(dto.getGradingPolicy())) {
            int pw = dto.getProcessWeight() != null ? dto.getProcessWeight() : 0;
            int fw = dto.getFinalWeight() != null ? dto.getFinalWeight() : 0;
            if (pw + fw != 100) {
                throw new RuntimeException("Tổng trọng số điểm quá trình và cuối kỳ phải bằng 100%");
            }
        }

        Lesson finalLesson = null;
        if (dto.getFinalLessonId() != null) {
            finalLesson = lessonRepository.findById(dto.getFinalLessonId()).orElse(null);
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
                .moderationStatus(initialModerationStatus)
                .visibility(dto.getVisibility() != null && !dto.getVisibility().isBlank() ? dto.getVisibility() : "PUBLIC")
                .thumbnailUrl(finalThumbnail)
                .description(dto.getDescription())
                .objectives(formatObjectives(dto.getObjectives()))
                .announcementTitle(dto.getAnnouncementTitle())
                .announcementContent(dto.getAnnouncementContent())
                .gradingPolicy(dto.getGradingPolicy() != null && !dto.getGradingPolicy().isBlank() ? dto.getGradingPolicy() : "PROGRESS_BASED")
                .processWeight(dto.getProcessWeight() != null ? dto.getProcessWeight() : 50)
                .finalWeight(dto.getFinalWeight() != null ? dto.getFinalWeight() : 50)
                .finalLesson(finalLesson)
                .minPassingGrade(dto.getMinPassingGrade() != null ? dto.getMinPassingGrade() : new BigDecimal("5.00"))
                .minFinalExamScore(dto.getMinFinalExamScore() != null ? dto.getMinFinalExamScore() : new BigDecimal("3.00"))
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
                cohort.setModerationStatus("APPROVED");
            }
        } else {
            if (cohort.getCourse() != null) {
                cohort.setCourse(null);
            }
        }

        boolean coreInfoChanged = false;
        if (dto.getName() != null && !dto.getName().equals(cohort.getName())) coreInfoChanged = true;
        if (dto.getCode() != null && !dto.getCode().equals(cohort.getCode())) coreInfoChanged = true;
        if (dto.getPrice() != null && (cohort.getPrice() == null || cohort.getPrice().compareTo(dto.getPrice()) != 0)) coreInfoChanged = true;
        if (dto.getDescription() != null && !Objects.equals(dto.getDescription(), cohort.getDescription())) coreInfoChanged = true;
        if (dto.getObjectives() != null && !Objects.equals(formatObjectives(dto.getObjectives()), cohort.getObjectives())) coreInfoChanged = true;

        if (coreInfoChanged && "APPROVED".equalsIgnoreCase(cohort.getModerationStatus())) {
            cohort.setModerationStatus("DRAFT");
            log.info("[CohortService] Chuyển module id: {} về DRAFT do cập nhật thông tin cốt lõi", id);
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
        cohort.setObjectives(formatObjectives(dto.getObjectives()));
        cohort.setAnnouncementTitle(dto.getAnnouncementTitle());
        cohort.setAnnouncementContent(dto.getAnnouncementContent());

        if ("WEIGHTED_GRADING".equalsIgnoreCase(dto.getGradingPolicy())) {
            int pw = dto.getProcessWeight() != null ? dto.getProcessWeight() : 0;
            int fw = dto.getFinalWeight() != null ? dto.getFinalWeight() : 0;
            if (pw + fw != 100) {
                throw new RuntimeException("Tổng trọng số điểm quá trình và cuối kỳ phải bằng 100%");
            }
        }

        cohort.setGradingPolicy(dto.getGradingPolicy() != null && !dto.getGradingPolicy().isBlank() ? dto.getGradingPolicy() : "PROGRESS_BASED");
        cohort.setProcessWeight(dto.getProcessWeight() != null ? dto.getProcessWeight() : 50);
        cohort.setFinalWeight(dto.getFinalWeight() != null ? dto.getFinalWeight() : 50);
        if (dto.getFinalLessonId() != null) {
            cohort.setFinalLesson(lessonRepository.findById(dto.getFinalLessonId()).orElse(null));
        } else {
            cohort.setFinalLesson(null);
        }
        if (dto.getMinPassingGrade() != null) cohort.setMinPassingGrade(dto.getMinPassingGrade());
        if (dto.getMinFinalExamScore() != null) cohort.setMinFinalExamScore(dto.getMinFinalExamScore());

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
            if (req.getUnlockAt() == null) continue;
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
        boolean isAdmin = SecurityUtils.isAdmin(instructor);
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

    private boolean isFullCourseCohort(Cohort c) {
        if (c == null) {
            return false;
        }
        boolean isCodeAll = c.getCode() != null && c.getCode().endsWith("-ALL");
        boolean isNameAll = c.getName() != null && c.getName().toLowerCase().startsWith("toàn bộ khóa học");
        return isCodeAll || isNameAll;
    }

    @Override
    @Transactional
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
        List<vn.edu.ute.utelearn.dto.CourseUpsellDTO> upsellSuggestions = new java.util.ArrayList<>();

        for (java.util.Map.Entry<Long, List<vn.edu.ute.utelearn.entity.CohortMember>> entry : courseMembershipsMap.entrySet()) {
            Long courseId = entry.getKey();
            List<vn.edu.ute.utelearn.entity.CohortMember> memberships = entry.getValue();
            Course course = courseMap.get(courseId);

            List<Cohort> allCohortsOfCourse = cohortRepository.findByCourseIdOrderByCreatedAtAsc(courseId);
            List<Cohort> realModules = allCohortsOfCourse.stream()
                    .filter(c -> !isFullCourseCohort(c))
                    .collect(java.util.stream.Collectors.toList());
            List<Cohort> modulesToConsider = realModules.isEmpty() ? allCohortsOfCourse : realModules;

            java.util.Set<Long> userEnrolledCohortIds = memberships.stream()
                    .map(cm -> cm.getCohort().getId())
                    .collect(java.util.stream.Collectors.toSet());

            boolean hasAllCohort = memberships.stream().anyMatch(cm -> isFullCourseCohort(cm.getCohort()));

            boolean isAllCohortsEnrolled = !modulesToConsider.isEmpty() && 
                modulesToConsider.stream().allMatch(c -> userEnrolledCohortIds.contains(c.getId()));

            boolean isFullCourse = hasAllCohort || isAllCohortsEnrolled;

            if (isFullCourse) {
                java.time.Instant earliestEnrolledAt = memberships.stream()
                        .map(vn.edu.ute.utelearn.entity.CohortMember::getEnrolledAt)
                        .filter(java.util.Objects::nonNull)
                        .min(java.util.Comparator.naturalOrder())
                        .orElse(java.time.Instant.now());

                checkAndSyncCourseCompletion(course, user);
                vn.edu.ute.utelearn.entity.Enrollment enrollment = (enrollmentRepository != null)
                        ? enrollmentRepository.findByUser_IdAndCourse_Id(user.getId(), courseId).orElse(null)
                        : null;

                int completedModulesCount = 0;
                for (Cohort mod : modulesToConsider) {
                    CohortMember modMember = memberships.stream()
                            .filter(m -> m.getCohort() != null && m.getCohort().getId().equals(mod.getId()))
                            .findFirst()
                            .orElse(null);
                    if (modMember != null && Boolean.TRUE.equals(modMember.getIsPassed())) {
                        completedModulesCount++;
                    }
                }
                int progressPercent = (modulesToConsider.size() > 0)
                        ? (completedModulesCount * 100 / modulesToConsider.size()) : 0;
                boolean isCompleted = (enrollment != null) ? Boolean.TRUE.equals(enrollment.getIsCompleted())
                        : (!modulesToConsider.isEmpty() && completedModulesCount >= modulesToConsider.size());
                java.math.BigDecimal averageGrade = (enrollment != null) ? enrollment.getAverageGrade() : null;
                String certificateUrl = (enrollment != null) ? enrollment.getCertificateUrl() : null;
                boolean hasReviewed = (enrollment != null && enrollment.hasReviewed());
                Integer rating = (enrollment != null) ? enrollment.getRating() : null;
                String reviewComment = (enrollment != null) ? enrollment.getReviewComment() : null;

                enrolledCourses.add(vn.edu.ute.utelearn.dto.UserEnrollmentSummaryDTO.EnrolledCourseItem.builder()
                        .course(course)
                        .enrolledAt(earliestEnrolledAt)
                        .totalModules(modulesToConsider.size())
                        .enrolledModulesCount(modulesToConsider.size())
                        .completedModulesCount(completedModulesCount)
                        .progressPercent(progressPercent)
                        .isCompleted(isCompleted)
                        .averageGrade(averageGrade)
                        .certificateUrl(certificateUrl)
                        .hasReviewed(hasReviewed)
                        .rating(rating)
                        .reviewComment(reviewComment)
                        .build());
            } else {
                List<vn.edu.ute.utelearn.entity.CohortMember> realMemberships = memberships.stream()
                        .filter(cm -> !isFullCourseCohort(cm.getCohort()))
                        .collect(java.util.stream.Collectors.toList());

                enrolledModules.addAll(realMemberships);

                List<Cohort> unenrolledCohorts = modulesToConsider.stream()
                        .filter(c -> !userEnrolledCohortIds.contains(c.getId()))
                        .collect(java.util.stream.Collectors.toList());

                if (!unenrolledCohorts.isEmpty()) {
                    java.math.BigDecimal coursePrice = course.getPrice() != null ? course.getPrice() : java.math.BigDecimal.ZERO;
                    java.math.BigDecimal totalPaidAmount = realMemberships.stream()
                            .map(cm -> cm.getCohort() != null && cm.getCohort().getPrice() != null ? cm.getCohort().getPrice() : java.math.BigDecimal.ZERO)
                            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

                    java.math.BigDecimal upgradePrice = coursePrice.subtract(totalPaidAmount);
                    if (upgradePrice.compareTo(java.math.BigDecimal.ZERO) < 0) {
                        upgradePrice = java.math.BigDecimal.ZERO;
                    }

                    java.math.BigDecimal remainingModulesOriginalPrice = unenrolledCohorts.stream()
                            .map(c -> c.getPrice() != null ? c.getPrice() : java.math.BigDecimal.ZERO)
                            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

                    java.math.BigDecimal savingsAmount = remainingModulesOriginalPrice.compareTo(upgradePrice) > 0
                            ? remainingModulesOriginalPrice.subtract(upgradePrice)
                            : java.math.BigDecimal.ZERO;

                    upsellSuggestions.add(vn.edu.ute.utelearn.dto.CourseUpsellDTO.builder()
                            .course(course)
                            .totalModules(modulesToConsider.size())
                            .enrolledModulesCount(realMemberships.size())
                            .remainingModulesCount(unenrolledCohorts.size())
                            .coursePrice(coursePrice)
                            .totalPaidAmount(totalPaidAmount)
                            .upgradePrice(upgradePrice)
                            .remainingModulesOriginalPrice(remainingModulesOriginalPrice)
                            .savingsAmount(savingsAmount)
                            .remainingCohorts(unenrolledCohorts)
                            .build());
                }
            }
        }

        return vn.edu.ute.utelearn.dto.UserEnrollmentSummaryDTO.builder()
                .enrolledCourses(enrolledCourses)
                .enrolledModules(enrolledModules)
                .upsellSuggestions(upsellSuggestions)
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

    @Override
    @Transactional
    public java.util.Map<String, Object> toggleLessonCompletion(Long cohortId, Long lessonId, User user) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        if (cohortId == null || lessonId == null || user == null) {
            response.put("success", false);
            response.put("message", "Tham số không hợp lệ");
            return response;
        }

        Cohort cohort = cohortRepository.findById(cohortId).orElse(null);
        vn.edu.ute.utelearn.entity.Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (cohort == null || lesson == null) {
            response.put("success", false);
            response.put("message", "Không tìm thấy module hoặc bài học");
            return response;
        }

        boolean isCompletedNow;
        boolean exists = lessonCompletionRepository.existsByCohort_IdAndLesson_IdAndUser_Id(cohortId, lessonId, user.getId());
        if (exists) {
            lessonCompletionRepository.deleteByCohortIdAndLessonIdAndUserId(cohortId, lessonId, user.getId());
            isCompletedNow = false;
        } else {
            vn.edu.ute.utelearn.entity.LessonCompletion completion = vn.edu.ute.utelearn.entity.LessonCompletion.builder()
                    .cohort(cohort)
                    .lesson(lesson)
                    .user(user)
                    .build();
            lessonCompletionRepository.save(completion);
            isCompletedNow = true;
        }

        long completedCount = lessonCompletionRepository.countByCohort_IdAndUser_Id(cohortId, user.getId());
        List<Long> cohortLessonIds = cohortScheduleRepository.findLessonIdsByCohortId(cohortId);
        int totalCount = cohortLessonIds != null ? cohortLessonIds.size() : 0;
        int progressPercent = (totalCount > 0) ? (int) Math.min(100, Math.round(((double) completedCount / totalCount) * 100)) : 0;

        boolean isPassed = false;
        java.util.Optional<vn.edu.ute.utelearn.entity.CohortMember> memberOpt = cohortMemberRepository.findByCohort_IdAndUser_Id(cohortId, user.getId());
        if (memberOpt.isPresent()) {
            vn.edu.ute.utelearn.entity.CohortMember member = memberOpt.get();
            if (totalCount > 0 && completedCount >= totalCount) {
                member.setIsPassed(true);
                isPassed = true;
            }
            cohortMemberRepository.save(member);
            if (cohort.getCourse() != null) {
                checkAndSyncCourseCompletion(cohort.getCourse(), user);
            }
        }

        response.put("success", true);
        response.put("completed", isCompletedNow);
        response.put("completedCount", completedCount);
        response.put("totalCount", totalCount);
        response.put("progressPercent", progressPercent);
        response.put("isPassed", isPassed);
        log.info("[CohortService] Toggle bài học id: {} cho user: {} tại cohort: {}. Trạng thái mới: {}, Tiến độ: {}%",
                lessonId, user.getEmail(), cohortId, isCompletedNow, progressPercent);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getCompletedLessonIds(Long cohortId, Long userId) {
        if (cohortId == null || userId == null) {
            return java.util.Collections.emptyList();
        }
        return lessonCompletionRepository.findCompletedLessonIds(cohortId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getCohortProgress(Long cohortId, Long userId) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        if (cohortId == null || userId == null) {
            response.put("completedCount", 0);
            response.put("totalCount", 0);
            response.put("progressPercent", 0);
            response.put("completedLessonIds", java.util.Collections.emptyList());
            return response;
        }

        List<Long> completedIds = lessonCompletionRepository.findCompletedLessonIds(cohortId, userId);
        List<Long> totalLessonIds = cohortScheduleRepository.findLessonIdsByCohortId(cohortId);
        int total = totalLessonIds != null ? totalLessonIds.size() : 0;
        int count = completedIds != null ? completedIds.size() : 0;
        int percent = (total > 0) ? (int) Math.min(100, Math.round(((double) count / total) * 100)) : 0;

        response.put("completedCount", count);
        response.put("totalCount", total);
        response.put("progressPercent", percent);
        response.put("completedLessonIds", completedIds != null ? completedIds : java.util.Collections.emptyList());
        return response;
    }

    private String formatObjectives(String objectivesStr) {
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

    @Override
    @Transactional
    public void revertToDraftIfApproved(Long cohortId) {
        if (cohortId == null) {
            return;
        }
        cohortRepository.findById(cohortId).ifPresent(this::revertToDraftIfApproved);
    }

    @Override
    @Transactional
    public void revertToDraftIfApproved(Cohort cohort) {
        if (cohort != null && "APPROVED".equalsIgnoreCase(cohort.getModerationStatus())) {
            cohort.setModerationStatus("DRAFT");
            cohortRepository.save(cohort);
            log.info("[CohortService] Tự động chuyển trạng thái Module id: {} về DRAFT do có thay đổi nội dung", cohort.getId());
        }
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void syncCohortStatuses() {
        try {
            Instant now = Instant.now();
            int enrolledCount = cohortRepository.autoPromoteUpcomingToEnrolling(now);
            int inProgressCount = cohortRepository.autoPromoteToInProgress(now);
            int completedCount = cohortRepository.autoPromoteToCompleted(now);
            int expiredCount = cohortRepository.autoCloseExpiredEnrollmentWithoutStudy(now);
            if (enrolledCount > 0 || inProgressCount > 0 || completedCount > 0 || expiredCount > 0) {
                log.info("[CohortService] Dong bo trang thai dot hoc: {} mo ghi danh, {} dang hoc, {} hoan thanh, {} het han",
                        enrolledCount, inProgressCount, completedCount, expiredCount);
            }
        } catch (Exception e) {
            log.warn("[CohortService] Khong the dong bo trang thai dot hoc: {}", e.getMessage());
        }
    }

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void onApplicationReady() {
        syncCohortStatuses();
    }

    @Override
    @Transactional
    public void checkAndSyncCourseCompletion(Course course, User user) {
        if (course == null || user == null) {
            return;
        }

        List<Cohort> allCohortsOfCourse = cohortRepository.findByCourseIdOrderByCreatedAtAsc(course.getId());
        List<Cohort> realModules = allCohortsOfCourse.stream()
                .filter(c -> !isFullCourseCohort(c))
                .toList();
        if (realModules.isEmpty()) {
            return;
        }

        List<CohortMember> memberships = cohortMemberRepository.findByCohort_Course_IdAndUser_Id(course.getId(), user.getId());
        int completedModulesCount = 0;
        java.math.BigDecimal sumGrades = java.math.BigDecimal.ZERO;
        int gradedCount = 0;

        for (Cohort mod : realModules) {
            CohortMember cm = memberships.stream()
                    .filter(m -> m.getCohort() != null && m.getCohort().getId().equals(mod.getId()))
                    .findFirst()
                    .orElse(null);
            if (cm != null && Boolean.TRUE.equals(cm.getIsPassed())) {
                completedModulesCount++;
                if (cm.getFinalGrade() != null) {
                    sumGrades = sumGrades.add(cm.getFinalGrade());
                    gradedCount++;
                }
            }
        }

        boolean allPassed = (completedModulesCount >= realModules.size());
        java.math.BigDecimal avgGrade = (gradedCount > 0)
                ? sumGrades.divide(java.math.BigDecimal.valueOf(gradedCount), 2, java.math.RoundingMode.HALF_UP)
                : null;
        double progress = ((double) completedModulesCount / realModules.size()) * 100.0;

        if (enrollmentRepository != null) {
            vn.edu.ute.utelearn.entity.Enrollment enrollment = enrollmentRepository.findByUser_IdAndCourse_Id(user.getId(), course.getId())
                    .orElseGet(() -> vn.edu.ute.utelearn.entity.Enrollment.builder()
                            .user(user)
                            .course(course)
                            .build());

            enrollment.setProgress(progress);
            if (allPassed) {
                enrollment.setIsCompleted(true);
                if (enrollment.getCompletedAt() == null) {
                    enrollment.setCompletedAt(java.time.Instant.now());
                }
                enrollment.setAverageGrade(avgGrade);
                if (enrollment.getCertificateUrl() == null || enrollment.getCertificateUrl().isBlank()) {
                    String certCode = "UTE-CERT-" + (course.getCode() != null ? course.getCode() : "CRS") + "-" + user.getId();
                    enrollment.setCertificateUrl("/certificates/verify?code=" + certCode);
                }
                log.info("[CohortService] Học viên {} đã hoàn thành toàn bộ khóa học: {} (Điểm TB: {})",
                        user.getEmail(), course.getCode(), avgGrade);
            }
            enrollmentRepository.save(enrollment);
        }
    }
}


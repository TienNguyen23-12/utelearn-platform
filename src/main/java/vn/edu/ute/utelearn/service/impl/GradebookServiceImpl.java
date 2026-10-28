package vn.edu.ute.utelearn.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.ute.utelearn.dao.*;
import vn.edu.ute.utelearn.dto.*;
import vn.edu.ute.utelearn.entity.*;
import vn.edu.ute.utelearn.service.GradebookService;
import vn.edu.ute.utelearn.util.SecurityUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GradebookServiceImpl implements GradebookService {

    private final CohortRepository cohortRepository;
    private final CohortMemberRepository cohortMemberRepository;
    private final CohortScheduleRepository cohortScheduleRepository;
    private final LessonCompletionRepository lessonCompletionRepository;
    private final LessonRepository lessonRepository;
    private final QuizRepository quizRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final UserRepository userRepository;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private vn.edu.ute.utelearn.service.CohortService cohortService;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CourseRepository courseRepository;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private EnrollmentRepository enrollmentRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    @Override
    @Transactional
    public GradebookDTO getGradebook(Long cohortId, Long instructorId) {
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đợt học"));
        checkInstructorOrAdmin(cohort, instructorId);

        Long finalLessonId = resolveFinalLessonId(cohort);
        List<Long> cohortLessonIds = resolveCohortLessonIds(cohort);
        int totalLessons = (cohortLessonIds != null) ? cohortLessonIds.size() : 0;

        List<Long> processLessonIds = new ArrayList<>();
        if (cohortLessonIds != null) {
            for (Long lid : cohortLessonIds) {
                if (finalLessonId == null || !finalLessonId.equals(lid)) {
                    processLessonIds.add(lid);
                }
            }
        }

        List<GradeColumnDTO> allColumns = buildGradeColumns(cohort, cohortLessonIds, finalLessonId);
        List<GradeColumnDTO> processColumns = allColumns.stream().filter(c -> !c.isFinalExam()).collect(Collectors.toList());
        GradeColumnDTO finalExamColumn = allColumns.stream().filter(GradeColumnDTO::isFinalExam).findFirst().orElse(null);

        List<CohortMember> members = cohortMemberRepository.findByCohort_IdOrderByEnrolledAtAsc(cohortId);
        List<StudentGradeItemDTO> studentItems = new ArrayList<>();
        int passedCount = 0;
        BigDecimal sumFinalGrades = BigDecimal.ZERO;
        int gradedCount = 0;

        for (CohortMember m : members) {
            User u = m.getUser();
            int completedCount = (int) lessonCompletionRepository.countByCohort_IdAndUser_Id(cohortId, u.getId());
            int progressPercent = (totalLessons > 0) ? (int) Math.min(100, Math.round(((double) completedCount / totalLessons) * 100)) : 0;

            BigDecimal currentFinalExam = calculateFinalExamScore(cohortId, u.getId(), finalLessonId);
            BigDecimal currentProcess = calculateProcessScore(cohortId, u.getId(), processLessonIds, completedCount, totalLessons);

            boolean hasNewFinalExam = (m.getFinalExamGrade() == null || m.getFinalExamGrade().compareTo(BigDecimal.ZERO) == 0)
                    && (currentFinalExam != null && currentFinalExam.compareTo(BigDecimal.ZERO) > 0);

            if (hasNewFinalExam) {
                BigDecimal processScore = (m.getProcessGrade() != null && m.getProcessGrade().compareTo(BigDecimal.ZERO) > 0)
                        ? m.getProcessGrade() : currentProcess;
                applyGradingRule(cohort, m, processScore, currentFinalExam, completedCount, totalLessons);
                cohortMemberRepository.save(m);
            } else if (m.getFinalGrade() == null) {
                BigDecimal processScore = (m.getProcessGrade() != null) ? m.getProcessGrade() : currentProcess;
                BigDecimal finalScore = (m.getFinalExamGrade() != null) ? m.getFinalExamGrade() : currentFinalExam;
                applyGradingRule(cohort, m, processScore, finalScore, completedCount, totalLessons);
                cohortMemberRepository.save(m);
            }

            if (Boolean.TRUE.equals(m.getIsPassed())) {
                passedCount++;
            }
            if (m.getFinalGrade() != null) {
                sumFinalGrades = sumFinalGrades.add(m.getFinalGrade());
                gradedCount++;
            }

            Map<Long, StudentColumnGradeDTO> colGrades = buildStudentColumnGrades(allColumns, u.getId(), cohortId);
            List<StudentColumnGradeDTO> studentProcessColGrades = new ArrayList<>();
            for (GradeColumnDTO pc : processColumns) {
                StudentColumnGradeDTO scg = colGrades.get(pc.getLessonId());
                if (scg != null) {
                    studentProcessColGrades.add(scg);
                }
            }
            StudentColumnGradeDTO studentFinalExamColGrade = (finalExamColumn != null) ? colGrades.get(finalExamColumn.getLessonId()) : null;
            if (studentFinalExamColGrade != null && studentFinalExamColGrade.getScore() == null && m.getFinalExamGrade() != null) {
                studentFinalExamColGrade.setScore(m.getFinalExamGrade());
                if (studentFinalExamColGrade.getStatus() == null || "NOT_SUBMITTED".equals(studentFinalExamColGrade.getStatus())) {
                    studentFinalExamColGrade.setStatus("GRADED");
                }
            }

            studentItems.add(StudentGradeItemDTO.builder()
                    .memberId(m.getId())
                    .userId(u.getId())
                    .fullName(u.getFullName())
                    .email(u.getEmail())
                    .avatarUrl(u.getAvatarUrl())
                    .enrolledAt(m.getEnrolledAt())
                    .completedLessonsCount(completedCount)
                    .totalLessonsCount(totalLessons)
                    .progressPercent(progressPercent)
                    .processGrade(m.getProcessGrade())
                    .finalExamGrade(m.getFinalExamGrade())
                    .finalGrade(m.getFinalGrade())
                    .isPassed(m.getIsPassed())
                    .certificateUrl(m.getCertificateUrl())
                    .gradedAt(m.getGradedAt())
                    .rating(m.getRating())
                    .reviewComment(m.getReviewComment())
                    .reviewedAt(m.getReviewedAt())
                    .hasReviewed(m.hasReviewed())
                    .columnGrades(colGrades)
                    .processColumnGrades(studentProcessColGrades)
                    .finalExamColumnGrade(studentFinalExamColGrade)
                    .build());
        }

        int totalStudents = members.size();
        int failedCount = totalStudents - passedCount;
        double passRate = (totalStudents > 0) ? Math.round(((double) passedCount / totalStudents) * 1000.0) / 10.0 : 0.0;
        BigDecimal avgGrade = (gradedCount > 0) ? sumFinalGrades.divide(BigDecimal.valueOf(gradedCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        return GradebookDTO.builder()
                .cohort(cohort)
                .totalStudents(totalStudents)
                .passedCount(passedCount)
                .failedCount(failedCount)
                .passRate(passRate)
                .averageGrade(avgGrade)
                .students(studentItems)
                .columns(allColumns)
                .processColumns(processColumns)
                .finalExamColumn(finalExamColumn)
                .build();
    }

    @Override
    @Transactional
    public void calculateAndSaveGrades(Long cohortId, Long instructorId) {
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đợt học"));
        checkInstructorOrAdmin(cohort, instructorId);

        log.info("[GradebookService] Bắt đầu tính toán bảng điểm cho Cohort: {}, chính sách: {}", cohortId, cohort.getGradingPolicy());

        List<CohortMember> members = cohortMemberRepository.findByCohort_IdOrderByEnrolledAtAsc(cohortId);
        List<Long> cohortLessonIds = cohortScheduleRepository.findLessonIdsByCohortId(cohortId);
        int totalLessons = (cohortLessonIds != null) ? cohortLessonIds.size() : 0;

        Long finalLessonId = resolveFinalLessonId(cohort);
        if (cohort.getFinalLesson() == null && finalLessonId != null) {
            lessonRepository.findById(finalLessonId).ifPresent(l -> {
                cohort.setFinalLesson(l);
                cohortRepository.save(cohort);
            });
        }

        List<Long> processLessonIds = new ArrayList<>();
        if (cohortLessonIds != null) {
            for (Long lid : cohortLessonIds) {
                if (finalLessonId == null || !finalLessonId.equals(lid)) {
                    processLessonIds.add(lid);
                }
            }
        }

        for (CohortMember member : members) {
            User student = member.getUser();
            int completedCount = (int) lessonCompletionRepository.countByCohort_IdAndUser_Id(cohortId, student.getId());

            BigDecimal processScore = calculateProcessScore(cohortId, student.getId(), processLessonIds, completedCount, totalLessons);
            BigDecimal finalExamScore = calculateFinalExamScore(cohortId, student.getId(), finalLessonId);

            applyGradingRule(cohort, member, processScore, finalExamScore, completedCount, totalLessons);
            cohortMemberRepository.save(member);
        }

        log.info("[GradebookService] Hoàn tất tính toán bảng điểm cho {} học viên của Cohort: {}", members.size(), cohortId);
    }

    @Override
    @Transactional
    public void publishGrades(Long cohortId, Long instructorId) {
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đợt học"));
        checkInstructorOrAdmin(cohort, instructorId);

        cohort.setIsGradesPublished(true);
        cohortRepository.save(cohort);
        log.info("[GradebookService] Giảng viên {} đã công bố bảng điểm cho Cohort: {}", instructorId, cohortId);
    }

    @Override
    @Transactional
    public StudentGradeItemDTO getStudentGrade(Long cohortId, Long userId) {
        CohortMember m = cohortMemberRepository.findByCohort_IdAndUser_Id(cohortId, userId).orElse(null);
        Cohort cohort = (m != null) ? m.getCohort() : cohortRepository.findById(cohortId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đợt học"));

        Long finalLessonId = resolveFinalLessonId(cohort);
        List<Long> cohortLessonIds = resolveCohortLessonIds(cohort);
        int totalLessons = (cohortLessonIds != null) ? cohortLessonIds.size() : 0;
        int completedCount = (userId != null) ? (int) lessonCompletionRepository.countByCohort_IdAndUser_Id(cohortId, userId) : 0;
        int progressPercent = (totalLessons > 0) ? (int) Math.min(100, Math.round(((double) completedCount / totalLessons) * 100)) : 0;

        List<Long> processLessonIds = new ArrayList<>();
        if (cohortLessonIds != null) {
            for (Long lid : cohortLessonIds) {
                if (finalLessonId == null || !finalLessonId.equals(lid)) {
                    processLessonIds.add(lid);
                }
            }
        }

        BigDecimal currentFinalExam = calculateFinalExamScore(cohortId, userId, finalLessonId);
        BigDecimal currentProcess = calculateProcessScore(cohortId, userId, processLessonIds, completedCount, totalLessons);

        if (m != null) {
            boolean hasNewFinalExam = (m.getFinalExamGrade() == null || m.getFinalExamGrade().compareTo(BigDecimal.ZERO) == 0)
                    && (currentFinalExam != null && currentFinalExam.compareTo(BigDecimal.ZERO) > 0);

            if (hasNewFinalExam) {
                BigDecimal processScore = (m.getProcessGrade() != null && m.getProcessGrade().compareTo(BigDecimal.ZERO) > 0)
                        ? m.getProcessGrade() : currentProcess;
                applyGradingRule(cohort, m, processScore, currentFinalExam, completedCount, totalLessons);
                m = cohortMemberRepository.save(m);
            } else if (m.getFinalGrade() == null) {
                BigDecimal processScore = (m.getProcessGrade() != null) ? m.getProcessGrade() : currentProcess;
                BigDecimal finalScore = (m.getFinalExamGrade() != null) ? m.getFinalExamGrade() : currentFinalExam;
                applyGradingRule(cohort, m, processScore, finalScore, completedCount, totalLessons);
                m = cohortMemberRepository.save(m);
            }
        }

        List<GradeColumnDTO> allColumns = buildGradeColumns(cohort, cohortLessonIds, finalLessonId);
        List<GradeColumnDTO> processColumns = allColumns.stream().filter(c -> !c.isFinalExam()).collect(Collectors.toList());
        GradeColumnDTO finalExamColumn = allColumns.stream().filter(GradeColumnDTO::isFinalExam).findFirst().orElse(null);

        Map<Long, StudentColumnGradeDTO> colGrades = buildStudentColumnGrades(allColumns, userId, cohortId);
        List<StudentColumnGradeDTO> studentProcessColGrades = new ArrayList<>();
        for (GradeColumnDTO pc : processColumns) {
            StudentColumnGradeDTO scg = colGrades.get(pc.getLessonId());
            if (scg != null) {
                studentProcessColGrades.add(scg);
            }
        }
        StudentColumnGradeDTO studentFinalExamColGrade = (finalExamColumn != null) ? colGrades.get(finalExamColumn.getLessonId()) : null;
        if (m != null && studentFinalExamColGrade != null && studentFinalExamColGrade.getScore() == null && m.getFinalExamGrade() != null) {
            studentFinalExamColGrade.setScore(m.getFinalExamGrade());
            if (studentFinalExamColGrade.getStatus() == null || "NOT_SUBMITTED".equals(studentFinalExamColGrade.getStatus())) {
                studentFinalExamColGrade.setStatus("GRADED");
            }
        }

        User u = (m != null) ? m.getUser() : userRepository.findById(userId).orElse(null);

        return StudentGradeItemDTO.builder()
                .memberId(m != null ? m.getId() : null)
                .userId(userId)
                .fullName(u != null ? u.getFullName() : "")
                .email(u != null ? u.getEmail() : "")
                .avatarUrl(u != null ? u.getAvatarUrl() : null)
                .enrolledAt(m != null ? m.getEnrolledAt() : null)
                .completedLessonsCount(completedCount)
                .totalLessonsCount(totalLessons)
                .progressPercent(progressPercent)
                .processGrade(m != null ? m.getProcessGrade() : (currentProcess != null && currentProcess.compareTo(BigDecimal.ZERO) > 0 ? currentProcess : null))
                .finalExamGrade(m != null ? m.getFinalExamGrade() : (currentFinalExam != null && currentFinalExam.compareTo(BigDecimal.ZERO) > 0 ? currentFinalExam : null))
                .finalGrade(m != null ? m.getFinalGrade() : null)
                .isPassed(m != null ? m.getIsPassed() : false)
                .certificateUrl(m != null ? m.getCertificateUrl() : null)
                .gradedAt(m != null ? m.getGradedAt() : null)
                .rating(m != null ? m.getRating() : null)
                .reviewComment(m != null ? m.getReviewComment() : null)
                .reviewedAt(m != null ? m.getReviewedAt() : null)
                .hasReviewed(m != null && m.hasReviewed())
                .columnGrades(colGrades)
                .processColumnGrades(studentProcessColGrades)
                .finalExamColumnGrade(studentFinalExamColGrade)
                .build();
    }

    @Override
    @Transactional
    public void syncStudentGrade(Long cohortId, Long userId) {
        Cohort cohort = cohortRepository.findById(cohortId).orElse(null);
        if (cohort == null) {
            return;
        }

        CohortMember member = cohortMemberRepository.findByCohort_IdAndUser_Id(cohortId, userId).orElse(null);
        if (member == null) {
            return;
        }

        List<Long> cohortLessonIds = resolveCohortLessonIds(cohort);
        int totalLessons = (cohortLessonIds != null) ? cohortLessonIds.size() : 0;
        Long finalLessonId = resolveFinalLessonId(cohort);

        List<Long> processLessonIds = new ArrayList<>();
        if (cohortLessonIds != null) {
            for (Long lid : cohortLessonIds) {
                if (finalLessonId == null || !finalLessonId.equals(lid)) {
                    processLessonIds.add(lid);
                }
            }
        }

        int completedCount = (int) lessonCompletionRepository.countByCohort_IdAndUser_Id(cohortId, userId);
        BigDecimal currentProcess = calculateProcessScore(cohortId, userId, processLessonIds, completedCount, totalLessons);
        BigDecimal currentFinalExam = calculateFinalExamScore(cohortId, userId, finalLessonId);

        BigDecimal processScore = (currentProcess != null && currentProcess.compareTo(BigDecimal.ZERO) > 0)
                ? currentProcess : member.getProcessGrade();
        BigDecimal finalScore = (currentFinalExam != null && currentFinalExam.compareTo(BigDecimal.ZERO) > 0)
                ? currentFinalExam : member.getFinalExamGrade();

        applyGradingRule(cohort, member, processScore, finalScore, completedCount, totalLessons);
        cohortMemberRepository.save(member);
        log.info("[GradebookService] Đã đồng bộ điểm học viên {}: Quá trình={}, Cuối kỳ={}, Tổng kết={}, Đạt={}",
                userId, member.getProcessGrade(), member.getFinalExamGrade(), member.getFinalGrade(), member.getIsPassed());
    }

    private Long resolveFinalLessonId(Cohort cohort) {
        if (cohort == null) {
            return null;
        }
        if (cohort.getFinalLesson() != null) {
            return cohort.getFinalLesson().getId();
        }

        List<CohortSchedule> schedules = cohortScheduleRepository.findByCohortIdWithDetails(cohort.getId());
        List<Lesson> candidates = new ArrayList<>();
        if (schedules != null && !schedules.isEmpty()) {
            for (CohortSchedule cs : schedules) {
                if (cs.getLesson() != null) {
                    candidates.add(cs.getLesson());
                }
            }
        } else if (cohort.getCourse() != null) {
            candidates = lessonRepository.findBySectionCourseIdOrderByOrderIndexAsc(cohort.getCourse().getId());
        }

        if (candidates.isEmpty()) {
            return null;
        }

        List<Lesson> testLessons = new ArrayList<>();
        for (Lesson l : candidates) {
            String type = l.getLessonType() != null ? l.getLessonType().toUpperCase() : "";
            if ("ASSIGNMENT".equals(type) || "QUIZ".equals(type) || "PROJECT".equals(type) || "EXAM".equals(type)) {
                testLessons.add(l);
            }
        }

        if (testLessons.isEmpty()) {
            return null;
        }

        for (int i = testLessons.size() - 1; i >= 0; i--) {
            Lesson l = testLessons.get(i);
            String title = l.getTitle() != null ? l.getTitle().toLowerCase() : "";
            if (title.contains("cuối kỳ") || title.contains("final") || title.contains("bài tập lớn")
                    || title.contains("đồ án") || title.contains("tổng kết") || title.contains("kết thúc") || title.contains("báo cáo")) {
                return l.getId();
            }
        }

        return testLessons.get(testLessons.size() - 1).getId();
    }

    private BigDecimal calculateProcessScore(Long cohortId, Long userId, List<Long> processLessonIds, int completedCount, int totalLessons) {
        List<BigDecimal> taskScores = new ArrayList<>();

        for (Long lessonId : processLessonIds) {
            Optional<Quiz> quizOpt = quizRepository.findByLessonId(lessonId);
            if (quizOpt.isPresent()) {
                Optional<QuizSubmission> subOpt = quizSubmissionRepository
                        .findTopByQuizIdAndUserIdAndCohortIdOrderByScoreDesc(quizOpt.get().getId(), userId, cohortId);
                if (subOpt.isEmpty()) {
                    subOpt = quizSubmissionRepository.findTopByQuizIdAndUserIdOrderByScoreDesc(quizOpt.get().getId(), userId);
                }
                if (subOpt.isPresent() && subOpt.get().getScore() != null) {
                    taskScores.add(subOpt.get().getScore());
                }
                continue;
            }

            Optional<Assignment> assignOpt = assignmentRepository.findByLessonId(lessonId);
            if (assignOpt.isPresent()) {
                Optional<AssignmentSubmission> subOpt = assignmentSubmissionRepository
                        .findByAssignmentIdAndUserIdAndCohortId(assignOpt.get().getId(), userId, cohortId);
                if (subOpt.isEmpty()) {
                    subOpt = assignmentSubmissionRepository.findByAssignmentIdAndUserId(assignOpt.get().getId(), userId);
                }
                if (subOpt.isPresent() && subOpt.get().getScore() != null) {
                    taskScores.add(subOpt.get().getScore());
                }
            }
        }

        if (!taskScores.isEmpty()) {
            BigDecimal sum = BigDecimal.ZERO;
            for (BigDecimal sc : taskScores) {
                sum = sum.add(sc != null ? sc : BigDecimal.ZERO);
            }
            return sum.divide(BigDecimal.valueOf(taskScores.size()), 2, RoundingMode.HALF_UP);
        }

        if (totalLessons > 0) {
            return BigDecimal.valueOf(((double) completedCount / totalLessons) * 10.0).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateFinalExamScore(Long cohortId, Long userId, Long finalLessonId) {
        if (finalLessonId == null) {
            Cohort cohort = cohortRepository.findById(cohortId).orElse(null);
            finalLessonId = resolveFinalLessonId(cohort);
        }
        if (finalLessonId == null) {
            return BigDecimal.ZERO;
        }

        Optional<Quiz> quizOpt = quizRepository.findByLessonId(finalLessonId);
        if (quizOpt.isPresent()) {
            Optional<QuizSubmission> subOpt = quizSubmissionRepository
                    .findTopByQuizIdAndUserIdAndCohortIdOrderByScoreDesc(quizOpt.get().getId(), userId, cohortId);
            if (subOpt.isEmpty()) {
                subOpt = quizSubmissionRepository.findTopByQuizIdAndUserIdOrderByScoreDesc(quizOpt.get().getId(), userId);
            }
            if (subOpt.isPresent() && subOpt.get().getScore() != null) {
                return subOpt.get().getScore();
            }
            return BigDecimal.ZERO;
        }

        Optional<Assignment> assignOpt = assignmentRepository.findByLessonId(finalLessonId);
        if (assignOpt.isPresent()) {
            Optional<AssignmentSubmission> subOpt = assignmentSubmissionRepository
                    .findByAssignmentIdAndUserIdAndCohortId(assignOpt.get().getId(), userId, cohortId);
            if (subOpt.isEmpty()) {
                subOpt = assignmentSubmissionRepository.findByAssignmentIdAndUserId(assignOpt.get().getId(), userId);
            }
            if (subOpt.isPresent() && subOpt.get().getScore() != null) {
                return subOpt.get().getScore();
            }
            return BigDecimal.ZERO;
        }

        return BigDecimal.ZERO;
    }

    private void checkInstructorOrAdmin(Cohort cohort, Long instructorId) {
        if (cohort == null || instructorId == null) {
            throw new RuntimeException("Truy cập không hợp lệ");
        }
        User user = userRepository.findById(instructorId).orElse(null);
        if (user == null) {
            throw new RuntimeException("Người dùng không tồn tại");
        }
        boolean isAdmin = SecurityUtils.isAdmin(user);
        boolean isTeacherOrInstructor = SecurityUtils.isInstructor(user);
        boolean isOwner = (cohort.getCreatedBy() != null && cohort.getCreatedBy().getId().equals(instructorId))
                || (cohort.getCourse() != null && cohort.getCourse().getCreatedBy() != null && cohort.getCourse().getCreatedBy().getId().equals(instructorId));
        boolean isUnassignedOrPublic = (cohort.getCreatedBy() == null)
                || (cohort.getVisibility() == null || "PUBLIC".equalsIgnoreCase(cohort.getVisibility()));

        if (!isAdmin && !isOwner && !(isTeacherOrInstructor && isUnassignedOrPublic)) {
            throw new RuntimeException("Bạn không có quyền quản lý bảng điểm của lớp học này");
        }
    }

    private void applyGradingRule(Cohort cohort, CohortMember member, BigDecimal processScore, BigDecimal finalExamScore, int completedCount, int totalLessons) {
        String policy = cohort.getGradingPolicy() != null ? cohort.getGradingPolicy() : "PROGRESS_BASED";
        BigDecimal calculatedFinalGrade;
        boolean isPassed;

        if ("EXIT_EXAM".equalsIgnoreCase(policy)) {
            calculatedFinalGrade = finalExamScore != null ? finalExamScore : BigDecimal.ZERO;
            BigDecimal minPassing = cohort.getMinPassingGrade() != null ? cohort.getMinPassingGrade() : new BigDecimal("5.00");
            isPassed = calculatedFinalGrade.compareTo(minPassing) >= 0;
        } else if ("WEIGHTED_GRADING".equalsIgnoreCase(policy)) {
            int pw = cohort.getProcessWeight() != null ? cohort.getProcessWeight() : 50;
            int fw = cohort.getFinalWeight() != null ? cohort.getFinalWeight() : 50;

            BigDecimal pScore = processScore != null ? processScore : BigDecimal.ZERO;
            BigDecimal fScore = finalExamScore != null ? finalExamScore : BigDecimal.ZERO;

            BigDecimal weightedProcess = pScore.multiply(BigDecimal.valueOf(pw));
            BigDecimal weightedFinal = fScore.multiply(BigDecimal.valueOf(fw));
            calculatedFinalGrade = weightedProcess.add(weightedFinal).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            BigDecimal minPassing = cohort.getMinPassingGrade() != null ? cohort.getMinPassingGrade() : new BigDecimal("5.00");
            BigDecimal minFinal = cohort.getMinFinalExamScore() != null ? cohort.getMinFinalExamScore() : new BigDecimal("3.00");

            boolean meetsOverall = calculatedFinalGrade.compareTo(minPassing) >= 0;
            boolean meetsFinal = fScore.compareTo(minFinal) >= 0;
            isPassed = meetsOverall && meetsFinal;
        } else {
            isPassed = (totalLessons > 0 && completedCount >= totalLessons);
            calculatedFinalGrade = (totalLessons > 0)
                    ? BigDecimal.valueOf(((double) completedCount / totalLessons) * 10.0).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            processScore = calculatedFinalGrade;
            finalExamScore = calculatedFinalGrade;
        }

        member.setProcessGrade(processScore);
        member.setFinalExamGrade(finalExamScore);
        member.setFinalGrade(calculatedFinalGrade);
        member.setIsPassed(isPassed);
        member.setGradedAt(Instant.now());

        if (isPassed && (member.getCertificateUrl() == null || member.getCertificateUrl().isBlank())) {
            String certCode = "UTE-CERT-" + (cohort.getCode() != null ? cohort.getCode() : "MOD") + "-" + member.getUser().getId();
            member.setCertificateUrl("/certificates/verify?code=" + certCode);
        }

        if (cohort.getCourse() != null && cohortService != null) {
            try {
                cohortService.checkAndSyncCourseCompletion(cohort.getCourse(), member.getUser());
            } catch (Exception e) {
                log.warn("[GradebookService] Không thể đồng bộ hoàn thành khóa học cho user {}: {}", member.getUser().getId(), e.getMessage());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportGradebookToExcel(Long cohortId, Long instructorId) {
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đợt học"));
        checkInstructorOrAdmin(cohort, instructorId);

        GradebookDTO gradebook = getGradebook(cohortId, instructorId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Bang_Diem");

            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(IndexedColors.ROYAL_BLUE.getIndex());

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            Font subFont = workbook.createFont();
            subFont.setItalic(true);
            subFont.setFontHeightInPoints((short) 10);
            subFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

            CellStyle subStyle = workbook.createCellStyle();
            subStyle.setFont(subFont);

            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("BẢNG ĐIỂM & ĐÁNH GIÁ KẾT THÚC KHÓA HỌC - UTELEARN LMS");
            c0.setCellStyle(titleStyle);

            Row r1 = sheet.createRow(1);
            Cell c1 = r1.createCell(0);
            c1.setCellValue("Đợt học: " + cohort.getName() + " | Mã đợt: " + cohort.getCode());
            c1.setCellStyle(subStyle);

            String policyDesc;
            if ("WEIGHTED_GRADING".equalsIgnoreCase(cohort.getGradingPolicy())) {
                policyDesc = "Mô hình: Trọng số (" + cohort.getProcessWeight() + "% Quá trình + " + cohort.getFinalWeight() + "% Cuối kỳ)"
                        + " | Điểm sàn đạt: " + cohort.getMinPassingGrade() + "/10"
                        + " | Điểm sàn cuối kỳ: " + cohort.getMinFinalExamScore() + "/10";
            } else if ("EXIT_EXAM".equalsIgnoreCase(cohort.getGradingPolicy())) {
                policyDesc = "Mô hình: Bài thi đầu ra bắt buộc | Điểm sàn đạt: " + cohort.getMinPassingGrade() + "/10";
            } else {
                policyDesc = "Mô hình: Theo tiến độ 100% bài học";
            }

            Row r2 = sheet.createRow(2);
            Cell c2 = r2.createCell(0);
            c2.setCellValue("Quy tắc đánh giá: " + policyDesc);
            c2.setCellStyle(subStyle);

            String exportDate = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault()).format(Instant.now());
            Row r3 = sheet.createRow(3);
            Cell c3 = r3.createCell(0);
            c3.setCellValue("Thời gian xuất file: " + exportDate + " | Tổng sĩ số: " + gradebook.getTotalStudents() + " học viên");
            c3.setCellStyle(subStyle);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            centerStyle.setBorderTop(BorderStyle.THIN);
            centerStyle.setBorderBottom(BorderStyle.THIN);
            centerStyle.setBorderLeft(BorderStyle.THIN);
            centerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle passStyle = workbook.createCellStyle();
            passStyle.setAlignment(HorizontalAlignment.CENTER);
            passStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            passStyle.setBorderTop(BorderStyle.THIN);
            passStyle.setBorderBottom(BorderStyle.THIN);
            passStyle.setBorderLeft(BorderStyle.THIN);
            passStyle.setBorderRight(BorderStyle.THIN);
            Font passFont = workbook.createFont();
            passFont.setBold(true);
            passFont.setColor(IndexedColors.GREEN.getIndex());
            passStyle.setFont(passFont);

            CellStyle failStyle = workbook.createCellStyle();
            failStyle.setAlignment(HorizontalAlignment.CENTER);
            failStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            failStyle.setBorderTop(BorderStyle.THIN);
            failStyle.setBorderBottom(BorderStyle.THIN);
            failStyle.setBorderLeft(BorderStyle.THIN);
            failStyle.setBorderRight(BorderStyle.THIN);
            Font failFont = workbook.createFont();
            failFont.setBold(true);
            failFont.setColor(IndexedColors.RED.getIndex());
            failStyle.setFont(failFont);

            String[] headers = {
                    "STT",
                    "Mã thành viên (*)",
                    "Email học viên (*)",
                    "Họ và tên",
                    "Tiến độ bài học",
                    "Điểm Quá trình (*)",
                    "Điểm Cuối kỳ (*)",
                    "Điểm Tổng kết",
                    "Kết quả",
                    "Chứng chỉ",
                    "Ghi chú"
            };

            Row headerRow = sheet.createRow(5);
            headerRow.setHeightInPoints(28);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            List<StudentGradeItemDTO> students = gradebook.getStudents();
            for (int r = 0; r < students.size(); r++) {
                StudentGradeItemDTO st = students.get(r);
                Row row = sheet.createRow(6 + r);
                row.setHeightInPoints(24);

                Cell cStt = row.createCell(0);
                cStt.setCellValue(String.valueOf(r + 1));
                cStt.setCellStyle(centerStyle);

                Cell cMemberId = row.createCell(1);
                cMemberId.setCellValue(st.getMemberId() != null ? String.valueOf(st.getMemberId()) : "");
                cMemberId.setCellStyle(centerStyle);

                Cell cEmail = row.createCell(2);
                cEmail.setCellValue(st.getEmail() != null ? st.getEmail() : "");
                cEmail.setCellStyle(dataStyle);

                Cell cName = row.createCell(3);
                cName.setCellValue(st.getFullName() != null ? st.getFullName() : "");
                cName.setCellStyle(dataStyle);

                Cell cProg = row.createCell(4);
                cProg.setCellValue(st.getCompletedLessonsCount() + "/" + st.getTotalLessonsCount() + " (" + st.getProgressPercent() + "%)");
                cProg.setCellStyle(centerStyle);

                Cell cProc = row.createCell(5);
                cProc.setCellValue(st.getProcessGrade() != null ? st.getProcessGrade().stripTrailingZeros().toPlainString() : "");
                cProc.setCellStyle(centerStyle);

                Cell cFinalExam = row.createCell(6);
                cFinalExam.setCellValue(st.getFinalExamGrade() != null ? st.getFinalExamGrade().stripTrailingZeros().toPlainString() : "");
                cFinalExam.setCellStyle(centerStyle);

                Cell cFinal = row.createCell(7);
                cFinal.setCellValue(st.getFinalGrade() != null ? st.getFinalGrade().stripTrailingZeros().toPlainString() : "");
                cFinal.setCellStyle(centerStyle);

                Cell cPassed = row.createCell(8);
                boolean isPassed = Boolean.TRUE.equals(st.getIsPassed());
                cPassed.setCellValue(isPassed ? "ĐẠT" : "CHƯA ĐẠT");
                cPassed.setCellStyle(isPassed ? passStyle : failStyle);

                Cell cCert = row.createCell(9);
                cCert.setCellValue(isPassed && st.getCertificateUrl() != null ? "Đủ điều kiện" : "Chưa cấp");
                cCert.setCellStyle(centerStyle);

                Cell cNote = row.createCell(10);
                cNote.setCellValue("");
                cNote.setCellStyle(dataStyle);
            }

            sheet.setColumnWidth(0, 8 * 256);
            sheet.setColumnWidth(1, 18 * 256);
            sheet.setColumnWidth(2, 30 * 256);
            sheet.setColumnWidth(3, 26 * 256);
            sheet.setColumnWidth(4, 20 * 256);
            sheet.setColumnWidth(5, 18 * 256);
            sheet.setColumnWidth(6, 18 * 256);
            sheet.setColumnWidth(7, 18 * 256);
            sheet.setColumnWidth(8, 16 * 256);
            sheet.setColumnWidth(9, 18 * 256);
            sheet.setColumnWidth(10, 24 * 256);

            Sheet guideSheet = workbook.createSheet("Huong_Dan_Nhap_Diem");
            Row gTitleRow = guideSheet.createRow(0);
            Cell gTitleCell = gTitleRow.createCell(0);
            gTitleCell.setCellValue("HƯỚNG DẪN NHẬP ĐIỂM HỌC VIÊN TỪ FILE EXCEL");
            gTitleCell.setCellStyle(titleStyle);

            String[][] instructions = {
                    {"1. Định danh học viên (*)", "Hệ thống sử dụng 'Mã thành viên (*)' (Cột B) hoặc 'Email học viên (*)' (Cột C) để tìm học viên trong đợt học. Vui lòng không thay đổi hai cột này."},
                    {"2. Cột Điểm Quá trình (*)", "Nhập số thập phân từ 0.00 đến 10.00 (VD: 8.5, 9.0). Dùng cho mô hình tính điểm trọng số."},
                    {"3. Cột Điểm Cuối kỳ (*)", "Nhập số thập phân từ 0.00 đến 10.00 (VD: 7.0, 8.5). Dùng cho mô hình trọng số hoặc bài thi đầu ra."},
                    {"4. Tự động tính điểm tổng kết", "Sau khi nạp file lên hệ thống, điểm tổng kết và kết quả ĐẠT / CHƯA ĐẠT sẽ được hệ thống tự động tính toán lại theo quy tắc của đợt học."},
                    {"5. Lưu ý cấu trúc file", "Không xóa hoặc thay đổi tên các tiêu đề cột tại dòng số 6 trong Sheet 'Bang_Diem'."}
            };

            for (int i = 0; i < instructions.length; i++) {
                Row r = guideSheet.createRow(i + 2);
                Cell col0 = r.createCell(0);
                col0.setCellValue(instructions[i][0]);
                col0.setCellStyle(dataStyle);
                Cell col1 = r.createCell(1);
                col1.setCellValue(instructions[i][1]);
                col1.setCellStyle(dataStyle);
            }

            guideSheet.setColumnWidth(0, 32 * 256);
            guideSheet.setColumnWidth(1, 90 * 256);

            workbook.write(out);
            log.info("[GradebookService] Đã xuất file Excel bảng điểm cho Cohort: {} thành công", cohortId);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("[GradebookService] Lỗi khi xuất Excel bảng điểm cho Cohort {}: {}", cohortId, e.getMessage(), e);
            throw new RuntimeException("Lỗi khi xuất Excel bảng điểm: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public GradebookImportResultDTO importGradebookFromExcel(Long cohortId, MultipartFile file, Long instructorId) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File tải lên không được để trống");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.toLowerCase().endsWith(".xlsx") && !originalFilename.toLowerCase().endsWith(".xls"))) {
            throw new RuntimeException("Định dạng file không hợp lệ. Vui lòng chọn file Excel (.xlsx hoặc .xls)");
        }

        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đợt học"));
        checkInstructorOrAdmin(cohort, instructorId);

        List<CohortMember> members = cohortMemberRepository.findByCohort_IdOrderByEnrolledAtAsc(cohortId);
        Map<Long, CohortMember> memberByIdMap = members.stream()
                .collect(Collectors.toMap(CohortMember::getId, Function.identity(), (a, b) -> a));
        Map<String, CohortMember> memberByEmailMap = members.stream()
                .filter(m -> m.getUser() != null && m.getUser().getEmail() != null)
                .collect(Collectors.toMap(m -> m.getUser().getEmail().trim().toLowerCase(), Function.identity(), (a, b) -> a));

        List<Long> cohortLessonIds = cohortScheduleRepository.findLessonIdsByCohortId(cohortId);
        int totalLessons = (cohortLessonIds != null) ? cohortLessonIds.size() : 0;

        List<String> errors = new ArrayList<>();
        int totalRows = 0;
        int updatedCount = 0;
        int failedCount = 0;

        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheet("Bang_Diem");
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }
            if (sheet == null) {
                throw new RuntimeException("Không tìm thấy Sheet bảng điểm trong file Excel");
            }

            int headerRowIndex = 5;
            int colMemberId = 1;
            int colEmail = 2;
            int colProcessGrade = 5;
            int colFinalExamGrade = 6;

            for (int r = 0; r <= Math.min(10, sheet.getLastRowNum()); r++) {
                Row row = sheet.getRow(r);
                if (row != null) {
                    boolean foundHeader = false;
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        String val = getCellValue(row.getCell(c)).toLowerCase();
                        if (val.contains("stt") || val.contains("mã") || val.contains("email")) {
                            foundHeader = true;
                        }
                        if (val.contains("mã") && (val.contains("thành viên") || val.contains("hv") || val.contains("id") || val.contains("member"))) {
                            colMemberId = c;
                        } else if (val.contains("email")) {
                            colEmail = c;
                        } else if (val.contains("quá trình") || val.contains("process")) {
                            colProcessGrade = c;
                        } else if (val.contains("cuối kỳ") || val.contains("final")) {
                            colFinalExamGrade = c;
                        }
                    }
                    if (foundHeader) {
                        headerRowIndex = r;
                        break;
                    }
                }
            }

            int lastRowNum = sheet.getLastRowNum();
            for (int rowIndex = headerRowIndex + 1; rowIndex <= lastRowNum; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                totalRows++;
                int displayRow = rowIndex + 1;

                String memberIdRaw = (colMemberId >= 0 && colMemberId < row.getLastCellNum()) ? getCellValue(row.getCell(colMemberId)) : "";
                String emailRaw = (colEmail >= 0 && colEmail < row.getLastCellNum()) ? getCellValue(row.getCell(colEmail)) : "";
                String processGradeRaw = (colProcessGrade >= 0 && colProcessGrade < row.getLastCellNum()) ? getCellValue(row.getCell(colProcessGrade)) : "";
                String finalExamGradeRaw = (colFinalExamGrade >= 0 && colFinalExamGrade < row.getLastCellNum()) ? getCellValue(row.getCell(colFinalExamGrade)) : "";

                CohortMember member = null;
                if (!memberIdRaw.isBlank()) {
                    try {
                        Long mid = Long.parseLong(memberIdRaw);
                        member = memberByIdMap.get(mid);
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (member == null && !emailRaw.isBlank()) {
                    member = memberByEmailMap.get(emailRaw.trim().toLowerCase());
                }

                if (member == null) {
                    errors.add("Dòng " + displayRow + ": Không tìm thấy học viên tương ứng (Mã: '" + memberIdRaw + "', Email: '" + emailRaw + "')");
                    failedCount++;
                    continue;
                }

                BigDecimal processScore = member.getProcessGrade();
                if (!processGradeRaw.isBlank()) {
                    try {
                        String clean = processGradeRaw.replace(",", ".");
                        BigDecimal val = new BigDecimal(clean).setScale(2, RoundingMode.HALF_UP);
                        if (val.compareTo(BigDecimal.ZERO) < 0 || val.compareTo(BigDecimal.TEN) > 0) {
                            errors.add("Dòng " + displayRow + ": Điểm quá trình (" + val + ") không hợp lệ. Phải nằm trong khoảng 0.0 - 10.0");
                            failedCount++;
                            continue;
                        }
                        processScore = val;
                    } catch (Exception e) {
                        errors.add("Dòng " + displayRow + ": Điểm quá trình '" + processGradeRaw + "' không đúng định dạng số");
                        failedCount++;
                        continue;
                    }
                }

                BigDecimal finalExamScore = member.getFinalExamGrade();
                if (!finalExamGradeRaw.isBlank()) {
                    try {
                        String clean = finalExamGradeRaw.replace(",", ".");
                        BigDecimal val = new BigDecimal(clean).setScale(2, RoundingMode.HALF_UP);
                        if (val.compareTo(BigDecimal.ZERO) < 0 || val.compareTo(BigDecimal.TEN) > 0) {
                            errors.add("Dòng " + displayRow + ": Điểm cuối kỳ (" + val + ") không hợp lệ. Phải nằm trong khoảng 0.0 - 10.0");
                            failedCount++;
                            continue;
                        }
                        finalExamScore = val;
                    } catch (Exception e) {
                        errors.add("Dòng " + displayRow + ": Điểm cuối kỳ '" + finalExamGradeRaw + "' không đúng định dạng số");
                        failedCount++;
                        continue;
                    }
                }

                int completedCount = (int) lessonCompletionRepository.countByCohort_IdAndUser_Id(cohortId, member.getUser().getId());
                applyGradingRule(cohort, member, processScore, finalExamScore, completedCount, totalLessons);
                cohortMemberRepository.save(member);
                updatedCount++;
            }

            log.info("[GradebookService] Nhập điểm từ Excel cho Cohort {}: {} cập nhật thành công, {} lỗi", cohortId, updatedCount, failedCount);

            return GradebookImportResultDTO.builder()
                    .totalRows(totalRows)
                    .updatedCount(updatedCount)
                    .failedCount(failedCount)
                    .errors(errors)
                    .build();
        } catch (Exception e) {
            log.error("[GradebookService] Lỗi khi đọc file Excel bảng điểm: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi đọc file Excel: " + e.getMessage());
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        switch (type) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                double num = cell.getNumericCellValue();
                if (num == Math.floor(num) && !Double.isInfinite(num)) {
                    return String.valueOf((long) num);
                }
                return String.valueOf(num);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK && !getCellValue(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private List<GradeColumnDTO> buildGradeColumns(Cohort cohort, List<Long> cohortLessonIds, Long finalLessonId) {
        List<GradeColumnDTO> columns = new ArrayList<>();
        if (cohortLessonIds == null || cohortLessonIds.isEmpty()) {
            cohortLessonIds = resolveCohortLessonIds(cohort);
        }
        if (cohortLessonIds == null || cohortLessonIds.isEmpty()) {
            if (finalLessonId != null) {
                GradeColumnDTO finalCol = createColumnForLesson(finalLessonId, true);
                if (finalCol != null) {
                    columns.add(finalCol);
                }
            }
            return columns;
        }

        boolean finalIncluded = false;
        for (Long lid : cohortLessonIds) {
            boolean isFinal = (finalLessonId != null && finalLessonId.equals(lid));
            if (isFinal) {
                finalIncluded = true;
            }
            GradeColumnDTO col = createColumnForLesson(lid, isFinal);
            if (col != null) {
                columns.add(col);
            }
        }

        if (finalLessonId != null && !finalIncluded) {
            GradeColumnDTO finalCol = createColumnForLesson(finalLessonId, true);
            if (finalCol != null) {
                columns.add(finalCol);
            }
        }
        return columns;
    }

    private GradeColumnDTO createColumnForLesson(Long lessonId, boolean isFinal) {
        if (lessonId == null) {
            return null;
        }

        Optional<Quiz> quizOpt = quizRepository.findByLessonId(lessonId);
        if (quizOpt.isPresent()) {
            Quiz q = quizOpt.get();
            String title = q.getTitle();
            if (title == null || title.isBlank()) {
                if (q.getLesson() != null && q.getLesson().getTitle() != null) {
                    title = q.getLesson().getTitle();
                } else {
                    title = lessonRepository.findById(lessonId).map(Lesson::getTitle).orElse("Bài trắc nghiệm");
                }
            }
            return GradeColumnDTO.builder()
                    .lessonId(lessonId)
                    .title(title)
                    .lessonType("QUIZ")
                    .isFinalExam(isFinal)
                    .maxScore(new BigDecimal("10.00"))
                    .passingScore(q.getPassingScore() != null ? q.getPassingScore() : new BigDecimal("5.00"))
                    .assessmentId(q.getId())
                    .build();
        }

        Optional<Assignment> assignOpt = assignmentRepository.findByLessonId(lessonId);
        if (assignOpt.isPresent()) {
            Assignment a = assignOpt.get();
            String title = a.getTitle();
            if (title == null || title.isBlank()) {
                if (a.getLesson() != null && a.getLesson().getTitle() != null) {
                    title = a.getLesson().getTitle();
                } else {
                    title = lessonRepository.findById(lessonId).map(Lesson::getTitle).orElse("Bài tập");
                }
            }
            return GradeColumnDTO.builder()
                    .lessonId(lessonId)
                    .title(title)
                    .lessonType("ASSIGNMENT")
                    .isFinalExam(isFinal)
                    .maxScore(a.getMaxScore() != null ? a.getMaxScore() : new BigDecimal("10.00"))
                    .passingScore(a.getPassingScore() != null ? a.getPassingScore() : new BigDecimal("5.00"))
                    .assessmentId(a.getId())
                    .build();
        }

        Optional<Lesson> lessonOpt = lessonRepository.findById(lessonId);
        if (lessonOpt.isPresent()) {
            Lesson l = lessonOpt.get();
            String type = l.getLessonType() != null ? l.getLessonType().toUpperCase() : "";
            if (isFinal || "QUIZ".equals(type) || "ASSIGNMENT".equals(type) || "CODE_PRACTICE".equals(type) || "EXAM".equals(type) || "PROJECT".equals(type)) {
                return GradeColumnDTO.builder()
                        .lessonId(lessonId)
                        .title(l.getTitle() != null && !l.getTitle().isBlank() ? l.getTitle() : ("Bài kiểm tra " + type))
                        .lessonType(!type.isBlank() ? type : (isFinal ? "EXAM" : "EVALUATION"))
                        .isFinalExam(isFinal)
                        .maxScore(new BigDecimal("10.00"))
                        .passingScore(new BigDecimal("5.00"))
                        .assessmentId(null)
                        .build();
            }
        }

        return null;
    }

    public List<Long> resolveCohortLessonIds(Cohort cohort) {
        if (cohort == null) {
            return Collections.emptyList();
        }
        List<Long> lessonIds = cohortScheduleRepository.findLessonIdsByCohortId(cohort.getId());
        if (lessonIds != null && !lessonIds.isEmpty()) {
            return lessonIds;
        }
        if (cohort.getCourse() != null) {
            List<Lesson> courseLessons = lessonRepository.findBySectionCourseIdOrderByOrderIndexAsc(cohort.getCourse().getId());
            if (courseLessons != null && !courseLessons.isEmpty()) {
                return courseLessons.stream().map(Lesson::getId).toList();
            }
        }
        return Collections.emptyList();
    }

    private Map<Long, StudentColumnGradeDTO> buildStudentColumnGrades(List<GradeColumnDTO> columns, Long userId, Long cohortId) {
        Map<Long, StudentColumnGradeDTO> map = new LinkedHashMap<>();
        if (columns == null || columns.isEmpty() || userId == null) {
            return map;
        }

        for (GradeColumnDTO col : columns) {
            Long lessonId = col.getLessonId();
            StudentColumnGradeDTO scg = StudentColumnGradeDTO.builder()
                    .lessonId(lessonId)
                    .lessonTitle(col.getTitle())
                    .lessonType(col.getLessonType())
                    .isFinalExam(col.isFinalExam())
                    .maxScore(col.getMaxScore())
                    .status("NOT_SUBMITTED")
                    .build();

            if ("QUIZ".equalsIgnoreCase(col.getLessonType())) {
                Long quizId = col.getAssessmentId();
                if (quizId == null) {
                    Optional<Quiz> q = quizRepository.findByLessonId(lessonId);
                    if (q.isPresent()) {
                        quizId = q.get().getId();
                    }
                }
                if (quizId != null) {
                    Optional<QuizSubmission> subOpt = (cohortId != null)
                            ? quizSubmissionRepository.findTopByQuizIdAndUserIdAndCohortIdOrderByScoreDesc(quizId, userId, cohortId)
                            : Optional.empty();
                    if (subOpt.isEmpty()) {
                        subOpt = quizSubmissionRepository.findTopByQuizIdAndUserIdOrderByScoreDesc(quizId, userId);
                    }
                    if (subOpt.isPresent()) {
                        QuizSubmission qs = subOpt.get();
                        scg.setSubmissionId(qs.getId());
                        scg.setScore(qs.getScore());
                        scg.setStatus(qs.getScore() != null ? "GRADED" : "SUBMITTED");
                        scg.setSubmittedAt(qs.getSubmittedAt());
                        scg.setGradedAt(qs.getSubmittedAt());
                    }
                }
            } else if ("ASSIGNMENT".equalsIgnoreCase(col.getLessonType())) {
                Long assignId = col.getAssessmentId();
                if (assignId == null) {
                    Optional<Assignment> a = assignmentRepository.findByLessonId(lessonId);
                    if (a.isPresent()) {
                        assignId = a.get().getId();
                    }
                }
                if (assignId != null) {
                    Optional<AssignmentSubmission> subOpt = (cohortId != null)
                            ? assignmentSubmissionRepository.findByAssignmentIdAndUserIdAndCohortId(assignId, userId, cohortId)
                            : Optional.empty();
                    if (subOpt.isEmpty()) {
                        subOpt = assignmentSubmissionRepository.findByAssignmentIdAndUserId(assignId, userId);
                    }
                    if (subOpt.isPresent()) {
                        AssignmentSubmission as = subOpt.get();
                        scg.setSubmissionId(as.getId());
                        scg.setScore(as.getScore());
                        scg.setStatus(as.getScore() != null ? "GRADED" : "SUBMITTED");
                        scg.setFeedback(as.getFeedback());
                        scg.setSubmittedAt(as.getSubmittedAt());
                        scg.setGradedAt(as.getGradedAt());
                    }
                }
            }
            map.put(lessonId, scg);
        }
        return map;
    }

    private void checkCourseInstructorOrAdmin(Course course, Long instructorId) {
        if (course == null || instructorId == null) {
            throw new RuntimeException("Truy cập không hợp lệ");
        }
        User user = userRepository.findById(instructorId).orElse(null);
        if (user == null) {
            throw new RuntimeException("Người dùng không tồn tại");
        }
        boolean isAdmin = SecurityUtils.isAdmin(user);
        boolean isTeacherOrInstructor = SecurityUtils.isInstructor(user);
        boolean isOwner = course.getCreatedBy() != null && course.getCreatedBy().getId().equals(instructorId);
        boolean isUnassigned = (course.getCreatedBy() == null);
        if (!isAdmin && !isOwner && !(isTeacherOrInstructor && isUnassigned)) {
            throw new RuntimeException("Bạn không có quyền quản lý bảng điểm của khóa học này");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CourseGradebookDTO getCourseGradebook(Long courseId, Long instructorId) {
        Course course = (courseRepository != null)
                ? courseRepository.findById(courseId).orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"))
                : null;
        checkCourseInstructorOrAdmin(course, instructorId);

        List<Cohort> allCohorts = cohortRepository.findByCourseIdOrdered(courseId);
        List<Cohort> realModules = allCohorts.stream()
                .filter(c -> c != null && !c.isFullCourseCohort())
                .toList();

        List<Enrollment> enrollments = (enrollmentRepository != null)
                ? enrollmentRepository.findByCourse_IdWithUser(courseId)
                : Collections.emptyList();
        Map<Long, Enrollment> enrollmentByUser = enrollments.stream()
                .filter(e -> e.getUser() != null)
                .collect(Collectors.toMap(e -> e.getUser().getId(), e -> e, (a, b) -> a));

        List<CohortMember> allModuleMembers = cohortMemberRepository.findByCourseIdWithUserAndCohort(courseId);
        Map<Long, List<CohortMember>> membersByUser = allModuleMembers.stream()
                .filter(cm -> cm.getUser() != null)
                .collect(Collectors.groupingBy(cm -> cm.getUser().getId()));

        Map<Long, User> studentMap = new LinkedHashMap<>();
        for (Enrollment e : enrollments) {
            if (e.getUser() != null) {
                studentMap.put(e.getUser().getId(), e.getUser());
            }
        }
        for (CohortMember cm : allModuleMembers) {
            if (cm.getUser() != null && !studentMap.containsKey(cm.getUser().getId())) {
                studentMap.put(cm.getUser().getId(), cm.getUser());
            }
        }

        List<CourseStudentGradeDTO> studentRows = new ArrayList<>();
        int completedCount = 0;
        BigDecimal totalAvgGradeSum = BigDecimal.ZERO;
        int gradedCompletedCount = 0;

        for (User student : studentMap.values()) {
            Long uid = student.getId();
            Enrollment en = enrollmentByUser.get(uid);
            List<CohortMember> userCms = membersByUser.getOrDefault(uid, Collections.emptyList());

            Map<Long, ModuleGradeDetailDTO> moduleGradesMap = new LinkedHashMap<>();
            int userPassedModCount = 0;
            BigDecimal userModGradesSum = BigDecimal.ZERO;
            int userGradedModCount = 0;
            Instant earliestEnrolledAt = (en != null && en.getEnrolledAt() != null) ? en.getEnrolledAt() : null;

            for (Cohort mod : realModules) {
                CohortMember cm = userCms.stream()
                        .filter(m -> m.getCohort() != null && m.getCohort().getId().equals(mod.getId()))
                        .findFirst().orElse(null);

                ModuleGradeDetailDTO.ModuleGradeDetailDTOBuilder modGradeBuilder = ModuleGradeDetailDTO.builder()
                        .moduleId(mod.getId())
                        .moduleCode(mod.getCode())
                        .moduleName(mod.getName())
                        .gradingPolicy(mod.getGradingPolicy())
                        .isGradesPublished(Boolean.TRUE.equals(mod.getIsGradesPublished()));

                if (cm != null) {
                    modGradeBuilder.isEnrolled(true);
                    modGradeBuilder.finalGrade(cm.getFinalGrade());
                    modGradeBuilder.isPassed(cm.getIsPassed());
                    modGradeBuilder.certificateUrl(cm.getCertificateUrl());
                    modGradeBuilder.hasReviewed(cm.hasReviewed());

                    if (Boolean.TRUE.equals(cm.getIsPassed())) {
                        userPassedModCount++;
                    }
                    if (cm.getFinalGrade() != null) {
                        userModGradesSum = userModGradesSum.add(cm.getFinalGrade());
                        userGradedModCount++;
                    }
                    if (earliestEnrolledAt == null || (cm.getEnrolledAt() != null && cm.getEnrolledAt().isBefore(earliestEnrolledAt))) {
                        earliestEnrolledAt = cm.getEnrolledAt();
                    }
                } else {
                    modGradeBuilder.isEnrolled(false);
                    modGradeBuilder.isPassed(false);
                }
                moduleGradesMap.put(mod.getId(), modGradeBuilder.build());
            }

            boolean isCompleted = (en != null && Boolean.TRUE.equals(en.getIsCompleted()))
                    || (!realModules.isEmpty() && userPassedModCount >= realModules.size());

            BigDecimal avgGrade = (en != null && en.getAverageGrade() != null)
                    ? en.getAverageGrade()
                    : ((userGradedModCount > 0) ? userModGradesSum.divide(BigDecimal.valueOf(userGradedModCount), 2, RoundingMode.HALF_UP) : null);

            if (isCompleted) {
                completedCount++;
                if (avgGrade != null) {
                    totalAvgGradeSum = totalAvgGradeSum.add(avgGrade);
                    gradedCompletedCount++;
                }
            }

            int progressPercent = (!realModules.isEmpty()) ? (userPassedModCount * 100 / realModules.size()) : 0;

            studentRows.add(CourseStudentGradeDTO.builder()
                    .userId(uid)
                    .fullName(student.getFullName())
                    .email(student.getEmail())
                    .avatarUrl(student.getAvatarUrl())
                    .enrolledAt(earliestEnrolledAt)
                    .completedModulesCount(userPassedModCount)
                    .totalModulesCount(realModules.size())
                    .progressPercent(progressPercent)
                    .courseAverageGrade(avgGrade)
                    .isCompleted(isCompleted)
                    .completedAt(en != null ? en.getCompletedAt() : null)
                    .certificateUrl(en != null ? en.getCertificateUrl() : null)
                    .hasReviewed(en != null && en.hasReviewed())
                    .rating(en != null ? en.getRating() : null)
                    .reviewComment(en != null ? en.getReviewComment() : null)
                    .moduleGrades(moduleGradesMap)
                    .build());
        }

        int totalStudents = studentRows.size();
        int inProgressCount = totalStudents - completedCount;
        double passRate = (totalStudents > 0) ? Math.round((double) completedCount * 1000.0 / totalStudents) / 10.0 : 0.0;
        BigDecimal overallAvg = (gradedCompletedCount > 0)
                ? totalAvgGradeSum.divide(BigDecimal.valueOf(gradedCompletedCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        log.info("[GradebookService] Tải bảng điểm khóa học {}: {} học viên, {} tốt nghiệp, tỉ lệ {}%",
                courseId, totalStudents, completedCount, passRate);

        return CourseGradebookDTO.builder()
                .course(course)
                .totalStudents(totalStudents)
                .completedCount(completedCount)
                .inProgressCount(inProgressCount)
                .passRate(passRate)
                .averageGrade(overallAvg)
                .modules(realModules)
                .students(studentRows)
                .build();
    }

    @Override
    @Transactional
    public void calculateAndSyncCourseGradebook(Long courseId, Long instructorId) {
        Course course = (courseRepository != null)
                ? courseRepository.findById(courseId).orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"))
                : null;
        checkCourseInstructorOrAdmin(course, instructorId);

        List<Enrollment> enrollments = (enrollmentRepository != null)
                ? enrollmentRepository.findByCourse_IdWithUser(courseId)
                : Collections.emptyList();
        List<CohortMember> allModuleMembers = cohortMemberRepository.findByCourseIdWithUserAndCohort(courseId);

        Map<Long, User> studentMap = new LinkedHashMap<>();
        for (Enrollment e : enrollments) {
            if (e.getUser() != null) {
                studentMap.put(e.getUser().getId(), e.getUser());
            }
        }
        for (CohortMember cm : allModuleMembers) {
            if (cm.getUser() != null && !studentMap.containsKey(cm.getUser().getId())) {
                studentMap.put(cm.getUser().getId(), cm.getUser());
            }
        }

        if (cohortService != null) {
            for (User student : studentMap.values()) {
                cohortService.checkAndSyncCourseCompletion(course, student);
            }
        }
        log.info("[GradebookService] Đã đồng bộ điểm và trạng thái hoàn thành khóa học {} cho {} học viên",
                courseId, studentMap.size());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportCourseGradebookToExcel(Long courseId, Long instructorId) {
        Course course = (courseRepository != null)
                ? courseRepository.findById(courseId).orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"))
                : null;
        checkCourseInstructorOrAdmin(course, instructorId);
        CourseGradebookDTO gradebook = getCourseGradebook(courseId, instructorId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Bang_Diem_Khoa_Hoc");

            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(IndexedColors.ROYAL_BLUE.getIndex());

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            Font subFont = workbook.createFont();
            subFont.setItalic(true);
            subFont.setFontHeightInPoints((short) 10);
            subFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

            CellStyle subStyle = workbook.createCellStyle();
            subStyle.setFont(subFont);

            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("BẢNG ĐIỂM & KẾT QUẢ TỐT NGHIỆP KHÓA HỌC - UTELEARN LMS");
            c0.setCellStyle(titleStyle);

            Row r1 = sheet.createRow(1);
            Cell c1 = r1.createCell(0);
            c1.setCellValue("Khóa học: " + (course != null ? course.getTitle() : "") + " | Mã khóa: " + (course != null ? course.getCode() : ""));
            c1.setCellStyle(subStyle);

            String exportDate = DATE_FORMATTER.format(Instant.now());
            Row r2 = sheet.createRow(2);
            Cell c2 = r2.createCell(0);
            c2.setCellValue("Thời gian xuất file: " + exportDate + " | Tổng sĩ số: " + gradebook.getTotalStudents() + " | Đã tốt nghiệp: " + gradebook.getCompletedCount() + " (" + gradebook.getPassRate() + "%) | ĐTB chung: " + gradebook.getAverageGrade() + "/10");
            c2.setCellStyle(subStyle);

            Row headerRow = sheet.createRow(4);
            CellStyle headerStyle = workbook.createCellStyle();
            Font hFont = workbook.createFont();
            hFont.setBold(true);
            hFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(hFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            int colIdx = 0;
            String[] baseHeaders = {"STT", "Mã HV", "Họ và tên", "Email", "Ngày ghi danh"};
            for (String h : baseHeaders) {
                Cell cell = headerRow.createCell(colIdx++);
                cell.setCellValue(h);
                cell.setCellStyle(headerStyle);
            }

            List<Cohort> modules = gradebook.getModules();
            if (modules != null) {
                for (Cohort m : modules) {
                    Cell cell = headerRow.createCell(colIdx++);
                    cell.setCellValue(m.getName() != null ? m.getName() : m.getCode());
                    cell.setCellStyle(headerStyle);
                }
            }

            String[] summaryHeaders = {"ĐTB Khóa học", "Tiến độ Module", "Kết quả", "Mã Chứng chỉ"};
            for (String h : summaryHeaders) {
                Cell cell = headerRow.createCell(colIdx++);
                cell.setCellValue(h);
                cell.setCellStyle(headerStyle);
            }

            CellStyle normalStyle = workbook.createCellStyle();
            normalStyle.setBorderBottom(BorderStyle.THIN);
            normalStyle.setBorderTop(BorderStyle.THIN);
            normalStyle.setBorderLeft(BorderStyle.THIN);
            normalStyle.setBorderRight(BorderStyle.THIN);
            normalStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.cloneStyleFrom(normalStyle);
            centerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle passedStyle = workbook.createCellStyle();
            passedStyle.cloneStyleFrom(centerStyle);
            Font passFont = workbook.createFont();
            passFont.setBold(true);
            passFont.setColor(IndexedColors.DARK_GREEN.getIndex());
            passedStyle.setFont(passFont);

            CellStyle studyingStyle = workbook.createCellStyle();
            studyingStyle.cloneStyleFrom(centerStyle);
            Font studyFont = workbook.createFont();
            studyFont.setBold(true);
            studyFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            studyingStyle.setFont(studyFont);

            List<CourseStudentGradeDTO> students = gradebook.getStudents();
            int rowIdx = 5;
            if (students != null) {
                int stt = 1;
                for (CourseStudentGradeDTO st : students) {
                    Row row = sheet.createRow(rowIdx++);
                    int cIdx = 0;

                    Cell cellStt = row.createCell(cIdx++);
                    cellStt.setCellValue(stt++);
                    cellStt.setCellStyle(centerStyle);

                    Cell cellId = row.createCell(cIdx++);
                    cellId.setCellValue(st.getUserId() != null ? "HV-" + st.getUserId() : "-");
                    cellId.setCellStyle(centerStyle);

                    Cell cellName = row.createCell(cIdx++);
                    cellName.setCellValue(st.getFullName() != null ? st.getFullName() : "-");
                    cellName.setCellStyle(normalStyle);

                    Cell cellEmail = row.createCell(cIdx++);
                    cellEmail.setCellValue(st.getEmail() != null ? st.getEmail() : "-");
                    cellEmail.setCellStyle(normalStyle);

                    Cell cellEnrolledAt = row.createCell(cIdx++);
                    cellEnrolledAt.setCellValue(st.getEnrolledAt() != null ? DATE_FORMATTER.format(st.getEnrolledAt()) : "-");
                    cellEnrolledAt.setCellStyle(centerStyle);

                    if (modules != null) {
                        for (Cohort m : modules) {
                            Cell cellMod = row.createCell(cIdx++);
                            cellMod.setCellStyle(centerStyle);
                            ModuleGradeDetailDTO mg = (st.getModuleGrades() != null) ? st.getModuleGrades().get(m.getId()) : null;
                            if (mg != null && mg.isEnrolled()) {
                                if (mg.getFinalGrade() != null) {
                                    cellMod.setCellValue(mg.getFinalGrade().toPlainString());
                                } else if (Boolean.TRUE.equals(mg.getIsPassed())) {
                                    cellMod.setCellValue("Đạt");
                                } else {
                                    cellMod.setCellValue("Đang học");
                                }
                            } else {
                                cellMod.setCellValue("-");
                            }
                        }
                    }

                    Cell cellAvg = row.createCell(cIdx++);
                    cellAvg.setCellValue(st.getCourseAverageGrade() != null ? st.getCourseAverageGrade().toPlainString() : "-");
                    cellAvg.setCellStyle(centerStyle);

                    Cell cellProg = row.createCell(cIdx++);
                    cellProg.setCellValue(st.getCompletedModulesCount() + "/" + st.getTotalModulesCount() + " (" + st.getProgressPercent() + "%)");
                    cellProg.setCellStyle(centerStyle);

                    Cell cellStatus = row.createCell(cIdx++);
                    if (Boolean.TRUE.equals(st.getIsCompleted())) {
                        cellStatus.setCellValue("ĐÃ TỐT NGHIỆP");
                        cellStatus.setCellStyle(passedStyle);
                    } else {
                        cellStatus.setCellValue("ĐANG HỌC");
                        cellStatus.setCellStyle(studyingStyle);
                    }

                    Cell cellCert = row.createCell(cIdx++);
                    cellCert.setCellValue(st.getCertificateUrl() != null ? st.getCertificateUrl() : "-");
                    cellCert.setCellStyle(normalStyle);
                }
            }

            for (int i = 0; i < colIdx; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) < 3000) {
                    sheet.setColumnWidth(i, 3000);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("[GradebookService] Lỗi khi xuất Excel bảng điểm khóa học {}: {}", courseId, e.getMessage(), e);
            throw new RuntimeException("Lỗi khi xuất Excel bảng điểm khóa học: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public StudentCourseGradeDTO getStudentCourseGrade(Long courseId, Long userId) {
        Course course = (courseRepository != null)
                ? courseRepository.findById(courseId).orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"))
                : null;

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Không tìm thấy học viên"));

        List<Cohort> allCohorts = cohortRepository.findByCourseIdOrdered(courseId);
        List<Cohort> realModules = allCohorts.stream()
                .filter(c -> c != null && !c.isFullCourseCohort())
                .toList();

        List<CohortMember> memberships = cohortMemberRepository.findByCohort_Course_IdAndUser_Id(courseId, userId);
        Enrollment enrollment = (enrollmentRepository != null)
                ? enrollmentRepository.findByUser_IdAndCourse_Id(userId, courseId).orElse(null)
                : null;

        List<ModuleGradeDetailDTO> moduleGradesList = new ArrayList<>();
        int completedModulesCount = 0;
        BigDecimal sumGrades = BigDecimal.ZERO;
        int gradedCount = 0;

        for (Cohort mod : realModules) {
            CohortMember cm = memberships.stream()
                    .filter(m -> m.getCohort() != null && m.getCohort().getId().equals(mod.getId()))
                    .findFirst().orElse(null);

            ModuleGradeDetailDTO.ModuleGradeDetailDTOBuilder builder = ModuleGradeDetailDTO.builder()
                    .moduleId(mod.getId())
                    .moduleCode(mod.getCode())
                    .moduleName(mod.getName())
                    .gradingPolicy(mod.getGradingPolicy())
                    .isGradesPublished(Boolean.TRUE.equals(mod.getIsGradesPublished()));

            if (cm != null) {
                builder.isEnrolled(true);
                builder.finalGrade(cm.getFinalGrade());
                builder.isPassed(cm.getIsPassed());
                builder.certificateUrl(cm.getCertificateUrl());
                builder.hasReviewed(cm.hasReviewed());

                if (Boolean.TRUE.equals(cm.getIsPassed())) {
                    completedModulesCount++;
                }
                if (cm.getFinalGrade() != null) {
                    sumGrades = sumGrades.add(cm.getFinalGrade());
                    gradedCount++;
                }
            } else {
                builder.isEnrolled(false);
                builder.isPassed(false);
            }
            moduleGradesList.add(builder.build());
        }

        boolean isCompleted = (enrollment != null && Boolean.TRUE.equals(enrollment.getIsCompleted()))
                || (!realModules.isEmpty() && completedModulesCount >= realModules.size());

        BigDecimal avgGrade = (enrollment != null && enrollment.getAverageGrade() != null)
                ? enrollment.getAverageGrade()
                : ((gradedCount > 0) ? sumGrades.divide(BigDecimal.valueOf(gradedCount), 2, RoundingMode.HALF_UP) : null);

        int progressPercent = (!realModules.isEmpty()) ? (completedModulesCount * 100 / realModules.size()) : 0;

        return StudentCourseGradeDTO.builder()
                .course(course)
                .userId(userId)
                .fullName(user.getFullName())
                .email(user.getEmail())
                .completedModulesCount(completedModulesCount)
                .totalModulesCount(realModules.size())
                .progressPercent(progressPercent)
                .courseAverageGrade(avgGrade)
                .isCompleted(isCompleted)
                .completedAt(enrollment != null ? enrollment.getCompletedAt() : null)
                .certificateUrl(enrollment != null ? enrollment.getCertificateUrl() : null)
                .hasReviewed(enrollment != null && enrollment.hasReviewed())
                .rating(enrollment != null ? enrollment.getRating() : null)
                .reviewComment(enrollment != null ? enrollment.getReviewComment() : null)
                .moduleGrades(moduleGradesList)
                .build();
    }
}

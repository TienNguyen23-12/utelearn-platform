package vn.edu.ute.utelearn.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.ute.utelearn.dao.*;
import vn.edu.ute.utelearn.dto.AssignmentDTO;
import vn.edu.ute.utelearn.dto.AssignmentGradeDTO;
import vn.edu.ute.utelearn.dto.AssignmentRequestDTO;
import vn.edu.ute.utelearn.dto.AssignmentSubmissionDTO;
import vn.edu.ute.utelearn.entity.*;
import vn.edu.ute.utelearn.service.AssignmentService;
import vn.edu.ute.utelearn.service.CloudinaryService;
import vn.edu.ute.utelearn.util.SecurityUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final LessonRepository lessonRepository;
    private final CohortRepository cohortRepository;
    private final UserRepository userRepository;
    private final CohortScheduleRepository cohortScheduleRepository;
    private final LessonCompletionRepository lessonCompletionRepository;
    private final CohortMemberRepository cohortMemberRepository;
    private final CloudinaryService cloudinaryService;

    private boolean isInstructorAuthorized(Lesson lesson, Long instructorId) {
        if (instructorId == null || lesson == null) {
            return false;
        }
        User user = userRepository.findById(instructorId).orElse(null);
        if (user != null && SecurityUtils.hasRole(user, "ADMIN", "ROLE_ADMIN", "MODERATOR", "ROLE_MODERATOR")) {
            return true;
        }
        Section section = lesson.getSection();
        if (section == null) {
            return false;
        }
        Course course = section.getCourse();
        Cohort cohort = section.getCohort();
        if (cohort == null) {
            List<CohortSchedule> schedules = cohortScheduleRepository.findByLessonId(lesson.getId());
            if (!schedules.isEmpty() && schedules.get(0).getCohort() != null) {
                cohort = schedules.get(0).getCohort();
            }
        }
        if (course != null && course.getCreatedBy() != null && course.getCreatedBy().getId().equals(instructorId)) {
            return true;
        }
        if (cohort != null && cohort.getCreatedBy() != null && cohort.getCreatedBy().getId().equals(instructorId)) {
            return true;
        }
        if (user != null && SecurityUtils.isInstructor(user)) {
            if ((course != null && course.getCreatedBy() == null) || (cohort != null && cohort.getCreatedBy() == null)) {
                return true;
            }
            if (cohort != null && cohort.getCourse() != null && cohort.getCourse().getCreatedBy() != null
                    && cohort.getCourse().getCreatedBy().getId().equals(instructorId)) {
                return true;
            }
            return true;
        }
        return false;
    }

    private AssignmentSubmissionDTO mapToSubmissionDTO(AssignmentSubmission sub, BigDecimal maxScore, BigDecimal passingScore) {
        if (sub == null) {
            return null;
        }
        boolean requiresGrading = maxScore != null && maxScore.compareTo(BigDecimal.ZERO) > 0;
        boolean passed = !requiresGrading || (sub.getScore() != null && passingScore != null && sub.getScore().compareTo(passingScore) >= 0);
        String gradedByName = sub.getGradedBy() != null ? sub.getGradedBy().getFullName() : null;
        String studentName = sub.getUser() != null ? sub.getUser().getFullName() : null;
        String studentEmail = sub.getUser() != null ? sub.getUser().getEmail() : null;
        String studentAvatar = sub.getUser() != null ? sub.getUser().getAvatarUrl() : null;

        return AssignmentSubmissionDTO.builder()
                .id(sub.getId())
                .assignmentId(sub.getAssignment().getId())
                .userId(sub.getUser().getId())
                .studentName(studentName)
                .studentEmail(studentEmail)
                .studentAvatar(studentAvatar)
                .cohortId(sub.getCohort() != null ? sub.getCohort().getId() : null)
                .submissionText(sub.getSubmissionText())
                .fileUrl(sub.getFileUrl())
                .fileName(sub.getFileName())
                .fileSizeBytes(sub.getFileSizeBytes())
                .status(sub.getStatus())
                .score(sub.getScore())
                .maxScore(maxScore)
                .feedback(sub.getFeedback())
                .gradedByName(gradedByName)
                .gradedAt(sub.getGradedAt())
                .submittedAt(sub.getSubmittedAt())
                .isPassed(passed)
                .isLate(Boolean.TRUE.equals(sub.getIsLate()))
                .requiresGrading(requiresGrading)
                .build();
    }

    private AssignmentDTO mapToDTO(Assignment assignment, Long cohortId, Long userId) {
        Lesson lesson = assignment.getLesson();
        Long totalSubmissions = 0L;
        Long gradedSubmissions = 0L;

        if (cohortId != null) {
            totalSubmissions = assignmentSubmissionRepository.countByAssignmentIdAndCohortId(assignment.getId(), cohortId);
            gradedSubmissions = assignmentSubmissionRepository.countByAssignmentIdAndCohortIdAndStatus(assignment.getId(), cohortId, "GRADED");
        } else {
            List<AssignmentSubmission> all = assignmentSubmissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(assignment.getId());
            totalSubmissions = (long) all.size();
            gradedSubmissions = all.stream().filter(s -> "GRADED".equalsIgnoreCase(s.getStatus())).count();
        }

        String unlockAt = null;
        String deadlineAt = null;
        boolean isLocked = false;
        boolean isPastDeadline = false;

        if (cohortId != null) {
            Optional<CohortSchedule> scheduleOpt = cohortScheduleRepository.findByCohortIdAndLessonId(cohortId, lesson.getId());
            if (scheduleOpt.isPresent()) {
                CohortSchedule schedule = scheduleOpt.get();
                unlockAt = schedule.getFormattedUnlockAt();
                deadlineAt = schedule.getFormattedDeadlineAt();
                if (schedule.getUnlockAt() != null && Instant.now().isBefore(schedule.getUnlockAt())) {
                    isLocked = true;
                }
                if (schedule.getDeadlineAt() != null && Instant.now().isAfter(schedule.getDeadlineAt())) {
                    isPastDeadline = true;
                }
            }
        }

        boolean requiresGrading = assignment.getMaxScore() != null && assignment.getMaxScore().compareTo(BigDecimal.ZERO) > 0;
        AssignmentSubmissionDTO currentSubDTO = null;
        boolean isPassed = false;
        if (userId != null) {
            Optional<AssignmentSubmission> subOpt = (cohortId != null)
                    ? assignmentSubmissionRepository.findByAssignmentIdAndUserIdAndCohortId(assignment.getId(), userId, cohortId)
                    : assignmentSubmissionRepository.findByAssignmentIdAndUserId(assignment.getId(), userId);
            if (subOpt.isPresent()) {
                AssignmentSubmission sub = subOpt.get();
                currentSubDTO = mapToSubmissionDTO(sub, assignment.getMaxScore(), assignment.getPassingScore());
                if (!requiresGrading) {
                    isPassed = true;
                } else if (sub.getScore() != null && assignment.getPassingScore() != null
                        && sub.getScore().compareTo(assignment.getPassingScore()) >= 0) {
                    isPassed = true;
                }
            }
        }

        boolean isReviewer = false;
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && (SecurityUtils.isModerator(user) || SecurityUtils.isAdmin(user) || isInstructorAuthorized(lesson, userId))) {
                isReviewer = true;
            }
        }

        return AssignmentDTO.builder()
                .id(assignment.getId())
                .lessonId(lesson.getId())
                .lessonStatus(lesson.getStatus())
                .title(assignment.getTitle())
                .instruction(assignment.getInstruction())
                .attachmentUrl(assignment.getAttachmentUrl())
                .maxScore(assignment.getMaxScore())
                .passingScore(assignment.getPassingScore())
                .allowLateSubmission(assignment.getAllowLateSubmission() != null ? assignment.getAllowLateSubmission() : true)
                .allowFileSubmission(assignment.getAllowFileSubmission())
                .allowTextSubmission(assignment.getAllowTextSubmission())
                .allowedFileTypes(assignment.getAllowedFileTypes())
                .maxFileSizeMb(assignment.getMaxFileSizeMb())
                .totalSubmissions(totalSubmissions)
                .gradedSubmissions(gradedSubmissions)
                .currentSubmission(currentSubDTO)
                .unlockAt(unlockAt)
                .deadlineAt(deadlineAt)
                .isLocked(isLocked)
                .isPastDeadline(isPastDeadline)
                .requiresGrading(requiresGrading)
                .isPassed(isPassed)
                .isReviewer(isReviewer)
                .build();
    }

    @Override
    @Transactional
    public AssignmentDTO getOrCreateAssignmentForLesson(Long lessonId, Long instructorId) {
        log.info("[AssignmentService] Yêu cầu lấy hoặc tạo Assignment cho lessonId: {}", lessonId);
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài học với ID: " + lessonId));

        if (!isInstructorAuthorized(lesson, instructorId)) {
            throw new RuntimeException("Bạn không có quyền quản lý bài tập cho bài học này");
        }

        if (!"ASSIGNMENT".equalsIgnoreCase(lesson.getLessonType())) {
            lesson.setLessonType("ASSIGNMENT");
            lessonRepository.save(lesson);
        }

        Assignment assignment = assignmentRepository.findByLessonId(lessonId).orElseGet(() -> {
            Assignment newAssignment = Assignment.builder()
                    .lesson(lesson)
                    .title(lesson.getTitle() != null ? lesson.getTitle() : "Bài tập mới")
                    .instruction("Vui lòng thực hiện yêu cầu bài tập và nộp bài trước hạn.")
                    .maxScore(new BigDecimal("10.0"))
                    .passingScore(new BigDecimal("5.0"))
                    .allowLateSubmission(true)
                    .allowFileSubmission(true)
                    .allowTextSubmission(true)
                    .allowedFileTypes("pdf,doc,docx,zip,rar,txt")
                    .maxFileSizeMb(20)
                    .build();
            return assignmentRepository.save(newAssignment);
        });

        return mapToDTO(assignment, null, instructorId);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentDTO getAssignmentByLessonId(Long lessonId, Long cohortId, Long userId) {
        log.info("[AssignmentService] Tải thông tin Assignment cho lessonId: {}, cohortId: {}, userId: {}", lessonId, cohortId, userId);
        Assignment assignment = assignmentRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new RuntimeException("Bài tập chưa được thiết lập cho bài học này"));

        return mapToDTO(assignment, cohortId, userId);
    }

    @Override
    @Transactional
    public AssignmentDTO updateAssignment(Long assignmentId, AssignmentRequestDTO request, Long instructorId) {
        log.info("[AssignmentService] Cập nhật Assignment ID: {} bởi instructorId: {}", assignmentId, instructorId);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập với ID: " + assignmentId));

        if (!isInstructorAuthorized(assignment.getLesson(), instructorId)) {
            throw new RuntimeException("Bạn không có quyền cập nhật bài tập này");
        }

        if (!Boolean.TRUE.equals(request.getAllowFileSubmission()) && !Boolean.TRUE.equals(request.getAllowTextSubmission())) {
            throw new RuntimeException("Vui lòng cho phép ít nhất một kiểu nộp bài (tệp tin hoặc văn bản)");
        }

        boolean requiresGrading = request.getMaxScore() != null && request.getMaxScore().compareTo(BigDecimal.ZERO) > 0;

        assignment.setTitle(request.getTitle());
        assignment.setInstruction(request.getInstruction());
        assignment.setAttachmentUrl(request.getAttachmentUrl());
        assignment.setMaxScore(requiresGrading ? request.getMaxScore() : BigDecimal.ZERO);
        assignment.setPassingScore(requiresGrading ? (request.getPassingScore() != null ? request.getPassingScore() : BigDecimal.ZERO) : BigDecimal.ZERO);
        assignment.setAllowLateSubmission(request.getAllowLateSubmission() != null ? request.getAllowLateSubmission() : true);
        assignment.setAllowFileSubmission(request.getAllowFileSubmission());
        assignment.setAllowTextSubmission(request.getAllowTextSubmission());
        assignment.setAllowedFileTypes(request.getAllowedFileTypes());
        assignment.setMaxFileSizeMb(request.getMaxFileSizeMb());

        Lesson lesson = assignment.getLesson();
        if (!Objects.equals(lesson.getTitle(), request.getTitle())) {
            lesson.setTitle(request.getTitle());
            lessonRepository.save(lesson);
        }

        Assignment saved = assignmentRepository.save(assignment);

        if (!requiresGrading) {
            List<AssignmentSubmission> existingList = assignmentSubmissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(saved.getId());
            for (AssignmentSubmission sub : existingList) {
                if (!"COMPLETED".equalsIgnoreCase(sub.getStatus())) {
                    sub.setStatus("COMPLETED");
                    sub.setScore(BigDecimal.ZERO);
                    assignmentSubmissionRepository.save(sub);
                    if (sub.getCohort() != null) {
                        recordLessonCompletion(sub.getCohort(), saved.getLesson(), sub.getUser());
                    }
                }
            }
        }

        return mapToDTO(saved, null, instructorId);
    }

    @Override
    @Transactional
    public AssignmentSubmissionDTO submitAssignment(Long assignmentId, Long cohortId, String submissionText, MultipartFile file, Long userId) {
        log.info("[AssignmentService] Học viên ID {} nộp bài tập ID {} trong cohort {}", userId, assignmentId, cohortId);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        Cohort cohort = null;
        boolean isLate = false;
        if (cohortId != null) {
            cohort = cohortRepository.findById(cohortId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đợt học/module"));
            Optional<CohortSchedule> scheduleOpt = cohortScheduleRepository.findByCohortIdAndLessonId(cohortId, assignment.getLesson().getId());
            if (scheduleOpt.isPresent()) {
                CohortSchedule schedule = scheduleOpt.get();
                if (schedule.getUnlockAt() != null && Instant.now().isBefore(schedule.getUnlockAt())) {
                    throw new RuntimeException("Bài tập chưa được mở");
                }
                if (schedule.getDeadlineAt() != null && Instant.now().isAfter(schedule.getDeadlineAt())) {
                    if (Boolean.FALSE.equals(assignment.getAllowLateSubmission())) {
                        throw new RuntimeException("Bài tập đã quá hạn nộp và không cho phép nộp trễ");
                    }
                    isLate = true;
                }
            }
        }

        boolean allowFile = Boolean.TRUE.equals(assignment.getAllowFileSubmission());
        boolean allowText = Boolean.TRUE.equals(assignment.getAllowTextSubmission());

        boolean hasFile = file != null && !file.isEmpty();
        boolean hasText = submissionText != null && !submissionText.trim().isEmpty();

        if (allowFile && !allowText) {
            if (!hasFile) {
                throw new RuntimeException("Bài tập này yêu cầu bắt buộc phải nộp tệp tin đính kèm");
            }
        } else if (!allowFile && allowText) {
            if (!hasText) {
                throw new RuntimeException("Bài tập này yêu cầu bắt buộc phải nhập văn bản hoặc liên kết");
            }
        } else {
            if (!hasFile && !hasText) {
                throw new RuntimeException("Vui lòng tải lên tệp tin hoặc nhập nội dung bài làm");
            }
        }

        String fileUrl = null;
        String fileName = null;
        Long fileSizeBytes = null;

        if (hasFile) {
            if (!Boolean.TRUE.equals(assignment.getAllowFileSubmission())) {
                throw new RuntimeException("Bài tập này không yêu cầu nộp tệp tin đính kèm");
            }

            long maxBytes = (long) assignment.getMaxFileSizeMb() * 1024 * 1024;
            if (file.getSize() > maxBytes) {
                throw new RuntimeException("Kích thước tệp tin vượt quá giới hạn " + assignment.getMaxFileSizeMb() + "MB");
            }

            String originalName = file.getOriginalFilename();
            if (originalName != null && assignment.getAllowedFileTypes() != null && !assignment.getAllowedFileTypes().isBlank()) {
                String ext = "";
                int dotIdx = originalName.lastIndexOf(".");
                if (dotIdx != -1) {
                    ext = originalName.substring(dotIdx + 1).toLowerCase();
                }
                List<String> allowed = Arrays.stream(assignment.getAllowedFileTypes().toLowerCase().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
                if (!allowed.isEmpty() && !allowed.contains(ext)) {
                    throw new RuntimeException("Định dạng tệp ." + ext + " không được hỗ trợ. Định dạng hợp lệ: " + assignment.getAllowedFileTypes());
                }
            }

            try {
                Map<String, Object> uploadResult = cloudinaryService.uploadFile(file, "/assignments", "raw");
                fileUrl = (String) uploadResult.get("secure_url");
                if (fileUrl == null) {
                    fileUrl = (String) uploadResult.get("url");
                }
                fileName = originalName;
                fileSizeBytes = file.getSize();
            } catch (Exception e) {
                log.error("[AssignmentService] Lỗi tải tệp lên Cloudinary: {}", e.getMessage(), e);
                throw new RuntimeException("Không thể tải tệp lên hệ thống: " + e.getMessage());
            }
        }

        Optional<AssignmentSubmission> existingOpt = (cohortId != null)
                ? assignmentSubmissionRepository.findByAssignmentIdAndUserIdAndCohortId(assignmentId, userId, cohortId)
                : assignmentSubmissionRepository.findByAssignmentIdAndUserId(assignmentId, userId);

        boolean requiresGrading = assignment.getMaxScore() != null && assignment.getMaxScore().compareTo(BigDecimal.ZERO) > 0;
        String status = !requiresGrading ? "COMPLETED" : (existingOpt.isPresent() ? "RESUBMITTED" : "SUBMITTED");
        BigDecimal initialScore = !requiresGrading ? BigDecimal.ZERO : null;

        AssignmentSubmission submission;
        if (existingOpt.isPresent()) {
            submission = existingOpt.get();
            if (hasText) {
                submission.setSubmissionText(submissionText.trim());
            }
            if (hasFile) {
                submission.setFileUrl(fileUrl);
                submission.setFileName(fileName);
                submission.setFileSizeBytes(fileSizeBytes);
            }
            submission.setSubmittedAt(Instant.now());
            submission.setIsLate(isLate);
            submission.setStatus(status);
            if (!requiresGrading) {
                submission.setScore(initialScore);
            }
        } else {
            submission = AssignmentSubmission.builder()
                    .assignment(assignment)
                    .user(user)
                    .cohort(cohort)
                    .submissionText(hasText ? submissionText.trim() : null)
                    .fileUrl(fileUrl)
                    .fileName(fileName)
                    .fileSizeBytes(fileSizeBytes)
                    .isLate(isLate)
                    .status(status)
                    .score(initialScore)
                    .submittedAt(Instant.now())
                    .build();
        }

        AssignmentSubmission saved = assignmentSubmissionRepository.save(submission);
        if (!requiresGrading && cohort != null) {
            recordLessonCompletion(cohort, assignment.getLesson(), user);
        }
        log.info("[AssignmentService] Đã lưu bài nộp ID {} của user {}, requiresGrading: {}, isLate: {}", saved.getId(), userId, requiresGrading, isLate);
        return mapToSubmissionDTO(saved, assignment.getMaxScore(), assignment.getPassingScore());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentSubmissionDTO> getSubmissionsForInstructor(Long assignmentId, Long cohortId, Long instructorId) {
        log.info("[AssignmentService] Lấy danh sách bài nộp cho Assignment ID {}, cohortId {}", assignmentId, cohortId);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài tập"));

        if (!isInstructorAuthorized(assignment.getLesson(), instructorId)) {
            User user = userRepository.findById(instructorId).orElse(null);
            if (user == null || (!SecurityUtils.isModerator(user) && !SecurityUtils.isAdmin(user))) {
                throw new RuntimeException("Bạn không có quyền xem danh sách bài nộp");
            }
        }

        List<AssignmentSubmission> list = (cohortId != null)
                ? assignmentSubmissionRepository.findByAssignmentIdAndCohortIdOrderBySubmittedAtDesc(assignmentId, cohortId)
                : assignmentSubmissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(assignmentId);

        return list.stream()
                .map(s -> mapToSubmissionDTO(s, assignment.getMaxScore(), assignment.getPassingScore()))
                .toList();
    }

    @Override
    @Transactional
    public AssignmentSubmissionDTO gradeSubmission(Long submissionId, AssignmentGradeDTO request, Long instructorId) {
        log.info("[AssignmentService] Chấm điểm bài nộp ID {} bởi instructorId: {}", submissionId, instructorId);
        AssignmentSubmission submission = assignmentSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài nộp"));

        Assignment assignment = submission.getAssignment();
        if (!isInstructorAuthorized(assignment.getLesson(), instructorId)) {
            User user = userRepository.findById(instructorId).orElse(null);
            if (user == null || (!SecurityUtils.isModerator(user) && !SecurityUtils.isAdmin(user))) {
                throw new RuntimeException("Bạn không có quyền chấm bài nộp này");
            }
        }

        boolean requiresGrading = assignment.getMaxScore() != null && assignment.getMaxScore().compareTo(BigDecimal.ZERO) > 0;
        if (requiresGrading) {
            if (request.getScore().compareTo(BigDecimal.ZERO) < 0 || request.getScore().compareTo(assignment.getMaxScore()) > 0) {
                throw new RuntimeException("Điểm số phải nằm trong khoảng từ 0 đến " + assignment.getMaxScore());
            }
        } else {
            if (request.getScore().compareTo(BigDecimal.ZERO) != 0) {
                request.setScore(BigDecimal.ZERO);
            }
        }

        User instructor = userRepository.findById(instructorId).orElse(null);
        submission.setScore(request.getScore());
        submission.setFeedback(request.getFeedback());
        submission.setGradedBy(instructor);
        submission.setGradedAt(Instant.now());
        submission.setStatus(!requiresGrading ? "COMPLETED" : "GRADED");

        AssignmentSubmission saved = assignmentSubmissionRepository.save(submission);

        if (submission.getCohort() != null) {
            boolean isPass = !requiresGrading || (request.getScore().compareTo(assignment.getPassingScore()) >= 0);
            if (isPass) {
                recordLessonCompletion(submission.getCohort(), assignment.getLesson(), submission.getUser());
            }
        }

        return mapToSubmissionDTO(saved, assignment.getMaxScore(), assignment.getPassingScore());
    }

    private void recordLessonCompletion(Cohort cohort, Lesson lesson, User user) {
        if (!lessonCompletionRepository.existsByCohort_IdAndLesson_IdAndUser_Id(cohort.getId(), lesson.getId(), user.getId())) {
            LessonCompletion completion = LessonCompletion.builder()
                    .cohort(cohort)
                    .lesson(lesson)
                    .user(user)
                    .build();
            lessonCompletionRepository.save(completion);
            log.info("[AssignmentService] Đã ghi nhận hoàn thành bài tập cho user {} tại cohort {}", user.getId(), cohort.getId());
        }

        long completedCount = lessonCompletionRepository.countByCohort_IdAndUser_Id(cohort.getId(), user.getId());
        List<Long> cohortLessonIds = cohortScheduleRepository.findLessonIdsByCohortId(cohort.getId());
        int totalCount = cohortLessonIds != null ? cohortLessonIds.size() : 0;
        int progressPercent = (totalCount > 0) ? (int) Math.min(100, Math.round(((double) completedCount / totalCount) * 100)) : 0;

        Optional<CohortMember> memberOpt = cohortMemberRepository.findByCohort_IdAndUser_Id(cohort.getId(), user.getId());
        if (memberOpt.isPresent()) {
            CohortMember member = memberOpt.get();
            if (totalCount > 0 && completedCount >= totalCount) {
                member.setIsPassed(true);
            }
            cohortMemberRepository.save(member);
        }
    }

    @Override
    public String uploadAssignmentAttachment(MultipartFile file, Long instructorId) {
        log.info("[AssignmentService] Giảng viên ID {} tải lên tệp đề bài đính kèm", instructorId);
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Tệp tin rỗng");
        }
        try {
            Map<String, Object> res = cloudinaryService.uploadFile(file, "/assignments/attachments", "raw");
            String url = (String) res.get("secure_url");
            return url != null ? url : (String) res.get("url");
        } catch (Exception e) {
            log.error("[AssignmentService] Lỗi tải tài liệu đề bài: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi tải tệp: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteStudentSubmission(Long assignmentId, Long cohortId, Long userId) {
        log.info("[AssignmentService] Yêu cầu xóa bài nộp cho assignmentId: {}, cohortId: {}, userId: {}", assignmentId, cohortId, userId);
        Optional<AssignmentSubmission> subOpt = (cohortId != null)
                ? assignmentSubmissionRepository.findByAssignmentIdAndUserIdAndCohortId(assignmentId, userId, cohortId)
                : assignmentSubmissionRepository.findByAssignmentIdAndUserId(assignmentId, userId);

        if (subOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy bài nộp để xóa");
        }

        AssignmentSubmission sub = subOpt.get();
        if (!Objects.equals(sub.getUser().getId(), userId)) {
            throw new RuntimeException("Bạn không có quyền xóa bài nộp của người khác");
        }

        assignmentSubmissionRepository.delete(sub);

        if (sub.getCohort() != null && sub.getAssignment() != null && sub.getAssignment().getLesson() != null) {
            Long lessonId = sub.getAssignment().getLesson().getId();
            lessonCompletionRepository.deleteByCohortIdAndLessonIdAndUserId(sub.getCohort().getId(), lessonId, userId);
            
            long completedCount = lessonCompletionRepository.countByCohort_IdAndUser_Id(sub.getCohort().getId(), userId);
            List<Long> cohortLessonIds = cohortScheduleRepository.findLessonIdsByCohortId(sub.getCohort().getId());
            int totalCount = cohortLessonIds != null ? cohortLessonIds.size() : 0;
            Optional<CohortMember> memberOpt = cohortMemberRepository.findByCohort_IdAndUser_Id(sub.getCohort().getId(), userId);
            if (memberOpt.isPresent()) {
                CohortMember member = memberOpt.get();
                if (totalCount > 0 && completedCount < totalCount) {
                    member.setIsPassed(false);
                }
                cohortMemberRepository.save(member);
            }
        }
        log.info("[AssignmentService] Đã xóa thành công bài nộp ID {} của user {}", sub.getId(), userId);
    }
}

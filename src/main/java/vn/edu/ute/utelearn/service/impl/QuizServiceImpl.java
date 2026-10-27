package vn.edu.ute.utelearn.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.ute.utelearn.dao.*;
import vn.edu.ute.utelearn.dto.*;
import vn.edu.ute.utelearn.entity.*;
import vn.edu.ute.utelearn.service.QuizService;
import vn.edu.ute.utelearn.service.GradebookService;
import vn.edu.ute.utelearn.util.SecurityUtils;
import org.springframework.context.annotation.Lazy;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final LessonRepository lessonRepository;
    private final CohortRepository cohortRepository;
    private final CohortScheduleRepository cohortScheduleRepository;
    private final CohortMemberRepository cohortMemberRepository;
    private final UserRepository userRepository;
    private final LessonCompletionRepository lessonCompletionRepository;
    private final CourseRepository courseRepository;
    @Lazy
    private final GradebookService gradebookService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private boolean isInstructorAuthorized(Lesson lesson, Long instructorId) {
        if (instructorId == null || lesson == null) {
            return false;
        }
        User user = userRepository.findById(instructorId).orElse(null);
        if (user != null && user.getRoles() != null && SecurityUtils.hasRole(user, "ADMIN", "ROLE_ADMIN")) {
            return true;
        }
        Section section = lesson.getSection();
        if (section == null) {
            return false;
        }
        if (section.getCourse() != null && section.getCourse().getCreatedBy() != null
                && section.getCourse().getCreatedBy().getId().equals(instructorId)) {
            return true;
        }
        if (section.getCohort() != null && section.getCohort().getCreatedBy() != null
                && section.getCohort().getCreatedBy().getId().equals(instructorId)) {
            return true;
        }
        return false;
    }

    private void markLessonAndParentAsDraft(Lesson lesson) {
        if (lesson == null) {
            return;
        }
        if (!"DRAFT".equalsIgnoreCase(lesson.getStatus())) {
            lesson.setStatus("DRAFT");
            lessonRepository.save(lesson);
        }
        Section section = lesson.getSection();
        if (section != null) {
            Cohort cohort = section.getCohort();
            if (cohort == null) {
                List<CohortSchedule> schedules = cohortScheduleRepository.findByLessonId(lesson.getId());
                if (!schedules.isEmpty() && schedules.get(0).getCohort() != null) {
                    cohort = schedules.get(0).getCohort();
                }
            }
            if (cohort != null) {
                if ("APPROVED".equalsIgnoreCase(cohort.getModerationStatus())) {
                    cohort.setModerationStatus("DRAFT");
                    cohortRepository.save(cohort);
                    log.info("[QuizService] Chuyển module id: {} về DRAFT do cập nhật quiz", cohort.getId());
                }
            } else {
                Course course = section.getCourse();
                if (course != null && "PUBLISHED".equalsIgnoreCase(course.getStatus())) {
                    course.setStatus("DRAFT");
                    courseRepository.save(course);
                    log.info("[QuizService] Chuyển khóa học id: {} về DRAFT do cập nhật quiz", course.getId());
                }
            }
        }
    }

    private List<QuestionOptionDTO> parseOptions(String json) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<QuestionOptionDTO>>() {});
        } catch (Exception e) {
            log.error("[QuizService] Lỗi parse options JSON: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String serializeOptions(List<QuestionOptionDTO> options) {
        if (options == null || options.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception e) {
            log.error("[QuizService] Lỗi serialize options: {}", e.getMessage());
            return "[]";
        }
    }

    private QuestionDTO mapToQuestionDTO(Question q, boolean revealAnswers) {
        List<QuestionOptionDTO> options = parseOptions(q.getOptions());
        if (!revealAnswers) {
            options = options.stream()
                    .map(opt -> QuestionOptionDTO.builder()
                            .id(opt.getId())
                            .text(opt.getText())
                            .isCorrect(null)
                            .build())
                    .toList();
        }
        return QuestionDTO.builder()
                .id(q.getId())
                .quizId(q.getQuiz().getId())
                .content(q.getContent())
                .questionType(q.getQuestionType())
                .points(q.getPoints())
                .explanation(revealAnswers ? q.getExplanation() : null)
                .orderIndex(q.getOrderIndex() != null ? q.getOrderIndex() : 1)
                .options(options)
                .build();
    }

    private boolean isReviewAllowed(Quiz quiz, Long cohortId) {
        if (quiz.getAllowReview() != null && !quiz.getAllowReview()) {
            return false;
        }
        if (cohortId != null) {
            Optional<CohortSchedule> scheduleOpt = cohortScheduleRepository.findByCohortIdAndLessonId(cohortId, quiz.getLesson().getId());
            if (scheduleOpt.isPresent()) {
                CohortSchedule schedule = scheduleOpt.get();
                if (Boolean.FALSE.equals(schedule.getAllowReviewAnswers())) {
                    return false;
                }
                if (schedule.getReviewAvailableAt() != null && Instant.now().isBefore(schedule.getReviewAvailableAt())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public QuizDTO getQuizByLessonId(Long lessonId, Long cohortId, Long userId) {
        Quiz quiz = quizRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new RuntimeException("Bài kiểm tra chưa được thiết lập cho bài học này"));

        List<Question> questions = questionRepository.findByQuizIdOrderByOrderIndexAscCreatedAtAsc(quiz.getId());
        BigDecimal totalPoints = questions.stream()
                .map(q -> q.getPoints() != null ? q.getPoints() : BigDecimal.ONE)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int attemptsCount = 0;
        BigDecimal highestScore = null;
        boolean userPassed = false;
        boolean canAttempt = true;

        if (userId != null && cohortId != null) {
            List<QuizSubmission> submissions = quizSubmissionRepository
                    .findByQuizIdAndUserIdAndCohortIdOrderBySubmittedAtDesc(quiz.getId(), userId, cohortId);
            attemptsCount = submissions.size();
            for (QuizSubmission sub : submissions) {
                if (highestScore == null || (sub.getScore() != null && sub.getScore().compareTo(highestScore) > 0)) {
                    highestScore = sub.getScore();
                }
                if (Boolean.TRUE.equals(sub.getIsPassed())) {
                    userPassed = true;
                }
            }
            if (quiz.getMaxAttempts() != null && quiz.getMaxAttempts() > 0 && attemptsCount >= quiz.getMaxAttempts()) {
                canAttempt = false;
            }
        }

        boolean isReviewer = false;
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && (SecurityUtils.isModerator(user) || SecurityUtils.isAdmin(user) || isInstructorAuthorized(quiz.getLesson(), userId))) {
                isReviewer = true;
            }
        }

        List<QuestionDTO> questionDTOs = new ArrayList<>();
        if (isReviewer) {
            questionDTOs = questions.stream()
                    .map(q -> mapToQuestionDTO(q, true))
                    .toList();
        }

        String unlockAt = null;
        String deadlineAt = null;
        String deadlineAtDisplay = null;
        boolean isPastDeadline = false;

        if (cohortId != null) {
            Optional<CohortSchedule> scheduleOpt = cohortScheduleRepository.findByCohortIdAndLessonId(cohortId, quiz.getLesson().getId());
            if (scheduleOpt.isPresent()) {
                CohortSchedule schedule = scheduleOpt.get();
                unlockAt = schedule.getFormattedUnlockAt();
                deadlineAt = schedule.getFormattedDeadlineAt();
                deadlineAtDisplay = schedule.getDisplayDeadlineAt();
                isPastDeadline = schedule.isDeadlinePassed();
            }
        }

        boolean allowLate = quiz.getAllowLateSubmission() != null ? quiz.getAllowLateSubmission() : true;
        if (isPastDeadline && !allowLate && !userPassed) {
            canAttempt = false;
        }

        boolean allowReview = isReviewAllowed(quiz, cohortId);

        return QuizDTO.builder()
                .id(quiz.getId())
                .lessonId(lessonId)
                .lessonStatus(quiz.getLesson() != null ? quiz.getLesson().getStatus() : null)
                .title(quiz.getTitle())
                .durationMinutes(quiz.getDurationMinutes())
                .passingScore(quiz.getPassingScore())
                .maxAttempts(quiz.getMaxAttempts())
                .shuffleQuestions(quiz.getShuffleQuestions())
                .allowReview(allowReview)
                .allowLateSubmission(allowLate)
                .examRules(quiz.getExamRules() != null && !quiz.getExamRules().isBlank() ? quiz.getExamRules() : Quiz.DEFAULT_EXAM_RULES)
                .totalQuestions(questions.size())
                .totalPoints(totalPoints)
                .questions(questionDTOs)
                .isReviewer(isReviewer)
                .userAttemptsCount(attemptsCount)
                .userHighestScore(highestScore)
                .userPassed(userPassed)
                .canAttempt(canAttempt)
                .unlockAt(unlockAt)
                .deadlineAt(deadlineAt)
                .deadlineAtDisplay(deadlineAtDisplay)
                .isPastDeadline(isPastDeadline)
                .build();
    }

    @Override
    @Transactional
    public QuizDTO getOrCreateQuizForLesson(Long lessonId, Long instructorId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài học ID: " + lessonId));

        if (!isInstructorAuthorized(lesson, instructorId)) {
            throw new RuntimeException("Bạn không có quyền quản lý bài kiểm tra này");
        }

        Quiz quiz = quizRepository.findByLessonId(lessonId).orElseGet(() -> {
            Quiz newQuiz = Quiz.builder()
                    .lesson(lesson)
                    .title(lesson.getTitle() != null ? lesson.getTitle() : "Bài kiểm tra trắc nghiệm")
                    .durationMinutes(15)
                    .passingScore(new BigDecimal("5.0"))
                    .maxAttempts(1)
                    .shuffleQuestions(true)
                    .allowReview(true)
                    .allowLateSubmission(true)
                    .examRules(Quiz.DEFAULT_EXAM_RULES)
                    .build();
            return quizRepository.save(newQuiz);
        });

        List<Question> questions = questionRepository.findByQuizIdOrderByOrderIndexAscCreatedAtAsc(quiz.getId());
        BigDecimal totalPoints = questions.stream()
                .map(q -> q.getPoints() != null ? q.getPoints() : BigDecimal.ONE)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<QuestionDTO> questionDTOs = questions.stream()
                .map(q -> mapToQuestionDTO(q, true))
                .toList();

        return QuizDTO.builder()
                .id(quiz.getId())
                .lessonId(lesson.getId())
                .lessonStatus(lesson.getStatus())
                .title(quiz.getTitle())
                .durationMinutes(quiz.getDurationMinutes())
                .passingScore(quiz.getPassingScore())
                .maxAttempts(quiz.getMaxAttempts())
                .shuffleQuestions(quiz.getShuffleQuestions())
                .allowReview(quiz.getAllowReview() != null ? quiz.getAllowReview() : true)
                .allowLateSubmission(quiz.getAllowLateSubmission() != null ? quiz.getAllowLateSubmission() : true)
                .examRules(quiz.getExamRules() != null && !quiz.getExamRules().isBlank() ? quiz.getExamRules() : Quiz.DEFAULT_EXAM_RULES)
                .totalQuestions(questions.size())
                .totalPoints(totalPoints)
                .questions(questionDTOs)
                .build();
    }

    @Override
    @Transactional
    public QuizDTO updateQuiz(Long quizId, QuizRequestDTO request, Long instructorId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài kiểm tra ID: " + quizId));

        if (!isInstructorAuthorized(quiz.getLesson(), instructorId)) {
            throw new RuntimeException("Bạn không có quyền cập nhật bài kiểm tra này");
        }

        boolean requiresReview = false;

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            String trimmedTitle = request.getTitle().trim();
            if (quiz.getTitle() == null || !quiz.getTitle().equals(trimmedTitle)) {
                quiz.setTitle(trimmedTitle);
                requiresReview = true;
            }
        }
        if (request.getDurationMinutes() != null) {
            if (!Objects.equals(quiz.getDurationMinutes(), request.getDurationMinutes())) {
                quiz.setDurationMinutes(request.getDurationMinutes());
                requiresReview = true;
            }
        }
        if (request.getPassingScore() != null) {
            if (quiz.getPassingScore() == null || quiz.getPassingScore().compareTo(request.getPassingScore()) != 0) {
                quiz.setPassingScore(request.getPassingScore());
                requiresReview = true;
            }
        }
        if (request.getMaxAttempts() != null) {
            if (!Objects.equals(quiz.getMaxAttempts(), request.getMaxAttempts())) {
                boolean isStrictDecrease = quiz.getMaxAttempts() != null && quiz.getMaxAttempts() > 0
                        && (request.getMaxAttempts() == 0 || request.getMaxAttempts() < quiz.getMaxAttempts());
                quiz.setMaxAttempts(request.getMaxAttempts());
                if (isStrictDecrease) {
                    requiresReview = true;
                }
            }
        }
        if (request.getShuffleQuestions() != null) {
            quiz.setShuffleQuestions(request.getShuffleQuestions());
        }
        if (request.getAllowReview() != null) {
            quiz.setAllowReview(request.getAllowReview());
        }
        if (request.getAllowLateSubmission() != null) {
            quiz.setAllowLateSubmission(request.getAllowLateSubmission());
        }
        if (request.getExamRules() != null) {
            String trimmedRules = request.getExamRules().trim();
            if (!Objects.equals(quiz.getExamRules(), trimmedRules)) {
                quiz.setExamRules(trimmedRules);
            }
        }

        Quiz saved = quizRepository.save(quiz);
        log.info("[QuizService] Đã cập nhật cấu hình Quiz ID: {}", saved.getId());

        List<Question> questions = questionRepository.findByQuizIdOrderByOrderIndexAscCreatedAtAsc(saved.getId());
        BigDecimal totalPoints = questions.stream()
                .map(q -> q.getPoints() != null ? q.getPoints() : BigDecimal.ONE)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (requiresReview) {
            markLessonAndParentAsDraft(saved.getLesson());
        }

        return QuizDTO.builder()
                .id(saved.getId())
                .lessonId(saved.getLesson().getId())
                .lessonStatus(saved.getLesson().getStatus())
                .title(saved.getTitle())
                .durationMinutes(saved.getDurationMinutes())
                .passingScore(saved.getPassingScore())
                .maxAttempts(saved.getMaxAttempts())
                .shuffleQuestions(saved.getShuffleQuestions())
                .allowReview(saved.getAllowReview() != null ? saved.getAllowReview() : true)
                .allowLateSubmission(saved.getAllowLateSubmission() != null ? saved.getAllowLateSubmission() : true)
                .examRules(saved.getExamRules())
                .totalQuestions(questions.size())
                .totalPoints(totalPoints)
                .build();
    }

    @Override
    public List<QuestionDTO> getQuestionsByQuizId(Long quizId, boolean revealAnswers, Long instructorId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài kiểm tra ID: " + quizId));

        User user = instructorId != null ? userRepository.findById(instructorId).orElse(null) : null;
        boolean isAuthorized = isInstructorAuthorized(quiz.getLesson(), instructorId)
                || (user != null && (SecurityUtils.isModerator(user) || SecurityUtils.isAdmin(user)));

        if (revealAnswers && !isAuthorized) {
            throw new RuntimeException("Bạn không có quyền xem đáp án bài kiểm tra này");
        }

        List<Question> questions = questionRepository.findByQuizIdOrderByOrderIndexAscCreatedAtAsc(quizId);
        return questions.stream()
                .map(q -> mapToQuestionDTO(q, revealAnswers))
                .toList();
    }

    @Override
    @Transactional
    public QuestionDTO createQuestion(Long quizId, QuestionRequestDTO request, Long instructorId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài kiểm tra ID: " + quizId));

        if (!isInstructorAuthorized(quiz.getLesson(), instructorId)) {
            throw new RuntimeException("Bạn không có quyền thêm câu hỏi vào bài kiểm tra này");
        }

        List<QuestionOptionDTO> options = request.getOptions();
        if (options == null) {
            options = new ArrayList<>();
        }
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).getId() == null || options.get(i).getId().isBlank()) {
                options.get(i).setId("opt_" + (i + 1) + "_" + System.currentTimeMillis() % 10000);
            }
        }

        if ("FILL_BLANK".equalsIgnoreCase(request.getQuestionType())) {
            for (QuestionOptionDTO opt : options) {
                opt.setIsCorrect(true);
            }
        }

        int nextOrder = (int) questionRepository.countByQuizId(quizId) + 1;
        Integer orderIndex = request.getOrderIndex() != null && request.getOrderIndex() > 0
                ? request.getOrderIndex() : nextOrder;

        Question question = Question.builder()
                .quiz(quiz)
                .content(request.getContent())
                .questionType(request.getQuestionType().toUpperCase())
                .points(request.getPoints() != null ? request.getPoints() : new BigDecimal("1.0"))
                .explanation(request.getExplanation())
                .orderIndex(orderIndex)
                .options(serializeOptions(options))
                .build();

        Question saved = questionRepository.save(question);
        markLessonAndParentAsDraft(quiz.getLesson());
        log.info("[QuizService] Đã tạo câu hỏi ID: {} cho Quiz ID: {}", saved.getId(), quizId);
        return mapToQuestionDTO(saved, true);
    }

    @Override
    @Transactional
    public QuestionDTO updateQuestion(Long questionId, QuestionRequestDTO request, Long instructorId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu hỏi ID: " + questionId));

        if (!isInstructorAuthorized(question.getQuiz().getLesson(), instructorId)) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa câu hỏi này");
        }

        boolean coreQuestionChanged = false;

        if (request.getContent() != null && !request.getContent().trim().equals(question.getContent())) {
            question.setContent(request.getContent().trim());
            coreQuestionChanged = true;
        }
        if (request.getQuestionType() != null && !request.getQuestionType().equalsIgnoreCase(question.getQuestionType())) {
            question.setQuestionType(request.getQuestionType().toUpperCase());
            coreQuestionChanged = true;
        }
        if (request.getPoints() != null && (question.getPoints() == null || question.getPoints().compareTo(request.getPoints()) != 0)) {
            question.setPoints(request.getPoints());
            coreQuestionChanged = true;
        }
        if (request.getOrderIndex() != null && request.getOrderIndex() > 0) {
            question.setOrderIndex(request.getOrderIndex());
        }
        if (request.getExplanation() != null) {
            question.setExplanation(request.getExplanation());
        }

        List<QuestionOptionDTO> options = request.getOptions();
        if (options != null) {
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).getId() == null || options.get(i).getId().isBlank()) {
                    options.get(i).setId("opt_" + (i + 1) + "_" + System.currentTimeMillis() % 10000);
                }
            }
            if ("FILL_BLANK".equalsIgnoreCase(request.getQuestionType())) {
                for (QuestionOptionDTO opt : options) {
                    opt.setIsCorrect(true);
                }
            }
            String newOptions = serializeOptions(options);
            if (!newOptions.equals(question.getOptions())) {
                question.setOptions(newOptions);
                coreQuestionChanged = true;
            }
        }

        Question saved = questionRepository.save(question);
        if (coreQuestionChanged) {
            markLessonAndParentAsDraft(question.getQuiz().getLesson());
        }
        log.info("[QuizService] Đã cập nhật câu hỏi ID: {}", saved.getId());
        return mapToQuestionDTO(saved, true);
    }

    @Override
    @Transactional
    public void deleteQuestion(Long questionId, Long instructorId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu hỏi ID: " + questionId));

        if (!isInstructorAuthorized(question.getQuiz().getLesson(), instructorId)) {
            throw new RuntimeException("Bạn không có quyền xóa câu hỏi này");
        }

        markLessonAndParentAsDraft(question.getQuiz().getLesson());
        questionRepository.delete(question);
        log.info("[QuizService] Đã xóa câu hỏi ID: {}", questionId);
    }

    @Override
    @Transactional
    public void reorderQuestions(Long quizId, List<Long> questionIds, Long instructorId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài kiểm tra ID: " + quizId));

        if (!isInstructorAuthorized(quiz.getLesson(), instructorId)) {
            throw new RuntimeException("Bạn không có quyền sắp xếp câu hỏi trong bài kiểm tra này");
        }

        if (questionIds == null || questionIds.isEmpty()) {
            return;
        }

        List<Question> questions = questionRepository.findByQuizIdOrderByOrderIndexAscCreatedAtAsc(quizId);
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        int order = 1;
        for (Long qId : questionIds) {
            Question q = questionMap.get(qId);
            if (q != null) {
                q.setOrderIndex(order++);
                questionRepository.save(q);
            }
        }

        log.info("[QuizService] Đã cập nhật lại thứ tự {} câu hỏi cho Quiz ID: {}", questionIds.size(), quizId);
    }

    @Override
    public QuizDTO startQuiz(Long quizId, Long cohortId, Long userId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài kiểm tra ID: " + quizId));

        if (!cohortMemberRepository.existsByCohort_IdAndUser_Id(cohortId, userId)) {
            User user = userRepository.findById(userId).orElse(null);
            boolean isAdminOrInstructor = user != null && user.getRoles() != null
                    && SecurityUtils.hasRole(user, "ADMIN", "ROLE_ADMIN", "INSTRUCTOR", "ROLE_INSTRUCTOR");
            if (!isAdminOrInstructor) {
                throw new RuntimeException("Bạn chưa ghi danh vào lớp học này");
            }
        }

        Optional<CohortSchedule> scheduleOpt = cohortScheduleRepository.findByCohortIdAndLessonId(cohortId, quiz.getLesson().getId());
        if (scheduleOpt.isPresent()) {
            CohortSchedule schedule = scheduleOpt.get();
            if (schedule.isLockedNow()) {
                throw new RuntimeException("Bài kiểm tra chưa đến thời điểm mở khóa");
            }
            if (schedule.getDeadlineAt() != null && Instant.now().isAfter(schedule.getDeadlineAt())) {
                if (Boolean.FALSE.equals(quiz.getAllowLateSubmission())) {
                    throw new RuntimeException("Bài kiểm tra đã quá hạn nộp bài và không cho phép làm trễ");
                }
            }
        }

        if (quiz.getMaxAttempts() != null && quiz.getMaxAttempts() > 0) {
            long existingAttempts = quizSubmissionRepository.countByQuizIdAndUserIdAndCohortId(quizId, userId, cohortId);
            if (existingAttempts >= quiz.getMaxAttempts()) {
                throw new RuntimeException("Bạn đã sử dụng hết " + quiz.getMaxAttempts() + " lượt làm bài");
            }
        }

        List<Question> questions = new ArrayList<>(questionRepository.findByQuizIdOrderByOrderIndexAscCreatedAtAsc(quizId));
        if (Boolean.TRUE.equals(quiz.getShuffleQuestions())) {
            Collections.shuffle(questions);
        }

        BigDecimal totalPoints = questions.stream()
                .map(q -> q.getPoints() != null ? q.getPoints() : BigDecimal.ONE)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<QuestionDTO> questionDTOs = questions.stream()
                .map(q -> mapToQuestionDTO(q, false))
                .toList();

        return QuizDTO.builder()
                .id(quiz.getId())
                .lessonId(quiz.getLesson().getId())
                .title(quiz.getTitle())
                .durationMinutes(quiz.getDurationMinutes())
                .passingScore(quiz.getPassingScore())
                .maxAttempts(quiz.getMaxAttempts())
                .totalQuestions(questions.size())
                .totalPoints(totalPoints)
                .questions(questionDTOs)
                .unlockAt(scheduleOpt.map(CohortSchedule::getFormattedUnlockAt).orElse(null))
                .deadlineAt(scheduleOpt.map(CohortSchedule::getFormattedDeadlineAt).orElse(null))
                .deadlineAtDisplay(scheduleOpt.map(CohortSchedule::getDisplayDeadlineAt).orElse(null))
                .isPastDeadline(scheduleOpt.map(CohortSchedule::isDeadlinePassed).orElse(false))
                .allowLateSubmission(quiz.getAllowLateSubmission() != null ? quiz.getAllowLateSubmission() : true)
                .build();
    }

    @Override
    @Transactional
    public QuizResultDTO submitQuiz(Long quizId, QuizSubmissionRequestDTO request, Long userId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài kiểm tra ID: " + quizId));

        Cohort cohort = cohortRepository.findById(request.getCohortId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin lớp học ID: " + request.getCohortId()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản học viên ID: " + userId));

        boolean isPastDeadline = false;
        Optional<CohortSchedule> scheduleOpt = cohortScheduleRepository.findByCohortIdAndLessonId(cohort.getId(), quiz.getLesson().getId());
        if (scheduleOpt.isPresent() && scheduleOpt.get().getDeadlineAt() != null) {
            if (Instant.now().isAfter(scheduleOpt.get().getDeadlineAt())) {
                isPastDeadline = true;
                if (Boolean.FALSE.equals(quiz.getAllowLateSubmission())) {
                    throw new RuntimeException("Đã quá hạn nộp bài kiểm tra và không cho phép làm trễ");
                }
            }
        }

        long existingAttempts = quizSubmissionRepository.countByQuizIdAndUserIdAndCohortId(quizId, userId, cohort.getId());
        if (quiz.getMaxAttempts() != null && quiz.getMaxAttempts() > 0 && existingAttempts >= quiz.getMaxAttempts()) {
            throw new RuntimeException("Bạn đã dùng hết số lần làm bài kiểm tra này");
        }

        List<Question> questions = questionRepository.findByQuizIdOrderByOrderIndexAscCreatedAtAsc(quizId);
        Map<Long, StudentAnswerDTO> studentAnswerMap = new HashMap<>();
        if (request.getAnswers() != null) {
            for (StudentAnswerDTO a : request.getAnswers()) {
                if (a.getQuestionId() != null) {
                    studentAnswerMap.put(a.getQuestionId(), a);
                }
            }
        }

        BigDecimal totalEarnedPoints = BigDecimal.ZERO;
        BigDecimal totalPossiblePoints = BigDecimal.ZERO;
        int correctCount = 0;

        List<QuestionReviewDTO> reviewList = new ArrayList<>();

        for (Question q : questions) {
            BigDecimal qPoints = q.getPoints() != null ? q.getPoints() : BigDecimal.ONE;
            totalPossiblePoints = totalPossiblePoints.add(qPoints);

            List<QuestionOptionDTO> allOptions = parseOptions(q.getOptions());
            StudentAnswerDTO ans = studentAnswerMap.get(q.getId());

            List<String> studentSelectedOptionIds = (ans != null && ans.getSelectedOptionIds() != null)
                    ? ans.getSelectedOptionIds() : Collections.emptyList();
            String studentText = (ans != null && ans.getTextAnswer() != null) ? ans.getTextAnswer().trim() : "";

            boolean isQuestionCorrect = false;
            String type = q.getQuestionType() != null ? q.getQuestionType().toUpperCase() : "SINGLE_CHOICE";

            if ("SINGLE_CHOICE".equals(type) || "TRUE_FALSE".equals(type)) {
                String correctOptId = allOptions.stream()
                        .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                        .map(QuestionOptionDTO::getId)
                        .findFirst()
                        .orElse(null);
                if (correctOptId != null && studentSelectedOptionIds.size() == 1
                        && correctOptId.equals(studentSelectedOptionIds.get(0))) {
                    isQuestionCorrect = true;
                }
            } else if ("MULTI_SELECT".equals(type)) {
                Set<String> correctIds = new HashSet<>(allOptions.stream()
                        .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                        .map(QuestionOptionDTO::getId)
                        .toList());
                Set<String> studentIds = new HashSet<>(studentSelectedOptionIds);
                if (!correctIds.isEmpty() && correctIds.equals(studentIds)) {
                    isQuestionCorrect = true;
                }
            } else if ("FILL_BLANK".equals(type)) {
                String normalizedStudentText = studentText != null ? studentText.trim() : "";
                for (QuestionOptionDTO opt : allOptions) {
                    if (opt.getText() == null || opt.getText().isBlank()) continue;
                    String raw = opt.getText().trim();
                    if (raw.equalsIgnoreCase(normalizedStudentText)) {
                        isQuestionCorrect = true;
                        break;
                    }
                    String[] parts = raw.split("[,;|/]");
                    for (String p : parts) {
                        if (p.trim().equalsIgnoreCase(normalizedStudentText)) {
                            isQuestionCorrect = true;
                            break;
                        }
                    }
                    if (isQuestionCorrect) break;
                }
            }

            BigDecimal earned = isQuestionCorrect ? qPoints : BigDecimal.ZERO;
            if (isQuestionCorrect) {
                correctCount++;
                totalEarnedPoints = totalEarnedPoints.add(qPoints);
            }

            reviewList.add(QuestionReviewDTO.builder()
                    .questionId(q.getId())
                    .content(q.getContent())
                    .questionType(type)
                    .points(qPoints)
                    .earnedPoints(earned)
                    .isCorrect(isQuestionCorrect)
                    .explanation(q.getExplanation())
                    .options(allOptions)
                    .studentSelectedOptionIds(studentSelectedOptionIds)
                    .studentTextAnswer(studentText)
                    .build());
        }

        BigDecimal scaledScore = BigDecimal.ZERO;
        if (totalPossiblePoints.compareTo(BigDecimal.ZERO) > 0) {
            scaledScore = totalEarnedPoints.multiply(new BigDecimal("10.0"))
                    .divide(totalPossiblePoints, 2, RoundingMode.HALF_UP);
        }

        BigDecimal passingScore = quiz.getPassingScore() != null ? quiz.getPassingScore() : new BigDecimal("5.0");
        boolean isPassed = scaledScore.compareTo(passingScore) >= 0;

        String answersJson;
        try {
            answersJson = objectMapper.writeValueAsString(reviewList);
        } catch (Exception e) {
            answersJson = "[]";
        }

        QuizSubmission submission = QuizSubmission.builder()
                .quiz(quiz)
                .cohort(cohort)
                .user(user)
                .startedAt(Instant.now())
                .submittedAt(Instant.now())
                .score(scaledScore)
                .isPassed(isPassed)
                .isLate(isPastDeadline)
                .status("SUBMITTED")
                .answers(answersJson)
                .build();

        QuizSubmission savedSub = quizSubmissionRepository.save(submission);
        log.info("[QuizService] Học viên ID: {} đã nộp Quiz ID: {}, điểm: {}, đạt: {}",
                userId, quizId, scaledScore, isPassed);

        if (isPassed) {
            Long lessonId = quiz.getLesson().getId();
            if (!lessonCompletionRepository.existsByCohort_IdAndLesson_IdAndUser_Id(cohort.getId(), lessonId, user.getId())) {
                LessonCompletion completion = LessonCompletion.builder()
                        .cohort(cohort)
                        .lesson(quiz.getLesson())
                        .user(user)
                        .completedAt(Instant.now())
                        .build();
                lessonCompletionRepository.save(completion);
                log.info("[QuizService] Đã tự động đánh dấu hoàn thành bài học Quiz ID: {} cho User ID: {}",
                        lessonId, userId);
            }
        }

        try {
            gradebookService.syncStudentGrade(cohort.getId(), user.getId());
        } catch (Exception e) {
            log.warn("[QuizService] Không thể đồng bộ bảng điểm cho Cohort {}: {}", cohort.getId(), e.getMessage());
        }

        int attemptNumber = (int) existingAttempts + 1;
        boolean canRetake = quiz.getMaxAttempts() == null || quiz.getMaxAttempts() <= 0
                || attemptNumber < quiz.getMaxAttempts();

        boolean allowReview = isReviewAllowed(quiz, cohort.getId());
        List<QuestionReviewDTO> returnedReviewList = allowReview ? reviewList : Collections.emptyList();

        return QuizResultDTO.builder()
                .submissionId(savedSub.getId())
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitle())
                .score(scaledScore)
                .passingScore(passingScore)
                .isPassed(isPassed)
                .totalQuestions(questions.size())
                .correctCount(correctCount)
                .startedAt(savedSub.getStartedAt())
                .submittedAt(savedSub.getSubmittedAt())
                .attemptNumber(attemptNumber)
                .maxAttempts(quiz.getMaxAttempts())
                .canRetake(canRetake)
                .allowReview(allowReview)
                .isLate(savedSub.getIsLate())
                .reviewQuestions(returnedReviewList)
                .build();
    }

    @Override
    public QuizResultDTO getSubmissionDetail(Long submissionId, Long userId) {
        QuizSubmission submission = quizSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài nộp ID: " + submissionId));

        if (!submission.getUser().getId().equals(userId)) {
            User user = userRepository.findById(userId).orElse(null);
            boolean isStaff = user != null && user.getRoles() != null
                    && SecurityUtils.hasRole(user, "ADMIN", "ROLE_ADMIN", "INSTRUCTOR", "ROLE_INSTRUCTOR");
            if (!isStaff) {
                throw new RuntimeException("Bạn không có quyền xem bài làm này");
            }
        }

        List<QuestionReviewDTO> reviewList = new ArrayList<>();
        try {
            if (submission.getAnswers() != null && !submission.getAnswers().isBlank()) {
                reviewList = objectMapper.readValue(submission.getAnswers(), new TypeReference<List<QuestionReviewDTO>>() {});
            }
        } catch (Exception e) {
            log.error("[QuizService] Lỗi parse kết quả bài nộp: {}", e.getMessage());
        }

        Quiz quiz = submission.getQuiz();
        int correctCount = (int) reviewList.stream().filter(r -> Boolean.TRUE.equals(r.getIsCorrect())).count();
        long totalAttempts = quizSubmissionRepository.countByQuizIdAndUserIdAndCohortId(quiz.getId(), submission.getUser().getId(), submission.getCohort().getId());

        boolean canRetake = quiz.getMaxAttempts() == null || quiz.getMaxAttempts() <= 0
                || totalAttempts < quiz.getMaxAttempts();

        User user = userRepository.findById(userId).orElse(null);
        boolean isStaff = user != null && user.getRoles() != null
                && SecurityUtils.hasRole(user, "ADMIN", "ROLE_ADMIN", "INSTRUCTOR", "ROLE_INSTRUCTOR");
        boolean allowReview = isStaff || isReviewAllowed(quiz, submission.getCohort().getId());
        List<QuestionReviewDTO> returnedReviewList = allowReview ? reviewList : Collections.emptyList();

        return QuizResultDTO.builder()
                .submissionId(submission.getId())
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitle())
                .score(submission.getScore())
                .passingScore(quiz.getPassingScore())
                .isPassed(submission.getIsPassed())
                .totalQuestions(reviewList.size())
                .correctCount(correctCount)
                .startedAt(submission.getStartedAt())
                .submittedAt(submission.getSubmittedAt())
                .attemptNumber((int) totalAttempts)
                .maxAttempts(quiz.getMaxAttempts())
                .canRetake(canRetake)
                .allowReview(allowReview)
                .isLate(submission.getIsLate())
                .reviewQuestions(returnedReviewList)
                .build();
    }

    @Override
    public List<QuizResultDTO> getSubmissionsForQuiz(Long quizId, Long cohortId, Long userId) {
        List<QuizSubmission> subs = quizSubmissionRepository
                .findByQuizIdAndUserIdAndCohortIdOrderBySubmittedAtDesc(quizId, userId, cohortId);
        List<QuizResultDTO> list = new ArrayList<>();
        for (QuizSubmission s : subs) {
            boolean allowReview = isReviewAllowed(s.getQuiz(), cohortId);
            list.add(QuizResultDTO.builder()
                    .submissionId(s.getId())
                    .quizId(quizId)
                    .quizTitle(s.getQuiz().getTitle())
                    .score(s.getScore())
                    .passingScore(s.getQuiz().getPassingScore())
                    .isPassed(s.getIsPassed())
                    .startedAt(s.getStartedAt())
                    .submittedAt(s.getSubmittedAt())
                    .allowReview(allowReview)
                    .isLate(s.getIsLate())
                    .build());
        }
        return list;
    }

    @Override
    public byte[] exportQuestionTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Danh_Sach_Cau_Hoi");

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

            String[] headers = {
                    "STT",
                    "Nội dung câu hỏi (*)",
                    "Loại câu hỏi (*)",
                    "Điểm số",
                    "Lựa chọn A",
                    "Lựa chọn B",
                    "Lựa chọn C",
                    "Lựa chọn D",
                    "Lựa chọn E",
                    "Lựa chọn F",
                    "Đáp án đúng (*)",
                    "Lời giải thích"
            };

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(28);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            String[][] sampleData = {
                    {"1", "Trong ngôn ngữ Java, phương thức nào là điểm khởi đầu thực thi của một ứng dụng console độc lập?", "SINGLE_CHOICE", "1.0", "public static void main(String[] args)", "public void start(String[] args)", "public static int run()", "void main()", "", "", "A", "Phương thức main với chữ ký public static void main(String[] args) là điểm khởi đầu bắt buộc của chương trình Java."},
                    {"2", "Những phát biểu nào sau đây là ĐÚNG về lập trình hướng đối tượng (OOP)?", "MULTI_SELECT", "1.5", "Tính đóng gói (Encapsulation) giúp che giấu dữ liệu nội bộ.", "Tính kế thừa (Inheritance) cho phép tái sử dụng mã nguồn.", "Lớp con bắt buộc phải ghi đè (override) tất cả các phương thức của lớp cha.", "Tính đa hình (Polymorphism) cho phép đối tượng thực thi nhiều hình thức khác nhau.", "", "", "A,B,D", "Lớp con không bắt buộc phải ghi đè tất cả các phương thức của lớp cha."},
                    {"3", "Trong cơ sở dữ liệu quan hệ, khóa chính (Primary Key) có thể chứa giá trị NULL.", "TRUE_FALSE", "1.0", "Đúng", "Sai", "", "", "", "", "B", "Ràng buộc khóa chính (Primary Key) tuyệt đối không được phép chứa giá trị NULL."},
                    {"4", "Trong ngôn ngữ lập trình Java, từ khóa nào được sử dụng để một lớp kế thừa từ một lớp khác?", "FILL_BLANK", "1.0", "", "", "", "", "", "", "extends, kế thừa, ke thua", "Trong Java, từ khóa extends được sử dụng để kế thừa lớp cha."}
            };

            for (int r = 0; r < sampleData.length; r++) {
                Row row = sheet.createRow(r + 1);
                row.setHeightInPoints(24);
                String[] rowData = sampleData[r];
                for (int c = 0; c < rowData.length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(rowData[c]);
                    if (c == 0 || c == 2 || c == 3 || c == 10) {
                        cell.setCellStyle(centerStyle);
                    } else {
                        cell.setCellStyle(dataStyle);
                    }
                }
            }

            sheet.setColumnWidth(0, 8 * 256);
            sheet.setColumnWidth(1, 45 * 256);
            sheet.setColumnWidth(2, 20 * 256);
            sheet.setColumnWidth(3, 12 * 256);
            sheet.setColumnWidth(4, 25 * 256);
            sheet.setColumnWidth(5, 25 * 256);
            sheet.setColumnWidth(6, 25 * 256);
            sheet.setColumnWidth(7, 25 * 256);
            sheet.setColumnWidth(8, 18 * 256);
            sheet.setColumnWidth(9, 18 * 256);
            sheet.setColumnWidth(10, 22 * 256);
            sheet.setColumnWidth(11, 40 * 256);

            Sheet guideSheet = workbook.createSheet("Huong_Dan");
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 13);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            CellStyle guideDataStyle = workbook.createCellStyle();
            guideDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Row titleRow = guideSheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("HƯỚNG DẪN SOẠN ĐỀ THI QUIZ BẰNG FILE EXCEL");
            titleCell.setCellStyle(titleStyle);

            String[][] instructions = {
                    {"1. Cột 'Nội dung câu hỏi (*)'", "Bắt buộc. Nhập câu hỏi cần kiểm tra kiến thức."},
                    {"2. Cột 'Loại câu hỏi (*)'", "Các giá trị hợp lệ: SINGLE_CHOICE, MULTI_SELECT, TRUE_FALSE, FILL_BLANK."},
                    {"3. Cột 'Điểm số'", "Điểm số của câu hỏi (mặc định 1.0 nếu để trống)."},
                    {"4. Cột 'Lựa chọn A..F'", "Nội dung các đáp án tương ứng A, B, C, D, E, F."},
                    {"5. Cột 'Đáp án đúng (*)'", "Quy định đáp án đúng cho từng loại câu hỏi:"},
                    {"   - SINGLE_CHOICE", "Nhập chữ cái đáp án đúng (ví dụ: A hoặc B hoặc C...)."},
                    {"   - MULTI_SELECT", "Nhập các chữ cái đúng cách nhau bởi dấu phẩy (ví dụ: A,B hoặc A,C,D)."},
                    {"   - TRUE_FALSE", "Nhập A (Đúng) hoặc B (Sai). Nếu để trống cột Lựa chọn A..B, hệ thống tự động sinh Đúng và Sai."},
                    {"   - FILL_BLANK", "Nhập các đáp án đúng dự đoán cách nhau bởi dấu phẩy (ví dụ: extends, kế thừa, ke thua)."},
                    {"6. Cột 'Lời giải thích'", "Tùy chọn. Giải thích ngắn gọn lý do đáp án đúng."},
                    {"7. Lưu ý quan trọng", "Không xóa hoặc đổi vị trí các cột trong Sheet 'Danh_Sach_Cau_Hoi'."}
            };

            for (int i = 0; i < instructions.length; i++) {
                Row r = guideSheet.createRow(i + 2);
                Cell c0 = r.createCell(0);
                c0.setCellValue(instructions[i][0]);
                c0.setCellStyle(guideDataStyle);
                Cell c1 = r.createCell(1);
                c1.setCellValue(instructions[i][1]);
                c1.setCellStyle(guideDataStyle);
            }

            guideSheet.setColumnWidth(0, 30 * 256);
            guideSheet.setColumnWidth(1, 80 * 256);

            workbook.write(out);
            log.info("[QuizService] Đã tạo file mẫu Excel câu hỏi Quiz thành công");
            return out.toByteArray();
        } catch (Exception e) {
            log.error("[QuizService] Lỗi khi tạo file mẫu Excel: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi tạo file mẫu Excel: " + e.getMessage());
        }
    }

    @Override
    public byte[] exportQuizQuestions(Long quizId, Long instructorId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài kiểm tra ID: " + quizId));

        User user = instructorId != null ? userRepository.findById(instructorId).orElse(null) : null;
        boolean isAuthorized = isInstructorAuthorized(quiz.getLesson(), instructorId)
                || (user != null && (SecurityUtils.isModerator(user) || SecurityUtils.isAdmin(user)));
        if (!isAuthorized) {
            throw new RuntimeException("Bạn không có quyền tải bộ câu hỏi của bài kiểm tra này");
        }

        List<Question> questions = questionRepository.findByQuizIdOrderByOrderIndexAscCreatedAtAsc(quizId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Danh_Sach_Cau_Hoi");

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

            String[] headers = {
                    "STT",
                    "Nội dung câu hỏi (*)",
                    "Loại câu hỏi (*)",
                    "Điểm số",
                    "Lựa chọn A",
                    "Lựa chọn B",
                    "Lựa chọn C",
                    "Lựa chọn D",
                    "Lựa chọn E",
                    "Lựa chọn F",
                    "Đáp án đúng (*)",
                    "Lời giải thích"
            };

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(28);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int r = 0; r < questions.size(); r++) {
                Question q = questions.get(r);
                Row row = sheet.createRow(r + 1);
                row.setHeightInPoints(24);

                Cell cellStt = row.createCell(0);
                cellStt.setCellValue(String.valueOf(r + 1));
                cellStt.setCellStyle(centerStyle);

                Cell cellContent = row.createCell(1);
                cellContent.setCellValue(q.getContent() != null ? q.getContent() : "");
                cellContent.setCellStyle(dataStyle);

                Cell cellType = row.createCell(2);
                cellType.setCellValue(q.getQuestionType() != null ? q.getQuestionType() : "");
                cellType.setCellStyle(centerStyle);

                Cell cellPoints = row.createCell(3);
                cellPoints.setCellValue(q.getPoints() != null ? q.getPoints().stripTrailingZeros().toPlainString() : "1.0");
                cellPoints.setCellStyle(centerStyle);

                List<QuestionOptionDTO> opts = parseOptions(q.getOptions());
                String correctAnswersStr = "";

                if ("FILL_BLANK".equalsIgnoreCase(q.getQuestionType())) {
                    List<String> textList = new ArrayList<>();
                    for (int j = 0; j < opts.size(); j++) {
                        String optText = opts.get(j).getText();
                        if (optText != null && !optText.isBlank()) {
                            textList.add(optText.trim());
                            if (j < 6) {
                                Cell cellOpt = row.createCell(4 + j);
                                cellOpt.setCellValue(optText.trim());
                                cellOpt.setCellStyle(dataStyle);
                            }
                        }
                    }
                    for (int j = opts.size(); j < 6; j++) {
                        Cell cellOpt = row.createCell(4 + j);
                        cellOpt.setCellValue("");
                        cellOpt.setCellStyle(dataStyle);
                    }
                    correctAnswersStr = String.join(", ", textList);
                } else {
                    List<String> correctLetters = new ArrayList<>();
                    for (int j = 0; j < 6; j++) {
                        Cell cellOpt = row.createCell(4 + j);
                        if (j < opts.size()) {
                            QuestionOptionDTO opt = opts.get(j);
                            cellOpt.setCellValue(opt.getText() != null ? opt.getText() : "");
                            if (Boolean.TRUE.equals(opt.getIsCorrect())) {
                                correctLetters.add(String.valueOf((char) ('A' + j)));
                            }
                        } else {
                            cellOpt.setCellValue("");
                        }
                        cellOpt.setCellStyle(dataStyle);
                    }
                    correctAnswersStr = String.join(",", correctLetters);
                }

                Cell cellCorrect = row.createCell(10);
                cellCorrect.setCellValue(correctAnswersStr);
                cellCorrect.setCellStyle(centerStyle);

                Cell cellExplanation = row.createCell(11);
                cellExplanation.setCellValue(q.getExplanation() != null ? q.getExplanation() : "");
                cellExplanation.setCellStyle(dataStyle);
            }

            sheet.setColumnWidth(0, 8 * 256);
            sheet.setColumnWidth(1, 45 * 256);
            sheet.setColumnWidth(2, 20 * 256);
            sheet.setColumnWidth(3, 12 * 256);
            sheet.setColumnWidth(4, 25 * 256);
            sheet.setColumnWidth(5, 25 * 256);
            sheet.setColumnWidth(6, 25 * 256);
            sheet.setColumnWidth(7, 25 * 256);
            sheet.setColumnWidth(8, 18 * 256);
            sheet.setColumnWidth(9, 18 * 256);
            sheet.setColumnWidth(10, 22 * 256);
            sheet.setColumnWidth(11, 40 * 256);

            Sheet guideSheet = workbook.createSheet("Huong_Dan");
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 13);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            CellStyle guideDataStyle = workbook.createCellStyle();
            guideDataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Row titleRow = guideSheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("HƯỚNG DẪN SOẠN ĐỀ THI QUIZ BẰNG FILE EXCEL");
            titleCell.setCellStyle(titleStyle);

            String[][] instructions = {
                    {"1. Cột 'Nội dung câu hỏi (*)'", "Bắt buộc. Nhập câu hỏi cần kiểm tra kiến thức."},
                    {"2. Cột 'Loại câu hỏi (*)'", "Các giá trị hợp lệ: SINGLE_CHOICE, MULTI_SELECT, TRUE_FALSE, FILL_BLANK."},
                    {"3. Cột 'Điểm số'", "Điểm số của câu hỏi (mặc định 1.0 nếu để trống)."},
                    {"4. Cột 'Lựa chọn A..F'", "Nội dung các đáp án tương ứng A, B, C, D, E, F."},
                    {"5. Cột 'Đáp án đúng (*)'", "Quy định đáp án đúng cho từng loại câu hỏi:"},
                    {"   - SINGLE_CHOICE", "Nhập chữ cái đáp án đúng (ví dụ: A hoặc B hoặc C...)."},
                    {"   - MULTI_SELECT", "Nhập các chữ cái đúng cách nhau bởi dấu phẩy (ví dụ: A,B hoặc A,C,D)."},
                    {"   - TRUE_FALSE", "Nhập A (Đúng) hoặc B (Sai). Nếu để trống cột Lựa chọn A..B, hệ thống tự động sinh Đúng và Sai."},
                    {"   - FILL_BLANK", "Nhập các đáp án đúng dự đoán cách nhau bởi dấu phẩy (ví dụ: extends, kế thừa, ke thua)."},
                    {"6. Cột 'Lời giải thích'", "Tùy chọn. Giải thích ngắn gọn lý do đáp án đúng."},
                    {"7. Lưu ý quan trọng", "Không xóa hoặc đổi vị trí các cột trong Sheet 'Danh_Sach_Cau_Hoi'."}
            };

            for (int i = 0; i < instructions.length; i++) {
                Row r = guideSheet.createRow(i + 2);
                Cell c0 = r.createCell(0);
                c0.setCellValue(instructions[i][0]);
                c0.setCellStyle(guideDataStyle);
                Cell c1 = r.createCell(1);
                c1.setCellValue(instructions[i][1]);
                c1.setCellStyle(guideDataStyle);
            }

            guideSheet.setColumnWidth(0, 30 * 256);
            guideSheet.setColumnWidth(1, 80 * 256);

            workbook.write(out);
            log.info("[QuizService] Đã xuất bộ {} câu hỏi cho Quiz ID: {} thành công", questions.size(), quizId);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("[QuizService] Lỗi khi xuất câu hỏi Quiz {}: {}", quizId, e.getMessage(), e);
            throw new RuntimeException("Lỗi khi xuất câu hỏi Quiz: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public QuestionImportResultDTO importQuestionsFromExcel(Long quizId, MultipartFile file, Long instructorId) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File tải lên không được để trống");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.toLowerCase().endsWith(".xlsx") && !originalFilename.toLowerCase().endsWith(".xls"))) {
            throw new RuntimeException("Định dạng file không hợp lệ. Vui lòng chọn file Excel (.xlsx hoặc .xls)");
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài kiểm tra ID: " + quizId));

        if (!isInstructorAuthorized(quiz.getLesson(), instructorId)) {
            throw new RuntimeException("Bạn không có quyền thêm câu hỏi vào bài kiểm tra này");
        }

        List<String> errors = new ArrayList<>();
        List<QuestionDTO> importedQuestions = new ArrayList<>();
        int totalRows = 0;
        int importedCount = 0;
        int failedCount = 0;

        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheet("Danh_Sach_Cau_Hoi");
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }
            if (sheet == null) {
                throw new RuntimeException("Không tìm thấy dữ liệu bảng câu hỏi trong file Excel");
            }

            int lastRowNum = sheet.getLastRowNum();
            for (int rowIndex = 1; rowIndex <= lastRowNum; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                totalRows++;
                int displayRow = rowIndex + 1;

                String content = getCellValue(row.getCell(1));
                if (content.isBlank()) {
                    errors.add("Dòng " + displayRow + ": Nội dung câu hỏi không được để trống");
                    failedCount++;
                    continue;
                }

                String typeRaw = getCellValue(row.getCell(2));
                String questionType = parseQuestionType(typeRaw);
                BigDecimal points = parsePoints(getCellValue(row.getCell(3)));

                List<String> rawOptions = new ArrayList<>();
                for (int c = 4; c <= 9; c++) {
                    String optVal = getCellValue(row.getCell(c));
                    if (!optVal.isBlank()) {
                        rawOptions.add(optVal);
                    }
                }

                String correctRaw = getCellValue(row.getCell(10));
                String explanation = getCellValue(row.getCell(11));

                List<QuestionOptionDTO> finalOptions = new ArrayList<>();

                if ("SINGLE_CHOICE".equalsIgnoreCase(questionType)) {
                    if (rawOptions.size() < 2) {
                        errors.add("Dòng " + displayRow + ": Câu hỏi trắc nghiệm một đáp án cần ít nhất 2 lựa chọn (A và B)");
                        failedCount++;
                        continue;
                    }
                    if (correctRaw.isBlank()) {
                        errors.add("Dòng " + displayRow + ": Chưa chỉ định đáp án đúng");
                        failedCount++;
                        continue;
                    }

                    int correctIndex = findOptionIndex(correctRaw, rawOptions);
                    if (correctIndex < 0) {
                        errors.add("Dòng " + displayRow + ": Không tìm thấy đáp án đúng '" + correctRaw + "' trong các lựa chọn");
                        failedCount++;
                        continue;
                    }

                    for (int i = 0; i < rawOptions.size(); i++) {
                        finalOptions.add(QuestionOptionDTO.builder()
                                .id("opt_" + (i + 1) + "_" + (System.currentTimeMillis() % 10000 + i))
                                .text(rawOptions.get(i))
                                .isCorrect(i == correctIndex)
                                .build());
                    }
                } else if ("MULTI_SELECT".equalsIgnoreCase(questionType)) {
                    if (rawOptions.size() < 2) {
                        errors.add("Dòng " + displayRow + ": Câu hỏi trắc nghiệm nhiều đáp án cần ít nhất 2 lựa chọn (A và B)");
                        failedCount++;
                        continue;
                    }
                    if (correctRaw.isBlank()) {
                        errors.add("Dòng " + displayRow + ": Chưa chỉ định các đáp án đúng");
                        failedCount++;
                        continue;
                    }

                    Set<Integer> correctIndices = findMultipleOptionIndices(correctRaw, rawOptions);
                    if (correctIndices.isEmpty()) {
                        errors.add("Dòng " + displayRow + ": Không tìm thấy đáp án đúng nào phù hợp trong các lựa chọn");
                        failedCount++;
                        continue;
                    }

                    for (int i = 0; i < rawOptions.size(); i++) {
                        finalOptions.add(QuestionOptionDTO.builder()
                                .id("opt_" + (i + 1) + "_" + (System.currentTimeMillis() % 10000 + i))
                                .text(rawOptions.get(i))
                                .isCorrect(correctIndices.contains(i))
                                .build());
                    }
                } else if ("TRUE_FALSE".equalsIgnoreCase(questionType)) {
                    String optA = rawOptions.size() > 0 ? rawOptions.get(0) : "Đúng";
                    String optB = rawOptions.size() > 1 ? rawOptions.get(1) : (optA.equalsIgnoreCase("Đúng") ? "Sai" : "Đúng");
                    List<String> tfOptions = List.of(optA, optB);

                    int correctIndex = parseTrueFalseCorrectIndex(correctRaw, tfOptions);
                    if (correctIndex < 0) {
                        errors.add("Dòng " + displayRow + ": Đáp án đúng cho câu hỏi Đúng/Sai phải là A (Đúng) hoặc B (Sai)");
                        failedCount++;
                        continue;
                    }

                    for (int i = 0; i < tfOptions.size(); i++) {
                        finalOptions.add(QuestionOptionDTO.builder()
                                .id("opt_" + (i + 1) + "_" + (System.currentTimeMillis() % 10000 + i))
                                .text(tfOptions.get(i))
                                .isCorrect(i == correctIndex)
                                .build());
                    }
                } else if ("FILL_BLANK".equalsIgnoreCase(questionType)) {
                    Set<String> acceptedWords = new LinkedHashSet<>();
                    for (String opt : rawOptions) {
                        if (!opt.isBlank()) {
                            acceptedWords.add(opt.trim());
                        }
                    }
                    if (!correctRaw.isBlank()) {
                        String[] parts = correctRaw.split("[,;\\n\\r|/]+");
                        for (String part : parts) {
                            String trimmed = part.trim();
                            if (!trimmed.isBlank()) {
                                acceptedWords.add(trimmed);
                            }
                        }
                    }

                    if (acceptedWords.isEmpty()) {
                        errors.add("Dòng " + displayRow + ": Vui lòng cung cấp ít nhất một đáp án đúng dự đoán cho câu hỏi điền từ");
                        failedCount++;
                        continue;
                    }

                    int idx = 1;
                    for (String word : acceptedWords) {
                        finalOptions.add(QuestionOptionDTO.builder()
                                .id("opt_" + idx + "_" + (System.currentTimeMillis() % 10000 + idx))
                                .text(word)
                                .isCorrect(true)
                                .build());
                        idx++;
                    }
                }

                int nextOrder = (int) questionRepository.countByQuizId(quizId) + 1;
                Question question = Question.builder()
                        .quiz(quiz)
                        .content(content)
                        .questionType(questionType)
                        .points(points)
                        .explanation(explanation)
                        .orderIndex(nextOrder)
                        .options(serializeOptions(finalOptions))
                        .build();

                Question saved = questionRepository.save(question);
                importedQuestions.add(mapToQuestionDTO(saved, true));
                importedCount++;
            }

            if (importedCount > 0) {
                markLessonAndParentAsDraft(quiz.getLesson());
            }

            log.info("[QuizService] Đã nhập {}/{} câu hỏi thành công từ Excel cho Quiz ID: {}", importedCount, totalRows, quizId);

            return QuestionImportResultDTO.builder()
                    .totalRows(totalRows)
                    .importedCount(importedCount)
                    .failedCount(failedCount)
                    .errors(errors)
                    .questions(importedQuestions)
                    .build();
        } catch (Exception e) {
            log.error("[QuizService] Lỗi khi đọc file Excel: {}", e.getMessage(), e);
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

    private String parseQuestionType(String raw) {
        if (raw == null || raw.isBlank()) return "SINGLE_CHOICE";
        String upper = raw.trim().toUpperCase();
        if (upper.contains("MULTI") || upper.contains("NHIEU")) return "MULTI_SELECT";
        if (upper.contains("TRUE") || upper.contains("FALSE") || upper.contains("DUNG") || upper.contains("SAI")) return "TRUE_FALSE";
        if (upper.contains("BLANK") || upper.contains("DIEN")) return "FILL_BLANK";
        return "SINGLE_CHOICE";
    }

    private BigDecimal parsePoints(String raw) {
        if (raw == null || raw.isBlank()) return new BigDecimal("1.0");
        try {
            BigDecimal val = new BigDecimal(raw.trim());
            if (val.compareTo(BigDecimal.ZERO) <= 0) return new BigDecimal("1.0");
            return val;
        } catch (Exception e) {
            return new BigDecimal("1.0");
        }
    }

    private int findOptionIndex(String correctRaw, List<String> options) {
        String trimmed = correctRaw.trim();
        if (trimmed.isEmpty()) return -1;
        char firstChar = Character.toUpperCase(trimmed.charAt(0));
        if (trimmed.length() == 1 && firstChar >= 'A' && firstChar <= 'F') {
            int idx = firstChar - 'A';
            if (idx < options.size()) return idx;
        }
        if (trimmed.length() == 1 && Character.isDigit(firstChar)) {
            int idx = Character.getNumericValue(firstChar) - 1;
            if (idx >= 0 && idx < options.size()) return idx;
        }
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).equalsIgnoreCase(trimmed)) {
                return i;
            }
        }
        return -1;
    }

    private Set<Integer> findMultipleOptionIndices(String correctRaw, List<String> options) {
        Set<Integer> result = new HashSet<>();
        String[] tokens = correctRaw.split("[,;\\s/]+");
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) continue;
            int idx = findOptionIndex(trimmed, options);
            if (idx >= 0) {
                result.add(idx);
            }
        }
        return result;
    }

    private int parseTrueFalseCorrectIndex(String correctRaw, List<String> tfOptions) {
        String trimmed = correctRaw.trim().toUpperCase();
        if ("A".equals(trimmed) || "1".equals(trimmed) || "ĐÚNG".equals(trimmed) || "DUNG".equals(trimmed) || "TRUE".equals(trimmed) || "T".equals(trimmed)) {
            return 0;
        }
        if ("B".equals(trimmed) || "2".equals(trimmed) || "SAI".equals(trimmed) || "FALSE".equals(trimmed) || "F".equals(trimmed)) {
            return 1;
        }
        for (int i = 0; i < tfOptions.size(); i++) {
            if (tfOptions.get(i).equalsIgnoreCase(trimmed)) {
                return i;
            }
        }
        return -1;
    }
}

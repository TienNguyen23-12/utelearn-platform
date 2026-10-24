package vn.edu.ute.utelearn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.mock.web.MockMultipartFile;
import vn.edu.ute.utelearn.dao.*;
import vn.edu.ute.utelearn.dto.*;
import vn.edu.ute.utelearn.entity.*;
import vn.edu.ute.utelearn.service.impl.QuizServiceImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QuizFeatureTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuizSubmissionRepository quizSubmissionRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CohortRepository cohortRepository;

    @Mock
    private CohortScheduleRepository cohortScheduleRepository;

    @Mock
    private CohortMemberRepository cohortMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LessonCompletionRepository lessonCompletionRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private QuizServiceImpl quizService;

    private User student;
    private User instructor;
    private Course course;
    private Cohort cohort;
    private Section section;
    private Lesson lesson;
    private Quiz quiz;

    @BeforeEach
    void setUp() {
        student = User.builder()
                .id(1L)
                .username("student1")
                .email("student1@ute.edu.vn")
                .fullName("Nguyen Van Hoc")
                .build();

        instructor = User.builder()
                .id(2L)
                .username("teacher1")
                .email("teacher1@ute.edu.vn")
                .fullName("Le Thi Day")
                .roles(Set.of(Role.builder().code("INSTRUCTOR").build()))
                .build();

        course = Course.builder()
                .id(10L)
                .title("Lập trình Java")
                .createdBy(instructor)
                .build();

        cohort = Cohort.builder()
                .id(20L)
                .code("JAVA-01")
                .course(course)
                .build();

        section = Section.builder()
                .id(30L)
                .course(course)
                .cohort(cohort)
                .build();

        lesson = Lesson.builder()
                .id(40L)
                .section(section)
                .title("Bài kiểm tra OOP")
                .lessonType("QUIZ")
                .build();

        quiz = Quiz.builder()
                .id(50L)
                .lesson(lesson)
                .title("Bài kiểm tra OOP")
                .durationMinutes(15)
                .passingScore(new BigDecimal("5.0"))
                .maxAttempts(2)
                .shuffleQuestions(false)
                .build();
    }

    @Test
    void testGetOrCreateQuizForLesson_NewQuizCreated() {
        when(lessonRepository.findById(40L)).thenReturn(Optional.of(lesson));
        when(userRepository.findById(2L)).thenReturn(Optional.of(instructor));
        when(quizRepository.findByLessonId(40L)).thenReturn(Optional.empty());
        when(quizRepository.save(any(Quiz.class))).thenReturn(quiz);
        when(questionRepository.findByQuizIdOrderByOrderIndexAscCreatedAtAsc(50L)).thenReturn(Collections.emptyList());

        QuizDTO result = quizService.getOrCreateQuizForLesson(40L, 2L);

        assertNotNull(result);
        assertEquals(50L, result.getId());
        assertEquals("Bài kiểm tra OOP", result.getTitle());
        verify(quizRepository).save(any(Quiz.class));
    }

    @Test
    void testCreateQuestion_SingleChoice() {
        when(quizRepository.findById(50L)).thenReturn(Optional.of(quiz));
        when(userRepository.findById(2L)).thenReturn(Optional.of(instructor));

        QuestionRequestDTO request = QuestionRequestDTO.builder()
                .content("Từ khóa kế thừa trong Java?")
                .questionType("SINGLE_CHOICE")
                .points(new BigDecimal("2.5"))
                .explanation("extends là từ khóa kế thừa")
                .options(List.of(
                        QuestionOptionDTO.builder().id("opt_1").text("extends").isCorrect(true).build(),
                        QuestionOptionDTO.builder().id("opt_2").text("implements").isCorrect(false).build()
                ))
                .build();

        Question saved = Question.builder()
                .id(101L)
                .quiz(quiz)
                .content(request.getContent())
                .questionType(request.getQuestionType())
                .points(request.getPoints())
                .explanation(request.getExplanation())
                .options("[{\"id\":\"opt_1\",\"text\":\"extends\",\"isCorrect\":true},{\"id\":\"opt_2\",\"text\":\"implements\",\"isCorrect\":false}]")
                .build();

        when(questionRepository.save(any(Question.class))).thenReturn(saved);

        QuestionDTO result = quizService.createQuestion(50L, request, 2L);

        assertNotNull(result);
        assertEquals(101L, result.getId());
        assertEquals("SINGLE_CHOICE", result.getQuestionType());
        assertEquals(2, result.getOptions().size());
        assertTrue(result.getOptions().get(0).getIsCorrect());
    }

    @Test
    void testStartQuiz_HidesCorrectAnswers() {
        when(quizRepository.findById(50L)).thenReturn(Optional.of(quiz));
        when(cohortMemberRepository.existsByCohort_IdAndUser_Id(20L, 1L)).thenReturn(true);
        when(cohortScheduleRepository.findByCohortIdAndLessonId(20L, 40L)).thenReturn(Optional.empty());
        when(quizSubmissionRepository.countByQuizIdAndUserIdAndCohortId(50L, 1L, 20L)).thenReturn(0L);

        Question q = Question.builder()
                .id(101L)
                .quiz(quiz)
                .content("Câu hỏi 1")
                .questionType("SINGLE_CHOICE")
                .points(new BigDecimal("10.0"))
                .options("[{\"id\":\"opt_1\",\"text\":\"A\",\"isCorrect\":true},{\"id\":\"opt_2\",\"text\":\"B\",\"isCorrect\":false}]")
                .build();

        when(questionRepository.findByQuizIdOrderByOrderIndexAscCreatedAtAsc(50L)).thenReturn(List.of(q));

        QuizDTO started = quizService.startQuiz(50L, 20L, 1L);

        assertNotNull(started);
        assertEquals(1, started.getQuestions().size());
        assertNull(started.getQuestions().get(0).getOptions().get(0).getIsCorrect());
    }

    @Test
    void testStartQuiz_ExceedMaxAttempts_ThrowsException() {
        when(quizRepository.findById(50L)).thenReturn(Optional.of(quiz));
        when(cohortMemberRepository.existsByCohort_IdAndUser_Id(20L, 1L)).thenReturn(true);
        when(cohortScheduleRepository.findByCohortIdAndLessonId(20L, 40L)).thenReturn(Optional.empty());
        when(quizSubmissionRepository.countByQuizIdAndUserIdAndCohortId(50L, 1L, 20L)).thenReturn(2L);

        assertThrows(RuntimeException.class, () -> quizService.startQuiz(50L, 20L, 1L));
    }

    @Test
    void testSubmitQuiz_FullScoringAndLessonCompletion() {
        when(quizRepository.findById(50L)).thenReturn(Optional.of(quiz));
        when(cohortRepository.findById(20L)).thenReturn(Optional.of(cohort));
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(cohortScheduleRepository.findByCohortIdAndLessonId(20L, 40L)).thenReturn(Optional.empty());
        when(quizSubmissionRepository.countByQuizIdAndUserIdAndCohortId(50L, 1L, 20L)).thenReturn(0L);

        Question q1 = Question.builder()
                .id(101L)
                .quiz(quiz)
                .content("Từ khóa kế thừa class?")
                .questionType("SINGLE_CHOICE")
                .points(new BigDecimal("2.5"))
                .options("[{\"id\":\"opt_1\",\"text\":\"extends\",\"isCorrect\":true},{\"id\":\"opt_2\",\"text\":\"implements\",\"isCorrect\":false}]")
                .build();

        Question q2 = Question.builder()
                .id(102L)
                .quiz(quiz)
                .content("Các trụ cột OOP?")
                .questionType("MULTI_SELECT")
                .points(new BigDecimal("2.5"))
                .options("[{\"id\":\"opt_1\",\"text\":\"Đóng gói\",\"isCorrect\":true},{\"id\":\"opt_2\",\"text\":\"Kế thừa\",\"isCorrect\":true},{\"id\":\"opt_3\",\"text\":\"Khác\",\"isCorrect\":false}]")
                .build();

        Question q3 = Question.builder()
                .id(103L)
                .quiz(quiz)
                .content("Java hỗ trợ đa kế thừa class trực tiếp?")
                .questionType("TRUE_FALSE")
                .points(new BigDecimal("2.5"))
                .options("[{\"id\":\"opt_1\",\"text\":\"Đúng\",\"isCorrect\":false},{\"id\":\"opt_2\",\"text\":\"Sai\",\"isCorrect\":true}]")
                .build();

        Question q4 = Question.builder()
                .id(104L)
                .quiz(quiz)
                .content("Tên viết tắt máy ảo Java?")
                .questionType("FILL_BLANK")
                .points(new BigDecimal("2.5"))
                .options("[{\"id\":\"opt_1\",\"text\":\"JVM\",\"isCorrect\":true}]")
                .build();

        when(questionRepository.findByQuizIdOrderByOrderIndexAscCreatedAtAsc(50L)).thenReturn(List.of(q1, q2, q3, q4));

        QuizSubmission savedSub = QuizSubmission.builder()
                .id(999L)
                .quiz(quiz)
                .user(student)
                .cohort(cohort)
                .startedAt(Instant.now())
                .submittedAt(Instant.now())
                .score(new BigDecimal("10.00"))
                .isPassed(true)
                .status("SUBMITTED")
                .build();

        when(quizSubmissionRepository.save(any(QuizSubmission.class))).thenReturn(savedSub);
        when(lessonCompletionRepository.existsByCohort_IdAndLesson_IdAndUser_Id(20L, 40L, 1L)).thenReturn(false);

        QuizSubmissionRequestDTO request = QuizSubmissionRequestDTO.builder()
                .cohortId(20L)
                .answers(List.of(
                        StudentAnswerDTO.builder().questionId(101L).selectedOptionIds(List.of("opt_1")).build(),
                        StudentAnswerDTO.builder().questionId(102L).selectedOptionIds(List.of("opt_1", "opt_2")).build(),
                        StudentAnswerDTO.builder().questionId(103L).selectedOptionIds(List.of("opt_2")).build(),
                        StudentAnswerDTO.builder().questionId(104L).textAnswer("jvm").build()
                ))
                .build();

        QuizResultDTO result = quizService.submitQuiz(50L, request, 1L);

        assertNotNull(result);
        assertEquals(new BigDecimal("10.00"), result.getScore());
        assertTrue(result.getIsPassed());
        assertEquals(4, result.getCorrectCount());
        assertEquals(4, result.getTotalQuestions());

        verify(lessonCompletionRepository).save(any(LessonCompletion.class));
    }

    @Test
    void testSubmitQuiz_FailedScore_DoesNotCompleteLesson() {
        when(quizRepository.findById(50L)).thenReturn(Optional.of(quiz));
        when(cohortRepository.findById(20L)).thenReturn(Optional.of(cohort));
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(cohortScheduleRepository.findByCohortIdAndLessonId(20L, 40L)).thenReturn(Optional.empty());
        when(quizSubmissionRepository.countByQuizIdAndUserIdAndCohortId(50L, 1L, 20L)).thenReturn(0L);

        Question q1 = Question.builder()
                .id(101L)
                .quiz(quiz)
                .content("Câu 1")
                .questionType("SINGLE_CHOICE")
                .points(new BigDecimal("10.0"))
                .options("[{\"id\":\"opt_1\",\"text\":\"A\",\"isCorrect\":true},{\"id\":\"opt_2\",\"text\":\"B\",\"isCorrect\":false}]")
                .build();

        when(questionRepository.findByQuizIdOrderByOrderIndexAscCreatedAtAsc(50L)).thenReturn(List.of(q1));

        QuizSubmission savedSub = QuizSubmission.builder()
                .id(999L)
                .quiz(quiz)
                .user(student)
                .cohort(cohort)
                .startedAt(Instant.now())
                .submittedAt(Instant.now())
                .score(new BigDecimal("0.00"))
                .isPassed(false)
                .status("SUBMITTED")
                .build();

        when(quizSubmissionRepository.save(any(QuizSubmission.class))).thenReturn(savedSub);

        QuizSubmissionRequestDTO request = QuizSubmissionRequestDTO.builder()
                .cohortId(20L)
                .answers(List.of(
                        StudentAnswerDTO.builder().questionId(101L).selectedOptionIds(List.of("opt_2")).build()
                ))
                .build();

        QuizResultDTO result = quizService.submitQuiz(50L, request, 1L);

        assertNotNull(result);
        assertEquals(new BigDecimal("0.00"), result.getScore());
        assertFalse(result.getIsPassed());
        assertEquals(0, result.getCorrectCount());

        verify(lessonCompletionRepository, never()).save(any(LessonCompletion.class));
    }

    @Test
    void testUpdateQuizInPublishedCourseRevertsCourseToDraft() {
        section.setCohort(null);
        course.setStatus("PUBLISHED");
        lesson.setStatus("PUBLISHED");

        when(userRepository.findById(2L)).thenReturn(Optional.of(instructor));
        when(quizRepository.findById(50L)).thenReturn(Optional.of(quiz));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionRepository.findByQuizIdOrderByOrderIndexAscCreatedAtAsc(50L)).thenReturn(Collections.emptyList());

        QuizRequestDTO request = QuizRequestDTO.builder()
                .title("Đề thi đã chỉnh sửa")
                .durationMinutes(30)
                .passingScore(new BigDecimal("6.0"))
                .build();

        QuizDTO updated = quizService.updateQuiz(50L, request, 2L);

        assertNotNull(updated);
        assertEquals("DRAFT", lesson.getStatus());
        assertEquals("DRAFT", course.getStatus());
        verify(courseRepository, atLeastOnce()).save(course);
        verify(lessonRepository, atLeastOnce()).save(lesson);
    }

    @Test
    void testUpdateQuizInApprovedCohortRevertsCohortToDraftNotCourse() {
        cohort.setModerationStatus("APPROVED");
        course.setStatus("PUBLISHED");
        lesson.setStatus("PUBLISHED");
        section.setCohort(cohort);

        when(userRepository.findById(2L)).thenReturn(Optional.of(instructor));
        when(quizRepository.findById(50L)).thenReturn(Optional.of(quiz));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionRepository.findByQuizIdOrderByOrderIndexAscCreatedAtAsc(50L)).thenReturn(Collections.emptyList());

        QuizRequestDTO request = QuizRequestDTO.builder()
                .title("Đề thi module đã chỉnh sửa")
                .durationMinutes(20)
                .passingScore(new BigDecimal("7.0"))
                .build();

        QuizDTO updated = quizService.updateQuiz(50L, request, 2L);

        assertNotNull(updated);
        assertEquals("DRAFT", lesson.getStatus());
        assertEquals("DRAFT", cohort.getModerationStatus());
        assertEquals("PUBLISHED", course.getStatus());
        verify(cohortRepository, atLeastOnce()).save(cohort);
        verify(courseRepository, never()).save(course);
        verify(lessonRepository, atLeastOnce()).save(lesson);
    }

    @Test
    void testGetQuizByLessonIdForModeratorRevealsQuestionsAndAnswers() {
        User moderator = User.builder()
                .id(99L)
                .username("moderator1")
                .email("mod@utelearn.edu.vn")
                .roles(Set.of(Role.builder().code("MODERATOR").build()))
                .build();

        Question q = Question.builder()
                .id(101L)
                .quiz(quiz)
                .content("Câu hỏi 1?")
                .questionType("SINGLE_CHOICE")
                .points(new BigDecimal("2.0"))
                .options("[{\"id\":\"opt_1\",\"text\":\"A\",\"isCorrect\":true},{\"id\":\"opt_2\",\"text\":\"B\",\"isCorrect\":false}]")
                .build();

        when(quizRepository.findByLessonId(40L)).thenReturn(Optional.of(quiz));
        when(questionRepository.findByQuizIdOrderByOrderIndexAscCreatedAtAsc(50L)).thenReturn(List.of(q));
        when(userRepository.findById(99L)).thenReturn(Optional.of(moderator));

        QuizDTO result = quizService.getQuizByLessonId(40L, 20L, 99L);

        assertNotNull(result);
        assertTrue(result.getIsReviewer());
        assertNotNull(result.getQuestions());
        assertEquals(1, result.getQuestions().size());
        assertEquals("Câu hỏi 1?", result.getQuestions().get(0).getContent());
        assertNotNull(result.getQuestions().get(0).getOptions());
        assertEquals(2, result.getQuestions().get(0).getOptions().size());
        assertTrue(result.getQuestions().get(0).getOptions().get(0).getIsCorrect());
        assertFalse(result.getQuestions().get(0).getOptions().get(1).getIsCorrect());
    }

    @Test
    void testGetQuizByLessonIdForStudentHidesQuestions() {
        Question q = Question.builder()
                .id(101L)
                .quiz(quiz)
                .content("Câu hỏi bí mật?")
                .questionType("SINGLE_CHOICE")
                .points(new BigDecimal("2.0"))
                .options("[{\"id\":\"opt_1\",\"text\":\"A\",\"isCorrect\":true}]")
                .build();

        when(quizRepository.findByLessonId(40L)).thenReturn(Optional.of(quiz));
        when(questionRepository.findByQuizIdOrderByOrderIndexAscCreatedAtAsc(50L)).thenReturn(List.of(q));
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(quizSubmissionRepository.findByQuizIdAndUserIdAndCohortIdOrderBySubmittedAtDesc(50L, 1L, 20L))
                .thenReturn(Collections.emptyList());

        QuizDTO result = quizService.getQuizByLessonId(40L, 20L, 1L);

        assertNotNull(result);
        assertFalse(result.getIsReviewer());
        assertTrue(result.getQuestions().isEmpty());
    }

    @Test
    void testGetQuestionsByQuizIdForModeratorAuthorized() {
        User moderator = User.builder()
                .id(99L)
                .username("moderator1")
                .email("mod@utelearn.edu.vn")
                .roles(Set.of(Role.builder().code("ROLE_MODERATOR").build()))
                .build();

        Question q = Question.builder()
                .id(101L)
                .quiz(quiz)
                .content("Câu hỏi kiểm định?")
                .questionType("SINGLE_CHOICE")
                .points(new BigDecimal("1.0"))
                .options("[{\"id\":\"opt_1\",\"text\":\"Đúng\",\"isCorrect\":true}]")
                .build();

        when(quizRepository.findById(50L)).thenReturn(Optional.of(quiz));
        when(userRepository.findById(99L)).thenReturn(Optional.of(moderator));
        when(questionRepository.findByQuizIdOrderByOrderIndexAscCreatedAtAsc(50L)).thenReturn(List.of(q));

        List<QuestionDTO> questions = quizService.getQuestionsByQuizId(50L, true, 99L);

        assertNotNull(questions);
        assertEquals(1, questions.size());
        assertTrue(questions.get(0).getOptions().get(0).getIsCorrect());
    }

    @Test
    void testUpdateQuiz_AllowReview() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(instructor));
        when(quizRepository.findById(50L)).thenReturn(Optional.of(quiz));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionRepository.findByQuizIdOrderByOrderIndexAscCreatedAtAsc(50L)).thenReturn(Collections.emptyList());

        QuizRequestDTO request = QuizRequestDTO.builder()
                .title("Đề thi cập nhật")
                .allowReview(false)
                .build();

        QuizDTO updated = quizService.updateQuiz(50L, request, 2L);

        assertNotNull(updated);
        assertFalse(updated.getAllowReview());
        assertFalse(quiz.getAllowReview());
    }

    @Test
    void testSubmitQuiz_WhenAllowReviewFalse_HidesReviewQuestions() {
        quiz.setAllowReview(false);

        when(quizRepository.findById(50L)).thenReturn(Optional.of(quiz));
        when(cohortRepository.findById(20L)).thenReturn(Optional.of(cohort));
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(cohortScheduleRepository.findByCohortIdAndLessonId(20L, 40L)).thenReturn(Optional.empty());
        when(quizSubmissionRepository.countByQuizIdAndUserIdAndCohortId(50L, 1L, 20L)).thenReturn(0L);

        Question q = Question.builder()
                .id(101L)
                .quiz(quiz)
                .content("Câu hỏi 1")
                .questionType("SINGLE_CHOICE")
                .points(new BigDecimal("10.0"))
                .options("[{\"id\":\"opt_1\",\"text\":\"A\",\"isCorrect\":true}]")
                .build();

        when(questionRepository.findByQuizIdOrderByOrderIndexAscCreatedAtAsc(50L)).thenReturn(List.of(q));

        QuizSubmission savedSub = QuizSubmission.builder()
                .id(999L)
                .quiz(quiz)
                .user(student)
                .cohort(cohort)
                .startedAt(Instant.now())
                .submittedAt(Instant.now())
                .score(new BigDecimal("10.00"))
                .isPassed(true)
                .status("SUBMITTED")
                .build();

        when(quizSubmissionRepository.save(any(QuizSubmission.class))).thenReturn(savedSub);

        QuizSubmissionRequestDTO request = QuizSubmissionRequestDTO.builder()
                .cohortId(20L)
                .answers(List.of(
                        StudentAnswerDTO.builder().questionId(101L).selectedOptionIds(List.of("opt_1")).build()
                ))
                .build();

        QuizResultDTO result = quizService.submitQuiz(50L, request, 1L);

        assertNotNull(result);
        assertFalse(result.getAllowReview());
        assertTrue(result.getReviewQuestions().isEmpty());
    }

    @Test
    void testGetSubmissionDetail_WhenAllowReviewFalse_HidesReviewQuestionsForStudent() {
        quiz.setAllowReview(false);

        QuizSubmission submission = QuizSubmission.builder()
                .id(888L)
                .quiz(quiz)
                .user(student)
                .cohort(cohort)
                .startedAt(Instant.now())
                .submittedAt(Instant.now())
                .score(new BigDecimal("8.00"))
                .isPassed(true)
                .answers("[{\"questionId\":101,\"content\":\"Q1\",\"isCorrect\":true}]")
                .build();

        when(quizSubmissionRepository.findById(888L)).thenReturn(Optional.of(submission));
        when(quizSubmissionRepository.countByQuizIdAndUserIdAndCohortId(50L, 1L, 20L)).thenReturn(1L);

        QuizResultDTO result = quizService.getSubmissionDetail(888L, 1L);

        assertNotNull(result);
        assertFalse(result.getAllowReview());
        assertTrue(result.getReviewQuestions().isEmpty());
    }

    @Test
    void testSubmitQuiz_FillBlank_MultipleOptionsMatch() {
        quiz.setAllowReview(true);

        when(quizRepository.findById(50L)).thenReturn(Optional.of(quiz));
        when(cohortRepository.findById(20L)).thenReturn(Optional.of(cohort));
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(cohortScheduleRepository.findByCohortIdAndLessonId(20L, 40L)).thenReturn(Optional.empty());
        when(quizSubmissionRepository.countByQuizIdAndUserIdAndCohortId(50L, 1L, 20L)).thenReturn(0L);

        Question q = Question.builder()
                .id(102L)
                .quiz(quiz)
                .content("Từ khóa kế thừa trong Java?")
                .questionType("FILL_BLANK")
                .points(new BigDecimal("10.0"))
                .options("[{\"id\":\"opt_1\",\"text\":\"extends\",\"isCorrect\":true},{\"id\":\"opt_2\",\"text\":\"kế thừa\",\"isCorrect\":true}]")
                .build();

        when(questionRepository.findByQuizIdOrderByOrderIndexAscCreatedAtAsc(50L)).thenReturn(List.of(q));
        when(quizSubmissionRepository.save(any(QuizSubmission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuizSubmissionRequestDTO request = QuizSubmissionRequestDTO.builder()
                .cohortId(20L)
                .answers(List.of(
                        StudentAnswerDTO.builder().questionId(102L).textAnswer("  Kế Thừa  ").build()
                ))
                .build();

        QuizResultDTO result = quizService.submitQuiz(50L, request, 1L);

        assertNotNull(result);
        assertEquals(0, new BigDecimal("10.00").compareTo(result.getScore()));
        assertTrue(result.getIsPassed());
        assertFalse(result.getReviewQuestions().isEmpty());
        assertTrue(result.getReviewQuestions().get(0).getIsCorrect());
    }

    @Test
    void testSubmitQuiz_FillBlank_DelimitedOptionsMatch() {
        quiz.setAllowReview(true);

        when(quizRepository.findById(50L)).thenReturn(Optional.of(quiz));
        when(cohortRepository.findById(20L)).thenReturn(Optional.of(cohort));
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(cohortScheduleRepository.findByCohortIdAndLessonId(20L, 40L)).thenReturn(Optional.empty());
        when(quizSubmissionRepository.countByQuizIdAndUserIdAndCohortId(50L, 1L, 20L)).thenReturn(0L);

        Question q = Question.builder()
                .id(103L)
                .quiz(quiz)
                .content("Các tính chất của OOP?")
                .questionType("FILL_BLANK")
                .points(new BigDecimal("10.0"))
                .options("[{\"id\":\"opt_1\",\"text\":\"đóng gói; đa hình, kế thừa / trừu tượng\",\"isCorrect\":true}]")
                .build();

        when(questionRepository.findByQuizIdOrderByOrderIndexAscCreatedAtAsc(50L)).thenReturn(List.of(q));
        when(quizSubmissionRepository.save(any(QuizSubmission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuizSubmissionRequestDTO request = QuizSubmissionRequestDTO.builder()
                .cohortId(20L)
                .answers(List.of(
                        StudentAnswerDTO.builder().questionId(103L).textAnswer("đa hình").build()
                ))
                .build();

        QuizResultDTO result = quizService.submitQuiz(50L, request, 1L);

        assertNotNull(result);
        assertEquals(0, new BigDecimal("10.00").compareTo(result.getScore()));
        assertTrue(result.getIsPassed());
        assertFalse(result.getReviewQuestions().isEmpty());
        assertTrue(result.getReviewQuestions().get(0).getIsCorrect());
    }

    @Test
    void testSubmitQuiz_FillBlank_IncorrectAnswer() {
        quiz.setAllowReview(true);

        when(quizRepository.findById(50L)).thenReturn(Optional.of(quiz));
        when(cohortRepository.findById(20L)).thenReturn(Optional.of(cohort));
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(cohortScheduleRepository.findByCohortIdAndLessonId(20L, 40L)).thenReturn(Optional.empty());
        when(quizSubmissionRepository.countByQuizIdAndUserIdAndCohortId(50L, 1L, 20L)).thenReturn(0L);

        Question q = Question.builder()
                .id(104L)
                .quiz(quiz)
                .content("Từ khóa kế thừa trong Java?")
                .questionType("FILL_BLANK")
                .points(new BigDecimal("10.0"))
                .options("[{\"id\":\"opt_1\",\"text\":\"extends\",\"isCorrect\":true},{\"id\":\"opt_2\",\"text\":\"kế thừa\",\"isCorrect\":true}]")
                .build();

        when(questionRepository.findByQuizIdOrderByOrderIndexAscCreatedAtAsc(50L)).thenReturn(List.of(q));
        when(quizSubmissionRepository.save(any(QuizSubmission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuizSubmissionRequestDTO request = QuizSubmissionRequestDTO.builder()
                .cohortId(20L)
                .answers(List.of(
                        StudentAnswerDTO.builder().questionId(104L).textAnswer("implements").build()
                ))
                .build();

        QuizResultDTO result = quizService.submitQuiz(50L, request, 1L);

        assertNotNull(result);
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getScore()));
        assertFalse(result.getIsPassed());
        assertFalse(result.getReviewQuestions().isEmpty());
        assertFalse(result.getReviewQuestions().get(0).getIsCorrect());
    }

    @Test
    void testCreateQuestion_FillBlank_ForcesAllOptionsCorrect() {
        when(quizRepository.findById(50L)).thenReturn(Optional.of(quiz));
        when(userRepository.findById(2L)).thenReturn(Optional.of(instructor));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
            Question question = invocation.getArgument(0);
            question.setId(205L);
            return question;
        });

        QuestionRequestDTO request = QuestionRequestDTO.builder()
                .content("Điền từ vào ô trống")
                .questionType("FILL_BLANK")
                .points(new BigDecimal("2.0"))
                .options(List.of(
                        QuestionOptionDTO.builder().text("cách 1").isCorrect(false).build(),
                        QuestionOptionDTO.builder().text("cách 2").isCorrect(null).build()
                ))
                .build();

        QuestionDTO created = quizService.createQuestion(50L, request, 2L);

        assertNotNull(created);
        assertEquals(2, created.getOptions().size());
        assertTrue(created.getOptions().stream().allMatch(o -> Boolean.TRUE.equals(o.getIsCorrect())));
    }

    @Test
    void testExportQuestionTemplate_Success() throws Exception {
        byte[] bytes = quizService.exportQuestionTemplate();

        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes);
             Workbook workbook = WorkbookFactory.create(in)) {
            assertEquals(2, workbook.getNumberOfSheets());
            Sheet qSheet = workbook.getSheet("Danh_Sach_Cau_Hoi");
            assertNotNull(qSheet);
            Sheet gSheet = workbook.getSheet("Huong_Dan");
            assertNotNull(gSheet);

            Row headerRow = qSheet.getRow(0);
            assertNotNull(headerRow);
            assertEquals(12, headerRow.getLastCellNum());
            assertEquals("Nội dung câu hỏi (*)", headerRow.getCell(1).getStringCellValue());
            assertEquals("Loại câu hỏi (*)", headerRow.getCell(2).getStringCellValue());
            assertEquals("Đáp án đúng (*)", headerRow.getCell(10).getStringCellValue());

            assertTrue(qSheet.getLastRowNum() >= 4);
        }
    }

    @Test
    void testImportQuestionsFromExcel_SuccessWithAllQuestionTypes() {
        when(quizRepository.findById(50L)).thenReturn(Optional.of(quiz));
        when(userRepository.findById(2L)).thenReturn(Optional.of(instructor));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
            Question q = invocation.getArgument(0);
            q.setId(1000L + System.nanoTime() % 1000);
            return q;
        });

        byte[] templateBytes = quizService.exportQuestionTemplate();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Mau_Cau_Hoi_Quiz_UTELearn.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                templateBytes
        );

        QuestionImportResultDTO result = quizService.importQuestionsFromExcel(50L, file, 2L);

        assertNotNull(result);
        assertEquals(4, result.getTotalRows());
        assertEquals(4, result.getImportedCount());
        assertEquals(0, result.getFailedCount());
        assertTrue(result.getErrors().isEmpty());
        assertEquals(4, result.getQuestions().size());

        verify(questionRepository, times(4)).save(any(Question.class));
    }

    @Test
    void testImportQuestionsFromExcel_HandlesInvalidRowsGracefully() throws Exception {
        when(quizRepository.findById(50L)).thenReturn(Optional.of(quiz));
        when(userRepository.findById(2L)).thenReturn(Optional.of(instructor));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
            Question q = invocation.getArgument(0);
            q.setId(500L);
            return q;
        });

        byte[] excelBytes;
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Danh_Sach_Cau_Hoi");
            Row header = sheet.createRow(0);
            header.createCell(1).setCellValue("Nội dung");
            header.createCell(2).setCellValue("Loại");
            header.createCell(4).setCellValue("Lựa chọn A");
            header.createCell(5).setCellValue("Lựa chọn B");
            header.createCell(10).setCellValue("Đáp án đúng");

            Row validRow = sheet.createRow(1);
            validRow.createCell(1).setCellValue("Câu hỏi hợp lệ 1?");
            validRow.createCell(2).setCellValue("SINGLE_CHOICE");
            validRow.createCell(4).setCellValue("Đáp án 1");
            validRow.createCell(5).setCellValue("Đáp án 2");
            validRow.createCell(10).setCellValue("A");

            Row missingContentRow = sheet.createRow(2);
            missingContentRow.createCell(1).setCellValue("");
            missingContentRow.createCell(2).setCellValue("SINGLE_CHOICE");
            missingContentRow.createCell(4).setCellValue("A");
            missingContentRow.createCell(5).setCellValue("B");
            missingContentRow.createCell(10).setCellValue("A");

            Row missingCorrectRow = sheet.createRow(3);
            missingCorrectRow.createCell(1).setCellValue("Câu hỏi thiếu đáp án đúng?");
            missingCorrectRow.createCell(2).setCellValue("SINGLE_CHOICE");
            missingCorrectRow.createCell(4).setCellValue("A");
            missingCorrectRow.createCell(5).setCellValue("B");
            missingCorrectRow.createCell(10).setCellValue("");

            wb.write(out);
            excelBytes = out.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invalid_test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelBytes
        );

        QuestionImportResultDTO result = quizService.importQuestionsFromExcel(50L, file, 2L);

        assertNotNull(result);
        assertEquals(3, result.getTotalRows());
        assertEquals(1, result.getImportedCount());
        assertEquals(2, result.getFailedCount());
        assertEquals(2, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("Dòng 3:"));
        assertTrue(result.getErrors().get(1).contains("Dòng 4:"));
    }

    @Test
    void testImportQuestionsFromExcel_UnauthorizedThrowsException() {
        when(quizRepository.findById(50L)).thenReturn(Optional.of(quiz));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3}
        );

        assertThrows(RuntimeException.class, () -> quizService.importQuestionsFromExcel(50L, file, 99L));
    }

    @Test
    void testExportQuizQuestions_Success() throws Exception {
        when(quizRepository.findById(50L)).thenReturn(Optional.of(quiz));
        when(userRepository.findById(2L)).thenReturn(Optional.of(instructor));

        Question q1 = Question.builder()
                .id(101L)
                .quiz(quiz)
                .content("Câu hỏi xuất trắc nghiệm?")
                .questionType("SINGLE_CHOICE")
                .points(new BigDecimal("2.0"))
                .options("[{\"id\":\"opt_1\",\"text\":\"Đáp án A\",\"isCorrect\":true},{\"id\":\"opt_2\",\"text\":\"Đáp án B\",\"isCorrect\":false}]")
                .explanation("Giải thích 1")
                .build();

        Question q2 = Question.builder()
                .id(102L)
                .quiz(quiz)
                .content("Câu hỏi xuất điền từ?")
                .questionType("FILL_BLANK")
                .points(new BigDecimal("1.0"))
                .options("[{\"id\":\"opt_1\",\"text\":\"kế thừa\",\"isCorrect\":true},{\"id\":\"opt_2\",\"text\":\"ke thua\",\"isCorrect\":true}]")
                .explanation("Giải thích 2")
                .build();

        when(questionRepository.findByQuizIdOrderByOrderIndexAscCreatedAtAsc(50L)).thenReturn(List.of(q1, q2));

        byte[] bytes = quizService.exportQuizQuestions(50L, 2L);

        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes);
             Workbook workbook = WorkbookFactory.create(in)) {
            assertEquals(2, workbook.getNumberOfSheets());
            Sheet qSheet = workbook.getSheet("Danh_Sach_Cau_Hoi");
            assertNotNull(qSheet);

            assertEquals(2, qSheet.getLastRowNum());
            Row row1 = qSheet.getRow(1);
            assertEquals("1", row1.getCell(0).getStringCellValue());
            assertEquals("Câu hỏi xuất trắc nghiệm?", row1.getCell(1).getStringCellValue());
            assertEquals("SINGLE_CHOICE", row1.getCell(2).getStringCellValue());
            assertEquals("2", row1.getCell(3).getStringCellValue());
            assertEquals("Đáp án A", row1.getCell(4).getStringCellValue());
            assertEquals("Đáp án B", row1.getCell(5).getStringCellValue());
            assertEquals("A", row1.getCell(10).getStringCellValue());

            Row row2 = qSheet.getRow(2);
            assertEquals("2", row2.getCell(0).getStringCellValue());
            assertEquals("Câu hỏi xuất điền từ?", row2.getCell(1).getStringCellValue());
            assertEquals("FILL_BLANK", row2.getCell(2).getStringCellValue());
            assertEquals("kế thừa, ke thua", row2.getCell(10).getStringCellValue());
        }
    }

    @Test
    void testExportQuizQuestions_UnauthorizedThrowsException() {
        when(quizRepository.findById(50L)).thenReturn(Optional.of(quiz));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> quizService.exportQuizQuestions(50L, 99L));
    }

    @Test
    void testReorderQuestions_Success() {
        lesson.setStatus("PUBLISHED");
        course.setStatus("PUBLISHED");
        Question q1 = Question.builder().id(101L).quiz(quiz).content("Cau 1").orderIndex(1).options("[]").build();
        Question q2 = Question.builder().id(102L).quiz(quiz).content("Cau 2").orderIndex(2).options("[]").build();
        Question q3 = Question.builder().id(103L).quiz(quiz).content("Cau 3").orderIndex(3).options("[]").build();

        when(quizRepository.findById(50L)).thenReturn(Optional.of(quiz));
        when(questionRepository.findByQuizIdOrderByOrderIndexAscCreatedAtAsc(50L)).thenReturn(List.of(q1, q2, q3));

        quizService.reorderQuestions(50L, List.of(103L, 101L, 102L), 2L);

        assertEquals(1, q3.getOrderIndex());
        assertEquals(2, q1.getOrderIndex());
        assertEquals(3, q2.getOrderIndex());
        assertEquals("PUBLISHED", lesson.getStatus());
        assertEquals("PUBLISHED", course.getStatus());
        verify(questionRepository, times(3)).save(any(Question.class));
        verify(lessonRepository, never()).save(any(Lesson.class));
        verify(courseRepository, never()).save(any(Course.class));
        verify(cohortRepository, never()).save(any(Cohort.class));
    }

    @Test
    void testUpdateQuiz_OnlyAllowReviewOrShuffle_DoesNotRevertToDraft() {
        lesson.setStatus("PUBLISHED");
        course.setStatus("PUBLISHED");

        when(userRepository.findById(2L)).thenReturn(Optional.of(instructor));
        when(quizRepository.findById(50L)).thenReturn(Optional.of(quiz));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionRepository.findByQuizIdOrderByOrderIndexAscCreatedAtAsc(50L)).thenReturn(Collections.emptyList());

        QuizRequestDTO request = QuizRequestDTO.builder()
                .title(quiz.getTitle())
                .durationMinutes(quiz.getDurationMinutes())
                .passingScore(quiz.getPassingScore())
                .maxAttempts(quiz.getMaxAttempts())
                .shuffleQuestions(true)
                .allowReview(false)
                .build();

        QuizDTO updated = quizService.updateQuiz(50L, request, 2L);

        assertNotNull(updated);
        assertFalse(updated.getAllowReview());
        assertTrue(updated.getShuffleQuestions());
        assertEquals("PUBLISHED", lesson.getStatus());
        assertEquals("PUBLISHED", course.getStatus());
        verify(lessonRepository, never()).save(any(Lesson.class));
        verify(courseRepository, never()).save(any(Course.class));
        verify(cohortRepository, never()).save(any(Cohort.class));
    }

    @Test
    void testReorderQuestions_UnauthorizedThrowsException() {
        when(quizRepository.findById(50L)).thenReturn(Optional.of(quiz));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> quizService.reorderQuestions(50L, List.of(101L, 102L), 99L));
    }

    @Test
    void testReorderQuestions_EmptyListDoesNothing() {
        when(quizRepository.findById(50L)).thenReturn(Optional.of(quiz));

        quizService.reorderQuestions(50L, Collections.emptyList(), 2L);

        verify(questionRepository, never()).save(any(Question.class));
    }

    @Test
    void testUpdateQuestion_OnlyExplanation_DoesNotRevertToDraft() {
        lesson.setStatus("PUBLISHED");
        course.setStatus("PUBLISHED");

        Question existing = Question.builder()
                .id(101L)
                .quiz(quiz)
                .content("Cau hoi goc")
                .questionType("SINGLE_CHOICE")
                .points(new BigDecimal("1.0"))
                .options("[]")
                .explanation("Giai thich cu")
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(instructor));
        when(questionRepository.findById(101L)).thenReturn(Optional.of(existing));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuestionRequestDTO request = QuestionRequestDTO.builder()
                .content("Cau hoi goc")
                .questionType("SINGLE_CHOICE")
                .points(new BigDecimal("1.0"))
                .explanation("Giai thich moi chi tiet hon")
                .options(Collections.emptyList())
                .build();

        QuestionDTO updated = quizService.updateQuestion(101L, request, 2L);

        assertNotNull(updated);
        assertEquals("Giai thich moi chi tiet hon", updated.getExplanation());
        assertEquals("PUBLISHED", lesson.getStatus());
        assertEquals("PUBLISHED", course.getStatus());
        verify(lessonRepository, never()).save(any(Lesson.class));
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void testUpdateQuiz_IncreaseMaxAttempts_DoesNotRevertToDraft() {
        lesson.setStatus("PUBLISHED");
        course.setStatus("PUBLISHED");
        quiz.setMaxAttempts(1);

        when(userRepository.findById(2L)).thenReturn(Optional.of(instructor));
        when(quizRepository.findById(50L)).thenReturn(Optional.of(quiz));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionRepository.findByQuizIdOrderByOrderIndexAscCreatedAtAsc(50L)).thenReturn(Collections.emptyList());

        QuizRequestDTO request = QuizRequestDTO.builder()
                .title(quiz.getTitle())
                .durationMinutes(quiz.getDurationMinutes())
                .passingScore(quiz.getPassingScore())
                .maxAttempts(3)
                .build();

        QuizDTO updated = quizService.updateQuiz(50L, request, 2L);

        assertNotNull(updated);
        assertEquals(3, updated.getMaxAttempts());
        assertEquals("PUBLISHED", lesson.getStatus());
        assertEquals("PUBLISHED", course.getStatus());
        verify(lessonRepository, never()).save(any(Lesson.class));
        verify(courseRepository, never()).save(any(Course.class));
    }
}

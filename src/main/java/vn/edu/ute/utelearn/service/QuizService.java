package vn.edu.ute.utelearn.service;

import org.springframework.web.multipart.MultipartFile;
import vn.edu.ute.utelearn.dto.*;

import java.util.List;
import java.util.Map;

public interface QuizService {
    QuizDTO getQuizByLessonId(Long lessonId, Long cohortId, Long userId);
    QuizDTO getQuizByIdForAttempt(Long quizId, Long cohortId, Long userId);
    QuizDTO getOrCreateQuizForLesson(Long lessonId, Long instructorId);
    QuizDTO updateQuiz(Long quizId, QuizRequestDTO request, Long instructorId);
    List<QuestionDTO> getQuestionsByQuizId(Long quizId, boolean revealAnswers, Long instructorId);
    QuestionDTO createQuestion(Long quizId, QuestionRequestDTO request, Long instructorId);
    QuestionDTO updateQuestion(Long questionId, QuestionRequestDTO request, Long instructorId);
    void deleteQuestion(Long questionId, Long instructorId);
    QuizDTO startQuiz(Long quizId, Long cohortId, Long userId);
    Map<String, Object> saveQuizProgress(Long quizId, QuizSubmissionRequestDTO request, Long userId);
    QuizResultDTO submitQuiz(Long quizId, QuizSubmissionRequestDTO request, Long userId);
    QuizResultDTO getSubmissionDetail(Long submissionId, Long userId);
    List<QuizResultDTO> getSubmissionsForQuiz(Long quizId, Long cohortId, Long userId);
    byte[] exportQuestionTemplate();
    byte[] exportQuizQuestions(Long quizId, Long instructorId);
    QuestionImportResultDTO importQuestionsFromExcel(Long quizId, MultipartFile file, Long instructorId);
    void reorderQuestions(Long quizId, List<Long> questionIds, Long instructorId);
    List<QuizResultDTO> getSubmissionsForInstructor(Long quizId, Long cohortId, Long instructorId);
}


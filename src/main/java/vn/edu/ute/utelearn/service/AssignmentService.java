package vn.edu.ute.utelearn.service;

import org.springframework.web.multipart.MultipartFile;
import vn.edu.ute.utelearn.dto.AssignmentDTO;
import vn.edu.ute.utelearn.dto.AssignmentGradeDTO;
import vn.edu.ute.utelearn.dto.AssignmentRequestDTO;
import vn.edu.ute.utelearn.dto.AssignmentSubmissionDTO;

import java.util.List;

public interface AssignmentService {
    AssignmentDTO getOrCreateAssignmentForLesson(Long lessonId, Long instructorId);
    AssignmentDTO getAssignmentByLessonId(Long lessonId, Long cohortId, Long userId);
    AssignmentDTO updateAssignment(Long assignmentId, AssignmentRequestDTO request, Long instructorId);
    AssignmentSubmissionDTO submitAssignment(Long assignmentId, Long cohortId, String submissionText, MultipartFile file, Long userId);
    List<AssignmentSubmissionDTO> getSubmissionsForInstructor(Long assignmentId, Long cohortId, Long instructorId);
    AssignmentSubmissionDTO gradeSubmission(Long submissionId, AssignmentGradeDTO request, Long instructorId);
    String uploadAssignmentAttachment(MultipartFile file, Long instructorId);
    void deleteStudentSubmission(Long assignmentId, Long cohortId, Long userId);
}

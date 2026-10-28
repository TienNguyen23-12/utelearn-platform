package vn.edu.ute.utelearn.service;

import org.springframework.web.multipart.MultipartFile;
import vn.edu.ute.utelearn.dto.*;

public interface GradebookService {
    GradebookDTO getGradebook(Long cohortId, Long instructorId);
    void calculateAndSaveGrades(Long cohortId, Long instructorId);
    void publishGrades(Long cohortId, Long instructorId);
    StudentGradeItemDTO getStudentGrade(Long cohortId, Long userId);
    void syncStudentGrade(Long cohortId, Long userId);
    byte[] exportGradebookToExcel(Long cohortId, Long instructorId);
    GradebookImportResultDTO importGradebookFromExcel(Long cohortId, MultipartFile file, Long instructorId);

    CourseGradebookDTO getCourseGradebook(Long courseId, Long instructorId);
    void calculateAndSyncCourseGradebook(Long courseId, Long instructorId);
    byte[] exportCourseGradebookToExcel(Long courseId, Long instructorId);
    StudentCourseGradeDTO getStudentCourseGrade(Long courseId, Long userId);
}

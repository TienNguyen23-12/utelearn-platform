package vn.edu.ute.utelearn.service;

import vn.edu.ute.utelearn.entity.Lesson;
import vn.edu.ute.utelearn.dto.LessonRequestDTO;

import java.util.List;

public interface LessonService {
    List<Lesson> getLessonsBySectionId(Long sectionId);
    Lesson getLessonById(Long lessonId);
    Lesson createLesson(Long sectionId, LessonRequestDTO request, Long instructorId);
    Lesson updateLesson(Long lessonId, LessonRequestDTO request, Long instructorId);
    void deleteLesson(Long lessonId, Long instructorId);
}

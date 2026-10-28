package vn.edu.ute.utelearn.service;

import vn.edu.ute.utelearn.dto.LessonNoteRequestDTO;
import vn.edu.ute.utelearn.dto.LessonNoteResponseDTO;

import java.util.List;

public interface LessonNoteService {
    List<LessonNoteResponseDTO> getNotesForLesson(Long lessonId, Long userId);
    LessonNoteResponseDTO createNote(LessonNoteRequestDTO request, Long userId);
    LessonNoteResponseDTO updateNote(Long noteId, LessonNoteRequestDTO request, Long userId);
    void deleteNote(Long noteId, Long userId);
}

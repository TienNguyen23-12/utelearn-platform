package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.LessonNote;

import java.util.List;

@Repository
public interface LessonNoteRepository extends JpaRepository<LessonNote, Long> {

    @Query("SELECT n FROM LessonNote n WHERE n.lesson.id = :lessonId AND n.user.id = :userId ORDER BY n.timestampSeconds ASC NULLS LAST, n.createdAt ASC")
    List<LessonNote> findByLessonIdAndUserIdOrderByTimeline(@Param("lessonId") Long lessonId, @Param("userId") Long userId);

    List<LessonNote> findByLessonIdAndUserId(Long lessonId, Long userId);
}

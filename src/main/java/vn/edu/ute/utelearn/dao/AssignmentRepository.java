package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.Assignment;

import java.util.Optional;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    Optional<Assignment> findByLessonId(Long lessonId);
    boolean existsByLessonId(Long lessonId);
    void deleteByLessonId(Long lessonId);
    java.util.List<Assignment> findByAttachmentUrl(String attachmentUrl);
}

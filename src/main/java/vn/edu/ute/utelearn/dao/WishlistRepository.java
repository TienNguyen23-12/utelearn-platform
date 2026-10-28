package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.Course;
import vn.edu.ute.utelearn.entity.User;
import vn.edu.ute.utelearn.entity.Wishlist;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    boolean existsByUserAndCourse(User user, Course course);

    boolean existsByUser_IdAndCourse_Id(Long userId, Long courseId);

    Optional<Wishlist> findByUserAndCourse(User user, Course course);

    @Query("SELECT w FROM Wishlist w JOIN FETCH w.course c LEFT JOIN FETCH c.createdBy WHERE w.user.id = :userId ORDER BY w.createdAt DESC")
    List<Wishlist> findByUserIdWithCourse(@Param("userId") Long userId);

    long countByUser_Id(Long userId);

    void deleteByUserAndCourse(User user, Course course);
}

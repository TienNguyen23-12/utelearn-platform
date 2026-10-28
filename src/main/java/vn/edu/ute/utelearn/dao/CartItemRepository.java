package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.utelearn.entity.CartItem;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Query("SELECT c FROM CartItem c LEFT JOIN FETCH c.course LEFT JOIN FETCH c.cohort WHERE c.user.id = :userId ORDER BY c.createdAt DESC")
    List<CartItem> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT c FROM CartItem c WHERE c.user.id = :userId AND c.course.id = :courseId")
    Optional<CartItem> findByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Query("SELECT c FROM CartItem c WHERE c.user.id = :userId AND c.cohort.id = :cohortId")
    Optional<CartItem> findByUserIdAndCohortId(@Param("userId") Long userId, @Param("cohortId") Long cohortId);

    @Query("SELECT COUNT(c) > 0 FROM CartItem c WHERE c.user.id = :userId AND c.course.id = :courseId")
    boolean existsByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Query("SELECT COUNT(c) > 0 FROM CartItem c WHERE c.user.id = :userId AND c.cohort.id = :cohortId")
    boolean existsByUserIdAndCohortId(@Param("userId") Long userId, @Param("cohortId") Long cohortId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem c WHERE c.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(c) FROM CartItem c WHERE c.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
}

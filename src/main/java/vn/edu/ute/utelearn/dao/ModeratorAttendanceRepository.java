package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.ModeratorAttendance;
import vn.edu.ute.utelearn.entity.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ModeratorAttendanceRepository extends JpaRepository<ModeratorAttendance, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"moderator"})
    Optional<ModeratorAttendance> findByModeratorAndWorkDate(User moderator, LocalDate workDate);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"moderator"})
    List<ModeratorAttendance> findByModeratorOrderByWorkDateDesc(User moderator);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"moderator"})
    List<ModeratorAttendance> findAllByOrderByWorkDateDesc();
    
    long countByWorkDateBetween(LocalDate start, LocalDate end);
}

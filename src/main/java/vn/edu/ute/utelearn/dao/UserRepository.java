package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameOrEmail(String username, String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    
    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE r.code IN :roleCodes")
    long countByRolesCodeIn(@org.springframework.data.repository.query.Param("roleCodes") java.util.Collection<String> roleCodes);

    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE r.code IN :roleCodes AND u.createdAt BETWEEN :start AND :end")
    long countByRolesCodeInAndCreatedAtBetween(@org.springframework.data.repository.query.Param("roleCodes") java.util.Collection<String> roleCodes, @org.springframework.data.repository.query.Param("start") java.time.Instant start, @org.springframework.data.repository.query.Param("end") java.time.Instant end);
}

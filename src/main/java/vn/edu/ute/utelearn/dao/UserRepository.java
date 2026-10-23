package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"roles"})
    Optional<User> findByUsername(String username);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"roles"})
    Optional<User> findByEmail(String email);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"roles"})
    Optional<User> findByUsernameOrEmail(String username, String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    
    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE r.code IN :roleCodes")
    long countByRolesCodeIn(@org.springframework.data.repository.query.Param("roleCodes") java.util.Collection<String> roleCodes);

    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE r.code IN :roleCodes AND u.createdAt BETWEEN :start AND :end")
    long countByRolesCodeInAndCreatedAtBetween(@org.springframework.data.repository.query.Param("roleCodes") java.util.Collection<String> roleCodes, @org.springframework.data.repository.query.Param("start") java.time.Instant start, @org.springframework.data.repository.query.Param("end") java.time.Instant end);

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE UPPER(r.code) = 'MODERATOR' AND (u.isActive IS NULL OR u.isActive = true)")
    java.util.List<User> findActiveModerators();

    @Query("SELECT u FROM User u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%'))")
    java.util.List<User> searchUsers(@org.springframework.data.repository.query.Param("query") String query, org.springframework.data.domain.Pageable pageable);
}

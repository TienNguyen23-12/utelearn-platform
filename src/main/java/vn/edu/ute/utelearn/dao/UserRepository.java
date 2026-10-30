package vn.edu.ute.utelearn.dao;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.User;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findByUsernameOrEmail(String username, String email);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT DISTINCT u FROM User u ORDER BY u.id ASC")
    List<User> findAll();

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE r.code IN :roleCodes")
    long countByRolesCodeIn(@Param("roleCodes") Collection<String> roleCodes);

    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE r.code IN :roleCodes AND u.createdAt BETWEEN :start AND :end")
    long countByRolesCodeInAndCreatedAtBetween(@Param("roleCodes") Collection<String> roleCodes, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE UPPER(r.code) = 'MODERATOR' AND (u.isActive IS NULL OR u.isActive = true)")
    List<User> findActiveModerators();

    @Query("SELECT u FROM User u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR CAST(function('unaccent', LOWER(u.fullName)) AS String) LIKE CAST(function('unaccent', LOWER(CONCAT('%', :query, '%'))) AS String)")
    List<User> searchUsers(@Param("query") String query, Pageable pageable);
}

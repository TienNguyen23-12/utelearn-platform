package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.Role;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    @EntityGraph(attributePaths = {"permissions"})
    Optional<Role> findByCode(String code);

    @EntityGraph(attributePaths = {"permissions"})
    Optional<Role> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"permissions"})
    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT r FROM Role r ORDER BY r.id ASC")
    java.util.List<Role> findAll();
}


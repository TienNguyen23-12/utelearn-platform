package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.PayrollConfig;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollConfigRepository extends JpaRepository<PayrollConfig, Long> {

    Optional<PayrollConfig> findFirstByIsGlobalTrue();

    @EntityGraph(attributePaths = {"user"})
    Optional<PayrollConfig> findByUser_Id(Long userId);

    @EntityGraph(attributePaths = {"user"})
    List<PayrollConfig> findByIsGlobalFalse();
}


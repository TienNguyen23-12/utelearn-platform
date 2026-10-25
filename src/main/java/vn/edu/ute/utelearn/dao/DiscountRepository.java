package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.Discount;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Long> {

    Optional<Discount> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<Discount> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query("UPDATE Discount d SET d.usedCount = COALESCE(d.usedCount, 0) + 1 WHERE LOWER(d.code) = LOWER(:code) AND (d.usageLimit IS NULL OR COALESCE(d.usedCount, 0) < d.usageLimit)")
    int incrementUsageAtomic(@Param("code") String code);
}

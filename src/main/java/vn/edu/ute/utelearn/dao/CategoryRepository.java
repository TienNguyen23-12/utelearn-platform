package vn.edu.ute.utelearn.dao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.utelearn.entity.Category;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    @EntityGraph(attributePaths = {"parent"})
    List<Category> findByParentIsNull();

    @EntityGraph(attributePaths = {"parent"})
    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @EntityGraph(attributePaths = {"parent"})
    List<Category> findByParentId(Long parentId);

    long countByParentId(Long parentId);

    @EntityGraph(attributePaths = {"parent"})
    List<Category> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"parent"})
    Optional<Category> findById(Long id);
}

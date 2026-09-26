package in.sellzon.catalog.category.repository;

import in.sellzon.catalog.category.entity.Category;
import in.sellzon.catalog.category.enums.CategoryLevel;
import in.sellzon.catalog.common.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findByIdAndStatus(UUID id, Status status);

    List<Category> findByParentIsNullAndStatusOrderByDisplayOrderAscNameAsc(Status status);

    List<Category> findByParentIdAndStatusOrderByDisplayOrderAscNameAsc(UUID parentId, Status status);

    boolean existsByParentIdAndNameIgnoreCase(UUID parentId, String name);

    boolean existsByParentIdAndSlug(UUID parentId, String slug);

    boolean existsByParentIdAndNameIgnoreCaseAndIdNot(UUID parentId, String name, UUID id);

    boolean existsByParentIdAndSlugAndIdNot(UUID parentId, String slug, UUID id);

    boolean existsByParentId(UUID parentId);

    List<Category> findByLevelAndStatusOrderByDisplayOrderAscNameAsc(CategoryLevel level, Status status);

    boolean existsByParentIsNullAndNameIgnoreCase(String name);

    boolean existsByParentIsNullAndSlug(String slug);

    boolean existsByParentIsNullAndNameIgnoreCaseAndIdNot(String name, UUID id);

    boolean existsByParentIsNullAndSlugAndIdNot(String slug, UUID id);

    Optional<Category> findTopByParentAndStatusOrderByDisplayOrderDesc(Category parent, Status status);

    Optional<Category> findTopByParentIsNullAndStatusOrderByDisplayOrderDesc(Status status);

    long countByParentAndStatus(Category parent, Status status);

    long countByParentIsNullAndStatus(Status status);

    @Query(value = """
            WITH RECURSIVE category_tree AS (
                SELECT id
                FROM categories
                WHERE id = :categoryId
                AND status = 'ACTIVE'

                UNION ALL

                SELECT c.id
                FROM categories c
                INNER JOIN category_tree ct
                    ON c.parent_id = ct.id
                WHERE c.status = 'ACTIVE'
            )
            SELECT id
            FROM category_tree
            """, nativeQuery = true)
    List<UUID> findActiveCategoryAndDescendantIds(
            @Param("categoryId") UUID categoryId
    );
}

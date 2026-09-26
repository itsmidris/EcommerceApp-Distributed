package in.sellzon.catalog.product.repository;

import in.sellzon.catalog.common.enums.Status;
import in.sellzon.catalog.product.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByMarketplaceProductId(String marketplaceProductId);
    Optional<Product> findByMarketplaceProductIdAndStatus(String marketplaceProductId, Status status);
    boolean existsByMarketplaceProductId(String marketplaceProductId);
    boolean existsBySellerIdAndSku(UUID sellerId, String sku);
    List<Product> findBySellerIdAndStatus(UUID sellerId, Status status);
    List<Product> findByCategoryIdAndStatus(UUID categoryId, Status status);
    long countBySellerIdAndStatus(UUID sellerId, Status status);
    List<Product> findBySellerIdOrderByCreatedAtDesc(UUID sellerId);

    @Query("""
            select p from Product p
            where p.status = :status
                        and (:query = ''
                                    or lower(p.title) like lower(concat('%', :query, '%'))
                                                or lower(p.brand) like lower(concat('%', :query, '%')))""")
    Page<Product> searchPublic(
            @Param("query") String query,
            @Param("status") Status status,
            Pageable pageable
    );

    @Query("""
            select p from Product p
            where p.status = :status
                        and p.category.id in :categoryIds
                                    and (
                                                :query = ''
                                                            or lower(p.title) like lower(concat('%', :query, '%'))
                                                                        or lower(p.brand) like lower(concat('%', :query, '%')))""")
    Page<Product> searchPublicByCategories(
            @Param("query") String query,
            @Param("categoryIds") List<UUID> categoryIds,
            @Param("status") Status status,
            Pageable pageable
    );

    Optional<Product> findByMarketplaceProductIdAndSellerId(
            String marketplaceProductId,
            UUID sellerId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select p from Product p
        where p.marketplaceProductId = :marketplaceProductId
        and p.sellerId = :sellerId
    """)
    Optional<Product> findByMarketplaceProductIdAndSellerIdForUpdate(
            @Param("marketplaceProductId") String marketplaceProductId,
            @Param("sellerId") UUID sellerId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from Product p
            where p.id = :productId""")
    Optional<Product> findByIdForUpdate(@Param("productId") UUID productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from Product p
            where p.marketplaceProductId = :marketplaceProductId
                    and p.status = :status""")
    Optional<Product> findByMarketplaceProductIdAndStatusForUpdate(
            @Param("marketplaceProductId") String marketplaceProductId,
            @Param("status") Status status
    );
}

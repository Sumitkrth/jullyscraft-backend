package com.jullyscraft.repository;

import com.jullyscraft.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository
        extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlugAndDeletedFalse(String slug);

    boolean existsBySlugAndDeletedFalse(String slug);
    boolean existsBySlugAndIdNotAndDeletedFalse(String slug, Long id);

    // For DB fallback search
    Page<Product> findByDeletedFalseAndNameContainingIgnoreCaseOrDeletedFalseAndBrandContainingIgnoreCase(
            String name, String brand, Pageable pageable);

    // Public browsing — active, not deleted
    Page<Product> findByActiveTrueAndDeletedFalse(Pageable pageable);

    // By category
    Page<Product> findByCategoryIdAndActiveTrueAndDeletedFalse(Long categoryId, Pageable pageable);

    // Featured
    Page<Product> findByFeaturedTrueAndActiveTrueAndDeletedFalse(Pageable pageable);

    // Admin — all including inactive
    Page<Product> findByDeletedFalse(Pageable pageable);

    // Full-text search on name / description
    @Query("""
        SELECT p FROM Product p
        WHERE p.deleted = false
          AND p.active  = true
          AND (LOWER(p.name)             LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(p.shortDescription) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(p.brand)            LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(p.tags)             LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<Product> search(String q, Pageable pageable);

    // Update rating after review saved
    @Modifying
    @Query("""
        UPDATE Product p
        SET p.averageRating = :rating, p.reviewCount = :count
        WHERE p.id = :id
        """)
    void updateRating(Long id, double rating, int count);

    // Stock update
    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :qty WHERE p.id = :id AND p.stockQuantity >= :qty")
    int decrementStock(Long id, int qty);

    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity + :qty WHERE p.id = :id")
    void incrementStock(Long id, int qty);


}
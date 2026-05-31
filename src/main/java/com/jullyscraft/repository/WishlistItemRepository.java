package com.jullyscraft.repository;

import com.jullyscraft.entity.WishlistItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    @Query("""
        SELECT w FROM WishlistItem w
        JOIN FETCH w.product p
        WHERE w.user.id = :userId
          AND w.deleted = false
        """)
    Page<WishlistItem> findByUserIdWithProduct(Long userId, Pageable pageable);

    Optional<WishlistItem> findByUserIdAndProductIdAndDeletedFalse(Long userId, Long productId);

    boolean existsByUserIdAndProductIdAndDeletedFalse(Long userId, Long productId);

    long countByUserIdAndDeletedFalse(Long userId);
}
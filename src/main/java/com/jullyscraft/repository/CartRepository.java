package com.jullyscraft.repository;

import com.jullyscraft.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    // Fetch cart with items + products eagerly (avoids N+1)
    @Query("""
        SELECT DISTINCT c FROM Cart c
        LEFT JOIN FETCH c.items i
        LEFT JOIN FETCH i.product p
        LEFT JOIN FETCH i.variant v
        WHERE c.user.id = :userId
          AND (i IS NULL OR i.deleted = false)
        """)
    Optional<Cart> findByUserIdWithItems(Long userId);
}
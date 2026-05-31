package com.jullyscraft.repository;

import com.jullyscraft.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByIdAndCartIdAndDeletedFalse(Long id, Long cartId);

    @Query("""
        SELECT ci FROM CartItem ci
        WHERE ci.cart.id = :cartId
          AND ci.product.id = :productId
          AND ci.deleted = false
          AND (:variantId IS NULL AND ci.variant IS NULL
               OR ci.variant.id = :variantId)
        """)
    Optional<CartItem> findExisting(Long cartId, Long productId, Long variantId);

    @Modifying
    @Query("UPDATE CartItem ci SET ci.deleted = true WHERE ci.cart.id = :cartId")
    void clearCart(Long cartId);

    long countByCartIdAndDeletedFalse(Long cartId);
}
package com.jullyscraft.repository;

import com.jullyscraft.entity.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCodeAndDeletedFalse(String code);

    boolean existsByCodeAndDeletedFalse(String code);
    boolean existsByCodeAndIdNotAndDeletedFalse(String code, Long id);

    Page<Coupon> findByDeletedFalse(Pageable pageable);

    Page<Coupon> findByActiveTrueAndDeletedFalse(Pageable pageable);

    // Flash sales active right now
    @Query("""
        SELECT c FROM Coupon c
        WHERE c.flashSale   = true
          AND c.active      = true
          AND c.deleted     = false
          AND c.startsAt   <= :now
          AND c.expiresAt  >= :now
        ORDER BY c.expiresAt ASC
        """)
    List<Coupon> findActiveFlashSales(LocalDateTime now);

    // Coupons expiring soon — for cleanup jobs
    @Query("""
        SELECT c FROM Coupon c
        WHERE c.active    = true
          AND c.deleted   = false
          AND c.expiresAt < :threshold
        """)
    List<Coupon> findExpiredCoupons(LocalDateTime threshold);

    @Modifying
    @Query("UPDATE Coupon c SET c.active = false WHERE c.expiresAt < :now AND c.active = true")
    int deactivateExpired(LocalDateTime now);

    @Modifying
    @Query("UPDATE Coupon c SET c.usageCount = c.usageCount + 1 WHERE c.id = :id")
    void incrementUsage(Long id);

    @Modifying
    @Query("UPDATE Coupon c SET c.usageCount = c.usageCount - 1 WHERE c.id = :id AND c.usageCount > 0")
    void decrementUsage(Long id);
}
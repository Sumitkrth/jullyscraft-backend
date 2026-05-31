package com.jullyscraft.repository;

import com.jullyscraft.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    long countByCouponIdAndUserId(Long couponId, Long userId);

    boolean existsByCouponIdAndOrderId(Long couponId, Long orderId);

    Optional<CouponUsage> findByCouponIdAndOrderId(Long couponId, Long orderId);

    @Query("SELECT COALESCE(SUM(cu.discountApplied), 0) FROM CouponUsage cu WHERE cu.coupon.id = :couponId")
    java.math.BigDecimal sumDiscountApplied(Long couponId);
}
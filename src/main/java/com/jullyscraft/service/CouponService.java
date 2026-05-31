package com.jullyscraft.service;

import com.jullyscraft.dto.request.ApplyCouponRequest;
import com.jullyscraft.dto.request.CreateCouponRequest;
import com.jullyscraft.dto.response.ApplyCouponResponse;
import com.jullyscraft.dto.response.CouponResponse;
import com.jullyscraft.dto.response.PageResponse;
import com.jullyscraft.entity.Coupon;
import com.jullyscraft.entity.CouponUsage;
import com.jullyscraft.entity.Order;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService {

    // ── Public ────────────────────────────────────────────────────────────────
    ApplyCouponResponse  validateAndApply(Long userId, ApplyCouponRequest request);
    List<CouponResponse> getActiveFlashSales();

    // ── Order lifecycle hooks ─────────────────────────────────────────────────
    void recordUsage(Coupon coupon, Order order, Long userId,
                     BigDecimal discountApplied);
    void releaseUsage(Long orderId);

    // ── Admin ─────────────────────────────────────────────────────────────────
    PageResponse<CouponResponse> getAll(int page, int size);
    CouponResponse               getById(Long id);
    CouponResponse               create(CreateCouponRequest request);
    CouponResponse               update(Long id, CreateCouponRequest request);
    void                         delete(Long id);
    CouponResponse               toggleActive(Long id);
    void                         deactivateExpired();
}
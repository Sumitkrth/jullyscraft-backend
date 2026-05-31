package com.jullyscraft.dto.response;

import com.jullyscraft.entity.Coupon;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ApplyCouponResponse {
    private String              couponCode;
    private Coupon.DiscountType discountType;
    private BigDecimal          originalAmount;
    private BigDecimal          discountAmount;
    private BigDecimal          finalAmount;
    private String              message;

    // BUY_X_GET_Y fields
    private Integer             freeQuantity;
    private Long                freeProductId;
}
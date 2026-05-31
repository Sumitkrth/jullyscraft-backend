package com.jullyscraft.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jullyscraft.entity.Coupon;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CouponResponse {
    private Long                 id;
    private String               code;
    private String               description;
    private Coupon.DiscountType  discountType;
    private BigDecimal           discountValue;
    private BigDecimal           maxDiscountAmount;
    private BigDecimal           minOrderAmount;
    private Integer              buyQuantity;
    private Integer              getQuantity;
    private LocalDateTime        startsAt;
    private LocalDateTime        expiresAt;
    private boolean              active;
    private boolean              flashSale;
    private Integer              maxUsageTotal;
    private Integer              maxUsagePerUser;
    private int                  usageCount;
    private boolean              expired;
    private Long                 applicableCategoryId;
    private LocalDateTime        createdAt;
}
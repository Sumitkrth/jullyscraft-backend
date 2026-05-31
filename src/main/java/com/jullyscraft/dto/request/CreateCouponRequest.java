package com.jullyscraft.dto.request;

import com.jullyscraft.entity.Coupon;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Builder
@Getter @Setter
public class CreateCouponRequest {

    @NotBlank(message = "Coupon code is required")
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[A-Z0-9_-]+$",
            message = "Code must be uppercase letters, digits, hyphens or underscores only")
    private String code;

    @NotBlank(message = "Description is required")
    @Size(max = 200)
    private String description;

    @NotNull(message = "Discount type is required")
    private Coupon.DiscountType discountType;

    @DecimalMin(value = "0.01")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal discountValue;

    @DecimalMin(value = "0.01")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal maxDiscountAmount;

    // BUY_X_GET_Y fields
    @Min(1) private Integer buyQuantity;
    @Min(1) private Integer getQuantity;
    private Long buyProductId;
    private Long getProductId;

    @DecimalMin(value = "0.00")
    @Builder.Default
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    private Long applicableCategoryId;

    @NotNull(message = "Start date is required")
    private LocalDateTime startsAt;

    @NotNull(message = "Expiry date is required")
    private LocalDateTime expiresAt;

    private boolean active     = true;
    private boolean flashSale  = false;

    @Min(1) private Integer maxUsageTotal;
    @Min(1) private Integer maxUsagePerUser;

    private Set<Long> allowedUserIds;     // empty = public
}
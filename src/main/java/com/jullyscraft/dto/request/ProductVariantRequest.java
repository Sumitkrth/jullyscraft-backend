package com.jullyscraft.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class ProductVariantRequest {

    @NotBlank(message = "SKU is required")
    @Size(max = 100)
    private String sku;

    @Size(max = 80)
    private String size;

    @Size(max = 80)
    private String color;

    @Size(max = 80)
    private String material;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal price;

    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    private BigDecimal discountPrice;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stockQuantity;

    @Min(value = 1)
    private int lowStockThreshold = 5;

    private boolean active = true;
}
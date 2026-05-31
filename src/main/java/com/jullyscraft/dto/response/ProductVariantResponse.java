package com.jullyscraft.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class ProductVariantResponse {
    private Long       id;
    private String     sku;
    private String     size;
    private String     color;
    private String     material;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private int        stockQuantity;
    private int        lowStockThreshold;
    private boolean    active;
    private boolean    inStock;
    private boolean    lowStock;
}
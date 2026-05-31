package com.jullyscraft.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class InventoryReportResponse {

    private long               totalSkus;
    private long               inStockSkus;
    private long               outOfStockSkus;
    private long               lowStockSkus;
    private BigDecimal         totalInventoryValue;

    private List<StockAlert>   outOfStockProducts;
    private List<StockAlert>   lowStockProducts;

    @Getter
    @Builder
    public static class StockAlert {
        private Long   productId;
        private String productName;
        private String sku;
        private int    currentStock;
        private int    threshold;
        private String categoryName;
    }
}
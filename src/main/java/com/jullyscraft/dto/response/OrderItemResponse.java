package com.jullyscraft.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class OrderItemResponse {
    private Long       id;
    private Long       productId;
    private String     productName;
    private String     productSku;
    private String     variantInfo;
    private String     productImageUrl;
    private int        quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}
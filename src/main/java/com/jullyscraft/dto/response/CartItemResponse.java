package com.jullyscraft.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartItemResponse {
    private Long       id;
    private Long       productId;
    private String     productName;
    private String     productSlug;
    private String     primaryImageUrl;
    private Long       variantId;
    private String     variantSku;
    private String     variantSize;
    private String     variantColor;
    private int        quantity;
    private BigDecimal priceSnapshot;
    private BigDecimal lineTotal;
    private boolean    inStock;
    private int        availableStock;
}
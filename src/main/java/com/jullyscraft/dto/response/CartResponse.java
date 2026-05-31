package com.jullyscraft.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
public class CartResponse {
    private Long                   cartId;
    private List<CartItemResponse> items;
    private int                    totalItems;
    private BigDecimal             subtotal;
}
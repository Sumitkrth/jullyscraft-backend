package com.jullyscraft.service;

import com.jullyscraft.dto.request.AddToCartRequest;
import com.jullyscraft.dto.request.UpdateCartItemRequest;
import com.jullyscraft.dto.response.CartResponse;

public interface CartService {
    CartResponse getCart(Long userId);
    CartResponse addItem(Long userId, AddToCartRequest request);
    CartResponse updateItem(Long userId, Long cartItemId, UpdateCartItemRequest request);
    CartResponse removeItem(Long userId, Long cartItemId);
    void         clearCart(Long userId);
    int          getCartItemCount(Long userId);
}
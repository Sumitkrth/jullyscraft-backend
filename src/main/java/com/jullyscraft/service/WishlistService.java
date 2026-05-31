package com.jullyscraft.service;

import com.jullyscraft.dto.request.AddToWishlistRequest;
import com.jullyscraft.dto.response.PageResponse;
import com.jullyscraft.dto.response.WishlistItemResponse;

public interface WishlistService {
    PageResponse<WishlistItemResponse> getWishlist(Long userId, int page, int size);
    WishlistItemResponse               addItem(Long userId, AddToWishlistRequest request);
    void                               removeItem(Long userId, Long wishlistItemId);
    void                               removeByProduct(Long userId, Long productId);
    boolean                            isInWishlist(Long userId, Long productId);
    long                               getWishlistCount(Long userId);
    void                               moveToCart(Long userId, Long productId);
}
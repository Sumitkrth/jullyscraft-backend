package com.jullyscraft.service.impl;

import com.jullyscraft.dto.request.AddToCartRequest;
import com.jullyscraft.dto.request.AddToWishlistRequest;
import com.jullyscraft.dto.response.PageResponse;
import com.jullyscraft.dto.response.WishlistItemResponse;
import com.jullyscraft.entity.User;
import com.jullyscraft.entity.UserProductInteraction;
import com.jullyscraft.entity.WishlistItem;
import com.jullyscraft.exception.BadRequestException;
import com.jullyscraft.exception.DuplicateResourceException;
import com.jullyscraft.exception.ResourceNotFoundException;
import com.jullyscraft.mapper.WishlistMapper;
import com.jullyscraft.repository.*;
import com.jullyscraft.service.CartService;
import com.jullyscraft.service.RecommendationService;
import com.jullyscraft.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class WishlistServiceImpl implements WishlistService {

    private final WishlistItemRepository wishlistRepository;
    private final ProductRepository      productRepository;
    private final UserRepository         userRepository;
    private final WishlistMapper         wishlistMapper;
    private final CartService            cartService;
    private final RecommendationService     recommendationService;

    // @Lazy breaks circular dependency (CartService → WishlistService → CartService)
    public WishlistServiceImpl(
            WishlistItemRepository wishlistRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            WishlistMapper wishlistMapper,
            RecommendationService recommendationService,
            @Lazy CartService cartService) {

        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.wishlistMapper = wishlistMapper;
        this.recommendationService = recommendationService;
        this.cartService = cartService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WishlistItemResponse> getWishlist(Long userId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.of(
                wishlistRepository.findByUserIdWithProduct(userId, pageable)
                        .map(wishlistMapper::toResponse));
    }

    @Override
    @Transactional
    public WishlistItemResponse addItem(Long userId, AddToWishlistRequest req) {
        if (wishlistRepository.existsByUserIdAndProductIdAndDeletedFalse(
                userId, req.getProductId())) {
            throw new DuplicateResourceException("Product is already in your wishlist");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        var product = productRepository.findById(req.getProductId())
                .filter(p -> !p.isDeleted() && p.isActive())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product", "id", req.getProductId()));

        WishlistItem item = WishlistItem.builder()
                .user(user)
                .product(product)
                .build();

        WishlistItem saved = wishlistRepository.save(item);

        recommendationService.recordInteraction(
                userId,
                req.getProductId(),
                UserProductInteraction.InteractionType.WISHLIST,
                null);

        return wishlistMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void removeItem(Long userId, Long wishlistItemId) {
        WishlistItem item = wishlistRepository.findById(wishlistItemId)
                .filter(w -> w.getUser().getId().equals(userId) && !w.isDeleted())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Wishlist item", "id", wishlistItemId));
        item.softDelete();
        wishlistRepository.save(item);
    }

    @Override
    @Transactional
    public void removeByProduct(Long userId, Long productId) {
        wishlistRepository
                .findByUserIdAndProductIdAndDeletedFalse(userId, productId)
                .ifPresent(item -> {
                    item.softDelete();
                    wishlistRepository.save(item);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInWishlist(Long userId, Long productId) {
        return wishlistRepository.existsByUserIdAndProductIdAndDeletedFalse(userId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getWishlistCount(Long userId) {
        return wishlistRepository.countByUserIdAndDeletedFalse(userId);
    }

    @Override
    @Transactional
    public void moveToCart(Long userId, Long productId) {
        WishlistItem item = wishlistRepository
                .findByUserIdAndProductIdAndDeletedFalse(userId, productId)
                .orElseThrow(() ->
                        new BadRequestException("Product not found in your wishlist"));

        AddToCartRequest cartReq = new AddToCartRequest();
        cartReq.setProductId(productId);
        cartReq.setQuantity(1);

        cartService.addItem(userId, cartReq);

        item.softDelete();
        wishlistRepository.save(item);
        log.info("Moved product {} from wishlist to cart for user {}", productId, userId);
    }
}
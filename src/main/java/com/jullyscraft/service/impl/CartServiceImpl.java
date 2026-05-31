package com.jullyscraft.service.impl;

import com.jullyscraft.dto.request.AddToCartRequest;
import com.jullyscraft.dto.request.UpdateCartItemRequest;
import com.jullyscraft.dto.response.CartResponse;
import com.jullyscraft.entity.*;
import com.jullyscraft.exception.BadRequestException;
import com.jullyscraft.exception.ResourceNotFoundException;
import com.jullyscraft.mapper.CartMapper;
import com.jullyscraft.repository.*;
import com.jullyscraft.service.AnalyticsService;
import com.jullyscraft.service.CartService;
import com.jullyscraft.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository        cartRepository;
    private final CartItemRepository    cartItemRepository;
    private final ProductRepository     productRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository        userRepository;
    private final CartMapper            cartMapper;
    private final RecommendationService recommendationService;
    private final AnalyticsService      analyticsService;

    private static final int MAX_CART_ITEMS = 50;

    // ── Get ───────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public int getCartItemCount(Long userId) {
        return cartRepository.findByUserId(userId)
                .map(c -> (int) cartItemRepository.countByCartIdAndDeletedFalse(c.getId()))
                .orElse(0);
    }

    // ── Add ───────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CartResponse addItem(Long userId, AddToCartRequest req) {
        Cart cart = getOrCreateCart(userId);

        // Enforce cart item limit
        long currentCount = cartItemRepository.countByCartIdAndDeletedFalse(cart.getId());
        if (currentCount >= MAX_CART_ITEMS) {
            throw new BadRequestException(
                    "Cart cannot contain more than " + MAX_CART_ITEMS + " items");
        }

        Product product = findActiveProduct(req.getProductId());
        ProductVariant variant = resolveVariant(req.getVariantId(), product);

        // Stock check
        int available = variant != null
                ? variant.getStockQuantity()
                : product.getStockQuantity();

        if (available < req.getQuantity()) {
            throw new BadRequestException(
                    "Only " + available + " unit(s) available in stock");
        }

        // If same item already in cart — increase quantity
        cartItemRepository.findExisting(
                        cart.getId(), req.getProductId(), req.getVariantId())
                .ifPresentOrElse(existing -> {
                    int newQty = existing.getQuantity() + req.getQuantity();
                    if (newQty > 100) throw new BadRequestException("Max 100 units per item");
                    if (newQty > available) {
                        throw new BadRequestException(
                                "Only " + available + " unit(s) available");
                    }
                    existing.setQuantity(newQty);
                    cartItemRepository.save(existing);
                }, () -> {
                    BigDecimal price = resolvePrice(product, variant);
                    CartItem item = CartItem.builder()
                            .product(product)
                            .variant(variant)
                            .quantity(req.getQuantity())
                            .priceSnapshot(price)
                            .build();
                    cart.addItem(item);
                });

        cartRepository.save(cart);
        analyticsService.track(userId, null,
                AnalyticsEvent.EventType.ADD_TO_CART,
                req.getProductId(), null, null, null);
        log.debug("Item added to cart — user: {}, product: {}", userId, product.getName());

        recommendationService.recordInteraction(
                userId,
                req.getProductId(),
                UserProductInteraction.InteractionType.CART_ADD,
                null);

        return cartMapper.toResponse(
                cartRepository.findByUserIdWithItems(userId).orElse(cart));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CartResponse updateItem(Long userId, Long cartItemId,
                                   UpdateCartItemRequest req) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository
                .findByIdAndCartIdAndDeletedFalse(cartItemId, cart.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", "id", cartItemId));

        int available = item.getVariant() != null
                ? item.getVariant().getStockQuantity()
                : item.getProduct().getStockQuantity();

        if (req.getQuantity() > available) {
            throw new BadRequestException("Only " + available + " unit(s) available");
        }

        item.setQuantity(req.getQuantity());
        cartItemRepository.save(item);

        return cartMapper.toResponse(
                cartRepository.findByUserIdWithItems(userId).orElse(cart));
    }

    // ── Remove ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CartResponse removeItem(Long userId, Long cartItemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository
                .findByIdAndCartIdAndDeletedFalse(cartItemId, cart.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", "id", cartItemId));

        item.softDelete();
        cartItemRepository.save(item);

        return cartMapper.toResponse(
                cartRepository.findByUserIdWithItems(userId).orElse(cart));
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        cartRepository.findByUserId(userId)
                .ifPresent(cart -> cartItemRepository.clearCart(cart.getId()));
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException("User", "id", userId));
                    Cart cart = Cart.builder().user(user).build();
                    return cartRepository.save(cart);
                });
    }

    private Product findActiveProduct(Long productId) {
        return productRepository.findById(productId)
                .filter(p -> !p.isDeleted() && p.isActive())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product", "id", productId));
    }

    private ProductVariant resolveVariant(Long variantId, Product product) {
        if (variantId == null) return null;
        return product.getVariants().stream()
                .filter(v -> v.getId().equals(variantId)
                        && !v.isDeleted() && v.isActive())
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException("Variant", "id", variantId));
    }

    private BigDecimal resolvePrice(Product product, ProductVariant variant) {
        if (variant != null) {
            if (variant.getDiscountPrice() != null) return variant.getDiscountPrice();
            if (variant.getPrice()         != null) return variant.getPrice();
        }
        return product.getEffectivePrice();
    }
}
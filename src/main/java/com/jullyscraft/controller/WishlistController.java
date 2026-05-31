package com.jullyscraft.controller;

import com.jullyscraft.dto.request.AddToWishlistRequest;
import com.jullyscraft.dto.response.ApiResponse;
import com.jullyscraft.dto.response.PageResponse;
import com.jullyscraft.dto.response.WishlistItemResponse;
import com.jullyscraft.security.userdetails.UserPrincipal;
import com.jullyscraft.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Wishlist", description = "Wishlist management")
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    @Operation(summary = "Get my wishlist")
    public ResponseEntity<ApiResponse<PageResponse<WishlistItemResponse>>> getWishlist(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                wishlistService.getWishlist(principal.getId(), page, size)));
    }

    @GetMapping("/count")
    @Operation(summary = "Get wishlist count")
    public ResponseEntity<ApiResponse<Long>> getCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                wishlistService.getWishlistCount(principal.getId())));
    }

    @GetMapping("/check/{productId}")
    @Operation(summary = "Check if product is in wishlist")
    public ResponseEntity<ApiResponse<Boolean>> check(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(
                wishlistService.isInWishlist(principal.getId(), productId)));
    }

    @PostMapping
    @Operation(summary = "Add product to wishlist")
    public ResponseEntity<ApiResponse<WishlistItemResponse>> addItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddToWishlistRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Added to wishlist",
                wishlistService.addItem(principal.getId(), request)));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove item from wishlist by item ID")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long itemId) {
        wishlistService.removeItem(principal.getId(), itemId);
        return ResponseEntity.ok(ApiResponse.success("Removed from wishlist"));
    }

    @DeleteMapping("/products/{productId}")
    @Operation(summary = "Remove product from wishlist by product ID")
    public ResponseEntity<ApiResponse<Void>> removeByProduct(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long productId) {
        wishlistService.removeByProduct(principal.getId(), productId);
        return ResponseEntity.ok(ApiResponse.success("Removed from wishlist"));
    }

    @PostMapping("/products/{productId}/move-to-cart")
    @Operation(summary = "Move wishlist item to cart")
    public ResponseEntity<ApiResponse<Void>> moveToCart(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long productId) {
        wishlistService.moveToCart(principal.getId(), productId);
        return ResponseEntity.ok(ApiResponse.success("Moved to cart"));
    }
}
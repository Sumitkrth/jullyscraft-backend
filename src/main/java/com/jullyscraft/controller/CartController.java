package com.jullyscraft.controller;

import com.jullyscraft.dto.request.AddToCartRequest;
import com.jullyscraft.dto.request.UpdateCartItemRequest;
import com.jullyscraft.dto.response.ApiResponse;
import com.jullyscraft.dto.response.CartResponse;
import com.jullyscraft.security.userdetails.UserPrincipal;
import com.jullyscraft.service.CartService;
import com.jullyscraft.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AppConstants.CART_BASE)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cart", description = "Shopping cart management")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get my cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                ApiResponse.success(cartService.getCart(principal.getId())));
    }

    @GetMapping("/count")
    @Operation(summary = "Get cart item count (for badge)")
    public ResponseEntity<ApiResponse<Integer>> getCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                ApiResponse.success(cartService.getCartItemCount(principal.getId())));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Item added to cart",
                cartService.addItem(principal.getId(), request)));
    }

    @PatchMapping("/items/{cartItemId}")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cart updated",
                cartService.updateItem(principal.getId(), cartItemId, request)));
    }

    @DeleteMapping("/items/{cartItemId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long cartItemId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Item removed",
                cartService.removeItem(principal.getId(), cartItemId)));
    }

    @DeleteMapping
    @Operation(summary = "Clear entire cart")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @AuthenticationPrincipal UserPrincipal principal) {
        cartService.clearCart(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Cart cleared"));
    }
}
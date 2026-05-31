package com.jullyscraft.mapper;

import com.jullyscraft.dto.response.CartItemResponse;
import com.jullyscraft.dto.response.CartResponse;
import com.jullyscraft.entity.Cart;
import com.jullyscraft.entity.CartItem;
import com.jullyscraft.entity.ProductImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(target = "totalItems", expression = "java(cart.getTotalItems())")
    @Mapping(target = "subtotal",   expression = "java(cart.getSubtotal())")
    CartResponse toResponse(Cart cart);

    @Mapping(target = "productId",      source = "product.id")
    @Mapping(target = "productName",    source = "product.name")
    @Mapping(target = "productSlug",    source = "product.slug")
    @Mapping(target = "primaryImageUrl",expression = "java(extractImage(item))")
    @Mapping(target = "variantId",      source = "variant.id")
    @Mapping(target = "variantSku",     source = "variant.sku")
    @Mapping(target = "variantSize",    source = "variant.size")
    @Mapping(target = "variantColor",   source = "variant.color")
    @Mapping(target = "lineTotal",      expression = "java(item.getLineTotal())")
    @Mapping(target = "inStock",        expression = "java(resolveStock(item) > 0)")
    @Mapping(target = "availableStock", expression = "java(resolveStock(item))")
    CartItemResponse toItemResponse(CartItem item);

    default String extractImage(CartItem item) {
        return item.getProduct().getImages().stream()
                .filter(ProductImage::isPrimaryImage)
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(item.getProduct().getImages().isEmpty()
                        ? null
                        : item.getProduct().getImages().get(0).getImageUrl());
    }

    default int resolveStock(CartItem item) {
        return item.getVariant() != null
                ? item.getVariant().getStockQuantity()
                : item.getProduct().getStockQuantity();
    }
}
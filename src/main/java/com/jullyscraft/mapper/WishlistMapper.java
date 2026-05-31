package com.jullyscraft.mapper;

import com.jullyscraft.dto.response.WishlistItemResponse;
import com.jullyscraft.entity.ProductImage;
import com.jullyscraft.entity.WishlistItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WishlistMapper {

    @Mapping(target = "productId",      source = "product.id")
    @Mapping(target = "productName",    source = "product.name")
    @Mapping(target = "productSlug",    source = "product.slug")
    @Mapping(target = "primaryImageUrl",expression = "java(extractImage(item))")
    @Mapping(target = "price",          source = "product.price")
    @Mapping(target = "discountPrice",  source = "product.discountPrice")
    @Mapping(target = "discountPercent",expression = "java(item.getProduct().getDiscountPercent())")
    @Mapping(target = "inStock",        expression = "java(item.getProduct().isInStock())")
    @Mapping(target = "averageRating",  source = "product.averageRating")
    @Mapping(target = "addedAt",        source = "createdAt")
    WishlistItemResponse toResponse(WishlistItem item);

    default String extractImage(WishlistItem item) {
        return item.getProduct().getImages().stream()
                .filter(ProductImage::isPrimaryImage)
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(item.getProduct().getImages().isEmpty()
                        ? null
                        : item.getProduct().getImages().get(0).getImageUrl());
    }
}
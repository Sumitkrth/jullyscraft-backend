package com.jullyscraft.mapper;

import com.jullyscraft.dto.response.ReviewResponse;
import com.jullyscraft.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "productId",  source = "product.id")
    @Mapping(target = "userId",     source = "user.id")
    @Mapping(target = "userName",   expression = "java(review.getUser().getFullName())")
    @Mapping(target = "userAvatar", source = "user.profileImageUrl")
    ReviewResponse toResponse(Review review);

    // Public response — hide moderation internals
    @Mapping(target = "productId",      source = "product.id")
    @Mapping(target = "userId",         source = "user.id")
    @Mapping(target = "userName",       expression = "java(review.getUser().getFullName())")
    @Mapping(target = "userAvatar",     source = "user.profileImageUrl")
    @Mapping(target = "moderationNote", ignore = true)
    @Mapping(target = "spamReason",     ignore = true)
    @Mapping(target = "spamFlagged",    ignore = true)
    ReviewResponse toPublicResponse(Review review);
}
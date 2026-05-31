package com.jullyscraft.mapper;

import com.jullyscraft.dto.request.CreateCouponRequest;
import com.jullyscraft.dto.response.CouponResponse;
import com.jullyscraft.entity.Coupon;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface CouponMapper {

    @Mapping(target = "expired", expression = "java(coupon.isExpired())")
    CouponResponse toResponse(Coupon coupon);

    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "usageCount",   ignore = true)
    @Mapping(target = "allowedUsers", ignore = true)
    @Mapping(target = "deleted",      ignore = true)
    @Mapping(target = "deletedAt",    ignore = true)
    @Mapping(target = "createdAt",    ignore = true)
    @Mapping(target = "updatedAt",    ignore = true)
    @Mapping(target = "createdBy",    ignore = true)
    @Mapping(target = "updatedBy",    ignore = true)
    Coupon toEntity(CreateCouponRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "usageCount",   ignore = true)
    @Mapping(target = "allowedUsers", ignore = true)
    @Mapping(target = "deleted",      ignore = true)
    @Mapping(target = "deletedAt",    ignore = true)
    @Mapping(target = "createdAt",    ignore = true)
    @Mapping(target = "updatedAt",    ignore = true)
    @Mapping(target = "createdBy",    ignore = true)
    @Mapping(target = "updatedBy",    ignore = true)
    void updateFromRequest(CreateCouponRequest request, @MappingTarget Coupon coupon);
}
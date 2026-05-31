package com.jullyscraft.mapper;

import com.jullyscraft.dto.request.CategoryRequest;
import com.jullyscraft.dto.response.CategoryResponse;
import com.jullyscraft.dto.response.CategoryTreeResponse;
import com.jullyscraft.entity.Category;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    // ── Flat response (used in lists / admin) ─────────────────────────────────
    @Mapping(target = "parentId",   source = "parent.id")
    @Mapping(target = "parentName", source = "parent.name")
    CategoryResponse toResponse(Category category);

    // ── Tree response (public tree API — children recursively mapped) ─────────
    @Mapping(target = "children", expression = "java(toTreeList(category.getChildren()))")
    CategoryTreeResponse toTreeResponse(Category category);

    default List<CategoryTreeResponse> toTreeList(List<Category> children) {
        if (children == null) return List.of();
        return children.stream()
                .filter(c -> !c.isDeleted() && c.isActive())
                .map(this::toTreeResponse)
                .toList();
    }

    // ── Request → Entity ─────────────────────────────────────────────────────
    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "slug",         ignore = true) // set in service
    @Mapping(target = "parent",       ignore = true) // set in service
    @Mapping(target = "children",     ignore = true)
    @Mapping(target = "productCount", ignore = true)
    @Mapping(target = "deleted",      ignore = true)
    @Mapping(target = "deletedAt",    ignore = true)
    @Mapping(target = "createdAt",    ignore = true)
    @Mapping(target = "updatedAt",    ignore = true)
    @Mapping(target = "createdBy",    ignore = true)
    @Mapping(target = "updatedBy",    ignore = true)
    Category toEntity(CategoryRequest request);

    // ── Partial update ────────────────────────────────────────────────────────
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "slug",         ignore = true)
    @Mapping(target = "parent",       ignore = true)
    @Mapping(target = "children",     ignore = true)
    @Mapping(target = "productCount", ignore = true)
    @Mapping(target = "deleted",      ignore = true)
    @Mapping(target = "deletedAt",    ignore = true)
    @Mapping(target = "createdAt",    ignore = true)
    @Mapping(target = "updatedAt",    ignore = true)
    @Mapping(target = "createdBy",    ignore = true)
    @Mapping(target = "updatedBy",    ignore = true)
    void updateFromRequest(CategoryRequest request, @MappingTarget Category category);
}
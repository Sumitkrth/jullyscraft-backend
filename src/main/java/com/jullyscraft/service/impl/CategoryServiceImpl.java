package com.jullyscraft.service.impl;

import com.jullyscraft.dto.request.CategoryRequest;
import com.jullyscraft.dto.response.CategoryResponse;
import com.jullyscraft.dto.response.CategoryTreeResponse;
import com.jullyscraft.entity.Category;
import com.jullyscraft.exception.BadRequestException;
import com.jullyscraft.exception.DuplicateResourceException;
import com.jullyscraft.exception.ResourceNotFoundException;
import com.jullyscraft.mapper.CategoryMapper;
import com.jullyscraft.repository.CategoryRepository;
import com.jullyscraft.service.CategoryService;
import com.jullyscraft.util.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper     categoryMapper;

    // ── Public ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = AppConstants.CACHE_CATEGORIES, key = "'tree'")
    public List<CategoryTreeResponse> getCategoryTree() {
        return categoryRepository.findActiveRootCategories()
                .stream()
                .map(categoryMapper::toTreeResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = AppConstants.CACHE_CATEGORIES, key = "'featured'")
    public List<CategoryResponse> getFeaturedCategories() {
        return categoryRepository.findFeaturedCategories()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = AppConstants.CACHE_CATEGORIES, key = "'flat'")
    public List<CategoryResponse> getAllActiveFlat() {
        return categoryRepository.findAllActiveFlat()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlugAndDeletedFalse(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getChildrenByParentId(Long parentId) {
        findActiveById(parentId); // validate parent exists
        return categoryRepository.findActiveChildrenByParentId(parentId)
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAllRootCategories()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        return categoryMapper.toResponse(findById(id));
    }

    @Override
    @Transactional
    @CacheEvict(value = AppConstants.CACHE_CATEGORIES, allEntries = true)
    public CategoryResponse createCategory(CategoryRequest request) {
        validateUniqueName(request.getName(), null);

        Category category = categoryMapper.toEntity(request);
        category.setSlug(generateUniqueSlug(request.getName(), null));

        if (request.getParentId() != null) {
            Category parent = findActiveById(request.getParentId());
            category.setParent(parent);
        }

        Category saved = categoryRepository.save(category);
        log.info("Category created: {} (slug: {})", saved.getName(), saved.getSlug());
        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = AppConstants.CACHE_CATEGORIES, allEntries = true)
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = findById(id);
        validateUniqueName(request.getName(), id);

        categoryMapper.updateFromRequest(request, category);
        category.setSlug(generateUniqueSlug(request.getName(), id));

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new BadRequestException("Category cannot be its own parent");
            }
            Category parent = findActiveById(request.getParentId());
            validateNoCircularReference(id, parent);
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    @CacheEvict(value = AppConstants.CACHE_CATEGORIES, allEntries = true)
    public void deleteCategory(Long id) {
        Category category = findById(id);

        if (categoryRepository.hasChildren(id)) {
            throw new BadRequestException(
                    "Cannot delete category with subcategories. Remove subcategories first.");
        }
        if (category.getProductCount() > 0) {
            throw new BadRequestException(
                    "Cannot delete category that contains products. Move or delete products first.");
        }

        category.softDelete();
        categoryRepository.save(category);
        log.info("Category soft-deleted: {}", category.getName());
    }

    @Override
    @Transactional
    @CacheEvict(value = AppConstants.CACHE_CATEGORIES, allEntries = true)
    public CategoryResponse toggleActive(Long id) {
        Category category = findById(id);
        category.setActive(!category.isActive());
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    @CacheEvict(value = AppConstants.CACHE_CATEGORIES, allEntries = true)
    public CategoryResponse toggleFeatured(Long id) {
        Category category = findById(id);
        category.setFeatured(!category.isFeatured());
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private Category findById(Long id) {
        return categoryRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
    }

    private Category findActiveById(Long id) {
        Category c = findById(id);
        if (!c.isActive()) {
            throw new BadRequestException("Category is not active: " + id);
        }
        return c;
    }

    private void validateUniqueName(String name, Long excludeId) {
        boolean exists = excludeId == null
                ? categoryRepository.existsByNameAndDeletedFalse(name)
                : categoryRepository.existsByNameAndIdNotAndDeletedFalse(name, excludeId);
        if (exists) {
            throw new DuplicateResourceException("Category name already exists: " + name);
        }
    }

    private String generateUniqueSlug(String name, Long excludeId) {
        String base = toSlug(name);
        String slug = base;
        int suffix = 1;

        while (excludeId == null
                ? categoryRepository.existsBySlugAndDeletedFalse(slug)
                : categoryRepository.existsBySlugAndIdNotAndDeletedFalse(slug, excludeId)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }

    private static final Pattern NON_LATIN  = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private String toSlug(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return NON_LATIN
                .matcher(WHITESPACE.matcher(normalized.toLowerCase(Locale.ENGLISH)).replaceAll("-"))
                .replaceAll("")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private void validateNoCircularReference(Long categoryId, Category proposedParent) {
        Category cursor = proposedParent;
        while (cursor != null) {
            if (cursor.getId().equals(categoryId)) {
                throw new BadRequestException(
                        "Circular reference detected: a category cannot be a descendant of itself");
            }
            cursor = cursor.getParent();
        }
    }
}
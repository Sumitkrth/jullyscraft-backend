package com.jullyscraft.service.impl;

import com.jullyscraft.dto.request.ProductFilterRequest;
import com.jullyscraft.dto.request.ProductRequest;
import com.jullyscraft.dto.request.ProductVariantRequest;
import com.jullyscraft.dto.response.PageResponse;
import com.jullyscraft.dto.response.ProductResponse;
import com.jullyscraft.dto.response.ProductSummaryResponse;
import com.jullyscraft.entity.*;
import com.jullyscraft.exception.BadRequestException;
import com.jullyscraft.exception.DuplicateResourceException;
import com.jullyscraft.exception.ResourceNotFoundException;
import com.jullyscraft.mapper.ProductMapper;
import com.jullyscraft.repository.*;
import com.jullyscraft.service.ProductService;
import com.jullyscraft.service.SearchService;
import com.jullyscraft.util.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository        productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository   imageRepository;
    private final CategoryRepository       categoryRepository;
    private final ProductMapper            productMapper;
    private final SearchService            searchService;

    // ── Public ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> getProducts(ProductFilterRequest f) {
        Pageable pageable = buildPageable(f.getPage(), f.getSize(), f.getSortBy(), f.getSortDir());
        Specification<Product> spec = buildSpec(f);
        return PageResponse.of(
                productRepository.findAll(spec, pageable).map(productMapper::toSummary));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = AppConstants.CACHE_PRODUCTS, key = "'featured:' + #page + ':' + #size")
    public PageResponse<ProductSummaryResponse> getFeaturedProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.of(
                productRepository.findByFeaturedTrueAndActiveTrueAndDeletedFalse(pageable)
                        .map(productMapper::toSummary));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> getProductsByCategory(
            Long categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.of(
                productRepository.findByCategoryIdAndActiveTrueAndDeletedFalse(categoryId, pageable)
                        .map(productMapper::toSummary));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> searchProducts(String q, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("averageRating").descending());
        return PageResponse.of(
                productRepository.search(q.trim(), pageable).map(productMapper::toSummary));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = AppConstants.CACHE_PRODUCTS, key = "#slug")
    public ProductResponse getProductBySlug(String slug) {
        return productMapper.toResponse(findActiveBySlug(slug));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return productMapper.toResponse(findActiveById(id));
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> getAllProductsAdmin(
            int page, int size, String sortBy, String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        return PageResponse.of(
                productRepository.findByDeletedFalse(pageable).map(productMapper::toSummary));
    }

    @Override
    @Transactional
    @CacheEvict(value = AppConstants.CACHE_PRODUCTS, allEntries = true)
    public ProductResponse createProduct(ProductRequest req) {
        validateSlugUnique(req.getName(), null);
        validateDiscountPrice(req.getPrice(), req.getDiscountPrice());
        validateVariantSkus(req.getVariants(), null);

        Category category = findActiveCategory(req.getCategoryId());

        Product product = Product.builder()
                .name(req.getName())
                .slug(generateUniqueSlug(req.getName(), null))
                .shortDescription(req.getShortDescription())
                .fullDescription(req.getFullDescription())
                .price(req.getPrice())
                .discountPrice(req.getDiscountPrice())
                .stockQuantity(req.getStockQuantity())
                .lowStockThreshold(req.getLowStockThreshold())
                .brand(req.getBrand())
                .tags(req.getTags())
                .metaTitle(req.getMetaTitle())
                .metaDescription(req.getMetaDescription())
                .active(req.isActive())
                .featured(req.isFeatured())
                .category(category)
                .build();

        attachImages(product, req.getImageUrls(), req.getPrimaryImageIndex());
        attachVariants(product, req.getVariants());
        attachSpecifications(product, req.getSpecifications());

        Product saved = productRepository.save(product);
        searchService.indexProduct(saved);
        category.incrementProductCount();
        categoryRepository.save(category);

        log.info("Product created: {} (slug: {})", saved.getName(), saved.getSlug());
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = AppConstants.CACHE_PRODUCTS, allEntries = true)
    public ProductResponse updateProduct(Long id, ProductRequest req) {
        Product product = findById(id);
        validateDiscountPrice(req.getPrice(), req.getDiscountPrice());
        validateVariantSkus(req.getVariants(), id);

        // Category change — update counts
        if (!product.getCategory().getId().equals(req.getCategoryId())) {
            product.getCategory().decrementProductCount();
            categoryRepository.save(product.getCategory());

            Category newCategory = findActiveCategory(req.getCategoryId());
            newCategory.incrementProductCount();
            categoryRepository.save(newCategory);
            product.setCategory(newCategory);
        }

        product.setName(req.getName());
        product.setSlug(generateUniqueSlug(req.getName(), id));
        product.setShortDescription(req.getShortDescription());
        product.setFullDescription(req.getFullDescription());
        product.setPrice(req.getPrice());
        product.setDiscountPrice(req.getDiscountPrice());
        product.setStockQuantity(req.getStockQuantity());
        product.setLowStockThreshold(req.getLowStockThreshold());
        product.setBrand(req.getBrand());
        product.setTags(req.getTags());
        product.setMetaTitle(req.getMetaTitle());
        product.setMetaDescription(req.getMetaDescription());
        product.setActive(req.isActive());
        product.setFeatured(req.isFeatured());

        // Replace images / variants / specs
        product.getImages().clear();
        product.getVariants().clear();
        product.getSpecifications().clear();

        attachImages(product, req.getImageUrls(), req.getPrimaryImageIndex());
        attachVariants(product, req.getVariants());
        attachSpecifications(product, req.getSpecifications());
        Product saved = productRepository.save(product);
        searchService.indexProduct(saved);
        return productMapper.toResponse(saved);

    }

    @Override
    @Transactional
    @CacheEvict(value = AppConstants.CACHE_PRODUCTS, allEntries = true)
    public void deleteProduct(Long id) {
        Product product = findById(id);
        product.softDelete();
        product.setActive(false);
        productRepository.save(product);
        searchService.removeProduct(id);
        product.getCategory().decrementProductCount();
        categoryRepository.save(product.getCategory());
        log.info("Product soft-deleted: {}", product.getName());
    }

    @Override
    @Transactional
    @CacheEvict(value = AppConstants.CACHE_PRODUCTS, allEntries = true)
    public void restoreProduct(Long id) {
        Product product = productRepository.findById(id)
                .filter(Product::isDeleted)
                .orElseThrow(() -> new ResourceNotFoundException("Deleted product", "id", id));
        product.restore();
        product.setActive(true);
        productRepository.save(product);

        product.getCategory().incrementProductCount();
        categoryRepository.save(product.getCategory());
        log.info("Product restored: {}", product.getName());
    }

    @Override
    @Transactional
    @CacheEvict(value = AppConstants.CACHE_PRODUCTS, allEntries = true)
    public ProductResponse toggleActive(Long id) {
        Product product = findById(id);
        product.setActive(!product.isActive());
        Product saved = productRepository.save(product);
        searchService.indexProduct(saved);
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = AppConstants.CACHE_PRODUCTS, allEntries = true)
    public ProductResponse toggleFeatured(Long id) {
        Product product = findById(id);
        product.setFeatured(!product.isFeatured());
        return productMapper.toResponse(productRepository.save(product));
    }

    // ── Specification builder (dynamic filters) ───────────────────────────────

    private Specification<Product> buildSpec(ProductFilterRequest f) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();

            predicates.add(cb.isFalse(root.get("deleted")));
            predicates.add(cb.isTrue(root.get("active")));

            if (f.getKeyword() != null && !f.getKeyword().isBlank()) {
                String pattern = "%" + f.getKeyword().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")),             pattern),
                        cb.like(cb.lower(root.get("shortDescription")), pattern),
                        cb.like(cb.lower(root.get("brand")),            pattern),
                        cb.like(cb.lower(root.get("tags")),             pattern)
                ));
            }

            if (f.getCategoryId() != null)
                predicates.add(cb.equal(root.get("category").get("id"), f.getCategoryId()));

            if (f.getBrand() != null && !f.getBrand().isBlank())
                predicates.add(cb.like(cb.lower(root.get("brand")),
                        "%" + f.getBrand().toLowerCase() + "%"));

            if (f.getMinPrice() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), f.getMinPrice()));

            if (f.getMaxPrice() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), f.getMaxPrice()));

            if (f.getMinRating() != null)
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("averageRating"), f.getMinRating()));

            if (Boolean.TRUE.equals(f.getInStock()))
                predicates.add(cb.greaterThan(root.get("stockQuantity"), 0));

            if (f.getFeatured() != null)
                predicates.add(cb.equal(root.get("featured"), f.getFeatured()));

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private void attachImages(Product product, List<String> urls, Long primaryIndex) {
        if (urls == null || urls.isEmpty()) return;
        for (int i = 0; i < urls.size(); i++) {
            ProductImage img = ProductImage.builder()
                    .imageUrl(urls.get(i))
                    .primaryImage(primaryIndex != null && primaryIndex == i)
                    .displayOrder(i)
                    .build();
            product.addImage(img);
        }
        // Ensure at least one primary
        boolean hasPrimary = product.getImages().stream().anyMatch(ProductImage::isPrimaryImage);
        if (!hasPrimary && !product.getImages().isEmpty()) {
            product.getImages().get(0).setPrimaryImage(true);
        }
    }

    private void attachVariants(Product product, List<ProductVariantRequest> requests) {
        if (requests == null || requests.isEmpty()) return;
        for (ProductVariantRequest req : requests) {
            ProductVariant variant = ProductVariant.builder()
                    .sku(req.getSku())
                    .size(req.getSize())
                    .color(req.getColor())
                    .material(req.getMaterial())
                    .price(req.getPrice())
                    .discountPrice(req.getDiscountPrice())
                    .stockQuantity(req.getStockQuantity())
                    .lowStockThreshold(req.getLowStockThreshold())
                    .active(req.isActive())
                    .build();
            product.addVariant(variant);
        }
    }

    private void attachSpecifications(Product product, List<Map<String, String>> specs) {
        if (specs == null || specs.isEmpty()) return;
        for (int i = 0; i < specs.size(); i++) {
            Map<String, String> spec = specs.get(i);
            String key   = spec.get("key");
            String value = spec.get("value");
            if (key == null || value == null) continue;
            ProductSpecification s = ProductSpecification.builder()
                    .specKey(key)
                    .specValue(value)
                    .displayOrder(i)
                    .build();
            product.addSpecification(s);
        }
    }

    private void validateDiscountPrice(BigDecimal price, BigDecimal discountPrice) {
        if (discountPrice != null && discountPrice.compareTo(price) >= 0) {
            throw new BadRequestException("Discount price must be less than the original price");
        }
    }

    private void validateVariantSkus(List<ProductVariantRequest> variants, Long productId) {
        if (variants == null) return;
        for (ProductVariantRequest v : variants) {
            boolean exists = productId == null
                    ? variantRepository.existsBySkuAndDeletedFalse(v.getSku())
                    : variantRepository.existsBySkuAndIdNotAndDeletedFalse(v.getSku(), productId);
            if (exists) {
                throw new DuplicateResourceException("SKU already exists: " + v.getSku());
            }
        }
    }

    private void validateSlugUnique(String name, Long excludeId) {
        String slug = toSlug(name);
        boolean exists = excludeId == null
                ? productRepository.existsBySlugAndDeletedFalse(slug)
                : productRepository.existsBySlugAndIdNotAndDeletedFalse(slug, excludeId);
        if (exists) {
            // Not an error — slug generation handles collision; just a pre-check log
            log.debug("Slug collision detected for '{}', will auto-suffix", slug);
        }
    }

    private Category findActiveCategory(Long id) {
        return categoryRepository.findById(id)
                .filter(c -> !c.isDeleted() && c.isActive())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
    }

    private Product findById(Long id) {
        return productRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    private Product findActiveById(Long id) {
        Product p = findById(id);
        if (!p.isActive()) throw new BadRequestException("Product is not available");
        return p;
    }

    private Product findActiveBySlug(String slug) {
        Product p = productRepository.findBySlugAndDeletedFalse(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));
        if (!p.isActive()) throw new BadRequestException("Product is not available");
        return p;
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        size = Math.min(size, AppConstants.MAX_PAGE_SIZE);
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return PageRequest.of(page, size, sort);
    }

    private String generateUniqueSlug(String name, Long excludeId) {
        String base = toSlug(name);
        String slug = base;
        int suffix  = 1;
        while (excludeId == null
                ? productRepository.existsBySlugAndDeletedFalse(slug)
                : productRepository.existsBySlugAndIdNotAndDeletedFalse(slug, excludeId)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }

    private static final Pattern NON_LATIN  = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private String toSlug(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return NON_LATIN
                .matcher(WHITESPACE
                        .matcher(normalized.toLowerCase(Locale.ENGLISH))
                        .replaceAll("-"))
                .replaceAll("")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
package com.jullyscraft.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter @Setter
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 200, message = "Name must be 3–200 characters")
    private String name;

    @NotBlank(message = "Short description is required")
    @Size(max = 500)
    private String shortDescription;

    @NotBlank(message = "Full description is required")
    private String fullDescription;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal price;

    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    private BigDecimal discountPrice;

    @NotNull(message = "Category is required")
    private Long categoryId;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0)
    private Integer stockQuantity;

    @Min(value = 1)
    private int lowStockThreshold = 5;

    @Size(max = 100)
    private String brand;

    @Size(max = 500)
    private String tags;

    @Size(max = 70)
    private String metaTitle;

    @Size(max = 160)
    private String metaDescription;

    private boolean active   = true;
    private boolean featured = false;

    @Valid
    private List<ProductVariantRequest> variants = new ArrayList<>();

    // key-value specifications e.g. {"Weight": "1kg", "Material": "Cotton"}
    private List<Map<String, String>> specifications = new ArrayList<>();

    // image URLs (after Cloudinary upload from frontend)
    private List<String> imageUrls = new ArrayList<>();

    private Long primaryImageIndex = 0L;
}
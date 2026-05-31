package com.jullyscraft.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 120, message = "Name must be 2–120 characters")
    private String name;

    @Size(max = 500, message = "Description max 500 characters")
    private String description;

    @Size(max = 500)
    private String imageUrl;

    private Long parentId;           // null = root category

    private boolean active = true;

    private boolean featured = false;

    @Min(value = 0, message = "Display order must be 0 or greater")
    private int displayOrder = 0;
}
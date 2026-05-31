package com.jullyscraft.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private Long parentId;
    private String parentName;
    private boolean active;
    private boolean featured;
    private int displayOrder;
    private long productCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
package com.jullyscraft.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryTreeResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private boolean featured;
    private int displayOrder;
    private long productCount;
    private List<CategoryTreeResponse> children;
}
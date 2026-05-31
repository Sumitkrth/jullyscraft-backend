package com.jullyscraft.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProductImageResponse {
    private Long    id;
    private String  imageUrl;
    private boolean primary;
    private int     displayOrder;
    private String  altText;
}
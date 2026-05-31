package com.jullyscraft.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class FileUploadRequest {
    private String folder;
    private String altText;
    private boolean generateThumbnail = true;
    private boolean generateWebp      = true;
}
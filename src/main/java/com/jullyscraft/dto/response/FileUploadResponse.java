package com.jullyscraft.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileUploadResponse {
    private String  publicId;
    private String  originalUrl;
    private String  thumbnailUrl;
    private String  mediumUrl;
    private String  webpUrl;
    private String  format;
    private long    bytes;
    private int     width;
    private int     height;
    private String  resourceType;
    private String  folder;
}
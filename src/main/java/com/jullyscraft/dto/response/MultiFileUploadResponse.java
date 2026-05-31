package com.jullyscraft.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MultiFileUploadResponse {
    private List<FileUploadResponse> files;
    private int                      successCount;
    private int                      failureCount;
    private List<String>             errors;
}
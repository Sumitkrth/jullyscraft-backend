package com.jullyscraft.util;

import com.jullyscraft.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Component
public class FileValidationUtil {

    @Value("${app.file.max-size-mb:10}")
    private int maxSizeMb;

    @Value("${app.file.allowed-image-types:image/jpeg,image/png,image/webp,image/gif}")
    private String allowedImageTypes;

    @Value("${app.file.allowed-doc-types:application/pdf}")
    private String allowedDocTypes;

    public void validateImage(MultipartFile file) {
        validateNotEmpty(file);
        validateSize(file);
        validateMimeType(file, allowedImageTypes);
    }

    public void validateDocument(MultipartFile file) {
        validateNotEmpty(file);
        validateSize(file);
        validateMimeType(file, allowedDocTypes);
    }

    public void validateImages(List<MultipartFile> files, int maxCount) {
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("At least one file is required");
        }
        if (files.size() > maxCount) {
            throw new BadRequestException(
                    "Maximum " + maxCount + " files allowed per upload");
        }
        files.forEach(this::validateImage);
    }

    private void validateNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File must not be empty");
        }
    }

    private void validateSize(MultipartFile file) {
        long maxBytes = (long) maxSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new BadRequestException(
                    "File size exceeds maximum allowed size of " + maxSizeMb + "MB");
        }
    }

    private void validateMimeType(MultipartFile file, String allowed) {
        List<String> allowedTypes = Arrays.asList(allowed.split(","));
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType.trim())) {
            throw new BadRequestException(
                    "File type not allowed: " + contentType
                            + ". Allowed types: " + allowed);
        }
    }

    public String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "jpg";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1)
                .toLowerCase();
    }

    public String sanitizeFilename(String filename) {
        if (filename == null) return "file";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_{2,}", "_");
    }
}
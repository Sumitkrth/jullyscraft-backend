package com.jullyscraft.service;

import com.jullyscraft.dto.response.FileUploadResponse;
import com.jullyscraft.dto.response.MultiFileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {

    // ── Single upload ─────────────────────────────────────────────────────────
    FileUploadResponse uploadProductImage(MultipartFile file, Long productId);
    FileUploadResponse uploadCategoryImage(MultipartFile file, Long categoryId);
    FileUploadResponse uploadProfileImage(MultipartFile file, Long userId);
    FileUploadResponse uploadDocument(MultipartFile file, String folder);

    // ── Bulk upload ───────────────────────────────────────────────────────────
    MultiFileUploadResponse uploadProductImages(List<MultipartFile> files,
                                                Long productId);

    // ── Delete ────────────────────────────────────────────────────────────────
    void deleteFile(String publicId);
    void deleteFiles(List<String> publicIds);

    // ── URL generation ────────────────────────────────────────────────────────
    String getThumbnailUrl(String publicId);
    String getMediumUrl(String publicId);
    String getOriginalUrl(String publicId);
    String getWebpUrl(String publicId);
}
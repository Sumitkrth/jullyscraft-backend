package com.jullyscraft.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.jullyscraft.dto.response.FileUploadResponse;
import com.jullyscraft.dto.response.MultiFileUploadResponse;
import com.jullyscraft.exception.BadRequestException;
import com.jullyscraft.service.FileStorageService;
import com.jullyscraft.util.FileValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryStorageServiceImpl implements FileStorageService {

    private final Cloudinary         cloudinary;
    private final FileValidationUtil fileValidationUtil;

    @Value("${app.cloudinary.cloud-name}")
    private String cloudName;

    @Value("${app.file.folders.products:jullyscraft/products}")
    private String productsFolder;

    @Value("${app.file.folders.profiles:jullyscraft/profiles}")
    private String profilesFolder;

    @Value("${app.file.folders.categories:jullyscraft/categories}")
    private String categoriesFolder;

    @Value("${app.file.thumbnail-width:300}")
    private int thumbnailWidth;

    @Value("${app.file.thumbnail-height:300}")
    private int thumbnailHeight;

    @Value("${app.file.medium-width:800}")
    private int mediumWidth;

    @Value("${app.file.medium-height:800}")
    private int mediumHeight;

    // ── Product image ─────────────────────────────────────────────────────────

    @Override
    public FileUploadResponse uploadProductImage(MultipartFile file, Long productId) {
        fileValidationUtil.validateImage(file);
        String folder    = productsFolder + "/" + productId;
        String publicId  = folder + "/" + generatePublicId();
        return upload(file, publicId, folder);
    }

    @Override
    public MultiFileUploadResponse uploadProductImages(List<MultipartFile> files,
                                                       Long productId) {
        fileValidationUtil.validateImages(files, 10);

        List<FileUploadResponse> results = new ArrayList<>();
        List<String>             errors  = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                results.add(uploadProductImage(file, productId));
            } catch (Exception e) {
                errors.add(file.getOriginalFilename() + ": " + e.getMessage());
                log.warn("Failed to upload file {}: {}",
                        file.getOriginalFilename(), e.getMessage());
            }
        }

        return MultiFileUploadResponse.builder()
                .files(results)
                .successCount(results.size())
                .failureCount(errors.size())
                .errors(errors)
                .build();
    }

    // ── Category image ────────────────────────────────────────────────────────

    @Override
    public FileUploadResponse uploadCategoryImage(MultipartFile file, Long categoryId) {
        fileValidationUtil.validateImage(file);
        String folder   = categoriesFolder + "/" + categoryId;
        String publicId = folder + "/" + generatePublicId();
        return upload(file, publicId, folder);
    }

    // ── Profile image ─────────────────────────────────────────────────────────

    @Override
    public FileUploadResponse uploadProfileImage(MultipartFile file, Long userId) {
        fileValidationUtil.validateImage(file);
        String folder   = profilesFolder + "/" + userId;
        String publicId = folder + "/avatar";   // single avatar per user
        return upload(file, publicId, folder);
    }

    // ── Document upload ───────────────────────────────────────────────────────

    @Override
    public FileUploadResponse uploadDocument(MultipartFile file, String folder) {
        fileValidationUtil.validateDocument(file);
        String publicId = folder + "/" + generatePublicId();
        return uploadRaw(file, publicId, folder);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Override
    @Async("notificationExecutor")
    public void deleteFile(String publicId) {
        if (publicId == null || publicId.isBlank()) return;
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Cloudinary file deleted: {}", publicId);
        } catch (Exception e) {
            log.error("Cloudinary delete failed for {}: {}", publicId, e.getMessage());
        }
    }

    @Override
    @Async("notificationExecutor")
    public void deleteFiles(List<String> publicIds) {
        if (publicIds == null || publicIds.isEmpty()) return;
        publicIds.forEach(this::deleteFile);
    }

    // ── URL generation ────────────────────────────────────────────────────────

    @Override
    public String getThumbnailUrl(String publicId) {
        return cloudinary.url()
                .transformation(new Transformation()
                        .width(thumbnailWidth)
                        .height(thumbnailHeight)
                        .crop("fill")
                        .gravity("auto")
                        .fetchFormat("auto")
                        .quality("auto"))
                .secure(true)
                .generate(publicId);
    }

    @Override
    public String getMediumUrl(String publicId) {
        return cloudinary.url()
                .transformation(new Transformation()
                        .width(mediumWidth)
                        .height(mediumHeight)
                        .crop("limit")
                        .fetchFormat("auto")
                        .quality("auto:good"))
                .secure(true)
                .generate(publicId);
    }

    @Override
    public String getOriginalUrl(String publicId) {
        return cloudinary.url()
                .transformation(new Transformation()
                        .fetchFormat("auto")
                        .quality("auto"))
                .secure(true)
                .generate(publicId);
    }

    @Override
    public String getWebpUrl(String publicId) {
        return cloudinary.url()
                .transformation(new Transformation()
                        .fetchFormat("webp")
                        .quality("auto"))
                .secure(true)
                .generate(publicId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private FileUploadResponse upload(MultipartFile file,
                                      String publicId,
                                      String folder) {
        try {
            Map<String, Object> params = ObjectUtils.asMap(
                    "public_id",      publicId,
                    "folder",         folder,
                    "resource_type",  "image",
                    "overwrite",      true,
                    "invalidate",     true,

                    // Auto format (serves WebP to supporting browsers)
                    "fetch_format",   "auto",

                    // Auto quality compression
                    "quality",        "auto",

                    // Eager transformations — generate thumbnail + medium on upload
                    "eager", List.of(
                            // Thumbnail
                            new Transformation()
                                    .width(thumbnailWidth)
                                    .height(thumbnailHeight)
                                    .crop("fill")
                                    .gravity("auto")
                                    .fetchFormat("auto")
                                    .quality("auto"),
                            // Medium
                            new Transformation()
                                    .width(mediumWidth)
                                    .height(mediumHeight)
                                    .crop("limit")
                                    .fetchFormat("auto")
                                    .quality("auto:good")
                    ),
                    "eager_async", false,

                    // Metadata
                    "tags", List.of("jullyscraft")
            );

            Map<String, Object> result =
                    cloudinary.uploader().upload(file.getBytes(), params);

            String uploadedPublicId = (String) result.get("public_id");
            String secureUrl        = (String) result.get("secure_url");
            String format           = (String) result.get("format");
            long   bytes            = result.containsKey("bytes")
                    ? ((Number) result.get("bytes")).longValue() : 0;
            int    width            = result.containsKey("width")
                    ? ((Number) result.get("width")).intValue() : 0;
            int    height           = result.containsKey("height")
                    ? ((Number) result.get("height")).intValue() : 0;

            log.info("Cloudinary upload success: {} ({} bytes)", uploadedPublicId, bytes);

            return FileUploadResponse.builder()
                    .publicId(uploadedPublicId)
                    .originalUrl(secureUrl)
                    .thumbnailUrl(getThumbnailUrl(uploadedPublicId))
                    .mediumUrl(getMediumUrl(uploadedPublicId))
                    .webpUrl(getWebpUrl(uploadedPublicId))
                    .format(format)
                    .bytes(bytes)
                    .width(width)
                    .height(height)
                    .resourceType("image")
                    .folder(folder)
                    .build();

        } catch (IOException e) {
            log.error("Cloudinary upload failed: {}", e.getMessage());
            throw new BadRequestException("File upload failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private FileUploadResponse uploadRaw(MultipartFile file,
                                         String publicId,
                                         String folder) {
        try {
            Map<String, Object> params = ObjectUtils.asMap(
                    "public_id",     publicId,
                    "folder",        folder,
                    "resource_type", "raw",
                    "overwrite",     true
            );

            Map<String, Object> result =
                    cloudinary.uploader().upload(file.getBytes(), params);

            return FileUploadResponse.builder()
                    .publicId((String) result.get("public_id"))
                    .originalUrl((String) result.get("secure_url"))
                    .format((String) result.get("format"))
                    .bytes(result.containsKey("bytes")
                            ? ((Number) result.get("bytes")).longValue() : 0)
                    .resourceType("raw")
                    .folder(folder)
                    .build();

        } catch (IOException e) {
            log.error("Cloudinary raw upload failed: {}", e.getMessage());
            throw new BadRequestException("File upload failed: " + e.getMessage());
        }
    }

    private String generatePublicId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
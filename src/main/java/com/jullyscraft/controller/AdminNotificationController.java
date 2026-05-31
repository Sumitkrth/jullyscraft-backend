package com.jullyscraft.controller;

import com.jullyscraft.dto.response.ApiResponse;
import com.jullyscraft.dto.response.PageResponse;
import com.jullyscraft.entity.NotificationLog;
import com.jullyscraft.repository.NotificationLogRepository;
import com.jullyscraft.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(AppConstants.ADMIN_BASE + "/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin — Notifications", description = "Notification delivery logs")
public class AdminNotificationController {

    private final NotificationLogRepository logRepository;

    @GetMapping
    @Operation(summary = "Get all notification logs")
    public ResponseEntity<ApiResponse<PageResponse<NotificationLog>>> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.of(logRepository.findAll(pageable))));
    }

    @GetMapping("/failed")
    @Operation(summary = "Get failed notifications")
    public ResponseEntity<ApiResponse<PageResponse<NotificationLog>>> getFailed(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.of(logRepository.findByStatus(
                        NotificationLog.DeliveryStatus.FAILED, pageable))));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get notifications for a specific user")
    public ResponseEntity<ApiResponse<PageResponse<NotificationLog>>> getByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.of(logRepository.findByUserId(userId, pageable))));
    }
}
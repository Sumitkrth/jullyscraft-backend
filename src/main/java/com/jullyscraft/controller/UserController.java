package com.jullyscraft.controller;

import com.jullyscraft.dto.request.AddressRequest;
import com.jullyscraft.dto.request.ChangePasswordRequest;
import com.jullyscraft.dto.request.UpdateProfileRequest;
import com.jullyscraft.dto.response.AddressResponse;
import com.jullyscraft.dto.response.ApiResponse;
import com.jullyscraft.dto.response.UserResponse;
import com.jullyscraft.security.userdetails.UserPrincipal;
import com.jullyscraft.service.UserService;
import com.jullyscraft.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(AppConstants.USER_BASE)
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User", description = "Profile, password, and address management")
public class UserController {

    private final UserService userService;

    // ── Profile ──────────────────────────────────────────────────────────────

    @GetMapping("/me")
    @Operation(summary = "Get my profile")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                ApiResponse.success(userService.getMyProfile(principal.getId())));
    }

    @PutMapping("/me")
    @Operation(summary = "Update my profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Profile updated successfully",
                userService.updateProfile(principal.getId(), request)));
    }

    @PatchMapping(value = "/me/profile-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload profile image")
    public ResponseEntity<ApiResponse<UserResponse>> uploadProfileImage(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(
                "Profile image updated",
                userService.uploadProfileImage(principal.getId(), file)));
    }

    @PatchMapping("/me/change-password")
    @Operation(summary = "Change my password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete my account (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @AuthenticationPrincipal UserPrincipal principal) {
        userService.deleteAccount(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Account deleted successfully"));
    }

    // ── Addresses ────────────────────────────────────────────────────────────

    @GetMapping("/me/addresses")
    @Operation(summary = "Get all my addresses")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.getMyAddresses(principal.getId())));
    }

    @PostMapping("/me/addresses")
    @Operation(summary = "Add a new address")
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Address added successfully",
                userService.addAddress(principal.getId(), request)));
    }

    @PutMapping("/me/addresses/{addressId}")
    @Operation(summary = "Update an address")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Address updated successfully",
                userService.updateAddress(principal.getId(), addressId, request)));
    }

    @DeleteMapping("/me/addresses/{addressId}")
    @Operation(summary = "Delete an address")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long addressId) {
        userService.deleteAddress(principal.getId(), addressId);
        return ResponseEntity.ok(ApiResponse.success("Address deleted successfully"));
    }

    @PatchMapping("/me/addresses/{addressId}/default")
    @Operation(summary = "Set address as default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefault(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long addressId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Default address updated",
                userService.setDefaultAddress(principal.getId(), addressId)));
    }
}
package com.jullyscraft.service;

import com.jullyscraft.dto.request.AddressRequest;
import com.jullyscraft.dto.request.ChangePasswordRequest;
import com.jullyscraft.dto.request.UpdateProfileRequest;
import com.jullyscraft.dto.response.AddressResponse;
import com.jullyscraft.dto.response.PageResponse;
import com.jullyscraft.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {

    // ── Profile ──────────────────────────────────────────
    UserResponse getMyProfile(Long userId);
    UserResponse updateProfile(Long userId, UpdateProfileRequest request);
    UserResponse uploadProfileImage(Long userId, MultipartFile file);
    void changePassword(Long userId, ChangePasswordRequest request);
    void deleteAccount(Long userId);

    // ── Addresses ─────────────────────────────────────────
    List<AddressResponse> getMyAddresses(Long userId);
    AddressResponse addAddress(Long userId, AddressRequest request);
    AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request);
    void deleteAddress(Long userId, Long addressId);
    AddressResponse setDefaultAddress(Long userId, Long addressId);

    // ── Admin ─────────────────────────────────────────────
    PageResponse<UserResponse> getAllUsers(Pageable pageable);
    UserResponse getUserById(Long id);
    UserResponse toggleUserEnabled(Long id);
}
package com.jullyscraft.service.impl;

import com.jullyscraft.dto.request.AddressRequest;
import com.jullyscraft.dto.request.ChangePasswordRequest;
import com.jullyscraft.dto.request.UpdateProfileRequest;
import com.jullyscraft.dto.response.AddressResponse;
import com.jullyscraft.dto.response.FileUploadResponse;
import com.jullyscraft.dto.response.PageResponse;
import com.jullyscraft.dto.response.UserResponse;
import com.jullyscraft.entity.Address;
import com.jullyscraft.entity.User;
import com.jullyscraft.exception.BadRequestException;
import com.jullyscraft.exception.ForbiddenException;
import com.jullyscraft.exception.ResourceNotFoundException;
import com.jullyscraft.mapper.AddressMapper;
import com.jullyscraft.mapper.UserMapper;
import com.jullyscraft.repository.AddressRepository;
import com.jullyscraft.repository.UserRepository;
import com.jullyscraft.service.FileStorageService;
import com.jullyscraft.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final UserMapper userMapper;
    private final AddressMapper addressMapper;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    // ── Profile ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyProfile(Long userId) {
        return userMapper.toResponse(findUserById(userId));
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserById(userId);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        if (request.getPhone() != null) {
            if (!request.getPhone().equals(user.getPhone())
                    && userRepository.existsByPhone(request.getPhone())) {
                throw new BadRequestException("Phone number already in use");
            }
            user.setPhone(request.getPhone());
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse uploadProfileImage(Long userId, MultipartFile file) {
        User user = findUserById(userId);

        if (file.isEmpty()) {
            throw new BadRequestException("File must not be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Only image files are allowed");
        }

        FileUploadResponse uploaded = fileStorageService
                .uploadProfileImage(file, userId);
        user.setProfileImageUrl(uploaded.getOriginalUrl());
        // String imageUrl = cloudinaryService.upload(file, "profiles");
        String imageUrl = "https://placeholder.cloudinary.com/profiles/" + userId;

        user.setProfileImageUrl(imageUrl);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }

        User user = findUserById(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("New password must differ from current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId) {
        User user = findUserById(userId);
        user.softDelete();
        user.setEnabled(false);
        userRepository.save(user);
        log.info("Account soft-deleted for user: {}", user.getEmail());
    }

    // ── Addresses ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getMyAddresses(Long userId) {
        return addressRepository.findByUserIdAndDeletedFalse(userId)
                .stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AddressResponse addAddress(Long userId, AddressRequest request) {
        long count = addressRepository.countByUserIdAndDeletedFalse(userId);
        if (count >= 5) {
            throw new BadRequestException("Maximum 5 addresses allowed per account");
        }

        User user = findUserById(userId);
        Address address = addressMapper.toEntity(request);
        address.setUser(user);

        if (request.isDefaultAddress() || count == 0) {
            addressRepository.clearDefaultAddress(userId);
            address.setDefaultAddress(true);
        }

        return addressMapper.toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        Address address = findAddressByIdAndUser(addressId, userId);
        addressMapper.updateFromRequest(request, address);

        if (request.isDefaultAddress()) {
            addressRepository.clearDefaultAddress(userId);
            address.setDefaultAddress(true);
        }

        return addressMapper.toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = findAddressByIdAndUser(addressId, userId);

        if (address.isDefaultAddress()) {
            throw new BadRequestException(
                    "Cannot delete default address. Set another address as default first.");
        }

        address.softDelete();
        addressRepository.save(address);
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(Long userId, Long addressId) {
        Address address = findAddressByIdAndUser(addressId, userId);
        addressRepository.clearDefaultAddress(userId);
        address.setDefaultAddress(true);
        return addressMapper.toResponse(addressRepository.save(address));
    }

    // ── Admin ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        return PageResponse.of(
                userRepository.findAll(pageable).map(userMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return userMapper.toResponse(findUserById(id));
    }

    @Override
    @Transactional
    public UserResponse toggleUserEnabled(Long id) {
        User user = findUserById(id);
        user.setEnabled(!user.isEnabled());
        return userMapper.toResponse(userRepository.save(user));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    private Address findAddressByIdAndUser(Long addressId, Long userId) {
        return addressRepository.findByIdAndUserIdAndDeletedFalse(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));
    }
}
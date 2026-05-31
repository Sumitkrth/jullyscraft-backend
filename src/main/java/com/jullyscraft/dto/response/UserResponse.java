package com.jullyscraft.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String profileImageUrl;
    private boolean emailVerified;
    private boolean enabled;
    private Set<String> roles;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
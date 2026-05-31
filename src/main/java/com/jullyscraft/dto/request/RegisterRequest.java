package com.jullyscraft.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegisterRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 80, message = "First name must be 2–80 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 80, message = "Last name must be 2–80 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be 8–100 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$",
            message = "Password must contain uppercase, lowercase, digit, and special character")
    private String password;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number")
    private String phone;
}
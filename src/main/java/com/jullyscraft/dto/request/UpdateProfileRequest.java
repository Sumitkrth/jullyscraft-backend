package com.jullyscraft.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateProfileRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 80)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 80)
    private String lastName;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number")
    private String phone;
}
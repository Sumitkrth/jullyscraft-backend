package com.jullyscraft.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ForgotPasswordRequest {

    @NotBlank
    @Email(message = "Invalid email format")
    private String email;
}
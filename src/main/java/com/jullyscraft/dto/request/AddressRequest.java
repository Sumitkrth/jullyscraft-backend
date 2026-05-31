package com.jullyscraft.dto.request;

import com.jullyscraft.entity.Address;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AddressRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number")
    private String phone;

    @NotBlank(message = "Address line 1 is required")
    @Size(max = 255)
    private String addressLine1;

    @Size(max = 255)
    private String addressLine2;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 100)
    private String state;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Invalid pincode")
    private String pincode;

    @NotBlank(message = "Country is required")
    @Size(max = 100)
    private String country;

    private Address.AddressType type = Address.AddressType.HOME;

    private boolean defaultAddress = false;
}
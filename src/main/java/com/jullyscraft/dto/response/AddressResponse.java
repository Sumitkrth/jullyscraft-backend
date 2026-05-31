package com.jullyscraft.dto.response;

import com.jullyscraft.entity.Address;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AddressResponse {
    private Long id;
    private String fullName;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;
    private String country;
    private Address.AddressType type;
    private boolean defaultAddress;
}
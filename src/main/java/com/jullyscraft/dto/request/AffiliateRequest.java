package com.jullyscraft.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AffiliateRequest {

    @NotBlank(message = "Company name is required")
    @Size(max = 100)
    private String companyName;

    @NotBlank(message = "Website URL is required")
    @Size(max = 200)
    private String websiteUrl;
}
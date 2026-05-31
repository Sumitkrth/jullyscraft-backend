package com.jullyscraft.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserSummaryResponse {
    private Long id;
    private String fullName;
    private String email;
    private String profileImageUrl;
}
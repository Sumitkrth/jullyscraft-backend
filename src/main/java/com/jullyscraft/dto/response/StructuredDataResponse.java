package com.jullyscraft.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StructuredDataResponse {
    private String context;       // https://schema.org
    private String type;          // Product | BreadcrumbList | Organization
    private String json;          // full JSON-LD string
}
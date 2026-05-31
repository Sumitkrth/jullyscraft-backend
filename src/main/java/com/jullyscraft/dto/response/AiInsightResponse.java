package com.jullyscraft.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiInsightResponse {

    private String       summary;           // executive summary
    private List<String> highlights;        // key positive findings
    private List<String> warnings;          // areas of concern
    private List<String> recommendations;   // actionable suggestions
    private String       generatedAt;
}
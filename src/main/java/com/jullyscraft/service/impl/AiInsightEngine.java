package com.jullyscraft.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jullyscraft.dto.response.AiInsightResponse;
import com.jullyscraft.dto.response.ConversionReportResponse;
import com.jullyscraft.dto.response.SalesReportResponse;
import com.jullyscraft.dto.response.UserBehaviorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiInsightEngine {

    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    public AiInsightResponse generateInsights(
            SalesReportResponse sales,
            ConversionReportResponse conversion,
            UserBehaviorResponse behavior) {

        try {
            String prompt = buildInsightPrompt(
                    sales,
                    conversion,
                    behavior
            );

            String response = geminiService.generateJson(prompt);

            if (response == null || response.isBlank()) {
                return fallbackInsights();
            }

            response = response
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            AiInsightResponse result =
                    objectMapper.readValue(response, AiInsightResponse.class);

            result.setGeneratedAt(
                    LocalDateTime.now()
                            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );

            return result;

        } catch (Exception e) {
            log.error("AI insight generation failed: {}", e.getMessage(), e);
            return fallbackInsights();
        }
    }

    private String buildInsightPrompt(
            SalesReportResponse sales,
            ConversionReportResponse conversion,
            UserBehaviorResponse behavior) {

        return """
            You are a senior ecommerce business analyst.

            Analyze this business data and provide insights.

            Respond ONLY with valid JSON.

            {
              "summary": "2-3 sentence executive summary",
              "highlights": [
                "positive finding 1",
                "positive finding 2"
              ],
              "warnings": [
                "concern 1",
                "concern 2"
              ],
              "recommendations": [
                "action 1",
                "action 2",
                "action 3"
              ]
            }

            SALES DATA
            Revenue: ₹%s
            Orders: %d
            Average Order Value: ₹%s
            Net Revenue: ₹%s
            Discounts: ₹%s
            Refunds: ₹%s

            CONVERSION DATA
            Product Views: %d
            Add To Cart: %d
            Checkout Starts: %d
            Orders Placed: %d
            Conversion Rate: %.2f%%
            Cart Abandonment Rate: %.2f%%
            Bounce Rate: %.2f%%

            USER DATA
            Total Page Views: %d
            Active Users: %d
            New Users: %d
            Returning Users: %d
            Top Searches: %s

            Provide practical recommendations for an Indian ecommerce business.
            """.formatted(
                fmt(sales.getTotalRevenue()),
                sales.getTotalOrders(),
                fmt(sales.getAverageOrderValue()),
                fmt(sales.getNetRevenue()),
                fmt(sales.getTotalDiscount()),
                fmt(sales.getTotalRefunds()),
                conversion.getProductViews(),
                conversion.getAddToCartEvents(),
                conversion.getCheckoutStarts(),
                conversion.getOrdersPlaced(),
                conversion.getOverallConversionRate(),
                conversion.getCartAbandonmentRate(),
                conversion.getBounceRate(),
                behavior.getTotalPageViews(),
                behavior.getActiveUsersNow(),
                behavior.getNewUsersCount(),
                behavior.getReturningUsersCount(),
                String.join(", ", behavior.getTopSearchQueries())
        );
    }

    private AiInsightResponse fallbackInsights() {
        return AiInsightResponse.builder()
                .summary("AI insights are currently unavailable.")
                .highlights(List.of())
                .warnings(List.of())
                .recommendations(List.of(
                        "Check Gemini API configuration",
                        "Review analytics dashboard manually"
                ))
                .generatedAt(
                        LocalDateTime.now()
                                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
                .build();
    }

    private String fmt(BigDecimal value) {
        return value != null
                ? value.toPlainString()
                : "0";
    }
}
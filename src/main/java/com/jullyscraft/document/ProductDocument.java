package com.jullyscraft.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "products", createIndex = true)
@Setting(settingPath = "elasticsearch/product-settings.json")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDocument {

    @Id
    private String id;                  // product DB id as string

    // ── Searchable fields ─────────────────────────────────────────────────────
    @MultiField(
            mainField  = @Field(type = FieldType.Text, analyzer = "standard"),
            otherFields = {
                    @InnerField(suffix = "keyword",    type = FieldType.Keyword),
                    @InnerField(suffix = "suggest",    type = FieldType.Search_As_You_Type),
                    @InnerField(suffix = "ngram",      type = FieldType.Text,
                            analyzer = "ngram_analyzer")
            }
    )
    private String name;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String shortDescription;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String fullDescription;

    @MultiField(
            mainField  = @Field(type = FieldType.Text),
            otherFields = {
                    @InnerField(suffix = "keyword", type = FieldType.Keyword)
            }
    )
    private String brand;

    @Field(type = FieldType.Text)
    private String tags;

    // ── Category ──────────────────────────────────────────────────────────────
    @Field(type = FieldType.Long)
    private Long categoryId;

    @Field(type = FieldType.Keyword)
    private String categoryName;

    @Field(type = FieldType.Keyword)
    private String categorySlug;

    // ── Pricing ───────────────────────────────────────────────────────────────
    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Double)
    private BigDecimal discountPrice;

    @Field(type = FieldType.Double)
    private BigDecimal effectivePrice;

    @Field(type = FieldType.Integer)
    private int discountPercent;

    // ── Stock / flags ─────────────────────────────────────────────────────────
    @Field(type = FieldType.Integer)
    private int stockQuantity;

    @Field(type = FieldType.Boolean)
    private boolean inStock;

    @Field(type = FieldType.Boolean)
    private boolean active;

    @Field(type = FieldType.Boolean)
    private boolean featured;

    // ── Ratings ───────────────────────────────────────────────────────────────
    @Field(type = FieldType.Double)
    private double averageRating;

    @Field(type = FieldType.Integer)
    private int reviewCount;

    // ── Images ────────────────────────────────────────────────────────────────
    @Field(type = FieldType.Keyword, index = false)
    private String primaryImageUrl;

    // ── Variant info (for filtering) ──────────────────────────────────────────
    @Field(type = FieldType.Keyword)
    private List<String> sizes;

    @Field(type = FieldType.Keyword)
    private List<String> colors;

    // ── Slug ──────────────────────────────────────────────────────────────────
    @Field(type = FieldType.Keyword)
    private String slug;

    // ── Timestamps ────────────────────────────────────────────────────────────
    @Field(type = FieldType.Date,
            format = DateFormat.date_hour_minute_second)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date,
            format = DateFormat.date_hour_minute_second)
    private LocalDateTime updatedAt;
}
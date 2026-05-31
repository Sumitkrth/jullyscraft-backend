package com.jullyscraft.service.impl;

import com.jullyscraft.dto.response.StructuredDataResponse;
import com.jullyscraft.entity.Category;
import com.jullyscraft.entity.Product;
import com.jullyscraft.entity.ProductImage;
import com.jullyscraft.exception.ResourceNotFoundException;
import com.jullyscraft.repository.CategoryRepository;
import com.jullyscraft.repository.ProductRepository;
import com.jullyscraft.service.SeoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeoServiceImpl implements SeoService {

    private final ProductRepository  productRepository;
    private final CategoryRepository categoryRepository;

    @Value("${app.notification.email.base-url:http://localhost:3000}")
    private String baseUrl;

    // ── Sitemap ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "seo", key = "'sitemap'")
    public String generateSitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"\n");
        xml.append("        xmlns:image=\"http://www.google.com/schemas/sitemap-image/1.1\">\n");

        // Static pages
        appendUrl(xml, baseUrl,                      "1.0", "daily");
        appendUrl(xml, baseUrl + "/products",        "0.9", "daily");
        appendUrl(xml, baseUrl + "/categories",      "0.8", "weekly");
        appendUrl(xml, baseUrl + "/about",           "0.5", "monthly");
        appendUrl(xml, baseUrl + "/contact",         "0.5", "monthly");

        // Category pages
        categoryRepository.findAllActiveFlat().forEach(c -> {
            String loc = baseUrl + "/category/" + c.getSlug();
            appendUrl(xml, loc, "0.8", "weekly");
        });

        // Product pages
        int page = 0;
        while (true) {
            var products = productRepository.findByActiveTrueAndDeletedFalse(
                    PageRequest.of(page++, 200));
            if (products.isEmpty()) break;

            products.getContent().forEach(p -> {
                String loc = baseUrl + "/products/" + p.getSlug();
                xml.append("  <url>\n");
                xml.append("    <loc>").append(loc).append("</loc>\n");
                xml.append("    <changefreq>weekly</changefreq>\n");
                xml.append("    <priority>0.7</priority>\n");

                // Include primary image for Google Image Search
                p.getImages().stream()
                        .filter(ProductImage::isPrimaryImage)
                        .findFirst()
                        .ifPresent(img -> {
                            xml.append("    <image:image>\n");
                            xml.append("      <image:loc>")
                                    .append(img.getImageUrl())
                                    .append("</image:loc>\n");
                            xml.append("      <image:title>")
                                    .append(escapeXml(p.getName()))
                                    .append("</image:title>\n");
                            xml.append("    </image:image>\n");
                        });

                xml.append("  </url>\n");
            });
        }

        xml.append("</urlset>");
        log.info("Sitemap generated");
        return xml.toString();
    }

    // ── Product structured data (JSON-LD) ─────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public StructuredDataResponse getProductStructuredData(Long productId) {
        Product p = productRepository.findById(productId)
                .filter(pr -> !pr.isDeleted() && pr.isActive())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product", "id", productId));

        String primaryImage = p.getImages().stream()
                .filter(ProductImage::isPrimaryImage)
                .map(ProductImage::getImageUrl)
                .findFirst().orElse("");

        String json = """
            {
              "@context": "https://schema.org",
              "@type": "Product",
              "name": "%s",
              "description": "%s",
              "image": "%s",
              "brand": {
                "@type": "Brand",
                "name": "%s"
              },
              "offers": {
                "@type": "Offer",
                "url": "%s",
                "priceCurrency": "INR",
                "price": "%s",
                "availability": "%s",
                "seller": {
                  "@type": "Organization",
                  "name": "Jully's Craft"
                }
              },
              "aggregateRating": {
                "@type": "AggregateRating",
                "ratingValue": "%.1f",
                "reviewCount": "%d"
              }
            }
            """.formatted(
                escapeJson(p.getName()),
                escapeJson(p.getShortDescription()),
                primaryImage,
                escapeJson(p.getBrand() != null ? p.getBrand() : "Jully's Craft"),
                baseUrl + "/products/" + p.getSlug(),
                p.getEffectivePrice().toPlainString(),
                p.isInStock()
                        ? "https://schema.org/InStock"
                        : "https://schema.org/OutOfStock",
                p.getAverageRating(),
                p.getReviewCount());

        return StructuredDataResponse.builder()
                .context("https://schema.org")
                .type("Product")
                .json(json)
                .build();
    }

    // ── Category breadcrumb structured data ───────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public StructuredDataResponse getCategoryStructuredData(Long categoryId) {
        Category c = categoryRepository.findById(categoryId)
                .filter(cat -> !cat.isDeleted() && cat.isActive())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category", "id", categoryId));

        StringBuilder items = new StringBuilder();
        items.append("""
                {
                  "@type": "ListItem",
                  "position": 1,
                  "name": "Home",
                  "item": "%s"
                }""".formatted(baseUrl));

        items.append("""
                ,
                {
                  "@type": "ListItem",
                  "position": 2,
                  "name": "%s",
                  "item": "%s"
                }""".formatted(
                escapeJson(c.getName()),
                baseUrl + "/category/" + c.getSlug()));

        String json = """
            {
              "@context": "https://schema.org",
              "@type": "BreadcrumbList",
              "itemListElement": [%s]
            }
            """.formatted(items);

        return StructuredDataResponse.builder()
                .context("https://schema.org")
                .type("BreadcrumbList")
                .json(json)
                .build();
    }

    // ── Organisation structured data ──────────────────────────────────────────

    @Override
    public StructuredDataResponse getOrganizationStructuredData() {
        String json = """
            {
              "@context": "https://schema.org",
              "@type": "Organization",
              "name": "Jully's Craft",
              "url": "%s",
              "logo": "%s/images/logo.png",
              "contactPoint": {
                "@type": "ContactPoint",
                "contactType": "customer service",
                "email": "support@jullyscraft.com",
                "availableLanguage": ["English", "Hindi"]
              },
              "sameAs": [
                "https://www.instagram.com/jullyscraft",
                "https://www.facebook.com/jullyscraft"
              ]
            }
            """.formatted(baseUrl, baseUrl);

        return StructuredDataResponse.builder()
                .context("https://schema.org")
                .type("Organization")
                .json(json)
                .build();
    }

    // ── robots.txt ────────────────────────────────────────────────────────────

    @Override
    public String generateRobotsTxt() {
        return """
            User-agent: *
            Allow: /

            Disallow: /api/
            Disallow: /admin/
            Disallow: /checkout/
            Disallow: /cart/
            Disallow: /account/

            Sitemap: %s/sitemap.xml
            """.formatted(baseUrl);
    }

    // ── Canonical URL ─────────────────────────────────────────────────────────

    @Override
    public String generateCanonicalUrl(String path) {
        return baseUrl + "/" + path.replaceAll("^/+", "");
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void appendUrl(StringBuilder xml, String loc,
                           String priority, String changefreq) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(loc).append("</loc>\n");
        xml.append("    <changefreq>").append(changefreq)
                .append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
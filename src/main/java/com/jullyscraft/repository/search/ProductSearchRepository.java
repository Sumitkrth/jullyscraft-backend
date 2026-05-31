package com.jullyscraft.repository.search;

import com.jullyscraft.document.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSearchRepository
        extends ElasticsearchRepository<ProductDocument, String> {

    // Simple keyword search across name / description / brand / tags
    Page<ProductDocument> findByNameContainingOrBrandContainingOrTagsContaining(
            String name, String brand, String tags, Pageable pageable);

    // Active products only
    Page<ProductDocument> findByActiveTrueAndInStockTrue(Pageable pageable);

    // By category
    Page<ProductDocument> findByCategoryIdAndActiveTrue(
            Long categoryId, Pageable pageable);

    // Featured
    Page<ProductDocument> findByFeaturedTrueAndActiveTrue(Pageable pageable);

    // Multi-match autocomplete using Search_As_You_Type
    @Query("""
        {
          "multi_match": {
            "query":  "?0",
            "type":   "bool_prefix",
            "fields": ["name", "name._2gram", "name._3gram",
                       "name.suggest", "brand", "categoryName"]
          }
        }
        """)
    List<ProductDocument> autocomplete(String prefix, Pageable pageable);

    // Fuzzy search (typo-tolerant)
    @Query("""
        {
          "multi_match": {
            "query":     "?0",
            "fields":    ["name^3", "shortDescription", "brand^2",
                          "tags", "categoryName"],
            "fuzziness": "AUTO",
            "operator":  "or"
          }
        }
        """)
    Page<ProductDocument> fuzzySearch(String query, Pageable pageable);
}
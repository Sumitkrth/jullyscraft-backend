package com.jullyscraft.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Slf4j
@Configuration
// ✅ This entire class is SKIPPED when app.elasticsearch.enabled=false
@ConditionalOnProperty(
        name  = "app.elasticsearch.enabled",
        havingValue = "true",
        matchIfMissing = false
)
@EnableElasticsearchRepositories(
        basePackages = "com.jullyscraft.repository.search")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    // Uses spring.elasticsearch.uris — falls back to localhost if missing
    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String elasticsearchUri;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Override
    public ClientConfiguration clientConfiguration() {
        log.info("Configuring Elasticsearch client: {}", elasticsearchUri);

        var builder = ClientConfiguration.builder()
                .connectedTo(elasticsearchUri
                        .replace("http://", "")
                        .replace("https://", ""));

        if (username != null && !username.isBlank()) {
            builder.withBasicAuth(username, password);
        }

        return builder.build();
    }
}
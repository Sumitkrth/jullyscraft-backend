package com.jullyscraft.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(
        basePackages = "com.jullyscraft.repository.search")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String elasticsearchUri;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Override
    public ClientConfiguration clientConfiguration() {
        var builder = ClientConfiguration.builder()
                .connectedTo(elasticsearchUri.replace("http://", "")
                        .replace("https://", ""));

        if (!username.isBlank()) {
            builder.withBasicAuth(username, password);
        }

        return builder.build();
    }
}
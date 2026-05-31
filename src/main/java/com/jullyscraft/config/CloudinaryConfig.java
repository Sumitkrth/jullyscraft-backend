package com.jullyscraft.config;

import com.cloudinary.Cloudinary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Slf4j
@Configuration
public class CloudinaryConfig {

    @Value("${app.cloudinary.cloud-name}")
    private String cloudName;

    @Value("${app.cloudinary.api-key}")
    private String apiKey;

    @Value("${app.cloudinary.api-secret}")
    private String apiSecret;

    @Value("${app.cloudinary.secure:true}")
    private boolean secure;

    @Bean
    public Cloudinary cloudinary() {
        Cloudinary cloudinary = new Cloudinary(Map.of(
                "cloud_name", cloudName,
                "api_key",    apiKey,
                "api_secret", apiSecret,
                "secure",     secure
        ));
        log.info("Cloudinary initialized for cloud: {}", cloudName);
        return cloudinary;
    }
}
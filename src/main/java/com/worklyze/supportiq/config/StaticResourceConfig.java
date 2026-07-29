package com.worklyze.supportiq.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    private final String imageBaseUrl;
    private final String imageStorageDir;

    public StaticResourceConfig(
            @Value("${supportiq.ingestion.image-base-url:/images}") String imageBaseUrl,
            @Value("${supportiq.ingestion.image-storage-dir:./data/images}") String imageStorageDir
    ) {
        this.imageBaseUrl = imageBaseUrl;
        this.imageStorageDir = imageStorageDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String diskPath = imageStorageDir.endsWith("/") ? imageStorageDir : imageStorageDir + "/";
        registry.addResourceHandler(imageBaseUrl + "/**")
                .addResourceLocations("file:" + diskPath);
    }
}

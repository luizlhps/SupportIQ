package com.worklyze.supportiq.feature.ingestion.shared;

import java.util.List;

public record ParsedPage(
        int pageNumber,
        String text,
        List<KnowledgeImage> images
) {

    public boolean hasText() {
        return text != null && !text.isBlank();
    }

    public boolean hasImages() {
        return images != null && !images.isEmpty();
    }
}
package com.worklyze.supportiq.feature.ingestion.application.service;

import com.worklyze.supportiq.feature.ingestion.shared.KnowledgeImage;

import java.util.List;

public interface ImageStorageService {

    List<String> store(
            String documentName,
            int pageNumber,
            List<KnowledgeImage> images
    );

    void deleteAllFor(String documentName);
}

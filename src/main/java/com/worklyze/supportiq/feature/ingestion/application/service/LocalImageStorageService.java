package com.worklyze.supportiq.feature.ingestion.application.service;

import com.worklyze.supportiq.feature.ingestion.shared.KnowledgeImage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class LocalImageStorageService implements ImageStorageService {

    private final String storageDir;
    private final String baseUrl;

    public LocalImageStorageService(
            @Value("${supportiq.ingestion.image-storage-dir:./data/images}") String storageDir,
            @Value("${supportiq.ingestion.image-base-url:/images}") String baseUrl
    ) {
        this.storageDir = storageDir;
        this.baseUrl = baseUrl;
    }

    @Override
    public List<String> store(
            String documentName,
            int pageNumber,
            List<KnowledgeImage> images
    ) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }

        String safeDocName = sanitize(documentName);
        Path pageDir = Paths.get(storageDir, safeDocName, String.valueOf(pageNumber));

        try {
            Files.createDirectories(pageDir);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao criar diretório de imagens: " + pageDir, e);
        }

        List<String> storedPaths = new ArrayList<>();

        for (KnowledgeImage image : images) {
            Path target = pageDir.resolve(image.fileName());

            try {
                Files.write(target, image.content());
            } catch (IOException e) {
                throw new RuntimeException("Erro ao salvar imagem: " + target, e);
            }

            String relativePath = baseUrl + "/" + safeDocName + "/" + pageNumber + "/" + image.fileName();
            storedPaths.add(relativePath);
        }

        return storedPaths;
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}

package com.worklyze.supportiq.feature.ingestion.domain.usecases;

import com.worklyze.supportiq.config.ai.AiProvider;

import java.io.InputStream;

public interface PdfIngestionUseCase {
    void execute(AiProvider provider, String fileName, InputStream inputStream);
}

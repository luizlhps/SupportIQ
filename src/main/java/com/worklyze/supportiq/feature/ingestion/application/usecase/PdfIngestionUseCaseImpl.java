package com.worklyze.supportiq.feature.ingestion.application.usecase;

import com.worklyze.supportiq.config.ai.AiModelRegistry;
import com.worklyze.supportiq.config.ai.AiProvider;
import com.worklyze.supportiq.feature.ingestion.application.service.DocumentSplitter;
import com.worklyze.supportiq.feature.ingestion.application.service.ImageStorageService;
import com.worklyze.supportiq.feature.embedding.KnowledgeEmbeddingService;
import com.worklyze.supportiq.feature.embedding.KnowledgeRepository;
import com.worklyze.supportiq.feature.ingestion.domain.usecases.PdfIngestionUseCase;
import com.worklyze.supportiq.feature.ingestion.shared.DocumentParser;
import com.worklyze.supportiq.feature.ingestion.shared.ParsedDocument;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfIngestionUseCaseImpl implements PdfIngestionUseCase {

    private final DocumentParser documentParser;
    private final DocumentSplitter splitter;
    private final KnowledgeEmbeddingService embeddingService;
    private final KnowledgeRepository repository;
    private final ImageStorageService imageStorageService;
    private final AiModelRegistry aiModelRegistry;

    @Override
    public void execute(
            AiProvider provider,
            String fileName,
            InputStream inputStream) {

        AiProvider effective = aiModelRegistry.resolve(provider);

        try {

            // Reingestão do mesmo arquivo: remove embeddings e imagens antigas
            // antes de gravar as novas para evitar conflito entre versões.
            repository.deleteByFileName(effective, fileName);
            imageStorageService.deleteAllFor(fileName);

            ParsedDocument document = documentParser.parse(fileName, inputStream);

            List<TextSegment> chunks = splitter.split(document);

            if (chunks.isEmpty()) {
                return;
            }

            List<Embedding> embedded = embeddingService.embed(effective, chunks);

            repository.saveAll(
                    effective,
                    embedded,
                    chunks
            );

        } catch (Exception ex) {
            throw new RuntimeException("Erro ao processar PDF: " + fileName, ex);
        }
    }
}
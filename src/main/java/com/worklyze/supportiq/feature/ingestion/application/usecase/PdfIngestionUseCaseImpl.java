package com.worklyze.supportiq.feature.ingestion.application.usecase;

import com.worklyze.supportiq.feature.ingestion.application.service.DocumentSplitter;
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

    @Override
    public void execute(
            String fileName,
            InputStream inputStream) {

        try {

            ParsedDocument document = documentParser.parse(fileName, inputStream);

            List<TextSegment> chunks = splitter.split(document);

            List<Embedding> embedded = embeddingService.embed(chunks);

            repository.saveAll(
                    embedded,
                    chunks
            );

        } catch (Exception ex) {
            throw new RuntimeException("Erro ao processar PDF: " + fileName, ex);
        }
    }
}
package com.worklyze.supportiq.feature.ingestion.infra.repository;


import com.worklyze.supportiq.config.ai.AiProvider;
import com.worklyze.supportiq.config.ai.VectorStoreRegistry;
import com.worklyze.supportiq.feature.embedding.KnowledgeRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
@RequiredArgsConstructor
public class PgVectorKnowledgeRepository
        implements KnowledgeRepository {


    private final VectorStoreRegistry vectorStoreRegistry;


    @Override
    public void saveAll(
            AiProvider provider,
            List<Embedding> embeddings,
            List<TextSegment> segments
    ) {

        vectorStoreRegistry.store(provider).addAll(
                embeddings,
                segments
        );
    }

    @Override
    public List<TextSegment> findRelevant(
            AiProvider provider,
            Embedding embedding,
            int maxResults
    ) {

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .maxResults(maxResults)
                .build();

        EmbeddingSearchResult<TextSegment> result = vectorStoreRegistry.store(provider).search(request);

        return result.matches().stream()
                .map(match -> match.embedded())
                .toList();
    }

    @Override
    public void deleteByFileName(AiProvider provider, String fileName) {

        if (fileName == null || fileName.isBlank()) {
            return;
        }

        Filter filter = MetadataFilterBuilder
                .metadataKey("fileName")
                .isEqualTo(fileName);

        vectorStoreRegistry.store(provider).removeAll(filter);
    }
}
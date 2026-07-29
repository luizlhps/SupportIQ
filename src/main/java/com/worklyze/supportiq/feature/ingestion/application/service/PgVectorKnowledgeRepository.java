package com.worklyze.supportiq.feature.ingestion.application.service;


import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
@RequiredArgsConstructor
public class PgVectorKnowledgeRepository
        implements KnowledgeRepository {


    private final EmbeddingStore<TextSegment> embeddingStore;


    @Override
    public void saveAll(
            List<Embedding> embeddings,
            List<TextSegment> segments
    ) {

        embeddingStore.addAll(
                embeddings,
                segments
        );
    }

    @Override
    public List<TextSegment> findRelevant(
            Embedding embedding,
            int maxResults
    ) {

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .maxResults(maxResults)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

        return result.matches().stream()
                .map(match -> match.embedded())
                .toList();
    }
}
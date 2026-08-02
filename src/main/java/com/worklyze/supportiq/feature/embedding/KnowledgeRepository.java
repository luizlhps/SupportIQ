package com.worklyze.supportiq.feature.embedding;

import com.worklyze.supportiq.config.ai.AiProvider;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;

import java.util.List;

public interface KnowledgeRepository {


    void saveAll(
            AiProvider provider,
            List<Embedding> embeddings,
            List<TextSegment> segments
    );

    List<TextSegment> findRelevant(
            AiProvider provider,
            Embedding embedding,
            int maxResults
    );

    void deleteByFileName(AiProvider provider, String fileName);

}
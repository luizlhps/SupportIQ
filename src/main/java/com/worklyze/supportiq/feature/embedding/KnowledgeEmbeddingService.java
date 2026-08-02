package com.worklyze.supportiq.feature.embedding;

import com.worklyze.supportiq.config.ai.AiModelRegistry;
import com.worklyze.supportiq.config.ai.AiProvider;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeEmbeddingService {

    private final AiModelRegistry aiModelRegistry;

    public List<Embedding> embed(
            AiProvider provider,
            List<TextSegment> segments
    ) {
        EmbeddingModel model = aiModelRegistry.embedding(provider);

        return segments.stream()
                .map(segment -> model.embed(segment).content())
                .toList();
    }

    public Embedding embed(AiProvider provider, String text) {
        return aiModelRegistry.embedding(provider).embed(text).content();
    }
}
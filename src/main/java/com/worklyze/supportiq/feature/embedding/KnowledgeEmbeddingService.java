package com.worklyze.supportiq.feature.embedding;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KnowledgeEmbeddingService {
    private final EmbeddingModel embeddingModel;


    public List<Embedding> embed(
            List<TextSegment> segments
    ){

        return segments.stream()
                .map(segment ->
                    embeddingModel.embed(segment)
                    .content()
                )
                .toList();
    }
}
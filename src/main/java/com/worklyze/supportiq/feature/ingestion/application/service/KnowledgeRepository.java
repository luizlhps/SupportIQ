package com.worklyze.supportiq.feature.ingestion.application.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;

import java.util.List;

public interface KnowledgeRepository {


    void saveAll(
            List<Embedding> embeddings,
            List<TextSegment> segments
    );

}
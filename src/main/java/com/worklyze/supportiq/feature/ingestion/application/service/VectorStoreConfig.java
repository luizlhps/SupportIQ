package com.worklyze.supportiq.feature.ingestion.application.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class VectorStoreConfig {


    private final EmbeddingModel embeddingModel;


    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(
            DataSource dataSource
    ) {


        return PgVectorEmbeddingStore.builder()
                .database(dataSource)
                .table("knowledge_embedding")
                .dimension(
                    embeddingModel.dimension()
                )
                .createTable(true)
                .build();
    }
}
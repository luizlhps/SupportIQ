package com.worklyze.supportiq.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class PgVectorConfig {


    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(
            DataSource dataSource
    ) {

        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .table("knowledge_embeddings")
                .dimension(768)
                .createTable(true)
                .build();
    }
}
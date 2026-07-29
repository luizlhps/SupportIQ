package com.worklyze.supportiq.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfiguration {

    @Value("${langchain4j.ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${langchain4j.ollama.embedding-model}")
    private String ollamaEmbeddingModel;

    @Value("${langchain4j.ollama.chat-model}")
    private String ollamaChatModel;

    @Bean
    EmbeddingModel embeddingModel() {

        return OllamaEmbeddingModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(ollamaEmbeddingModel)
                .build();
    }

    @Bean
    ChatModel chatModel() {

        return OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(ollamaChatModel)
                .build();
    }
}
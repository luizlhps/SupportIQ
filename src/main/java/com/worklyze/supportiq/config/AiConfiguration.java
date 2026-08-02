package com.worklyze.supportiq.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfiguration {

    // ---------- Ollama (sempre disponivel) ----------

    @Bean("ollamaEmbeddingModel")
    EmbeddingModel ollamaEmbeddingModel(
            @Value("${langchain4j.ollama.base-url}") String baseUrl,
            @Value("${langchain4j.ollama.embedding-model}") String modelName
    ) {
        return OllamaEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();
    }

    @Bean("ollamaChatModel")
    ChatModel ollamaChatModel(
            @Value("${langchain4j.ollama.base-url}") String baseUrl,
            @Value("${langchain4j.ollama.chat-model}") String modelName
    ) {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();
    }

    // ---------- OpenAI (habilitado se api-key definida) ----------

    @Bean("openAiChatModel")
    @ConditionalOnProperty(prefix = "supportiq.ai.openai", name = "api-key")
    ChatModel openAiChatModel(
            @Value("${supportiq.ai.openai.api-key}") String apiKey,
            @Value("${supportiq.ai.openai.chat-model}") String modelName
    ) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }

    @Bean("openAiEmbeddingModel")
    @ConditionalOnProperty(prefix = "supportiq.ai.openai", name = "api-key")
    EmbeddingModel openAiEmbeddingModel(
            @Value("${supportiq.ai.openai.api-key}") String apiKey,
            @Value("${supportiq.ai.openai.embedding-model}") String modelName
    ) {
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }

    // ---------- Google Gemini (habilitado se api-key definida) ----------

    @Bean("geminiChatModel")
    @ConditionalOnProperty(prefix = "supportiq.ai.gemini", name = "api-key")
    ChatModel geminiChatModel(
            @Value("${supportiq.ai.gemini.api-key}") String apiKey,
            @Value("${supportiq.ai.gemini.chat-model}") String modelName
    ) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }

    @Bean("geminiEmbeddingModel")
    @ConditionalOnProperty(prefix = "supportiq.ai.gemini", name = "api-key")
    EmbeddingModel geminiEmbeddingModel(
            @Value("${supportiq.ai.gemini.api-key}") String apiKey,
            @Value("${supportiq.ai.gemini.embedding-model}") String modelName
    ) {
        return GoogleAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }
}
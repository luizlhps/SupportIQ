package com.worklyze.supportiq.config.ai;

import com.worklyze.supportiq.shared.exceptions.BadRequestException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class AiModelRegistry {

    private final Map<AiProvider, ChatModel> chatModels = new EnumMap<>(AiProvider.class);
    private final Map<AiProvider, EmbeddingModel> embeddingModels = new EnumMap<>(AiProvider.class);
    private final AiProvider defaultProvider;

    public AiModelRegistry(
            ApplicationContext context,
            @Value("${supportiq.ai.default-provider:ollama}") String defaultProvider
    ) {
        this.defaultProvider = AiProvider.valueOf(defaultProvider.trim().toUpperCase());

        register(context, AiProvider.OLLAMA, "ollamaChatModel", "ollamaEmbeddingModel");
        register(context, AiProvider.OPENAI, "openAiChatModel", "openAiEmbeddingModel");
        register(context, AiProvider.GEMINI, "geminiChatModel", "geminiEmbeddingModel");
    }

    private void register(
            ApplicationContext context,
            AiProvider provider,
            String chatBeanName,
            String embeddingBeanName
    ) {
        if (context.containsBean(chatBeanName)) {
            chatModels.put(provider, context.getBean(chatBeanName, ChatModel.class));
        }

        if (context.containsBean(embeddingBeanName)) {
            embeddingModels.put(provider, context.getBean(embeddingBeanName, EmbeddingModel.class));
        }
    }

    public AiProvider defaultProvider() {
        return defaultProvider;
    }

    public AiProvider resolve(AiProvider requested) {
        return requested != null ? requested : defaultProvider;
    }

    public ChatModel chat(AiProvider provider) {
        AiProvider effective = resolve(provider);
        ChatModel model = chatModels.get(effective);

        if (model == null) {
            throw new BadRequestException(
                    AiExceptionCode.CHAT_MODEL_NOT_CONFIGURED.getMessage().formatted(effective),
                    AiExceptionCode.CHAT_MODEL_NOT_CONFIGURED.getCode()
            );
        }

        return model;
    }

    public EmbeddingModel embedding(AiProvider provider) {
        AiProvider effective = resolve(provider);
        EmbeddingModel model = embeddingModels.get(effective);

        if (model == null) {
            throw new BadRequestException(
                    AiExceptionCode.EMBEDDING_MODEL_NOT_CONFIGURED.getMessage().formatted(effective),
                    AiExceptionCode.EMBEDDING_MODEL_NOT_CONFIGURED.getCode()
            );
        }

        return model;
    }

    public boolean isAvailable(AiProvider provider) {
        return embeddingModels.containsKey(provider) && chatModels.containsKey(provider);
    }
}

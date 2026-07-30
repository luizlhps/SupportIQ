package com.worklyze.supportiq.feature.chat.application.usecase;

import com.worklyze.supportiq.feature.chat.application.service.ChatSessionMemoryStore;
import com.worklyze.supportiq.feature.chat.domain.usecases.AskQuestionUseCase;
import com.worklyze.supportiq.feature.chat.shared.ChatAnswer;
import com.worklyze.supportiq.feature.embedding.KnowledgeRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AskQuestionUseCaseImpl implements AskQuestionUseCase {

    private final EmbeddingModel embeddingModel;
    private final KnowledgeRepository knowledgeRepository;
    private final ChatModel chatModel;
    private final ChatSessionMemoryStore chatSessionMemoryStore;

    @Value("${supportiq.chat.max-results}")
    private int maxResults;

    @Override
    public ChatAnswer execute(String sessionId, String question) {

        String activeSessionId = (sessionId == null || sessionId.isBlank())
                ? UUID.randomUUID().toString()
                : sessionId;

        ChatMemory memory = chatSessionMemoryStore.getOrCreate(activeSessionId);

        Embedding questionEmbedding = embeddingModel.embed(question).content();

        List<TextSegment> relevantSegments = knowledgeRepository.findRelevant(
                questionEmbedding,
                maxResults
        );

        String context = relevantSegments.stream()
                .map(TextSegment::text)
                .collect(Collectors.joining("\n---\n"));

        List<String> imagePaths = extractImagePaths(relevantSegments);

        memory.add(SystemMessage.from(buildSystemPrompt(context, imagePaths)));
        memory.add(UserMessage.from(question));

        ChatResponse response = chatModel.chat(memory.messages());
        AiMessage aiMessage = response.aiMessage();

        memory.add(aiMessage);

        return new ChatAnswer(activeSessionId, aiMessage.text(), imagePaths);
    }

    private String buildSystemPrompt(String context, List<String> imagePaths) {

        String imageInfo = imagePaths.isEmpty()
                ? ""
                : "\n\nAs seguintes imagens foram encontradas no contexto e podem ser relevantes:\n"
                        + String.join("\n", imagePaths)
                        + "\n\nCite as imagens relevantes na sua resposta quando apropriado.";

        if (context.isBlank()) {
            return """
                    Você é um assistente de suporte. Não há contexto disponível na base de conhecimento para a
                    pergunta atual. Informe ao usuário que não foi encontrada informação relevante e responda
                    com cautela, sem inventar fatos. Use o histórico da conversa apenas como referência de contexto
                    da interação, não como fonte de fatos sobre o produto/serviço.%s
                    """.formatted(imageInfo);
        }

        return """
                Você é um assistente de suporte. Responda a pergunta do usuário utilizando apenas as informações
                do contexto abaixo, extraído da base de conhecimento. Se o contexto não for suficiente para
                responder, diga que não possui essa informação. Considere também o histórico da conversa para
                manter a coerência das respostas.%s

                Contexto:
                %s
                """.formatted(imageInfo, context);
    }

    private List<String> extractImagePaths(List<TextSegment> segments) {

        Set<String> paths = new LinkedHashSet<>();

        for (TextSegment segment : segments) {
            String images = segment.metadata().getString("images");
            if (images != null && !images.isBlank()) {
                Arrays.stream(images.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .forEach(paths::add);
            }
        }

        return new ArrayList<>(paths);
    }
}

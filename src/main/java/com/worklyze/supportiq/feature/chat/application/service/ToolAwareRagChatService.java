package com.worklyze.supportiq.feature.chat.application.service;

import com.worklyze.supportiq.config.ai.AiModelRegistry;
import com.worklyze.supportiq.config.ai.AiProvider;
import com.worklyze.supportiq.feature.embedding.KnowledgeRepository;
import com.worklyze.supportiq.feature.support.application.service.SupportSessionContext;
import com.worklyze.supportiq.feature.support.application.service.SupportTools;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Versão aprimorada do RagChatService com suporte a Function Calling.
 * <p>
 * A IA pode invocar as {@link SupportTools} automaticamente quando detectar
 * que o usuário precisa de suporte humano, eliminando a necessidade do
 * marcador [OFFER_SUPPORT].
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolAwareRagChatService {

    private final AiModelRegistry aiModelRegistry;
    private final KnowledgeRepository knowledgeRepository;
    private final ChatSessionMemoryStore chatSessionMemoryStore;
    private final SupportTools supportTools;

    @Value("${supportiq.chat.max-results}")
    private int maxResults;

    @Value("${supportiq.chat.use-tools:true}")
    private boolean useTools;

    /**
     * Interface do AiService com tools habilitadas.
     */
    interface SupportAssistant {
        String chat(String userMessage);
    }

    public Result chat(AiProvider provider, String sessionId, String question) {
        EmbeddingModel embeddingModel = aiModelRegistry.embedding(provider);
        ChatModel chatModel = aiModelRegistry.chat(provider);
        ChatMemory memory = chatSessionMemoryStore.getOrCreate(sessionId);

        // Recupera contexto do RAG
        Embedding questionEmbedding = embeddingModel.embed(question).content();
        List<TextSegment> segments = knowledgeRepository.findRelevant(
                provider,
                questionEmbedding,
                maxResults
        );

        String context = segments.stream()
                .map(TextSegment::text)
                .collect(Collectors.joining("\n---\n"));

        List<String> imagePaths = extractImagePaths(segments);
        String systemPrompt = buildSystemPrompt(context, imagePaths);

        // Adiciona contexto na memória
        memory.add(SystemMessage.from(systemPrompt));
        memory.add(UserMessage.from(question));

        String answer;
        boolean toolsUsed = false;

        if (useTools) {
            // Seta o contexto da sessão para as tools
            SupportSessionContext.set(sessionId);
            try {
                SupportAssistant assistant = AiServices.builder(SupportAssistant.class)
                        .chatModel(chatModel)
                        .chatMemory(memory)
                        .tools(supportTools)
                        .build();

                answer = assistant.chat(question);
                toolsUsed = true;
                log.debug("Resposta gerada com tools habilitadas");
            } catch (Exception e) {
                log.warn("Falha ao usar tools, fallback para chat simples: {}", e.getMessage());
                answer = fallbackChat(chatModel, memory);
            } finally {
                SupportSessionContext.clear();
            }
        } else {
            answer = fallbackChat(chatModel, memory);
        }

        return new Result(answer, imagePaths, toolsUsed);
    }

    private String fallbackChat(ChatModel chatModel, ChatMemory memory) {
        ChatResponse response = chatModel.chat(memory.messages());
        AiMessage aiMessage = response.aiMessage();
        memory.add(aiMessage);
        return aiMessage.text() == null ? "" : aiMessage.text().trim();
    }

    private String buildSystemPrompt(String context, List<String> imagePaths) {
        String imageInfo = imagePaths.isEmpty()
                ? ""
                : "\n\nAs seguintes imagens foram encontradas no contexto e podem ser relevantes:\n"
                + String.join("\n", imagePaths)
                + "\n\nCite as imagens relevantes na sua resposta quando apropriado.";

        String toolsInstruction = useTools ? """
                
                IMPORTANTE: Você tem acesso a ferramentas de suporte. Use-as quando:
                - O contexto não contém informação suficiente para responder
                - O usuário está frustrado ou pede atendimento humano
                - O problema persiste após múltiplas tentativas
                
                Primeiro verifique se o suporte está disponível antes de oferecê-lo.
                """ : "";

        if (context.isBlank()) {
            return """
                    Você é um assistente de suporte. Não há contexto disponível na base de conhecimento para a
                    pergunta atual. Informe ao usuário que não foi encontrada informação relevante e responda
                    com cautela, sem inventar fatos. Use o histórico da conversa apenas como referência de contexto
                    da interação, não como fonte de fatos sobre o produto/serviço.%s%s
                    """.formatted(imageInfo, toolsInstruction);
        }

        return """
                Você é um assistente de suporte. Responda a pergunta do usuário utilizando apenas as informações
                do contexto abaixo, extraído da base de conhecimento. Se o contexto não for suficiente para
                responder, diga que não possui essa informação. Considere também o histórico da conversa para
                manter a coerência das respostas.%s%s

                Contexto:
                %s
                """.formatted(imageInfo, toolsInstruction, context);
    }

    private List<String> extractImagePaths(List<TextSegment> segments) {
        if (segments == null || segments.isEmpty()) return List.of();

        TextSegment topSegment = segments.get(0);
        String images = topSegment.metadata().getString("images");
        if (images == null || images.isBlank()) return List.of();

        return Arrays.stream(images.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    public record Result(String answer, List<String> images, boolean toolsUsed) {}
}

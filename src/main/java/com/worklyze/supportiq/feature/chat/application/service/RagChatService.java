package com.worklyze.supportiq.feature.chat.application.service;

import com.worklyze.supportiq.config.ai.AiModelRegistry;
import com.worklyze.supportiq.config.ai.AiProvider;
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Executa uma rodada de chat com RAG: recupera contexto do vector store,
 * chama o modelo e detecta o marcador [OFFER_SUPPORT] que a IA emite quando
 * nao consegue resolver o problema.
 */
@Service
@RequiredArgsConstructor
public class RagChatService {

    public static final String OFFER_SUPPORT_MARKER = "[OFFER_SUPPORT]";

    private final AiModelRegistry aiModelRegistry;
    private final KnowledgeRepository knowledgeRepository;
    private final ChatSessionMemoryStore chatSessionMemoryStore;

    @Value("${supportiq.chat.max-results}")
    private int maxResults;

    public Result chat(AiProvider provider, String sessionId, String question) {

        EmbeddingModel embeddingModel = aiModelRegistry.embedding(provider);
        ChatModel chatModel = aiModelRegistry.chat(provider);
        ChatMemory memory = chatSessionMemoryStore.getOrCreate(sessionId);

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

        memory.add(SystemMessage.from(buildSystemPrompt(context, imagePaths)));
        memory.add(UserMessage.from(question));

        ChatResponse response = chatModel.chat(memory.messages());
        AiMessage aiMessage = response.aiMessage();
        memory.add(aiMessage);

        String raw = aiMessage.text() == null ? "" : aiMessage.text();
        boolean offerSupport = raw.contains(OFFER_SUPPORT_MARKER);
        String clean = raw.replace(OFFER_SUPPORT_MARKER, "").trim();

        return new Result(clean, imagePaths, offerSupport);
    }

    private String buildSystemPrompt(String context, List<String> imagePaths) {

        String imageInfo = imagePaths.isEmpty()
                ? ""
                : "\n\nAs seguintes imagens foram encontradas no contexto e podem ser relevantes:\n"
                        + String.join("\n", imagePaths)
                        + "\n\nCite as imagens relevantes na sua resposta quando apropriado.";

        String offerSupportRule = """

                IMPORTANTE: Adicione o marcador literal [OFFER_SUPPORT] em uma linha separada no final da resposta
                APENAS nas seguintes situacoes:
                
                1. Voce nao encontrou nenhuma informacao no contexto que responda a pergunta do usuario
                2. O usuario ja tentou todas as solucoes apresentadas e o problema persiste (verifique pelo historico)
                3. A pergunta e claramente um pedido para falar com um humano/atendente/suporte
                
                NAO adicione o marcador se:
                - Voce conseguiu responder com passos ou instrucoes do contexto
                - A resposta menciona nomes de pessoas de suporte (@Isayas, @Jair, etc) como parte das instrucoes
                - A resposta menciona WhatsApp, telefone ou e-mail como informacao util
                - O contexto tem frases como "fale com o TI" ou "entre em contato" mas voce conseguiu dar uma solucao
                
                Quando decidir adicionar o marcador, escreva ANTES dele uma frase natural oferecendo suporte,
                como por exemplo: "Se preferir, posso te conectar com o suporte. Quer contatar o suporte? (sim/nao)"
                Adaptando ao contexto da conversa. O marcador [OFFER_SUPPORT] sera removido antes de mostrar ao usuario.
                """;

        if (context.isBlank()) {
            return """
                    Você é um assistente de suporte. Não há contexto disponível na base de conhecimento para a
                    pergunta atual. Informe ao usuário que não foi encontrada informação relevante e responda
                    com cautela, sem inventar fatos. Use o histórico da conversa apenas como referência de contexto
                    da interação, não como fonte de fatos sobre o produto/serviço.%s%s
                    """.formatted(imageInfo, offerSupportRule);
        }

        return """
                Você é um assistente de suporte. Responda a pergunta do usuário utilizando apenas as informações
                do contexto abaixo, extraído da base de conhecimento. Se o contexto não for suficiente para
                responder, diga que não possui essa informação. Considere também o histórico da conversa para
                manter a coerência das respostas.%s%s

                Contexto:
                %s
                """.formatted(imageInfo, offerSupportRule, context);
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

    public record Result(String answer, List<String> images, boolean shouldOfferSupport) {}
}

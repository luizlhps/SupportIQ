package com.worklyze.supportiq.feature.chat.application.service;

import com.worklyze.supportiq.config.ai.AiModelRegistry;
import com.worklyze.supportiq.config.ai.AiProvider;
import com.worklyze.supportiq.feature.embedding.KnowledgeRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
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
        boolean hasMarker = raw.contains(OFFER_SUPPORT_MARKER);
        String clean = raw.replace(OFFER_SUPPORT_MARKER, "").trim();

        boolean offerSupport = hasMarker || aiDetectsSupportOffer(chatModel, clean);

        return new Result(clean, imagePaths, offerSupport);
    }

    private String buildSystemPrompt(String context, List<String> imagePaths) {

        String imageInfo = imagePaths.isEmpty()
                ? ""
                : "\n\nAs seguintes imagens foram encontradas no contexto e podem ser relevantes:\n"
                        + String.join("\n", imagePaths)
                        + "\n\nCite as imagens relevantes na sua resposta quando apropriado.";

        String offerSupportRule = """

                IMPORTANTE: Se voce nao souber responder a pergunta OU perceber, pelo historico da conversa,
                que o problema do usuario nao foi resolvido, termine sua resposta com o marcador literal
                [OFFER_SUPPORT] em uma linha separada. Esse marcador sera removido antes de mostrar ao usuario
                e usado apenas para oferecer contato com o suporte humano.
                
                IMPORTANTE:
                Sempre que o contexto indicar que o usuário deve entrar em contato com o suporte,
                OU houver frases como:
                
                - entre em contato com o suporte
                - abra um chamado
                - contate o help desk
                - procure o administrador
                - fale com o TI
                - não funcionou
                - chame o @Suporte
                
                NAO apenas forneça o telefone/WhatsApp do suporte e deixe o usuario por conta propria.
                Em vez disso, forneça a informacao de contato E adicione o marcador [OFFER_SUPPORT] em uma
                linha separada no final da resposta, para que o sistema possa oferecer enviar uma mensagem
                estruturada automaticamente.
                
                [OFFER_SUPPORT]
                
                em uma linha separada no final da resposta.
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

    private boolean aiDetectsSupportOffer(ChatModel chatModel, String answer) {
        if (answer == null || answer.isBlank()) return false;

        String detectionPrompt = """
            Você é um classificador. Analise a resposta abaixo e diga se ela está oferecendo,
            sugerindo ou direcionando o usuário a entrar em contato com suporte humano
            (por WhatsApp, telefone, e-mail, help desk, TI, administrador, chamado, etc).

            Responda APENAS com: SIM ou NAO

            Resposta para analisar:
            %s
            """.formatted(answer);

        List<ChatMessage> messages = List.of(
                SystemMessage.from(detectionPrompt),
                UserMessage.from(answer)
        );

        try {
            ChatResponse response = chatModel.chat(messages);
            String result = response.aiMessage().text().trim().toUpperCase();
            return result.startsWith("SIM");
        } catch (Exception e) {
            return false;
        }
    }

    private List<String> extractImagePaths(List<TextSegment> segments) {

        Set<String> paths = new LinkedHashSet<>();

        for (TextSegment segment : segments) {
            String images = segment.metadata().getString("images");
            if (images == null || images.isBlank()) continue;

            Arrays.stream(images.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(paths::add);
        }

        return new ArrayList<>(paths);
    }

    public record Result(String answer, List<String> images, boolean shouldOfferSupport) {}
}

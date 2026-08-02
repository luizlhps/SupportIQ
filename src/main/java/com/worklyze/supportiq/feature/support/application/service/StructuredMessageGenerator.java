package com.worklyze.supportiq.feature.support.application.service;

import com.worklyze.supportiq.config.ai.AiModelRegistry;
import com.worklyze.supportiq.config.ai.AiProvider;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Gera a mensagem estruturada do ticket a partir do historico da conversa
 * fornecido pelo chamador (ex: use case de chat). Nao acessa a memoria de
 * chat diretamente - respeitando o limite de contexto entre features.
 */
@Service
@RequiredArgsConstructor
public class StructuredMessageGenerator {

    private static final String TEMPLATE = """
            Com base no historico da conversa, gere uma mensagem estruturada para o time de suporte
            no formato abaixo, sem adicionar nada alem dela:

            *Novo chamado de suporte*
            - Sessao: %s
            - Problema: <descricao objetiva do problema do usuario>
            - O que ja foi tentado: <resumo do que foi conversado>
            - Contexto adicional: <informacoes relevantes, se houver>

            Nao inclua marcadores como [OFFER_SUPPORT]. Use apenas texto simples com asteriscos para negrito.
            %s
            """;

    private final AiModelRegistry aiModelRegistry;

    public String generate(
            AiProvider provider,
            String sessionId,
            String userFeedback,
            List<ChatMessage> history
    ) {
        ChatModel chatModel = aiModelRegistry.chat(provider);

        String feedbackLine = (userFeedback == null || userFeedback.isBlank())
                ? ""
                : "Ajustes solicitados pelo usuario: " + userFeedback;

        String instruction = TEMPLATE.formatted(sessionId, feedbackLine);

        List<ChatMessage> messages = new ArrayList<>(history == null ? List.of() : history);
        messages.add(UserMessage.from(instruction));

        ChatResponse response = chatModel.chat(messages);
        String text = response.aiMessage().text();
        return text == null ? "" : text.trim();
    }
}

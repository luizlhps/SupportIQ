package com.worklyze.supportiq.feature.chat.application.usecase;

import com.worklyze.supportiq.config.ai.AiModelRegistry;
import com.worklyze.supportiq.config.ai.AiProvider;
import com.worklyze.supportiq.feature.chat.application.service.ChatSessionMemoryStore;
import com.worklyze.supportiq.feature.chat.application.service.RagChatService;
import com.worklyze.supportiq.feature.chat.domain.usecases.AskQuestionUseCase;
import com.worklyze.supportiq.feature.chat.shared.ChatAnswer;
import com.worklyze.supportiq.feature.chat.shared.ChatResponse;
import com.worklyze.supportiq.feature.support.application.service.SupportFlowHandler;
import com.worklyze.supportiq.feature.support.shared.SupportFlowState;
import com.worklyze.supportiq.feature.support.shared.SupportReply;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orquestra uma rodada de chat: decide entre continuar o fluxo de suporte
 * (se em andamento) ou executar o pipeline RAG normal. O fluxo de suporte
 * vive na feature {@code support}; aqui apenas fornecemos o historico da
 * conversa quando necessario.
 */
@Service
@RequiredArgsConstructor
public class AskQuestionUseCaseImpl implements AskQuestionUseCase {

    private final AiModelRegistry aiModelRegistry;
    private final ChatSessionMemoryStore chatSessionMemoryStore;
    private final RagChatService ragChatService;
    private final SupportFlowHandler supportFlowHandler;

    @Override
    public ChatAnswer execute(AiProvider provider, String sessionId, String question) {
        AiProvider effectiveProvider = aiModelRegistry.resolve(provider);

        String activeSessionId = resolveSessionId(sessionId);

        SupportFlowState state = supportFlowHandler.currentState(activeSessionId);

        // USA IA PARA DECIDIR O FLUXO, INCLUINDO CONTEXTO DO ESTADO ATUAL
        String decision = routeIntent(effectiveProvider, question, state);

        if (state == SupportFlowState.AWAITING_SUPPORT_CONFIRMATION) {
            if (decision.contains("SUPPORT")) {
                Optional<SupportReply> reply = handleSupportConfirmation(effectiveProvider, activeSessionId, question);
                return reply
                        .map(r -> asChatAnswer(activeSessionId, r))
                        .orElseGet(() -> proceedWithChat(effectiveProvider, activeSessionId, question));
            }
            // Usuario nao quer suporte: volta para chat normal
            supportFlowHandler.reset(activeSessionId);
            return proceedWithChat(effectiveProvider, activeSessionId, question);
        }

        if (state == SupportFlowState.AWAITING_MESSAGE_CONFIRMATION) {
            if (decision.contains("SUPPORT")) {
                SupportReply reply = handleMessageConfirmation(effectiveProvider, activeSessionId, question);
                return asChatAnswer(activeSessionId, reply);
            }
            // Cancela envio e volta para chat
            supportFlowHandler.reset(activeSessionId);
            return proceedWithChat(effectiveProvider, activeSessionId, question);
        }

        // Estado NORMAL: IA decide se usuario quer suporte
        if (decision.contains("SUPPORT")) {
            String suffix = supportFlowHandler.startOffer(activeSessionId);
            return new ChatAnswer(activeSessionId, "Entendi que você precisa de suporte." + suffix, List.of());
        }

        return proceedWithChat(effectiveProvider, activeSessionId, question);
    }

    private ChatAnswer proceedWithChat(AiProvider provider, String sessionId, String question) {
        RagChatService.Result result = ragChatService.chat(provider, sessionId, question);

        if (result.shouldOfferSupport()) {
            String suffix = supportFlowHandler.startOffer(sessionId);
            return new ChatAnswer(sessionId, result.answer() + suffix, result.images());
        }

        return new ChatAnswer(sessionId, result.answer(), result.images());
    }

    private String routeIntent(AiProvider provider, String question, SupportFlowState state) {
        ChatModel chatModel = aiModelRegistry.chat(provider);

        String stateContext = switch (state) {
            case AWAITING_SUPPORT_CONFIRMATION -> "\nO usuario foi oferecido suporte humano e esta respondendo se aceita ou nao. SUPPORT = aceita, CHAT = recusa.";
            case AWAITING_MESSAGE_CONFIRMATION -> "\nO usuario recebeu um rascunho da mensagem de suporte e esta respondendo. SUPPORT = confirmar/enviar/ajustar, CHAT = cancelar.";
            case NORMAL -> "";
        };

        String routingPrompt = """
            Você é um roteador de fluxo. Analise a mensagem do usuário e decida qual fluxo seguir.
            Responda APENAS com uma das opções: SUPPORT ou CHAT
            
            - SUPPORT: quando o usuário quer falar com suporte humano, abrir ticket, reportar problema não resolvido, ou confirma/envia mensagem de suporte
            - CHAT: para perguntas gerais, dúvidas técnicas, solicitações de informação, ou recusa/cancela suporte
            %s
            Mensagem do usuário: %s
            """.formatted(stateContext, question);

        List<ChatMessage> routingMessages = List.of(
                SystemMessage.from(routingPrompt),
                UserMessage.from(question)
        );

        dev.langchain4j.model.chat.response.ChatResponse routingResponse = chatModel.chat(routingMessages);
        return routingResponse.aiMessage().text().trim().toUpperCase();
    }

    private SupportReply handleMessageConfirmation(AiProvider provider, String sessionId, String question) {
        return supportFlowHandler.handleMessageConfirmation(provider, sessionId, question, historyOf(sessionId));
    }

    private Optional<SupportReply> handleSupportConfirmation(AiProvider provider, String sessionId, String question) {
        return supportFlowHandler.handleSupportConfirmation(provider, sessionId, question, historyOf(sessionId));
    }

    private String resolveSessionId(String sessionId) {
        return (sessionId == null || sessionId.isBlank())
                ? UUID.randomUUID().toString()
                : sessionId;
    }

    private List<ChatMessage> historyOf(String sessionId) {
        ChatMemory memory = chatSessionMemoryStore.getOrCreate(sessionId);
        return List.copyOf(memory.messages());
    }

    private ChatAnswer asChatAnswer(String sessionId, SupportReply reply) {
        return new ChatAnswer(sessionId, reply.text(), List.of());
    }
}

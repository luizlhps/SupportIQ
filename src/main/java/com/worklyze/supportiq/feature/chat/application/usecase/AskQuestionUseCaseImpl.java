package com.worklyze.supportiq.feature.chat.application.usecase;

import com.worklyze.supportiq.config.ai.AiModelRegistry;
import com.worklyze.supportiq.config.ai.AiProvider;
import com.worklyze.supportiq.feature.chat.application.service.ChatSessionMemoryStore;
import com.worklyze.supportiq.feature.chat.application.service.RagChatService;
import com.worklyze.supportiq.feature.chat.domain.usecases.AskQuestionUseCase;
import com.worklyze.supportiq.feature.chat.shared.ChatAnswer;
import com.worklyze.supportiq.feature.support.application.service.SupportFlowHandler;
import com.worklyze.supportiq.feature.support.shared.SupportFlowState;
import com.worklyze.supportiq.feature.support.shared.SupportReply;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
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

        if (state == SupportFlowState.AWAITING_MESSAGE_CONFIRMATION) {
            SupportReply reply = handleMessageConfirmation(effectiveProvider, activeSessionId, question);

            return asChatAnswer(activeSessionId, reply);
        }

        if (state == SupportFlowState.AWAITING_SUPPORT_CONFIRMATION) {
            Optional<SupportReply> intercepted = handleSupportConfirmation(effectiveProvider, activeSessionId, question);

            if (intercepted.isPresent()) {
                return asChatAnswer(activeSessionId, intercepted.get());
            }
        }

        RagChatService.Result result = ragChatService.chat(effectiveProvider, activeSessionId, question);

        if (result.shouldOfferSupport() && supportFlowHandler.isAvailable()) {
            String suffix = supportFlowHandler.startOffer(activeSessionId);

            return new ChatAnswer(activeSessionId, result.answer() + suffix, result.images());
        }

        return new ChatAnswer(activeSessionId, result.answer(), result.images());
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

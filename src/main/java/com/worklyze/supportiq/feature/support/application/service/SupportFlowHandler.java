package com.worklyze.supportiq.feature.support.application.service;

import com.worklyze.supportiq.config.ai.AiModelRegistry;
import com.worklyze.supportiq.config.ai.AiProvider;
import com.worklyze.supportiq.feature.support.application.gateway.SupportSessionGateway;
import com.worklyze.supportiq.feature.support.application.gateway.SupportTicketGateway;
import com.worklyze.supportiq.feature.support.shared.SupportFlowState;
import com.worklyze.supportiq.feature.support.shared.SupportReply;
import com.worklyze.supportiq.feature.support.shared.SupportSession;
import dev.langchain4j.data.message.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Orquestra a maquina de estados do fluxo de suporte:
 * <ol>
 *     <li>{@code startOffer}: propoe ao usuario contatar o suporte;</li>
 *     <li>{@code handleSupportConfirmation}: interpreta a resposta e, se afirmativa,
 *         gera o rascunho da mensagem;</li>
 *     <li>{@code handleMessageConfirmation}: envia via gateway apos confirmacao,
 *         ou aplica ajustes solicitados pelo usuario.</li>
 * </ol>
 * O historico da conversa e sempre fornecido pelo chamador - o fluxo de
 * suporte nao acessa a memoria do chat diretamente.
 */
@Service
@RequiredArgsConstructor
public class SupportFlowHandler {

    private final SupportSessionGateway sessionGateway;
    private final SupportTicketGateway ticketGateway;
    private final StructuredMessageGenerator messageGenerator;
    private final AiYesNoInterpreter aiYesNo;
    private final AiModelRegistry aiModelRegistry;

    public boolean isAvailable() {
        return ticketGateway.isConfigured();
    }

    public SupportFlowState currentState(String sessionId) {
        return sessionGateway.getOrCreate(sessionId).getState();
    }

    public void reset(String sessionId) {
        sessionGateway.reset(sessionId);
    }

    /**
     * Marca a sessao como aguardando confirmacao. A frase de oferta de suporte
     * ja foi escrita pela IA do RAG como parte da resposta (antes do marcador).
     */
    public String startOffer(String sessionId) {
        SupportSession session = sessionGateway.getOrCreate(sessionId);
        sessionGateway.save(sessionId, session.toBuilder()
                .state(SupportFlowState.AWAITING_SUPPORT_CONFIRMATION)
                .build());
        return "";
    }

    /**
     * @return {@code Optional.empty()} quando a resposta e ambigua e o chamador
     *         deve prosseguir com o fluxo normal de chat.
     */
    public Optional<SupportReply> handleSupportConfirmation(
            AiProvider provider,
            String sessionId,
            String userMessage
    ) {
        AiProvider resolved = aiModelRegistry.resolve(provider);
        return switch (aiYesNo.classify(resolved, userMessage)) {
            case YES     -> Optional.of(askForName(sessionId));
            case NO      -> Optional.of(declineSupportOffer(sessionId));
            case UNCLEAR -> abandonSupportOffer(sessionId);
        };
    }

    public SupportReply handleNameInput(
            AiProvider provider,
            String sessionId,
            String userMessage,
            List<ChatMessage> history
    ) {
        String name = userMessage == null ? "" : userMessage.trim();
        if (name.isBlank()) {
            return new SupportReply("Por favor, digite seu nome para continuar.");
        }

        SupportSession session = sessionGateway.getOrCreate(sessionId);
        sessionGateway.save(sessionId, session.toBuilder()
                .userName(name)
                .build());

        return generateDraft(provider, sessionId, history);
    }

    public SupportReply handleMessageConfirmation(
            AiProvider provider,
            String sessionId,
            String userMessage,
            List<ChatMessage> history
    ) {
        AiProvider resolved = aiModelRegistry.resolve(provider);
        return switch (aiYesNo.classify(resolved, userMessage)) {
            case YES     -> sendDraft(sessionId);
            case NO      -> cancelDraft(sessionId);
            case UNCLEAR -> adjustDraft(provider, sessionId, userMessage, history);
        };
    }

    // ---- handleSupportConfirmation branches ------------------------------

    private SupportReply askForName(String sessionId) {
        SupportSession session = sessionGateway.getOrCreate(sessionId);
        sessionGateway.save(sessionId, session.toBuilder()
                .state(SupportFlowState.AWAITING_NAME)
                .build());
        return new SupportReply("Antes de continuar, qual é o seu nome?");
    }

    private SupportReply generateDraft(AiProvider provider, String sessionId, List<ChatMessage> history) {
        SupportSession session = sessionGateway.getOrCreate(sessionId);
        String name = session.getUserName();

        String draft = messageGenerator.generate(provider, sessionId, null, history, name);

        sessionGateway.save(sessionId, session.toBuilder()
                .state(SupportFlowState.AWAITING_MESSAGE_CONFIRMATION)
                .draftMessage(draft)
                .build());

        return new SupportReply(
                "Preparei esta mensagem para o suporte:\n\n" + draft
                        + "\n\nEssa descricao esta correta? Deseja enviar assim para o suporte? "
                        + "(responda 'sim' para enviar, 'nao' para cancelar, ou envie correcoes que eu ajusto)"
        );
    }

    private SupportReply declineSupportOffer(String sessionId) {
        sessionGateway.reset(sessionId);
        return new SupportReply("Sem problemas. Como posso te ajudar?");
    }

    private Optional<SupportReply> abandonSupportOffer(String sessionId) {
        // Resposta ambigua: descarta o fluxo e sinaliza ao chamador para seguir como pergunta normal.
        sessionGateway.reset(sessionId);
        return Optional.empty();
    }

    // ---- handleMessageConfirmation branches ------------------------------

    private SupportReply sendDraft(String sessionId) {
        SupportSession session = sessionGateway.getOrCreate(sessionId);

        try {
            String link = ticketGateway.generateLink(session.getDraftMessage());
            sessionGateway.reset(sessionId);
            return new SupportReply(
                    "Clique no link abaixo para enviar a mensagem pelo WhatsApp:\n\n" + link
            );
        } catch (Exception ex) {
            sessionGateway.reset(sessionId);
            return new SupportReply(
                    "Nao consegui gerar o link de suporte: " + ex.getMessage()
            );
        }
    }

    private SupportReply cancelDraft(String sessionId) {
        sessionGateway.reset(sessionId);
        return new SupportReply(
                "Envio cancelado. Se quiser, me diga como posso continuar te ajudando."
        );
    }

    private SupportReply adjustDraft(
            AiProvider provider,
            String sessionId,
            String userFeedback,
            List<ChatMessage> history
    ) {
        SupportSession session = sessionGateway.getOrCreate(sessionId);
        String updated = messageGenerator.generate(provider, sessionId, userFeedback, history, session.getUserName());

        sessionGateway.save(sessionId, session.toBuilder()
                .draftMessage(updated)
                .build());

        return new SupportReply(
                "Atualizei a mensagem:\n\n" + updated
                        + "\n\nPosso enviar assim para o suporte? (sim/nao)"
        );
    }
}

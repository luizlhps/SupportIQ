package com.worklyze.supportiq.feature.support.application.service;

import com.worklyze.supportiq.feature.support.application.gateway.SupportSessionGateway;
import com.worklyze.supportiq.feature.support.application.gateway.SupportTicketGateway;
import com.worklyze.supportiq.feature.support.shared.SupportFlowState;
import com.worklyze.supportiq.feature.support.shared.SupportSession;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Ferramentas disponíveis para a IA durante o fluxo de suporte.
 * Usando LangChain4j Function Calling (AI Tools).
 * <p>
 * A IA pode invocar essas ferramentas automaticamente quando detectar
 * que o usuário precisa de suporte humano, sem necessidade do marcador
 * [OFFER_SUPPORT].
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SupportTools {

    private final SupportTicketGateway ticketGateway;
    private final SupportSessionGateway sessionGateway;

    private String currentSessionId() {
        return SupportSessionContext.get();
    }

    @Tool("""
            Verifica se o sistema de suporte via WhatsApp está configurado e disponível.
            Use esta ferramenta antes de oferecer suporte ao usuário para confirmar
            que é possível conectá-lo com um atendente humano.
            """)
    public boolean checkSupportAvailability() {
        boolean available = ticketGateway.isConfigured();
        log.info("Verificação de disponibilidade do suporte: {}", available);
        return available;
    }

    @Tool("""
            Prepara uma mensagem estruturada para o suporte e pede confirmação do usuário.
            Use esta ferramenta SOMENTE DEPOIS de:
            1. O usuário aceitar falar com o suporte
            2. Perguntar e receber o nome do usuário (use askUserName primeiro)
            
            IMPORTANTE: Retorna um RASCUNHO para o usuário revisar ANTES de gerar o link.
            O usuário deve confirmar se a descrição está correta.
            """)
    public String prepareSupportMessage(
            @P("Nome do usuário (informado pelo próprio usuário após askUserName)") String userName,
            @P("Descrição clara e objetiva do problema do usuário") String problemDescription,
            @P("Resumo do que já foi tentado na conversa") String attemptedSolutions
    ) {
        // Valida se o nome foi fornecido
        if (userName == null || userName.isBlank()) {
            return askUserName();
        }

        log.info("Preparando mensagem de suporte para usuário: {}", userName);

        String structuredMessage = formatTicketMessage(userName, problemDescription, attemptedSolutions);

        // Salva o rascunho na sessão para confirmar depois
        String sessionId = currentSessionId();
        SupportSession session = sessionGateway.getOrCreate(sessionId);
        sessionGateway.save(sessionId, session.toBuilder()
                .state(SupportFlowState.AWAITING_MESSAGE_CONFIRMATION)
                .draftMessage(structuredMessage)
                .userName(userName)
                .build());

        log.info("Rascunho salvo, aguardando confirmação do usuário");

        return "Preparei a seguinte mensagem para o suporte:\n\n" + structuredMessage +
                "\n\nEssa descrição está correta? (responda 'sim' para enviar, 'não' para cancelar, " +
                "ou me diga o que precisa ser corrigido)";
    }

    @Tool("""
            Gera o link do WhatsApp após o usuário confirmar a mensagem.
            SOMENTE use esta ferramenta DEPOIS que o usuário confirmar que a mensagem está correta.
            Nunca use diretamente sem preparar a mensagem antes com prepareSupportMessage.
            """)
    public String confirmAndGenerateLink() {
        String sessionId = currentSessionId();
        SupportSession session = sessionGateway.getOrCreate(sessionId);

        if (session.getState() != SupportFlowState.AWAITING_MESSAGE_CONFIRMATION) {
            return "Erro: Nenhuma mensagem preparada para confirmação. Use prepareSupportMessage primeiro.";
        }

        String draftMessage = session.getDraftMessage();
        if (draftMessage == null || draftMessage.isBlank()) {
            return "Erro: Mensagem de suporte não encontrada.";
        }

        try {
            String link = ticketGateway.generateLink(draftMessage);
            sessionGateway.reset(sessionId);

            log.info("Link de suporte gerado após confirmação");
            return "Clique no link abaixo para enviar a mensagem pelo WhatsApp:\n\n" + link;
        } catch (Exception ex) {
            sessionGateway.reset(sessionId);
            return "Não consegui gerar o link de suporte: " + ex.getMessage();
        }
    }

    @Tool("""
            Oferece ao usuário a opção de contatar o suporte humano.
            Use esta ferramenta quando detectar que:
            - A resposta não foi satisfatória
            - O problema é complexo demais para resolver via chat
            - O usuário parece frustrado ou insatisfeito
            
            Retorna uma mensagem perguntando se o usuário deseja suporte.
            """)
    public String offerHumanSupport(
            @P("Motivo pelo qual está oferecendo suporte") String reason
    ) {
        if (!ticketGateway.isConfigured()) {
            return "Infelizmente o suporte via WhatsApp não está disponível no momento.";
        }

        String sessionId = currentSessionId();
        SupportSession session = sessionGateway.getOrCreate(sessionId);
        sessionGateway.save(sessionId, session.toBuilder()
                .state(SupportFlowState.AWAITING_SUPPORT_CONFIRMATION)
                .build());

        log.info("Oferecendo suporte humano. Motivo: {}", reason);
        return "Entendo que isso pode ser frustrante. Gostaria que eu te conectasse com " +
                "nosso time de suporte para resolver isso? (sim/não)";
    }

    @Tool("""
            Pergunta o nome do usuário para identificação no chamado de suporte.
            Use esta ferramenta DEPOIS que o usuário aceitar falar com o suporte
            e ANTES de preparar a mensagem estruturada.
            
            IMPORTANTE: Sempre pergunte o nome antes de chamar prepareSupportMessage.
            """)
    public String askUserName() {
        String sessionId = currentSessionId();
        SupportSession session = sessionGateway.getOrCreate(sessionId);
        sessionGateway.save(sessionId, session.toBuilder()
                .state(SupportFlowState.AWAITING_NAME)
                .build());

        log.info("Solicitando nome do usuário para o chamado");
        return "Para abrir o chamado, preciso do seu nome. Como você gostaria de ser identificado?";
    }

    private String formatTicketMessage(String userName, String problem, String attempted) {
        return """
                *Novo chamado de suporte*
                - Nome do usuário: %s
                - Problema: %s
                - O que já foi tentado: %s
                - Origem: Chat IA (SupportIQ)
                """.formatted(
                userName != null ? userName : "Não informado",
                problem != null ? problem : "Não especificado",
                attempted != null ? attempted : "Nenhuma tentativa registrada"
        );
    }
}

package com.worklyze.supportiq.feature.support.application.service;

import com.worklyze.supportiq.feature.support.application.gateway.SupportTicketGateway;
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
            Gera um link do WhatsApp para o usuário entrar em contato com o suporte.
            Use esta ferramenta quando:
            - O usuário confirmar que deseja falar com o suporte
            - Você não conseguir resolver o problema do usuário
            - O usuário solicitar explicitamente um atendente humano
            
            Retorna o link wa.me pronto para o usuário clicar e enviar a mensagem.
            """)
    public String generateSupportLink(
            @P("Nome do usuário para identificação no ticket") String userName,
            @P("Descrição clara e objetiva do problema do usuário") String problemDescription,
            @P("Resumo do que já foi tentado na conversa") String attemptedSolutions
    ) {
        log.info("Gerando link de suporte para usuário: {}", userName);

        String structuredMessage = formatTicketMessage(userName, problemDescription, attemptedSolutions);
        String link = ticketGateway.generateLink(structuredMessage);

        log.info("Link de suporte gerado com sucesso");
        return link;
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

        log.info("Oferecendo suporte humano. Motivo: {}", reason);
        return "Entendo que isso pode ser frustrante. Gostaria que eu te conectasse com " +
                "nosso time de suporte para resolver isso? (sim/não)";
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

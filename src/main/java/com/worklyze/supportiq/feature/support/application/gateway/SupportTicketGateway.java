package com.worklyze.supportiq.feature.support.application.gateway;

/**
 * Porta (Clean Architecture) para geracao de link de ticket de suporte
 * (WhatsApp, Slack, e-mail, etc). A implementacao concreta vive em
 * {@code feature/support/infra/gateway}.
 */
public interface SupportTicketGateway {

    /**
     * Gera um link para o usuario enviar a mensagem estruturada pelo
     * proprio WhatsApp (wa.me).
     * @throws IllegalStateException se o gateway nao estiver configurado
     */
    String generateLink(String structuredMessage);

    boolean isConfigured();
}

package com.worklyze.supportiq.feature.support.application.gateway;

/**
 * Porta (Clean Architecture) para envio de tickets de suporte para um canal
 * externo (WhatsApp, Slack, e-mail, etc). A implementacao concreta vive em
 * {@code feature/support/infra/gateway}.
 */
public interface SupportTicketGateway {

    /**
     * Envia a mensagem estruturada para o canal de suporte.
     * @throws IllegalStateException se o gateway nao estiver configurado
     * @throws RuntimeException em falha de rede/API
     */
    void send(String structuredMessage);

    boolean isConfigured();
}

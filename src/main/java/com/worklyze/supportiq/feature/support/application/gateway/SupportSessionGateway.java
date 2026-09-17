package com.worklyze.supportiq.feature.support.application.gateway;

import com.worklyze.supportiq.feature.support.shared.SupportSession;

/**
 * Gateway para armazenamento de sessões de suporte.
 * Permite implementações in-memory (local) ou distribuídas (Redis).
 */
public interface SupportSessionGateway {

    /**
     * Obtém ou cria uma sessão para o ID informado.
     */
    SupportSession getOrCreate(String sessionId);

    /**
     * Salva/atualiza uma sessão.
     */
    void save(String sessionId, SupportSession session);

    /**
     * Reseta a sessão para o estado inicial.
     */
    void reset(String sessionId);

    /**
     * Remove sessão (opcional, usado para limpeza).
     */
    void remove(String sessionId);
}

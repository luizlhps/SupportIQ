package com.worklyze.supportiq.feature.support.application.service;

/**
 * Contexto de sessão de suporte usando ThreadLocal.
 * Permite que as SupportTools acessem o sessionId da conversa atual.
 */
public final class SupportSessionContext {

    private static final ThreadLocal<String> CURRENT_SESSION_ID = new ThreadLocal<>();

    private SupportSessionContext() {}

    public static void set(String sessionId) {
        CURRENT_SESSION_ID.set(sessionId);
    }

    public static String get() {
        String sessionId = CURRENT_SESSION_ID.get();
        return sessionId != null ? sessionId : "default";
    }

    public static void clear() {
        CURRENT_SESSION_ID.remove();
    }
}

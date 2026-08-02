package com.worklyze.supportiq.feature.support.application.service;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Heuristica simples para interpretar respostas afirmativas/negativas do usuario
 * em portugues e ingles.
 */
@Component
public class YesNoInterpreter {

    private static final Set<String> YES_EXACT = Set.of(
            "sim", "s", "yes", "y", "ok", "claro",
            "confirmo", "confirmar", "pode enviar",
            "envie", "envia", "enviar", "manda", "mandar"
    );

    private static final Set<String> NO_EXACT = Set.of(
            "nao", "não", "n", "no", "cancela", "cancelar", "negativo"
    );

    private static final Set<String> YES_PREFIXES = Set.of(
            "sim,", "sim ", "pode ", "claro "
    );

    private static final Set<String> NO_PREFIXES = Set.of(
            "nao,", "não,", "nao ", "não "
    );

    public enum Answer { YES, NO, UNCLEAR }

    public Answer classify(String text) {
        String norm = normalize(text);
        if (norm.isEmpty()) return Answer.UNCLEAR;
        if (YES_EXACT.contains(norm) || startsWithAny(norm, YES_PREFIXES)) return Answer.YES;
        if (NO_EXACT.contains(norm) || startsWithAny(norm, NO_PREFIXES)) return Answer.NO;
        return Answer.UNCLEAR;
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase();
    }

    private boolean startsWithAny(String value, Set<String> prefixes) {
        for (String p : prefixes) {
            if (value.startsWith(p)) return true;
        }
        return false;
    }
}

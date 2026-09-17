package com.worklyze.supportiq.feature.support.shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Representa o estado de uma sessão de fluxo de suporte.
 * Imutável para thread-safety quando usado com stores concorrentes.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SupportSession {

    @Builder.Default
    private SupportFlowState state = SupportFlowState.NORMAL;

    private String draftMessage;
    private String userName;

    @Builder.Default
    private Instant lastActivity = Instant.now();

    /**
     * Cria uma nova sessão com timestamp atualizado.
     */
    public SupportSession touch() {
        return this.toBuilder()
                .lastActivity(Instant.now())
                .build();
    }

    /**
     * Verifica se a sessão expirou baseado no TTL em minutos.
     */
    public boolean isExpired(long ttlMinutes) {
        return Instant.now().isAfter(lastActivity.plusSeconds(ttlMinutes * 60));
    }

    /**
     * Cria uma sessão resetada (estado NORMAL, sem dados).
     */
    public static SupportSession reset() {
        return SupportSession.builder().build();
    }
}

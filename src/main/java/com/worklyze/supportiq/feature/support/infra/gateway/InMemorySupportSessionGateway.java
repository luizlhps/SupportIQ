package com.worklyze.supportiq.feature.support.infra.gateway;

import com.worklyze.supportiq.feature.support.application.gateway.SupportSessionGateway;
import com.worklyze.supportiq.feature.support.shared.SupportSession;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implementação in-memory do gateway de sessões de suporte.
 * Inclui limpeza automática de sessões expiradas (TTL).
 * <p>
 * Ativada por padrão ou quando {@code supportiq.support.session.store=local}.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "supportiq.support.session.store",
        havingValue = "local",
        matchIfMissing = true
)
public class InMemorySupportSessionGateway implements SupportSessionGateway {

    private final Map<String, SupportSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    @Value("${supportiq.support.session.ttl-minutes:30}")
    private long ttlMinutes;

    @Value("${supportiq.support.session.cleanup-interval-minutes:5}")
    private long cleanupIntervalMinutes;

    @PostConstruct
    void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(
                this::cleanupExpiredSessions,
                cleanupIntervalMinutes,
                cleanupIntervalMinutes,
                TimeUnit.MINUTES
        );

        log.info("InMemorySupportSessionGateway iniciado (TTL={}min, cleanup={}min)",
                ttlMinutes, cleanupIntervalMinutes);
    }

    @PreDestroy
    void stopCleanupTask() {
        cleanupExecutor.shutdown();

        log.info("InMemorySupportSessionGateway encerrado");
    }

    @Override
    public SupportSession getOrCreate(String sessionId) {
        return sessions.compute(sessionId, (id, existing) -> {
            if (existing == null) {
                return SupportSession.builder().build();
            }

            return existing.touch();
        });
    }

    @Override
    public void save(String sessionId, SupportSession session) {
        sessions.put(sessionId, session.touch());
    }

    @Override
    public void reset(String sessionId) {
        sessions.put(sessionId, SupportSession.reset());
    }

    @Override
    public void remove(String sessionId) {
        sessions.remove(sessionId);
    }

    private void cleanupExpiredSessions() {
        int before = sessions.size();

        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired(ttlMinutes));

        int removed = before - sessions.size();

        if (removed > 0) {
            log.debug("Limpeza de sessões: {} removidas, {} ativas", removed, sessions.size());
        }
    }
}

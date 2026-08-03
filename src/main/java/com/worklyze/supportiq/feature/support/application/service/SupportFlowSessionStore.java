package com.worklyze.supportiq.feature.support.application.service;

import com.worklyze.supportiq.feature.support.shared.SupportFlowState;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estado do fluxo de suporte por sessao. Em memoria e nao distribuido -
 * suficiente para uso local/dev.
 */
@Component
public class SupportFlowSessionStore {

    public static final class Session {
        private SupportFlowState state = SupportFlowState.NORMAL;
        private String draftMessage;
        private String userName;

        public SupportFlowState state() { return state; }
        public String draftMessage() { return draftMessage; }
        public String userName() { return userName; }

        public void setState(SupportFlowState state) { this.state = state; }
        public void setDraftMessage(String draftMessage) { this.draftMessage = draftMessage; }
        public void setUserName(String userName) { this.userName = userName; }
    }

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public Session getOrCreate(String sessionId) {
        return sessions.computeIfAbsent(sessionId, id -> new Session());
    }

    public void reset(String sessionId) {
        Session s = sessions.get(sessionId);
        if (s != null) {
            s.state = SupportFlowState.NORMAL;
            s.draftMessage = null;
            s.userName = null;
        }
    }
}

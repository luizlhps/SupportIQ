package com.worklyze.supportiq.feature.chat.application.service;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store of {@link ChatMemory} instances keyed by session id.
 * Not persisted and not shared across instances; sufficient for local/dev usage.
 * For production, replace with a distributed/persistent {@code ChatMemoryStore}.
 */
@Component
public class ChatSessionMemoryStore {

    private final Map<String, ChatMemory> sessions = new ConcurrentHashMap<>();

    @Value("${supportiq.chat.memory.max-messages}")
    private int maxMessages;

    public ChatMemory getOrCreate(String sessionId) {
        return sessions.computeIfAbsent(
                sessionId,
                id -> MessageWindowChatMemory.withMaxMessages(maxMessages)
        );
    }
}

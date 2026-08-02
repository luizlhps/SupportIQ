package com.worklyze.supportiq.feature.chat.domain.usecases;

import com.worklyze.supportiq.config.ai.AiProvider;
import com.worklyze.supportiq.feature.chat.shared.ChatAnswer;

public interface AskQuestionUseCase {
    ChatAnswer execute(AiProvider provider, String sessionId, String question);
}

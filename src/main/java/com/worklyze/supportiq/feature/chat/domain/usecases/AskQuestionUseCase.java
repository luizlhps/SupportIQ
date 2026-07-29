package com.worklyze.supportiq.feature.chat.domain.usecases;

import com.worklyze.supportiq.feature.chat.shared.ChatAnswer;

public interface AskQuestionUseCase {
    ChatAnswer execute(String sessionId, String question);
}

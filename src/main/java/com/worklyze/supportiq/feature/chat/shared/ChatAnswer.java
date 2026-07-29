package com.worklyze.supportiq.feature.chat.shared;

import java.util.List;

public record ChatAnswer(
        String sessionId,
        String answer,
        List<String> images
) {
}

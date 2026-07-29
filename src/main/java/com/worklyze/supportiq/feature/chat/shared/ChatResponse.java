package com.worklyze.supportiq.feature.chat.shared;

import java.util.List;

public record ChatResponse(
        String answer,
        String sessionId,
        List<String> images
) {
}

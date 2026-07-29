package com.worklyze.supportiq.feature.chat.shared;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "A pergunta não pode estar em branco.")
        String question,
        String sessionId
) {
}

package com.worklyze.supportiq.config.ai;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = "Provider de IA usado para chat e/ou geracao de embeddings.",
        enumAsRef = true,
        allowableValues = {"ollama", "openai", "gemini"},
        example = "ollama"
)
public enum AiProvider {

    OLLAMA,
    OPENAI,
    GEMINI;

    @JsonCreator
    public static AiProvider fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return AiProvider.valueOf(value.trim().toUpperCase());
    }
}

package com.worklyze.supportiq.feature.chat.shared;

import com.worklyze.supportiq.config.ai.AiProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Requisicao de chat com pergunta, sessao opcional e provider opcional.")
public record ChatRequest(

        @Schema(description = "Pergunta do usuario.", example = "Como resetar minha senha?", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "A pergunta não pode estar em branco.")
        String question,

        @Schema(description = "Identificador da sessao para manter contexto multi-turn. "
                + "Se omitido, uma nova sessao sera criada e retornada na resposta.",
                example = "9a1b7f60-2a3c-4b5d-9e21-2f9d1c9f0b11")
        String sessionId,

        @Schema(description = "Provider de IA. Se omitido, usa supportiq.ai.default-provider.")
        AiProvider provider
) {
}

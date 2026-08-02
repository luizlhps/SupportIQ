package com.worklyze.supportiq.feature.chat.shared;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resposta do chat com o texto gerado, sessao e imagens relevantes.")
public record ChatResponse(

        @Schema(description = "Texto da resposta gerada pelo modelo.")
        String answer,

        @Schema(description = "Sessao atual (nova ou continuada). Use no proximo request para manter contexto.")
        String sessionId,

        @Schema(description = "Caminhos das imagens da base de conhecimento associadas ao contexto usado.")
        List<String> images
) {
}

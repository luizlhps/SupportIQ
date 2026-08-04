package com.worklyze.supportiq.config.ai;

import com.worklyze.supportiq.shared.exceptions.ExceptionCode;
import lombok.Getter;

@Getter
public enum AiExceptionCode implements ExceptionCode {
    CHAT_MODEL_NOT_CONFIGURED("Chat model não configurado para o provider: %s. Configure a API key correspondente.", "CHAT_MODEL_NOT_CONFIGURED"),
    EMBEDDING_MODEL_NOT_CONFIGURED("Embedding model não configurado para o provider: %s. Configure a API key correspondente.", "EMBEDDING_MODEL_NOT_CONFIGURED"),
    VECTOR_STORE_NOT_CONFIGURED("Vector store não configurado para o provider: %s.", "VECTOR_STORE_NOT_CONFIGURED"),
    ;

    private String message;
    private String code;

    AiExceptionCode(String message, String code) {
        this.message = message;
        this.code = code;
    }

}

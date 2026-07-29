package com.worklyze.supportiq.feature.ingestion.shared;

import lombok.Getter;

@Getter
public enum IngestionExceptionCode {
    ACTIVITY_NOT_FOUND("Atividade não encontrada.", "ACTIVITY_NOT_FOUND"),
    ACTIVITY_ALREADY_ACTIVITY("Já existe uma atividade iniciada", "ACTIVITY_ALREADY_ACTIVITY"),
    ACTIVITY_INVALID_START_TIME("O horário de início não pode ser menor que o horário de término da atividade anterior.", "ACTIVITY_INVALID_START_TIME"),
    ;

    private String message;
    private String code;

    IngestionExceptionCode(String message, String code) {
        this.message = message;
        this.code = code;
    }

}

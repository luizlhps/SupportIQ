package com.worklyze.supportiq.feature.ingestion.shared;

import com.worklyze.supportiq.shared.exceptions.ExceptionCode;
import lombok.Getter;

@Getter
public enum IngestionExceptionCode implements ExceptionCode {
    PDF_PROCESSING_ERROR("Erro ao processar PDF: %s", "PDF_PROCESSING_ERROR"),
    FILE_PROCESSING_ERROR("Erro ao processar arquivo: %s", "FILE_PROCESSING_ERROR"),
    IMAGE_DIR_CREATE_ERROR("Erro ao criar diretório de imagens: %s", "IMAGE_DIR_CREATE_ERROR"),
    IMAGE_SAVE_ERROR("Erro ao salvar imagem: %s", "IMAGE_SAVE_ERROR"),
    IMAGE_FILE_DELETE_ERROR("Erro ao remover arquivo: %s", "IMAGE_FILE_DELETE_ERROR"),
    IMAGE_DIR_DELETE_ERROR("Erro ao remover diretório de imagens: %s", "IMAGE_DIR_DELETE_ERROR"),
    ;

    private String message;
    private String code;

    IngestionExceptionCode(String message, String code) {
        this.message = message;
        this.code = code;
    }

}

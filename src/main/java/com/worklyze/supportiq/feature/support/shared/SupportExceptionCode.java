package com.worklyze.supportiq.feature.support.shared;

import com.worklyze.supportiq.shared.exceptions.ExceptionCode;
import lombok.Getter;

@Getter
public enum SupportExceptionCode implements ExceptionCode {
    WHATSAPP_NOT_CONFIGURED("Suporte WhatsApp não configurado. Defina supportiq.support.whatsapp.phone.", "WHATSAPP_NOT_CONFIGURED"),
    ;

    private String message;
    private String code;

    SupportExceptionCode(String message, String code) {
        this.message = message;
        this.code = code;
    }

}

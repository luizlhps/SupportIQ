package com.worklyze.supportiq.shared.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BadRequestException extends CustomException {
    private final HttpStatus status;
    private final String code;

    public BadRequestException(String message, String code) {
        super(message);
        this.status = HttpStatus.valueOf(400);
        this.code = code;
    }

    public BadRequestException(ExceptionCode exceptionCode) {
        super(exceptionCode.getMessage());
        this.status = HttpStatus.valueOf(400);
        this.code = exceptionCode.getCode();
    }
}

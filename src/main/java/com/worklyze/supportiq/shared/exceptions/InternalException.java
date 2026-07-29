package com.worklyze.supportiq.shared.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InternalException extends CustomException {
    private final HttpStatus status;
    private final String code;

    public InternalException(String message, String code) {
        super(message);
        this.status = HttpStatus.valueOf(500);
        this.code = code;
    }

    public InternalException(ExceptionCode exceptionCode) {
        super(exceptionCode.getMessage());
        this.status = HttpStatus.valueOf(500);
        this.code = exceptionCode.getCode();
    }
}

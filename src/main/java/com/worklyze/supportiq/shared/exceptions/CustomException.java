package com.worklyze.supportiq.shared.exceptions;

import org.springframework.http.HttpStatus;

public class CustomException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public CustomException(String message) {
        super(message);
        this.status = HttpStatus.valueOf(500);
        this.code = "INTERNAL_SERVER_ERROR";
    }

    public CustomException(ExceptionCode ex) {
        super(ex.getMessage());
        this.status = HttpStatus.valueOf(500);
        this.code = "INTERNAL_SERVER_ERROR";
    }

    public CustomException(ExceptionCode ex, Throwable throwable) {
        super(ex.getMessage(), throwable);
        this.status = HttpStatus.valueOf(500);
        this.code = "INTERNAL_SERVER_ERROR";
    }

    public CustomException(String message, Throwable throwable) {
        super(message, throwable);
        this.status = HttpStatus.valueOf(500);
        this.code = "INTERNAL_SERVER_ERROR";
    }
}

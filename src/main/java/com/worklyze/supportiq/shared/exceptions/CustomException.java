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

}

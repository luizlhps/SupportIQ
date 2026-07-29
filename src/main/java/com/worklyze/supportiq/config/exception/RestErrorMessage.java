package com.worklyze.supportiq.config.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.http.HttpStatus;

import java.util.Map;

@AllArgsConstructor
@Getter
@Setter
@ToString
public class RestErrorMessage {
    @JsonProperty("status")
    private HttpStatus status;

    @JsonProperty("error_description")
    private String errorDescription;

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("stack")
    private String stack;

    @JsonProperty("fields_error")
    private Map<String, String> fieldsError = null;

    public RestErrorMessage(HttpStatus status, String message, String errorCode, Throwable throwable) {
        this.status = status;
        this.errorDescription = message;
        this.errorCode = errorCode;
        this.stack = throwable != null ? getStackTrace(throwable) : null;
    }

    public RestErrorMessage(HttpStatus httpStatus, String message, String errorCode, Throwable throwable, Map<String, String> fieldErrors) {
        this.status = httpStatus;
        this.errorDescription = message;
        this.errorCode = errorCode;
        this.stack = null;
        this.fieldsError = fieldErrors;
    }

    @JsonProperty("status")
    public int getStatus() {
        return status.value();
    }

    private String getStackTrace(Throwable throwable) {
        return throwable.getMessage();
    }

}


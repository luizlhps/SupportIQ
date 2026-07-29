package com.worklyze.supportiq.config.exception;

import com.worklyze.supportiq.shared.exceptions.CustomException;
import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;

@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ResponseEntity<RestErrorMessage> handleNotFound(NoHandlerFoundException ex) {
        RestErrorMessage errorResponse = new RestErrorMessage(HttpStatus.NOT_FOUND, ex.getMessage(), "NOT_FOUND", ex);
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<RestErrorMessage> handleCustomException(CustomException ex) {
        RestErrorMessage errorResponse = new RestErrorMessage(ex.getStatus(), ex.getMessage(), ex.getCode(), ex);
        return new ResponseEntity<RestErrorMessage>(errorResponse, ex.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestErrorMessage> handleCustomException(MethodArgumentNotValidException ex) {

        var errors = ex.getFieldErrors();
        var fields = new HashMap<String, String>();

        errors.forEach(e -> {
            fields.put(e.getField(), e.getDefaultMessage());
        });

        RestErrorMessage errorResponse = new RestErrorMessage(HttpStatus.BAD_REQUEST, "Erro de validação", "BAD_REQUEST", ex, fields);

        return new ResponseEntity<RestErrorMessage>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestErrorMessage> handleGeneralException(Exception ex) {

        RestErrorMessage errorResponse = new RestErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno.", "INTERNAL_SERVER_ERROR", ex);
        logger.error("Erro interno não tratado: {}", ex.getMessage(), ex);

        return new ResponseEntity<RestErrorMessage>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }


}
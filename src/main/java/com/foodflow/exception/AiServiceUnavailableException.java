package com.foodflow.exception;

import org.springframework.http.HttpStatus;

public class AiServiceUnavailableException extends ApiException {

    public AiServiceUnavailableException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE);
    }

    public AiServiceUnavailableException(String message, Throwable cause) {
        super(message, cause, HttpStatus.SERVICE_UNAVAILABLE);
    }
}

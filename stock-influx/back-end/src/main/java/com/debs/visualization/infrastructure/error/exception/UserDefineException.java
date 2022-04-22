package com.debs.visualization.infrastructure.error.exception;

import lombok.Getter;

@Getter
public class UserDefineException extends RuntimeException {

    private String originalErrorMessage;

    public UserDefineException(String message) {
        super(message);
    }

    public UserDefineException(String message, String originalErrorMessage) {
        super(message);
        this.originalErrorMessage = originalErrorMessage;
    }
}

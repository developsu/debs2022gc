package com.debs.visualization.infrastructure.error.exception;

public class BadRequestException extends BusinessLogicException {

    public BadRequestException(String message) {
        super(String.format("Bad request = %s", message));
    }
}

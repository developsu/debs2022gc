package com.debs.visualization.infrastructure.error.exception;

import com.debs.visualization.infrastructure.error.model.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessLogicException extends RuntimeException {

    private ErrorCode errorCode;

    public BusinessLogicException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessLogicException(String message) {
        super(message);
    }
}

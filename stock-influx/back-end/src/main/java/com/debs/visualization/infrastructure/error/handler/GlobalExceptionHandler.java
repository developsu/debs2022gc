package com.debs.visualization.infrastructure.error.handler;

import com.debs.visualization.infrastructure.error.exception.BusinessLogicException;
import com.debs.visualization.infrastructure.error.exception.UserDefineException;
import com.debs.visualization.infrastructure.error.model.ErrorCode;
import com.debs.visualization.infrastructure.http.ResponseFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseFormat<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult()
                               .getAllErrors()
                               .get(0)
                               .getDefaultMessage();
        log.warn(errorMessage);
        return new ResponseEntity<>(
            ResponseFormat.fail(ErrorCode.INVALID_INPUT_VALUE, errorMessage),
            HttpStatus.valueOf(ErrorCode.INVALID_INPUT_VALUE.getStatusCode())
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ResponseFormat<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn(e.getMessage());
        return new ResponseEntity<>(
            ResponseFormat.fail(ErrorCode.INVALID_TYPE_VALUE, e.getMessage()),
            HttpStatus.valueOf(ErrorCode.INVALID_TYPE_VALUE.getStatusCode())
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResponseFormat<Void>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn(e.getMessage());
        return new ResponseEntity<>(
            ResponseFormat.fail(ErrorCode.METHOD_NOT_ALLOWED, e.getMessage()),
            HttpStatus.METHOD_NOT_ALLOWED
        );
    }

    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<ResponseFormat<Void>> handleRuntimeException(BusinessLogicException e) {
        log.warn(e.getMessage());
        final ErrorCode errorCode = e.getErrorCode();
        if (errorCode == null) {
            return new ResponseEntity<>(
                ResponseFormat.fail(e.getMessage()),
                HttpStatus.BAD_REQUEST
            );
        }
        return new ResponseEntity<>(
            ResponseFormat.fail(errorCode, e.getMessage()),
            HttpStatus.valueOf(errorCode.getStatusCode())
        );
    }

    @ExceptionHandler(UserDefineException.class)
    public ResponseEntity<ResponseFormat<Void>> handleUserDefineException(UserDefineException e) {
        log.warn(e.getMessage());
        return new ResponseEntity<>(
            ResponseFormat.fail(e.getMessage()),
            HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ResponseFormat<Void>> handleIllegalStateException(IllegalStateException e) {
        log.error(e.getMessage());
        return new ResponseEntity<>(
            ResponseFormat.fail(ErrorCode.SERVER_ERROR, e.getMessage()),
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseFormat<Void>> handleException(Exception e) {
        log.error(e.getMessage());
        return new ResponseEntity<>(
            ResponseFormat.fail(ErrorCode.SERVER_ERROR, e.getMessage()),
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}

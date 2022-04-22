package com.debs.visualization.infrastructure.http;

import com.debs.visualization.infrastructure.error.exception.UserDefineException;
import com.debs.visualization.infrastructure.error.model.ErrorCode;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseFormat<T> {

    @ApiModelProperty(value = "성공은 1 or 실패는 2", example = "1")
    private int code;

    private T data;

    @ApiModelProperty(value = "부가설명", example = "요청이 성공적으로 처리되었습니다.")
    private String description;

    public static ResponseFormat<Void> ok() {
        return ResponseFormat.<Void>builder()
                             .code(ResponseCode.SUCCESS.getCode())
                             .data(null)
                             .description("요청이 성공적으로 처리되었습니다.")
                             .build();
    }

    public static <T> ResponseFormat<T> ok(T data) {
        return ResponseFormat.<T>builder()
                             .code(ResponseCode.SUCCESS.getCode())
                             .data(data)
                             .description("요청이 성공적으로 처리되었습니다.")
                             .build();
    }

    public static ResponseFormat<Void> fail(String message) {
        return ResponseFormat.<Void>builder()
                             .code(ResponseCode.FAIL.getCode())
                             .data(null)
                             .description(message)
                             .build();
    }

    public static ResponseFormat<Void> fail(ErrorCode errorCode, String originalErrorMessage) {
        return ResponseFormat.<Void>builder()
                             .code(ResponseCode.FAIL.getCode())
                             .data(null)
                             .description(errorCode.toString() + " : " + originalErrorMessage)
                             .build();
    }

    // for exception handler
    public static ResponseFormat<?> of(Object response) {
        try {
            if (response instanceof ResponseEntity) {
                response = ((ResponseEntity<?>)response).getBody();
            }
            return (ResponseFormat<?>)response;
        } catch (Exception e) {
            throw new UserDefineException("ResponseFormat 형변환 과정에서 문제가 발생했습니다. " + e.getMessage());
        }
    }
}

package com.debs.visualization.infrastructure.http;

import com.debs.visualization.infrastructure.error.exception.BusinessLogicException;
import java.util.Arrays;
import lombok.Getter;

@Getter
public enum ResponseCode {

    SUCCESS(1), FAIL(2);

    private final int code;

    ResponseCode(int code) {
        this.code = code;
    }

    public static ResponseCode of(int code) {
        return Arrays.stream(ResponseCode.values())
                     .filter(responseCode -> responseCode.getCode() == code)
                     .findFirst()
                     .orElseThrow(() -> new BusinessLogicException("응답 코드를 찾을 수 없습니다."));
    }
}

package com.debs.visualization.infrastructure.error.model;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // Common
    SERVER_ERROR("CO_001", "서버가 응답하지 않습니다.", 500),
    INVALID_INPUT_VALUE("CO_002", "유효하지 않은 입력입니다.", 400),
    INVALID_TYPE_VALUE("CO_003", "형식에 맞지 않는 입력입니다.", 400),
    METHOD_NOT_ALLOWED("CO_004", "사용할 수 없는 API 입니다.", 405),

    // Google Drive
    DOWNLOAD_ERROR("GD_001", "Google Drive에서 파일을 다운로드 받는 과정에서 문제가 발생했습니다.", 400),

    // InfluxDB
    INSERTION_QUERY_ERROR("IF_001", "InfluxDB에 Insert 쿼리 실행 과정 중 문제가 발생했습니다.", 400),
    SELECTION_QUERY_ERROR("IF_002", "InfluxDB에 Select 쿼리 실행 과정 중 문제가 발생했습니다.", 400);

    private final String code;
    private final String message;
    private final Integer statusCode;

    ErrorCode(String code, String message, Integer statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", code, message);
    }
}

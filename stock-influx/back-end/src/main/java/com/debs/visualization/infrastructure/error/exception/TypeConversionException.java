package com.debs.visualization.infrastructure.error.exception;

public class TypeConversionException extends RuntimeException {

    private String from;
    private String to;

    public TypeConversionException(String from, String to) {
        super(from + "에서 " + to + "로 형변환 과정에서 문제가 발생했습니다.");
        this.from = from;
        this.to = to;
    }
}

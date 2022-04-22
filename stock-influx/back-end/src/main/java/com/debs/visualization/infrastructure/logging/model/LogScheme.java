package com.debs.visualization.infrastructure.logging.model;

import com.debs.visualization.infrastructure.logging.util.ServletExtractor.Servlets;
import com.debs.visualization.infrastructure.http.ResponseCode;
import com.debs.visualization.infrastructure.http.ResponseFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
@Getter
public class LogScheme {

    private String type;
    private String uri;
    private String http_method;
    private String response_code;
    private String response_description;
    private String method_name;

    public LogScheme(LogType type, Servlets servlets, ResponseFormat<?> response, String methodName) {
        this.type = type.toString();
        this.uri = servlets.getRequest().getRequestURI();
        this.http_method = servlets.getRequest().getMethod();
        this.response_code = ResponseCode.of(response.getCode()).toString();
        this.response_description = response.getDescription();
        this.method_name = methodName;
    }

    @Override
    public String toString() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "LogScheme Json Error";
        }
    }
}

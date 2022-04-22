package com.debs.visualization.infrastructure.logging.util;

import com.debs.visualization.infrastructure.error.exception.NotFoundException;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ServletExtractor {

    @Getter
    @Builder
    public static class Servlets {
        HttpServletRequest request;
        HttpServletResponse response;
    }

    public static Servlets get(RequestAttributes requestAttributes) {
        final ServletRequestAttributes servletRequestAttributes = Optional.of((ServletRequestAttributes)requestAttributes)
                                                                          .orElseThrow(() -> new NotFoundException("ServletRequestAttributes"));

        return Servlets.builder()
                       .request(servletRequestAttributes.getRequest())
                       .response(servletRequestAttributes.getResponse())
                       .build();
    }
}

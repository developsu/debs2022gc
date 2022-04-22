package com.debs.visualization.domain.influxdb.model;

import com.debs.visualization.infrastructure.error.exception.TypeConversionException;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

@Getter
@Setter
@Builder
@Measurement(name = "debs_measurement")
public class DEBSMeasurement {

    @Column(name = "symbol")
    private String symbol;

    @Column(name = "last_trade_price")
    private Double lastTradePrice;

    @Column(name = "last_trade_time")
    private Instant lastTradeTime;

    public static ResponseDto toDto(List<Object> point) {
        try {
            return ResponseDto.builder()
                              .lastTradeTime(((String)point.get(0)))
                              .lastTradePrice((Double)point.get(1))
                              .build();
        } catch (Exception e) {
            throw new TypeConversionException("List", "ResponseDto");
        }
    }
}

package com.debs.visualization.domain.influxdb.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseDto {

    private Double lastTradePrice;
    private String lastTradeTime;
}

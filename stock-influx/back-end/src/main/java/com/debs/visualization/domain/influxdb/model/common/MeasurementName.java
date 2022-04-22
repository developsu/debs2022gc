package com.debs.visualization.domain.influxdb.model.common;

import lombok.Getter;

@Getter
public enum MeasurementName {

    DEBS_MEASUREMENT("debs_measurement");

    String name;

    MeasurementName(String name) {
        this.name = name;
    }
}

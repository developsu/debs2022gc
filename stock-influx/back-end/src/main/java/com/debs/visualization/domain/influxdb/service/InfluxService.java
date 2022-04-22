package com.debs.visualization.domain.influxdb.service;

import static com.debs.visualization.domain.influxdb.model.common.MeasurementName.DEBS_MEASUREMENT;

import com.debs.visualization.domain.csv.service.CsvService;
import com.debs.visualization.domain.influxdb.model.DEBSMeasurement;
import com.debs.visualization.domain.influxdb.model.ResponseDto;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.dto.BoundParameterQuery.QueryBuilder;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.dto.QueryResult.Series;
import org.springframework.data.influxdb.InfluxDBTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class InfluxService {

    private final InfluxDBTemplate<Point> template;
    private final CsvService csvService;

    public void insert(List<List<String>> readData) {
        // in List<String> ...
        // [1] symbol, [3] lasttradeprice, [5] seconds, [6] nanos
        for (List<String> data : readData) {
            final Point point = Point.measurement(DEBS_MEASUREMENT.getName())
                                     .tag("symbol", data.get(1))
                                     .addField("last_trade_price", Double.parseDouble(data.get(3)))
                                     .time(Instant.ofEpochSecond(Long.parseLong(data.get(5)), Long.parseLong(data.get(6))).toEpochMilli(), TimeUnit.MILLISECONDS)
                                     .build();
            template.write(point);
        }
    }

    public List<ResponseDto> select(final String symbol) {
        final Query query = QueryBuilder
            .newQuery(String.format("SELECT time, last_trade_price FROM %s WHERE symbol='%s' ORDER BY time ASC", DEBS_MEASUREMENT.getName(), symbol))
            .forDatabase("debs_db")
            .create();
        return getResponseDtoList(query);
    }

    public List<ResponseDto> selectIf(final String symbol, final String lastTradePrice) {
        final Query query = QueryBuilder
            .newQuery(String.format("SELECT time, last_trade_price FROM %s WHERE symbol='%s' AND time>'%s' ORDER BY time ASC", DEBS_MEASUREMENT.getName(), symbol, lastTradePrice))
            .forDatabase("debs_db")
            .create();
        return getResponseDtoList(query);
    }

    public List<String> getSymbolList() {
        final Query query = QueryBuilder
            .newQuery("SHOW TAG VALUES WITH key=symbol")
            .forDatabase("debs_db")
            .create();

        final QueryResult queryResult = template.query(query);

        final List<Series> seriesList = queryResult.getResults()
                                                   .get(0).getSeries();

        if (seriesList == null) {
            return Collections.emptyList();
        }

        final List<List<Object>> rawResult = seriesList.get(0).getValues();

        return rawResult.stream()
                        .parallel()
                        .map(ls -> ls.get(1))
                        .map(Object::toString)
                        .collect(Collectors.toList());
    }

    private List<ResponseDto> getResponseDtoList(Query query) {
        final QueryResult queryResult = template.query(query);

        final List<Series> seriesList = queryResult.getResults()
                                                   .get(0).getSeries();

        if (seriesList == null) {
            return Collections.emptyList();
        }

        final List<List<Object>> result = seriesList.get(0).getValues();

        return result.stream()
                     .map(DEBSMeasurement::toDto)
                     .collect(Collectors.toList());
    }
}

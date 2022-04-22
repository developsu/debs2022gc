package com.debs.visualization.interfaces.controller;

import com.debs.visualization.domain.googledrive.service.GoogleDriveService;
import com.debs.visualization.domain.influxdb.model.ResponseDto;
import com.debs.visualization.domain.influxdb.service.InfluxService;
import com.debs.visualization.infrastructure.http.ResponseFormat;
import io.swagger.annotations.ApiOperation;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/v1/api")
@RestController
public class InfluxDBController {

    private final InfluxService influxService;
    private final GoogleDriveService googleDriveService;

    @ApiOperation("Google Drive의 최근 1,000개의 데이터를 중복 제거 후 DB에 적재")
    @PostMapping("/data")
    public ResponseFormat<Void> updateDatasetFromGoogleDrive() {
        log.info(">>> 파일 적재 시작 : {}", LocalDateTime.now());
        googleDriveService.updateDatasetFromGoogleDrive();
        log.info(">>> 파일 적재 완료 : {}", LocalDateTime.now());
        return ResponseFormat.ok();
    }

    @ApiOperation("DB의 데이터를 종목별로 조회")
    @GetMapping("/symbols/{symbol}")
    public ResponseFormat<List<ResponseDto>> select(@PathVariable(name = "symbol") String symbol) {
        return ResponseFormat.ok(influxService.select(symbol));
    }

    @ApiOperation("DB의 데이터를 종목별로 조회, 시간 조건 추가 가능")
    @GetMapping("/symbols/{symbol}/{lastTradeTime}")
    public ResponseFormat<List<ResponseDto>> select(
        @PathVariable(name = "symbol") String symbol,
        @PathVariable(name = "lastTradeTime") String lastTradeTime
    ) {
        return ResponseFormat.ok(influxService.selectIf(symbol, lastTradeTime));
    }

    @ApiOperation("DB의 모든 종목명 조회")
    @GetMapping("/symbols")
    public ResponseFormat<List<String>> getSymbolList() {
        return ResponseFormat.ok(influxService.getSymbolList());
    }
}

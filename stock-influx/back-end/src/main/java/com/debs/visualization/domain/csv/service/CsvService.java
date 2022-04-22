package com.debs.visualization.domain.csv.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CsvService {

    public List<List<String>> csvToList(String csv) {
        final String[] csvRows = csv.split("\r\n");
        return Arrays.stream(csvRows)
                     .skip(1) // field names
                     .map(s -> s.split(","))
                     .map(Arrays::asList)
                     .collect(Collectors.toList());
    }
}

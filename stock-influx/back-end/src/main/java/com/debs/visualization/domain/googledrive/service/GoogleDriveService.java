package com.debs.visualization.domain.googledrive.service;

import com.debs.visualization.domain.csv.service.CsvService;
import com.debs.visualization.domain.googledrive.model.StoredFileIdSet;
import com.debs.visualization.domain.influxdb.service.InfluxService;
import com.debs.visualization.infrastructure.error.exception.BadRequestException;
import com.debs.visualization.infrastructure.error.exception.UserDefineException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class GoogleDriveService {

    private static final String MIME_TYPE = "application/vnd.google-apps.folder";
    private static final String DATASET_SHARED_FOLDER_NAME = "nano_second_stream";
    private static final int PAGE_SIZE = 1_000; // MAX_PAGE_SIZE

    private final Drive drive;
    private final InfluxService influxService;
    private final CsvService csvService;

    public void updateDatasetFromGoogleDrive() {
        final List<String> fileIdList = findFileIdList().stream()
                                                        .filter(StoredFileIdSet::isNotStored)
                                                        .collect(Collectors.toList());

        final int totalSize = fileIdList.size();
        for (int i = 0; i < totalSize; i++) {
            log.info("processing ... ({} / {}) ___ {}", i + 1, totalSize, LocalDateTime.now());
            final String fileId = fileIdList.get(i);
            final String rawCsvString = downloadOnMemory(fileId);
            final List<List<String>> csvList = csvService.csvToList(rawCsvString);
            influxService.insert(csvList);
            StoredFileIdSet.addFileId(fileId);
        }
    }

    public String downloadOnMemory(String fileId) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            drive.files().get(fileId).executeMediaAndDownloadTo(outputStream);
            return String.valueOf(outputStream);
        } catch (IOException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    public List<String> findFileIdList() {
        try {
            final String query = String.format("mimeType = '%s' and name contains '%s'", MIME_TYPE, DATASET_SHARED_FOLDER_NAME);
            final FileList result = drive.files().list()
                                         .setQ(query)
                                         .setSpaces("drive")
                                         .execute();

            final List<File> files = result.getFiles();
            if (files.isEmpty()) {
                throw new UserDefineException("공유 폴더 " + DATASET_SHARED_FOLDER_NAME + " 을 찾지 못했습니다.");
            }

            final String folderId = files.get(0).getId();

            final String folderQuery = String.format("parents in '%s'", folderId);
            final FileList folderResult = drive.files().list()
                                               .setQ(folderQuery)
                                               .setPageSize(PAGE_SIZE)
                                               .execute();

            return folderResult.getFiles().stream()
                               .map(File::getId)
                               .collect(Collectors.toList());
        } catch (IOException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}

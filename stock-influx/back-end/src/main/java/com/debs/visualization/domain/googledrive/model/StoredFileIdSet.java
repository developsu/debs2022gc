package com.debs.visualization.domain.googledrive.model;

import java.util.HashSet;
import java.util.Set;

public class StoredFileIdSet {

    private static final Set<String> fileIdSet = new HashSet<>();

    public static boolean isNotStored(String fileId) {
        return !fileIdSet.contains(fileId);
    }

    public static void addFileId(String fileId) {
        fileIdSet.add(fileId);
    }
}

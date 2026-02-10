package com.filemanager.backend.service;

import java.io.File;
import java.util.List;

public interface FileFilterService {
    boolean isFileIncluded(File file);
    boolean isFileFiltered(File file);
    List<String> getScanFilterList();
    void addScanFilter(String filter);
    void removeScanFilter(String filter);
    void clearScanFilters();
}

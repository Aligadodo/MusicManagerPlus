package com.filemanager.domain.service;

import java.util.List;
import java.util.Map;

public interface LogService {
    List<Map<String, Object>> getLogFiles();

    Map<String, Object> getLogEntries(String fileName, String keyword, int page, int size);

    Map<String, Object> downloadLogFile(String fileName);

    Map<String, Object> clearOldLogs(int days);

    String getLogDirectory();
}

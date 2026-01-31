package com.filemanager.backend.logging;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogManager {
    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE_PREFIX = "filemanager";

    public static String getCurrentLogFileName() {
        return LOG_DIR + "/" + LOG_FILE_PREFIX + "_" + LocalDateTime.now().format(FILE_NAME_FORMATTER) + ".log";
    }

    public static String getLogFileName(LocalDateTime dateTime) {
        return LOG_DIR + "/" + LOG_FILE_PREFIX + "_" + dateTime.format(FILE_NAME_FORMATTER) + ".log";
    }

    public static String formatTimestamp(LocalDateTime dateTime) {
        return dateTime.format(TIMESTAMP_FORMATTER);
    }
}

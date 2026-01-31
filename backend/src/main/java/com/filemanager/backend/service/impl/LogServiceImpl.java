package com.filemanager.backend.service.impl;

import com.filemanager.domain.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class LogServiceImpl implements LogService {
    private static final Logger logger = LoggerFactory.getLogger(LogServiceImpl.class);

    @Value("${app.log.directory:logs}")
    private String logDirectory;

    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public List<Map<String, Object>> getLogFiles() {
        List<Map<String, Object>> logFiles = new ArrayList<>();
        Path logDir = Paths.get(logDirectory);

        if (!Files.exists(logDir)) {
            logger.warn("Log directory does not exist: {}", logDirectory);
            return logFiles;
        }

        try (Stream<Path> paths = Files.list(logDir)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".log"))
                 .sorted((p1, p2) -> {
                     try {
                         return Files.getLastModifiedTime(p2).compareTo(Files.getLastModifiedTime(p1));
                     } catch (IOException e) {
                         return 0;
                     }
                 })
                 .forEach(path -> {
                     Map<String, Object> fileInfo = new HashMap<>();
                     fileInfo.put("fileName", path.getFileName().toString());
                     fileInfo.put("fileSize", formatFileSize(path.toFile().length()));
                     fileInfo.put("createTime", formatFileTime(path));
                     logFiles.add(fileInfo);
                 });
        } catch (IOException e) {
            logger.error("Failed to list log files", e);
        }

        return logFiles;
    }

    @Override
    public Map<String, Object> getLogEntries(String fileName, String keyword, int page, int size) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> entries = new ArrayList<>();

        Path logFile = Paths.get(logDirectory, fileName);
        if (!Files.exists(logFile)) {
            result.put("success", false);
            result.put("message", "Log file not found: " + fileName);
            return result;
        }

        try {
            List<String> lines = Files.readAllLines(logFile);
            List<String> filteredLines = lines;

            if (keyword != null && !keyword.isEmpty()) {
                final String lowerKeyword = keyword.toLowerCase();
                filteredLines = lines.stream()
                        .filter(line -> line.toLowerCase().contains(lowerKeyword))
                        .collect(Collectors.toList());
            }

            int total = filteredLines.size();
            int totalPages = (int) Math.ceil((double) total / size);

            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, total);

            if (startIndex < total) {
                for (int i = startIndex; i < endIndex; i++) {
                    Map<String, Object> entry = parseLogEntry(filteredLines.get(i));
                    entries.add(entry);
                }
            }

            result.put("success", true);
            result.put("entries", entries);
            result.put("total", total);
            result.put("pages", totalPages);
            result.put("page", page);
            result.put("size", size);
        } catch (IOException e) {
            logger.error("Failed to read log file: {}", fileName, e);
            result.put("success", false);
            result.put("message", "Failed to read log file: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> downloadLogFile(String fileName) {
        Map<String, Object> result = new HashMap<>();
        Path logFile = Paths.get(logDirectory, fileName);

        if (!Files.exists(logFile)) {
            result.put("success", false);
            result.put("message", "Log file not found: " + fileName);
            return result;
        }

        try {
            result.put("success", true);
            result.put("message", "Log file ready for download");
            result.put("filePath", logFile.toString());
            result.put("fileSize", logFile.toFile().length());
        } catch (Exception e) {
            logger.error("Failed to prepare log file for download: {}", fileName, e);
            result.put("success", false);
            result.put("message", "Failed to prepare log file: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> clearOldLogs(int days) {
        Map<String, Object> result = new HashMap<>();
        Path logDir = Paths.get(logDirectory);

        if (!Files.exists(logDir)) {
            result.put("success", false);
            result.put("message", "Log directory does not exist");
            return result;
        }

        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(days);
            int deletedCount = 0;

            try (Stream<Path> paths = Files.list(logDir)) {
                List<Path> toDelete = paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".log"))
                        .filter(path -> {
                            try {
                                LocalDateTime fileTime = Files.getLastModifiedTime(path)
                                        .toInstant()
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDateTime();
                                return fileTime.isBefore(cutoffTime);
                            } catch (IOException e) {
                                return false;
                            }
                        })
                        .collect(Collectors.toList());

                for (Path path : toDelete) {
                    Files.delete(path);
                    deletedCount++;
                    logger.info("Deleted old log file: {}", path.getFileName());
                }
            }

            result.put("success", true);
            result.put("message", "Deleted " + deletedCount + " log files older than " + days + " days");
            result.put("deletedCount", deletedCount);
        } catch (IOException e) {
            logger.error("Failed to clear old logs", e);
            result.put("success", false);
            result.put("message", "Failed to clear old logs: " + e.getMessage());
        }

        return result;
    }

    private Map<String, Object> parseLogEntry(String line) {
        Map<String, Object> entry = new HashMap<>();

        try {
            String[] parts = line.split("\\s+", 5);
            if (parts.length >= 5) {
                String timestamp = parts[0] + " " + parts[1];
                String level = parts[2];
                String thread = parts[3];
                String message = parts[4];

                entry.put("timestamp", timestamp);
                entry.put("level", level);
                entry.put("thread", thread);
                entry.put("message", message);

                if (message.contains("Exception") || message.contains("Error")) {
                    entry.put("stackTrace", extractStackTrace(line));
                }
            } else {
                entry.put("timestamp", "");
                entry.put("level", "INFO");
                entry.put("message", line);
            }
        } catch (Exception e) {
            entry.put("timestamp", "");
            entry.put("level", "INFO");
            entry.put("message", line);
        }

        return entry;
    }

    private String extractStackTrace(String line) {
        int stackIndex = line.indexOf("at ");
        if (stackIndex > 0) {
            return line.substring(stackIndex);
        }
        return null;
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    private String formatFileTime(Path path) {
        try {
            LocalDateTime time = Files.getLastModifiedTime(path)
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            return time.format(TIMESTAMP_FORMATTER);
        } catch (IOException e) {
            return "Unknown";
        }
    }
}

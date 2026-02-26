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
import java.util.concurrent.atomic.AtomicInteger;
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
                 .filter(path -> {
                     String fileName = path.getFileName().toString().toLowerCase();
                     return fileName.endsWith(".log") || fileName.endsWith(".txt");
                 })
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
                    fileInfo.put("fileSize", path.toFile().length());
                    fileInfo.put("lastModified", formatFileTime(path));
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

            // 处理多行堆栈信息
            List<Map<String, Object>> processedEntries = new ArrayList<>();
            Map<String, Object> currentEntry = null;
            StringBuilder currentStackTrace = new StringBuilder();

            for (String line : filteredLines) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                // 检查是否是新的日志条目
                boolean isNewEntry = false;
                
                // 1. 标准Spring Boot日志格式（带时间戳）
                if (line.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}.*")) {
                    isNewEntry = true;
                }

                if (isNewEntry) {
                    // 保存当前条目
                    if (currentEntry != null) {
                        if (currentStackTrace.length() > 0) {
                            currentEntry.put("stackTrace", currentStackTrace.toString().trim().replaceAll("\\n", " "));
                        }
                        processedEntries.add(currentEntry);
                    }

                    // 开始新条目
                    currentEntry = parseLogEntry(line);
                    currentStackTrace.setLength(0);

                    // 检查是否是错误条目或包含堆栈信息的开始
                } else if (currentEntry != null) {
                    // 检查是否是堆栈信息的延续
                    if (line.trim().startsWith("at ") || 
                        line.trim().startsWith("Caused by:") || 
                        line.trim().startsWith("StackTrace:") || 
                        line.trim().startsWith("    at ") ||
                        line.trim().startsWith("        at ")) {
                        currentStackTrace.append(line.trim()).append(" ");
                    } else if (currentStackTrace.length() > 0) {
                        // 堆栈信息结束
                        currentEntry.put("stackTrace", currentStackTrace.toString().trim().replaceAll("\\n", " "));
                        processedEntries.add(currentEntry);
                        currentEntry = null;
                        currentStackTrace.setLength(0);
                    }
                }
            }

            // 保存最后一个条目
            if (currentEntry != null) {
                if (currentStackTrace.length() > 0) {
                    currentEntry.put("stackTrace", currentStackTrace.toString().trim().replaceAll("\\n", " "));
                }
                processedEntries.add(currentEntry);
            }

            // 过滤和分页
            List<Map<String, Object>> finalEntries = processedEntries;
            if (keyword != null && !keyword.isEmpty()) {
                final String lowerKeyword = keyword.toLowerCase();
                finalEntries = processedEntries.stream()
                        .filter(entry -> {
                            String message = entry.get("message") != null ? entry.get("message").toString() : "";
                            String stackTrace = entry.get("stackTrace") != null ? entry.get("stackTrace").toString() : "";
                            return message.toLowerCase().contains(lowerKeyword) || stackTrace.toLowerCase().contains(lowerKeyword);
                        })
                        .collect(Collectors.toList());
            }

            // 反转日志顺序，让最新的日志在最前面
            Collections.reverse(finalEntries);

            int total = finalEntries.size();
            int totalPages = (int) Math.ceil((double) total / size);

            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, total);

            if (startIndex < total) {
                entries = finalEntries.subList(startIndex, endIndex);
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
                        .filter(path -> {
                            String fileName = path.getFileName().toString().toLowerCase();
                            return fileName.endsWith(".log") || fileName.endsWith(".txt");
                        })
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

    @Override
    public Map<String, Object> deleteLogFile(String fileName) {
        Map<String, Object> result = new HashMap<>();
        Path logFile = Paths.get(logDirectory, fileName);

        if (!Files.exists(logFile)) {
            result.put("success", false);
            result.put("message", "Log file not found: " + fileName);
            return result;
        }

        try {
            Files.delete(logFile);
            logger.info("Deleted log file: {}", fileName);
            result.put("success", true);
            result.put("message", "Log file deleted successfully: " + fileName);
        } catch (IOException e) {
            logger.error("Failed to delete log file: {}", fileName, e);
            result.put("success", false);
            result.put("message", "Failed to delete log file: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> clearAllLogs() {
        Map<String, Object> result = new HashMap<>();
        Path logDir = Paths.get(logDirectory);

        if (!Files.exists(logDir)) {
            result.put("success", false);
            result.put("message", "Log directory does not exist");
            return result;
        }

        try {
            int deletedCount = 0;
            AtomicInteger skippedCount = new AtomicInteger(0);

            try (Stream<Path> paths = Files.list(logDir)) {
                List<Path> toDelete = paths.filter(Files::isRegularFile)
                        .filter(path -> {
                            String fileName = path.getFileName().toString().toLowerCase();
                            return fileName.endsWith(".log") || fileName.endsWith(".txt");
                        })
                        .filter(path -> {
                            // 跳过最新的不带时间或序号尾缀的日志文件
                            String fileName = path.getFileName().toString();
                            // 检查文件名是否带有时间或序号尾缀
                            // 假设时间尾缀格式为yyyy-MM-dd或类似格式
                            // 假设序号尾缀格式为-数字
                            if (fileName.matches("^[^-]+\\.(log|txt)$")) {
                                // 不带时间或序号尾缀的文件，跳过
                                skippedCount.incrementAndGet();
                                logger.info("Skipped active log file: {}", fileName);
                                return false;
                            }
                            return true;
                        })
                        .collect(Collectors.toList());

                for (Path path : toDelete) {
                    Files.delete(path);
                    deletedCount++;
                    logger.info("Deleted log file: {}", path.getFileName());
                }
            }

            result.put("success", true);
            result.put("message", "Deleted " + deletedCount + " log files, skipped " + skippedCount.get() + " active log files");
            result.put("deletedCount", deletedCount);
            result.put("skippedCount", skippedCount.get());
        } catch (IOException e) {
            logger.error("Failed to clear all logs", e);
            result.put("success", false);
            result.put("message", "Failed to clear all logs: " + e.getMessage());
        }

        return result;
    }

    @Override
    public String getLogDirectory() {
        return logDirectory;
    }

    private Map<String, Object> parseLogEntry(String line) {
        Map<String, Object> entry = new HashMap<>();

        try {
            String timestamp = "";
            String level = "INFO";
            String message = line;

            int timestampEnd = line.indexOf(']');
            if (timestampEnd > 0) {
                timestamp = line.substring(0, timestampEnd + 1);
                
                String remaining = line.substring(timestampEnd + 1).trim();
                
                int levelEnd = remaining.indexOf(' ');
                if (levelEnd > 0) {
                    level = remaining.substring(0, levelEnd);
                    message = remaining.substring(levelEnd + 1);
                }
            }

            entry.put("timestamp", timestamp);
            entry.put("level", level);
            entry.put("message", message);

            if (message.contains("Exception") || message.contains("Error")) {
                entry.put("stackTrace", extractStackTrace(line));
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

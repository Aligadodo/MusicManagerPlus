package com.filemanager.plugin.util;

import com.filemanager.domain.dto.PreconditionDTO;
import com.filemanager.domain.dto.PreconditionGroupDTO;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class PreconditionEvaluator {

    private static final Set<String> AUDIO_EXTENSIONS = new HashSet<>(Arrays.asList(
        "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "opus"
    ));

    private static final Set<String> VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList(
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp"
    ));

    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
        "jpg", "jpeg", "png", "gif", "bmp", "tiff", "webp", "svg", "ico"
    ));

    private static final Set<String> TEXT_EXTENSIONS = new HashSet<>(Arrays.asList(
        "txt", "csv", "md", "json", "xml", "html", "css", "js", "ts", "py", "java", "c", "cpp", "h", "sh", "bat"
    ));

    private static final Set<String> DOCUMENT_EXTENSIONS = new HashSet<>(Arrays.asList(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "rtf"
    ));

    private static final Set<String> ARCHIVE_EXTENSIONS = new HashSet<>(Arrays.asList(
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "cab", "iso"
    ));

    public static boolean evaluate(File file, List<PreconditionGroupDTO> preconditionGroups) {
        if (preconditionGroups == null || preconditionGroups.isEmpty()) {
            return true;
        }

        for (PreconditionGroupDTO group : preconditionGroups) {
            if (evaluateGroup(file, group)) {
                return true;
            }
        }

        return false;
    }

    private static boolean evaluateGroup(File file, PreconditionGroupDTO group) {
        if (group == null || group.getPreconditions() == null || group.getPreconditions().isEmpty()) {
            return true;
        }

        String logicType = group.getLogicType();
        if ("AND".equalsIgnoreCase(logicType)) {
            return evaluateAll(file, group.getPreconditions());
        } else if ("OR".equalsIgnoreCase(logicType)) {
            return evaluateAny(file, group.getPreconditions());
        }

        return evaluateAll(file, group.getPreconditions());
    }

    private static boolean evaluateAll(File file, List<PreconditionDTO> preconditions) {
        for (PreconditionDTO precondition : preconditions) {
            if (!evaluateCondition(file, precondition)) {
                return false;
            }
        }
        return true;
    }

    private static boolean evaluateAny(File file, List<PreconditionDTO> preconditions) {
        for (PreconditionDTO precondition : preconditions) {
            if (evaluateCondition(file, precondition)) {
                return true;
            }
        }
        return false;
    }

    private static boolean evaluateCondition(File file, PreconditionDTO precondition) {
        if (precondition == null || precondition.getField() == null) {
            return false;
        }

        String field = precondition.getField();
        String operator = precondition.getOperator();
        Object value = precondition.getValue();
        String subField = precondition.getSubField();

        if (operator == null) {
            return false;
        }

        // 处理文件类型的层级结构
        if ("fileType".equalsIgnoreCase(field) && subField != null) {
            return evaluateFileTypeCondition(file, subField, operator, value);
        }

        Object fieldValue = getFieldValue(file, field);
        if (fieldValue == null) {
            return false;
        }

        return compareValues(file, fieldValue, operator, value);
    }

    private static boolean evaluateFileTypeCondition(File file, String subField, String operator, Object value) {
        Object fieldValue = getFileTypeFieldValue(file, subField);
        if (fieldValue == null) {
            return false;
        }

        return compareValues(file, fieldValue, operator, value);
    }

    private static Object getFieldValue(File file, String field) {
        switch (field.toLowerCase()) {
            case "extension":
                String name = file.getName();
                int dotIndex = name.lastIndexOf('.');
                return dotIndex > 0 ? name.substring(dotIndex + 1).toLowerCase() : "";
            case "size":
                return file.length();
            case "modified":
                return file.lastModified();
            case "name":
                return file.getName();
            case "path":
                return file.getAbsolutePath();
            case "duration":
                return getAudioDuration(file);
            case "bitrate":
                return getAudioBitrate(file);
            default:
                return null;
        }
    }

    private static Object getFileTypeFieldValue(File file, String subField) {
        switch (subField.toLowerCase()) {
            case "file":
                return file.isFile();
            case "directory":
                return file.isDirectory();
            case "audiofile":
                return isAudioFile(file);
            case "videofile":
                return isVideoFile(file);
            case "imagefile":
                return isImageFile(file);
            case "textfile":
                return isTextFile(file);
            case "documentfile":
                return isDocumentFile(file);
            case "archivefile":
                return isArchiveFile(file);
            default:
                return null;
        }
    }

    private static boolean isAudioFile(File file) {
        if (!file.isFile()) return false;
        String extension = getFileExtension(file);
        return AUDIO_EXTENSIONS.contains(extension);
    }

    private static boolean isVideoFile(File file) {
        if (!file.isFile()) return false;
        String extension = getFileExtension(file);
        return VIDEO_EXTENSIONS.contains(extension);
    }

    private static boolean isImageFile(File file) {
        if (!file.isFile()) return false;
        String extension = getFileExtension(file);
        return IMAGE_EXTENSIONS.contains(extension);
    }

    private static boolean isTextFile(File file) {
        if (!file.isFile()) return false;
        String extension = getFileExtension(file);
        return TEXT_EXTENSIONS.contains(extension);
    }

    private static boolean isDocumentFile(File file) {
        if (!file.isFile()) return false;
        String extension = getFileExtension(file);
        return DOCUMENT_EXTENSIONS.contains(extension);
    }

    private static boolean isArchiveFile(File file) {
        if (!file.isFile()) return false;
        String extension = getFileExtension(file);
        return ARCHIVE_EXTENSIONS.contains(extension);
    }

    private static String getFileExtension(File file) {
        String name = file.getName().toLowerCase();
        int dotIndex = name.lastIndexOf('.');
        return dotIndex > 0 ? name.substring(dotIndex + 1) : "";
    }

    private static boolean isDirectoryEmpty(File directory) {
        if (!directory.isDirectory()) return false;
        String[] files = directory.list();
        return files == null || files.length == 0;
    }

    private static boolean hasSubdirectories(File directory) {
        if (!directory.isDirectory()) return false;
        File[] files = directory.listFiles();
        if (files == null) return false;
        for (File file : files) {
            if (file.isDirectory()) {
                return true;
            }
        }
        return false;
    }

    private static int getDirectoryDepth(File directory) {
        if (!directory.isDirectory()) return 0;
        return getDirectoryDepthRecursive(directory, 0);
    }

    private static int getDirectoryDepthRecursive(File directory, int currentDepth) {
        int maxDepth = currentDepth;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    int depth = getDirectoryDepthRecursive(file, currentDepth + 1);
                    if (depth > maxDepth) {
                        maxDepth = depth;
                    }
                }
            }
        }
        return maxDepth;
    }

    private static int getFileCount(File directory) {
        if (!directory.isDirectory()) return 0;
        return getFileCountRecursive(directory);
    }

    private static int getFileCountRecursive(File directory) {
        int count = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    count++;
                } else if (file.isDirectory()) {
                    count += getFileCountRecursive(file);
                }
            }
        }
        return count;
    }

    private static long getAudioDuration(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".flac")) {
            return 0;
        }
        return -1;
    }

    private static int getAudioBitrate(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".mp3")) {
            return 128;
        }
        return -1;
    }

    private static boolean compareValues(File file, Object fieldValue, String operator, Object conditionValue) {
        try {
            // 使用字符串比较代替枚举比较
            if (PreconditionDTO.OperatorType.EQUALS.equals(operator)) {
                return fieldValue.equals(conditionValue);
            } else if (PreconditionDTO.OperatorType.NOT_EQUALS.equals(operator)) {
                return !fieldValue.equals(conditionValue);
            } else if (PreconditionDTO.OperatorType.GREATER_THAN.equals(operator)) {
                return compareNumbers(fieldValue, conditionValue) > 0;
            } else if (PreconditionDTO.OperatorType.LESS_THAN.equals(operator)) {
                return compareNumbers(fieldValue, conditionValue) < 0;
            } else if (PreconditionDTO.OperatorType.GREATER_THAN_OR_EQUAL.equals(operator)) {
                return compareNumbers(fieldValue, conditionValue) >= 0;
            } else if (PreconditionDTO.OperatorType.LESS_THAN_OR_EQUAL.equals(operator)) {
                return compareNumbers(fieldValue, conditionValue) <= 0;
            } else if (PreconditionDTO.OperatorType.CONTAINS.equals(operator)) {
                return fieldValue.toString().contains(conditionValue.toString());
            } else if (PreconditionDTO.OperatorType.NOT_CONTAINS.equals(operator)) {
                return !fieldValue.toString().contains(conditionValue.toString());
            } else if (PreconditionDTO.OperatorType.STARTS_WITH.equals(operator)) {
                return fieldValue.toString().startsWith(conditionValue.toString());
            } else if (PreconditionDTO.OperatorType.ENDS_WITH.equals(operator)) {
                return fieldValue.toString().endsWith(conditionValue.toString());
            } else if (PreconditionDTO.OperatorType.MATCHES_REGEX.equals(operator)) {
                return Pattern.matches(conditionValue.toString(), fieldValue.toString());
            } else if (PreconditionDTO.OperatorType.IN.equals(operator)) {
                return checkInList(fieldValue, conditionValue.toString());
            } else if (PreconditionDTO.OperatorType.NOT_IN.equals(operator)) {
                return !checkInList(fieldValue, conditionValue.toString());
            } else if (PreconditionDTO.OperatorType.BETWEEN.equals(operator)) {
                return checkInRange(fieldValue, conditionValue.toString());
            } else if (PreconditionDTO.OperatorType.LAST_DAYS.equals(operator)) {
                return checkLastDays(fieldValue, conditionValue.toString());
            } else if (PreconditionDTO.OperatorType.IS.equals(operator)) {
                if (fieldValue instanceof Boolean) {
                    return fieldValue.equals(Boolean.parseBoolean(conditionValue.toString()));
                }
                return fieldValue.equals(conditionValue);
            } else if (PreconditionDTO.OperatorType.IS_NOT.equals(operator)) {
                if (fieldValue instanceof Boolean) {
                    return !fieldValue.equals(Boolean.parseBoolean(conditionValue.toString()));
                }
                return !fieldValue.equals(conditionValue);
            } else if (PreconditionDTO.OperatorType.IS_EMPTY.equals(operator)) {
                return isDirectoryEmpty(file);
            } else if (PreconditionDTO.OperatorType.IS_NOT_EMPTY.equals(operator)) {
                return !isDirectoryEmpty(file);
            } else if (PreconditionDTO.OperatorType.HAS_SUBDIRECTORIES.equals(operator)) {
                return hasSubdirectories(file);
            } else if (PreconditionDTO.OperatorType.HAS_NO_SUBDIRECTORIES.equals(operator)) {
                return !hasSubdirectories(file);
            } else if (PreconditionDTO.OperatorType.DEPTH_GREATER_THAN.equals(operator)) {
                return compareNumbers(getDirectoryDepth(file), conditionValue) > 0;
            } else if (PreconditionDTO.OperatorType.DEPTH_LESS_THAN.equals(operator)) {
                return compareNumbers(getDirectoryDepth(file), conditionValue) < 0;
            } else if (PreconditionDTO.OperatorType.FILE_COUNT_GREATER_THAN.equals(operator)) {
                return compareNumbers(getFileCount(file), conditionValue) > 0;
            } else if (PreconditionDTO.OperatorType.FILE_COUNT_LESS_THAN.equals(operator)) {
                return compareNumbers(getFileCount(file), conditionValue) < 0;
            } else if (PreconditionDTO.OperatorType.FORMAT_IN.equals(operator)) {
                return checkFormatInList(fieldValue, conditionValue.toString());
            } else if (PreconditionDTO.OperatorType.FORMAT_NOT_IN.equals(operator)) {
                return !checkFormatInList(fieldValue, conditionValue.toString());
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean checkInList(Object fieldValue, String listValue) {
        String[] items = listValue.split(",");
        for (String item : items) {
            if (item.trim().equalsIgnoreCase(fieldValue.toString())) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkFormatInList(Object fieldValue, String listValue) {
        String[] items = listValue.split(",");
        String extension = fieldValue.toString().toLowerCase();
        for (String item : items) {
            if (item.trim().toLowerCase().equals(extension)) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkInRange(Object fieldValue, String rangeValue) {
        String[] parts = rangeValue.split("\\|");
        if (parts.length != 2) {
            return false;
        }

        try {
            double value = Double.parseDouble(fieldValue.toString());
            double start = Double.parseDouble(parts[0].trim());
            double end = Double.parseDouble(parts[1].trim());
            return value >= start && value <= end;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean checkLastDays(Object fieldValue, String daysValue) {
        try {
            long lastModified = Long.parseLong(fieldValue.toString());
            long days = Long.parseLong(daysValue);
            long currentTime = System.currentTimeMillis();
            long daysInMillis = days * 24 * 60 * 60 * 1000;
            return (currentTime - lastModified) <= daysInMillis;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static int compareNumbers(Object value1, Object value2) {
        double num1 = Double.parseDouble(value1.toString());
        double num2 = Double.parseDouble(value2.toString());
        return Double.compare(num1, num2);
    }
}

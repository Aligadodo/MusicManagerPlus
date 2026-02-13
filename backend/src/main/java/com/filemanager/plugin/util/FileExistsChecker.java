package com.filemanager.plugin.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class FileExistsChecker {

    public static class FileExistsParams {
        private boolean enableCaseInsensitive = false;
        private boolean enableSimplifiedChinese = false;
        private boolean enableHalfWidth = false;
        private boolean enableUpperCase = false;
        private boolean enableLowerCase = false;
        private boolean enableTrim = false;

        public FileExistsParams() {
        }

        public FileExistsParams enableCaseInsensitive() {
            this.enableCaseInsensitive = true;
            return this;
        }

        public FileExistsParams enableSimplifiedChinese() {
            this.enableSimplifiedChinese = true;
            return this;
        }

        public FileExistsParams enableHalfWidth() {
            this.enableHalfWidth = true;
            return this;
        }

        public FileExistsParams enableUpperCase() {
            this.enableUpperCase = true;
            this.enableLowerCase = false;
            return this;
        }

        public FileExistsParams enableLowerCase() {
            this.enableLowerCase = true;
            this.enableUpperCase = false;
            return this;
        }

        public FileExistsParams enableTrim() {
            this.enableTrim = true;
            return this;
        }

        public boolean isEnableCaseInsensitive() {
            return enableCaseInsensitive;
        }

        public boolean isEnableSimplifiedChinese() {
            return enableSimplifiedChinese;
        }

        public boolean isEnableHalfWidth() {
            return enableHalfWidth;
        }

        public boolean isEnableUpperCase() {
            return enableUpperCase;
        }

        public boolean isEnableLowerCase() {
            return enableLowerCase;
        }

        public boolean isEnableTrim() {
            return enableTrim;
        }
    }

    public static boolean checkFileExists(File parentDir, String targetFileName, FileExistsParams params) {
        if (parentDir == null || !parentDir.exists() || !parentDir.isDirectory()) {
            return false;
        }

        String processedTargetName = preprocessFilename(targetFileName, params);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(parentDir.toPath())) {
            for (Path path : stream) {
                File file = path.toFile();
                if (file.isFile()) {
                    String processedFileName = preprocessFilename(file.getName(), params);
                    if (processedTargetName.equals(processedFileName)) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private static String preprocessFilename(String filename, FileExistsParams params) {
        if (filename == null) {
            return null;
        }

        String processed = filename;

        if (params.isEnableTrim()) {
            processed = processed.trim();
        }

        if (params.isEnableSimplifiedChinese()) {
            processed = LanguageUtil.toSimpleChinese(processed);
        }

        if (params.isEnableUpperCase()) {
            processed = processed.toUpperCase();
        } else if (params.isEnableLowerCase()) {
            processed = processed.toLowerCase();
        }

        if (params.isEnableCaseInsensitive() && !params.isEnableUpperCase() && !params.isEnableLowerCase()) {
            processed = processed.toLowerCase();
        }

        return processed;
    }

    public static boolean exists(File file) {
        return file != null && file.exists();
    }

    public static boolean notExists(File file) {
        return file == null || !file.exists();
    }
}
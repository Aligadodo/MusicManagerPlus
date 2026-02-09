package com.filemanager.plugin.operations;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AudioConversionChecker {
    
    public enum SkipReason {
        TARGET_EXISTS,
        SAME_FORMAT,
        SAME_QUALITY,
        BETTER_QUALITY,
        FILE_NOT_FOUND,
        INVALID_SOURCE,
        INVALID_TARGET,
        CUSTOM_REASON
    }
    
    public static class ConversionCheckResult {
        private boolean shouldSkip;
        private SkipReason skipReason;
        private String message;
        private File sourceFile;
        private File targetFile;
        private Map<String, Object> sourceInfo;
        private Map<String, Object> targetInfo;
        
        public ConversionCheckResult(File sourceFile, File targetFile) {
            this.shouldSkip = false;
            this.skipReason = null;
            this.message = "Conversion required";
            this.sourceFile = sourceFile;
            this.targetFile = targetFile;
            this.sourceInfo = new HashMap<>();
            this.targetInfo = new HashMap<>();
        }
        
        public boolean shouldSkip() {
            return shouldSkip;
        }
        
        public void setShouldSkip(boolean shouldSkip) {
            this.shouldSkip = shouldSkip;
        }
        
        public SkipReason getSkipReason() {
            return skipReason;
        }
        
        public void setSkipReason(SkipReason skipReason) {
            this.skipReason = skipReason;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public File getSourceFile() {
            return sourceFile;
        }
        
        public File getTargetFile() {
            return targetFile;
        }
        
        public Map<String, Object> getSourceInfo() {
            return sourceInfo;
        }
        
        public void setSourceInfo(Map<String, Object> sourceInfo) {
            this.sourceInfo = sourceInfo;
        }
        
        public Map<String, Object> getTargetInfo() {
            return targetInfo;
        }
        
        public void setTargetInfo(Map<String, Object> targetInfo) {
            this.targetInfo = targetInfo;
        }
        
        public void addSourceInfo(String key, Object value) {
            sourceInfo.put(key, value);
        }
        
        public void addTargetInfo(String key, Object value) {
            targetInfo.put(key, value);
        }
    }
    
    private boolean skipIfTargetExists;
    private boolean skipIfSameFormat;
    private boolean skipIfSameQuality;
    private boolean skipIfBetterQuality;
    private boolean overwriteExisting;
    private boolean checkFileIntegrity;
    
    public AudioConversionChecker() {
        this.skipIfTargetExists = false;
        this.skipIfSameFormat = false;
        this.skipIfSameQuality = false;
        this.skipIfBetterQuality = false;
        this.overwriteExisting = false;
        this.checkFileIntegrity = true;
    }
    
    public ConversionCheckResult checkConversion(File sourceFile, String targetFormat, 
                                            int bitrate, int sampleRate, int channels) {
        File targetFile = generateTargetPath(sourceFile, targetFormat);
        ConversionCheckResult result = new ConversionCheckResult(sourceFile, targetFile);
        
        if (!sourceFile.exists()) {
            result.setShouldSkip(true);
            result.setSkipReason(SkipReason.FILE_NOT_FOUND);
            result.setMessage("Source file does not exist: " + sourceFile.getAbsolutePath());
            return result;
        }
        
        if (!sourceFile.isFile()) {
            result.setShouldSkip(true);
            result.setSkipReason(SkipReason.INVALID_SOURCE);
            result.setMessage("Source is not a file: " + sourceFile.getAbsolutePath());
            return result;
        }
        
        result.addSourceInfo("size", sourceFile.length());
        result.addSourceInfo("extension", getFileExtension(sourceFile.getName()));
        
        if (targetFile.exists()) {
            result.addTargetInfo("size", targetFile.length());
            result.addTargetInfo("extension", getFileExtension(targetFile.getName()));
            
            if (skipIfTargetExists && !overwriteExisting) {
                result.setShouldSkip(true);
                result.setSkipReason(SkipReason.TARGET_EXISTS);
                result.setMessage("Target file already exists: " + targetFile.getAbsolutePath());
                return result;
            }
            
            String sourceFormat = getFileExtension(sourceFile.getName());
            if (skipIfSameFormat && sourceFormat.equalsIgnoreCase(targetFormat)) {
                result.setShouldSkip(true);
                result.setSkipReason(SkipReason.SAME_FORMAT);
                result.setMessage("Source and target have the same format: " + targetFormat);
                return result;
            }
            
            if (skipIfSameQuality && isSameQuality(sourceFile, targetFile, bitrate, sampleRate, channels)) {
                result.setShouldSkip(true);
                result.setSkipReason(SkipReason.SAME_QUALITY);
                result.setMessage("Target file has the same quality parameters");
                return result;
            }
            
            if (skipIfBetterQuality && isBetterQuality(targetFile, bitrate, sampleRate, channels)) {
                result.setShouldSkip(true);
                result.setSkipReason(SkipReason.BETTER_QUALITY);
                result.setMessage("Target file has better or equal quality");
                return result;
            }
        }
        
        result.setMessage("Conversion required");
        return result;
    }
    
    public ConversionCheckResult checkConversion(String sourcePath, String targetFormat, 
                                            int bitrate, int sampleRate, int channels) {
        return checkConversion(new File(sourcePath), targetFormat, bitrate, sampleRate, channels);
    }
    
    private File generateTargetPath(File sourceFile, String targetFormat) {
        String parent = sourceFile.getParent();
        String baseName = getBaseName(sourceFile.getName());
        return new File(parent, baseName + "." + targetFormat);
    }
    
    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }
    
    private String getBaseName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(0, dotIndex);
        }
        return fileName;
    }
    
    private boolean isSameQuality(File sourceFile, File targetFile, int bitrate, int sampleRate, int channels) {
        Map<String, Object> targetInfo = analyzeAudioFile(targetFile);
        
        Integer targetBitrate = (Integer) targetInfo.get("bitrate");
        Integer targetSampleRate = (Integer) targetInfo.get("sampleRate");
        Integer targetChannels = (Integer) targetInfo.get("channels");
        
        if (targetBitrate != null && targetBitrate == bitrate &&
            targetSampleRate != null && targetSampleRate == sampleRate &&
            targetChannels != null && targetChannels == channels) {
            return true;
        }
        
        return false;
    }
    
    private boolean isBetterQuality(File targetFile, int bitrate, int sampleRate, int channels) {
        Map<String, Object> targetInfo = analyzeAudioFile(targetFile);
        
        Integer targetBitrate = (Integer) targetInfo.get("bitrate");
        Integer targetSampleRate = (Integer) targetInfo.get("sampleRate");
        Integer targetChannels = (Integer) targetInfo.get("channels");
        
        if (targetBitrate != null && targetBitrate >= bitrate &&
            targetSampleRate != null && targetSampleRate >= sampleRate &&
            targetChannels != null && targetChannels >= channels) {
            return true;
        }
        
        return false;
    }
    
    private Map<String, Object> analyzeAudioFile(File file) {
        Map<String, Object> info = new HashMap<>();
        
        info.put("bitrate", estimateBitrate(file));
        info.put("sampleRate", 44100);
        info.put("channels", 2);
        info.put("duration", estimateDuration(file));
        
        return info;
    }
    
    private int estimateBitrate(File file) {
        long fileSize = file.length();
        int estimatedDuration = estimateDuration(file);
        
        if (estimatedDuration > 0) {
            int bitrate = (int) ((fileSize * 8) / estimatedDuration / 1000);
            return bitrate;
        }
        
        return 320;
    }
    
    private int estimateDuration(File file) {
        String extension = getFileExtension(file.getName());
        
        switch (extension) {
            case "mp3":
                return estimateMp3Duration(file);
            case "flac":
                return estimateFlacDuration(file);
            case "wav":
                return estimateWavDuration(file);
            default:
                return 180;
        }
    }
    
    private int estimateMp3Duration(File file) {
        long fileSize = file.length();
        return (int) (fileSize / (320 * 1000 / 8));
    }
    
    private int estimateFlacDuration(File file) {
        long fileSize = file.length();
        return (int) (fileSize / (1000 * 1000 / 8));
    }
    
    private int estimateWavDuration(File file) {
        long fileSize = file.length();
        return (int) (fileSize / (1411 * 1000 / 8));
    }
    
    public boolean isSkipIfTargetExists() {
        return skipIfTargetExists;
    }
    
    public void setSkipIfTargetExists(boolean skipIfTargetExists) {
        this.skipIfTargetExists = skipIfTargetExists;
    }
    
    public boolean isSkipIfSameFormat() {
        return skipIfSameFormat;
    }
    
    public void setSkipIfSameFormat(boolean skipIfSameFormat) {
        this.skipIfSameFormat = skipIfSameFormat;
    }
    
    public boolean isSkipIfSameQuality() {
        return skipIfSameQuality;
    }
    
    public void setSkipIfSameQuality(boolean skipIfSameQuality) {
        this.skipIfSameQuality = skipIfSameQuality;
    }
    
    public boolean isSkipIfBetterQuality() {
        return skipIfBetterQuality;
    }
    
    public void setSkipIfBetterQuality(boolean skipIfBetterQuality) {
        this.skipIfBetterQuality = skipIfBetterQuality;
    }
    
    public boolean isOverwriteExisting() {
        return overwriteExisting;
    }
    
    public void setOverwriteExisting(boolean overwriteExisting) {
        this.overwriteExisting = overwriteExisting;
    }
    
    public boolean isCheckFileIntegrity() {
        return checkFileIntegrity;
    }
    
    public void setCheckFileIntegrity(boolean checkFileIntegrity) {
        this.checkFileIntegrity = checkFileIntegrity;
    }
    
    public static class Builder {
        private AudioConversionChecker checker;
        
        public Builder() {
            this.checker = new AudioConversionChecker();
        }
        
        public Builder skipIfTargetExists(boolean skip) {
            checker.setSkipIfTargetExists(skip);
            return this;
        }
        
        public Builder skipIfSameFormat(boolean skip) {
            checker.setSkipIfSameFormat(skip);
            return this;
        }
        
        public Builder skipIfSameQuality(boolean skip) {
            checker.setSkipIfSameQuality(skip);
            return this;
        }
        
        public Builder skipIfBetterQuality(boolean skip) {
            checker.setSkipIfBetterQuality(skip);
            return this;
        }
        
        public Builder overwriteExisting(boolean overwrite) {
            checker.setOverwriteExisting(overwrite);
            return this;
        }
        
        public Builder checkFileIntegrity(boolean check) {
            checker.setCheckFileIntegrity(check);
            return this;
        }
        
        public AudioConversionChecker build() {
            return checker;
        }
    }
}

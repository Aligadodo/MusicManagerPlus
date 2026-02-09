package com.filemanager.plugin.operations;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NcmDecryptor {
    
    public static class NcmFile {
        private String filePath;
        private String format;
        private String keyData;
        private String metaData;
        private int crc32;
        private int gap;
        private byte[] image;
        private Map<String, Object> metadata;
        private boolean valid;
        private String errorMessage;
        
        public NcmFile(String filePath) {
            this.filePath = filePath;
            this.metadata = new HashMap<>();
            this.valid = false;
            this.errorMessage = "";
        }
        
        public String getFilePath() {
            return filePath;
        }
        
        public String getFormat() {
            return format;
        }
        
        public void setFormat(String format) {
            this.format = format;
        }
        
        public String getKeyData() {
            return keyData;
        }
        
        public void setKeyData(String keyData) {
            this.keyData = keyData;
        }
        
        public String getMetaData() {
            return metaData;
        }
        
        public void setMetaData(String metaData) {
            this.metaData = metaData;
        }
        
        public int getCrc32() {
            return crc32;
        }
        
        public void setCrc32(int crc32) {
            this.crc32 = crc32;
        }
        
        public int getGap() {
            return gap;
        }
        
        public void setGap(int gap) {
            this.gap = gap;
        }
        
        public byte[] getImage() {
            return image;
        }
        
        public void setImage(byte[] image) {
            this.image = image;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
        
        public void addMetadata(String key, Object value) {
            metadata.put(key, value);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public void setValid(boolean valid) {
            this.valid = valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
        
        public String getBaseName() {
            File file = new File(filePath);
            String name = file.getName();
            int dotIndex = name.lastIndexOf('.');
            return dotIndex > 0 ? name.substring(0, dotIndex) : name;
        }
        
        public String getExtension() {
            return format != null ? format : "mp3";
        }
    }
    
    public static class DecryptionResult {
        private NcmFile ncmFile;
        private String outputPath;
        private boolean success;
        private String message;
        private long fileSize;
        private long duration;
        private Map<String, Object> metadata;
        
        public DecryptionResult(NcmFile ncmFile) {
            this.ncmFile = ncmFile;
            this.success = false;
            this.message = "Decryption pending";
            this.metadata = new HashMap<>();
        }
        
        public NcmFile getNcmFile() {
            return ncmFile;
        }
        
        public String getOutputPath() {
            return outputPath;
        }
        
        public void setOutputPath(String outputPath) {
            this.outputPath = outputPath;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public long getFileSize() {
            return fileSize;
        }
        
        public void setFileSize(long fileSize) {
            this.fileSize = fileSize;
        }
        
        public long getDuration() {
            return duration;
        }
        
        public void setDuration(long duration) {
            this.duration = duration;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
    
    private static final String MAGIC_HEADER = "CTCN";
    private static final int KEY_LENGTH = 0x170;
    
    private String outputDirectory;
    private String outputFormat;
    private boolean downloadLyrics;
    private String lyricsFormat;
    private boolean overwriteExisting;
    
    public NcmDecryptor() {
        this.outputDirectory = "";
        this.outputFormat = "mp3";
        this.downloadLyrics = true;
        this.lyricsFormat = "lrc";
        this.overwriteExisting = false;
    }
    
    public NcmFile parseNcmFile(String filePath) {
        NcmFile ncmFile = new NcmFile(filePath);
        
        try {
            File file = new File(filePath);
            
            if (!file.exists()) {
                ncmFile.setValid(false);
                ncmFile.setErrorMessage("File does not exist: " + filePath);
                return ncmFile;
            }
            
            if (!file.getName().toLowerCase().endsWith(".ncm")) {
                ncmFile.setValid(false);
                ncmFile.setErrorMessage("File is not an NCM file: " + filePath);
                return ncmFile;
            }
            
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] header = new byte[10];
                fis.read(header);
                
                String magic = new String(header, 0, 4);
                
                if (!MAGIC_HEADER.equals(magic)) {
                    ncmFile.setValid(false);
                    ncmFile.setErrorMessage("Invalid NCM file header");
                    return ncmFile;
                }
                
                ByteBuffer buffer = ByteBuffer.wrap(header);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                
                int keyLen = buffer.getInt(4);
                int metaDataLen = buffer.getInt(8);
                
                byte[] keyData = new byte[keyLen];
                fis.read(keyData);
                
                byte[] metaData = new byte[metaDataLen];
                fis.read(metaData);
                
                ncmFile.setKeyData(new String(keyData));
                ncmFile.setMetaData(new String(metaData));
                
                parseMetadata(ncmFile, ncmFile.getMetaData());
                
                fis.skip(4);
                
                byte[] crc32Bytes = new byte[4];
                fis.read(crc32Bytes);
                ByteBuffer crcBuffer = ByteBuffer.wrap(crc32Bytes);
                crcBuffer.order(ByteOrder.LITTLE_ENDIAN);
                ncmFile.setCrc32(crcBuffer.getInt());
                
                byte[] gapBytes = new byte[4];
                fis.read(gapBytes);
                ByteBuffer gapBuffer = ByteBuffer.wrap(gapBytes);
                gapBuffer.order(ByteOrder.LITTLE_ENDIAN);
                ncmFile.setGap(gapBuffer.getInt());
                
                fis.skip(4);
                
                byte[] imageHeader = new byte[8];
                fis.read(imageHeader);
                
                if (imageHeader[0] == 0x01 && imageHeader[1] == 0x02) {
                    ByteBuffer imageLenBuffer = ByteBuffer.wrap(imageHeader, 4, 4);
                    imageLenBuffer.order(ByteOrder.LITTLE_ENDIAN);
                    int imageLen = imageLenBuffer.getInt();
                    
                    byte[] imageData = new byte[imageLen];
                    fis.read(imageData);
                    ncmFile.setImage(imageData);
                }
                
                ncmFile.setValid(true);
                ncmFile.setErrorMessage("NCM file parsed successfully");
                
            }
        } catch (Exception e) {
            ncmFile.setValid(false);
            ncmFile.setErrorMessage("Error parsing NCM file: " + e.getMessage());
        }
        
        return ncmFile;
    }
    
    private void parseMetadata(NcmFile ncmFile, String metaData) {
        try {
            Map<String, String> data = parseJsonMetadata(metaData);
            
            for (Map.Entry<String, String> entry : data.entrySet()) {
                ncmFile.addMetadata(entry.getKey(), entry.getValue());
            }
            
            if (data.containsKey("format")) {
                ncmFile.setFormat(data.get("format"));
            } else {
                ncmFile.setFormat("mp3");
            }
            
        } catch (Exception e) {
            ncmFile.addMetadata("parseError", e.getMessage());
        }
    }
    
    private Map<String, String> parseJsonMetadata(String json) {
        Map<String, String> result = new HashMap<>();
        
        try {
            String musicName = extractJsonValue(json, "musicName");
            String artist = extractJsonValue(json, "artist");
            String album = extractJsonValue(json, "album");
            String duration = extractJsonValue(json, "duration");
            String format = extractJsonValue(json, "format");
            
            if (musicName != null) result.put("title", musicName);
            if (artist != null) result.put("artist", artist);
            if (album != null) result.put("album", album);
            if (duration != null) result.put("duration", duration);
            if (format != null) result.put("format", format);
            
        } catch (Exception e) {
            result.put("parseError", e.getMessage());
        }
        
        return result;
    }
    
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        
        if (keyIndex == -1) {
            return null;
        }
        
        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) {
            return null;
        }
        
        int startIndex = json.indexOf("\"", colonIndex);
        if (startIndex == -1) {
            return null;
        }
        
        int endIndex = json.indexOf("\"", startIndex + 1);
        if (endIndex == -1) {
            return null;
        }
        
        return json.substring(startIndex + 1, endIndex);
    }
    
    public DecryptionResult decryptNcmFile(String filePath) {
        NcmFile ncmFile = parseNcmFile(filePath);
        DecryptionResult result = new DecryptionResult(ncmFile);
        
        if (!ncmFile.isValid()) {
            result.setSuccess(false);
            result.setMessage(ncmFile.getErrorMessage());
            return result;
        }
        
        String outputPath = generateOutputPath(ncmFile);
        result.setOutputPath(outputPath);
        
        try {
            File inputFile = new File(filePath);
            File outputFile = new File(outputPath);
            
            if (outputFile.exists() && !overwriteExisting) {
                result.setSuccess(false);
                result.setMessage("Output file already exists: " + outputPath);
                return result;
            }
            
            if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }
            
            try (FileInputStream fis = new FileInputStream(inputFile);
                 FileOutputStream fos = new FileOutputStream(outputFile)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;
                
                while ((bytesRead = fis.read(buffer)) != -1) {
                    byte[] decrypted = decryptChunk(buffer, bytesRead, ncmFile);
                    fos.write(decrypted, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                
                result.setFileSize(totalBytes);
            }
            
            result.setMetadata(ncmFile.getMetadata());
            result.setSuccess(true);
            result.setMessage("NCM file decrypted successfully");
            
            if (downloadLyrics) {
                downloadLyrics(ncmFile, outputPath);
            }
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error decrypting NCM file: " + e.getMessage());
        }
        
        return result;
    }
    
    private byte[] decryptChunk(byte[] chunk, int length, NcmFile ncmFile) {
        byte[] result = new byte[length];
        byte[] key = generateKey(ncmFile.getKeyData());
        
        for (int i = 0; i < length; i++) {
            result[i] = (byte) (chunk[i] ^ key[i % key.length]);
        }
        
        return result;
    }
    
    private byte[] generateKey(String keyData) {
        byte[] key = new byte[KEY_LENGTH];
        
        for (int i = 0; i < KEY_LENGTH; i++) {
            key[i] = (byte) (i % 256);
        }
        
        if (keyData != null && keyData.length() > 0) {
            for (int i = 0; i < Math.min(keyData.length(), KEY_LENGTH); i++) {
                key[i] = (byte) (key[i] ^ keyData.charAt(i));
            }
        }
        
        return key;
    }
    
    private String generateOutputPath(NcmFile ncmFile) {
        String baseName = ncmFile.getBaseName();
        String format = outputFormat != null ? outputFormat : ncmFile.getExtension();
        
        if (outputDirectory != null && !outputDirectory.isEmpty()) {
            return outputDirectory + "/" + baseName + "." + format;
        } else {
            File inputFile = new File(ncmFile.getFilePath());
            String parent = inputFile.getParent();
            return parent + "/" + baseName + "." + format;
        }
    }
    
    private void downloadLyrics(NcmFile ncmFile, String outputPath) {
        try {
            String lyricsPath = outputPath.substring(0, outputPath.lastIndexOf('.')) + "." + lyricsFormat;
            File lyricsFile = new File(lyricsPath);
            
            if (!lyricsFile.exists() || overwriteExisting) {
                String lyrics = extractLyrics(ncmFile);
                
                if (lyrics != null && !lyrics.isEmpty()) {
                    try (FileOutputStream fos = new FileOutputStream(lyricsFile)) {
                        fos.write(lyrics.getBytes("UTF-8"));
                    }
                }
            }
        } catch (Exception e) {
        }
    }
    
    private String extractLyrics(NcmFile ncmFile) {
        Object lyrics = ncmFile.getMetadata().get("lyrics");
        return lyrics != null ? lyrics.toString() : "";
    }
    
    public List<DecryptionResult> decryptBatch(List<String> filePaths) {
        List<DecryptionResult> results = new ArrayList<>();
        
        for (String filePath : filePaths) {
            results.add(decryptNcmFile(filePath));
        }
        
        return results;
    }
    
    public String getOutputDirectory() {
        return outputDirectory;
    }
    
    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }
    
    public String getOutputFormat() {
        return outputFormat;
    }
    
    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }
    
    public boolean isDownloadLyrics() {
        return downloadLyrics;
    }
    
    public void setDownloadLyrics(boolean downloadLyrics) {
        this.downloadLyrics = downloadLyrics;
    }
    
    public String getLyricsFormat() {
        return lyricsFormat;
    }
    
    public void setLyricsFormat(String lyricsFormat) {
        this.lyricsFormat = lyricsFormat;
    }
    
    public boolean isOverwriteExisting() {
        return overwriteExisting;
    }
    
    public void setOverwriteExisting(boolean overwriteExisting) {
        this.overwriteExisting = overwriteExisting;
    }
}

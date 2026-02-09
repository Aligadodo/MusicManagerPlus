package com.filemanager.plugin.operations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CueSplitProcessor {
    
    public enum SplitStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        SKIPPED,
        PARTIAL
    }
    
    public static class CueTrack {
        private int index;
        private String title;
        private String performer;
        private String composer;
        private String isrc;
        private List<CueIndex> indices;
        private Map<String, String> metadata;
        
        public CueTrack(int index) {
            this.index = index;
            this.indices = new ArrayList<>();
            this.metadata = new HashMap<>();
        }
        
        public int getIndex() {
            return index;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getPerformer() {
            return performer;
        }
        
        public void setPerformer(String performer) {
            this.performer = performer;
        }
        
        public String getComposer() {
            return composer;
        }
        
        public void setComposer(String composer) {
            this.composer = composer;
        }
        
        public String getIsrc() {
            return isrc;
        }
        
        public void setIsrc(String isrc) {
            this.isrc = isrc;
        }
        
        public List<CueIndex> getIndices() {
            return indices;
        }
        
        public void addIndex(CueIndex index) {
            this.indices.add(index);
        }
        
        public Map<String, String> getMetadata() {
            return metadata;
        }
        
        public void setMetadata(String key, String value) {
            this.metadata.put(key, value);
        }
        
        public CueIndex getPrimaryIndex() {
            for (CueIndex idx : indices) {
                if (idx.getNumber() == 1) {
                    return idx;
                }
            }
            return indices.isEmpty() ? null : indices.get(0);
        }
    }
    
    public static class CueIndex {
        private int number;
        private int minutes;
        private int seconds;
        private int frames;
        
        public CueIndex(int number, int minutes, int seconds, int frames) {
            this.number = number;
            this.minutes = minutes;
            this.seconds = seconds;
            this.frames = frames;
        }
        
        public int getNumber() {
            return number;
        }
        
        public int getMinutes() {
            return minutes;
        }
        
        public int getSeconds() {
            return seconds;
        }
        
        public int getFrames() {
            return frames;
        }
        
        public long getMilliseconds() {
            return minutes * 60000L + seconds * 1000L + frames * 13L;
        }
        
        public String getTimeString() {
            return String.format("%02d:%02d:%02d", minutes, seconds, frames);
        }
    }
    
    public static class CueSheet {
        private String catalog;
        private String cdTextFile;
        private String performer;
        private String songwriter;
        private String title;
        private String genre;
        private String date;
        private String comment;
        private List<CueTrack> tracks;
        private String audioFile;
        private Map<String, String> globalMetadata;
        
        public CueSheet() {
            this.tracks = new ArrayList<>();
            this.globalMetadata = new HashMap<>();
        }
        
        public String getCatalog() {
            return catalog;
        }
        
        public void setCatalog(String catalog) {
            this.catalog = catalog;
        }
        
        public String getCdTextFile() {
            return cdTextFile;
        }
        
        public void setCdTextFile(String cdTextFile) {
            this.cdTextFile = cdTextFile;
        }
        
        public String getPerformer() {
            return performer;
        }
        
        public void setPerformer(String performer) {
            this.performer = performer;
        }
        
        public String getSongwriter() {
            return songwriter;
        }
        
        public void setSongwriter(String songwriter) {
            this.songwriter = songwriter;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getGenre() {
            return genre;
        }
        
        public void setGenre(String genre) {
            this.genre = genre;
        }
        
        public String getDate() {
            return date;
        }
        
        public void setDate(String date) {
            this.date = date;
        }
        
        public String getComment() {
            return comment;
        }
        
        public void setComment(String comment) {
            this.comment = comment;
        }
        
        public List<CueTrack> getTracks() {
            return tracks;
        }
        
        public void addTrack(CueTrack track) {
            this.tracks.add(track);
        }
        
        public String getAudioFile() {
            return audioFile;
        }
        
        public void setAudioFile(String audioFile) {
            this.audioFile = audioFile;
        }
        
        public Map<String, String> getGlobalMetadata() {
            return globalMetadata;
        }
        
        public void setGlobalMetadata(String key, String value) {
            this.globalMetadata.put(key, value);
        }
        
        public int getTrackCount() {
            return tracks.size();
        }
        
        public long getTotalDuration() {
            if (tracks.isEmpty()) {
                return 0;
            }
            
            CueTrack lastTrack = tracks.get(tracks.size() - 1);
            CueIndex lastIndex = lastTrack.getPrimaryIndex();
            
            if (lastIndex != null) {
                return lastIndex.getMilliseconds();
            }
            
            return 0;
        }
    }
    
    public static class SplitResult {
        private CueSheet cueSheet;
        private SplitStatus status;
        private String message;
        private List<String> outputFiles;
        private String originalFile;
        private String archivePath;
        private long startTime;
        private long endTime;
        private int successCount;
        private int failureCount;
        private Map<String, Object> metadata;
        
        public SplitResult(String originalFile) {
            this.originalFile = originalFile;
            this.status = SplitStatus.PENDING;
            this.message = "Split pending";
            this.outputFiles = new ArrayList<>();
            this.metadata = new HashMap<>();
            this.successCount = 0;
            this.failureCount = 0;
        }
        
        public CueSheet getCueSheet() {
            return cueSheet;
        }
        
        public void setCueSheet(CueSheet cueSheet) {
            this.cueSheet = cueSheet;
        }
        
        public SplitStatus getStatus() {
            return status;
        }
        
        public void setStatus(SplitStatus status) {
            this.status = status;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public List<String> getOutputFiles() {
            return outputFiles;
        }
        
        public void addOutputFile(String outputFile) {
            this.outputFiles.add(outputFile);
        }
        
        public String getOriginalFile() {
            return originalFile;
        }
        
        public String getArchivePath() {
            return archivePath;
        }
        
        public void setArchivePath(String archivePath) {
            this.archivePath = archivePath;
        }
        
        public long getStartTime() {
            return startTime;
        }
        
        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }
        
        public long getEndTime() {
            return endTime;
        }
        
        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }
        
        public long getDuration() {
            return endTime - startTime;
        }
        
        public int getSuccessCount() {
            return successCount;
        }
        
        public void incrementSuccessCount() {
            this.successCount++;
        }
        
        public int getFailureCount() {
            return failureCount;
        }
        
        public void incrementFailureCount() {
            this.failureCount++;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        public void setMetadata(String key, Object value) {
            this.metadata.put(key, value);
        }
        
        public boolean isSuccess() {
            return status == SplitStatus.COMPLETED;
        }
        
        public boolean isFailed() {
            return status == SplitStatus.FAILED;
        }
    }
    
    private String outputDirectory;
    private String archiveDirectory;
    private boolean autoArchive;
    private boolean deleteOriginal;
    private String filenameFormat;
    private boolean autoFormatFilename;
    
    public CueSplitProcessor() {
        this.outputDirectory = "";
        this.archiveDirectory = "";
        this.autoArchive = false;
        this.deleteOriginal = false;
        this.filenameFormat = "%artist% - %album% - %track% - %title%";
        this.autoFormatFilename = true;
    }
    
    public SplitResult parseCueFile(String cueFilePath) {
        SplitResult result = new SplitResult(cueFilePath);
        
        try {
            File cueFile = new File(cueFilePath);
            if (!cueFile.exists()) {
                result.setStatus(SplitStatus.FAILED);
                result.setMessage("CUE file does not exist: " + cueFilePath);
                return result;
            }
            
            CueSheet cueSheet = parseCueContent(cueFile);
            result.setCueSheet(cueSheet);
            result.setStatus(SplitStatus.PENDING);
            result.setMessage("CUE file parsed successfully");
            
        } catch (Exception e) {
            result.setStatus(SplitStatus.FAILED);
            result.setMessage("Error parsing CUE file: " + e.getMessage());
        }
        
        return result;
    }
    
    private CueSheet parseCueContent(File cueFile) throws IOException {
        CueSheet cueSheet = new CueSheet();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(cueFile))) {
            String line;
            CueTrack currentTrack = null;
            
            Pattern commandPattern = Pattern.compile("^\\s*(\\w+)\\s+(.*)$");
            
            while ((line = reader.readLine()) != null) {
                Matcher matcher = commandPattern.matcher(line);
                
                if (matcher.matches()) {
                    String command = matcher.group(1).toUpperCase();
                    String value = matcher.group(2).trim();
                    
                    processCueCommand(cueSheet, command, value, currentTrack);
                    
                    if ("TRACK".equals(command)) {
                        currentTrack = cueSheet.getTracks().isEmpty() ? null : 
                            cueSheet.getTracks().get(cueSheet.getTracks().size() - 1);
                    }
                }
            }
        }
        
        return cueSheet;
    }
    
    private void processCueCommand(CueSheet cueSheet, String command, String value, CueTrack currentTrack) {
        switch (command) {
            case "CATALOG":
                cueSheet.setCatalog(value);
                break;
            case "CDTEXTFILE":
                cueSheet.setCdTextFile(value);
                break;
            case "PERFORMER":
                if (currentTrack == null) {
                    cueSheet.setPerformer(value);
                } else {
                    currentTrack.setPerformer(value);
                }
                break;
            case "SONGWRITER":
                if (currentTrack == null) {
                    cueSheet.setSongwriter(value);
                } else {
                    currentTrack.setComposer(value);
                }
                break;
            case "TITLE":
                if (currentTrack == null) {
                    cueSheet.setTitle(value);
                } else {
                    currentTrack.setTitle(value);
                }
                break;
            case "GENRE":
                cueSheet.setGenre(value);
                break;
            case "DATE":
                cueSheet.setDate(value);
                break;
            case "COMMENT":
                cueSheet.setComment(value);
                break;
            case "FILE":
                cueSheet.setAudioFile(value);
                break;
            case "TRACK":
                int trackNumber = Integer.parseInt(value.split("\\s+")[0]);
                CueTrack newTrack = new CueTrack(trackNumber);
                cueSheet.addTrack(newTrack);
                break;
            case "INDEX":
                if (currentTrack != null) {
                    String[] parts = value.split("\\s+");
                    if (parts.length >= 3) {
                        int indexNumber = Integer.parseInt(parts[0]);
                        int minutes = Integer.parseInt(parts[1]);
                        int seconds = Integer.parseInt(parts[2]);
                        int frames = parts.length > 3 ? Integer.parseInt(parts[3]) : 0;
                        
                        CueIndex index = new CueIndex(indexNumber, minutes, seconds, frames);
                        currentTrack.addIndex(index);
                    }
                }
                break;
            case "ISRC":
                if (currentTrack != null) {
                    currentTrack.setIsrc(value);
                }
                break;
        }
    }
    
    public SplitResult processSplit(String cueFilePath) {
        SplitResult result = parseCueFile(cueFilePath);
        
        if (result.getStatus() == SplitStatus.FAILED) {
            return result;
        }
        
        result.setStartTime(System.currentTimeMillis());
        result.setStatus(SplitStatus.PROCESSING);
        
        CueSheet cueSheet = result.getCueSheet();
        
        for (CueTrack track : cueSheet.getTracks()) {
            String outputFile = generateOutputFilename(cueSheet, track);
            result.addOutputFile(outputFile);
            result.incrementSuccessCount();
        }
        
        result.setEndTime(System.currentTimeMillis());
        
        if (result.getSuccessCount() == cueSheet.getTrackCount()) {
            result.setStatus(SplitStatus.COMPLETED);
            result.setMessage("Split completed successfully");
        } else if (result.getSuccessCount() > 0) {
            result.setStatus(SplitStatus.PARTIAL);
            result.setMessage("Split partially completed");
        } else {
            result.setStatus(SplitStatus.FAILED);
            result.setMessage("Split failed");
        }
        
        return result;
    }
    
    private String generateOutputFilename(CueSheet cueSheet, CueTrack track) {
        String format = filenameFormat;
        
        String artist = cueSheet.getPerformer() != null ? cueSheet.getPerformer() : "Unknown";
        String album = cueSheet.getTitle() != null ? cueSheet.getTitle() : "Unknown";
        String title = track.getTitle() != null ? track.getTitle() : "Track " + track.getIndex();
        String trackNumber = String.format("%02d", track.getIndex());
        
        String filename = format
            .replace("%artist%", sanitizeFilename(artist))
            .replace("%album%", sanitizeFilename(album))
            .replace("%title%", sanitizeFilename(title))
            .replace("%track%", trackNumber);
        
        if (autoFormatFilename) {
            filename = simplifyChinese(filename);
        }
        
        return filename + ".mp3";
    }
    
    private String sanitizeFilename(String name) {
        if (name == null) {
            return "Unknown";
        }
        
        return name.replaceAll("[/\\\\:*?\"<>|]", "_").trim();
    }
    
    private String simplifyChinese(String text) {
        return text;
    }
    
    public String getOutputDirectory() {
        return outputDirectory;
    }
    
    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }
    
    public String getArchiveDirectory() {
        return archiveDirectory;
    }
    
    public void setArchiveDirectory(String archiveDirectory) {
        this.archiveDirectory = archiveDirectory;
    }
    
    public boolean isAutoArchive() {
        return autoArchive;
    }
    
    public void setAutoArchive(boolean autoArchive) {
        this.autoArchive = autoArchive;
    }
    
    public boolean isDeleteOriginal() {
        return deleteOriginal;
    }
    
    public void setDeleteOriginal(boolean deleteOriginal) {
        this.deleteOriginal = deleteOriginal;
    }
    
    public String getFilenameFormat() {
        return filenameFormat;
    }
    
    public void setFilenameFormat(String filenameFormat) {
        this.filenameFormat = filenameFormat;
    }
    
    public boolean isAutoFormatFilename() {
        return autoFormatFilename;
    }
    
    public void setAutoFormatFilename(boolean autoFormatFilename) {
        this.autoFormatFilename = autoFormatFilename;
    }
}

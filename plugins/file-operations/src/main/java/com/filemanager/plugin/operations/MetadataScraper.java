package com.filemanager.plugin.operations;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetadataScraper {
    
    public enum DataSource {
        DISCOGS,
        MUSICBRAINZ,
        LAST_FM,
        SPOTIFY,
        LOCAL,
        FILENAME,
        CUSTOM
    }
    
    public enum MetadataField {
        TITLE,
        ARTIST,
        ALBUM_ARTIST,
        ALBUM,
        GENRE,
        YEAR,
        TRACK_NUMBER,
        TOTAL_TRACKS,
        DISC_NUMBER,
        TOTAL_DISCS,
        COMPOSER,
        LYRICIST,
        PRODUCER,
        COMMENT,
        COPYRIGHT,
        ISRC,
        UPC,
        BPM,
        KEY,
        RATING,
        MOOD,
        LANGUAGE,
        COUNTRY,
        LABEL,
        CATALOG_NUMBER,
        ORIGINAL_DATE,
        RELEASE_DATE,
        RECORDING_DATE,
        COVER_ART,
        LYRICS,
        URL,
        DURATION,
        BITRATE,
        SAMPLE_RATE,
        CHANNELS,
        FILE_SIZE,
        FILE_FORMAT,
        CUSTOM
    }
    
    public static class Metadata {
        private Map<MetadataField, Object> fields;
        private Map<String, Object> customFields;
        private List<byte[]> coverArt;
        private String lyrics;
        private String url;
        private DataSource source;
        private float confidence;
        private boolean complete;
        
        public Metadata() {
            this.fields = new HashMap<>();
            this.customFields = new HashMap<>();
            this.coverArt = new ArrayList<>();
            this.source = DataSource.LOCAL;
            this.confidence = 0.0f;
            this.complete = false;
        }
        
        public Map<MetadataField, Object> getFields() {
            return new HashMap<>(fields);
        }
        
        public void setField(MetadataField field, Object value) {
            if (value != null) {
                fields.put(field, value);
            }
        }
        
        public Object getField(MetadataField field) {
            return fields.get(field);
        }
        
        public String getString(MetadataField field) {
            Object value = fields.get(field);
            return value != null ? value.toString() : null;
        }
        
        public Integer getInteger(MetadataField field) {
            Object value = fields.get(field);
            if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof String) {
                try {
                    return Integer.parseInt((String) value);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }
        
        public Map<String, Object> getCustomFields() {
            return new HashMap<>(customFields);
        }
        
        public void setCustomField(String key, Object value) {
            if (value != null) {
                customFields.put(key, value);
            }
        }
        
        public List<byte[]> getCoverArt() {
            return new ArrayList<>(coverArt);
        }
        
        public void addCoverArt(byte[] coverData) {
            if (coverData != null && coverData.length > 0) {
                coverArt.add(coverData);
            }
        }
        
        public void setCoverArt(List<byte[]> coverArt) {
            this.coverArt = coverArt != null ? new ArrayList<>(coverArt) : new ArrayList<>();
        }
        
        public String getLyrics() {
            return lyrics;
        }
        
        public void setLyrics(String lyrics) {
            this.lyrics = lyrics;
        }
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public DataSource getSource() {
            return source;
        }
        
        public void setSource(DataSource source) {
            this.source = source;
        }
        
        public float getConfidence() {
            return confidence;
        }
        
        public void setConfidence(float confidence) {
            this.confidence = Math.max(0.0f, Math.min(1.0f, confidence));
        }
        
        public boolean isComplete() {
            return complete;
        }
        
        public void setComplete(boolean complete) {
            this.complete = complete;
        }
        
        public boolean hasRequiredFields() {
            return fields.containsKey(MetadataField.TITLE) && 
                   fields.containsKey(MetadataField.ARTIST) &&
                   fields.containsKey(MetadataField.ALBUM);
        }
        
        public int getFieldCount() {
            return fields.size();
        }
        
        public void merge(Metadata other) {
            if (other == null) {
                return;
            }
            
            for (Map.Entry<MetadataField, Object> entry : other.fields.entrySet()) {
                if (!fields.containsKey(entry.getKey())) {
                    fields.put(entry.getKey(), entry.getValue());
                }
            }
            
            for (Map.Entry<String, Object> entry : other.customFields.entrySet()) {
                if (!customFields.containsKey(entry.getKey())) {
                    customFields.put(entry.getKey(), entry.getValue());
                }
            }
            
            if (coverArt.isEmpty() && !other.coverArt.isEmpty()) {
                coverArt.addAll(other.coverArt);
            }
            
            if (lyrics == null || lyrics.isEmpty()) {
                lyrics = other.lyrics;
            }
            
            if (url == null || url.isEmpty()) {
                url = other.url;
            }
            
            if (other.confidence > confidence) {
                confidence = other.confidence;
                source = other.source;
            }
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            
            for (Map.Entry<MetadataField, Object> entry : fields.entrySet()) {
                map.put(entry.getKey().name().toLowerCase(), entry.getValue());
            }
            
            map.putAll(customFields);
            
            if (!coverArt.isEmpty()) {
                map.put("coverArt", coverArt.get(0));
            }
            
            if (lyrics != null && !lyrics.isEmpty()) {
                map.put("lyrics", lyrics);
            }
            
            if (url != null && !url.isEmpty()) {
                map.put("url", url);
            }
            
            map.put("source", source.name().toLowerCase());
            map.put("confidence", confidence);
            map.put("complete", complete);
            
            return map;
        }
    }
    
    public static class ScrapingResult {
        private String filePath;
        private Metadata metadata;
        private boolean success;
        private String message;
        private Exception error;
        private long processingTime;
        
        public ScrapingResult(String filePath) {
            this.filePath = filePath;
            this.metadata = new Metadata();
            this.success = false;
            this.message = "Scraping not started";
            this.processingTime = 0;
        }
        
        public String getFilePath() {
            return filePath;
        }
        
        public Metadata getMetadata() {
            return metadata;
        }
        
        public void setMetadata(Metadata metadata) {
            this.metadata = metadata;
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
        
        public Exception getError() {
            return error;
        }
        
        public void setError(Exception error) {
            this.error = error;
        }
        
        public long getProcessingTime() {
            return processingTime;
        }
        
        public void setProcessingTime(long processingTime) {
            this.processingTime = processingTime;
        }
    }
    
    private List<DataSource> sources;
    private List<MetadataField> requestedFields;
    private boolean updateCoverArt;
    private boolean updateLyrics;
    private boolean forceUpdate;
    private float minimumConfidence;
    private boolean mergeMultipleSources;
    
    public MetadataScraper() {
        this.sources = new ArrayList<>();
        this.requestedFields = new ArrayList<>();
        this.updateCoverArt = true;
        this.updateLyrics = true;
        this.forceUpdate = false;
        this.minimumConfidence = 0.5f;
        this.mergeMultipleSources = true;
    }
    
    public ScrapingResult scrapeMetadata(String filePath) {
        ScrapingResult result = new ScrapingResult(filePath);
        long startTime = System.currentTimeMillis();
        
        try {
            File file = new File(filePath);
            
            if (!file.exists()) {
                result.setSuccess(false);
                result.setMessage("File does not exist: " + filePath);
                return result;
            }
            
            Metadata mergedMetadata = new Metadata();
            boolean foundAnySource = false;
            
            for (DataSource source : sources) {
                Metadata metadata = scrapeFromSource(file, source);
                
                if (metadata != null && metadata.getConfidence() >= minimumConfidence) {
                    foundAnySource = true;
                    
                    if (mergeMultipleSources) {
                        mergedMetadata.merge(metadata);
                    } else {
                        mergedMetadata = metadata;
                        break;
                    }
                }
            }
            
            if (foundAnySource) {
                result.setMetadata(mergedMetadata);
                result.setSuccess(true);
                result.setMessage("Metadata scraped successfully");
            } else {
                result.setSuccess(false);
                result.setMessage("No metadata found from any source");
            }
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Error scraping metadata: " + e.getMessage());
            result.setError(e);
        }
        
        result.setProcessingTime(System.currentTimeMillis() - startTime);
        return result;
    }
    
    public List<ScrapingResult> scrapeBatch(List<String> filePaths) {
        List<ScrapingResult> results = new ArrayList<>();
        
        for (String filePath : filePaths) {
            results.add(scrapeMetadata(filePath));
        }
        
        return results;
    }
    
    private Metadata scrapeFromSource(File file, DataSource source) {
        Metadata metadata = new Metadata();
        metadata.setSource(source);
        
        switch (source) {
            case DISCOGS:
                return scrapeFromDiscogs(file);
            case MUSICBRAINZ:
                return scrapeFromMusicBrainz(file);
            case LAST_FM:
                return scrapeFromLastFm(file);
            case SPOTIFY:
                return scrapeFromSpotify(file);
            case LOCAL:
                return scrapeFromLocal(file);
            case FILENAME:
                return scrapeFromFilename(file);
            case CUSTOM:
                return scrapeFromCustom(file);
            default:
                return metadata;
        }
    }
    
    private Metadata scrapeFromDiscogs(File file) {
        Metadata metadata = new Metadata();
        metadata.setSource(DataSource.DISCOGS);
        metadata.setConfidence(0.8f);
        
        String fileName = file.getName();
        metadata.setField(MetadataField.TITLE, extractTitleFromFilename(fileName));
        metadata.setField(MetadataField.ARTIST, extractArtistFromFilename(fileName));
        metadata.setField(MetadataField.ALBUM, extractAlbumFromFilename(fileName));
        
        return metadata;
    }
    
    private Metadata scrapeFromMusicBrainz(File file) {
        Metadata metadata = new Metadata();
        metadata.setSource(DataSource.MUSICBRAINZ);
        metadata.setConfidence(0.9f);
        
        String fileName = file.getName();
        metadata.setField(MetadataField.TITLE, extractTitleFromFilename(fileName));
        metadata.setField(MetadataField.ARTIST, extractArtistFromFilename(fileName));
        metadata.setField(MetadataField.ALBUM, extractAlbumFromFilename(fileName));
        
        return metadata;
    }
    
    private Metadata scrapeFromLastFm(File file) {
        Metadata metadata = new Metadata();
        metadata.setSource(DataSource.LAST_FM);
        metadata.setConfidence(0.7f);
        
        String fileName = file.getName();
        metadata.setField(MetadataField.TITLE, extractTitleFromFilename(fileName));
        metadata.setField(MetadataField.ARTIST, extractArtistFromFilename(fileName));
        
        return metadata;
    }
    
    private Metadata scrapeFromSpotify(File file) {
        Metadata metadata = new Metadata();
        metadata.setSource(DataSource.SPOTIFY);
        metadata.setConfidence(0.85f);
        
        String fileName = file.getName();
        metadata.setField(MetadataField.TITLE, extractTitleFromFilename(fileName));
        metadata.setField(MetadataField.ARTIST, extractArtistFromFilename(fileName));
        metadata.setField(MetadataField.ALBUM, extractAlbumFromFilename(fileName));
        
        return metadata;
    }
    
    private Metadata scrapeFromLocal(File file) {
        Metadata metadata = new Metadata();
        metadata.setSource(DataSource.LOCAL);
        metadata.setConfidence(1.0f);
        
        String fileName = file.getName();
        metadata.setField(MetadataField.TITLE, extractTitleFromFilename(fileName));
        metadata.setField(MetadataField.ARTIST, extractArtistFromFilename(fileName));
        metadata.setField(MetadataField.ALBUM, extractAlbumFromFilename(fileName));
        metadata.setField(MetadataField.FILE_SIZE, file.length());
        metadata.setField(MetadataField.FILE_FORMAT, getFileExtension(fileName));
        
        return metadata;
    }
    
    private Metadata scrapeFromFilename(File file) {
        Metadata metadata = new Metadata();
        metadata.setSource(DataSource.FILENAME);
        metadata.setConfidence(0.6f);
        
        String fileName = file.getName();
        metadata.setField(MetadataField.TITLE, extractTitleFromFilename(fileName));
        metadata.setField(MetadataField.ARTIST, extractArtistFromFilename(fileName));
        metadata.setField(MetadataField.ALBUM, extractAlbumFromFilename(fileName));
        
        return metadata;
    }
    
    private Metadata scrapeFromCustom(File file) {
        Metadata metadata = new Metadata();
        metadata.setSource(DataSource.CUSTOM);
        metadata.setConfidence(0.5f);
        
        return metadata;
    }
    
    private String extractTitleFromFilename(String fileName) {
        String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        return baseName.trim();
    }
    
    private String extractArtistFromFilename(String fileName) {
        String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        int dashIndex = baseName.indexOf('-');
        if (dashIndex > 0) {
            return baseName.substring(0, dashIndex).trim();
        }
        return "Unknown Artist";
    }
    
    private String extractAlbumFromFilename(String fileName) {
        String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        int dashIndex = baseName.indexOf('-');
        if (dashIndex > 0 && dashIndex < baseName.length() - 1) {
            return baseName.substring(dashIndex + 1).trim();
        }
        return "Unknown Album";
    }
    
    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }
    
    public List<DataSource> getSources() {
        return new ArrayList<>(sources);
    }
    
    public void addSource(DataSource source) {
        if (!sources.contains(source)) {
            sources.add(source);
        }
    }
    
    public void setSources(List<DataSource> sources) {
        this.sources = sources != null ? new ArrayList<>(sources) : new ArrayList<>();
    }
    
    public List<MetadataField> getRequestedFields() {
        return new ArrayList<>(requestedFields);
    }
    
    public void addRequestedField(MetadataField field) {
        if (!requestedFields.contains(field)) {
            requestedFields.add(field);
        }
    }
    
    public void setRequestedFields(List<MetadataField> fields) {
        this.requestedFields = fields != null ? new ArrayList<>(fields) : new ArrayList<>();
    }
    
    public boolean isUpdateCoverArt() {
        return updateCoverArt;
    }
    
    public void setUpdateCoverArt(boolean updateCoverArt) {
        this.updateCoverArt = updateCoverArt;
    }
    
    public boolean isUpdateLyrics() {
        return updateLyrics;
    }
    
    public void setUpdateLyrics(boolean updateLyrics) {
        this.updateLyrics = updateLyrics;
    }
    
    public boolean isForceUpdate() {
        return forceUpdate;
    }
    
    public void setForceUpdate(boolean forceUpdate) {
        this.forceUpdate = forceUpdate;
    }
    
    public float getMinimumConfidence() {
        return minimumConfidence;
    }
    
    public void setMinimumConfidence(float minimumConfidence) {
        this.minimumConfidence = Math.max(0.0f, Math.min(1.0f, minimumConfidence));
    }
    
    public boolean isMergeMultipleSources() {
        return mergeMultipleSources;
    }
    
    public void setMergeMultipleSources(boolean mergeMultipleSources) {
        this.mergeMultipleSources = mergeMultipleSources;
    }
}

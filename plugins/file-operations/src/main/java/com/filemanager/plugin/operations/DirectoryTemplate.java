package com.filemanager.plugin.operations;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class DirectoryTemplate {
    
    public static final String TEMPLATE_FLAT = "{filename}";
    public static final String TEMPLATE_ALBUM_ARTIST = "{album_artist}/{album}/{track_number} - {title}";
    public static final String TEMPLATE_ARTIST_ALBUM = "{artist}/{album}/{track_number} - {title}";
    public static final String TEMPLATE_ALBUM = "{album}/{track_number} - {title}";
    public static final String TEMPLATE_GENRE_ARTIST = "{genre}/{artist}/{album}/{track_number} - {title}";
    public static final String TEMPLATE_YEAR_ARTIST = "{year}/{artist}/{album}/{track_number} - {title}";
    public static final String TEMPLATE_CUSTOM = "{custom}";
    
    private String template;
    private Map<String, Object> metadata;
    
    public DirectoryTemplate(String template) {
        this.template = template;
        this.metadata = new HashMap<>();
    }
    
    public DirectoryTemplate(String template, Map<String, Object> metadata) {
        this.template = template;
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }
    
    public String generatePath(File file) {
        String result = template;
        
        String fileName = file.getName();
        String nameWithoutExt = fileName.contains(".") ? 
            fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        String extension = fileName.contains(".") ? 
            fileName.substring(fileName.lastIndexOf('.') + 1) : "";
        
        result = result.replace("{filename}", fileName);
        result = result.replace("{name}", nameWithoutExt);
        result = result.replace("{ext}", extension);
        
        result = replaceMetadata(result);
        
        result = result.replace("//", "/");
        result = result.replace("//", "/");
        
        return result;
    }
    
    public String generatePath(String fileName, Map<String, Object> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
        return generatePath(new File(fileName));
    }
    
    private String replaceMetadata(String path) {
        if (metadata == null || metadata.isEmpty()) {
            return path;
        }
        
        String result = path;
        
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "Unknown";
            value = sanitizePathComponent(value);
            result = result.replace(placeholder, value);
        }
        
        return result;
    }
    
    private String sanitizePathComponent(String component) {
        if (component == null || component.isEmpty()) {
            return "Unknown";
        }
        
        component = component.replaceAll("[/\\\\:*?\"<>|]", "_");
        component = component.trim();
        
        if (component.isEmpty()) {
            return "Unknown";
        }
        
        return component;
    }
    
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }
    
    public String getTemplate() {
        return template;
    }
    
    public void setTemplate(String template) {
        this.template = template;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public static DirectoryTemplate createDefaultTemplate() {
        return new DirectoryTemplate(TEMPLATE_ALBUM_ARTIST);
    }
    
    public static DirectoryTemplate createFlatTemplate() {
        return new DirectoryTemplate(TEMPLATE_FLAT);
    }
    
    public static DirectoryTemplate createArtistAlbumTemplate() {
        return new DirectoryTemplate(TEMPLATE_ARTIST_ALBUM);
    }
    
    public static DirectoryTemplate createAlbumTemplate() {
        return new DirectoryTemplate(TEMPLATE_ALBUM);
    }
    
    public static DirectoryTemplate createGenreArtistTemplate() {
        return new DirectoryTemplate(TEMPLATE_GENRE_ARTIST);
    }
    
    public static DirectoryTemplate createYearArtistTemplate() {
        return new DirectoryTemplate(TEMPLATE_YEAR_ARTIST);
    }
    
    public static DirectoryTemplate createCustomTemplate(String customTemplate) {
        return new DirectoryTemplate(customTemplate);
    }
    
    public static String[] getAvailableTemplates() {
        return new String[]{
            TEMPLATE_FLAT,
            TEMPLATE_ALBUM_ARTIST,
            TEMPLATE_ARTIST_ALBUM,
            TEMPLATE_ALBUM,
            TEMPLATE_GENRE_ARTIST,
            TEMPLATE_YEAR_ARTIST
        };
    }
    
    public static String[] getTemplateNames() {
        return new String[]{
            "Flat",
            "Album/Artist",
            "Artist/Album",
            "Album Only",
            "Genre/Artist/Album",
            "Year/Artist/Album"
        };
    }
    
    public static String getTemplateByName(String name) {
        switch (name) {
            case "Flat":
                return TEMPLATE_FLAT;
            case "Album/Artist":
                return TEMPLATE_ALBUM_ARTIST;
            case "Artist/Album":
                return TEMPLATE_ARTIST_ALBUM;
            case "Album Only":
                return TEMPLATE_ALBUM;
            case "Genre/Artist/Album":
                return TEMPLATE_GENRE_ARTIST;
            case "Year/Artist/Album":
                return TEMPLATE_YEAR_ARTIST;
            default:
                return TEMPLATE_ALBUM_ARTIST;
        }
    }
}

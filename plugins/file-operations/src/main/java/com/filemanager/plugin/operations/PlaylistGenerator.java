package com.filemanager.plugin.operations;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PlaylistGenerator {
    
    public enum PlaylistFormat {
        M3U,
        M3U8,
        PLS
    }
    
    public static class PlaylistEntry {
        private String filePath;
        private String title;
        private int duration;
        private String artist;
        private String album;
        
        public PlaylistEntry(String filePath) {
            this.filePath = filePath;
            this.title = "";
            this.duration = -1;
            this.artist = "";
            this.album = "";
        }
        
        public PlaylistEntry(String filePath, String title, int duration) {
            this.filePath = filePath;
            this.title = title;
            this.duration = duration;
            this.artist = "";
            this.album = "";
        }
        
        public PlaylistEntry(String filePath, String title, int duration, String artist, String album) {
            this.filePath = filePath;
            this.title = title;
            this.duration = duration;
            this.artist = artist;
            this.album = album;
        }
        
        public String getFilePath() {
            return filePath;
        }
        
        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public int getDuration() {
            return duration;
        }
        
        public void setDuration(int duration) {
            this.duration = duration;
        }
        
        public String getArtist() {
            return artist;
        }
        
        public void setArtist(String artist) {
            this.artist = artist;
        }
        
        public String getAlbum() {
            return album;
        }
        
        public void setAlbum(String album) {
            this.album = album;
        }
    }
    
    private PlaylistFormat format;
    private String playlistName;
    private List<PlaylistEntry> entries;
    
    public PlaylistGenerator() {
        this.format = PlaylistFormat.M3U;
        this.playlistName = "playlist";
        this.entries = new ArrayList<>();
    }
    
    public PlaylistGenerator(PlaylistFormat format, String playlistName) {
        this.format = format;
        this.playlistName = playlistName;
        this.entries = new ArrayList<>();
    }
    
    public void addEntry(PlaylistEntry entry) {
        if (entry != null) {
            entries.add(entry);
        }
    }
    
    public void addEntry(String filePath) {
        entries.add(new PlaylistEntry(filePath));
    }
    
    public void addEntry(String filePath, String title, int duration) {
        entries.add(new PlaylistEntry(filePath, title, duration));
    }
    
    public void addEntry(String filePath, String title, int duration, String artist, String album) {
        entries.add(new PlaylistEntry(filePath, title, duration, artist, album));
    }
    
    public void addEntries(List<PlaylistEntry> entries) {
        if (entries != null) {
            this.entries.addAll(entries);
        }
    }
    
    public void clearEntries() {
        entries.clear();
    }
    
    public List<PlaylistEntry> getEntries() {
        return new ArrayList<>(entries);
    }
    
    public String generate(String outputPath) throws IOException {
        return generate(outputPath, false);
    }
    
    public String generate(String outputPath, boolean useRelativePaths) throws IOException {
        String playlistPath = getPlaylistFilePath(outputPath);
        
        switch (format) {
            case M3U:
                generateM3U(playlistPath, useRelativePaths);
                break;
            case M3U8:
                generateM3U8(playlistPath, useRelativePaths);
                break;
            case PLS:
                generatePLS(playlistPath, useRelativePaths);
                break;
        }
        
        return playlistPath;
    }
    
    private String getPlaylistFilePath(String outputPath) {
        String extension;
        
        switch (format) {
            case M3U8:
                extension = ".m3u8";
                break;
            case PLS:
                extension = ".pls";
                break;
            case M3U:
            default:
                extension = ".m3u";
                break;
        }
        
        File outputDir = new File(outputPath);
        if (outputDir.isDirectory()) {
            return outputPath + "/" + playlistName + extension;
        } else {
            return outputPath;
        }
    }
    
    private void generateM3U(String playlistPath, boolean useRelativePaths) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(playlistPath), StandardCharsets.UTF_8))) {
            writer.write("#EXTM3U");
            writer.newLine();
            
            for (PlaylistEntry entry : entries) {
                if (entry.getDuration() >= 0 && !entry.getTitle().isEmpty()) {
                    writer.write("#EXTINF:" + entry.getDuration() + "," + entry.getTitle());
                    writer.newLine();
                }
                
                String path = entry.getFilePath();
                if (useRelativePaths) {
                    path = getRelativePath(playlistPath, path);
                }
                writer.write(path);
                writer.newLine();
            }
        }
    }
    
    private void generateM3U8(String playlistPath, boolean useRelativePaths) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(playlistPath), StandardCharsets.UTF_8))) {
            writer.write("#EXTM3U");
            writer.newLine();
            
            for (PlaylistEntry entry : entries) {
                if (entry.getDuration() >= 0 && !entry.getTitle().isEmpty()) {
                    writer.write("#EXTINF:" + entry.getDuration() + "," + entry.getTitle());
                    writer.newLine();
                }
                
                String path = entry.getFilePath();
                if (useRelativePaths) {
                    path = getRelativePath(playlistPath, path);
                }
                writer.write(path);
                writer.newLine();
            }
        }
    }
    
    private void generatePLS(String playlistPath, boolean useRelativePaths) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(playlistPath), StandardCharsets.UTF_8))) {
            writer.write("[playlist]");
            writer.newLine();
            
            for (int i = 0; i < entries.size(); i++) {
                PlaylistEntry entry = entries.get(i);
                int index = i + 1;
                
                String path = entry.getFilePath();
                if (useRelativePaths) {
                    path = getRelativePath(playlistPath, path);
                }
                
                writer.write("File" + index + "=" + path);
                writer.newLine();
                
                if (!entry.getTitle().isEmpty()) {
                    writer.write("Title" + index + "=" + entry.getTitle());
                    writer.newLine();
                }
                
                if (entry.getDuration() >= 0) {
                    writer.write("Length" + index + "=" + entry.getDuration());
                    writer.newLine();
                }
            }
            
            writer.write("NumberOfEntries=" + entries.size());
            writer.newLine();
            writer.write("Version=2");
            writer.newLine();
        }
    }
    
    private String getRelativePath(String basePath, String targetPath) {
        try {
            Path base = Paths.get(basePath).getParent();
            Path target = Paths.get(targetPath);
            
            if (base == null) {
                return targetPath;
            }
            
            Path relative = base.relativize(target);
            return relative.toString();
        } catch (Exception e) {
            return targetPath;
        }
    }
    
    public static PlaylistGenerator fromFiles(List<String> filePaths) {
        PlaylistGenerator generator = new PlaylistGenerator();
        
        for (String filePath : filePaths) {
            File file = new File(filePath);
            if (file.exists()) {
                generator.addEntry(filePath, file.getName(), -1);
            }
        }
        
        return generator;
    }
    
    public static PlaylistGenerator fromFiles(List<String> filePaths, List<PlaylistEntry> metadata) {
        PlaylistGenerator generator = new PlaylistGenerator();
        
        for (int i = 0; i < filePaths.size() && i < metadata.size(); i++) {
            PlaylistEntry entry = metadata.get(i);
            entry.setFilePath(filePaths.get(i));
            generator.addEntry(entry);
        }
        
        return generator;
    }
    
    public PlaylistFormat getFormat() {
        return format;
    }
    
    public void setFormat(PlaylistFormat format) {
        this.format = format;
    }
    
    public String getPlaylistName() {
        return playlistName;
    }
    
    public void setPlaylistName(String playlistName) {
        this.playlistName = playlistName;
    }
    
    public int size() {
        return entries.size();
    }
    
    public boolean isEmpty() {
        return entries.isEmpty();
    }
}

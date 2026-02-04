package com.filemanager.plugin.impl.audioconverter.utils;

import com.filemanager.plugin.ExecutionContext;

import java.io.File;

public class AudioFileUtils {
    
    private static final long CUE_TRACK_SIZE_THRESHOLD = 100 * 1024 * 1024; // 100MB
    
    private static final String[] AUDIO_EXTENSIONS = {
        ".mp3", ".flac", ".wav", ".aac", ".ogg", ".wma", 
        ".m4a", ".ape", ".m4p", ".mp4"
    };
    
    public static boolean isAudioFile(File file) {
        if (!file.isFile()) {
            return false;
        }
        
        String fileName = file.getName().toLowerCase();
        for (String ext : AUDIO_EXTENSIONS) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean shouldSkipCueTrack(File file, ExecutionContext context) {
        if (file.length() <= CUE_TRACK_SIZE_THRESHOLD) {
            return false;
        }
        
        File parentDir = file.getParentFile();
        if (parentDir != null) {
            File[] cueFiles = parentDir.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".cue"));
            if (cueFiles != null && cueFiles.length > 0) {
                context.logDebug("Found CUE file in directory: " + parentDir.getPath());
                return true;
            }
        }
        
        return false;
    }
    
    public static String formatFilename(String filename) {
        String formatted = filename.trim();
        return formatted;
    }
    
    public static String changeExtension(String fileName, String newExtension) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex + 1) + newExtension;
        }
        return fileName + "." + newExtension;
    }
    
    public static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }
}
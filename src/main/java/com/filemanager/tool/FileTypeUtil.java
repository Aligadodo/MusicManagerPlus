package com.filemanager.tool;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @author 28667
 */
public class FileTypeUtil {
    public static List<String> MUSIC_TYPES = Arrays.asList("dsf", "dff", "dts", "ape", "wav", "flac",
            "m4a", "dfd", "tak", "tta", "wv", "mp3", "aac", "ogg", "wma");
    public static boolean isMusicFile(File file){
        if(file.isDirectory()){
            return false;
        }
        String ext = "";
        int dot = file.getName().lastIndexOf('.');
        if (dot > 0) {
            ext = file.getName().substring(dot);
        }
        return MUSIC_TYPES.contains(ext.toLowerCase());
    }
}

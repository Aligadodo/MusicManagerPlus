package com.filemanager.plugin.util;

import java.io.File;

public class FileStatisticInfo {
    public File file;
    public String type;
    public String oriName;
    public String classicName;
    public int fileNameLength;
    public double fileSizeMb;

    public static FileStatisticInfo create(File file) {
        FileStatisticInfo statisticInfo = new FileStatisticInfo();
        statisticInfo.file = file;
        statisticInfo.type = getFileType(file);
        statisticInfo.fileSizeMb = getFileSizeMB(file);
        String filename = file.getName();
        if (filename.indexOf('.') > 0) {
            filename = filename.substring(0, filename.lastIndexOf('.'));
        }
        statisticInfo.oriName = filename;
        statisticInfo.fileNameLength = filename.length();
        statisticInfo.classicName = toClassicName(filename);
        return statisticInfo;
    }

    private static String getFileType(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0) {
            return name.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    private static double getFileSizeMB(File file) {
        long bytes = file.length();
        return bytes / (1024.0 * 1024.0);
    }

    private static String toClassicName(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9\\s\\-_()\\[\\]]", "");
    }

    public boolean isMusic() {
        String musicTypes = "mp3,flac,wav,aiff,iso,ape,asf,dfd,dsf,dts,dff";
        return musicTypes.contains(this.type);
    }
}
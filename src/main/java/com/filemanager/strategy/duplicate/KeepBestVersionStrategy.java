/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-28
 */
package com.filemanager.strategy.duplicate;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 保留最佳版本策略
 * 根据文件质量、大小、修改时间等因素选择保留最佳版本
 */
public class KeepBestVersionStrategy implements DuplicateStrategy {
    // 常见媒体类型定义
    private static final List<String> EXT_AUDIO;
    private static final List<String> EXT_VIDEO;
    private static final List<String> EXT_IMAGE;
    
    static {
        // 音频文件格式（常见）
        EXT_AUDIO = new ArrayList<>();
        EXT_AUDIO.add("mp3");
        EXT_AUDIO.add("flac");
        EXT_AUDIO.add("wav");
        EXT_AUDIO.add("aac");
        EXT_AUDIO.add("m4a");
        EXT_AUDIO.add("ogg");
        EXT_AUDIO.add("wma");
        EXT_AUDIO.add("ape");
        EXT_AUDIO.add("alac");
        EXT_AUDIO.add("aiff");
        EXT_AUDIO.add("dsf");
        EXT_AUDIO.add("dff");
        
        // 视频文件格式（常见）
        EXT_VIDEO = new ArrayList<>();
        EXT_VIDEO.add("mp4");
        EXT_VIDEO.add("mkv");
        EXT_VIDEO.add("avi");
        EXT_VIDEO.add("mov");
        EXT_VIDEO.add("wmv");
        EXT_VIDEO.add("flv");
        EXT_VIDEO.add("m4v");
        EXT_VIDEO.add("mpg");
        
        // 图片文件格式（常见）
        EXT_IMAGE = new ArrayList<>();
        EXT_IMAGE.add("jpg");
        EXT_IMAGE.add("jpeg");
        EXT_IMAGE.add("png");
        EXT_IMAGE.add("bmp");
        EXT_IMAGE.add("gif");
        EXT_IMAGE.add("webp");
        EXT_IMAGE.add("tiff");
    }
    
    private final boolean keepLargest;
    private final boolean keepNewest;
    private final boolean audioSpecial;
    private final String keepExt;
    
    /**
     * 构造函数
     * @param keepLargest 是否保留最大文件
     * @param keepNewest 是否保留最新文件
     * @param audioSpecial 是否对音频文件进行特殊处理
     * @param keepExt 优先保留的文件扩展名
     */
    public KeepBestVersionStrategy(boolean keepLargest, boolean keepNewest, boolean audioSpecial, String keepExt) {
        this.keepLargest = keepLargest;
        this.keepNewest = keepNewest;
        this.audioSpecial = audioSpecial;
        this.keepExt = keepExt;
    }
    
    @Override
    public List<File> processDuplicates(List<File> duplicates) {
        if (duplicates == null || duplicates.size() <= 1) {
            return duplicates;
        }
        
        List<File> result = new ArrayList<>(duplicates);
        
        // 排序文件，最佳版本排在第一位
        result.sort(new FileQualityComparator());
        
        return result;
    }
    
    @Override
    public String getName() {
        return "保留最佳版本";
    }
    
    @Override
    public String getDescription() {
        return "根据文件质量、大小、修改时间等因素选择保留最佳版本";
    }
    
    /**
     * 文件质量比较器
     */
    private class FileQualityComparator implements Comparator<File> {
        @Override
            public int compare(File f1, File f2) {
                // 1. 优先比较文件扩展名（支持优先级顺序）
                if (keepExt != null && !keepExt.isEmpty()) {
                    List<String> extPriorities = new ArrayList<>();
                    for (String ext : keepExt.split(",")) {
                        extPriorities.add(ext.trim().toLowerCase());
                    }
                    
                    String ext1 = getFileExtension(f1);
                    String ext2 = getFileExtension(f2);
                    
                    int idx1 = extPriorities.indexOf(ext1);
                    int idx2 = extPriorities.indexOf(ext2);
                    
                    if (idx1 != idx2) {
                        if (idx1 == -1) return 1; // f1不在优先级列表，f2优先
                        if (idx2 == -1) return -1; // f2不在优先级列表，f1优先
                        return Integer.compare(idx1, idx2); // 索引小的优先级高
                    }
                }
            
            // 2. 检查是否是音频文件
            String ext1 = getFileExtension(f1);
            String ext2 = getFileExtension(f2);
            boolean isAudio1 = EXT_AUDIO.contains(ext1);
            boolean isAudio2 = EXT_AUDIO.contains(ext2);
            
            if (audioSpecial && isAudio1 && isAudio2) {
                // 音频文件特殊处理：比较码率
                int bitrateCmp = compareAudioBitrate(f1, f2);
                if (bitrateCmp != 0) {
                    return bitrateCmp;
                }
            }
            
            // 3. 比较文件大小
            if (keepLargest) {
                int sizeCmp = Long.compare(f2.length(), f1.length());
                if (sizeCmp != 0) {
                    return sizeCmp;
                }
            }
            
            // 4. 比较文件修改时间
            if (keepNewest) {
                int timeCmp = Long.compare(f2.lastModified(), f1.lastModified());
                if (timeCmp != 0) {
                    return timeCmp;
                }
                
                // 尝试比较创建时间
                try {
                    BasicFileAttributes attr1 = Files.readAttributes(f1.toPath(), BasicFileAttributes.class);
                    BasicFileAttributes attr2 = Files.readAttributes(f2.toPath(), BasicFileAttributes.class);
                    int createTimeCmp = attr2.creationTime().compareTo(attr1.creationTime());
                    if (createTimeCmp != 0) {
                        return createTimeCmp;
                    }
                } catch (IOException e) {
                    // 忽略异常
                }
            }
            
            // 5. 默认：比较文件名长度（通常不带序号的文件名更短）
            int lenCmp = Integer.compare(f1.getName().length(), f2.getName().length());
            if (lenCmp != 0) {
                return lenCmp;
            }
            
            // 6. 最后的比较：文件路径
            return f1.getAbsolutePath().compareTo(f2.getAbsolutePath());
        }
        
        /**
         * 比较音频文件码率
         */
        private int compareAudioBitrate(File f1, File f2) {
            try {
                AudioFile audioFile1 = AudioFileIO.read(f1);
                AudioFile audioFile2 = AudioFileIO.read(f2);
                
                if (audioFile1 != null && audioFile2 != null) {
                    AudioHeader header1 = audioFile1.getAudioHeader();
                    AudioHeader header2 = audioFile2.getAudioHeader();
                    
                    if (header1 != null && header2 != null) {
                        long bitrate1 = header1.getBitRateAsNumber();
                        long bitrate2 = header2.getBitRateAsNumber();
                        return Long.compare(bitrate2, bitrate1); // 码率高的优先
                    }
                }
            } catch (Exception e) {
                // 忽略异常
            }
            return 0;
        }
        
        /**
         * 获取文件扩展名
         */
        private String getFileExtension(File file) {
            String name = file.getName();
            int lastDotIndex = name.lastIndexOf('.');
            if (lastDotIndex > 0 && lastDotIndex < name.length() - 1) {
                return name.substring(lastDotIndex + 1).toLowerCase();
            }
            return "";
        }
    }
}

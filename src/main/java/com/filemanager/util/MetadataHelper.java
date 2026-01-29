/*
 * Copyright (c) 2026 hrcao (chrse1997@163.com)
 * Licensed under GPLv3 + Non-Commercial Clause.
 * You may not use this file except in compliance with the License.
 * See the LICENSE file in the project root for more information.
 * Author: hrcao
 * Mail: chrse1997@163.com
 * Date: 2026-01-12
 */
package com.filemanager.util;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MetadataHelper {
    static {
        Logger.getLogger("org.jaudiotagger").setLevel(Level.OFF);
    }

    public static AudioMeta getSmartMetadata(File file, boolean forceFile) {
        AudioMeta meta = new AudioMeta();
        boolean tagValid = false;

        if (!forceFile) {
            try {
                AudioFile f = AudioFileIO.read(file);
                Tag tag = f.getTag();
                if (tag != null) {
                    meta.title = tag.getFirst(FieldKey.TITLE);
                    meta.artist = tag.getFirst(FieldKey.ARTIST);
                    meta.album = tag.getFirst(FieldKey.ALBUM);
                    meta.year = tag.getFirst(FieldKey.YEAR);
                    meta.track = tag.getFirst(FieldKey.TRACK);
                    if (isValid(meta.title) || isValid(meta.artist)) tagValid = true;
                    if (isMessy(meta.title) || isMessy(meta.artist) || isMessy(meta.album)) tagValid = false;
                }
            } catch (Exception e) { /* ignore */ }
        }

        if (!tagValid) {
            AudioMeta guessed = extractFromFileSystem(file);
            meta = guessed;
            meta.isGuessed = true;
        } else {
            if (!isValid(meta.track)) {
                AudioMeta guessed = extractFromFileSystem(file);
                if (isValid(guessed.track)) meta.track = guessed.track;
            }
        }
        if (meta.title == null || meta.title.isEmpty()) meta.title = removeExt(file.getName());
        return meta;
    }

    private static boolean isValid(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static boolean isMessy(String s) {
        if (s == null) return false;
        return s.contains("\uFFFD") || s.contains("????");
    }

    public static AudioMeta extractFromFileSystem(File file) {
        AudioMeta meta = new AudioMeta();
        String name = file.getName();
        
        // 首先从文件名提取信息
        if (name.matches("^\\d+[.\\s-].*")) {
            // Split by number followed by any separator pattern, then take rest as title
            String trackNumber = name.replaceAll("^([\\d]+).*", "$1").trim();
            meta.track = trackNumber;
            // Remove track number and separator from beginning of string
            String titlePart = name.replaceFirst("^\\d+[.\\s-]+", "").trim();
            // 只有当标题部分不是文件扩展名时才设置标题
            String titleWithoutExt = removeExt(titlePart);
            if (!titleWithoutExt.isEmpty() && !titleWithoutExt.equalsIgnoreCase("wav") && 
                !titleWithoutExt.equalsIgnoreCase("flac") && !titleWithoutExt.equalsIgnoreCase("mp3")) {
                meta.title = titleWithoutExt;
            }
        } else if (name.contains(" - ")) {
            String[] parts = name.split(" - ");
            if (parts.length >= 2) {
                // 从文件名提取艺术家和标题
                String artistPart = parts[0].trim();
                String titlePart = removeExt(parts[1].trim());
                
                // 处理多艺术家的情况（如 "陶喆 蔡依林 - 今天你要嫁给我"）
                // 检查艺术家部分是否包含空格分隔的多个艺术家
                if (artistPart.contains(" ")) {
                    // 尝试分割艺术家部分
                    String[] artistCandidates = artistPart.split("\\s+");
                    // 如果分割后有多部分，可能是多艺术家
                    if (artistCandidates.length > 1) {
                        // 检查是否是常见的多艺术家模式
                        boolean isMultiArtist = true;
                        for (String artist : artistCandidates) {
                            // 如果某个部分太长或包含特殊字符，可能不是多艺术家
                            if (artist.length() > 10 || artist.matches(".*\\d{4}.*")) {
                                isMultiArtist = false;
                                break;
                            }
                        }
                        if (isMultiArtist) {
                            // 使用逗号分隔多个艺术家
                            meta.artist = String.join(", ", artistCandidates);
                        } else {
                            meta.artist = artistPart;
                        }
                    } else {
                        meta.artist = artistPart;
                    }
                } else {
                    meta.artist = artistPart;
                }
                
                // 设置标题
                if (!titlePart.isEmpty()) {
                    meta.title = titlePart;
                }
                
                // 如果有第三部分，可能是专辑
                if (parts.length >= 3) {
                    meta.album = parts[1].trim();
                }
            }
        } else {
            // 只有当文件名不是纯数字时才设置标题
            String titleWithoutExt = removeExt(name);
            if (!titleWithoutExt.matches("^\\d+$")) {
                meta.title = titleWithoutExt;
            }
        }

        // 从目录结构提取信息
        File parent = file.getParentFile();
        if (parent != null) {
            String parentName = parent.getName();
            
            // 去除开头的格式标识（如"DTS-"、"SACD-"等）
            parentName = parentName.replaceFirst("^(DTS|DTS-|SACD|SACD-)\\s*-?\\s*", "");
            
            // 跳过"Split - WAV"这样的目录，继续向上查找
            if (parentName.equals("Split - WAV") || parentName.equals("Split - FLAC") || 
                parentName.equals("Split - MP3") || parentName.equals("Split")) {
                File grandParent = parent.getParentFile();
                if (grandParent != null) {
                    parentName = grandParent.getName();
                    parent = grandParent;
                }
            }
            
            // 尝试从父目录名提取年份和专辑
            if (parentName.matches("^\\d{4}\\.\\d{2}\\.\\d{2}.*")) {
                // 处理类似 "2013.06.11 - 再见你好吗" 的格式
                String year = parentName.substring(0, 4);
                if (year.matches("\\d{4}")) {
                    meta.year = year;
                }
                // 提取专辑名（去掉日期和分隔符后的部分）
                String albumPart = parentName.substring(10).trim();
                // 去除日期后面的分隔符（如" - "）
                albumPart = albumPart.replaceFirst("^[-\\s]+", "");
                // 去除版本信息（如【xxx】[xxx]）
                albumPart = cleanAlbumName(albumPart);
                if (!albumPart.isEmpty()) {
                    meta.album = albumPart;
                }
            } else if (parentName.matches("^\\d{4}.*")) {
                // 提取年份
                String year = parentName.substring(0, 4);
                if (year.matches("\\d{4}")) {
                    meta.year = year;
                }
                
                // 提取专辑名（去掉年份和分隔符后的部分）
                String albumPart = parentName.substring(4).trim();
                // 去除年份后面的分隔符（如" - "）
                albumPart = albumPart.replaceFirst("^[-\\s]+", "");
                // 去除版本信息（如【xxx】[xxx]）
                albumPart = cleanAlbumName(albumPart);
                if (!albumPart.isEmpty()) {
                    meta.album = albumPart;
                }
            } else if (parentName.contains(" - ")) {
                String[] parts = parentName.split(" - ", 2);
                if (parts.length >= 2) {
                    // 第一部分可能是年份或艺术家
                    String firstPart = parts[0].trim();
                    String secondPart = parts[1].trim();
                    
                    // 检查是否是"艺术家.年份"格式
                    if (firstPart.contains(".")) {
                        String[] firstParts = firstPart.split("\\.", 2);
                        if (firstParts.length >= 2) {
                            String artistPart = firstParts[0].trim();
                            String yearPart = firstParts[1].trim();
                            
                            // 如果第二部分是年份格式，提取年份
                            if (yearPart.matches("^\\d{4}$")) {
                                meta.year = yearPart;
                                if (!isValid(meta.artist)) {
                                    meta.artist = cleanArtistName(artistPart);
                                }
                                meta.album = cleanAlbumName(secondPart);
                            }
                        }
                    }
                    
                    // 如果第一部分是年份格式，提取年份
                    if (firstPart.matches("^\\d{4}$")) {
                        meta.year = firstPart;
                        meta.album = cleanAlbumName(secondPart);
                    } else if (firstPart.matches("^\\d{8}\\s+\\d+")) {
                        // 处理类似 "20151005 02" 的格式
                        String dateStr = firstPart.substring(0, 8);
                        if (dateStr.matches("\\d{8}")) {
                            meta.year = dateStr.substring(0, 4);
                        }
                        meta.album = cleanAlbumName(secondPart);
                    } else if (firstPart.matches("^\\d{8}\\s+.*")) {
                        // 处理类似 "20151005 02 陈奕迅 最冷一天" 的格式
                        String dateStr = firstPart.substring(0, 8);
                        if (dateStr.matches("\\d{8}")) {
                            meta.year = dateStr.substring(0, 4);
                        }
                        // 从第一部分提取专辑名（去掉日期和空格后的部分）
                        String albumFromFirst = firstPart.substring(8).trim();
                        // 去除开头的数字和空格（如 "02 "）
                        albumFromFirst = albumFromFirst.replaceFirst("^\\d+\\s+", "");
                        // 如果第二部分不为空，合并两部分作为专辑名
                        if (!secondPart.isEmpty()) {
                            meta.album = cleanAlbumName(albumFromFirst + " " + secondPart);
                        } else {
                            meta.album = cleanAlbumName(albumFromFirst);
                        }
                    } else if (secondPart.matches("^\\d{4}\\s*[-–—]?\\s*.*")) {
                        // 处理类似 "艺术家 - 1999 - 专辑" 的格式
                        // 从第二部分提取年份
                        String yearMatch = secondPart.replaceFirst("^(\\d{4}).*", "$1");
                        if (yearMatch.matches("\\d{4}")) {
                            meta.year = yearMatch;
                        }
                        // 从第二部分提取专辑名（去掉年份和分隔符）
                        String albumFromSecond = secondPart.replaceFirst("^\\d{4}\\s*[-–—]?\\s*", "");
                        meta.album = cleanAlbumName(albumFromSecond);
                    } else {
                        // 第一部分是艺术家
                        if (!isValid(meta.artist)) {
                            meta.artist = cleanArtistName(firstPart);
                        }
                        meta.album = cleanAlbumName(secondPart);
                    }
                }
            } else {
                meta.album = cleanAlbumName(parentName);
            }

            // 从祖父目录提取艺术家和处理特殊目录结构
            File grandParent = parent.getParentFile();
            if (grandParent != null) {
                String grandParentName = grandParent.getName();
                
                // 处理类似 "1 - 国歌、战友情" 的编号目录
                if (grandParentName.equals("1 - 国歌、战友情") && parentName.equals("怀念战友")) {
                    meta.artist = "未知";
                    meta.album = parentName;
                }
                // 处理类似 "J - 金海心" 的格式（父目录）
                else if (parentName.matches("^[A-Z]\\s*-\\s*.*")) {
                    String[] parts = parentName.split("\\s*-\\s*", 2);
                    if (parts.length >= 2) {
                        meta.artist = parts[1].trim();
                        meta.album = parentName;
                    }
                } 
                // 处理类似 "0 - 中文歌手"、"0 - 日韩歌手" 的分类目录
                else if (grandParentName.matches("^\\d+\\s*-\\s*.*")) {
                    String[] grandParts = grandParentName.split("\\s*-\\s*", 2);
                    if (grandParts.length >= 2) {
                        String category = grandParts[1].trim();
                        
                        // 检查父目录是否包含艺术家信息
                        if (parentName.contains(" - ")) {
                            // 父目录包含艺术家和专辑信息
                            String[] parts = parentName.split(" - ", 2);
                            if (parts.length >= 2) {
                                meta.artist = parts[0].trim();
                                meta.album = parts[1].trim();
                            }
                        } else {
                            // 父目录是艺术家名
                            meta.artist = parentName;
                            
                            // 对于日韩歌手，使用分类目录作为专辑
                            if (category.equals("日韩歌手")) {
                                meta.album = category;
                            } else {
                                meta.album = parentName;
                            }
                        }
                    }
                }
                // 常规处理祖父目录
                else if (!isValid(meta.artist)) {
                    if (grandParentName.contains(" - ")) {
                        // 处理 "C - Artist" 这样的格式
                        String[] parts = grandParentName.split(" - ", 2);
                        if (parts.length >= 2) {
                            // 优先选择第二部分作为艺术家，并清理额外信息
                            meta.artist = cleanArtistName(parts[1].trim());
                        } else {
                            meta.artist = cleanArtistName(grandParentName);
                        }
                    } else {
                        meta.artist = cleanArtistName(grandParentName);
                    }
                }
            }
        }
        
        return meta;
    }

    private static String cleanAlbumName(String albumName) {
        // 去除版本信息
        String cleaned = albumName;
        
        // 去除开头的格式标识（如"DTS-"、"DTS"等）
        cleaned = cleaned.replaceFirst("^(DTS|DTS-|SACD|SACD-)\\s*-?\\s*", "");
        
        // 去除开头的年份和分隔符（如 "1999 - " 或 "1999-"）
        cleaned = cleaned.replaceFirst("^\\d{4}\\s*[-–—]?\\s*", "");
        
        // 去除【xxx】格式的信息
        cleaned = cleaned.replaceAll("\\【[^】]*\\】", "");
        // 去除[xxx]格式的信息
        cleaned = cleaned.replaceAll("\\[[^\\]]*\\]", "");
        // 去除《xxx》格式的信息
        cleaned = cleaned.replaceAll("\\《[^》]*\\》", "");
        // 去除(xxx)格式的信息
        cleaned = cleaned.replaceAll("\\([^)]*\\)", "");
        // 去除文件扩展名
        cleaned = removeExt(cleaned);
        // 去除年份后面的点号（如.2020）
        cleaned = cleaned.replaceAll("\\.\\d{4}", "");
        // 去除格式标识（如WAV、FLAC、MP3等）
        cleaned = cleaned.replaceAll("\\b(WAV|FLAC|MP3|M4A|OGG|APE|DTS|SACD)\\b", "");
        // 去除CD标识（如CD01、CD1、CD2等）
        cleaned = cleaned.replaceAll("\\bCD\\d+\\b", "");
        // 去除末尾的分隔符和空格
        cleaned = cleaned.replaceAll("\\s*[-–—]\\s*$", "");
        // 去除多余空格
        cleaned = cleaned.trim().replaceAll("\\s+", " ");
        
        return cleaned;
    }

    private static String cleanArtistName(String artistName) {
        // Remove common suffixes and extra information from artist names
        // Remove patterns like 【xxx】【yyy】
        String cleaned = artistName.replaceAll("\\【.*?\\】", "");
        // Remove patterns like [xxx][yyy]
        cleaned = cleaned.replaceAll("\\[.*?\\]", "");
        // Remove patterns like (xxx)(yyy)
        cleaned = cleaned.replaceAll("\\(.*?\\)", "");
        // Remove patterns like 《xxx》《yyy》
        cleaned = cleaned.replaceAll("\\《.*?\\》", "");
        // Remove file extensions if present
        cleaned = removeExt(cleaned);
        // Remove year patterns like .2020, .2021, etc.
        cleaned = cleaned.replaceAll("\\.\\d{4}", "");
        // Remove format identifiers like DTS, SACD, etc.
        cleaned = cleaned.replaceFirst("^(DTS|DTS-|SACD|SACD-)\\s*-?\\s*", "");
        // Remove CD identifiers like CD01, CD1, CD2, etc.
        cleaned = cleaned.replaceAll("\\bCD\\d+\\b", "");
        // Remove format identifiers at the end
        cleaned = cleaned.replaceAll("\\s*[-–—]\\s*(DTS|SACD|WAV|FLAC|MP3|M4A|OGG|APE)\\b", "");
        return cleaned.trim();
    }

    private static String removeExt(String s) {
        int d = s.lastIndexOf('.');
        return d > 0 ? s.substring(0, d) : s;
    }

    public static String format(String template, AudioMeta meta) {
        return template.replace("%artist%", meta.artist == null ? "" : meta.artist)
                .replace("%album%", meta.album == null ? "" : meta.album)
                .replace("%title%", meta.title == null ? "" : meta.title)
                .replace("%year%", meta.year == null ? "" : meta.year)
                .replace("%track%", meta.track == null ? "" : meta.track);
    }

    @Data
    @NoArgsConstructor
    public static class AudioMeta {
        public String artist = "";
        public String album = "";
        public String title = "";
        public String year = "";
        public String track = "";
        public String genre = "";
        public boolean isGuessed = false; // 标记是否是猜出来的（文件名推断）

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

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getYear() {
            return year;
        }

        public void setYear(String year) {
            this.year = year;
        }

        public String getTrack() {
            return track;
        }

        public void setTrack(String track) {
            this.track = track;
        }

        public String getGenre() {
            return genre;
        }

        public void setGenre(String genre) {
            this.genre = genre;
        }

        public boolean isGuessed() {
            return isGuessed;
        }

        public void setGuessed(boolean guessed) {
            isGuessed = guessed;
        }
    }
}
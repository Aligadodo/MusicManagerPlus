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
        if (name.matches("^\\d+[.\\s-].*")) {
            // Split by number followed by any separator pattern, then take the rest as title
            String trackNumber = name.replaceAll("^([\\d]+).*", "$1").trim();
            meta.track = trackNumber;
            // Remove track number and separator from the beginning of the string
            String titlePart = name.replaceFirst("^\\d+[.\\s-]+", "").trim();
            meta.title = removeExt(titlePart);
        } else if (name.contains(" - ")) {
            String[] parts = name.split(" - ");
            if (parts.length >= 2) {
                meta.artist = parts[0].trim();
                meta.title = removeExt(parts[1].trim());
            }
            if (parts.length >= 3) meta.album = parts[1].trim();
        } else {
            meta.title = removeExt(name);
        }

        File parent = file.getParentFile();
        if (parent != null) {
            String parentName = parent.getName();
            if (parentName.matches("^\\d{4}\\s+-\\s+.*")) {
                meta.year = parentName.substring(0, 4);
                meta.album = parentName.substring(7).trim();
            } else if (parentName.contains(" - ")) {
                String[] parts = parentName.split(" - ", 2);
                if (!isValid(meta.artist)) {
                    // For parent directory, prefer the first part as artist
                    // Only use the second part if the first part looks like a category letter (single character)
                    String potentialArtist1 = cleanArtistName(parts[0].trim());
                    String potentialArtist2 = cleanArtistName(parts[1].trim());
                    
                    // If first part is a single character (like "C", "A", etc.), use second part
                    // Otherwise, prefer the first part as it's usually the artist name
                    if (potentialArtist1.length() <= 1 && potentialArtist2.length() > 1) {
                        meta.artist = potentialArtist2;
                    } else {
                        meta.artist = potentialArtist1;
                    }
                }
                meta.album = parts[1].trim();
            } else {
                meta.album = parentName;
            }

            File grandParent = parent.getParentFile();
            if (grandParent != null && !isValid(meta.artist)) {
                String grandParentName = grandParent.getName();
                if (grandParentName.contains(" - ")) {
                    //同样处理祖父目录，尝试从 "C - Artist" 这样的格式中提取艺术家
                    String[] parts = grandParentName.split(" - ", 2);
                    if (parts.length >= 2) {
                        //优先选择第二部分作为艺术家，并清理额外信息
                        meta.artist = cleanArtistName(parts[1].trim());
                    } else {
                        meta.artist = grandParentName;
                    }
                } else {
                    meta.artist = grandParentName;
                }
            }
        }
        return meta;
    }

    private static String cleanArtistName(String artistName) {
        // Remove common suffixes and extra information from artist names
        // Remove patterns like 【xxx】【yyy】
        String cleaned = artistName.replaceAll("\\【.*?\\】", "");
        // Remove patterns like [xxx][yyy]
        cleaned = cleaned.replaceAll("\\[.*?\\]", "");
        // Remove patterns like (xxx)(yyy)
        cleaned = cleaned.replaceAll("\\(.*?\\)", "");
        // Remove file extensions if present
        cleaned = removeExt(cleaned);
        // Remove year patterns like .2020, .2021, etc.
        cleaned = cleaned.replaceAll("\\.\\d{4}", "");
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
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
            String[] parts = name.split("[.\\s-]", 2);
            if (parts.length > 1) {
                meta.track = parts[0].trim();
                meta.title = removeExt(parts[1].trim());
            }
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
                if (!isValid(meta.artist)) meta.artist = parts[0].trim();
                meta.album = parts[1].trim();
            } else {
                meta.album = parentName;
            }

            File grandParent = parent.getParentFile();
            if (grandParent != null && !isValid(meta.artist)) meta.artist = grandParent.getName();
        }
        return meta;
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
        public boolean isGuessed = false; // 标记是否是猜出来的（文件名推断）
    }
}
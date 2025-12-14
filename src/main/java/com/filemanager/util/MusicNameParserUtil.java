package com.filemanager.util;

import com.filemanager.model.FileStatisticInfo;
import com.filemanager.model.MusicInfo;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MusicNameParserUtil {
    public static Map<String, File> existingDirMapping = new HashMap<>();
    public static final Pattern eng = Pattern.compile("[0-9a-zA-Z\\s]*");
    public static final String full_music_types = "mp3,flac,wav,aiff,ape,dfd,dsf,iso";
    static {
        scanArtistDirs("X:\\0 - 中文歌手");
        scanArtistDirs("X:\\0 - 欧美歌手");
        scanArtistDirs("X:\\0 - 日韩歌手");
        scanArtistDirs("X:\\1 - 西式古典");
        scanArtistDirs("X:\\1 - 乐团乐队");
        scanArtistDirs("X:\\1 - 80年代老歌手");

        System.out.println(String.format("find %d dirs ", existingDirMapping.size()));
    }

    public static boolean isMusicFile(String filename){
        return StringUtils.endsWithAny(filename.toLowerCase(), full_music_types.split(","));
    }


    private static void scanArtistDirs(String dir) {
        if(new File(dir).listFiles()==null){
            return;
        }
        Arrays.stream(Objects.requireNonNull(new File(dir).listFiles())).forEach(i -> {
            if (i.isDirectory()) {
                if (StringUtils.contains(i.getName(), "-")) {
                    Arrays.stream(LanguageUtil.toClassicName(i.getName()).split("-"))
                            .filter(MusicNameParserUtil::isValidName)
                            .forEach(name -> {
                                existingDirMapping.put(LanguageUtil.toClassicName(name), i);
                            });
                } else if (StringUtils.contains(i.getName(), " / ")) {
                    Arrays.stream(LanguageUtil.toClassicName(i.getName()).split("/"))
                            .filter(MusicNameParserUtil::isValidName)
                            .forEach(name -> {
                                existingDirMapping.put(LanguageUtil.toClassicName(name), i);
                            });
                } else {
                    existingDirMapping.put(LanguageUtil.toClassicName(i.getName()), i);
                }
            }
        });
    }


    private static boolean isValidName(String name) {
        if (StringUtils.containsAny(name,
                "群星", "Star", "STAR", "ARTIST", "Artist", "artist",
                "CD", "DSC", "cd", "dsc", "HZ", "BIT", "CLIP", "hz", "bit", "clip", "OP", "op", "声道", "测试",
                "wav", "WAV", "flac", "FLAC", "mp3", "MP3", "dsf", "DSF", "APE", "ape", "AIFF", "aiff", "iso", "ISO",
                "#", ":", "《", "》", "：", "-", "_",
                "最", "的", "音乐", "系列", "雨果", "'T")) {
            return false;
        }
        if (StringUtils.isNumeric(name)) {
            return false;
        }
        if (eng.matcher(name).matches() && name.length() < 9) {
            return false;
        }
        if (LanguageUtil.isJapaneseStr(name, 2) && name.length() < 7 && name.length() > 3) {
            return true;
        }
        return name.length() > 2 && name.length() < 8;
    }


    /**
     * 格式化歌曲名字
     **/
    public static String getFormatedMusicName(MusicInfo musicInfo, FileStatisticInfo statisticInfo) {
        String songName = musicInfo.songName;
        if (StringUtils.isBlank(songName)) {
            songName = statisticInfo.oriName;
        }
        String artist = musicInfo.artist;
        if (songName.contains("--")) {
            songName = songName.split("--")[1] + "-" + songName.split("--")[0];
            musicInfo.artist = songName.split("-")[0];
        }
        File file = musicInfo.file;
        if (StringUtils.isBlank(songName)) {
            songName = file.getName().substring(0, file.getName().lastIndexOf('.')).replaceAll(" - ", "-");
        }
        songName = LanguageUtil.toHalfWidth(LanguageUtil.toSimpleChinese(songName));
        songName = songName.replaceAll("^[0-9]{1,3}[./s]{0,2}", "");
        songName = songName.replace("(1)", "");
        songName = songName.replace("(2)", "");
        songName = songName.replace("+", " ");
        songName = songName.trim();
        FileStatisticInfo parentStatistic = FileStatisticInfo.create(file.getParentFile());
        if (artist == null) {
            artist = getPossibleArtistFromDirName(musicInfo, statisticInfo, parentStatistic);
        }
        if (artist == null && (parentStatistic.fileNameLength < 5 || parentStatistic.oriName.contains("CD"))) {
            parentStatistic = FileStatisticInfo.create(file.getParentFile());
            artist = getPossibleArtistFromDirName(musicInfo, statisticInfo, parentStatistic);
        }
        if (artist != null) {
            if (StringUtils.isNoneBlank(artist) && StringUtils.isNoneBlank(songName) && !songName.contains(artist)) {
                songName = artist + "-" + songName;
            }
            if (songName.contains(artist) && songName.endsWith(artist)) {
                int index = songName.lastIndexOf(artist);
                if (index > 3) {
                    char beforeArtChar = songName.charAt(index - 1);
                    if (beforeArtChar == ' ' || beforeArtChar == '-') {
                        songName = artist + "-" + songName.substring(0, index).trim();
                    }
                }
            }
        }
        if (artist != null && songName.indexOf(artist) > 3
                && parentStatistic.countCNChars > 5 && parentStatistic.fileNameLength < parentStatistic.countCNChars + 8
        ) {
            String firstSongName = songName.split(" ")[0].split("\\(")[0].split("-")[0];
            if (firstSongName.length() > artist.length()) {
                songName = artist + "-" + firstSongName;
            }
        }
        if (StringUtils.isNoneBlank(artist) && StringUtils.isNoneBlank(songName) && songName.startsWith(artist) && !songName.contains(artist + "-")) {
            songName = songName.replace(artist, artist + "-");
        }
        return songName;
    }


    private static String getPossibleArtistFromDirName(MusicInfo musicInfo, FileStatisticInfo statisticInfo, FileStatisticInfo parentStatistic) {
        String dirName = parentStatistic.oriName;
        String candidate = MusicNameParserUtil.getDirNameFromArtists(parentStatistic.file, existingDirMapping.keySet());
        if (candidate != null) {
            return candidate;
        }
        if (StringUtils.isEmpty(musicInfo.artist)) {
            if (statisticInfo.fileNameLength < 30 && statisticInfo.countENChars > 15
                    && parentStatistic.fileNameLength < 15 && parentStatistic.countENChars > 8
                    && parentStatistic.fileNameLength < parentStatistic.countENChars + 5) {
                return dirName;
            }
            if (statisticInfo.fileNameLength < 15 && statisticInfo.countCNChars > 7
                    && parentStatistic.fileNameLength < 7 && parentStatistic.countCNChars > 5
                    && parentStatistic.fileNameLength < parentStatistic.countCNChars + 8) {
                return dirName;
            }
        }
        return null;
    }

    public static String getDirNameFromMusicFile(File file) {
        return getDirNameFromMusicFile(file, true);
    }

    /**
     * 从文件名中获取第一格关于歌手的信息并做一些格式化
     **/
    public static String getDirNameFromMusicFile(File file, Boolean chineseOnly) {
        String dir = null;
        String filename = file.getName();
        filename = LanguageUtil.toClassicName(filename);
        FileStatisticInfo statisticInfo = FileStatisticInfo.create(file);
        if (chineseOnly && statisticInfo.countCNChars * 2 < statisticInfo.fileNameLength) {
            return null;
        }
        String matchExistingArtist = getDirNameFromArtists(file, existingDirMapping.keySet());
        if (matchExistingArtist != null) {
            return matchExistingArtist;
        }
        String matchExistingArtist2 = getDirNameFromArtists(file.getParentFile(), existingDirMapping.keySet());
        if (matchExistingArtist2 != null) {
            return matchExistingArtist2;
        }
        if (file.getName().contains("-")) {
            dir = Arrays.stream(filename.split("-")).filter(MusicNameParserUtil::isValidName).findFirst().orElse(null);
        }
        if (dir == null) {
            return null;
        }
        if (dir.endsWith(".")) {
            dir = dir.substring(0, dir.length() - 1);
        }
        return LanguageUtil.toClassicName(dir);
    }

    /**
     * 从给定的歌手中挑选可能接近的，去分类
     **/
    public static String getDirNameFromArtists(File file, Collection<String> artists) {
        String dir = null;
        if (file.isDirectory()) {
            dir = file.getName();
        } else {
            List<String> parts = null;
            String filename = LanguageUtil.toHalfWidth(file.getName());
            if (filename.lastIndexOf(".") > 0 && (filename.length() - filename.lastIndexOf(".")) < 6) {
                filename = filename.substring(0, filename.lastIndexOf("."));
            }
            if (filename.endsWith(")")) {
                filename = filename.substring(0, filename.length() - 1).replace("(", "-");
            }
            dir = filename;
        }
        if (dir == null) {
            return null;
        }
        dir = LanguageUtil.toSimpleChinese(dir);
        if (artists.contains(dir)) {
            return dir;
        } else {
            List<String> match = artists.stream().filter(dir::startsWith).collect(Collectors.toList());
            if (match.isEmpty()) {
                match = artists.stream().filter(dir::endsWith).collect(Collectors.toList());
            }
            if (match.isEmpty()) {
                match = TextSpliteUtil.split(dir).stream().filter(artists::contains).collect(Collectors.toList());
            }
            if (match.size() == 1) {
                return match.get(0);
            } else if (match.size() > 1) {
                // 多个匹配的时候，选取匹配最长的那个
                return match.stream().sorted(new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return o2.length() - o1.length();
                    }
                }).collect(Collectors.toList()).get(0);
            }
        }
        if (file.isDirectory() && !LanguageUtil.isEnglishStr(dir, 0)) {
            List<String> matchs = artists.stream().filter(dir::contains).collect(Collectors.toList());
            if (matchs.size() == 1) {
                return matchs.get(0);
            }
        }
        return null;
    }

}

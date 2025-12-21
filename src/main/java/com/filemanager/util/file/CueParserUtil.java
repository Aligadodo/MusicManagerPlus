package com.filemanager.util.file;

import com.filemanager.model.CueSheet;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CueParserUtil {
    // 匹配 CUE 指令的正则：指令 + 引号内的内容
    private static final Pattern PATTERN = Pattern.compile("([A-Z]+)\\s+(.*)");


    public static CueSheet parse(Path path) throws Exception{
        CueSheet cue = new CueSheet();
        CueSheet.CueTrack currentCueTrack = null;

        // CUE 常见编码为 GBK 或 UTF-8
        List<String> lines = Files.readAllLines(path, FileEncodingUtil.guessCharset(path.toString()));

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            Matcher m = PATTERN.matcher(line);
            if (!m.find()) continue;

            String command = m.group(1);
            // 去掉引号
            String value = m.group(2).replace("\"", "").trim();

            switch (command) {
                case "TITLE":
                    if (currentCueTrack == null) cue.albumTitle = value;
                    else currentCueTrack.title = value;
                    break;
                case "PERFORMER":
                    if (currentCueTrack == null) cue.albumPerformer = value;
                    else currentCueTrack.performer = value;
                    break;
                case "FILE":
                    // 格式通常是 "filename.flac" WAVE
                    String filename = value.split("(?i) WAVE|MP3|FLAC")[0];
                    cue.allFiles.add(filename);
                    if (currentCueTrack == null) cue.albumFileName = filename;
                    else currentCueTrack.fileName = filename;
                    break;
                case "TRACK":
                    currentCueTrack = new CueSheet.CueTrack();
                    currentCueTrack.number = Integer.parseInt(value.split(" ")[0]);
                    currentCueTrack.cueSheet = cue;
                    cue.tracks.add(currentCueTrack);
                    break;
                case "INDEX":
                    // Index 01 是音轨起始点
                    if (value.startsWith("01")) {
                        String timeStr = value.split("\\s+")[1];
                        currentCueTrack.rawStartTime = timeStr;
                        currentCueTrack.soundStartTimeMs = cueTimeToMs(timeStr);
                    }
                    // Index 02 是音轨结束点
                    if (value.startsWith("02")) {
                        String timeStr = value.split("\\s+")[1];
                        currentCueTrack.rawEndTime = timeStr;
                        currentCueTrack.soundEndTimeMs = cueTimeToMs(timeStr);
                    }
                    break;
            }
        }
        updateTrackInfo(cue);
        return cue;
    }

    /**
     * 由于音轨的信息经常是不完整的，需要额外补全处理
     */
    private static void updateTrackInfo(CueSheet cueSheet) {
        List<CueSheet.CueTrack> cueTracks = cueSheet.getTracks();
        for (int i = 0; i < cueTracks.size(); i++) {
            CueSheet.CueTrack t = cueTracks.get(i);
            // 计算起止时间
            long startTime = t.getSoundStartTimeMs();

            long duration = 0L;
            if (i > 0 && t.getSoundStartTimeMs() > 0 && t.getSoundEndTimeMs() > t.getSoundStartTimeMs()) {
                duration = t.getSoundEndTimeMs() - t.getSoundStartTimeMs();
            } else if (i == 0 && t.getSoundStartTimeMs() == 0 && t.getSoundEndTimeMs() > 0) {
                duration = t.getSoundEndTimeMs();
            } else if (i < cueTracks.size() - 1) {
                CueSheet.CueTrack next = cueTracks.get(i + 1);
                duration = next.getSoundStartTimeMs() - startTime;
            }
            t.setDuration(duration);

            // 继承全局信息
            String artist = t.getPerformer();
            if (artist == null || artist.isEmpty()) artist = cueSheet.getAlbumPerformer();
            t.setPerformer(artist == null ? "Unknown" : artist);

            // 格式化时长用于展示 (MM:SS)
            String durationStr = "??:??";
            if (duration != 0L) {
                int totalSec = Math.round(duration / 1000);
                durationStr = String.format("%02d:%02d", totalSec / 60, totalSec % 60);
            }
            t.setDurationStr(durationStr);

            // [核心优化] 丰富预览信息：[01] 歌名 - 歌手 [04:20]
            String displayInfo = String.format("[%02d] %s - %s [%s]", t.getNumber(), t.getTitle(), artist, durationStr);
            t.setDisplayInfo(displayInfo);
        }
    }

    /**
     * 将 CUE 时间格式 MM:SS:FF 转换为毫秒
     * 1秒 = 75帧 (Frames)
     */
    private static long cueTimeToMs(String time) {
        String[] parts = time.split(":");
        long min = Long.parseLong(parts[0]);
        long sec = Long.parseLong(parts[1]);
        long frames = 0;
        if (parts.length > 2) {
            frames = Long.parseLong(parts[2]);
        }
        return (min * 60 * 1000) + (sec * 1000) + (frames * 1000 / 75);
    }

    public static File locateAudioFile(File cueFile, String declaredName) {
        File dir = cueFile.getParentFile();
        if (declaredName != null && !declaredName.isEmpty()) {
            File f = new File(dir, declaredName);
            if (f.exists()) return f;
        }

        String baseName = cueFile.getName();
        int dot = baseName.lastIndexOf('.');
        if (dot > 0) baseName = baseName.substring(0, dot);

        String[] exts = {".flac", ".wav", ".ape", ".m4a", ".dsf", ".dff", ".tak", ".tta", ".wv"};
        for (String ext : exts) {
            File f = new File(dir, baseName + ext);
            if (f.exists()) return f;
            if (declaredName != null) {
                int d2 = declaredName.lastIndexOf('.');
                if (d2 > 0) {
                    File f2 = new File(dir, declaredName.substring(0, d2) + ext);
                    if (f2.exists()) return f2;
                }
            }
        }
        return null;
    }
}
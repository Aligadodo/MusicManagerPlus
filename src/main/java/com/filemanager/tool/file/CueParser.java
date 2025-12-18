package com.filemanager.tool.file;

import com.filemanager.model.CueSheet;
import lombok.SneakyThrows;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.nio.file.*;
import java.util.List;
import java.util.regex.*;

public class CueParser {
    // 匹配 CUE 指令的正则：指令 + 引号内的内容
    private static final Pattern PATTERN = Pattern.compile("([A-Z]+)\\s+(.*)");


    public static CueSheet parse(Path path) {
        try {
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
                String value = m.group(2).replace("\"", "").trim(); // 去掉引号

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
                        cue.fileName = value.split("(?i) WAVE|MP3|FLAC")[0];
                        break;
                    case "TRACK":
                        currentCueTrack = new CueSheet.CueTrack();
                        currentCueTrack.number = Integer.parseInt(value.split(" ")[0]);
                        cue.tracks.add(currentCueTrack);
                        break;
                    case "INDEX":
                        if (value.startsWith("01")) { // Index 01 是音轨起始点
                            String timeStr = value.split("\\s+")[1];
                            currentCueTrack.rawTime = timeStr;
                            currentCueTrack.startTimeMs = cueTimeToMs(timeStr);
                        }
                        if (value.startsWith("01")) { // Index 01 是音轨起始点
                            String timeStr = value.split("\\s+")[1];
                            currentCueTrack.rawTime = timeStr;
                            currentCueTrack.startTimeMs = cueTimeToMs(timeStr);
                        }
                        break;
                }
            }

            return cue;
        } catch (Throwable e) {
            System.out.println("Cue文件解析失败: " + path + ExceptionUtils.getStackTrace(e));
            return null;
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
}
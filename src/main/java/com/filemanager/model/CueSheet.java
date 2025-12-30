package com.filemanager.model;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * cue文件解析类
 * demo:
 * PERFORMER "徐小明 "
 * TITLE "完全歌集22首"
 * FILE "album.wav" WAVE
 * TRACK 01 AUDIO
 * TITLE "木棉袈裟"
 * INDEX 01 00:00:00
 * TRACK 02 AUDIO
 * TITLE "大侠霍元甲"
 * INDEX 00 02:35:41
 * INDEX 01 02:37:41
 * TRACK 03 AUDIO
 * TITLE "圈中人"
 * INDEX 00 05:16:04
 * INDEX 01 05:18:04
 *
 * @author 28667
 */
@Getter
@Setter
public class CueSheet {
    public String albumTitle;
    public String albumPerformer;
    public String albumFileName;
    public Set<String> allFiles = new LinkedHashSet<>();
    public List<CueTrack> tracks = new ArrayList<>();

    public int getCountFiles() {
        return allFiles.size();
    }

    @Getter
    @Setter
    public static class CueTrack {
        public int number;
        public String title; // 歌曲名，可能有乱码，需要注意
        public String fileName; // 每个歌曲都可能是独立的文件
        public String performer; // 每首歌都可能有独立的演奏者
        public long lastEndTimeMs; // 文件起始时间，转换后的毫秒数，默认为0
        public long soundStartTimeMs; // 文件起始时间，转换后的毫秒数，默认为0
        public long soundEndTimeMs; // 文件结束时间，转换后的毫秒数，默认为0
        public long duration; // 文件结束时间，转换后的毫秒数，默认为0
        public String rawLastEndTime; // 上个文件结束时间（原始值）
        public String rawStartTime; // 文件起始时间（原始值）
        public String rawEndTime; // 文件结束时间（原始值）
        public String displayInfo;
        public String durationStr;
        public CueSheet cueSheet;

        public String getFormatedFileName() {
            if (StringUtils.isNotBlank(fileName)) {
                return fileName;
            } else if (StringUtils.isNotBlank(cueSheet.albumFileName)) {
                return cueSheet.albumFileName;
            } else {
                return "Track - " + number;
            }
        }

        public String getFormatedTrackName(String format){
            String trackTitle = this.getTitle();
            String safeTitle = trackTitle.replaceAll("[\\\\/:*?\"<>|]", "_");
            return String.format("%02d - %s.%s", this.getNumber(), safeTitle, format);
        }
    }
}
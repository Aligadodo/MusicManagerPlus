/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
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
    
    // 添加getter方法
    public List<CueTrack> getTracks() { return tracks; }
    public String getAlbumFileName() { return albumFileName; }
    public String getAlbumTitle() { return albumTitle; }

    public int getCountFiles() {
        return allFiles.size();
    }
    
    public String getAlbumPerformer() {
        return albumPerformer;
    }

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
        
        // 手动添加的getter和setter方法
        public int getNumber() { return number; }
        public void setNumber(int number) { this.number = number; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public String getPerformer() { return performer; }
        public void setPerformer(String performer) { this.performer = performer; }
        
        public long getLastEndTimeMs() { return lastEndTimeMs; }
        public void setLastEndTimeMs(long lastEndTimeMs) { this.lastEndTimeMs = lastEndTimeMs; }
        
        public long getSoundStartTimeMs() { return soundStartTimeMs; }
        public void setSoundStartTimeMs(long soundStartTimeMs) { this.soundStartTimeMs = soundStartTimeMs; }
        
        public long getSoundEndTimeMs() { return soundEndTimeMs; }
        public void setSoundEndTimeMs(long soundEndTimeMs) { this.soundEndTimeMs = soundEndTimeMs; }
        
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        
        public String getRawLastEndTime() { return rawLastEndTime; }
        public void setRawLastEndTime(String rawLastEndTime) { this.rawLastEndTime = rawLastEndTime; }
        
        public String getRawStartTime() { return rawStartTime; }
        public void setRawStartTime(String rawStartTime) { this.rawStartTime = rawStartTime; }
        
        public String getRawEndTime() { return rawEndTime; }
        public void setRawEndTime(String rawEndTime) { this.rawEndTime = rawEndTime; }
        
        public String getDisplayInfo() { return displayInfo; }
        public void setDisplayInfo(String displayInfo) { this.displayInfo = displayInfo; }
        
        public String getDurationStr() { return durationStr; }
        public void setDurationStr(String durationStr) { this.durationStr = durationStr; }
        
        public CueSheet getCueSheet() { return cueSheet; }
        public void setCueSheet(CueSheet cueSheet) { this.cueSheet = cueSheet; }

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
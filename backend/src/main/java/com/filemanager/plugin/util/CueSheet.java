package com.filemanager.plugin.util;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class CueSheet {
    public String albumTitle;
    public String albumPerformer;
    public String albumFileName;
    public Set<String> allFiles = new LinkedHashSet<>();
    public List<CueTrack> tracks = new ArrayList<>();

    public List<CueTrack> getTracks() {
        return tracks;
    }

    public String getAlbumFileName() {
        return albumFileName;
    }

    public String getAlbumTitle() {
        return albumTitle;
    }

    public int getCountFiles() {
        return allFiles.size();
    }

    public String getAlbumPerformer() {
        return albumPerformer;
    }

    public Set<String> getAllFiles() {
        return allFiles;
    }

    public static class CueTrack {
        public int number;
        public String title;
        public String fileName;
        public String performer;
        public long lastEndTimeMs;
        public long soundStartTimeMs;
        public long soundEndTimeMs;
        public long duration;
        public String rawLastEndTime;
        public String rawStartTime;
        public String rawEndTime;
        public String displayInfo;
        public String durationStr;
        public CueSheet cueSheet;

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getPerformer() {
            return performer;
        }

        public void setPerformer(String performer) {
            this.performer = performer;
        }

        public long getLastEndTimeMs() {
            return lastEndTimeMs;
        }

        public void setLastEndTimeMs(long lastEndTimeMs) {
            this.lastEndTimeMs = lastEndTimeMs;
        }

        public long getSoundStartTimeMs() {
            return soundStartTimeMs;
        }

        public void setSoundStartTimeMs(long soundStartTimeMs) {
            this.soundStartTimeMs = soundStartTimeMs;
        }

        public long getSoundEndTimeMs() {
            return soundEndTimeMs;
        }

        public void setSoundEndTimeMs(long soundEndTimeMs) {
            this.soundEndTimeMs = soundEndTimeMs;
        }

        public long getDuration() {
            return duration;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }

        public String getRawLastEndTime() {
            return rawLastEndTime;
        }

        public void setRawLastEndTime(String rawLastEndTime) {
            this.rawLastEndTime = rawLastEndTime;
        }

        public String getRawStartTime() {
            return rawStartTime;
        }

        public void setRawStartTime(String rawStartTime) {
            this.rawStartTime = rawStartTime;
        }

        public String getRawEndTime() {
            return rawEndTime;
        }

        public void setRawEndTime(String rawEndTime) {
            this.rawEndTime = rawEndTime;
        }

        public String getDisplayInfo() {
            return displayInfo;
        }

        public void setDisplayInfo(String displayInfo) {
            this.displayInfo = displayInfo;
        }

        public String getDurationStr() {
            return durationStr;
        }

        public void setDurationStr(String durationStr) {
            this.durationStr = durationStr;
        }

        public CueSheet getCueSheet() {
            return cueSheet;
        }

        public void setCueSheet(CueSheet cueSheet) {
            this.cueSheet = cueSheet;
        }

        public String getFormatedFileName() {
            if (StringUtils.isNotBlank(fileName)) {
                return fileName;
            } else if (StringUtils.isNotBlank(cueSheet.albumFileName)) {
                return cueSheet.albumFileName;
            } else {
                return "Track - " + number;
            }
        }

        public String getFormatedTrackName(String format) {
            String trackTitle = this.getTitle();
            String safeTitle = trackTitle.replaceAll("[\\\\/:*?\"<>|]", "_");
            return String.format("%02d - %s.%s", this.getNumber(), safeTitle, format);
        }
    }
}
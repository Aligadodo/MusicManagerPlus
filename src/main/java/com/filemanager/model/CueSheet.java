package com.filemanager.model;

import java.util.*;

/**
 * cue文件解析类
 * demo:
 * PERFORMER "徐小明 "
 * TITLE "完全歌集22首"
 * FILE "album.wav" WAVE
 *   TRACK 01 AUDIO
 *     TITLE "木棉袈裟"
 *     INDEX 01 00:00:00
 *   TRACK 02 AUDIO
 *     TITLE "大侠霍元甲"
 *     INDEX 00 02:35:41
 *     INDEX 01 02:37:41
 *   TRACK 03 AUDIO
 *     TITLE "圈中人"
 *     INDEX 00 05:16:04
 *     INDEX 01 05:18:04
 */
public class CueSheet {
    public String albumTitle;
    public String albumPerformer;
    public String fileName;
    public List<CueTrack> tracks = new ArrayList<>();

    public static class CueTrack {
        public int number;
        public String title;
        public String performer;
        public long startTimeMs; // 转换后的毫秒数
        public String rawTime;
    }
}
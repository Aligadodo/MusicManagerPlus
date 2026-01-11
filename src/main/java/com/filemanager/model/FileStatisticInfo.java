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

import com.alibaba.fastjson.JSONObject;
import com.filemanager.util.FileUtil;
import com.filemanager.util.LanguageUtil;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.jaudiotagger.audio.aiff.AiffFileReader;
import org.jaudiotagger.audio.asf.AsfFileReader;
import org.jaudiotagger.audio.dff.DffFileReader;
import org.jaudiotagger.audio.dsf.DsfFileReader;
import org.jaudiotagger.audio.flac.FlacFileReader;
import org.jaudiotagger.audio.generic.AudioFileReader;
import org.jaudiotagger.audio.mp3.MP3FileReader;
import org.jaudiotagger.audio.real.RealFileReader;
import org.jaudiotagger.audio.wav.WavFileReader;

import java.io.File;

@Data
@Getter
@Setter
public class FileStatisticInfo {
    private static final String full_music_types = "mp3,flac,wav,aiff,iso,ape,asf,dfd,dsf,dts,dff";
    public File file;
    /***
     * basic info
     */
    public String type;
    public String oriName;
    public String classicName;
    public int fileNameLength;
    public double fileSizeMb;
    /***
     * name related info
     */
    public int countCNChars;
    public int countENChars;
    public int countJPChars;
    public int countKoreanChars;
    public int countNUMChars;

    public static FileStatisticInfo create(File file) {
        FileStatisticInfo statisticInfo = new FileStatisticInfo();
        statisticInfo.file = file;
        statisticInfo.type = FileUtil.getFileType(file);
        statisticInfo.fileSizeMb = FileUtil.getFileSizeMB(file);
        String filename = file.getName();
        if (filename.indexOf('.') > 0) {
            filename = filename.substring(0, filename.lastIndexOf('.'));
        }
        statisticInfo.oriName = filename;
        statisticInfo.fileNameLength = filename.length();
        for (char c : filename.toCharArray()) {
            if (LanguageUtil.isChineseChar(c)) {
                statisticInfo.countCNChars = statisticInfo.countCNChars + 1;
            } else if (LanguageUtil.isEnglishChar(c)) {
                statisticInfo.countENChars = statisticInfo.countENChars + 1;
            } else if (LanguageUtil.isJapaneseChar(c)) {
                statisticInfo.countJPChars = statisticInfo.countJPChars + 1;
            } else if (LanguageUtil.isKoreaChar(c)) {
                statisticInfo.countKoreanChars = statisticInfo.countKoreanChars + 1;
            } else if (LanguageUtil.isNumChar(c)) {
                statisticInfo.countNUMChars = statisticInfo.countNUMChars + 1;
            }
        }
        statisticInfo.classicName = LanguageUtil.toClassicName(filename, statisticInfo.countCNChars > 0);
        return statisticInfo;
    }

    public void print() {
        System.out.println(this.classicName + ":" + JSONObject.toJSONString(this));
    }

    public boolean isMp3() {
        return "mp3".equals(this.type);
    }

    public boolean isMusic() {
        return full_music_types.contains(this.type);
    }

    //mp3,flac,wav,aiff,ape,asf,dfd,dsf,iso,dts,dff
    public AudioFileReader getAudioFileReader() {
        switch (this.type) {
            case "mp3":
                return new MP3FileReader();
            case "flac":
                return new FlacFileReader();
            case "wav":
                return new WavFileReader();
            case "aiff":
                return new AiffFileReader();
            case "ape":
                return new WavFileReader();
            case "asf":
                return new AsfFileReader();
            case "dfd":
                return new DffFileReader();
            case "dsf":
                return new DsfFileReader();
            case "dts":
            case "dff":
                return new DffFileReader();
            default:
                return new RealFileReader();

        }
    }
}

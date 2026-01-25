/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-25 
 */
package com.filemanager.strategy.ncm.model;

/**
 * 缓存文件信息类
 * 用于存储和管理缓存文件的相关信息
 */
public class CacheFileInfo {
    private String songName;
    private String artistName;
    private String audioFormat;
    
    public CacheFileInfo(String songName, String artistName, String audioFormat) {
        this.songName = songName;
        this.artistName = artistName;
        this.audioFormat = audioFormat;
    }
    
    public String getSongName() {
        return songName;
    }
    
    public String getArtistName() {
        return artistName;
    }
    
    public String getAudioFormat() {
        return audioFormat;
    }
    
    public String getDisplayName() {
        if (songName != null && !songName.isEmpty() && artistName != null && !artistName.isEmpty()) {
            return artistName + " - " + songName + "." + audioFormat;
        } else if (songName != null && !songName.isEmpty()) {
            return songName + "." + audioFormat;
        } else {
            return "Unknown Song." + audioFormat;
        }
    }
}

package com.filemanager.model;

import lombok.Data;

import java.io.File;

@Data
public class MusicInfo{
    public File file;
    public String artist;// 歌手名
    public String album;// 專輯名
    public String songName;// 歌名
}
package com.filemanager.strategy.scraper.model;

import java.util.List;

/**
 * 专辑信息模型
 */
public class AlbumInfo {
    private String name;           // 专辑名称
    private String artist;          // 艺术家
    private String year;            // 发行年份
    private String genre;           // 流派
    private String description;     // 专辑简介
    private String copyright;       // 版权信息
    private List<TrackInfo> tracks; // 曲目列表
    private String source;          // 数据来源
    
    public AlbumInfo() {
        this.tracks = new java.util.ArrayList<>();
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getArtist() {
        return artist;
    }
    
    public void setArtist(String artist) {
        this.artist = artist;
    }
    
    public String getYear() {
        return year;
    }
    
    public void setYear(String year) {
        this.year = year;
    }
    
    public String getGenre() {
        return genre;
    }
    
    public void setGenre(String genre) {
        this.genre = genre;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCopyright() {
        return copyright;
    }
    
    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }
    
    public List<TrackInfo> getTracks() {
        return tracks;
    }
    
    public void setTracks(List<TrackInfo> tracks) {
        this.tracks = tracks;
    }
    
    public void addTrack(TrackInfo track) {
        this.tracks.add(track);
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
}
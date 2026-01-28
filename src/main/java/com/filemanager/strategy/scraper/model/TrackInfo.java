package com.filemanager.strategy.scraper.model;

/**
 * 曲目信息模型
 */
public class TrackInfo {
    private String title;      // 曲目标题
    private String artist;     // 艺术家
    private String album;      // 专辑名称
    private String year;       // 发行年份
    private String genre;      // 流派
    private int trackNumber;   // 曲目序号
    private int duration;     // 时长（秒）
    private String source;     // 数据来源
    
    public TrackInfo() {}
    
    public TrackInfo(String title, String artist) {
        this.title = title;
        this.artist = artist;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getArtist() {
        return artist;
    }
    
    public void setArtist(String artist) {
        this.artist = artist;
    }
    
    public String getAlbum() {
        return album;
    }
    
    public void setAlbum(String album) {
        this.album = album;
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
    
    public int getTrackNumber() {
        return trackNumber;
    }
    
    public void setTrackNumber(int trackNumber) {
        this.trackNumber = trackNumber;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public void setDuration(int duration) {
        this.duration = duration;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
}
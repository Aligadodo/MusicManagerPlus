package com.filemanager.strategy.scraper.model;

/**
 * 歌词信息模型
 */
public class LyricsInfo {
    private String content;      // 歌词内容
    private String format;        // 歌词格式（LRC, TXT等）
    private String source;        // 数据来源
    private boolean isVerified;   // 是否已验证
    
    public LyricsInfo() {}
    
    public LyricsInfo(String content, String format, String source) {
        this.content = content;
        this.format = format;
        this.source = source;
        this.isVerified = false;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getFormat() {
        return format;
    }
    
    public void setFormat(String format) {
        this.format = format;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public boolean isVerified() {
        return isVerified;
    }
    
    public void setVerified(boolean verified) {
        isVerified = verified;
    }
}
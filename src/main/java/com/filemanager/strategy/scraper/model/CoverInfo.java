package com.filemanager.strategy.scraper.model;

/**
 * 封面信息模型
 */
public class CoverInfo {
    private String imageUrl;      // 封面图片URL
    private String format;        // 图片格式（JPG, PNG等）
    private int width;           // 图片宽度
    private int height;          // 图片高度
    private String source;        // 数据来源
    private long size;           // 文件大小（字节）
    
    public CoverInfo() {}
    
    public CoverInfo(String imageUrl, String format, String source) {
        this.imageUrl = imageUrl;
        this.format = format;
        this.source = source;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getFormat() {
        return format;
    }
    
    public void setFormat(String format) {
        this.format = format;
    }
    
    public int getWidth() {
        return width;
    }
    
    public void setWidth(int width) {
        this.width = width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public void setHeight(int height) {
        this.height = height;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public long getSize() {
        return size;
    }
    
    public void setSize(long size) {
        this.size = size;
    }
}
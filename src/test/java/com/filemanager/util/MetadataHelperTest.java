package com.filemanager.util;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class MetadataHelperTest {

    @Test
    public void testExtractFromFileSystemWithCategoryDirectory() {
        // Test case for directory structure like "W:\C - 蔡琴\专辑名\01 - 歌曲名.mp3"
        File file = new File("W:/C - 蔡琴/专辑名/01 - 歌曲名.mp3");
        MetadataHelper.AudioMeta meta = MetadataHelper.extractFromFileSystem(file);
        
        // Should extract "蔡琴" as artist, "专辑名" as album, "歌曲名" as title, "01" as track
        assertEquals("蔡琴", meta.getArtist());
        assertEquals("专辑名", meta.getAlbum());
        assertEquals("歌曲名", meta.getTitle());
        assertEquals("01", meta.getTrack());
    }

    @Test
    public void testExtractFromFileSystemWithDifferentCategoryDirectories() {
        // Test case for directory structure like "W:\A - 刘德华\忘情水\02 - 忘情水.mp3"
        File file = new File("W:/A - 刘德华/忘情水/02 - 忘情水.mp3");
        MetadataHelper.AudioMeta meta = MetadataHelper.extractFromFileSystem(file);
        
        // Should extract "刘德华" as artist, "忘情水" as album, "忘情水" as title, "02" as track
        assertEquals("刘德华", meta.getArtist());
        assertEquals("忘情水", meta.getAlbum());
        assertEquals("忘情水", meta.getTitle());
        assertEquals("02", meta.getTrack());
    }

    @Test
    public void testExtractFromFileSystemWithNormalDirectory() {
        // Test case for normal directory structure like "W:\普通目录\歌曲.mp3"
        File file = new File("W:/普通目录/歌曲.mp3");
        MetadataHelper.AudioMeta meta = MetadataHelper.extractFromFileSystem(file);
        
        // Should extract "普通目录" as album, "歌曲" as title
        assertEquals("", meta.getArtist()); // No artist information available
        assertEquals("普通目录", meta.getAlbum());
        assertEquals("歌曲", meta.getTitle());
        assertEquals("", meta.getTrack()); // No track information available
    }
}

package com.filemanager.util;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class MetadataHelperTest2 {

    @Test
    public void testExtractFromFileSystemWithComplexPath() {
        // Test case for directory structure like "W:\C - 陈婧霏\陈婧霏.2020 - 陈婧霏【有此山文化】【WAV+CUE】\01 - 歌曲名.wav"
        File file = new File("W:/C - 陈婧霏/陈婧霏.2020 - 陈婧霏【有此山文化】【WAV+CUE】/01 - 歌曲名.wav");
        MetadataHelper.AudioMeta meta = MetadataHelper.extractFromFileSystem(file);
        
        System.out.println("Test path: " + file.getPath());
        System.out.println("Extracted Artist: " + meta.getArtist());
        System.out.println("Extracted Album: " + meta.getAlbum());
        System.out.println("Extracted Title: " + meta.getTitle());
        System.out.println("Extracted Track: " + meta.getTrack());
        
        // Should extract "陈婧霏" as artist
        assertEquals("陈婧霏", meta.getArtist());
    }

    @Test
    public void testExtractFromFileSystemWithComplexPath2() {
        // Test case for directory structure like "W:\C - 陈婧霏\陈婧霏.2020 - 陈婧霏【有此山文化】【WAV+CUE】\歌曲名.wav"
        File file = new File("W:/C - 陈婧霏/陈婧霏.2020 - 陈婧霏【有此山文化】【WAV+CUE】/歌曲名.wav");
        MetadataHelper.AudioMeta meta = MetadataHelper.extractFromFileSystem(file);
        
        System.out.println("Test path: " + file.getPath());
        System.out.println("Extracted Artist: " + meta.getArtist());
        System.out.println("Extracted Album: " + meta.getAlbum());
        System.out.println("Extracted Title: " + meta.getTitle());
        System.out.println("Extracted Track: " + meta.getTrack());
        
        // Should extract "陈婧霏" as artist
        assertEquals("陈婧霏", meta.getArtist());
    }

    @Test
    public void testExtractFromFileSystemWithComplexPath3() {
        // Test case for directory structure like "W:\C - 陈婧霏\陈婧霏.2020 - 陈婧霏【有此山文化】【WAV+CUE】\陈婧霏 - 歌曲名.wav"
        File file = new File("W:/C - 陈婧霏/陈婧霏.2020 - 陈婧霏【有此山文化】【WAV+CUE】/陈婧霏 - 歌曲名.wav");
        MetadataHelper.AudioMeta meta = MetadataHelper.extractFromFileSystem(file);
        
        System.out.println("Test path: " + file.getPath());
        System.out.println("Extracted Artist: " + meta.getArtist());
        System.out.println("Extracted Album: " + meta.getAlbum());
        System.out.println("Extracted Title: " + meta.getTitle());
        System.out.println("Extracted Track: " + meta.getTrack());
        
        // Should extract "陈婧霏" as artist
        assertEquals("陈婧霏", meta.getArtist());
    }
}

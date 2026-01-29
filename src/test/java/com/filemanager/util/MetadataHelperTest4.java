package com.filemanager.util;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class MetadataHelperTest4 {

    @Test
    public void testExtractFromFileSystemWithComplexPath() {
        // Test case for the exact path mentioned in the issue
        File file = new File("W:/C - 陈婧霏/陈婧霏.2024 - 春色（EP）【有此山文化】【FLAC分轨】/02. 午夜爱未眠.flac");
        MetadataHelper.AudioMeta meta = MetadataHelper.extractFromFileSystem(file);
        
        System.out.println("Test path: " + file.getPath());
        System.out.println("Extracted Artist: " + meta.getArtist());
        System.out.println("Extracted Album: " + meta.getAlbum());
        System.out.println("Extracted Title: " + meta.getTitle());
        System.out.println("Extracted Track: " + meta.getTrack());
        
        // Should extract "陈婧霏" as artist, not "春色（EP）"
        assertEquals("陈婧霏", meta.getArtist());
        assertEquals("午夜爱未眠", meta.getTitle());
        assertEquals("春色（EP）【有此山文化】【FLAC分轨】", meta.getAlbum());
        assertEquals("02", meta.getTrack());
    }
}
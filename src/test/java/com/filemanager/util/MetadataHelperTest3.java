package com.filemanager.util;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class MetadataHelperTest3 {

    @Test
    public void testVariousComplexPaths() {
        // Test 1: Complex path with brackets and year
        testPath("W:/C - 陈婧霏/陈婧霏.2020 - 陈婧霏【有此山文化】【WAV+CUE】/01 - 歌曲名.wav", 
                 "陈婧霏", "陈婧霏【有此山文化】【WAV+CUE】", "歌曲名", "01");

        // Test 2: Path with square brackets
        testPath("W:/A - 刘德华/刘德华.1995 - 忘情水[华纳唱片][CD]/02 - 忘情水.mp3", 
                 "刘德华", "忘情水[华纳唱片][CD]", "忘情水", "02");

        // Test 3: Path with parentheses
        testPath("W:/B - 张学友/张学友.1993 - 吻别(宝丽金)(HQ)/03 - 吻别.flac", 
                 "张学友", "吻别(宝丽金)(HQ)", "吻别", "03");

        // Test 4: Simple path
        testPath("W:/D - 周杰伦/七里香/04 - 七里香.mp3", 
                 "周杰伦", "七里香", "七里香", "04");

        // Test 5: Path without track number
        testPath("W:/E - 邓紫棋/光年之外/光年之外.flac", 
                 "邓紫棋", "光年之外", "光年之外", "");

        // Test 6: Path with artist in filename
        testPath("W:/F - 林俊杰/江南/林俊杰 - 江南.mp3", 
                 "林俊杰", "江南", "江南", "");

        // Test 7: Very complex path with multiple brackets
        testPath("W:/G - 五月天/五月天.2011 - 第二人生【相信】【相信音乐】【HQCD】/01 - 第二人生.mp3", 
                 "五月天", "第二人生【相信】【相信音乐】【HQCD】", "第二人生", "01");

        // Test 8: Path with mixed brackets
        testPath("W:/H - 薛之谦/薛之谦.2016 - 初学者[华宇音乐][HQ]/薛之谦 - 初学者.flac", 
                 "薛之谦", "初学者[华宇音乐][HQ]", "初学者", "");

        // Test 9: Path with year in album name
        testPath("W:/I - 蔡琴/蔡琴.1996 - 民歌蔡琴【点将唱片】/01 - 恰似你的温柔.wav", 
                 "蔡琴", "民歌蔡琴【点将唱片】", "恰似你的温柔", "01");

        // Test 10: Path with multiple separators
        testPath("W:/J - 孙燕姿/孙燕姿.2000 - 孙燕姿[华纳][台湾版]/01 - 天黑黑.mp3", 
                 "孙燕姿", "孙燕姿[华纳][台湾版]", "天黑黑", "01");
    }

    private void testPath(String path, String expectedArtist, String expectedAlbum, String expectedTitle, String expectedTrack) {
        System.out.println("\nTest path: " + path);
        File file = new File(path);
        MetadataHelper.AudioMeta meta = MetadataHelper.extractFromFileSystem(file);
        
        System.out.println("Extracted Artist: " + meta.getArtist());
        System.out.println("Extracted Album: " + meta.getAlbum());
        System.out.println("Extracted Title: " + meta.getTitle());
        System.out.println("Extracted Track: " + meta.getTrack());
        
        assertEquals("Artist mismatch for path: " + path, expectedArtist, meta.getArtist());
        assertEquals("Album mismatch for path: " + path, expectedAlbum, meta.getAlbum());
        assertEquals("Title mismatch for path: " + path, expectedTitle, meta.getTitle());
        assertEquals("Track mismatch for path: " + path, expectedTrack, meta.getTrack());
    }
}

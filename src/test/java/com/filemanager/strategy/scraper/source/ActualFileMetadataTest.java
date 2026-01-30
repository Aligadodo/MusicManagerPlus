package com.filemanager.strategy.scraper.source;

import com.filemanager.strategy.scraper.model.*;
import com.filemanager.strategy.scraper.source.impl.*;
import com.filemanager.util.MetadataHelper;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.*;

@Ignore("忽略外部API测试，避免执行过慢")

/**
 * 使用实际文件测试元数据刮削功能
 * 测试路径: W:\C - 陈婧霏\陈婧霏.2020 - 陈婧霏【有此山文化】【WAV+CUE】\Split - WAV
 */
public class ActualFileMetadataTest {

    private static final String TEST_FOLDER = "W:\\C - 陈婧霏\\陈婧霏.2020 - 陈婧霏【有此山文化】【WAV+CUE】\\Split - WAV";

    @Test
    public void testActualFileMetadata() {
        System.out.println("开始使用实际文件测试元数据刮削功能");
        System.out.println("测试路径: " + TEST_FOLDER);
        System.out.println("=====================================");

        // 获取测试文件
        List<File> testFiles = getTestFiles();
        if (testFiles.isEmpty()) {
            System.out.println("未找到测试文件");
            return;
        }

        System.out.println("找到 " + testFiles.size() + " 个音频文件:");
        for (File file : testFiles) {
            System.out.println("- " + file.getName());
        }
        System.out.println("=====================================");

        // 初始化数据源
        List<MetadataSource> sources = initializeSources();

        // 对每个文件测试所有数据源
        for (File file : testFiles) {
            testFileMetadata(file, sources);
        }

        System.out.println("=====================================");
        System.out.println("测试完成");
    }

    /**
     * 获取测试文件
     */
    private static List<File> getTestFiles() {
        List<File> files = new ArrayList<>();
        File folder = new File(TEST_FOLDER);
        
        if (folder.exists() && folder.isDirectory()) {
            File[] fileArray = folder.listFiles();
            if (fileArray != null) {
                for (File file : fileArray) {
                    if (file.isFile() && file.getName().toLowerCase().endsWith(".wav")) {
                        files.add(file);
                    }
                }
            }
        }
        
        return files;
    }

    /**
     * 初始化数据源
     */
    private static List<MetadataSource> initializeSources() {
        List<MetadataSource> sources = new ArrayList<>();
        
        sources.add(new ITunesSource());
        sources.add(new MusicBrainzSource());
        sources.add(new NeteaseMusicSource());
        sources.add(new MiguMusicSource());
        sources.add(new LastFmSource());
        sources.add(new DiscogsSource());
        
        return sources;
    }

    /**
     * 测试单个文件的元数据刮削
     */
    private static void testFileMetadata(File file, List<MetadataSource> sources) {
        System.out.println("\n测试文件: " + file.getName());
        System.out.println("=====================================");

        // 从文件名提取元数据
        MetadataHelper.AudioMeta guess = MetadataHelper.extractFromFileSystem(file);
        System.out.println("从文件名提取的元数据:");
        System.out.println("- 艺术家: " + guess.getArtist());
        System.out.println("- 标题: " + guess.getTitle());
        System.out.println("- 专辑: " + guess.getAlbum());

        // 对每个数据源进行测试
        for (MetadataSource source : sources) {
            testSource(source, guess.getArtist(), guess.getTitle(), guess.getAlbum());
        }
    }

    /**
     * 测试单个数据源
     */
    private static void testSource(MetadataSource source, String artist, String title, String album) {
        System.out.println("\n测试数据源: " + source.getSourceName());
        System.out.println("-------------------------------------");

        // 测试搜索曲目信息
        System.out.println("1. 测试搜索曲目信息:");
        try {
            TrackInfo trackInfo = source.searchTrackInfo(artist, title);
            if (trackInfo != null) {
                System.out.println("   ✅ 成功获取曲目信息:");
                System.out.println("      - 标题: " + trackInfo.getTitle());
                System.out.println("      - 艺术家: " + trackInfo.getArtist());
                System.out.println("      - 专辑: " + trackInfo.getAlbum());
                System.out.println("      - 时长: " + trackInfo.getDuration() + "秒");
                System.out.println("      - 年份: " + trackInfo.getYear());
                System.out.println("      - 流派: " + trackInfo.getGenre());
            } else {
                System.out.println("   ❌ 未找到曲目信息");
            }
        } catch (Exception e) {
            System.out.println("   ❌ 搜索曲目信息失败: " + e.getMessage());
        }
        System.out.println("   请求URL: " + source.getLastRequestUrl());
        System.out.println("   错误信息: " + source.getLastRequestError());

        // 测试搜索专辑信息
        System.out.println("2. 测试搜索专辑信息:");
        try {
            AlbumInfo albumInfo = source.searchAlbumInfo(artist, album);
            if (albumInfo != null) {
                System.out.println("   ✅ 成功获取专辑信息:");
                System.out.println("      - 专辑名称: " + albumInfo.getName());
                System.out.println("      - 艺术家: " + albumInfo.getArtist());
                System.out.println("      - 年份: " + albumInfo.getYear());
                System.out.println("      - 流派: " + albumInfo.getGenre());
                System.out.println("      - 曲目数: " + albumInfo.getTracks().size());
            } else {
                System.out.println("   ❌ 未找到专辑信息");
            }
        } catch (Exception e) {
            System.out.println("   ❌ 搜索专辑信息失败: " + e.getMessage());
        }
        System.out.println("   请求URL: " + source.getLastRequestUrl());
        System.out.println("   错误信息: " + source.getLastRequestError());

        // 测试搜索封面
        System.out.println("3. 测试搜索封面:");
        try {
            CoverInfo coverInfo = source.searchCover(artist, album);
            if (coverInfo != null) {
                System.out.println("   ✅ 成功获取封面信息:");
                System.out.println("      - 封面URL: " + coverInfo.getImageUrl());
                System.out.println("      - 格式: " + coverInfo.getFormat());
                System.out.println("      - 尺寸: " + coverInfo.getWidth() + "x" + coverInfo.getHeight());
            } else {
                System.out.println("   ❌ 未找到封面信息");
            }
        } catch (Exception e) {
            System.out.println("   ❌ 搜索封面失败: " + e.getMessage());
        }
        System.out.println("   请求URL: " + source.getLastRequestUrl());
        System.out.println("   错误信息: " + source.getLastRequestError());

        // 测试搜索歌词
        System.out.println("4. 测试搜索歌词:");
        try {
            LyricsInfo lyricsInfo = source.searchLyrics(artist, title, 240);
            if (lyricsInfo != null && lyricsInfo.getContent() != null && !lyricsInfo.getContent().isEmpty()) {
                System.out.println("   ✅ 成功获取歌词:");
                System.out.println("      - 歌词长度: " + lyricsInfo.getContent().length() + "字符");
                System.out.println("      - 歌词预览: " + (lyricsInfo.getContent().length() > 100 ? lyricsInfo.getContent().substring(0, 100) + "..." : lyricsInfo.getContent()));
            } else {
                System.out.println("   ❌ 未找到歌词");
            }
        } catch (Exception e) {
            System.out.println("   ❌ 搜索歌词失败: " + e.getMessage());
        }
        System.out.println("   请求URL: " + source.getLastRequestUrl());
        System.out.println("   错误信息: " + source.getLastRequestError());
    }
}

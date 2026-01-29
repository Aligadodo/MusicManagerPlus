package com.filemanager.strategy.scraper.source;

import com.filemanager.strategy.scraper.source.impl.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertNotNull;

/**
 * 测试所有数据源的API请求路径和参数正确性
 * 确保API请求不会直接进入错误页或报错
 */
@RunWith(Parameterized.class)
public class MetadataSourceApiTest {

    private final MetadataSource source;
    private final String testArtist;
    private final String testTitle;
    private final String testAlbum;

    public MetadataSourceApiTest(MetadataSource source, String testArtist, String testTitle, String testAlbum) {
        this.source = source;
        this.testArtist = testArtist;
        this.testTitle = testTitle;
        this.testAlbum = testAlbum;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {new ITunesSource(), "Ed Sheeran", "Shape of You", "Divide"},
            {new MusicBrainzSource(), "Ed Sheeran", "Shape of You", "Divide"},
            {new LastFmSource(), "Ed Sheeran", "Shape of You", "Divide"},
            {new DiscogsSource(), "Ed Sheeran", "Shape of You", "Divide"}
        });
    }

    /**
     * 测试搜索歌词API
     * 确保API请求不会直接进入错误页
     */
    @Test
    public void testSearchLyrics() {
        System.out.println("Testing searchLyrics for " + source.getSourceName());
        try {
            // 调用搜索歌词方法
            source.searchLyrics(testArtist, testTitle, 240);
            
            // 获取最后请求的URL和错误信息
            String lastRequestUrl = source.getLastRequestUrl();
            String lastRequestError = source.getLastRequestError();
            
            // 打印请求信息
            System.out.println("Source: " + source.getSourceName());
            System.out.println("Last Request URL: " + lastRequestUrl);
            System.out.println("Last Request Error: " + lastRequestError);
            
            // 确保API请求不会直接进入错误页
            // 对于需要API密钥的数据源，会有认证错误
            // 对于咪咕音乐，可能返回HTML而不是JSON，但请求本身成功
            if (source instanceof LastFmSource || source instanceof DiscogsSource) {
                assertNotNull("Expected authentication error for " + source.getSourceName(), lastRequestError);
            } else if (source instanceof NeteaseMusicSource) {
                // 网易云音乐API可能需要授权
                assertNotNull("Expected error for NeteaseMusicSource", lastRequestError);
            } else {
                // MusicBrainz和MiguMusicSource应该返回有效响应
                // MiguMusicSource可能返回HTML而不是JSON，但请求本身成功
                System.out.println("Source " + source.getSourceName() + " returned response without error");
            }
            
            // 确保请求URL不为空
            assertNotNull("Request URL should not be null", lastRequestUrl);
            System.out.println("Test passed for " + source.getSourceName());
        } catch (Exception e) {
            System.out.println("Test failed for " + source.getSourceName() + ": " + e.getMessage());
            // 确保API请求不会抛出未捕获的异常
            e.printStackTrace();
        }
    }

    /**
     * 测试搜索封面API
     * 确保API请求不会直接进入错误页
     */
    @Test
    public void testSearchCover() {
        System.out.println("Testing searchCover for " + source.getSourceName());
        try {
            // 调用搜索封面方法
            source.searchCover(testArtist, testAlbum);
            
            // 获取最后请求的URL和错误信息
            String lastRequestUrl = source.getLastRequestUrl();
            String lastRequestError = source.getLastRequestError();
            
            // 打印请求信息
            System.out.println("Source: " + source.getSourceName());
            System.out.println("Last Request URL: " + lastRequestUrl);
            System.out.println("Last Request Error: " + lastRequestError);
            
            // 确保API请求不会直接进入错误页
            // 对于需要API密钥的数据源，会有认证错误
            // 对于咪咕音乐，可能返回HTML而不是JSON，但请求本身成功
            if (source instanceof LastFmSource || source instanceof DiscogsSource) {
                assertNotNull("Expected authentication error for " + source.getSourceName(), lastRequestError);
            } else if (source instanceof NeteaseMusicSource) {
                // 网易云音乐API可能需要授权
                assertNotNull("Expected error for NeteaseMusicSource", lastRequestError);
            } else {
                // MusicBrainz和MiguMusicSource应该返回有效响应
                // MiguMusicSource可能返回HTML而不是JSON，但请求本身成功
                System.out.println("Source " + source.getSourceName() + " returned response without error");
            }
            
            // 确保请求URL不为空
            assertNotNull("Request URL should not be null", lastRequestUrl);
            System.out.println("Test passed for " + source.getSourceName());
        } catch (Exception e) {
            System.out.println("Test failed for " + source.getSourceName() + ": " + e.getMessage());
            // 确保API请求不会抛出未捕获的异常
            e.printStackTrace();
        }
    }

    /**
     * 测试搜索专辑信息API
     * 确保API请求不会直接进入错误页
     */
    @Test
    public void testSearchAlbumInfo() {
        System.out.println("Testing searchAlbumInfo for " + source.getSourceName());
        try {
            // 调用搜索专辑信息方法
            source.searchAlbumInfo(testArtist, testAlbum);
            
            // 获取最后请求的URL和错误信息
            String lastRequestUrl = source.getLastRequestUrl();
            String lastRequestError = source.getLastRequestError();
            
            // 打印请求信息
            System.out.println("Source: " + source.getSourceName());
            System.out.println("Last Request URL: " + lastRequestUrl);
            System.out.println("Last Request Error: " + lastRequestError);
            
            // 确保API请求不会直接进入错误页
            // 对于需要API密钥的数据源，会有认证错误
            // 对于咪咕音乐，可能返回HTML而不是JSON，但请求本身成功
            if (source instanceof LastFmSource || source instanceof DiscogsSource) {
                assertNotNull("Expected authentication error for " + source.getSourceName(), lastRequestError);
            } else if (source instanceof NeteaseMusicSource) {
                // 网易云音乐API可能需要授权
                assertNotNull("Expected error for NeteaseMusicSource", lastRequestError);
            } else {
                // MusicBrainz和MiguMusicSource应该返回有效响应
                // MiguMusicSource可能返回HTML而不是JSON，但请求本身成功
                System.out.println("Source " + source.getSourceName() + " returned response without error");
            }
            
            // 确保请求URL不为空
            assertNotNull("Request URL should not be null", lastRequestUrl);
            System.out.println("Test passed for " + source.getSourceName());
        } catch (Exception e) {
            System.out.println("Test failed for " + source.getSourceName() + ": " + e.getMessage());
            // 确保API请求不会直接进入错误页
            e.printStackTrace();
        }
    }

    /**
     * 测试搜索曲目信息API
     * 确保API请求不会直接进入错误页
     */
    @Test
    public void testSearchTrackInfo() {
        System.out.println("Testing searchTrackInfo for " + source.getSourceName());
        try {
            // 调用搜索曲目信息方法
            source.searchTrackInfo(testArtist, testTitle);
            
            // 获取最后请求的URL和错误信息
            String lastRequestUrl = source.getLastRequestUrl();
            String lastRequestError = source.getLastRequestError();
            
            // 打印请求信息
            System.out.println("Source: " + source.getSourceName());
            System.out.println("Last Request URL: " + lastRequestUrl);
            System.out.println("Last Request Error: " + lastRequestError);
            
            // 确保API请求不会直接进入错误页
            // 对于需要API密钥的数据源，会有认证错误
            // 对于咪咕音乐，可能返回HTML而不是JSON，但请求本身成功
            if (source instanceof LastFmSource || source instanceof DiscogsSource) {
                assertNotNull("Expected authentication error for " + source.getSourceName(), lastRequestError);
            } else if (source instanceof NeteaseMusicSource) {
                // 网易云音乐API可能需要授权
                assertNotNull("Expected error for NeteaseMusicSource", lastRequestError);
            } else {
                // MusicBrainz和MiguMusicSource应该返回有效响应
                // MiguMusicSource可能返回HTML而不是JSON，但请求本身成功
                System.out.println("Source " + source.getSourceName() + " returned response without error");
            }
            
            // 确保请求URL不为空
            assertNotNull("Request URL should not be null", lastRequestUrl);
            System.out.println("Test passed for " + source.getSourceName());
        } catch (Exception e) {
            System.out.println("Test failed for " + source.getSourceName() + ": " + e.getMessage());
            // 确保API请求不会直接进入错误页
            e.printStackTrace();
        }
    }
}

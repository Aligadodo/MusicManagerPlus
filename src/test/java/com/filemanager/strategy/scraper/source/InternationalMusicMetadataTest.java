package com.filemanager.strategy.scraper.source;

import com.filemanager.strategy.scraper.model.*;
import com.filemanager.strategy.scraper.source.impl.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 测试不同国家、语种、风格的流行歌曲元数据刮削
 * 用于验证各个数据源的可用性
 */
public class InternationalMusicMetadataTest {

    /**
     * 测试歌曲信息类
     */
    static class TestSong {
        private final String artist;        // 艺术家
        private final String title;         // 歌曲标题
        private final String album;         // 专辑名称
        private final String country;       // 国家/地区
        private final String language;      // 语言
        private final String genre;         // 流派
        private final int duration;         // 歌曲时长（秒）

        public TestSong(String artist, String title, String album, String country, String language, String genre, int duration) {
            this.artist = artist;
            this.title = title;
            this.album = album;
            this.country = country;
            this.language = language;
            this.genre = genre;
            this.duration = duration;
        }

        public String getArtist() {
            return artist;
        }

        public String getTitle() {
            return title;
        }

        public String getAlbum() {
            return album;
        }

        public String getCountry() {
            return country;
        }

        public String getLanguage() {
            return language;
        }

        public String getGenre() {
            return genre;
        }

        public int getDuration() {
            return duration;
        }

        @Override
        public String toString() {
            return artist + " - " + title + " (" + country + ", " + language + ", " + genre + ")";
        }
    }

    /**
     * 测试结果类
     */
    static class TestResult {
        private final TestSong song;
        private final String source;
        private final boolean trackInfoFound;
        private final boolean albumInfoFound;
        private final boolean coverFound;
        private final boolean lyricsFound;

        public TestResult(TestSong song, String source, boolean trackInfoFound, boolean albumInfoFound, boolean coverFound, boolean lyricsFound) {
            this.song = song;
            this.source = source;
            this.trackInfoFound = trackInfoFound;
            this.albumInfoFound = albumInfoFound;
            this.coverFound = coverFound;
            this.lyricsFound = lyricsFound;
        }

        public TestSong getSong() {
            return song;
        }

        public String getSource() {
            return source;
        }

        public boolean isTrackInfoFound() {
            return trackInfoFound;
        }

        public boolean isAlbumInfoFound() {
            return albumInfoFound;
        }

        public boolean isCoverFound() {
            return coverFound;
        }

        public boolean isLyricsFound() {
            return lyricsFound;
        }

        public int getScore() {
            int score = 0;
            if (trackInfoFound) score++;
            if (albumInfoFound) score++;
            if (coverFound) score++;
            if (lyricsFound) score++;
            return score;
        }
    }

    /**
     * 测试歌曲列表 - 包含不同国家、语种、风格的流行歌曲
     */
    private static List<TestSong> getTestSongs() {
        List<TestSong> songs = new ArrayList<>();

        // 中文歌曲
        songs.add(new TestSong("周杰伦", "七里香", "七里香", "中国台湾", "中文", "流行", 279));
        songs.add(new TestSong("陈奕迅", "浮夸", "U87", "中国香港", "中文", "流行", 287));
        songs.add(new TestSong("Taylor Swift", "Love Story", "Fearless", "美国", "英文", "乡村流行", 236));
        songs.add(new TestSong("Ed Sheeran", "Shape of You", "Divide", "英国", "英文", "流行", 233));
        songs.add(new TestSong("Billie Eilish", "Bad Guy", "When We All Fall Asleep, Where Do We Go?", "美国", "英文", "另类流行", 194));
        songs.add(new TestSong("BTS", "Dynamite", "Be", "韩国", "英文", "K-pop", 213));
        songs.add(new TestSong("BLACKPINK", "Kill This Love", "Kill This Love", "韩国", "韩文/英文", "K-pop", 208));
        songs.add(new TestSong("Arashi", "Face Down", "Face Down", "日本", "日文", "J-pop", 243));
        songs.add(new TestSong("安室奈美惠", "Hero", "Hero", "日本", "日文", "J-pop", 286));
        songs.add(new TestSong("Shakira", "Hips Don't Lie", "Oral Fixation, Vol. 2", "哥伦比亚", "英文/西班牙文", "拉丁流行", 278));
        songs.add(new TestSong("Enrique Iglesias", "Bailando", "Sex and Love", "西班牙", "西班牙文", "拉丁流行", 288));
        songs.add(new TestSong("MC Hammer", "U Can't Touch This", "Please Hammer, Don't Hurt 'Em", "美国", "英文", "说唱", 290));
        songs.add(new TestSong("Queen", "Bohemian Rhapsody", "A Night at the Opera", "英国", "英文", "摇滚", 354));
        songs.add(new TestSong("Michael Jackson", "Thriller", "Thriller", "美国", "英文", "流行", 357));
        songs.add(new TestSong("Madonna", "Like a Virgin", "Like a Virgin", "美国", "英文", "流行", 235));

        return songs;
    }

    /**
     * 初始化数据源
     */
    private static List<MetadataSource> initializeSources() {
        List<MetadataSource> sources = new ArrayList<>();

        sources.add(new ITunesSource());        // iTunes (苹果音乐)
        sources.add(new MusicBrainzSource());   // MusicBrainz (开源数据库)
        sources.add(new NeteaseMusicSource());  // 网易云音乐 (中文歌曲)
        sources.add(new MiguMusicSource());     // 咪咕音乐 (版权歌曲)
        sources.add(new LastFmSource());        // Last.fm (全球音乐平台)
        sources.add(new DiscogsSource());       // Discogs (音乐数据库)

        return sources;
    }

    /**
     * 测试所有数据源对国际流行歌曲的元数据刮削能力
     */
    @Test
    public void testInternationalMusicMetadata() {
        System.out.println("开始测试国际流行歌曲元数据刮削");
        System.out.println("=====================================");

        List<TestSong> testSongs = getTestSongs();
        List<MetadataSource> sources = initializeSources();
        List<TestResult> results = new ArrayList<>();

        System.out.println("测试歌曲列表:");
        for (int i = 0; i < testSongs.size(); i++) {
            System.out.println((i + 1) + ". " + testSongs.get(i));
        }
        System.out.println("=====================================");

        // 对每首歌曲测试所有数据源
        for (TestSong song : testSongs) {
            System.out.println("\n测试歌曲: " + song);
            System.out.println("=====================================");

            for (MetadataSource source : sources) {
                TestResult result = testSongWithSource(song, source);
                results.add(result);
            }
        }

        // 生成测试报告
        generateTestReport(results);

        System.out.println("=====================================");
        System.out.println("国际流行歌曲元数据刮削测试完成");
    }

    /**
     * 使用单个数据源测试单个歌曲
     */
    private static TestResult testSongWithSource(TestSong song, MetadataSource source) {
        System.out.println("\n测试数据源: " + source.getSourceName());
        System.out.println("-------------------------------------");

        boolean trackInfoFound = false;
        boolean albumInfoFound = false;
        boolean coverFound = false;
        boolean lyricsFound = false;

        // 测试搜索曲目信息
        System.out.println("1. 测试搜索曲目信息:");
        try {
            TrackInfo trackInfo = source.searchTrackInfo(song.getArtist(), song.getTitle());
            if (trackInfo != null) {
                System.out.println("   ✅ 成功获取曲目信息:");
                System.out.println("      - 标题: " + trackInfo.getTitle());
                System.out.println("      - 艺术家: " + trackInfo.getArtist());
                System.out.println("      - 专辑: " + trackInfo.getAlbum());
                trackInfoFound = true;
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
            AlbumInfo albumInfo = source.searchAlbumInfo(song.getArtist(), song.getAlbum());
            if (albumInfo != null) {
                System.out.println("   ✅ 成功获取专辑信息:");
                System.out.println("      - 专辑名称: " + albumInfo.getName());
                System.out.println("      - 艺术家: " + albumInfo.getArtist());
                System.out.println("      - 年份: " + albumInfo.getYear());
                albumInfoFound = true;
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
            CoverInfo coverInfo = source.searchCover(song.getArtist(), song.getAlbum());
            if (coverInfo != null) {
                System.out.println("   ✅ 成功获取封面信息:");
                System.out.println("      - 封面URL: " + coverInfo.getImageUrl());
                System.out.println("      - 格式: " + coverInfo.getFormat());
                System.out.println("      - 尺寸: " + coverInfo.getWidth() + "x" + coverInfo.getHeight());
                coverFound = true;
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
            LyricsInfo lyricsInfo = source.searchLyrics(song.getArtist(), song.getTitle(), song.getDuration());
            if (lyricsInfo != null && lyricsInfo.getContent() != null && !lyricsInfo.getContent().isEmpty()) {
                System.out.println("   ✅ 成功获取歌词:");
                System.out.println("      - 歌词长度: " + lyricsInfo.getContent().length() + "字符");
                System.out.println("      - 歌词预览: " + (lyricsInfo.getContent().length() > 100 ? lyricsInfo.getContent().substring(0, 100) + "..." : lyricsInfo.getContent()));
                lyricsFound = true;
            } else {
                System.out.println("   ❌ 未找到歌词");
            }
        } catch (Exception e) {
            System.out.println("   ❌ 搜索歌词失败: " + e.getMessage());
        }
        System.out.println("   请求URL: " + source.getLastRequestUrl());
        System.out.println("   错误信息: " + source.getLastRequestError());

        return new TestResult(song, source.getSourceName(), trackInfoFound, albumInfoFound, coverFound, lyricsFound);
    }

    /**
     * 生成测试报告
     */
    private static void generateTestReport(List<TestResult> results) {
        System.out.println("\n=====================================");
        System.out.println("测试报告");
        System.out.println("=====================================");

        // 按数据源分组统计
        java.util.Map<String, List<TestResult>> resultsBySource = new java.util.HashMap<>();
        for (TestResult result : results) {
            resultsBySource.computeIfAbsent(result.getSource(), k -> new ArrayList<>()).add(result);
        }

        // 统计每个数据源的表现
        for (Map.Entry<String, List<TestResult>> entry : resultsBySource.entrySet()) {
            String source = entry.getKey();
            List<TestResult> sourceResults = entry.getValue();

            int totalTests = sourceResults.size();
            int totalScore = 0;
            int trackInfoCount = 0;
            int albumInfoCount = 0;
            int coverCount = 0;
            int lyricsCount = 0;

            for (TestResult result : sourceResults) {
                totalScore += result.getScore();
                if (result.isTrackInfoFound()) trackInfoCount++;
                if (result.isAlbumInfoFound()) albumInfoCount++;
                if (result.isCoverFound()) coverCount++;
                if (result.isLyricsFound()) lyricsCount++;
            }

            double avgScore = (double) totalScore / (totalTests * 4) * 100;

            System.out.println("\n数据源: " + source);
            System.out.println("测试歌曲数: " + totalTests);
            System.out.println("平均得分: " + String.format("%.1f%%", avgScore));
            System.out.println("曲目信息: " + trackInfoCount + "/" + totalTests + " (" + String.format("%.1f%%", (double) trackInfoCount / totalTests * 100) + ")");
            System.out.println("专辑信息: " + albumInfoCount + "/" + totalTests + " (" + String.format("%.1f%%", (double) albumInfoCount / totalTests * 100) + ")");
            System.out.println("封面信息: " + coverCount + "/" + totalTests + " (" + String.format("%.1f%%", (double) coverCount / totalTests * 100) + ")");
            System.out.println("歌词信息: " + lyricsCount + "/" + totalTests + " (" + String.format("%.1f%%", (double) lyricsCount / totalTests * 100) + ")");
        }

        // 找出表现最好的数据源
        System.out.println("\n=====================================");
        System.out.println("数据源表现排名");
        System.out.println("=====================================");

        resultsBySource.entrySet().stream()
                .sorted((e1, e2) -> {
                    double score1 = calculateAverageScore(e1.getValue());
                    double score2 = calculateAverageScore(e2.getValue());
                    return Double.compare(score2, score1);
                })
                .forEach(entry -> {
                    double avgScore = calculateAverageScore(entry.getValue());
                    System.out.println(entry.getKey() + ": " + String.format("%.1f%%", avgScore));
                });
    }

    /**
     * 计算数据源的平均得分
     */
    private static double calculateAverageScore(List<TestResult> results) {
        if (results.isEmpty()) return 0;
        int totalScore = results.stream().mapToInt(TestResult::getScore).sum();
        return (double) totalScore / (results.size() * 4) * 100;
    }

    /**
     * 主方法用于单独运行测试
     */
    public static void main(String[] args) {
        InternationalMusicMetadataTest test = new InternationalMusicMetadataTest();
        test.testInternationalMusicMetadata();
    }
}

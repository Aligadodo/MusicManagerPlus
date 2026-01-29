package com.filemanager.util;

import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 扩展元数据提取算法测试用例
 * 基于更多真实文件路径测试元数据提取功能
 * 包含不同风格的目录结构
 */
public class ExtendedMetadataExtractionTest {

    /**
     * 测试用例类
     */
    static class TestCase {
        private final String filePath;
        private final String expectedArtist;
        private final String expectedAlbum;
        private final String expectedTitle;
        private final String expectedTrack;
        private final String expectedYear;
        private final String description;

        public TestCase(String filePath, String expectedArtist, String expectedAlbum, 
                     String expectedTitle, String expectedTrack, String expectedYear, String description) {
            this.filePath = filePath;
            this.expectedArtist = expectedArtist;
            this.expectedAlbum = expectedAlbum;
            this.expectedTitle = expectedTitle;
            this.expectedTrack = expectedTrack;
            this.expectedYear = expectedYear;
            this.description = description;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getExpectedArtist() {
            return expectedArtist;
        }

        public String getExpectedAlbum() {
            return expectedAlbum;
        }

        public String getExpectedTitle() {
            return expectedTitle;
        }

        public String getExpectedTrack() {
            return expectedTrack;
        }

        public String getExpectedYear() {
            return expectedYear;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return description + ": " + filePath;
        }
    }

    /**
     * 测试结果类
     */
    static class TestResult {
        private final TestCase testCase;
        private final MetadataHelper.AudioMeta actualMeta;
        private final boolean artistMatch;
        private final boolean albumMatch;
        private final boolean titleMatch;
        private final boolean trackMatch;
        private final boolean yearMatch;

        public TestResult(TestCase testCase, MetadataHelper.AudioMeta actualMeta) {
            this.testCase = testCase;
            this.actualMeta = actualMeta;
            this.artistMatch = equals(testCase.getExpectedArtist(), actualMeta.getArtist());
            this.albumMatch = equals(testCase.getExpectedAlbum(), actualMeta.getAlbum());
            this.titleMatch = equals(testCase.getExpectedTitle(), actualMeta.getTitle());
            this.trackMatch = equals(testCase.getExpectedTrack(), actualMeta.getTrack());
            this.yearMatch = equals(testCase.getExpectedYear(), actualMeta.getYear());
        }

        private boolean equals(String expected, String actual) {
            if (expected == null && actual == null) return true;
            if (expected == null || actual == null) return false;
            return expected.trim().equalsIgnoreCase(actual.trim());
        }

        public TestCase getTestCase() {
            return testCase;
        }

        public MetadataHelper.AudioMeta getActualMeta() {
            return actualMeta;
        }

        public boolean isArtistMatch() {
            return artistMatch;
        }

        public boolean isAlbumMatch() {
            return albumMatch;
        }

        public boolean isTitleMatch() {
            return titleMatch;
        }

        public boolean isTrackMatch() {
            return trackMatch;
        }

        public boolean isYearMatch() {
            return yearMatch;
        }

        public int getScore() {
            int score = 0;
            if (artistMatch) score++;
            if (albumMatch) score++;
            if (titleMatch) score++;
            if (trackMatch) score++;
            if (yearMatch) score++;
            return score;
        }

        public int getMaxScore() {
            int maxScore = 0;
            if (testCase.getExpectedArtist() != null) maxScore++;
            if (testCase.getExpectedAlbum() != null) maxScore++;
            if (testCase.getExpectedTitle() != null) maxScore++;
            if (testCase.getExpectedTrack() != null) maxScore++;
            if (testCase.getExpectedYear() != null) maxScore++;
            return maxScore;
        }

        public double getAccuracy() {
            int maxScore = getMaxScore();
            if (maxScore == 0) return 100.0;
            return (double) getScore() / maxScore * 100;
        }
    }

    /**
     * 获取扩展测试用例
     */
    private static List<TestCase> getExtendedTestCases() {
        List<TestCase> cases = new ArrayList<>();

        // 阿杜专辑（标准格式）
        cases.add(new TestCase(
            "W:\\A - 阿杜\\2002-坚持到底[台湾首版][WAV]\\Split - WAV\\01 - 坚持到底.wav",
            "阿杜",
            "坚持到底",
            "坚持到底",
            "01",
            "2002",
            "阿杜专辑：标准格式"
        ));

        // 周杰伦专辑（年份开头）
        cases.add(new TestCase(
            "W:\\Z - 周杰伦\\周杰伦 - 2012 - 12新作 - WAV\\乌克丽丽.wav",
            "周杰伦",
            "12新作",
            "乌克丽丽",
            null,
            "2012",
            "周杰伦专辑：年份开头"
        ));

        cases.add(new TestCase(
            "W:\\Z - 周杰伦\\周杰伦 - 2012 - 12新作 - WAV\\傻笑.wav",
            "周杰伦",
            "12新作",
            "傻笑",
            null,
            "2012",
            "周杰伦专辑：年份开头"
        ));

        // 林俊杰专辑（复杂格式）
        cases.add(new TestCase(
            "W:\\L - 林俊杰\\林俊杰.2023 - JJ的咖啡调调, Vol.1【十倍音质wav分轨】\\01.wav",
            "林俊杰",
            "JJ的咖啡调调, Vol.1",
            null,
            "01",
            "2023",
            "林俊杰专辑：复杂格式"
        ));

        // 凤凰传奇专辑（DTS格式）
        cases.add(new TestCase(
            "W:\\F - 凤凰传奇\\DTS-凤凰传奇《东方民谣》2CD1\\01.wav",
            "凤凰传奇",
            "东方民谣",
            null,
            "01",
            null,
            "凤凰传奇专辑：DTS格式"
        ));

        // 陶喆专辑（日期格式）
        cases.add(new TestCase(
            "W:\\T - 陶喆\\2013.06.11 - 再见你好吗 - WAV\\01.wav",
            "陶喆",
            "再见你好吗",
            null,
            "01",
            "2013",
            "陶喆专辑：日期格式"
        ));

        cases.add(new TestCase(
            "W:\\T - 陶喆\\1997.12.06 - DAVID.TAO - WAV\\01.wav",
            "陶喆",
            "DAVID.TAO",
            null,
            "01",
            "1997",
            "陶喆专辑：英文专辑名"
        ));

        // Beyond专辑（SACD格式）
        cases.add(new TestCase(
            "W:\\B - Beyond\\1986-再见理想[香港A字首版][WAV]\\01.wav",
            "Beyond",
            "再见理想",
            null,
            "01",
            "1986",
            "Beyond专辑：SACD格式"
        ));

        // 陈奕迅专辑（DTS格式）
        cases.add(new TestCase(
            "W:\\C - 陈奕迅\\DTS-陈奕迅 天生歌狂 2CD1 - WAV\\01.wav",
            "陈奕迅",
            "天生歌狂",
            null,
            "01",
            null,
            "陈奕迅专辑：DTS格式"
        ));

        // 孙楠专辑（标准格式）
        cases.add(new TestCase(
            "W:\\S - 孙楠\\孙楠 - 2000 - 起风了 - WAV\\01.wav",
            "孙楠",
            "起风了",
            null,
            "01",
            "2000",
            "孙楠专辑：标准格式"
        ));

        // 林志炫专辑（标准格式）
        cases.add(new TestCase(
            "W:\\L - 林志炫\\林志炫 - 2000 - 美丽时光 - WAV\\01.wav",
            "林志炫",
            "美丽时光",
            null,
            "01",
            "2000",
            "林志炫专辑：标准格式"
        ));

        // 张学友专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Z - 张学友\\张学友 - 1993 - 我与你 - WAV\\01.wav",
            "张学友",
            "我与你",
            null,
            "01",
            "1993",
            "张学友专辑：标准格式"
        ));

        // 王菲专辑（标准格式）
        cases.add(new TestCase(
            "W:\\W - 王菲\\王菲 - 1998 - 唱游 - WAV\\01.wav",
            "王菲",
            "唱游",
            null,
            "01",
            "1998",
            "王菲专辑：标准格式"
        ));

        // 伍佰专辑（标准格式）
        cases.add(new TestCase(
            "W:\\W - 伍佰\\伍佰 - 1992 - 浪人情歌 - WAV\\01.wav",
            "伍佰",
            "浪人情歌",
            null,
            "01",
            "1992",
            "伍佰专辑：标准格式"
        ));

        // 张杰专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Z - 张杰\\张杰 - 2012 - 这，就是爱 - WAV\\01.wav",
            "张杰",
            "这，就是爱",
            null,
            "01",
            "2012",
            "张杰专辑：标准格式"
        ));

        // 张靓颖专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Z - 张靓颖\\张靓颖 - 2007 - 梦中画 - WAV\\01.wav",
            "张靓颖",
            "梦中画",
            null,
            "01",
            "2007",
            "张靓颖专辑：标准格式"
        ));

        // 邓丽君专辑（标准格式）
        cases.add(new TestCase(
            "W:\\D - 邓丽君\\邓丽君 - 1983 - 淡淡幽情 - WAV\\01.wav",
            "邓丽君",
            "淡淡幽情",
            null,
            "01",
            "1983",
            "邓丽君专辑：标准格式"
        ));

        // 任贤齐专辑（标准格式）
        cases.add(new TestCase(
            "W:\\R - 任贤齐\\任贤齐 - 1988 - 爱到极至 - WAV\\01.wav",
            "任贤齐",
            "爱到极至",
            null,
            "01",
            "1988",
            "任贤齐专辑：标准格式"
        ));

        // 蔡琴专辑（标准格式）
        cases.add(new TestCase(
            "W:\\C - 蔡琴\\蔡琴 - 1994 - 空白 - WAV\\01.wav",
            "蔡琴",
            "空白",
            null,
            "01",
            "1994",
            "蔡琴专辑：标准格式"
        ));

        // 陈百强专辑（标准格式）
        cases.add(new TestCase(
            "W:\\C - 陈百强\\陈百强 - 1985 - 深爱着你 - WAV\\01.wav",
            "陈百强",
            "深爱着你",
            null,
            "01",
            "1985",
            "陈百强专辑：标准格式"
        ));

        // 罗大佑专辑（标准格式）
        cases.add(new TestCase(
            "W:\\L - 罗大佑\\罗大佑 - 1982 - 之乎者也 - WAV\\01.wav",
            "罗大佑",
            "之乎者也",
            null,
            "01",
            "1982",
            "罗大佑专辑：标准格式"
        ));

        // 李宗盛专辑（标准格式）
        cases.add(new TestCase(
            "W:\\L - 李宗盛\\李宗盛 - 1986 - 生命中的精灵 - WAV\\01.wav",
            "李宗盛",
            "生命中的精灵",
            null,
            "01",
            "1986",
            "李宗盛专辑：标准格式"
        ));

        // 张国荣专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Z - 张国荣\\张国荣 - 1987 - Summer Romance - WAV\\01.wav",
            "张国荣",
            "Summer Romance",
            null,
            "01",
            "1987",
            "张国荣专辑：英文专辑名"
        ));

        // 梅艳芳专辑（标准格式）
        cases.add(new TestCase(
            "W:\\M - 梅艳芳\\梅艳芳 - 1985 - 似水流年 - WAV\\01.wav",
            "梅艳芳",
            "似水流年",
            null,
            "01",
            "1985",
            "梅艳芳专辑：标准格式"
        ));

        // 徐小凤专辑（标准格式）
        cases.add(new TestCase(
            "W:\\X - 徐小凤\\徐小凤 - 1983 - 随想曲 - WAV\\01.wav",
            "徐小凤",
            "随想曲",
            null,
            "01",
            "1983",
            "徐小凤专辑：标准格式"
        ));

        // 许巍专辑（标准格式）
        cases.add(new TestCase(
            "W:\\X - 许巍\\许巍 - 2000 - 那一天 - WAV\\01.wav",
            "许巍",
            "那一天",
            null,
            "01",
            "2000",
            "许巍专辑：标准格式"
        ));

        // 许茹芸专辑（标准格式）
        cases.add(new TestCase(
            "W:\\X - 许茹芸\\许茹芸 - 2001 - 只是一个人 - WAV\\01.wav",
            "许茹芸",
            "只是一个人",
            null,
            "01",
            "2001",
            "许茹芸专辑：标准格式"
        ));

        // 韩红专辑（标准格式）
        cases.add(new TestCase(
            "W:\\H - 韩红\\韩红 - 1999 - 醒了 - WAV\\01.wav",
            "韩红",
            "醒了",
            null,
            "01",
            "1999",
            "韩红专辑：标准格式"
        ));

        // 毛不易专辑（标准格式）
        cases.add(new TestCase(
            "W:\\M - 毛不易\\毛不易 - 2018 - 平凡的一天 - WAV\\01.wav",
            "毛不易",
            "平凡的一天",
            null,
            "01",
            "2018",
            "毛不易专辑：标准格式"
        ));

        // 莫文蔚专辑（标准格式）
        cases.add(new TestCase(
            "W:\\M - 莫文蔚\\莫文蔚 - 1999 - You Can - WAV\\01.wav",
            "莫文蔚",
            "You Can",
            null,
            "01",
            "1999",
            "莫文蔚专辑：英文专辑名"
        ));

        // 米线专辑（标准格式）
        cases.add(new TestCase(
            "W:\\M - 米线\\米线 - 1995 - 痴了 - WAV\\01.wav",
            "米线",
            "痴了",
            null,
            "01",
            "1995",
            "米线专辑：标准格式"
        ));

        // 南拳妈妈专辑（标准格式）
        cases.add(new TestCase(
            "W:\\N - 南拳妈妈\\南拳妈妈 - 1995 - 逍遥游 - WAV\\01.wav",
            "南拳妈妈",
            "逍遥游",
            null,
            "01",
            "1995",
            "南拳妈妈专辑：标准格式"
        ));

        // 告五人专辑（标准格式）
        cases.add(new TestCase(
            "W:\\G - 告五人\\告五人 - 1996 - 第一张 - WAV\\01.wav",
            "告五人",
            "第一张",
            null,
            "01",
            "1996",
            "告五人专辑：标准格式"
        ));

        // 金池专辑（标准格式）
        cases.add(new TestCase(
            "W:\\J - 金池\\金池 - 1990 - 奉献 - WAV\\01.wav",
            "金池",
            "奉献",
            null,
            "01",
            "1990",
            "金池专辑：标准格式"
        ));

        // 孙燕姿专辑（标准格式）
        cases.add(new TestCase(
            "W:\\S - 孙燕姿\\孙燕姿 - 2000 - 孙燕姿 - WAV\\01.wav",
            "孙燕姿",
            "孙燕姿",
            null,
            "01",
            "2000",
            "孙燕姿专辑：同名专辑"
        ));

        // 田馥甄专辑（标准格式）
        cases.add(new TestCase(
            "W:\\T - 田馥甄\\田馥甄 - 1997 - 雪花飘蝶 - WAV\\01.wav",
            "田馥甄",
            "雪花飘蝶",
            null,
            "01",
            "1997",
            "田馥甄专辑：标准格式"
        ));

        // 童安格专辑（标准格式）
        cases.add(new TestCase(
            "W:\\T - 童安格\\童安格 - 1995 - 天空 - WAV\\01.wav",
            "童安格",
            "天空",
            null,
            "01",
            "1995",
            "童安格专辑：标准格式"
        ));

        // 腾格尔专辑（标准格式）
        cases.add(new TestCase(
            "W:\\T - 腾格尔\\腾格尔 - 1994 - 草原 - WAV\\01.wav",
            "腾格尔",
            "草原",
            null,
            "01",
            "1994",
            "腾格尔专辑：标准格式"
        ));

        // 伍思凯专辑（标准格式）
        cases.add(new TestCase(
            "W:\\W - 伍思凯\\伍思凯 - 1999 - 笑傲江湖 - WAV\\01.wav",
            "伍思凯",
            "笑傲江湖",
            null,
            "01",
            "1999",
            "伍思凯专辑：标准格式"
        ));

        // 吴克群专辑（标准格式）
        cases.add(new TestCase(
            "W:\\W - 吴克群\\吴克群 - 1990 - 谁是大英雄 - WAV\\01.wav",
            "吴克群",
            "谁是大英雄",
            null,
            "01",
            "1990",
            "吴克群专辑：标准格式"
        ));

        // 汪峰专辑（标准格式）
        cases.add(new TestCase(
            "W:\\W - 汪峰\\汪峰 - 2000 - 花火 - WAV\\01.wav",
            "汪峰",
            "花火",
            null,
            "01",
            "2000",
            "汪峰专辑：标准格式"
        ));

        // 汪苏泷专辑（标准格式）
        cases.add(new TestCase(
            "W:\\W - 汪苏泷\\汪苏泷 - 2001 - 蓝莲花 - WAV\\01.wav",
            "汪苏泷",
            "蓝莲花",
            null,
            "01",
            "2001",
            "汪苏泷专辑：标准格式"
        ));

        // 王力宏专辑（标准格式）
        cases.add(new TestCase(
            "W:\\W - 王力宏\\王力宏 - 1998 - 公转自转 - WAV\\01.wav",
            "王力宏",
            "公转自转",
            null,
            "01",
            "1998",
            "王力宏专辑：标准格式"
        ));

        // 王杰专辑（标准格式）
        cases.add(new TestCase(
            "W:\\W - 王杰\\王杰 - 1987 - 谁明浪子心 - WAV\\01.wav",
            "王杰",
            "谁明浪子心",
            null,
            "01",
            "1987",
            "王杰专辑：标准格式"
        ));

        // 王菲专辑（标准格式）
        cases.add(new TestCase(
            "W:\\W - 王菲\\王菲 - 1994 - 迷 - WAV\\01.wav",
            "王菲",
            "迷",
            null,
            "01",
            "1994",
            "王菲专辑：标准格式"
        ));

        // 魏如萱专辑（标准格式）
        cases.add(new TestCase(
            "W:\\W - 魏如萱\\魏如萱 - 2000 - 天使 - WAV\\01.wav",
            "魏如萱",
            "天使",
            null,
            "01",
            "2000",
            "魏如萱专辑：标准格式"
        ));

        // 薛之谦专辑（标准格式）
        cases.add(new TestCase(
            "W:\\X - 薛之谦\\薛之谦 - 2016 - 初学者 - WAV\\01.wav",
            "薛之谦",
            "初学者",
            null,
            "01",
            "2016",
            "薛之谦专辑：标准格式"
        ));

        // 许巍专辑（标准格式）
        cases.add(new TestCase(
            "W:\\X - 许巍\\许巍 - 2004 - 每一刻都是崭新的 - WAV\\01.wav",
            "许巍",
            "每一刻都是崭新的",
            null,
            "01",
            "2004",
            "许巍专辑：标准格式"
        ));

        // 许美静专辑（标准格式）
        cases.add(new TestCase(
            "W:\\X - 许美静\\许美静 - 2000 - 静听 - WAV\\01.wav",
            "许美静",
            "静听",
            null,
            "01",
            "2000",
            "许美静专辑：标准格式"
        ));

        // 谢春花专辑（标准格式）
        cases.add(new TestCase(
            "W:\\X - 谢春花\\谢春花 - 1999 - 懂你 - WAV\\01.wav",
            "谢春花",
            "懂你",
            null,
            "01",
            "1999",
            "谢春花专辑：标准格式"
        ));

        // 辛晓琪专辑（标准格式）
        cases.add(new TestCase(
            "W:\\X - 辛晓琪\\辛晓琪 - 1999 - 第一张 - WAV\\01.wav",
            "辛晓琪",
            "第一张",
            null,
            "01",
            "1999",
            "辛晓琪专辑：标准格式"
        ));

        // 杨丞琳专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Y - 杨丞琳\\杨丞琳 - 1999 - 雨爱 - WAV\\01.wav",
            "杨丞琳",
            "雨爱",
            null,
            "01",
            "1999",
            "杨丞琳专辑：标准格式"
        ));

        // 杨千嬅专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Y - 杨千嬅\\杨千嬅 - 1998 - 夏天的故事 - WAV\\01.wav",
            "杨千嬅",
            "夏天的故事",
            null,
            "01",
            "1998",
            "杨千嬅专辑：标准格式"
        ));

        // 杨坤专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Y - 杨坤\\杨坤 - 2002 - 无所谓 - WAV\\01.wav",
            "杨坤",
            "无所谓",
            null,
            "01",
            "2002",
            "杨坤专辑：标准格式"
        ));

        // 杨钰莹专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Y - 杨钰莹\\杨钰莹 - 1998 - 雪花飘蝶 - WAV\\01.wav",
            "杨钰莹",
            "雪花飘蝶",
            null,
            "01",
            "1998",
            "杨钰莹专辑：标准格式"
        ));

        // 周传雄专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Z - 周传雄\\周传雄 - 1999 - 我心依旧 - WAV\\01.wav",
            "周传雄",
            "我心依旧",
            null,
            "01",
            "1999",
            "周传雄专辑：标准格式"
        ));

        // 周华健专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Z - 周华健\\周华健 - 1999 - 让我欢喜让我忧 - WAV\\01.wav",
            "周华健",
            "让我欢喜让我忧",
            null,
            "01",
            "1999",
            "周华健专辑：标准格式"
        ));

        // 周杰伦专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Z - 周杰伦\\周杰伦 - 2000 - Jay - WAV\\01.wav",
            "周杰伦",
            "Jay",
            null,
            "01",
            "2000",
            "周杰伦专辑：英文专辑名"
        ));

        // 周深专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Z - 周深\\周深 - 1999 - 声声慢 - WAV\\01.wav",
            "周深",
            "声声慢",
            null,
            "01",
            "1999",
            "周深专辑：标准格式"
        ));

        // 周笔畅专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Z - 周笔畅\\周笔畅 - 1999 - 歌手 - WAV\\01.wav",
            "周笔畅",
            "歌手",
            null,
            "01",
            "1999",
            "周笔畅专辑：标准格式"
        ));

        // 庄心妍专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Z - 庄心妍\\庄心妍 - 1999 - 关心 - WAV\\01.wav",
            "庄心妍",
            "关心",
            null,
            "01",
            "1999",
            "庄心妍专辑：标准格式"
        ));

        // 张信哲专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Z - 张信哲\\张信哲 - 1999 - 心太软 - WAV\\01.wav",
            "张信哲",
            "心太软",
            null,
            "01",
            "1999",
            "张信哲专辑：标准格式"
        ));

        // 张国荣专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Z - 张国荣\\张国荣 - 1987 - Summer Romance - WAV\\01.wav",
            "张国荣",
            "Summer Romance",
            null,
            "01",
            "1987",
            "张国荣专辑：英文专辑名"
        ));

        // 张学友专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Z - 张学友\\张学友 - 1993 - 我与你 - WAV\\01.wav",
            "张学友",
            "我与你",
            null,
            "01",
            "1993",
            "张学友专辑：标准格式"
        ));

        // 张宇专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Z - 张宇\\张宇 - 1999 - 月亮惹的祸 - WAV\\01.wav",
            "张宇",
            "月亮惹的祸",
            null,
            "01",
            "1999",
            "张宇专辑：标准格式"
        ));

        // 张惠妹专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Z - 张惠妹\\张惠妹 - 1999 - 我可以 - WAV\\01.wav",
            "张惠妹",
            "我可以",
            null,
            "01",
            "1999",
            "张惠妹专辑：标准格式"
        ));

        // 张敬轩专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Z - 张敬轩\\张敬轩 - 1999 - 春天 - WAV\\01.wav",
            "张敬轩",
            "春天",
            null,
            "01",
            "1999",
            "张敬轩专辑：标准格式"
        ));

        // 张杰专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Z - 张杰\\张杰 - 2012 - 这，就是爱 - WAV\\01.wav",
            "张杰",
            "这，就是爱",
            null,
            "01",
            "2012",
            "张杰专辑：标准格式"
        ));

        // 张碧晨专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Z - 张碧晨\\张碧晨 - 1999 - 花火 - WAV\\01.wav",
            "张碧晨",
            "花火",
            null,
            "01",
            "1999",
            "张碧晨专辑：标准格式"
        ));

        // 张震岳专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Z - 张震岳\\张震岳 - 1999 - 第一张 - WAV\\01.wav",
            "张震岳",
            "第一张",
            null,
            "01",
            "1999",
            "张震岳专辑：标准格式"
        ));

        // 张韶涵专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Z - 张韶涵\\张韶涵 - 1999 - 欧若拉 - WAV\\01.wav",
            "张韶涵",
            "欧若拉",
            null,
            "01",
            "1999",
            "张韶涵专辑：标准格式"
        ));

        // 赵雷专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Z - 赵雷\\赵雷 - 1999 - 白桦林 - WAV\\01.wav",
            "赵雷",
            "白桦林",
            null,
            "01",
            "1999",
            "赵雷专辑：标准格式"
        ));

        // 赵鹏专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Z - 赵鹏\\赵鹏 - 1999 - 三里屯 - WAV\\01.wav",
            "赵鹏",
            "三里屯",
            null,
            "01",
            "1999",
            "赵鹏专辑：标准格式"
        ));

        // 郑源专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Z - 郑源\\郑源 - 1999 - 恋恋 - WAV\\01.wav",
            "郑源",
            "恋爱",
            null,
            "01",
            "1999",
            "郑源专辑：标准格式"
        ));

        // 郑秀文专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Z - 郑秀文\\郑秀文 - 1999 - 爱情 - WAV\\01.wav",
            "郑秀文",
            "爱情",
            null,
            "01",
            "1999",
            "郑秀文专辑：标准格式"
        ));

        // 郑钧专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Z - 郑钧\\郑钧 - 1999 - 爱情 - WAV\\01.wav",
            "郑钧",
            "爱情",
            null,
            "01",
            "1999",
            "郑钧专辑：标准格式"
        ));

        // 李克勤专辑（标准格式）
        cases.add(new TestCase(
            "W:\\L - 李克勤\\李克勤 - 1999 - 红日 - WAV\\01.wav",
            "李克勤",
            "红日",
            null,
            "01",
            "1999",
            "李克勤专辑：标准格式"
        ));

        // 李圣杰专辑（标准格式）
        cases.add(new TestCase(
            "W:\\L - 李圣杰\\李圣杰 - 1999 - 情书 - WAV\\01.wav",
            "李圣杰",
            "情书",
            null,
            "01",
            "1999",
            "李圣杰专辑：标准格式"
        ));

        // 李玉刚专辑（标准格式）
        cases.add(new TestCase(
            "W:\\L - 李玉刚\\李玉刚 - 1999 - 常回家看看 - WAV\\01.wav",
            "李玉刚",
            "常回家看看",
            null,
            "01",
            "1999",
            "李玉刚专辑：标准格式"
        ));

        // 李玖哲专辑（标准格式）
        cases.add(new TestCase(
            "W:\\L - 李玖哲\\李玖哲 - 1999 - 一生有你 - WAV\\01.wav",
            "李玖哲",
            "一生有你",
            null,
            "01",
            "1999",
            "李玖哲专辑：标准格式"
        ));

        // 李玲玉专辑（标准格式）
        cases.add(new TestCase(
            "W:\\L - 李玲玉\\李玲玉 - 1999 - 爱情 - WAV\\01.wav",
            "李玲玉",
            "爱情",
            null,
            "01",
            "1999",
            "李玲玉专辑：标准格式"
        ));

        // 李荣浩专辑（标准格式）
        cases.add(new TestCase(
            "W:\\L - 李荣浩\\李荣浩 - 1999 - 爱情 - WAV\\01.wav",
            "李荣浩",
            "爱情",
            null,
            "01",
            "1999",
            "李荣浩专辑：标准格式"
        ));

        // 林俊杰专辑（标准格式）
        cases.add(new TestCase(
            "W:\\L - 林俊杰\\林俊杰 - 2003 - 第二天堂 - WAV\\01.wav",
            "林俊杰",
            "第二天堂",
            null,
            "01",
            "2003",
            "林俊杰专辑：标准格式"
        ));

        // 林子祥专辑（标准格式）
        cases.add(new TestCase(
            "W:\\L - 林子祥\\林子祥 - 1999 - 爱情 - WAV\\01.wav",
            "林子祥",
            "爱情",
            null,
            "01",
            "1999",
            "林子祥专辑：标准格式"
        ));

        // 林宥嘉专辑（标准格式）
        cases.add(new TestCase(
            "W:\\L - 林宥嘉\\林宥嘉 - 1999 - 爱情 - WAV\\01.wav",
            "林宥嘉",
            "爱情",
            null,
            "01",
            "1999",
            "林宥嘉专辑：标准格式"
        ));

        // 林忆莲专辑（标准格式）
        cases.add(new TestCase(
            "W:\\L - 林忆莲\\林忆莲 - 1999 - 爱情 - WAV\\01.wav",
            "林忆莲",
            "爱情",
            null,
            "01",
            "1999",
            "林忆莲专辑：标准格式"
        ));

        // 梁咏琪专辑（标准格式）
        cases.add(new TestCase(
            "W:\\L - 梁咏琪\\梁咏琪 - 1999 - 爱情 - WAV\\01.wav",
            "梁咏琪",
            "爱情",
            null,
            "01",
            "1999",
            "梁咏琪专辑：标准格式"
        ));

        // 梁静茹专辑（标准格式）
        cases.add(new TestCase(
            "W:\\L - 梁静茹\\梁静茹 - 1999 - 爱情 - WAV\\01.wav",
            "梁静茹",
            "爱情",
            null,
            "01",
            "1999",
            "梁静茹专辑：标准格式"
        ));

        // 罗大佑专辑（标准格式）
        cases.add(new TestCase(
            "W:\\L - 罗大佑\\罗大佑 - 1982 - 之乎者也 - WAV\\01.wav",
            "罗大佑",
            "之乎者也",
            null,
            "01",
            "1982",
            "罗大佑专辑：标准格式"
        ));

        // 罗志祥专辑（标准格式）
        cases.add(new TestCase(
            "W:\\L - 罗志祥\\罗志祥 - 1999 - 爱情 - WAV\\01.wav",
            "罗志祥",
            "爱情",
            null,
            "01",
            "1999",
            "罗志祥专辑：标准格式"
        ));

        // 老狼专辑（标准格式）
        cases.add(new TestCase(
            "W:\\L - 老狼\\老狼 - 1999 - 爱情 - WAV\\01.wav",
            "老狼",
            "爱情",
            null,
            "01",
            "1999",
            "老狼专辑：标准格式"
        ));

        // 黎明专辑（标准格式）
        cases.add(new TestCase(
            "W:\\L - 黎明\\黎明 - 1999 - 爱情 - WAV\\01.wav",
            "黎明",
            "爱情",
            null,
            "01",
            "1999",
            "黎明专辑：标准格式"
        ));

        // 李宗盛专辑（标准格式）
        cases.add(new TestCase(
            "W:\\L - 李宗盛\\李宗盛 - 1986 - 生命中的精灵 - WAV\\01.wav",
            "李宗盛",
            "生命中的精灵",
            null,
            "01",
            "1986",
            "李宗盛专辑：标准格式"
        ));

        // 梦之旅专辑（标准格式）
        cases.add(new TestCase(
            "W:\\M - 梦之旅\\梦之旅 - 1999 - 爱情 - WAV\\01.wav",
            "梦之旅",
            "爱情",
            null,
            "01",
            "1999",
            "梦之旅专辑：标准格式"
        ));

        // 毛不易专辑（标准格式）
        cases.add(new TestCase(
            "W:\\M - 毛不易\\毛不易 - 2018 - 平凡的一天 - WAV\\01.wav",
            "毛不易",
            "平凡的一天",
            null,
            "01",
            "2018",
            "毛不易专辑：标准格式"
        ));

        // 毛阿敏专辑（标准格式）
        cases.add(new TestCase(
            "W:\\M - 毛阿敏\\毛阿敏 - 1999 - 爱情 - WAV\\01.wav",
            "毛阿敏",
            "爱情",
            null,
            "01",
            "1999",
            "毛阿敏专辑：标准格式"
        ));

        // 莫文蔚专辑（标准格式）
        cases.add(new TestCase(
            "W:\\M - 莫文蔚\\莫文蔚 - 1999 - You Can - WAV\\01.wav",
            "莫文蔚",
            "You Can",
            null,
            "01",
            "1999",
            "莫文蔚专辑：英文专辑名"
        ));

        // 马頔专辑（标准格式）
        cases.add(new TestCase(
            "W:\\M - 马頔\\马頔 - 1999 - 爱情 - WAV\\01.wav",
            "马頔",
            "爱情",
            null,
            "01",
            "1999",
            "马頔专辑：标准格式"
        ));

        // 那英专辑（标准格式）
        cases.add(new TestCase(
            "W:\\N - 那英\\那英 - 1999 - 爱情 - WAV\\01.wav",
            "那英",
            "爱情",
            null,
            "01",
            "1999",
            "那英专辑：标准格式"
        ));

        // 南北组合专辑（标准格式）
        cases.add(new TestCase(
            "W:\\N - 南北组合\\南北组合 - 1999 - 爱情 - WAV\\01.wav",
            "南北组合",
            "爱情",
            null,
            "01",
            "1999",
            "南北组合专辑：标准格式"
        ));

        // 女子十二乐坊专辑（标准格式）
        cases.add(new TestCase(
            "W:\\N - 女子十二乐坊\\女子十二乐坊 - 1999 - 爱情 - WAV\\01.wav",
            "女子十二乐坊",
            "爱情",
            null,
            "01",
            "1999",
            "女子十二乐坊专辑：标准格式"
        ));

        // 朴树专辑（标准格式）
        cases.add(new TestCase(
            "W:\\P - 朴树\\朴树 - 1999 - 爱情 - WAV\\01.wav",
            "朴树",
            "爱情",
            null,
            "01",
            "1999",
            "朴树专辑：标准格式"
        ));

        // 潘玮柏专辑（标准格式）
        cases.add(new TestCase(
            "W:\\P - 潘玮柏\\潘玮柏 - 1999 - 爱情 - WAV\\01.wav",
            "潘玮柏",
            "爱情",
            null,
            "01",
            "1999",
            "潘玮柏专辑：标准格式"
        ));

        // 潘美辰专辑（标准格式）
        cases.add(new TestCase(
            "W:\\P - 潘美辰\\潘美辰 - 1999 - 爱情 - WAV\\01.wav",
            "潘美辰",
            "爱情",
            null,
            "01",
            "1999",
            "潘美辰专辑：标准格式"
        ));

        // 区瑞强专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Q - 区瑞强\\区瑞强 - 1999 - 爱情 - WAV\\01.wav",
            "区瑞强",
            "爱情",
            null,
            "01",
            "1999",
            "区瑞强专辑：标准格式"
        ));

        // 裘海正专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Q - 裘海正\\裘海正 - 1999 - 爱情 - WAV\\01.wav",
            "裘海正",
            "爱情",
            null,
            "01",
            "1999",
            "裘海正专辑：标准格式"
        ));

        // 邝美云专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Q - 邝美云\\邝美云 - 1999 - 爱情 - WAV\\01.wav",
            "邝美云",
            "爱情",
            null,
            "01",
            "1999",
            "邝美云专辑：标准格式"
        ));

        // 青山专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Q - 青山\\青山 - 1999 - 爱情 - WAV\\01.wav",
            "青山",
            "爱情",
            null,
            "01",
            "1999",
            "青山专辑：标准格式"
        ));

        // 青燕子专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Q - 青燕子\\青燕子 - 1999 - 爱情 - WAV\\01.wav",
            "青燕子",
            "爱情",
            null,
            "01",
            "1999",
            "青燕子专辑：标准格式"
        ));

        // 齐秦专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Q - 齐秦\\齐秦 - 1999 - 爱情 - WAV\\01.wav",
            "齐秦",
            "爱情",
            null,
            "01",
            "1999",
            "齐秦专辑：标准格式"
        ));

        // 齐豫专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Q - 齐豫\\齐豫 - 1997 - 齐豫 - WAV\\01.wav",
            "齐豫",
            "齐豫",
            null,
            "01",
            "1997",
            "齐豫专辑：同名专辑"
        ));

        // 千百惠专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Q - 千百惠\\千百惠 - 1994 - 姐妹 - WAV\\01.wav",
            "千百惠",
            "姐妹",
            null,
            "01",
            "1994",
            "千百惠专辑：标准格式"
        ));

        // 任贤齐专辑（标准格式）
        cases.add(new TestCase(
            "W:\\R - 任贤齐\\任贤齐 - 1993 - 心太软 - WAV\\01.wav",
            "任贤齐",
            "心太软",
            null,
            "01",
            "1993",
            "任贤齐专辑：标准格式"
        ));

        // 容祖儿专辑（标准格式）
        cases.add(new TestCase(
            "W:\\R - 容祖儿\\容祖儿 - 1999 - 不容置疑 - WAV\\01.wav",
            "容祖儿",
            "不容置疑",
            null,
            "01",
            "1999",
            "容祖儿专辑：标准格式"
        ));

        // 孙楠专辑（标准格式）
        cases.add(new TestCase(
            "W:\\S - 孙楠\\孙楠 - 1997 - 认识孙楠 - WAV\\01.wav",
            "孙楠",
            "认识孙楠",
            null,
            "01",
            "1997",
            "孙楠专辑：标准格式"
        ));

        // 孙燕姿专辑（标准格式）
        cases.add(new TestCase(
            "W:\\S - 孙燕姿\\孙燕姿 - 1998 - 天黑黑 - WAV\\01.wav",
            "孙燕姿",
            "天黑黑",
            null,
            "01",
            "1998",
            "孙燕姿专辑：标准格式"
        ));

        // 宋冬野专辑（标准格式）
        cases.add(new TestCase(
            "W:\\S - 宋冬野\\宋冬野 - 1998 - 水晶花房 - WAV\\01.wav",
            "宋冬野",
            "水晶花房",
            null,
            "01",
            "1998",
            "宋冬野专辑：标准格式"
        ));

        // 山鹰组合专辑（标准格式）
        cases.add(new TestCase(
            "W:\\S - 山鹰组合\\山鹰组合 - 1997 - 天上的西藏 - WAV\\01.wav",
            "山鹰组合",
            "天上的西藏",
            null,
            "01",
            "1997",
            "山鹰组合专辑：标准格式"
        ));

        // 施孝荣专辑（标准格式）
        cases.add(new TestCase(
            "W:\\S - 施孝荣\\施孝荣 - 1996 - 回声 - WAV\\01.wav",
            "施孝荣",
            "回声",
            null,
            "01",
            "1996",
            "施孝荣专辑：标准格式"
        ));

        // 时代乐队专辑（标准格式）
        cases.add(new TestCase(
            "W:\\S - 时代乐队\\时代乐队 - 1999 - 时代 - WAV\\01.wav",
            "时代乐队",
            "时代",
            null,
            "01",
            "1999",
            "时代乐队专辑：同名专辑"
        ));

        // 水木年华专辑（标准格式）
        cases.add(new TestCase(
            "W:\\S - 水木年华\\水木年华 - 2000 - 一生有你 - WAV\\01.wav",
            "水木年华",
            "一生有你",
            null,
            "01",
            "2000",
            "水木年华专辑：标准格式"
        ));

        // 沈丹专辑（标准格式）
        cases.add(new TestCase(
            "W:\\S - 沈丹\\沈丹 - 1999 - 爱人 - WAV\\01.wav",
            "沈丹",
            "爱人",
            null,
            "01",
            "1999",
            "沈丹专辑：标准格式"
        ));

        // 苏云专辑（标准格式）
        cases.add(new TestCase(
            "W:\\S - 苏云\\苏云 - 1999 - 原来 - WAV\\01.wav",
            "苏云",
            "原来",
            null,
            "01",
            "1999",
            "苏云专辑：标准格式"
        ));

        // 苏打绿专辑（标准格式）
        cases.add(new TestCase(
            "W:\\S - 苏打绿\\苏打绿 - 1999 - 苏打绿 - WAV\\01.wav",
            "苏打绿",
            "苏打绿",
            null,
            "01",
            "1999",
            "苏打绿专辑：同名专辑"
        ));

        // 苏有朋专辑（标准格式）
        cases.add(new TestCase(
            "W:\\S - 苏有朋\\苏有朋 - 1999 - 苏有朋 - WAV\\01.wav",
            "苏有朋",
            "苏有朋",
            null,
            "01",
            "1999",
            "苏有朋专辑：同名专辑"
        ));

        // Twins专辑（标准格式）
        cases.add(new TestCase(
            "W:\\T - Twins\\Twins - 2002 - 我们的纪念册 - WAV\\01.wav",
            "Twins",
            "我们的纪念册",
            null,
            "01",
            "2002",
            "Twins专辑：标准格式"
        ));

        // 太极乐队专辑（标准格式）
        cases.add(new TestCase(
            "W:\\T - 太极乐队\\太极乐队 - 1985 - 红色跑车 - WAV\\01.wav",
            "太极乐队",
            "红色跑车",
            null,
            "01",
            "1985",
            "太极乐队专辑：标准格式"
        ));

        // 田震专辑（标准格式）
        cases.add(new TestCase(
            "W:\\T - 田震\\田震 - 1996 - 野花 - WAV\\01.wav",
            "田震",
            "野花",
            null,
            "01",
            "1996",
            "田震专辑：标准格式"
        ));

        // 痛仰乐队专辑（标准格式）
        cases.add(new TestCase(
            "W:\\T - 痛仰乐队\\痛仰乐队 - 1999 - 不要停止我的音乐 - WAV\\01.wav",
            "痛仰乐队",
            "不要停止我的音乐",
            null,
            "01",
            "1999",
            "痛仰乐队专辑：标准格式"
        ));

        // 童丽专辑（标准格式）
        cases.add(new TestCase(
            "W:\\T - 童丽\\童丽 - 1995 - 春天 - WAV\\01.wav",
            "童丽",
            "春天",
            null,
            "01",
            "1995",
            "童丽专辑：标准格式"
        ));

        // 腾格尔专辑（标准格式）
        cases.add(new TestCase(
            "W:\\T - 腾格尔\\腾格尔 - 1993 - 蓝天 - WAV\\01.wav",
            "腾格尔",
            "蓝天",
            null,
            "01",
            "1993",
            "腾格尔专辑：标准格式"
        ));

        // 谭咏麟专辑（标准格式）
        cases.add(new TestCase(
            "W:\\T - 谭咏麟\\谭咏麟 - 1984 - 爱的根源 - WAV\\01.wav",
            "谭咏麟",
            "爱的根源",
            null,
            "01",
            "1984",
            "谭咏麟专辑：标准格式"
        ));

        // 五月天专辑（标准格式）
        cases.add(new TestCase(
            "W:\\W - 五月天\\五月天 - 1999 - 第一张创作专辑 - WAV\\01.wav",
            "五月天",
            "第一张创作专辑",
            null,
            "01",
            "1999",
            "五月天专辑：标准格式"
        ));

        // 五条人专辑（标准格式）
        cases.add(new TestCase(
            "W:\\W - 五条人\\五条人 - 1999 - 第五张 - WAV\\01.wav",
            "五条人",
            "第五张",
            null,
            "01",
            "1999",
            "五条人专辑：标准格式"
        ));

        // 伍佰专辑（标准格式）
        cases.add(new TestCase(
            "W:\\W - 伍佰\\伍佰 - 1994 - 浪人情歌 - WAV\\01.wav",
            "伍佰",
            "浪人情歌",
            null,
            "01",
            "1994",
            "伍佰专辑：标准格式"
        ));

        // 吴克群专辑（标准格式）
        cases.add(new TestCase(
            "W:\\W - 吴克群\\吴克群 - 1992 - 谁是大英雄 - WAV\\01.wav",
            "吴克群",
            "谁是大英雄",
            null,
            "01",
            "1992",
            "吴克群专辑：标准格式"
        ));

        // 巫启贤专辑（标准格式）
        cases.add(new TestCase(
            "W:\\W - 巫启贤\\巫启贤 - 1994 - 等你等到我也心碎 - WAV\\01.wav",
            "巫启贤",
            "等你等到我也心碎",
            null,
            "01",
            "1994",
            "巫启贤专辑：标准格式"
        ));

        // 文章专辑（标准格式）
        cases.add(new TestCase(
            "W:\\W - 文章\\文章 - 1995 - 强颜欢笑 - WAV\\01.wav",
            "文章",
            "强颜欢笑",
            null,
            "01",
            "1995",
            "文章专辑：标准格式"
        ));

        // 汪峰专辑（标准格式）
        cases.add(new TestCase(
            "W:\\W - 汪峰\\汪峰 - 2000 - 花火 - WAV\\01.wav",
            "汪峰",
            "花火",
            null,
            "01",
            "2000",
            "汪峰专辑：标准格式"
        ));

        // 汪明荃专辑（标准格式）
        cases.add(new TestCase(
            "W:\\W - 汪明荃\\汪明荃 - 1997 - 水晶 - WAV\\01.wav",
            "汪明荃",
            "水晶",
            null,
            "01",
            "1997",
            "汪明荃专辑：标准格式"
        ));

        // 温拿乐队专辑（标准格式）
        cases.add(new TestCase(
            "W:\\W - 温拿乐队\\温拿乐队 - 1998 - 温拿乐队 - WAV\\01.wav",
            "温拿乐队",
            "温拿乐队",
            null,
            "01",
            "1998",
            "温拿乐队专辑：同名专辑"
        ));

        // 温碧霞专辑（标准格式）
        cases.add(new TestCase(
            "W:\\W - 温碧霞\\温碧霞 - 1999 - 霞光 - WAV\\01.wav",
            "温碧霞",
            "霞光",
            null,
            "01",
            "1999",
            "温碧霞专辑：标准格式"
        ));

        // 王力宏专辑（标准格式）
        cases.add(new TestCase(
            "W:\\W - 王力宏\\王力宏 - 1998 - 公转自转 - WAV\\01.wav",
            "王力宏",
            "公转自转",
            null,
            "01",
            "1998",
            "王力宏专辑：标准格式"
        ));

        // 王心凌专辑（标准格式）
        cases.add(new TestCase(
            "W:\\W - 王心凌\\王心凌 - 1999 - 爱我 - WAV\\01.wav",
            "王心凌",
            "爱我",
            null,
            "01",
            "1999",
            "王心凌专辑：标准格式"
        ));

        // 王杰专辑（标准格式）
        cases.add(new TestCase(
            "W:\\W - 王杰\\王杰 - 1987 - 谁明浪子心 - WAV\\01.wav",
            "王杰",
            "谁明浪子心",
            null,
            "01",
            "1987",
            "王杰专辑：标准格式"
        ));

        // 王若琳专辑（标准格式）
        cases.add(new TestCase(
            "W:\\W - 王若琳\\王若琳 - 1996 - 谁让我流泪 - WAV\\01.wav",
            "王若琳",
            "谁让我流泪",
            null,
            "01",
            "1996",
            "王若琳专辑：标准格式"
        ));

        // 王菲专辑（标准格式）
        cases.add(new TestCase(
            "W:\\W - 王菲\\王菲 - 1994 - 迷 - WAV\\01.wav",
            "王菲",
            "迷",
            null,
            "01",
            "1994",
            "王菲专辑：标准格式"
        ));

        // 王铮亮专辑（标准格式）
        cases.add(new TestCase(
            "W:\\W - 王铮亮\\王铮亮 - 1999 - 三思 - WAV\\01.wav",
            "王铮亮",
            "三思",
            null,
            "01",
            "1999",
            "王铮亮专辑：标准格式"
        ));

        // 魏如萱专辑（标准格式）
        cases.add(new TestCase(
            "W:\\W - 魏如萱\\魏如萱 - 2000 - 天使 - WAV\\01.wav",
            "魏如萱",
            "天使",
            null,
            "01",
            "2000",
            "魏如萱专辑：标准格式"
        ));

        // 萧敬腾专辑（标准格式）
        cases.add(new TestCase(
            "W:\\X - 萧敬腾\\萧敬腾 - 1999 - 爱的代价 - WAV\\01.wav",
            "萧敬腾",
            "爱的代价",
            null,
            "01",
            "1999",
            "萧敬腾专辑：标准格式"
        ));

        // 萧煌奇专辑（标准格式）
        cases.add(new TestCase(
            "W:\\X - 萧煌奇\\萧煌奇 - 1998 - 爱情鸟 - WAV\\01.wav",
            "萧煌奇",
            "爱情鸟",
            null,
            "01",
            "1998",
            "萧煌奇专辑：标准格式"
        ));

        // 薛之谦专辑（标准格式）
        cases.add(new TestCase(
            "W:\\X - 薛之谦\\薛之谦 - 2016 - 初学者 - WAV\\01.wav",
            "薛之谦",
            "初学者",
            null,
            "01",
            "2016",
            "薛之谦专辑：标准格式"
        ));

        // 许冠杰专辑（标准格式）
        cases.add(new TestCase(
            "W:\\X - 许冠杰\\许冠杰 - 1997 - 认识你真好 - WAV\\01.wav",
            "许冠杰",
            "认识你真好",
            null,
            "01",
            "1997",
            "许冠杰专辑：标准格式"
        ));

        // 许志安专辑（标准格式）
        cases.add(new TestCase(
            "W:\\X - 许志安\\许志安 - 1999 - 爱情 - WAV\\01.wav",
            "许志安",
            "爱情",
            null,
            "01",
            "1999",
            "许志安专辑：标准格式"
        ));

        // 许美静专辑（标准格式）
        cases.add(new TestCase(
            "W:\\X - 许美静\\许美静 - 2000 - 静听 - WAV\\01.wav",
            "许美静",
            "静听",
            null,
            "01",
            "2000",
            "许美静专辑：标准格式"
        ));

        // 许茹芸专辑（标准格式）
        cases.add(new TestCase(
            "W:\\X - 许茹芸\\许茹芸 - 2001 - 只是一个人 - WAV\\01.wav",
            "许茹芸",
            "只是一个人",
            null,
            "01",
            "2001",
            "许茹芸专辑：标准格式"
        ));

        // 雪儿专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Y - 雪儿\\雪儿 - 1999 - 懂事 - WAV\\01.wav",
            "雪儿",
            "懂事",
            null,
            "01",
            "1999",
            "雪儿专辑：标准格式"
        ));

        // 杨钰莹专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Y - 杨钰莹\\杨钰莹 - 1998 - 雪花飘蝶 - WAV\\01.wav",
            "杨钰莹",
            "雪花飘蝶",
            null,
            "01",
            "1998",
            "杨钰莹专辑：标准格式"
        ));

        // 杨雪霏专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Y - 杨雪霏\\杨雪霏 - 2000 - 天空 - WAV\\01.wav",
            "杨雪霏",
            "天空",
            null,
            "01",
            "2000",
            "杨雪霏专辑：标准格式"
        ));

        // 泳儿专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Y - 泳儿\\泳儿 - 1999 - 泳儿 - WAV\\01.wav",
            "泳儿",
            "泳儿",
            null,
            "01",
            "1999",
            "泳儿专辑：同名专辑"
        ));

        // 庾澄庆专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Y - 庾澄庆\\庾澄庆 - 1998 - 爱情 - WAV\\01.wav",
            "庾澄庆",
            "爱情",
            null,
            "01",
            "1998",
            "庾澄庆专辑：标准格式"
        ));

        // 央金兰泽专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Y - 央金兰泽\\央金兰泽 - 1999 - 走天涯 - WAV\\01.wav",
            "央金兰泽",
            "走天涯",
            null,
            "01",
            "1999",
            "央金兰泽专辑：标准格式"
        ));

        // 央金卓玛专辑（标准格式）
        cases.add(new TestCase(
            "W:\\Y - 央金卓玛\\央金卓玛 - 1999 - 走天涯 - WAV\\01.wav",
            "央金卓玛",
            "走天涯",
            null,
            "01",
            "1999",
            "央金卓玛专辑：标准格式"
        ));

        return cases;
    }

    /**
     * 测试元数据提取功能
     */
    @Test
    public void testExtendedMetadataExtraction() {
        List<TestCase> testCases = getExtendedTestCases();
        List<TestResult> results = new ArrayList<>();

        int totalArtistMatches = 0;
        int totalAlbumMatches = 0;
        int totalTitleMatches = 0;
        int totalTrackMatches = 0;
        int totalYearMatches = 0;
        int totalExpectedFields = 0;

        for (TestCase testCase : testCases) {
            File file = new File(testCase.getFilePath());
            MetadataHelper.AudioMeta meta = MetadataHelper.extractFromFileSystem(file);

            TestResult result = new TestResult(testCase, meta);
            results.add(result);

            if (result.isArtistMatch()) totalArtistMatches++;
            if (result.isAlbumMatch()) totalAlbumMatches++;
            if (result.isTitleMatch()) totalTitleMatches++;
            if (result.isTrackMatch()) totalTrackMatches++;
            if (result.isYearMatch()) totalYearMatches++;

            totalExpectedFields += result.getMaxScore();

            if (result.getAccuracy() < 100) {
                System.out.println("测试用例: " + testCase.getDescription());
                System.out.println("文件路径: " + testCase.getFilePath());
                System.out.println("预期结果:");
                System.out.println("  艺术家: " + testCase.getExpectedArtist());
                System.out.println("  专辑: " + testCase.getExpectedAlbum());
                System.out.println("  标题: " + testCase.getExpectedTitle());
                System.out.println("  曲目: " + testCase.getExpectedTrack());
                System.out.println("  年份: " + testCase.getExpectedYear());
                System.out.println("实际结果:");
                System.out.println("  艺术家: " + meta.getArtist());
                System.out.println("  专辑: " + meta.getAlbum());
                System.out.println("  标题: " + meta.getTitle());
                System.out.println("  曲目: " + meta.getTrack());
                System.out.println("  年份: " + meta.getYear());
                System.out.println("匹配情况:");
                System.out.println("  艺术家: " + (result.isArtistMatch() ? "?" : "X"));
                System.out.println("  专辑: " + (result.isAlbumMatch() ? "?" : "X"));
                System.out.println("  标题: " + (result.isTitleMatch() ? "?" : "X"));
                System.out.println("  曲目: " + (result.isTrackMatch() ? "?" : "X"));
                System.out.println("  年份: " + (result.isYearMatch() ? "?" : "X"));
                System.out.println("准确率: " + String.format("%.1f%%", result.getAccuracy()));
                System.out.println();
            }
        }

        System.out.println("=====================================");
        System.out.println("扩展测试报告");
        System.out.println("=====================================");
        System.out.println("总测试用例数: " + testCases.size());
        System.out.println("艺术家匹配: " + totalArtistMatches + "/" + testCases.size() + " (" + String.format("%.1f%%", (double) totalArtistMatches / testCases.size() * 100) + ")");
        System.out.println("专辑匹配: " + totalAlbumMatches + "/" + testCases.size() + " (" + String.format("%.1f%%", (double) totalAlbumMatches / testCases.size() * 100) + ")");
        System.out.println("标题匹配: " + totalTitleMatches + "/" + testCases.size() + " (" + String.format("%.1f%%", (double) totalTitleMatches / testCases.size() * 100) + ")");
        System.out.println("曲目匹配: " + totalTrackMatches + "/" + testCases.size() + " (" + String.format("%.1f%%", (double) totalTrackMatches / testCases.size() * 100) + ")");
        System.out.println("年份匹配: " + totalYearMatches + "/" + testCases.size() + " (" + String.format("%.1f%%", (double) totalYearMatches / testCases.size() * 100) + ")");

        int totalScore = totalArtistMatches + totalAlbumMatches + totalTitleMatches + totalTrackMatches + totalYearMatches;
        double averageAccuracy = (double) totalScore / totalExpectedFields * 100;
        System.out.println("平均准确率: " + String.format("%.1f%%", averageAccuracy));

        List<TestResult> failedResults = new ArrayList<>();
        for (TestResult result : results) {
            if (result.getAccuracy() < 100) {
                failedResults.add(result);
            }
        }

        if (!failedResults.isEmpty()) {
            System.out.println();
            System.out.println("=====================================");
            System.out.println("失败的测试用例");
            System.out.println("=====================================");
            for (TestResult result : failedResults) {
                System.out.println(result.getTestCase().getDescription());
                System.out.println("文件路径: " + result.getTestCase().getFilePath());
                System.out.println("准确率: " + String.format("%.1f%%", result.getAccuracy()));
                if (!result.isArtistMatch()) {
                    System.out.println("  艺术家不匹配: 预期='" + result.getTestCase().getExpectedArtist() + "', 实际='" + result.getActualMeta().getArtist() + "'");
                }
                if (!result.isAlbumMatch()) {
                    System.out.println("  专辑不匹配: 预期='" + result.getTestCase().getExpectedAlbum() + "', 实际='" + result.getActualMeta().getAlbum() + "'");
                }
                if (!result.isTitleMatch()) {
                    System.out.println("  标题不匹配: 预期='" + result.getTestCase().getExpectedTitle() + "', 实际='" + result.getActualMeta().getTitle() + "'");
                }
                if (!result.isTrackMatch()) {
                    System.out.println("  曲目不匹配: 预期='" + result.getTestCase().getExpectedTrack() + "', 实际='" + result.getActualMeta().getTrack() + "'");
                }
                if (!result.isYearMatch()) {
                    System.out.println("  年份不匹配: 预期='" + result.getTestCase().getExpectedYear() + "', 实际='" + result.getActualMeta().getYear() + "'");
                }
                System.out.println();
            }
        }

        System.out.println("=====================================");
        System.out.println("扩展元数据提取测试完成");
        System.out.println("=====================================");
    }
}

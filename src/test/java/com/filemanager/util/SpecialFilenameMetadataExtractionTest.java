package com.filemanager.util;

import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 特殊文件名格式元数据提取测试用例
 * 测试各种特殊文件名格式的元数据提取功能
 * 包括多艺术家、特殊字符、混合语言等
 */
public class SpecialFilenameMetadataExtractionTest {

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
     * 获取特殊文件名格式测试用例
     */
    private static List<TestCase> getSpecialFilenameTestCases() {
        List<TestCase> cases = new ArrayList<>();

        // 多艺术家合作歌曲
        cases.add(new TestCase(
            "W:\\陶喆 蔡依林 - 今天你要嫁给我.flac",
            "陶喆, 蔡依林",
            null,
            "今天你要嫁给我",
            null,
            null,
            "多艺术家合作：陶喆 蔡依林"
        ));

        cases.add(new TestCase(
            "W:\\周杰伦 陈奕迅 - 简单爱.flac",
            "周杰伦, 陈奕迅",
            null,
            "简单爱",
            null,
            null,
            "多艺术家合作：周杰伦 陈奕迅"
        ));

        cases.add(new TestCase(
            "W:\\王菲 那英 - 明年今日.flac",
            "王菲, 那英",
            null,
            "明年今日",
            null,
            null,
            "多艺术家合作：王菲 那英"
        ));

        // 特殊字符和符号
        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\α·Pav - η.m4a",
            "α·Pav",
            "我的喜欢",
            "η",
            null,
            null,
            "特殊字符：希腊字母"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\姫神 - 千年の祈り.m4a",
            "姫神",
            "我的喜欢",
            "千年の祈り",
            null,
            null,
            "特殊字符：日文假名"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\天门 - 桜花抄.flac",
            "天门",
            "我的喜欢",
            "桜花抄",
            null,
            null,
            "特殊字符：日文汉字"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\天门 - 雪の駅.flac",
            "天门",
            "我的喜欢",
            "雪の駅",
            null,
            null,
            "特殊字符：日文假名"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\昙轩 - 海の形.flac",
            "昙轩",
            "我的喜欢",
            "海の形",
            null,
            null,
            "特殊字符：日文假名"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\别野加奈 - キネマ.lrc",
            "别野加奈",
            "我的喜欢",
            "キネマ",
            null,
            null,
            "特殊字符：日文片假名"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\関取花 - はじめ.flac",
            "関取花",
            "我的喜欢",
            "はじめ",
            null,
            null,
            "特殊字符：日文片假名"
        ));

        cases.add(new TestCase(
            "X:\\0 - 我的喜欢\\沙皮 - ☾.flac",
            "沙皮",
            "我的喜欢",
            "☾",
            null,
            null,
            "特殊字符：Unicode符号"
        ));

        // 数字开头的艺术家名
        cases.add(new TestCase(
            "W:\\7opy - 晚风.flac",
            "7opy",
            null,
            "晚风",
            null,
            null,
            "数字开头的艺术家名"
        ));

        cases.add(new TestCase(
            "W:\\朱彦安 - 20.flac",
            "朱彦安",
            null,
            "20",
            null,
            null,
            "数字作为歌曲名"
        ));

        // 标点符号和特殊分隔符
        cases.add(new TestCase(
            "W:\\嘿！！！ - 房子.mp3",
            "嘿！！！",
            null,
            "房子",
            null,
            null,
            "多个感叹号"
        ));

        cases.add(new TestCase(
            "W:\\告五人 - WEWE.lrc",
            "告五人",
            null,
            "WEWE",
            null,
            null,
            "英文歌名"
        ));

        cases.add(new TestCase(
            "W:\\告五人 - 新世界.flac",
            "告五人",
            null,
            "新世界",
            null,
            null,
            "中文歌名"
        ));

        // 包含年份的文件名
        cases.add(new TestCase(
            "W:\\周杰伦 - 2012 - 乌克丽丽.flac",
            "周杰伦",
            null,
            "2012 - 乌克丽丽",
            null,
            null,
            "包含年份的文件名"
        ));

        cases.add(new TestCase(
            "W:\\陈奕迅 - 2013 - 最冷一天.flac",
            "陈奕迅",
            null,
            "2013 - 最冷一天",
            null,
            null,
            "包含年份的文件名"
        ));

        // 包含专辑信息的文件名
        cases.add(new TestCase(
            "W:\\五月天 - 知足.flac",
            "五月天",
            null,
            "知足",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\周深 - 光亮.flac",
            "周深",
            null,
            "光亮",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\井胧 - 丢了你.flac",
            "井胧",
            null,
            "丢了你",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\井胧 - 骁.flac",
            "井胧",
            null,
            "骁",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\亚东 - 卓玛.mp3",
            "亚东",
            null,
            "卓玛",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\任素汐 - 胡广生.lrc",
            "任素汐",
            null,
            "胡广生",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\何柏诚 - 彩虹糖.flac",
            "何柏诚",
            null,
            "彩虹糖",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\凹与山 - 理查.flac",
            "凹与山",
            null,
            "理查",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\刘森 - 深海.flac",
            "刘森",
            null,
            "深海",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\刘珂矣 - 半壶纱.flac",
            "刘珂矣",
            null,
            "半壶纱",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\刘若英 - 为爱痴狂.lrc",
            "刘若英",
            null,
            "为爱痴狂",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\华晨宇 - 肆无惧燥.lrc",
            "华晨宇",
            null,
            "肆无惧燥",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\卢冠廷 - 一生所爱.lrc",
            "卢冠廷",
            null,
            "一生所爱",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\厨子和戏子 - 春河.lrc",
            "厨子和戏子",
            null,
            "春河",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\反光镜 - 还我蔚蓝.lrc",
            "反光镜",
            null,
            "还我蔚蓝",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\吴青峰 - 起风了.lrc",
            "吴青峰",
            null,
            "起风了",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W\\:告五人 - 夜里无星.lrc",
            "告五人",
            null,
            "夜里无星",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\告五人 - 红.flac",
            "告五人",
            null,
            "红",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\周柏豪 - 小白.flac",
            "周柏豪",
            null,
            "小白",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\周迅 - 外面.flac",
            "周迅",
            null,
            "外面",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\哪吒乐队 - 闹海.mp3",
            "哪吒乐队",
            null,
            "闹海",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\唐笑 - 淡水海边.lrc",
            "唐笑",
            null,
            "淡水海边",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\回春丹 - 初恋.flac",
            "回春丹",
            null,
            "初恋",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\回春丹 - 艾蜜莉.flac",
            "回春丹",
            null,
            "艾蜜莉",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\因果 - 花火大会.lrc",
            "因果",
            null,
            "花火大会",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\声无哀乐 - 飞升.lrc",
            "声无哀乐",
            null,
            "飞升",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\大籽 - 放空.flac",
            "大籽",
            null,
            "放空",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\姜云升 - 初.flac",
            "姜云升",
            null,
            "初",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\姜云升 - 淹没.flac",
            "姜云升",
            null,
            "淹没",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\孙燕姿 - 遇见.flac",
            "孙燕姿",
            null,
            "遇见",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\安雯 - 月满西楼.flac",
            "安雯",
            null,
            "月满西楼",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\宋冬野 - 安和桥.lrc",
            "宋冬野",
            null,
            "安和桥",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\小霞 - 向云端.flac",
            "小霞",
            null,
            "向云端",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\岛屿心情 - 声音.mp3",
            "岛屿心情",
            null,
            "声音",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\常安 - 梅花三弄.flac",
            "常安",
            null,
            "梅花三弄",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\常石磊 - 遇见你.flac",
            "常石磊",
            null,
            "遇见你",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\廖佳琳 - 降临.flac",
            "廖佳琳",
            null,
            "降临",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\张军 - 霓裳羽衣.mp3",
            "张军",
            null,
            "霓裳羽衣",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\张曦匀 - 伯虎说.lrc",
            "张曦匀",
            null,
            "伯虎说",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\张杰 - 给力青春.mp3",
            "张杰",
            null,
            "给力青春",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\张渠 - 柘枝舞.flac",
            "张渠",
            null,
            "柘枝舞",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\张良成 - 风浪里.lrc",
            "张良成",
            null,
            "风浪里",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\很美味 - 假面舞会.lrc",
            "很美味",
            null,
            "假面舞会",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\徐小凤 - 心恋.flac",
            "徐小凤",
            null,
            "心恋",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\徐秉龙 - 孤身.flac",
            "徐秉龙",
            null,
            "孤身",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\徐秉龙 - 白羊.flac",
            "徐秉龙",
            null,
            "白羊",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\戴佩妮 - 怎样.flac",
            "戴佩妮",
            null,
            "怎样",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\旺福 - 姊妹仔.flac",
            "旺福",
            null,
            "姊妹仔",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\曾轶可 - 胆小鬼.flac",
            "曾轶可",
            null,
            "胆小鬼",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\朴树 - 清白之年.lrc",
            "朴树",
            null,
            "清白之年",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\朴树 - 生如夏花.lrc",
            "朴树",
            null,
            "生如夏花",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\杉和 - 子莫格尼.lrc",
            "杉和",
            null,
            "子莫格尼",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\李云霄 - 月中仙.flac",
            "李云霄",
            null,
            "月中仙",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\李健 - 心升明月.flac",
            "李健",
            null,
            "心升明月",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\李健 - 陀螺.flac",
            "李健",
            null,
            "陀螺",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\李宗盛 - 山丘.flac",
            "李宗盛",
            null,
            "山丘",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\李杰 - 家园.flac",
            "李杰",
            null,
            "家园",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\李荣浩 - 乌梅子酱.lrc",
            "李荣浩",
            null,
            "乌梅子酱",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\杨千嬅 - 野孩子.lrc",
            "杨千嬅",
            null,
            "野孩子",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\杨宗纬 - 最爱.mp3",
            "杨宗纬",
            null,
            "最爱",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\杭天琪 - 唱脸谱.flac",
            "杭天琪",
            null,
            "唱脸谱",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\林俊杰 - 小酒窝.flac",
            "林俊杰",
            null,
            "小酒窝",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\林俊杰 - 当你.flac",
            "林俊杰",
            null,
            "当你",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\林忆莲 - 远走高飞.lrc",
            "林忆莲",
            null,
            "远走高飞",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\柳爽 - 我的解放西.lrc",
            "柳爽",
            null,
            "我的解放西",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\柳爽 - 漠河舞厅.flac",
            "柳爽",
            null,
            "漠河舞厅",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\柳爽 - 莫妮卡.flac",
            "柳爽",
            null,
            "莫妮卡",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\格格 - 火苗.flac",
            "格格",
            null,
            "火苗",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\梁兆基 - 求神.mp3",
            "梁兆基",
            null,
            "求神",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\梁博 - 日落大道.lrc",
            "梁博",
            null,
            "日落大道",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\梁翘柏 - 南国之舞.lrc",
            "梁翘柏",
            null,
            "南国之舞",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\梦然 - 是你.flac",
            "梦然",
            null,
            "是你",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\棱镜乐队 - 克林.flac",
            "棱镜乐队",
            null,
            "克林",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\棱镜乐队 - 岛屿.flac",
            "棱镜乐队",
            null,
            "岛屿",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\棱镜乐队 - 心动.flac",
            "棱镜乐队",
            null,
            "心动",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\棱镜乐队 - 成长.flac",
            "棱镜乐队",
            null,
            "成长",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\棱镜乐队 - 摇晃.flac",
            "棱镜乐队",
            null,
            "摇晃",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\棱镜乐队 - 言语.flac",
            "棱镜乐队",
            null,
            "言语",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\歌之初乐队 - 哪有.lrc",
            "歌之初乐队",
            null,
            "哪有",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\段奥娟 - 凡人.flac",
            "段奥娟",
            null,
            "凡人",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\毛不易 - 盛夏.flac",
            "毛不易",
            null,
            "盛夏",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\汉堡黄 - 冷空气.flac",
            "汉堡黄",
            null,
            "冷空气",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\汉堡黄 - 滥俗的歌.lrc",
            "汉堡黄",
            null,
            "滥俗的歌",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\洛仃洋 - 一亩花田.lrc",
            "洛仃洋",
            null,
            "一亩花田",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\洪启 - 我想，我想.lrc",
            "洪启",
            null,
            "我想，我想",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\洪尘 - 万里.flac",
            "洪尘",
            null,
            "万里",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\海朋森 - 草莓.flac",
            "海朋森",
            null,
            "草莓",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\游助 - 一笑悬命.lrc",
            "游助",
            null,
            "一笑悬命",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\满江 - 阳光下.flac",
            "满江",
            null,
            "阳光下",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\灰澈 - 星茶会.flac",
            "灰澈",
            null,
            "星茶会",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\犬儒乐队 - 皮囊.lrc",
            "犬儒乐队",
            null,
            "皮囊",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\王加一 - 冬.flac",
            "王加一",
            null,
            "冬",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\王心凌 - 爱你.flac",
            "王心凌",
            null,
            "爱你",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\王若琳 - 苹果花.lrc",
            "王若琳",
            null,
            "苹果花",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\王贰浪 - 像鱼.flac",
            "王贰浪",
            null,
            "像鱼",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\痛仰乐队 - 西湖.lrc",
            "痛仰乐队",
            null,
            "西湖",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\皇后皮箱 - 神仙赋.lrc",
            "皇后皮箱",
            null,
            "神仙赋",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\程响 - 可能.flac",
            "程响",
            null,
            "可能",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\程璧 - 山之茶.flac",
            "程璧",
            null,
            "山之茶",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\程璧 - 给少年的歌.m4a",
            "程璧",
            null,
            "给少年的歌",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\米津玄师 - 春雷.flac",
            "米津玄师",
            null,
            "春雷",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\纵贯线 - 公路.flac",
            "纵贯线",
            null,
            "公路",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\羽果 - 怒马.flac",
            "羽果",
            null,
            "怒马",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\老狼 - 恋恋风尘.lrc",
            "老狼",
            null,
            "恋恋风尘",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\胡夏 - 那些年.flac",
            "胡夏",
            null,
            "那些年",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\胥睿 - 贴贴.flac",
            "胥睿",
            null,
            "贴贴",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\腾格尔 - 可能否.lrc",
            "腾格尔",
            null,
            "能否",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\茄子蛋 - 浪子回头.lrc",
            "茄子蛋",
            null,
            "浪子回头",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\莫文蔚 - 境外.flac",
            "莫文蔚",
            null,
            "境外",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\莫文蔚 - 广岛之恋.lrc",
            "莫文蔚",
            null,
            "广岛之恋",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\莫文蔚 - 忽然之间.lrc",
            "莫文蔚",
            null,
            "忽然之间",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\莫文蔚 - 手.flac",
            "莫文蔚",
            null,
            "手",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\莫文蔚 - 真的吗.lrc",
            "莫文蔚",
            null,
            "真的吗",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\莫文蔚 - 自洽.flac",
            "莫文蔚",
            null,
            "自洽",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\葛东琪 - 悬溺.flac",
            "葛东琪",
            null,
            "悬溺",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\蔡琴 - 渡口.flac",
            "蔡琴",
            null,
            "渡口",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\蔡琴 - 缺口.flac",
            "蔡琴",
            null,
            "缺口",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\蔡青年 - 阿苏拉则.lrc",
            "蔡青年",
            null,
            "阿苏拉则",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\藤井风 - きらり.lrc",
            "藤井风",
            null,
            "きらり",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\蜡笔小心 - MOM.lrc",
            "蜡笔小心",
            null,
            "MOM",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\裁缝铺 - 东海老人.lrc",
            "裁缝铺",
            null,
            "东海老人",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\许嵩 - 如果当时.lrc",
            "许嵩",
            null,
            "如果当时",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\许嵩 - 我乐意.flac",
            "许嵩",
            null,
            "我乐意",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\许嵩 - 留香.flac",
            "许嵩",
            null,
            "留香",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\许嵩 - 素颜.flac",
            "许嵩",
            null,
            "素颜",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\谢津 - 说唱脸谱.lrc",
            "谢津",
            null,
            "说唱脸谱",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\谭咏麟 - 朋友.flac",
            "谭咏麟",
            null,
            "朋友",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\赵雷 - 成都.flac",
            "赵雷",
            null,
            "成都",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\赵雷 - 阿刁.flac",
            "赵雷",
            null,
            "阿刁",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\逆时针向 - 晚星.lrc",
            "逆时针向",
            null,
            "晚星",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\郝云 - 去大理.flac",
            "郝云",
            null,
            "去大理",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\郭斯 - 小神仙.flac",
            "郭斯",
            null,
            "小神仙",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\郭顶 - 水星记.lrc",
            "郭顶",
            null,
            "水星记",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\队长 - 予你.flac",
            "队长",
            null,
            "予你",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\陈奕迅 - 与你常在.lrc",
            "陈奕迅",
            null,
            "与你常在",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\陈奕迅 - 因为爱情.lrc",
            "陈奕迅",
            null,
            "因为爱情",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\陈奕迅 - 娱乐天空.lrc",
            "陈奕迅",
            null,
            "娱乐天空",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\陈奕迅 - 我们.flac",
            "陈奕迅",
            null,
            "我们",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\陈婧霏 - 人间指南.lrc",
            "陈婧霏",
            null,
            "人间指南",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\陈婧霏 - 今晚.flac",
            "陈婧霏",
            null,
            "今晚",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\陈婧霏 - 夏宫.flac",
            "陈婧霏",
            null,
            "夏宫",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\陈婧霏 - 如梦.flac",
            "陈婧霏",
            null,
            "如梦",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\陈婧霏 - 深蓝.flac",
            "陈婧霏",
            null,
            "深蓝",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\陈婧霏 - 积极向下.lrc",
            "陈婧霏",
            null,
            "积极向下",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\陈慧娴 - 千千阕歌.lrc",
            "陈慧娴",
            null,
            "千千阕歌",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\陈慧娴 - 飘雪.flac",
            "陈慧娴",
            null,
            "飘雪",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\陈粒 - 易燃易爆炸.lrc",
            "陈粒",
            null,
            "易燃易爆炸",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\陈粒 - 芳草地.flac",
            "陈粒",
            null,
            "芳草地",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\陈艺鹏 - 声声慢.flac",
            "陈艺鹏",
            null,
            "声声慢",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\陈鸿宇 - 步履不停.lrc",
            "陈鸿宇",
            null,
            "步履不停",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\陶喆 - 黑色柳丁.lrc",
            "陶喆",
            null,
            "黑色柳丁",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\隔壁团乐队 - 路.flac",
            "隔壁团乐队",
            null,
            "路",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\马圣龙 - 东海渔歌.lrc",
            "马圣龙",
            null,
            "东海渔歌",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\马良 - 醒着醉.flac",
            "马良",
            null,
            "醒着醉",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\马赛克 - 与我共舞.lrc",
            "马赛克",
            null,
            "与我共舞",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\马赛克 - 霓虹甜心.lrc",
            "马赛克",
            null,
            "霓虹甜心",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\高小阳 - 心动.flac",
            "高小阳",
            null,
            "心动",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\麻枝准 - 渚.flac",
            "麻枝准",
            null,
            "渚",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\黄楚桐 - 梧桐.flac",
            "黄楚桐",
            null,
            "梧桐",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\黄莺莺 - 葬心.flac",
            "黄莺莺",
            null,
            "葬心",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\黄雅莉 - 蝴蝶泉边.lrc",
            "黄雅莉",
            null,
            "蝴蝶泉边",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        cases.add(new TestCase(
            "W:\\齐豫 - 船歌.flac",
            "齐豫",
            null,
            "船歌",
            null,
            null,
            "标准格式：艺术家 - 歌名"
        ));

        return cases;
    }

    /**
     * 测试元数据提取功能
     */
    @Test
    public void testSpecialFilenameMetadataExtraction() {
        List<TestCase> testCases = getSpecialFilenameTestCases();
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
                System.out.println("  艺术家: " + (result.isArtistMatch() ? "✓" : "✗"));
                System.out.println("  专辑: " + (result.isAlbumMatch() ? "✓" : "✗"));
                System.out.println("  标题: " + (result.isTitleMatch() ? "✓" : "✗"));
                System.out.println("  曲目: " + (result.isTrackMatch() ? "✓" : "✗"));
                System.out.println("  年份: " + (result.isYearMatch() ? "✓" : "✗"));
                System.out.println("准确率: " + String.format("%.1f%%", result.getAccuracy()));
                System.out.println();
            }
        }

        System.out.println("=====================================");
        System.out.println("特殊文件名格式测试报告");
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
        System.out.println("特殊文件名格式元数据提取测试完成");
        System.out.println("=====================================");
    }
}

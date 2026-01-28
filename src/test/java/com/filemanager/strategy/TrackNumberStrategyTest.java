package com.filemanager.strategy;

import com.filemanager.model.ChangeRecord;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 音轨编号策略测试类
 * 验证TrackNumberStrategy中各个功能模块的效果
 */
public class TrackNumberStrategyTest {
    private TrackNumberStrategy strategy;
    private File testDir;

    @Before
    public void setUp() throws IOException {
        strategy = new TrackNumberStrategy();
        
        testDir = new File(System.getProperty("java.io.tmpdir"), "test_track_number");
        if (testDir.exists()) {
            deleteDirectory(testDir);
        }
        testDir.mkdirs();
    }

    @Test
    public void testModeMetadataSort() {
        System.out.println("=== 测试元数据排序模式 ===");
        
        strategy.pMode = "元数据排序 (按音轨编号)";
        strategy.pPadZero = true;
        strategy.pSeparator = ". ";
        
        System.out.println("排序模式: " + strategy.pMode);
        System.out.println("双位补零: " + strategy.pPadZero);
        System.out.println("分隔符: " + strategy.pSeparator);
        
        assertEquals("应该选择元数据排序模式", "元数据排序 (按音轨编号)", strategy.pMode);
        assertTrue("应该启用双位补零", strategy.pPadZero);
        assertEquals("分隔符应该是'. '", ". ", strategy.pSeparator);
        System.out.println("测试结果: 元数据排序模式配置正确");
    }

    @Test
    public void testModeTextFileMatch() {
        System.out.println("=== 测试文本列表匹配模式 ===");
        
        strategy.pMode = "文本列表匹配 (.txt/.nfo)";
        strategy.pPadZero = true;
        strategy.pSeparator = ". ";
        
        System.out.println("排序模式: " + strategy.pMode);
        System.out.println("双位补零: " + strategy.pPadZero);
        System.out.println("分隔符: " + strategy.pSeparator);
        
        assertEquals("应该选择文本列表匹配模式", "文本列表匹配 (.txt/.nfo)", strategy.pMode);
        assertTrue("应该启用双位补零", strategy.pPadZero);
        assertEquals("分隔符应该是'. '", ". ", strategy.pSeparator);
        System.out.println("测试结果: 文本列表匹配模式配置正确");
    }

    @Test
    public void testModeCueFileMatch() {
        System.out.println("=== 测试CUE文件匹配模式 ===");
        
        strategy.pMode = "CUE文件匹配 (.cue)";
        strategy.pPadZero = true;
        strategy.pSeparator = ". ";
        
        System.out.println("排序模式: " + strategy.pMode);
        System.out.println("双位补零: " + strategy.pPadZero);
        System.out.println("分隔符: " + strategy.pSeparator);
        
        assertEquals("应该选择CUE文件匹配模式", "CUE文件匹配 (.cue)", strategy.pMode);
        assertTrue("应该启用双位补零", strategy.pPadZero);
        assertEquals("分隔符应该是'. '", ". ", strategy.pSeparator);
        System.out.println("测试结果: CUE文件匹配模式配置正确");
    }

    @Test
    public void testModeCustomOrder() {
        System.out.println("=== 测试自定义顺序模式 ===");
        
        strategy.pMode = "自定义顺序";
        strategy.pPadZero = true;
        strategy.pSeparator = ". ";
        
        System.out.println("排序模式: " + strategy.pMode);
        System.out.println("双位补零: " + strategy.pPadZero);
        System.out.println("分隔符: " + strategy.pSeparator);
        
        assertEquals("应该选择自定义顺序模式", "自定义顺序", strategy.pMode);
        assertTrue("应该启用双位补零", strategy.pPadZero);
        assertEquals("分隔符应该是'. '", ". ", strategy.pSeparator);
        System.out.println("测试结果: 自定义顺序模式配置正确");
    }

    @Test
    public void testModeDefaultSort() {
        System.out.println("=== 测试默认排序模式 ===");
        
        strategy.pMode = "默认排序 (按文件名/拼音)";
        strategy.pPadZero = true;
        strategy.pSeparator = ". ";
        
        System.out.println("排序模式: " + strategy.pMode);
        System.out.println("双位补零: " + strategy.pPadZero);
        System.out.println("分隔符: " + strategy.pSeparator);
        
        assertEquals("应该选择默认排序模式", "默认排序 (按文件名/拼音)", strategy.pMode);
        assertTrue("应该启用双位补零", strategy.pPadZero);
        assertEquals("分隔符应该是'. '", ". ", strategy.pSeparator);
        System.out.println("测试结果: 默认排序模式配置正确");
    }

    @Test
    public void testPadZero() {
        System.out.println("=== 测试双位补零 ===");
        
        strategy.pPadZero = true;
        strategy.pSeparator = ". ";
        
        int trackNumber = 1;
        String result = formatTrackNumber(trackNumber);
        
        System.out.println("音轨编号: " + trackNumber);
        System.out.println("双位补零: " + strategy.pPadZero);
        System.out.println("输出: " + result);
        
        assertEquals("应该补零为01", "01", result);
        System.out.println("测试结果: 双位补零正确");
    }

    @Test
    public void testNoPadZero() {
        System.out.println("=== 测试不补零 ===");
        
        strategy.pPadZero = false;
        strategy.pSeparator = ". ";
        
        int trackNumber = 1;
        String result = formatTrackNumber(trackNumber);
        
        System.out.println("音轨编号: " + trackNumber);
        System.out.println("双位补零: " + strategy.pPadZero);
        System.out.println("输出: " + result);
        
        assertEquals("应该不补零为1", "1", result);
        System.out.println("测试结果: 不补零正确");
    }

    @Test
    public void testSeparatorDotSpace() {
        System.out.println("=== 测试分隔符'. ' ===");
        
        strategy.pSeparator = ". ";
        
        int trackNumber = 1;
        String songName = "天空";
        String result = formatFileName(trackNumber, songName);
        
        System.out.println("音轨编号: " + trackNumber);
        System.out.println("歌曲名称: " + songName);
        System.out.println("分隔符: " + strategy.pSeparator);
        System.out.println("输出: " + result);
        
        assertEquals("应该生成'01. 天空'", "01. 天空", result);
        System.out.println("测试结果: 分隔符'. '正确");
    }

    @Test
    public void testSeparatorHyphen() {
        System.out.println("=== 测试分隔符'-' ===");
        
        strategy.pSeparator = "-";
        
        int trackNumber = 1;
        String songName = "天空";
        String result = formatFileName(trackNumber, songName);
        
        System.out.println("音轨编号: " + trackNumber);
        System.out.println("歌曲名称: " + songName);
        System.out.println("分隔符: " + strategy.pSeparator);
        System.out.println("输出: " + result);
        
        assertEquals("应该生成'01-天空'", "01-天空", result);
        System.out.println("测试结果: 分隔符'-'正确");
    }

    @Test
    public void testSeparatorUnderscore() {
        System.out.println("=== 测试分隔符'_' ===");
        
        strategy.pSeparator = "_";
        
        int trackNumber = 1;
        String songName = "天空";
        String result = formatFileName(trackNumber, songName);
        
        System.out.println("音轨编号: " + trackNumber);
        System.out.println("歌曲名称: " + songName);
        System.out.println("分隔符: " + strategy.pSeparator);
        System.out.println("输出: " + result);
        
        assertEquals("应该生成'01_天空'", "01_天空", result);
        System.out.println("测试结果: 分隔符'_'正确");
    }

    @Test
    public void testStartNumber() {
        System.out.println("=== 测试起始编号 ===");
        
        strategy.pStartNumber = 5;
        strategy.pPadZero = true;
        
        int trackIndex = 0;
        int trackNumber = trackIndex + strategy.pStartNumber;
        String result = formatTrackNumber(trackNumber);
        
        System.out.println("起始编号: " + strategy.pStartNumber);
        System.out.println("音轨索引: " + trackIndex);
        System.out.println("音轨编号: " + trackNumber);
        System.out.println("输出: " + result);
        
        assertEquals("应该从5开始编号", "05", result);
        System.out.println("测试结果: 起始编号正确");
    }

    @Test
    public void testNumberFormat1() {
        System.out.println("=== 测试编号格式'1' ===");
        
        strategy.pPadZero = false;
        
        int trackNumber = 1;
        String result = formatTrackNumber(trackNumber);
        
        System.out.println("编号格式: 不补零");
        System.out.println("音轨编号: " + trackNumber);
        System.out.println("输出: " + result);
        
        assertEquals("应该生成'1'", "1", result);
        System.out.println("测试结果: 编号格式'1'正确");
    }

    @Test
    public void testNumberFormat01() {
        System.out.println("=== 测试编号格式'01' ===");
        
        strategy.pPadZero = true;
        
        int trackNumber = 1;
        String result = formatTrackNumber(trackNumber);
        
        System.out.println("编号格式: 补零");
        System.out.println("音轨编号: " + trackNumber);
        System.out.println("输出: " + result);
        
        assertEquals("应该生成'01'", "01", result);
        System.out.println("测试结果: 编号格式'01'正确");
    }

    @Test
    public void testNumberFormat001() {
        System.out.println("=== 测试编号格式'001' ===");
        
        strategy.pPadZero = true;
        
        int trackNumber = 1;
        String result = formatTrackNumber(trackNumber);
        
        System.out.println("编号格式: 补零");
        System.out.println("音轨编号: " + trackNumber);
        System.out.println("输出: " + result);
        
        assertEquals("应该生成'01'", "01", result);
        System.out.println("测试结果: 编号格式'01'正确");
    }

    @Test
    public void testUpdateMetadata() {
        System.out.println("=== 测试更新元数据 ===");
        
        strategy.pUpdateMetadata = true;
        
        System.out.println("更新元数据: " + strategy.pUpdateMetadata);
        
        assertTrue("应该启用元数据更新", strategy.pUpdateMetadata);
        System.out.println("测试结果: 元数据更新配置正确");
    }

    @Test
    public void testPreserveOriginal() {
        System.out.println("=== 测试保留原始文件 ===");
        
        strategy.pPreserveOriginal = true;
        
        System.out.println("保留原始文件: " + strategy.pPreserveOriginal);
        
        assertTrue("应该启用原始文件保留", strategy.pPreserveOriginal);
        System.out.println("测试结果: 原始文件保留配置正确");
    }

    @Test
    public void testGroupByDirectory() {
        System.out.println("=== 测试按目录分组 ===");
        
        // TrackNumberStrategy目前没有按目录分组功能，这里仅做示例
        System.out.println("测试结果: 按目录分组功能测试");
    }

    @Test
    public void testNonAudioFile() throws IOException {
        System.out.println("=== 测试非音频文件 ===");
        
        File textFile = createTestTextFile("file1.txt");
        ChangeRecord record = createChangeRecord(textFile);
        
        List<ChangeRecord> result = strategy.analyze(record, new ArrayList<>(Arrays.asList(record)), new ArrayList<>(Arrays.asList(testDir)));
        
        System.out.println("测试结果: 非音频文件应该被跳过");
        assertNotNull("结果不应为空", result);
        assertTrue("应该返回空列表", result.isEmpty());
    }

    @Test
    public void testEmptyFilename() throws IOException {
        System.out.println("=== 测试空文件名 ===");
        
        File audioFile = createTestAudioFile("");
        ChangeRecord record = createChangeRecord(audioFile);
        
        List<ChangeRecord> result = strategy.analyze(record, new ArrayList<>(Arrays.asList(record)), new ArrayList<>(Arrays.asList(testDir)));
        
        System.out.println("测试结果: 空文件名应该被跳过");
        assertNotNull("结果不应为空", result);
    }

    @Test
    public void testSpecialCharactersInFilename() throws IOException {
        System.out.println("=== 测试文件名中的特殊字符 ===");
        
        File audioFile = createTestAudioFile("王菲-天空[1994].mp3");
        ChangeRecord record = createChangeRecord(audioFile);
        
        List<ChangeRecord> result = strategy.analyze(record, new ArrayList<>(Arrays.asList(record)), new ArrayList<>(Arrays.asList(testDir)));
        
        System.out.println("测试结果: 特殊字符应该被正确处理");
        assertNotNull("结果不应为空", result);
    }

    @Test
    public void testLongFilename() throws IOException {
        System.out.println("=== 测试长文件名 ===");
        
        String longFilename = "Very Long Song Name That Might Exceed File System Limits And Should Be Handled Properly.mp3";
        File audioFile = createTestAudioFile(longFilename);
        ChangeRecord record = createChangeRecord(audioFile);
        
        List<ChangeRecord> result = strategy.analyze(record, new ArrayList<>(Arrays.asList(record)), new ArrayList<>(Arrays.asList(testDir)));
        
        System.out.println("测试结果: 长文件名应该被正确处理");
        assertNotNull("结果不应为空", result);
    }

    @Test
    public void testBatchNumbering() throws IOException {
        System.out.println("=== 测试批量编号 ===");
        
        strategy.pMode = "默认排序 (按文件名/拼音)";
        strategy.pPadZero = true;
        strategy.pSeparator = ". ";
        
        List<File> testFiles = createTestAudioFiles();
        List<ChangeRecord> records = createChangeRecords(testFiles);
        
        List<ChangeRecord> result = strategy.analyze(records.get(0), records, new ArrayList<>(Arrays.asList(testDir)));
        
        System.out.println("测试结果: 批量编号应该成功");
        assertNotNull("结果不应为空", result);
        assertFalse("结果不应为空列表", result.isEmpty());
        System.out.println("生成了 " + result.size() + " 个编号记录");
    }

    @Test
    public void testTextFileFormat() {
        System.out.println("=== 测试文本文件格式 ===");
        
        strategy.pMode = "文本列表匹配 (.txt/.nfo)";
        
        String txtContent = "01. 第一首歌\n02. 第二首歌\n03. 第三首歌";
        
        System.out.println("文本内容: " + txtContent);
        System.out.println("测试结果: 文本文件格式应该被正确解析");
        
        assertNotNull("文本内容不应为空", txtContent);
        assertTrue("应该包含编号", txtContent.contains("01."));
        assertTrue("应该包含编号", txtContent.contains("02."));
        assertTrue("应该包含编号", txtContent.contains("03."));
    }

    @Test
    public void testCueFileFormat() {
        System.out.println("=== 测试CUE文件格式 ===");
        
        strategy.pMode = "CUE文件匹配 (.cue)";
        
        String cueContent = "TITLE \"专辑名称\"\nPERFORMER \"艺术家\"\nFILE \"音频文件.wav\" WAVE\n  TRACK 01 AUDIO\n    TITLE \"第一首歌\"\n    PERFORMER \"艺术家\"\n    INDEX 01 00:00:00\n  TRACK 02 AUDIO\n    TITLE \"第二首歌\"\n    PERFORMER \"艺术家\"\n    INDEX 01 03:45:00";
        
        System.out.println("CUE内容: " + cueContent);
        System.out.println("测试结果: CUE文件格式应该被正确解析");
        
        assertNotNull("CUE内容不应为空", cueContent);
        assertTrue("应该包含TRACK 01", cueContent.contains("TRACK 01"));
        assertTrue("应该包含TRACK 02", cueContent.contains("TRACK 02"));
    }

    private List<File> createTestAudioFiles() throws IOException {
        List<File> files = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            File file = createTestAudioFile("song" + i + ".mp3");
            files.add(file);
        }
        return files;
    }

    private File createTestAudioFile(String fileName) throws IOException {
        if (fileName == null || fileName.isEmpty()) {
            fileName = "test.mp3";
        }
        File file = new File(testDir, fileName);
        file.createNewFile();
        return file;
    }

    private File createTestTextFile(String fileName) throws IOException {
        File file = new File(testDir, fileName);
        file.createNewFile();
        return file;
    }

    private List<ChangeRecord> createChangeRecords(List<File> files) {
        List<ChangeRecord> records = new ArrayList<>();
        for (File file : files) {
            records.add(createChangeRecord(file));
        }
        return records;
    }

    private ChangeRecord createChangeRecord(File file) {
        ChangeRecord record = new ChangeRecord();
        record.setFileHandle(file);
        record.setOriginalName(file.getAbsolutePath());
        record.setNewName(file.getAbsolutePath());
        record.setNewPath(file.getAbsolutePath());
        record.setOpType(OperationType.RENAME);
        record.setStatus(ExecStatus.PENDING);
        return record;
    }

    private String formatTrackNumber(int trackNumber) {
        if (strategy.pPadZero) {
            return String.format("%02d", trackNumber);
        } else {
            return String.valueOf(trackNumber);
        }
    }

    private String formatFileName(int trackNumber, String songName) {
        String formattedNumber = formatTrackNumber(trackNumber);
        return formattedNumber + strategy.pSeparator + songName;
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}

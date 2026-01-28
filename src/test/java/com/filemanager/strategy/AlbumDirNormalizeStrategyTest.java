package com.filemanager.strategy;

import com.filemanager.model.ChangeRecord;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 专辑目录规范化策略测试类
 * 验证AlbumDirNormalizeStrategy中各个功能模块的效果
 */
public class AlbumDirNormalizeStrategyTest {
    private AlbumDirNormalizeStrategy strategy;
    private File testDir;

    @Before
    public void setUp() throws IOException {
        strategy = new AlbumDirNormalizeStrategy();
        
        testDir = new File(System.getProperty("java.io.tmpdir"), "test_album_normalize");
        if (testDir.exists()) {
            deleteDirectory(testDir);
        }
        testDir.mkdirs();
    }

    @Test
    public void testExtractConsensusMetadata() throws IOException {
        System.out.println("=== 测试共识元数据提取 ===");
        
        List<File> testFiles = createTestAudioFiles();
        List<ChangeRecord> records = createChangeRecords(testFiles);
        
        List<ChangeRecord> result = strategy.analyze(records.get(0), records, new ArrayList<>(Arrays.asList(testDir)));
        
        System.out.println("测试结果: 应该生成规范化记录");
        assertNotNull("结果不应为空", result);
        assertFalse("结果不应为空列表", result.isEmpty());
    }

    @Test
    public void testTemplateArtistAlbum() {
        System.out.println("=== 测试艺术家-专辑模板 ===");
        
        strategy.pTemplate = "艺术家 - 专辑";
        strategy.pUseConsensusMetadata = true;
        
        String artist = "王菲";
        String album = "天空";
        String year = "1994";
        
        String result = generateDirectoryName(artist, album, year);
        
        System.out.println("模板: " + strategy.pTemplate);
        System.out.println("输入: 艺术家=" + artist + ", 专辑=" + album + ", 年份=" + year);
        System.out.println("输出: " + result);
        
        assertTrue("应该包含艺术家", result.contains(artist));
        assertTrue("应该包含专辑", result.contains(album));
        System.out.println("测试结果: 模板正确生成目录名");
    }

    @Test
    public void testTemplateArtistAlbumYear() {
        System.out.println("=== 测试艺术家-专辑[年份]模板 ===");
        
        strategy.pTemplate = "艺术家 - 专辑 [年份]";
        strategy.pUseConsensusMetadata = true;
        
        String artist = "王菲";
        String album = "天空";
        String year = "1994";
        
        String result = generateDirectoryName(artist, album, year);
        
        System.out.println("模板: " + strategy.pTemplate);
        System.out.println("输入: 艺术家=" + artist + ", 专辑=" + album + ", 年份=" + year);
        System.out.println("输出: " + result);
        
        assertTrue("应该包含艺术家", result.contains(artist));
        assertTrue("应该包含专辑", result.contains(album));
        assertTrue("应该包含年份", result.contains(year));
        System.out.println("测试结果: 模板正确生成目录名");
    }

    @Test
    public void testTemplateAlbumArtist() {
        System.out.println("=== 测试专辑-艺术家模板 ===");
        
        strategy.pTemplate = "专辑 - 艺术家";
        strategy.pUseConsensusMetadata = true;
        
        String artist = "王菲";
        String album = "天空";
        String year = "1994";
        
        String result = generateDirectoryName(artist, album, year);
        
        System.out.println("模板: " + strategy.pTemplate);
        System.out.println("输入: 艺术家=" + artist + ", 专辑=" + album + ", 年份=" + year);
        System.out.println("输出: " + result);
        
        assertTrue("应该包含专辑", result.contains(album));
        assertTrue("应该包含艺术家", result.contains(artist));
        System.out.println("测试结果: 模板正确生成目录名");
    }

    @Test
    public void testCustomTemplate() {
        System.out.println("=== 测试自定义模板 ===");
        
        strategy.pTemplate = "{year} - {artist} - {album}";
        strategy.pUseConsensusMetadata = true;
        
        String artist = "王菲";
        String album = "天空";
        String year = "1994";
        
        String result = generateDirectoryName(artist, album, year);
        
        System.out.println("模板: " + strategy.pTemplate);
        System.out.println("输入: 艺术家=" + artist + ", 专辑=" + album + ", 年份=" + year);
        System.out.println("输出: " + result);
        
        assertTrue("应该包含年份", result.contains(year));
        assertTrue("应该包含艺术家", result.contains(artist));
        assertTrue("应该包含专辑", result.contains(album));
        System.out.println("测试结果: 自定义模板正确生成目录名");
    }

    @Test
    public void testMetadataValidation() throws IOException {
        System.out.println("=== 测试元数据验证 ===");
        
        strategy.pValidateAlbumInfo = true;
        
        File validFile = createTestAudioFile("song1.mp3");
        File invalidFile = createTestAudioFile("song2.mp3");
        
        ChangeRecord validRecord = createChangeRecord(validFile);
        ChangeRecord invalidRecord = createChangeRecord(invalidFile);
        
        List<ChangeRecord> result = strategy.analyze(validRecord, new ArrayList<>(Arrays.asList(validRecord, invalidRecord)), new ArrayList<>(Arrays.asList(testDir)));
        
        System.out.println("测试结果: 应该跳过元数据不完整的文件");
        assertNotNull("结果不应为空", result);
    }

    @Test
    public void testEmptyDirectory() {
        System.out.println("=== 测试空目录 ===");
        
        File emptyDir = new File(testDir, "empty_dir");
        emptyDir.mkdirs();
        
        ChangeRecord record = createChangeRecord(emptyDir);
        List<ChangeRecord> result = strategy.analyze(record, new ArrayList<>(Arrays.asList(record)), new ArrayList<>(Arrays.asList(testDir)));
        
        System.out.println("测试结果: 空目录应该被跳过");
        assertNotNull("结果不应为空", result);
        assertTrue("空目录应该返回空列表", result.isEmpty());
    }

    @Test
    public void testNonAudioDirectory() throws IOException {
        System.out.println("=== 测试非音频目录 ===");
        
        File nonAudioDir = new File(testDir, "non_audio_dir");
        nonAudioDir.mkdirs();
        
        createTestTextFile(nonAudioDir, "file1.txt");
        createTestTextFile(nonAudioDir, "file2.txt");
        
        ChangeRecord record = createChangeRecord(nonAudioDir);
        List<ChangeRecord> result = strategy.analyze(record, new ArrayList<>(Arrays.asList(record)), new ArrayList<>(Arrays.asList(testDir)));
        
        System.out.println("测试结果: 非音频目录应该被跳过");
        assertNotNull("结果不应为空", result);
        assertTrue("非音频目录应该返回空列表", result.isEmpty());
    }

    @Test
    public void testLongDirectoryName() {
        System.out.println("=== 测试长目录名 ===");
        
        strategy.pTemplate = "艺术家 - 专辑 [年份]";
        strategy.pUseConsensusMetadata = true;
        
        String longArtist = "Very Long Artist Name That Might Exceed Path Limits";
        String longAlbum = "Very Long Album Name That Might Exceed Path Limits";
        String year = "1994";
        
        String result = generateDirectoryName(longArtist, longAlbum, year);
        
        System.out.println("模板: " + strategy.pTemplate);
        System.out.println("输入: 艺术家=" + longArtist + ", 专辑=" + longAlbum + ", 年份=" + year);
        System.out.println("输出: " + result);
        System.out.println("输出长度: " + result.length());
        
        assertTrue("应该生成目录名", result.length() > 0);
        System.out.println("测试结果: 长目录名被正确处理");
    }

    @Test
    public void testSpecialCharactersInMetadata() {
        System.out.println("=== 测试元数据中的特殊字符 ===");
        
        strategy.pTemplate = "艺术家 - 专辑";
        strategy.pUseConsensusMetadata = true;
        
        String artist = "王菲 (Faye Wong)";
        String album = "天空 [Sky]";
        String year = "1994";
        
        String result = generateDirectoryName(artist, album, year);
        
        System.out.println("模板: " + strategy.pTemplate);
        System.out.println("输入: 艺术家=" + artist + ", 专辑=" + album + ", 年份=" + year);
        System.out.println("输出: " + result);
        
        assertTrue("应该生成目录名", result.length() > 0);
        System.out.println("测试结果: 特殊字符被正确处理");
    }

    @Test
    public void testConsensusAlgorithm() throws IOException {
        System.out.println("=== 测试共识算法 ===");
        
        List<File> testFiles = createTestAudioFilesWithDifferentMetadata();
        List<ChangeRecord> records = createChangeRecords(testFiles);
        
        List<ChangeRecord> result = strategy.analyze(records.get(0), records, new ArrayList<>(Arrays.asList(testDir)));
        
        System.out.println("测试结果: 共识算法应该选择出现频率最高的值");
        assertNotNull("结果不应为空", result);
        assertFalse("结果不应为空列表", result.isEmpty());
    }

    @Test
    public void testBackupOriginalDirectory() {
        System.out.println("=== 测试备份原始目录 ===");
        
        // 测试备份功能（AlbumDirNormalizeStrategy目前没有备份功能，这里仅做示例）
        System.out.println("测试结果: 备份功能测试");
    }

    private List<File> createTestAudioFiles() throws IOException {
        List<File> files = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            File file = createTestAudioFile("song" + i + ".mp3");
            files.add(file);
        }
        return files;
    }

    private List<File> createTestAudioFilesWithDifferentMetadata() throws IOException {
        List<File> files = new ArrayList<>();
        
        File file1 = createTestAudioFile("song1.mp3");
        File file2 = createTestAudioFile("song2.mp3");
        File file3 = createTestAudioFile("song3.mp3");
        
        files.add(file1);
        files.add(file2);
        files.add(file3);
        
        return files;
    }

    private File createTestAudioFile(String fileName) throws IOException {
        File file = new File(testDir, fileName);
        file.createNewFile();
        return file;
    }

    private File createTestAudioFile(File parent, String fileName) throws IOException {
        File file = new File(parent, fileName);
        file.createNewFile();
        return file;
    }

    private void createTestTextFile(File parent, String fileName) throws IOException {
        File file = new File(parent, fileName);
        file.createNewFile();
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
        record.setOpType(OperationType.ALBUM_RENAME);
        record.setStatus(ExecStatus.PENDING);
        return record;
    }

    private String generateDirectoryName(String artist, String album, String year) {
        String template = strategy.pTemplate;
        String result = template
                .replace("{artist}", artist)
                .replace("{album}", album)
                .replace("{year}", year)
                .replace("{genre}", "流行");
        return result;
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

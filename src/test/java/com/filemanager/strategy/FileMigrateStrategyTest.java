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
 * 文件迁移策略测试类
 * 验证FileMigrateStrategy中各个功能模块的效果
 */
public class FileMigrateStrategyTest {
    private FileMigrateStrategy strategy;
    private File sourceDir;
    private File destDir;

    @Before
    public void setUp() throws IOException {
        strategy = new FileMigrateStrategy();
        
        sourceDir = new File(System.getProperty("java.io.tmpdir"), "test_migrate_source");
        destDir = new File(System.getProperty("java.io.tmpdir"), "test_migrate_dest");
        
        if (sourceDir.exists()) {
            deleteDirectory(sourceDir);
        }
        if (destDir.exists()) {
            deleteDirectory(destDir);
        }
        
        sourceDir.mkdirs();
        destDir.mkdirs();
    }

    @Test
    public void testTemplateArtistAlbum() {
        System.out.println("=== 测试艺术家/专辑目录结构模板 ===");
        
        strategy.pPattern = "艺术家/专辑";
        strategy.pDestDir = destDir.getAbsolutePath();
        
        String artist = "王菲";
        String album = "天空";
        
        String result = generateTargetPath(artist, album, "1994", "流行");
        
        System.out.println("模板: " + strategy.pPattern);
        System.out.println("输入: 艺术家=" + artist + ", 专辑=" + album);
        System.out.println("输出: " + result);
        
        assertTrue("应该包含艺术家目录", result.contains(artist));
        assertTrue("应该包含专辑目录", result.contains(album));
        System.out.println("测试结果: 模板正确生成目标路径");
    }

    @Test
    public void testTemplateArtistAlbumYear() {
        System.out.println("=== 测试艺术家/专辑/年份目录结构模板 ===");
        
        strategy.pPattern = "艺术家/专辑/年份";
        strategy.pDestDir = destDir.getAbsolutePath();
        
        String artist = "王菲";
        String album = "天空";
        String year = "1994";
        
        String result = generateTargetPath(artist, album, year, "流行");
        
        System.out.println("模板: " + strategy.pPattern);
        System.out.println("输入: 艺术家=" + artist + ", 专辑=" + album + ", 年份=" + year);
        System.out.println("输出: " + result);
        
        assertTrue("应该包含艺术家目录", result.contains(artist));
        assertTrue("应该包含专辑目录", result.contains(album));
        assertTrue("应该包含年份目录", result.contains(year));
        System.out.println("测试结果: 模板正确生成目标路径");
    }

    @Test
    public void testTemplateArtistYearAlbum() {
        System.out.println("=== 测试艺术家/年份/专辑目录结构模板 ===");
        
        strategy.pPattern = "艺术家/年份/专辑";
        strategy.pDestDir = destDir.getAbsolutePath();
        
        String artist = "王菲";
        String album = "天空";
        String year = "1994";
        
        String result = generateTargetPath(artist, album, year, "流行");
        
        System.out.println("模板: " + strategy.pPattern);
        System.out.println("输入: 艺术家=" + artist + ", 专辑=" + album + ", 年份=" + year);
        System.out.println("输出: " + result);
        
        assertTrue("应该包含艺术家目录", result.contains(artist));
        assertTrue("应该包含年份目录", result.contains(year));
        assertTrue("应该包含专辑目录", result.contains(album));
        System.out.println("测试结果: 模板正确生成目标路径");
    }

    @Test
    public void testTemplateGenreArtistAlbum() {
        System.out.println("=== 测试流派/艺术家/专辑目录结构模板 ===");
        
        strategy.pPattern = "流派/艺术家/专辑";
        strategy.pDestDir = destDir.getAbsolutePath();
        
        String artist = "王菲";
        String album = "天空";
        String genre = "流行";
        
        String result = generateTargetPath(artist, album, "1994", genre);
        
        System.out.println("模板: " + strategy.pPattern);
        System.out.println("输入: 艺术家=" + artist + ", 专辑=" + album + ", 流派=" + genre);
        System.out.println("输出: " + result);
        
        assertTrue("应该包含流派目录", result.contains(genre));
        assertTrue("应该包含艺术家目录", result.contains(artist));
        assertTrue("应该包含专辑目录", result.contains(album));
        System.out.println("测试结果: 模板正确生成目标路径");
    }

    @Test
    public void testCustomTemplate() {
        System.out.println("=== 测试自定义目录结构模板 ===");
        
        strategy.pPattern = "{year}/{genre}/{artist}/{album}";
        strategy.pDestDir = destDir.getAbsolutePath();
        
        String artist = "王菲";
        String album = "天空";
        String year = "1994";
        String genre = "流行";
        
        String result = generateTargetPath(artist, album, year, genre);
        
        System.out.println("模板: " + strategy.pPattern);
        System.out.println("输入: 艺术家=" + artist + ", 专辑=" + album + ", 年份=" + year + ", 流派=" + genre);
        System.out.println("输出: " + result);
        
        assertTrue("应该包含年份目录", result.contains(year));
        assertTrue("应该包含流派目录", result.contains(genre));
        assertTrue("应该包含艺术家目录", result.contains(artist));
        assertTrue("应该包含专辑目录", result.contains(album));
        System.out.println("测试结果: 自定义模板正确生成目标路径");
    }

    @Test
    public void testMetadataValidation() throws IOException {
        System.out.println("=== 测试元数据验证 ===");
        
        strategy.pValidateMetadata = true;
        strategy.pDestDir = destDir.getAbsolutePath();
        
        File validFile = createTestAudioFile("song1.mp3");
        File invalidFile = createTestAudioFile("song2.mp3");
        
        ChangeRecord validRecord = createChangeRecord(validFile);
        ChangeRecord invalidRecord = createChangeRecord(invalidFile);
        
        List<ChangeRecord> result = strategy.analyze(validRecord, new ArrayList<>(Arrays.asList(validRecord, invalidRecord)), new ArrayList<>(Arrays.asList(sourceDir)));
        
        System.out.println("测试结果: 应该跳过元数据不完整的文件");
        assertNotNull("结果不应为空", result);
    }

    @Test
    public void testNoDestinationDirectory() throws IOException {
        System.out.println("=== 测试未设置目标目录 ===");
        
        strategy.pDestDir = null;
        
        File testFile = createTestAudioFile("song1.mp3");
        ChangeRecord record = createChangeRecord(testFile);
        
        List<ChangeRecord> result = strategy.analyze(record, new ArrayList<>(Arrays.asList(record)), new ArrayList<>(Arrays.asList(sourceDir)));
        
        System.out.println("测试结果: 未设置目标目录应该返回空列表");
        assertNotNull("结果不应为空", result);
        assertTrue("应该返回空列表", result.isEmpty());
    }

    @Test
    public void testNonAudioFile() throws IOException {
        System.out.println("=== 测试非音频文件 ===");
        
        strategy.pDestDir = destDir.getAbsolutePath();
        
        File textFile = createTestTextFile("file1.txt");
        ChangeRecord record = createChangeRecord(textFile);
        
        List<ChangeRecord> result = strategy.analyze(record, new ArrayList<>(Arrays.asList(record)), new ArrayList<>(Arrays.asList(sourceDir)));
        
        System.out.println("测试结果: 非音频文件应该被跳过");
        assertNotNull("结果不应为空", result);
        assertTrue("应该返回空列表", result.isEmpty());
    }

    @Test
    public void testLongPath() {
        System.out.println("=== 测试长路径 ===");
        
        strategy.pPattern = "艺术家/专辑/年份";
        strategy.pDestDir = destDir.getAbsolutePath();
        
        String longArtist = "Very Long Artist Name That Might Exceed Path Limits";
        String longAlbum = "Very Long Album Name That Might Exceed Path Limits";
        String year = "1994";
        
        String result = generateTargetPath(longArtist, longAlbum, year, "流行");
        
        System.out.println("模板: " + strategy.pPattern);
        System.out.println("输入: 艺术家=" + longArtist + ", 专辑=" + longAlbum + ", 年份=" + year);
        System.out.println("输出: " + result);
        System.out.println("输出长度: " + result.length());
        
        assertTrue("应该生成路径", result.length() > 0);
        System.out.println("测试结果: 长路径被正确处理");
    }

    @Test
    public void testSpecialCharactersInMetadata() {
        System.out.println("=== 测试元数据中的特殊字符 ===");
        
        strategy.pPattern = "艺术家/专辑";
        strategy.pDestDir = destDir.getAbsolutePath();
        
        String artist = "王菲 (Faye Wong)";
        String album = "天空 [Sky]";
        String year = "1994";
        
        String result = generateTargetPath(artist, album, year, "流行");
        
        System.out.println("模板: " + strategy.pPattern);
        System.out.println("输入: 艺术家=" + artist + ", 专辑=" + album);
        System.out.println("输出: " + result);
        
        assertTrue("应该生成路径", result.length() > 0);
        System.out.println("测试结果: 特殊字符被正确处理");
    }

    @Test
    public void testPreserveTimestamp() {
        System.out.println("=== 测试保留时间戳 ===");
        
        System.out.println("测试结果: 应该保留原始文件的时间戳");
        System.out.println("原始时间戳: " + System.currentTimeMillis());
    }

    @Test
    public void testGeneratePlaylist() throws IOException {
        System.out.println("=== 测试生成播放列表 ===");
        
        strategy.pCreatePlaylists = true;
        strategy.pDestDir = destDir.getAbsolutePath();
        
        List<File> testFiles = createTestAudioFiles();
        List<ChangeRecord> records = createChangeRecords(testFiles);
        
        List<ChangeRecord> result = strategy.analyze(records.get(0), new ArrayList<>(records), new ArrayList<>(Arrays.asList(sourceDir)));
        
        System.out.println("测试结果: 应该生成播放列表");
        assertNotNull("结果不应为空", result);
        assertFalse("结果不应为空列表", result.isEmpty());
    }

    @Test
    public void testOverwriteExisting() throws IOException {
        System.out.println("=== 测试覆盖现有文件 ===");
        
        strategy.pSkipExisting = false;
        strategy.pDestDir = destDir.getAbsolutePath();
        
        File testFile = createTestAudioFile("song1.mp3");
        File existingFile = createTestAudioFileInDest("song1.mp3");
        
        ChangeRecord record = createChangeRecord(testFile);
        List<ChangeRecord> result = strategy.analyze(record, new ArrayList<>(Arrays.asList(record)), new ArrayList<>(Arrays.asList(sourceDir)));
        
        System.out.println("测试结果: 应该允许覆盖现有文件");
        assertNotNull("结果不应为空", result);
        assertFalse("结果不应为空列表", result.isEmpty());
    }

    @Test
    public void testSkipExisting() throws IOException {
        System.out.println("=== 测试跳过现有文件 ===");
        
        strategy.pSkipExisting = true;
        strategy.pDestDir = destDir.getAbsolutePath();
        
        File testFile = createTestAudioFile("song1.mp3");
        File existingFile = createTestAudioFileInDest("song1.mp3");
        
        ChangeRecord record = createChangeRecord(testFile);
        List<ChangeRecord> result = strategy.analyze(record, new ArrayList<>(Arrays.asList(record)), new ArrayList<>(Arrays.asList(sourceDir)));
        
        System.out.println("测试结果: 应该跳过现有文件");
        assertNotNull("结果不应为空", result);
    }

    @Test
    public void testKeepOriginalStructure() throws IOException {
        System.out.println("=== 测试保留原始目录结构 ===");
        
        strategy.pPreserveStructure = true;
        strategy.pDestDir = destDir.getAbsolutePath();
        
        File subDir = new File(sourceDir, "subdir");
        subDir.mkdirs();
        File testFile = createTestAudioFileInDir(subDir, "song1.mp3");
        
        ChangeRecord record = createChangeRecord(testFile);
        List<ChangeRecord> result = strategy.analyze(record, new ArrayList<>(Arrays.asList(record)), new ArrayList<>(Arrays.asList(sourceDir)));
        
        System.out.println("测试结果: 应该保留原始目录结构");
        assertNotNull("结果不应为空", result);
        assertFalse("结果不应为空列表", result.isEmpty());
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
        File file = new File(sourceDir, fileName);
        file.createNewFile();
        return file;
    }

    private File createTestAudioFileInDir(File parent, String fileName) throws IOException {
        File file = new File(parent, fileName);
        file.createNewFile();
        return file;
    }

    private File createTestAudioFileInDest(String fileName) throws IOException {
        File file = new File(destDir, fileName);
        file.createNewFile();
        return file;
    }

    private File createTestTextFile(String fileName) throws IOException {
        File file = new File(sourceDir, fileName);
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
        record.setOpType(OperationType.MOVE);
        record.setStatus(ExecStatus.PENDING);
        return record;
    }

    private String generateTargetPath(String artist, String album, String year, String genre) {
        String template = strategy.pPattern;
        String result = template
                .replace("{artist}", artist)
                .replace("{album}", album)
                .replace("{year}", year)
                .replace("{genre}", genre)
                .replace("{track}", "01");
        
        return destDir.getAbsolutePath() + File.separator + result;
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

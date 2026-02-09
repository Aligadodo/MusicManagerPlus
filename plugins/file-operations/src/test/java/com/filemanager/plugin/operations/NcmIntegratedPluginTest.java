package com.filemanager.plugin.operations;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NcmIntegratedPluginTest {

    private NcmIntegratedPlugin plugin;
    private PluginConfigDTO config;
    private ExecutionContext context;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        plugin = new NcmIntegratedPlugin();
        config = new PluginConfigDTO();
        config.setConfigValues(new HashMap<>());
        context = new ExecutionContext();
    }

    @Test
    public void testPluginInitialization() {
        assertEquals("ncm-integrated", plugin.getId());
        assertEquals("网易云音乐工具集插件", plugin.getName());
        assertNotNull(plugin.getParameters());
        assertFalse(plugin.getParameters().isEmpty());
    }

    @Test
    public void testGetDefaultConfig() {
        PluginConfigDTO defaultConfig = plugin.getDefaultConfig();
        assertNotNull(defaultConfig);
        assertNotNull(defaultConfig.getConfigValues());
        assertEquals("convert", defaultConfig.getValue("function"));
        assertEquals("mp3", defaultConfig.getValue("outputFormat"));
        assertEquals("320k", defaultConfig.getValue("bitrate"));
        assertTrue((boolean) defaultConfig.getValue("downloadLyric"));
        assertEquals("lrc", defaultConfig.getValue("lyricFormat"));
        assertFalse((boolean) defaultConfig.getValue("overwrite"));
    }

    @Test
    public void testPreview_EmptyFileList() {
        List<ChangeRecord> records = plugin.preview(new ArrayList<>(), config, context);
        assertNotNull(records);
        assertTrue(records.isEmpty());
    }

    @Test
    public void testPreview_WithFiles() throws IOException {
        File testFile = tempDir.resolve("test.ncm").toFile();
        Files.write(testFile.toPath(), createTestNcmContent().getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        List<ChangeRecord> records = plugin.preview(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals("PENDING", records.get(0).getStatus());
    }

    @Test
    public void testExecute_WithFiles() throws IOException {
        File testFile = tempDir.resolve("test.ncm").toFile();
        Files.write(testFile.toPath(), createTestNcmContent().getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_WithConvertFunction() throws IOException {
        File testFile = tempDir.resolve("test.ncm").toFile();
        Files.write(testFile.toPath(), createTestNcmContent().getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        config.setValue("function", "convert");
        config.setValue("outputFormat", "mp3");
        config.setValue("bitrate", "320k");
        config.setValue("outputDir", tempDir.resolve("output").toString());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_WithCacheScanFunction() throws IOException {
        File testFile = tempDir.resolve("test.ncm").toFile();
        Files.write(testFile.toPath(), createTestNcmContent().getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        config.setValue("function", "cache_scan");
        config.setValue("cacheDir", tempDir.toString());
        config.setValue("outputDir", tempDir.resolve("output").toString());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_WithLyricDownloadFunction() throws IOException {
        File testFile = tempDir.resolve("test.ncm").toFile();
        Files.write(testFile.toPath(), createTestNcmContent().getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        config.setValue("function", "lyric_download");
        config.setValue("lyricFormat", "lrc");
        config.setValue("outputDir", tempDir.resolve("output").toString());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_NonExistentFile() {
        List<String> filePaths = new ArrayList<>();
        filePaths.add("/nonexistent/file.ncm");

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals("SKIPPED", records.get(0).getStatus());
    }

    @Test
    public void testExecute_WithCancellation() throws IOException {
        File testFile = tempDir.resolve("test.ncm").toFile();
        Files.write(testFile.toPath(), createTestNcmContent().getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        context.cancel();
        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertTrue(records.isEmpty() || records.get(0).getStatus().equals("SKIPPED"));
    }

    @Test
    public void testNcmDecryptor_ParseNcmFile() throws IOException {
        NcmDecryptor decryptor = new NcmDecryptor();
        File ncmFile = tempDir.resolve("test.ncm").toFile();
        Files.write(ncmFile.toPath(), createTestNcmContent().getBytes());
        
        NcmDecryptor.NcmFile result = decryptor.parseNcmFile(ncmFile.getAbsolutePath());
        assertNotNull(result);
        assertEquals(ncmFile.getAbsolutePath(), result.getFilePath());
    }

    @Test
    public void testNcmDecryptor_ParseNonExistentFile() {
        NcmDecryptor decryptor = new NcmDecryptor();
        NcmDecryptor.NcmFile result = decryptor.parseNcmFile("/nonexistent/file.ncm");
        assertNotNull(result);
        assertFalse(result.isValid());
    }

    @Test
    public void testNcmDecryptor_ParseInvalidFile() throws IOException {
        NcmDecryptor decryptor = new NcmDecryptor();
        File invalidFile = tempDir.resolve("invalid.txt").toFile();
        Files.write(invalidFile.toPath(), "invalid content".getBytes());
        
        NcmDecryptor.NcmFile result = decryptor.parseNcmFile(invalidFile.getAbsolutePath());
        assertNotNull(result);
        assertFalse(result.isValid());
    }

    @Test
    public void testNcmDecryptor_DecryptNcmFile() throws IOException {
        NcmDecryptor decryptor = new NcmDecryptor();
        decryptor.setOutputDirectory(tempDir.resolve("output").toString());
        decryptor.setOutputFormat("mp3");
        decryptor.setOverwriteExisting(true);
        
        File ncmFile = tempDir.resolve("test.ncm").toFile();
        Files.write(ncmFile.toPath(), createTestNcmContent().getBytes());
        
        NcmDecryptor.DecryptionResult result = decryptor.decryptNcmFile(ncmFile.getAbsolutePath());
        assertNotNull(result);
        assertNotNull(result.getNcmFile());
    }

    @Test
    public void testNcmDecryptor_BatchDecrypt() throws IOException {
        NcmDecryptor decryptor = new NcmDecryptor();
        decryptor.setOutputDirectory(tempDir.resolve("output").toString());
        
        File file1 = tempDir.resolve("file1.ncm").toFile();
        File file2 = tempDir.resolve("file2.ncm").toFile();
        Files.write(file1.toPath(), createTestNcmContent().getBytes());
        Files.write(file2.toPath(), createTestNcmContent().getBytes());
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(file1.getAbsolutePath());
        filePaths.add(file2.getAbsolutePath());
        
        List<NcmDecryptor.DecryptionResult> results = decryptor.decryptBatch(filePaths);
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    public void testNcmDecryptor_DownloadLyrics() throws IOException {
        NcmDecryptor decryptor = new NcmDecryptor();
        decryptor.setOutputDirectory(tempDir.resolve("output").toString());
        decryptor.setDownloadLyrics(true);
        decryptor.setLyricsFormat("lrc");
        decryptor.setOverwriteExisting(true);
        
        File ncmFile = tempDir.resolve("test.ncm").toFile();
        Files.write(ncmFile.toPath(), createTestNcmContent().getBytes());
        
        NcmDecryptor.DecryptionResult result = decryptor.decryptNcmFile(ncmFile.getAbsolutePath());
        assertNotNull(result);
    }

    @Test
    public void testNcmDecryptor_GenerateOutputPath() throws IOException {
        NcmDecryptor decryptor = new NcmDecryptor();
        decryptor.setOutputDirectory(tempDir.resolve("output").toString());
        decryptor.setOutputFormat("mp3");
        
        File ncmFile = tempDir.resolve("test.ncm").toFile();
        Files.write(ncmFile.toPath(), createTestNcmContent().getBytes());
        
        NcmDecryptor.NcmFile parsedFile = decryptor.parseNcmFile(ncmFile.getAbsolutePath());
        String outputPath = decryptor.generateOutputPath(parsedFile);
        
        assertNotNull(outputPath);
        assertTrue(outputPath.startsWith(tempDir.resolve("output").toString()));
        assertTrue(outputPath.endsWith(".mp3"));
    }

    @Test
    public void testValidateNcmFile() throws IOException {
        File validFile = tempDir.resolve("valid.ncm").toFile();
        Files.write(validFile.toPath(), createTestNcmContent().getBytes());
        
        boolean isValid = plugin.validateNcmFile(validFile.getAbsolutePath());
        assertTrue(isValid);
    }

    @Test
    public void testValidateNcmFile_NonExistent() {
        boolean isValid = plugin.validateNcmFile("/nonexistent/file.ncm");
        assertFalse(isValid);
    }

    @Test
    public void testValidateNcmFile_InvalidExtension() throws IOException {
        File invalidFile = tempDir.resolve("invalid.txt").toFile();
        Files.write(invalidFile.toPath(), createTestNcmContent().getBytes());
        
        boolean isValid = plugin.validateNcmFile(invalidFile.getAbsolutePath());
        assertFalse(isValid);
    }

    @Test
    public void testProcessCacheScan() throws IOException {
        File cacheDir = tempDir.resolve("cache").toFile();
        cacheDir.mkdirs();
        
        File ncmFile = new File(cacheDir, "test.ncm");
        Files.write(ncmFile.toPath(), createTestNcmContent().getBytes());
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(cacheDir.getAbsolutePath());
        
        config.setValue("function", "cache_scan");
        config.setValue("cacheDir", cacheDir.getAbsolutePath());
        config.setValue("outputDir", tempDir.resolve("output").toString());
        
        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertTrue(records.size() >= 1);
    }

    @Test
    public void testProcessLyricDownload() throws IOException {
        File ncmFile = tempDir.resolve("test.ncm").toFile();
        Files.write(ncmFile.toPath(), createTestNcmContent().getBytes());
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(ncmFile.getAbsolutePath());
        
        config.setValue("function", "lyric_download");
        config.setValue("lyricFormat", "lrc");
        config.setValue("outputDir", tempDir.resolve("output").toString());
        
        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testBatchProcessing() throws IOException {
        File file1 = tempDir.resolve("file1.ncm").toFile();
        File file2 = tempDir.resolve("file2.ncm").toFile();
        
        Files.write(file1.toPath(), createTestNcmContent().getBytes());
        Files.write(file2.toPath(), createTestNcmContent().getBytes());
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(file1.getAbsolutePath());
        filePaths.add(file2.getAbsolutePath());
        
        config.setValue("function", "convert");
        config.setValue("outputDir", tempDir.resolve("output").toString());
        
        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(2, records.size());
    }

    @Test
    public void testErrorHandling() throws IOException {
        File testFile = tempDir.resolve("test.ncm").toFile();
        Files.write(testFile.toPath(), createTestNcmContent().getBytes());
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());
        
        // Test with invalid function
        config.setValue("function", "invalid_function");
        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testSupportedFormats() {
        List<String> supportedFormats = plugin.getSupportedFormats();
        assertNotNull(supportedFormats);
        assertFalse(supportedFormats.isEmpty());
        assertTrue(supportedFormats.contains("mp3"));
        assertTrue(supportedFormats.contains("flac"));
        assertTrue(supportedFormats.contains("wav"));
        assertTrue(supportedFormats.contains("ogg"));
        assertTrue(supportedFormats.contains("aac"));
    }

    @Test
    public void testIsSupportedFormat() {
        assertTrue(plugin.isSupportedFormat("mp3"));
        assertTrue(plugin.isSupportedFormat("flac"));
        assertTrue(plugin.isSupportedFormat("wav"));
        assertTrue(plugin.isSupportedFormat("ogg"));
        assertTrue(plugin.isSupportedFormat("aac"));
        assertFalse(plugin.isSupportedFormat("txt"));
        assertFalse(plugin.isSupportedFormat("jpg"));
    }

    @Test
    public void testCleanup() throws IOException {
        File tempFile = tempDir.resolve("temp.txt").toFile();
        Files.write(tempFile.toPath(), "temp content".getBytes());
        
        boolean cleaned = plugin.cleanup(tempFile.getAbsolutePath());
        assertTrue(cleaned);
        assertFalse(tempFile.exists());
    }

    @Test
    public void testCleanup_NonExistent() {
        boolean cleaned = plugin.cleanup("/nonexistent/file.txt");
        assertTrue(cleaned);
    }

    private String createTestNcmContent() {
        // 使用字节数组来创建测试数据，避免使用非法的转义符
        byte[] data = new byte[30];
        // Magic header: CTCN
        data[0] = 'C';
        data[1] = 'T';
        data[2] = 'C';
        data[3] = 'N';
        // Key length: 1
        data[4] = 0;
        data[5] = 0;
        data[6] = 0;
        data[7] = 1;
        // Meta data length: 1
        data[8] = 0;
        data[9] = 0;
        data[10] = 0;
        data[11] = 1;
        // Key data: k
        data[12] = 'k';
        // Meta data: m
        data[13] = 'm';
        // CRC32: 0
        data[14] = 0;
        data[15] = 0;
        data[16] = 0;
        data[17] = 0;
        // Gap: 0
        data[18] = 0;
        data[19] = 0;
        data[20] = 0;
        data[21] = 0;
        // Image header: 0
        data[22] = 0;
        data[23] = 0;
        data[24] = 0;
        data[25] = 0;
        // Test audio content
        byte[] content = "test audio content".getBytes();
        System.arraycopy(content, 0, data, 26, Math.min(content.length, 4));
        
        return new String(data);
    }
}

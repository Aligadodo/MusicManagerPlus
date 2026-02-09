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

public class AudioConverterPluginTest {

    private AudioConverterPlugin plugin;
    private PluginConfigDTO config;
    private ExecutionContext context;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        plugin = new AudioConverterPlugin();
        config = new PluginConfigDTO();
        config.setConfigValues(new HashMap<>());
        context = new ExecutionContext();
    }

    @Test
    public void testPluginInitialization() {
        assertEquals("audio-converter", plugin.getId());
        assertEquals("音频格式转换", plugin.getName());
        assertNotNull(plugin.getParameters());
        assertFalse(plugin.getParameters().isEmpty());
    }

    @Test
    public void testGetDefaultConfig() {
        PluginConfigDTO defaultConfig = plugin.getDefaultConfig();
        assertNotNull(defaultConfig);
        assertNotNull(defaultConfig.getConfigValues());
    }

    @Test
    public void testPreview_EmptyFileList() {
        List<ChangeRecord> records = plugin.preview(new ArrayList<>(), config, context);
        assertNotNull(records);
        assertTrue(records.isEmpty());
    }

    @Test
    public void testPreview_WithFiles() throws IOException {
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        List<ChangeRecord> records = plugin.preview(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals("PENDING", records.get(0).getStatus());
    }

    @Test
    public void testExecute_WithFiles() throws IOException {
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_WithFormatConversion() throws IOException {
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        config.setValue("outputFormat", "flac");
        config.setValue("bitrate", "320k");

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_NonExistentFile() {
        List<String> filePaths = new ArrayList<>();
        filePaths.add("/nonexistent/file.mp3");

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals("SKIPPED", records.get(0).getStatus());
    }

    @Test
    public void testExecute_WithCancellation() throws IOException {
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        context.cancel();
        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertTrue(records.isEmpty() || records.get(0).getStatus().equals("SKIPPED"));
    }

    @Test
    public void testAudioConversionChecker_FileNotFound() {
        AudioConversionChecker checker = new AudioConversionChecker();
        AudioConversionChecker.ConversionCheckResult result = checker.checkConversion(
            "/nonexistent/file.mp3", "flac", 320, 44100, 2
        );
        
        assertNotNull(result);
        assertTrue(result.isShouldSkip());
        assertEquals(AudioConversionChecker.SkipReason.FILE_NOT_FOUND, result.getSkipReason());
    }

    @Test
    public void testAudioConversionChecker_InvalidSource() {
        AudioConversionChecker checker = new AudioConversionChecker();
        File directory = tempDir.resolve("directory").toFile();
        directory.mkdirs();
        
        AudioConversionChecker.ConversionCheckResult result = checker.checkConversion(
            directory.getAbsolutePath(), "flac", 320, 44100, 2
        );
        
        assertNotNull(result);
        assertTrue(result.isShouldSkip());
        assertEquals(AudioConversionChecker.SkipReason.INVALID_SOURCE, result.getSkipReason());
    }

    @Test
    public void testAudioConversionChecker_TargetExists() throws IOException {
        AudioConversionChecker checker = new AudioConversionChecker();
        checker.setSkipIfTargetExists(true);
        checker.setOverwriteExisting(false);
        
        File sourceFile = tempDir.resolve("source.mp3").toFile();
        File targetFile = tempDir.resolve("source.flac").toFile();
        
        Files.write(sourceFile.toPath(), "content".getBytes());
        Files.write(targetFile.toPath(), "content".getBytes());
        
        AudioConversionChecker.ConversionCheckResult result = checker.checkConversion(
            sourceFile.getAbsolutePath(), "flac", 320, 44100, 2
        );
        
        assertNotNull(result);
        assertTrue(result.isShouldSkip());
        assertEquals(AudioConversionChecker.SkipReason.TARGET_EXISTS, result.getSkipReason());
    }

    @Test
    public void testAudioConversionChecker_SameFormat() throws IOException {
        AudioConversionChecker checker = new AudioConversionChecker();
        checker.setSkipIfSameFormat(true);
        
        File sourceFile = tempDir.resolve("source.mp3").toFile();
        Files.write(sourceFile.toPath(), "content".getBytes());
        
        AudioConversionChecker.ConversionCheckResult result = checker.checkConversion(
            sourceFile.getAbsolutePath(), "mp3", 320, 44100, 2
        );
        
        assertNotNull(result);
        assertTrue(result.isShouldSkip());
        assertEquals(AudioConversionChecker.SkipReason.SAME_FORMAT, result.getSkipReason());
    }

    @Test
    public void testAudioConversionChecker_SameQuality() throws IOException {
        AudioConversionChecker checker = new AudioConversionChecker();
        checker.setSkipIfSameQuality(true);
        
        File sourceFile = tempDir.resolve("source.mp3").toFile();
        Files.write(sourceFile.toPath(), "content".getBytes());
        
        AudioConversionChecker.ConversionCheckResult result = checker.checkConversion(
            sourceFile.getAbsolutePath(), "flac", 320, 44100, 2
        );
        
        assertNotNull(result);
        assertFalse(result.isShouldSkip());
    }

    @Test
    public void testAudioConversionChecker_HigherQualityExists() throws IOException {
        AudioConversionChecker checker = new AudioConversionChecker();
        checker.setSkipIfHigherQualityExists(true);
        
        File sourceFile = tempDir.resolve("source.mp3").toFile();
        File higherQualityFile = tempDir.resolve("source.flac").toFile();
        
        Files.write(sourceFile.toPath(), "content".getBytes());
        Files.write(higherQualityFile.toPath(), "content".getBytes());
        
        AudioConversionChecker.ConversionCheckResult result = checker.checkConversion(
            sourceFile.getAbsolutePath(), "flac", 320, 44100, 2
        );
        
        assertNotNull(result);
        assertTrue(result.isShouldSkip());
        assertEquals(AudioConversionChecker.SkipReason.HIGHER_QUALITY_EXISTS, result.getSkipReason());
    }

    @Test
    public void testAudioConversionChecker_ConversionRequired() throws IOException {
        AudioConversionChecker checker = new AudioConversionChecker();
        checker.setSkipIfTargetExists(false);
        checker.setOverwriteExisting(true);
        checker.setSkipIfSameFormat(false);
        checker.setSkipIfSameQuality(false);
        checker.setSkipIfHigherQualityExists(false);
        
        File sourceFile = tempDir.resolve("source.mp3").toFile();
        Files.write(sourceFile.toPath(), "content".getBytes());
        
        AudioConversionChecker.ConversionCheckResult result = checker.checkConversion(
            sourceFile.getAbsolutePath(), "flac", 320, 44100, 2
        );
        
        assertNotNull(result);
        assertFalse(result.isShouldSkip());
        assertEquals("Conversion required", result.getMessage());
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
    public void testBitrateOptions() {
        List<String> bitrateOptions = plugin.getBitrateOptions();
        assertNotNull(bitrateOptions);
        assertFalse(bitrateOptions.isEmpty());
        assertTrue(bitrateOptions.contains("64k"));
        assertTrue(bitrateOptions.contains("128k"));
        assertTrue(bitrateOptions.contains("192k"));
        assertTrue(bitrateOptions.contains("256k"));
        assertTrue(bitrateOptions.contains("320k"));
    }

    @Test
    public void testGenerateTargetPath() throws IOException {
        File sourceFile = tempDir.resolve("source.mp3").toFile();
        Files.write(sourceFile.toPath(), "content".getBytes());
        
        String targetPath = plugin.generateTargetPath(sourceFile.getAbsolutePath(), "flac");
        assertNotNull(targetPath);
        assertTrue(targetPath.endsWith(".flac"));
        assertFalse(targetPath.endsWith(".mp3"));
    }

    @Test
    public void testGenerateTargetPath_WithOutputDir() throws IOException {
        File sourceFile = tempDir.resolve("source.mp3").toFile();
        File outputDir = tempDir.resolve("output").toFile();
        outputDir.mkdirs();
        
        Files.write(sourceFile.toPath(), "content".getBytes());
        
        config.setValue("outputDir", outputDir.getAbsolutePath());
        String targetPath = plugin.generateTargetPath(sourceFile.getAbsolutePath(), "flac");
        
        assertNotNull(targetPath);
        assertTrue(targetPath.startsWith(outputDir.getAbsolutePath()));
        assertTrue(targetPath.endsWith(".flac"));
    }

    @Test
    public void testValidateInputFile() throws IOException {
        File validFile = tempDir.resolve("valid.mp3").toFile();
        Files.write(validFile.toPath(), "content".getBytes());
        
        boolean isValid = plugin.validateInputFile(validFile.getAbsolutePath());
        assertTrue(isValid);
    }

    @Test
    public void testValidateInputFile_NonExistent() {
        boolean isValid = plugin.validateInputFile("/nonexistent/file.mp3");
        assertFalse(isValid);
    }

    @Test
    public void testValidateInputFile_InvalidExtension() throws IOException {
        File invalidFile = tempDir.resolve("invalid.txt").toFile();
        Files.write(invalidFile.toPath(), "content".getBytes());
        
        boolean isValid = plugin.validateInputFile(invalidFile.getAbsolutePath());
        assertFalse(isValid);
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
    public void testIsSupportedBitrate() {
        assertTrue(plugin.isSupportedBitrate("64k"));
        assertTrue(plugin.isSupportedBitrate("128k"));
        assertTrue(plugin.isSupportedBitrate("192k"));
        assertTrue(plugin.isSupportedBitrate("256k"));
        assertTrue(plugin.isSupportedBitrate("320k"));
        assertFalse(plugin.isSupportedBitrate("48k"));
        assertFalse(plugin.isSupportedBitrate("512k"));
    }

    @Test
    public void testExtractFileInfo() throws IOException {
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "content".getBytes());
        
        AudioConverterPlugin.FileInfo info = plugin.extractFileInfo(testFile.getAbsolutePath());
        assertNotNull(info);
        assertEquals("mp3", info.getFormat());
        assertEquals(testFile.length(), info.getSize());
    }

    @Test
    public void testExtractFileInfo_NonExistent() {
        AudioConverterPlugin.FileInfo info = plugin.extractFileInfo("/nonexistent/file.mp3");
        assertNull(info);
    }

    @Test
    public void testCreateOutputDirectory() throws IOException {
        String outputDir = tempDir.resolve("output/dir/structure").toString();
        boolean created = plugin.createOutputDirectory(outputDir);
        
        assertTrue(created);
        assertTrue(new File(outputDir).exists());
        assertTrue(new File(outputDir).isDirectory());
    }

    @Test
    public void testCreateOutputDirectory_Existing() throws IOException {
        String outputDir = tempDir.resolve("existing").toString();
        new File(outputDir).mkdirs();
        
        boolean created = plugin.createOutputDirectory(outputDir);
        assertTrue(created);
        assertTrue(new File(outputDir).exists());
    }

    @Test
    public void testConversionWithDifferentBitrates() throws IOException {
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "content".getBytes());
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());
        
        String[] bitrates = {"64k", "128k", "192k", "256k", "320k"};
        
        for (String bitrate : bitrates) {
            config.setValue("outputFormat", "mp3");
            config.setValue("bitrate", bitrate);
            
            List<ChangeRecord> records = plugin.execute(filePaths, config, context);
            assertNotNull(records);
            assertEquals(1, records.size());
        }
    }

    @Test
    public void testConversionWithDifferentFormats() throws IOException {
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "content".getBytes());
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());
        
        String[] formats = {"flac", "wav", "ogg", "aac"};
        
        for (String format : formats) {
            config.setValue("outputFormat", format);
            config.setValue("bitrate", "320k");
            
            List<ChangeRecord> records = plugin.execute(filePaths, config, context);
            assertNotNull(records);
            assertEquals(1, records.size());
        }
    }

    @Test
    public void testBatchConversion() throws IOException {
        File file1 = tempDir.resolve("file1.mp3").toFile();
        File file2 = tempDir.resolve("file2.mp3").toFile();
        File file3 = tempDir.resolve("file3.mp3").toFile();
        
        Files.write(file1.toPath(), "content1".getBytes());
        Files.write(file2.toPath(), "content2".getBytes());
        Files.write(file3.toPath(), "content3".getBytes());
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(file1.getAbsolutePath());
        filePaths.add(file2.getAbsolutePath());
        filePaths.add(file3.getAbsolutePath());
        
        config.setValue("outputFormat", "flac");
        
        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(3, records.size());
    }
}

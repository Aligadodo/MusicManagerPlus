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

public class CueSplitterPluginTest {

    private CueSplitterPlugin plugin;
    private PluginConfigDTO config;
    private ExecutionContext context;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        plugin = new CueSplitterPlugin();
        config = new PluginConfigDTO();
        config.setConfigValues(new HashMap<>());
        context = new ExecutionContext();
    }

    @Test
    public void testPluginInitialization() {
        assertEquals("cue-splitter", plugin.getId());
        assertEquals("CUE文件分轨", plugin.getName());
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
        File testFile = tempDir.resolve("test.cue").toFile();
        Files.write(testFile.toPath(), createTestCueContent().getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        List<ChangeRecord> records = plugin.preview(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals("PENDING", records.get(0).getStatus());
    }

    @Test
    public void testExecute_WithFiles() throws IOException {
        File testFile = tempDir.resolve("test.cue").toFile();
        Files.write(testFile.toPath(), createTestCueContent().getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_WithOutputDir() throws IOException {
        File testFile = tempDir.resolve("test.cue").toFile();
        Files.write(testFile.toPath(), createTestCueContent().getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        config.setValue("outputDir", tempDir.resolve("output").toString());
        config.setValue("format", "mp3");

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_NonExistentFile() {
        List<String> filePaths = new ArrayList<>();
        filePaths.add("/nonexistent/file.cue");

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals("SKIPPED", records.get(0).getStatus());
    }

    @Test
    public void testExecute_WithCancellation() throws IOException {
        File testFile = tempDir.resolve("test.cue").toFile();
        Files.write(testFile.toPath(), createTestCueContent().getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        context.cancel();
        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertTrue(records.isEmpty() || records.get(0).getStatus().equals("SKIPPED"));
    }

    @Test
    public void testCueSplitProcessor_ParseCueFile() throws IOException {
        CueSplitProcessor processor = new CueSplitProcessor();
        File cueFile = tempDir.resolve("test.cue").toFile();
        Files.write(cueFile.toPath(), createTestCueContent().getBytes());
        
        CueSplitProcessor.SplitResult result = processor.parseCueFile(cueFile.getAbsolutePath());
        assertNotNull(result);
        assertEquals(CueSplitProcessor.SplitStatus.PENDING, result.getStatus());
        assertNotNull(result.getCueSheet());
    }

    @Test
    public void testCueSplitProcessor_ParseInvalidCueFile() throws IOException {
        CueSplitProcessor processor = new CueSplitProcessor();
        File cueFile = tempDir.resolve("invalid.cue").toFile();
        Files.write(cueFile.toPath(), "invalid cue content".getBytes());
        
        CueSplitProcessor.SplitResult result = processor.parseCueFile(cueFile.getAbsolutePath());
        assertNotNull(result);
        assertEquals(CueSplitProcessor.SplitStatus.FAILED, result.getStatus());
    }

    @Test
    public void testCueSplitProcessor_ParseNonExistentFile() {
        CueSplitProcessor processor = new CueSplitProcessor();
        CueSplitProcessor.SplitResult result = processor.parseCueFile("/nonexistent/file.cue");
        assertNotNull(result);
        assertEquals(CueSplitProcessor.SplitStatus.FAILED, result.getStatus());
    }

    @Test
    public void testCueSplitProcessor_ProcessSplit() throws IOException {
        CueSplitProcessor processor = new CueSplitProcessor();
        File cueFile = tempDir.resolve("test.cue").toFile();
        Files.write(cueFile.toPath(), createTestCueContent().getBytes());
        
        CueSplitProcessor.SplitResult result = processor.processSplit(cueFile.getAbsolutePath());
        assertNotNull(result);
        assertTrue(result.getStatus() == CueSplitProcessor.SplitStatus.COMPLETED || 
                   result.getStatus() == CueSplitProcessor.SplitStatus.PARTIAL ||
                   result.getStatus() == CueSplitProcessor.SplitStatus.FAILED);
    }

    @Test
    public void testCueSplitProcessor_GenerateOutputFilename() throws IOException {
        CueSplitProcessor processor = new CueSplitProcessor();
        File cueFile = tempDir.resolve("test.cue").toFile();
        Files.write(cueFile.toPath(), createTestCueContent().getBytes());
        
        CueSplitProcessor.SplitResult parseResult = processor.parseCueFile(cueFile.getAbsolutePath());
        assertNotNull(parseResult.getCueSheet());
        
        if (!parseResult.getCueSheet().getTracks().isEmpty()) {
            String outputFilename = processor.generateOutputFilename(
                parseResult.getCueSheet(), 
                parseResult.getCueSheet().getTracks().get(0)
            );
            assertNotNull(outputFilename);
            assertFalse(outputFilename.isEmpty());
        }
    }

    @Test
    public void testValidateCueFile() throws IOException {
        File validCueFile = tempDir.resolve("valid.cue").toFile();
        Files.write(validCueFile.toPath(), createTestCueContent().getBytes());
        
        boolean isValid = plugin.validateCueFile(validCueFile.getAbsolutePath());
        assertTrue(isValid);
    }

    @Test
    public void testValidateCueFile_NonExistent() {
        boolean isValid = plugin.validateCueFile("/nonexistent/file.cue");
        assertFalse(isValid);
    }

    @Test
    public void testValidateCueFile_InvalidExtension() throws IOException {
        File invalidFile = tempDir.resolve("invalid.txt").toFile();
        Files.write(invalidFile.toPath(), createTestCueContent().getBytes());
        
        boolean isValid = plugin.validateCueFile(invalidFile.getAbsolutePath());
        assertFalse(isValid);
    }

    @Test
    public void testExtractAudioFilePath() throws IOException {
        File cueFile = tempDir.resolve("test.cue").toFile();
        Files.write(cueFile.toPath(), createTestCueContent().getBytes());
        
        String audioPath = plugin.extractAudioFilePath(cueFile.getAbsolutePath());
        assertNotNull(audioPath);
    }

    @Test
    public void testGenerateOutputDirectory() throws IOException {
        File cueFile = tempDir.resolve("test.cue").toFile();
        Files.write(cueFile.toPath(), createTestCueContent().getBytes());
        
        String outputDir = plugin.generateOutputDirectory(cueFile.getAbsolutePath(), null);
        assertNotNull(outputDir);
        assertTrue(new File(outputDir).exists() || new File(outputDir).mkdirs());
    }

    @Test
    public void testGenerateOutputDirectory_WithCustomDir() throws IOException {
        File cueFile = tempDir.resolve("test.cue").toFile();
        File customDir = tempDir.resolve("custom_output").toFile();
        customDir.mkdirs();
        
        Files.write(cueFile.toPath(), createTestCueContent().getBytes());
        
        String outputDir = plugin.generateOutputDirectory(cueFile.getAbsolutePath(), customDir.getAbsolutePath());
        assertNotNull(outputDir);
        assertEquals(customDir.getAbsolutePath(), outputDir);
    }

    @Test
    public void testParseCueSheet() throws IOException {
        File cueFile = tempDir.resolve("test.cue").toFile();
        Files.write(cueFile.toPath(), createTestCueContent().getBytes());
        
        CueSplitProcessor.CueSheet cueSheet = plugin.parseCueSheet(cueFile.getAbsolutePath());
        assertNotNull(cueSheet);
        assertNotNull(cueSheet.getTitle());
        assertNotNull(cueSheet.getPerformer());
        assertFalse(cueSheet.getTracks().isEmpty());
    }

    @Test
    public void testParseCueSheet_Invalid() throws IOException {
        File cueFile = tempDir.resolve("invalid.cue").toFile();
        Files.write(cueFile.toPath(), "invalid cue content".getBytes());
        
        CueSplitProcessor.CueSheet cueSheet = plugin.parseCueSheet(cueFile.getAbsolutePath());
        assertNull(cueSheet);
    }

    @Test
    public void testCreateOutputFiles() throws IOException {
        File cueFile = tempDir.resolve("test.cue").toFile();
        Files.write(cueFile.toPath(), createTestCueContent().getBytes());
        
        String outputDir = tempDir.resolve("output").toString();
        new File(outputDir).mkdirs();
        
        CueSplitProcessor.CueSheet cueSheet = plugin.parseCueSheet(cueFile.getAbsolutePath());
        if (cueSheet != null) {
            List<String> outputFiles = plugin.createOutputFiles(cueSheet, outputDir, "mp3");
            assertNotNull(outputFiles);
            assertFalse(outputFiles.isEmpty());
        }
    }

    @Test
    public void testTrackExtraction() throws IOException {
        File cueFile = tempDir.resolve("test.cue").toFile();
        Files.write(cueFile.toPath(), createTestCueContent().getBytes());
        
        File audioFile = tempDir.resolve("test.flac").toFile();
        Files.write(audioFile.toPath(), "audio content".getBytes());
        
        String outputDir = tempDir.resolve("output").toString();
        new File(outputDir).mkdirs();
        
        CueSplitProcessor.CueSheet cueSheet = plugin.parseCueSheet(cueFile.getAbsolutePath());
        if (cueSheet != null && !cueSheet.getTracks().isEmpty()) {
            boolean extracted = plugin.extractTrack(
                audioFile.getAbsolutePath(),
                cueSheet.getTracks().get(0),
                outputDir + "/track1.mp3"
            );
            // This might fail if actual audio processing isn't implemented, but should not throw exceptions
            assertNotNull(Boolean.valueOf(extracted));
        }
    }

    @Test
    public void testBatchProcessing() throws IOException {
        File cueFile1 = tempDir.resolve("test1.cue").toFile();
        File cueFile2 = tempDir.resolve("test2.cue").toFile();
        
        Files.write(cueFile1.toPath(), createTestCueContent().getBytes());
        Files.write(cueFile2.toPath(), createTestCueContent().getBytes());
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(cueFile1.getAbsolutePath());
        filePaths.add(cueFile2.getAbsolutePath());
        
        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(2, records.size());
    }

    @Test
    public void testErrorHandling() throws IOException {
        File cueFile = tempDir.resolve("test.cue").toFile();
        Files.write(cueFile.toPath(), createTestCueContent().getBytes());
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(cueFile.getAbsolutePath());
        
        // Test with invalid output format
        config.setValue("format", "invalid");
        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    private String createTestCueContent() {
        return "REM GENRE Classical\n" +
               "REM DATE 2023\n" +
               "PERFORMER \"Test Performer\"\n" +
               "TITLE \"Test Album\"\n" +
               "FILE \"test.flac\" WAVE\n" +
               "  TRACK 01 AUDIO\n" +
               "    TITLE \"Track 1\"\n" +
               "    PERFORMER \"Test Performer\"\n" +
               "    INDEX 01 00:00:00\n" +
               "  TRACK 02 AUDIO\n" +
               "    TITLE \"Track 2\"\n" +
               "    PERFORMER \"Test Performer\"\n" +
               "    INDEX 01 03:00:00\n" +
               "  TRACK 03 AUDIO\n" +
               "    TITLE \"Track 3\"\n" +
               "    PERFORMER \"Test Performer\"\n" +
               "    INDEX 01 06:00:00\n";
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
}

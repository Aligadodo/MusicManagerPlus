package com.filemanager.plugin.impl;

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
        config.setConfigMap(new HashMap<>());
        context = new ExecutionContext();
    }

    @Test
    public void testPluginInitialization() {
        assertEquals("cue-splitter", plugin.getId());
        assertEquals("CUE分轨", plugin.getName());
        assertNotNull(plugin.getParameters());
        assertFalse(plugin.getParameters().isEmpty());
    }

    @Test
    public void testGetDefaultConfig() {
        PluginConfigDTO defaultConfig = plugin.getDefaultConfig();
        assertNotNull(defaultConfig);
        assertNotNull(defaultConfig.getConfigMap());
    }

    @Test
    public void testPreview_EmptyFileList() {
        List<ChangeRecord> records = plugin.preview(new ArrayList<>(), config, context);
        assertNotNull(records);
        assertTrue(records.isEmpty());
    }

    @Test
    public void testPreview_WithCueFile() throws IOException {
        File cueFile = tempDir.resolve("test.cue").toFile();
        String cueContent = "TITLE \"Test Album\"\n" +
                         "PERFORMER \"Test Artist\"\n" +
                         "FILE \"audio.wav\" WAVE\n" +
                         "  TRACK 01 AUDIO\n" +
                         "    TITLE \"Track 1\"\n" +
                         "    INDEX 01 00:00:00\n" +
                         "  TRACK 02 AUDIO\n" +
                         "    TITLE \"Track 2\"\n" +
                         "    INDEX 01 03:00:00\n";
        Files.write(cueFile.toPath(), cueContent.getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(cueFile.getAbsolutePath());

        List<ChangeRecord> records = plugin.preview(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals("PENDING", records.get(0).getStatus());
    }

    @Test
    public void testExecute_WithValidCueFile() throws IOException {
        File cueFile = tempDir.resolve("test.cue").toFile();
        File audioFile = tempDir.resolve("audio.wav").toFile();
        
        String cueContent = "TITLE \"Test Album\"\n" +
                         "PERFORMER \"Test Artist\"\n" +
                         "FILE \"audio.wav\" WAVE\n" +
                         "  TRACK 01 AUDIO\n" +
                         "    TITLE \"Track 1\"\n" +
                         "    INDEX 01 00:00:00\n" +
                         "  TRACK 02 AUDIO\n" +
                         "    TITLE \"Track 2\"\n" +
                         "    INDEX 01 03:00:00\n";
        Files.write(cueFile.toPath(), cueContent.getBytes());
        Files.write(audioFile.toPath(), new byte[1024]);

        List<String> filePaths = new ArrayList<>();
        filePaths.add(cueFile.getAbsolutePath());

        config.setValue("outputPath", tempDir.resolve("output").toString());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertFalse(records.isEmpty());
    }

    @Test
    public void testExecute_WithWAVFormat() throws IOException {
        File cueFile = tempDir.resolve("test.cue").toFile();
        File audioFile = tempDir.resolve("audio.wav").toFile();
        
        String cueContent = "TITLE \"Test Album\"\n" +
                         "PERFORMER \"Test Artist\"\n" +
                         "FILE \"audio.wav\" WAVE\n" +
                         "  TRACK 01 AUDIO\n" +
                         "    TITLE \"Track 1\"\n" +
                         "    INDEX 01 00:00:00\n";
        Files.write(cueFile.toPath(), cueContent.getBytes());
        Files.write(audioFile.toPath(), new byte[1024]);

        List<String> filePaths = new ArrayList<>();
        filePaths.add(cueFile.getAbsolutePath());

        config.setValue("targetFormat", "WAV (CD标准)");
        config.setValue("outputPath", tempDir.resolve("output").toString());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertFalse(records.isEmpty());
    }

    @Test
    public void testExecute_WithFLACFormat() throws IOException {
        File cueFile = tempDir.resolve("test.cue").toFile();
        File audioFile = tempDir.resolve("audio.wav").toFile();
        
        String cueContent = "TITLE \"Test Album\"\n" +
                         "PERFORMER \"Test Artist\"\n" +
                         "FILE \"audio.wav\" WAVE\n" +
                         "  TRACK 01 AUDIO\n" +
                         "    TITLE \"Track 1\"\n" +
                         "    INDEX 01 00:00:00\n";
        Files.write(cueFile.toPath(), cueContent.getBytes());
        Files.write(audioFile.toPath(), new byte[1024]);

        List<String> filePaths = new ArrayList<>();
        filePaths.add(cueFile.getAbsolutePath());

        config.setValue("targetFormat", "FLAC");
        config.setValue("outputPath", tempDir.resolve("output").toString());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertFalse(records.isEmpty());
    }

    @Test
    public void testExecute_WithSubdirectoryMode() throws IOException {
        File cueFile = tempDir.resolve("test.cue").toFile();
        File audioFile = tempDir.resolve("audio.wav").toFile();
        
        String cueContent = "TITLE \"Test Album\"\n" +
                         "PERFORMER \"Test Artist\"\n" +
                         "FILE \"audio.wav\" WAVE\n" +
                         "  TRACK 01 AUDIO\n" +
                         "    TITLE \"Track 1\"\n" +
                         "    INDEX 01 00:00:00\n";
        Files.write(cueFile.toPath(), cueContent.getBytes());
        Files.write(audioFile.toPath(), new byte[1024]);

        List<String> filePaths = new ArrayList<>();
        filePaths.add(cueFile.getAbsolutePath());

        config.setValue("outputDirMode", "子目录");
        config.setValue("outputPath", "Split - WAV");

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertFalse(records.isEmpty());
    }

    @Test
    public void testExecute_WithCustomSampleRate() throws IOException {
        File cueFile = tempDir.resolve("test.cue").toFile();
        File audioFile = tempDir.resolve("audio.wav").toFile();
        
        String cueContent = "TITLE \"Test Album\"\n" +
                         "PERFORMER \"Test Artist\"\n" +
                         "FILE \"audio.wav\" WAVE\n" +
                         "  TRACK 01 AUDIO\n" +
                         "    TITLE \"Track 1\"\n" +
                         "    INDEX 01 00:00:00\n";
        Files.write(cueFile.toPath(), cueContent.getBytes());
        Files.write(audioFile.toPath(), new byte[1024]);

        List<String> filePaths = new ArrayList<>();
        filePaths.add(cueFile.getAbsolutePath());

        config.setValue("sampleRate", "48000");
        config.setValue("outputPath", tempDir.resolve("output").toString());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertFalse(records.isEmpty());
    }

    @Test
    public void testExecute_WithAfterSplitActionDelete() throws IOException {
        File cueFile = tempDir.resolve("test.cue").toFile();
        File audioFile = tempDir.resolve("audio.wav").toFile();
        
        String cueContent = "TITLE \"Test Album\"\n" +
                         "PERFORMER \"Test Artist\"\n" +
                         "FILE \"audio.wav\" WAVE\n" +
                         "  TRACK 01 AUDIO\n" +
                         "    TITLE \"Track 1\"\n" +
                         "    INDEX 01 00:00:00\n";
        Files.write(cueFile.toPath(), cueContent.getBytes());
        Files.write(audioFile.toPath(), new byte[1024]);

        List<String> filePaths = new ArrayList<>();
        filePaths.add(cueFile.getAbsolutePath());

        config.setValue("afterSplitAction", "删除原始文件");
        config.setValue("outputPath", tempDir.resolve("output").toString());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertFalse(records.isEmpty());
    }

    @Test
    public void testExecute_WithAfterSplitActionArchive() throws IOException {
        File cueFile = tempDir.resolve("test.cue").toFile();
        File audioFile = tempDir.resolve("audio.wav").toFile();
        File archiveDir = tempDir.resolve("archive").toFile();
        archiveDir.mkdirs();
        
        String cueContent = "TITLE \"Test Album\"\n" +
                         "PERFORMER \"Test Artist\"\n" +
                         "FILE \"audio.wav\" WAVE\n" +
                         "  TRACK 01 AUDIO\n" +
                         "    TITLE \"Track 1\"\n" +
                         "    INDEX 01 00:00:00\n";
        Files.write(cueFile.toPath(), cueContent.getBytes());
        Files.write(audioFile.toPath(), new byte[1024]);

        List<String> filePaths = new ArrayList<>();
        filePaths.add(cueFile.getAbsolutePath());

        config.setValue("afterSplitAction", "归档原始文件");
        config.setValue("enableArchive", true);
        config.setValue("archiveDir", archiveDir.getAbsolutePath());
        config.setValue("outputPath", tempDir.resolve("output").toString());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertFalse(records.isEmpty());
    }

    @Test
    public void testExecute_NonExistentCueFile() {
        List<String> filePaths = new ArrayList<>();
        filePaths.add("/nonexistent/test.cue");

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals("SKIPPED", records.get(0).getStatus());
    }

    @Test
    public void testExecute_NonCueFile() throws IOException {
        File testFile = tempDir.resolve("test.txt").toFile();
        Files.write(testFile.toPath(), "not a cue file".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals("SKIPPED", records.get(0).getStatus());
    }

    @Test
    public void testExecute_WithCancellation() throws IOException {
        File cueFile = tempDir.resolve("test.cue").toFile();
        File audioFile = tempDir.resolve("audio.wav").toFile();
        
        String cueContent = "TITLE \"Test Album\"\n" +
                         "PERFORMER \"Test Artist\"\n" +
                         "FILE \"audio.wav\" WAVE\n" +
                         "  TRACK 01 AUDIO\n" +
                         "    TITLE \"Track 1\"\n" +
                         "    INDEX 01 00:00:00\n";
        Files.write(cueFile.toPath(), cueContent.getBytes());
        Files.write(audioFile.toPath(), new byte[1024]);

        List<String> filePaths = new ArrayList<>();
        filePaths.add(cueFile.getAbsolutePath());

        context.cancel();
        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertTrue(records.isEmpty() || records.get(0).getStatus().equals("SKIPPED"));
    }

    @Test
    public void testExecute_WithTempSuffix() throws IOException {
        File cueFile = tempDir.resolve("test.cue").toFile();
        File audioFile = tempDir.resolve("audio.wav").toFile();
        
        String cueContent = "TITLE \"Test Album\"\n" +
                         "PERFORMER \"Test Artist\"\n" +
                         "FILE \"audio.wav\" WAVE\n" +
                         "  TRACK 01 AUDIO\n" +
                         "    TITLE \"Track 1\"\n" +
                         "    INDEX 01 00:00:00\n";
        Files.write(cueFile.toPath(), cueContent.getBytes());
        Files.write(audioFile.toPath(), new byte[1024]);

        List<String> filePaths = new ArrayList<>();
        filePaths.add(cueFile.getAbsolutePath());

        config.setValue("enableTempSuffix", true);
        config.setValue("outputPath", tempDir.resolve("output").toString());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertFalse(records.isEmpty());
    }

    @Test
    public void testExecute_WithAutoFormatFilename() throws IOException {
        File cueFile = tempDir.resolve("test.cue").toFile();
        File audioFile = tempDir.resolve("audio.wav").toFile();
        
        String cueContent = "TITLE \"Test Album\"\n" +
                         "PERFORMER \"Test Artist\"\n" +
                         "FILE \"audio.wav\" WAVE\n" +
                         "  TRACK 01 AUDIO\n" +
                         "    TITLE \"Track 1\"\n" +
                         "    INDEX 01 00:00:00\n";
        Files.write(cueFile.toPath(), cueContent.getBytes());
        Files.write(audioFile.toPath(), new byte[1024]);

        List<String> filePaths = new ArrayList<>();
        filePaths.add(cueFile.getAbsolutePath());

        config.setValue("autoFormatFilename", true);
        config.setValue("outputPath", tempDir.resolve("output").toString());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertFalse(records.isEmpty());
    }
}

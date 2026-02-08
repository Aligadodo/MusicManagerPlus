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

public class FileMigratePluginTest {

    private FileMigratePlugin plugin;
    private PluginConfigDTO config;
    private ExecutionContext context;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        plugin = new FileMigratePlugin();
        config = new PluginConfigDTO();
        config.setConfigValues(new HashMap<>());
        context = new ExecutionContext();
    }

    @Test
    public void testPluginInitialization() {
        assertEquals("file-migrate", plugin.getId());
        assertEquals("文件批量归档和移动", plugin.getName());
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
        File testFile = tempDir.resolve("test.txt").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        List<ChangeRecord> records = plugin.preview(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals("PENDING", records.get(0).getStatus());
    }

    @Test
    public void testExecute_MoveOperation() throws IOException {
        File sourceFile = tempDir.resolve("source.txt").toFile();
        Files.write(sourceFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(sourceFile.getAbsolutePath());

        config.setValue("operationMode", "移动 (MOVE)");
        config.setValue("outputPath", tempDir.resolve("output").toString());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals("MOVE", records.get(0).getOperationType());
    }

    @Test
    public void testExecute_CopyOperation() throws IOException {
        File sourceFile = tempDir.resolve("source.txt").toFile();
        Files.write(sourceFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(sourceFile.getAbsolutePath());

        config.setValue("operationMode", "复制 (COPY)");
        config.setValue("outputPath", tempDir.resolve("output").toString());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals("COPY", records.get(0).getOperationType());
        assertTrue(sourceFile.exists());
    }

    @Test
    public void testExecute_WithStructureTemplate() throws IOException {
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        config.setValue("structureTemplate", "艺术家/专辑");
        config.setValue("outputPath", tempDir.resolve("output").toString());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_WithCustomTemplate() throws IOException {
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        config.setValue("structureTemplate", "自定义模板");
        config.setValue("customTemplate", "{artist}/{album}");
        config.setValue("outputPath", tempDir.resolve("output").toString());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_WithMetadataValidation() throws IOException {
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        config.setValue("validateMetadata", true);
        config.setValue("outputPath", tempDir.resolve("output").toString());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_WithTimestampPreservation() throws IOException {
        File sourceFile = tempDir.resolve("source.txt").toFile();
        Files.write(sourceFile.toPath(), "test content".getBytes());

        long originalTime = sourceFile.lastModified();

        List<String> filePaths = new ArrayList<>();
        filePaths.add(sourceFile.getAbsolutePath());

        config.setValue("preserveTimestamp", true);
        config.setValue("outputPath", tempDir.resolve("output").toString());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_WithPlaylistGeneration() throws IOException {
        File testFile = tempDir.resolve("test.mp3").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        config.setValue("generatePlaylist", true);
        config.setValue("playlistFormat", "M3U");
        config.setValue("outputPath", tempDir.resolve("output").toString());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_NonExistentFile() {
        List<String> filePaths = new ArrayList<>();
        filePaths.add("/nonexistent/file.txt");

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals("SKIPPED", records.get(0).getStatus());
    }

    @Test
    public void testExecute_WithFileConflict() throws IOException {
        File sourceFile = tempDir.resolve("source.txt").toFile();
        File targetDir = tempDir.resolve("output").toFile();
        targetDir.mkdirs();
        File targetFile = new File(targetDir, "source.txt");
        
        Files.write(sourceFile.toPath(), "source content".getBytes());
        Files.write(targetFile.toPath(), "target content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(sourceFile.getAbsolutePath());

        config.setValue("outputPath", targetDir.getAbsolutePath());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_WithCancellation() throws IOException {
        File testFile = tempDir.resolve("test.txt").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        context.cancel();
        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertTrue(records.isEmpty() || records.get(0).getStatus().equals("SKIPPED"));
    }

    @Test
    public void testExecute_WithSubdirectoryMode() throws IOException {
        File sourceFile = tempDir.resolve("source.txt").toFile();
        Files.write(sourceFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(sourceFile.getAbsolutePath());

        config.setValue("outputDirMode", "子目录");
        config.setValue("outputPath", "Archive");

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_WithSpecifiedDirectoryMode() throws IOException {
        File sourceFile = tempDir.resolve("source.txt").toFile();
        File targetDir = tempDir.resolve("output").toFile();
        targetDir.mkdirs();
        Files.write(sourceFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(sourceFile.getAbsolutePath());

        config.setValue("outputDirMode", "指定目录");
        config.setValue("outputPath", targetDir.getAbsolutePath());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }
}

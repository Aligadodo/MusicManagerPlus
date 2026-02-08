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

public class FileCleanupPluginTest {

    private FileCleanupPlugin plugin;
    private PluginConfigDTO config;
    private ExecutionContext context;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        plugin = new FileCleanupPlugin();
        config = new PluginConfigDTO();
        config.setConfigValues(new HashMap<>());
        context = new ExecutionContext();
    }

    @Test
    public void testPluginInitialization() {
        assertEquals("file-cleanup", plugin.getId());
        assertEquals("文件清理与去重", plugin.getName());
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
    public void testExecute_WithMD5Deduplication() throws IOException {
        File file1 = tempDir.resolve("file1.txt").toFile();
        File file2 = tempDir.resolve("file2.txt").toFile();
        
        String content = "duplicate content";
        Files.write(file1.toPath(), content.getBytes());
        Files.write(file2.toPath(), content.getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(file1.getAbsolutePath());
        filePaths.add(file2.getAbsolutePath());

        config.setValue("mode", "文件去重");
        config.setValue("method", "伪删除");

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(2, records.size());
    }

    @Test
    public void testExecute_WithFolderDeduplication() throws IOException {
        File folder1 = tempDir.resolve("folder1").toFile();
        File folder2 = tempDir.resolve("folder2").toFile();
        
        folder1.mkdirs();
        folder2.mkdirs();
        
        File file1 = new File(folder1, "file.txt");
        File file2 = new File(folder2, "file.txt");
        Files.write(file1.toPath(), "same content".getBytes());
        Files.write(file2.toPath(), "same content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(folder1.getAbsolutePath());
        filePaths.add(folder2.getAbsolutePath());

        config.setValue("mode", "文件夹去重");

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(2, records.size());
    }

    @Test
    public void testExecute_WithEmptyDirectoryCleanup() throws IOException {
        File emptyDir = tempDir.resolve("empty").toFile();
        emptyDir.mkdirs();

        List<String> filePaths = new ArrayList<>();
        filePaths.add(emptyDir.getAbsolutePath());

        config.setValue("mode", "清理空目录");

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_WithNestedEmptyDirectories() throws IOException {
        File parentDir = tempDir.resolve("parent").toFile();
        File childDir = new File(parentDir, "child");
        File grandChildDir = new File(childDir, "grandchild");
        
        parentDir.mkdirs();
        childDir.mkdirs();
        grandChildDir.mkdirs();

        List<String> filePaths = new ArrayList<>();
        filePaths.add(parentDir.getAbsolutePath());

        config.setValue("cleanupMode", "空目录清理");

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_WithNonEmptyDirectory() throws IOException {
        File dir = tempDir.resolve("nonempty").toFile();
        dir.mkdirs();
        
        File file = new File(dir, "file.txt");
        Files.write(file.toPath(), "content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(dir.getAbsolutePath());

        config.setValue("mode", "清理空目录");
        config.setValue("method", "直接删除");

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals("SKIPPED", records.get(0).getStatus());
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
    public void testExecute_WithKeepLargest() throws IOException {
        File file1 = tempDir.resolve("small.txt").toFile();
        File file2 = tempDir.resolve("large.txt").toFile();
        
        String content = "duplicate content";
        Files.write(file1.toPath(), content.getBytes());
        Files.write(file2.toPath(), (content + " extra").getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(file1.getAbsolutePath());
        filePaths.add(file2.getAbsolutePath());

        config.setValue("cleanupMode", "文件去重");
        config.setValue("deduplicationMethod", "MD5");
        config.setValue("keepLargest", true);

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(2, records.size());
    }

    @Test
    public void testExecute_WithKeepEarliest() throws IOException, InterruptedException {
        File file1 = tempDir.resolve("old.txt").toFile();
        File file2 = tempDir.resolve("new.txt").toFile();
        
        String content = "duplicate content";
        Files.write(file1.toPath(), content.getBytes());
        Files.write(file2.toPath(), content.getBytes());
        
        Thread.sleep(100);
        Files.setLastModifiedTime(file2.toPath(), java.nio.file.attribute.FileTime.fromMillis(
            java.nio.file.Files.getLastModifiedTime(file1.toPath()).toMillis() + 1000));

        List<String> filePaths = new ArrayList<>();
        filePaths.add(file1.getAbsolutePath());
        filePaths.add(file2.getAbsolutePath());

        config.setValue("cleanupMode", "文件去重");
        config.setValue("deduplicationMethod", "MD5");
        config.setValue("keepEarliest", true);

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(2, records.size());
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
}

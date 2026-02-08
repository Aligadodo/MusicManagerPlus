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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AdvancedRenamePluginTest {

    private AdvancedRenamePlugin plugin;
    private PluginConfigDTO config;
    private ExecutionContext context;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        plugin = new AdvancedRenamePlugin();
        config = new PluginConfigDTO();
        config.setConfigValues(new HashMap<>());
        context = new ExecutionContext();
    }

    @Test
    public void testPluginInitialization() {
        assertEquals("advanced-rename", plugin.getId());
        assertEquals("高级重命名策略", plugin.getName());
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
    public void testExecute_WithRenameAction() throws IOException {
        File testFile = tempDir.resolve("oldname.txt").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        List<Map<String, Object>> rules = new ArrayList<>();
        Map<String, Object> rule = new HashMap<>();
        rule.put("name", "Test Rule");
        rule.put("enabled", true);
        
        List<Map<String, Object>> conditions = new ArrayList<>();
        Map<String, Object> condition = new HashMap<>();
        condition.put("type", "文件名匹配");
        condition.put("operator", "包含");
        condition.put("value", "oldname");
        conditions.add(condition);
        rule.put("conditions", conditions);
        
        List<Map<String, Object>> actions = new ArrayList<>();
        Map<String, Object> action = new HashMap<>();
        action.put("type", "替换文本");
        Map<String, Object> replaceValue = new HashMap<>();
        replaceValue.put("searchText", "oldname");
        replaceValue.put("replaceText", "newname");
        action.put("value", replaceValue);
        actions.add(action);
        rule.put("actions", actions);
        
        rules.add(rule);
        
        config.setValue("rules", rules);

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals("RENAME", records.get(0).getOperationType());
    }

    @Test
    public void testExecute_WithPrefixAction() throws IOException {
        File testFile = tempDir.resolve("test.txt").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        List<Map<String, Object>> rules = new ArrayList<>();
        Map<String, Object> rule = new HashMap<>();
        rule.put("name", "Prefix Rule");
        rule.put("enabled", true);
        
        List<Map<String, Object>> actions = new ArrayList<>();
        Map<String, Object> action = new HashMap<>();
        action.put("type", "添加前缀");
        action.put("value", "prefix_");
        actions.add(action);
        rule.put("actions", actions);
        
        rules.add(rule);
        
        config.setValue("rules", rules);

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_WithSuffixAction() throws IOException {
        File testFile = tempDir.resolve("test.txt").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        List<Map<String, Object>> rules = new ArrayList<>();
        Map<String, Object> rule = new HashMap<>();
        rule.put("name", "Suffix Rule");
        rule.put("enabled", true);
        
        List<Map<String, Object>> actions = new ArrayList<>();
        Map<String, Object> action = new HashMap<>();
        action.put("type", "添加后缀");
        action.put("value", "_suffix");
        actions.add(action);
        rule.put("actions", actions);
        
        rules.add(rule);
        
        config.setValue("rules", rules);

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_WithUppercaseAction() throws IOException {
        File testFile = tempDir.resolve("test.txt").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        List<Map<String, Object>> rules = new ArrayList<>();
        Map<String, Object> rule = new HashMap<>();
        rule.put("name", "Uppercase Rule");
        rule.put("enabled", true);
        
        List<Map<String, Object>> actions = new ArrayList<>();
        Map<String, Object> action = new HashMap<>();
        action.put("type", "大小写转换");
        action.put("value", "全部大写");
        actions.add(action);
        rule.put("actions", actions);
        
        rules.add(rule);
        
        config.setValue("rules", rules);

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_WithLowercaseAction() throws IOException {
        File testFile = tempDir.resolve("TEST.txt").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        List<Map<String, Object>> rules = new ArrayList<>();
        Map<String, Object> rule = new HashMap<>();
        rule.put("name", "Lowercase Rule");
        rule.put("enabled", true);
        
        List<Map<String, Object>> actions = new ArrayList<>();
        Map<String, Object> action = new HashMap<>();
        action.put("type", "大小写转换");
        action.put("value", "全部小写");
        actions.add(action);
        rule.put("actions", actions);
        
        rules.add(rule);
        
        config.setValue("rules", rules);

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_WithRegexAction() throws IOException {
        File testFile = tempDir.resolve("test123.txt").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        List<Map<String, Object>> rules = new ArrayList<>();
        Map<String, Object> rule = new HashMap<>();
        rule.put("name", "Regex Rule");
        rule.put("enabled", true);
        
        List<Map<String, Object>> actions = new ArrayList<>();
        Map<String, Object> action = new HashMap<>();
        action.put("type", "正则替换");
        Map<String, Object> regexValue = new HashMap<>();
        regexValue.put("pattern", "(\\d+)");
        regexValue.put("replacement", "");
        action.put("value", regexValue);
        actions.add(action);
        rule.put("actions", actions);
        
        rules.add(rule);
        
        config.setValue("rules", rules);

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_WithMultipleConditions() throws IOException {
        File testFile = tempDir.resolve("test.txt").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        List<Map<String, Object>> rules = new ArrayList<>();
        Map<String, Object> rule = new HashMap<>();
        rule.put("name", "Multi-Condition Rule");
        rule.put("enabled", true);
        
        List<Map<String, Object>> conditions = new ArrayList<>();
        Map<String, Object> condition1 = new HashMap<>();
        condition1.put("type", "文件名匹配");
        condition1.put("operator", "包含");
        condition1.put("value", "test");
        conditions.add(condition1);
        
        Map<String, Object> condition2 = new HashMap<>();
        condition2.put("type", "文件扩展名");
        condition2.put("operator", "等于");
        condition2.put("value", "txt");
        conditions.add(condition2);
        
        rule.put("conditions", conditions);
        
        List<Map<String, Object>> actions = new ArrayList<>();
        Map<String, Object> action = new HashMap<>();
        action.put("type", "添加前缀");
        action.put("value", "prefix_");
        actions.add(action);
        rule.put("actions", actions);
        
        rules.add(rule);
        
        config.setValue("rules", rules);

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
    public void testExecute_WithDisabledRule() throws IOException {
        File testFile = tempDir.resolve("test.txt").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        List<Map<String, Object>> rules = new ArrayList<>();
        Map<String, Object> rule = new HashMap<>();
        rule.put("name", "Disabled Rule");
        rule.put("enabled", false);
        
        List<Map<String, Object>> actions = new ArrayList<>();
        Map<String, Object> action = new HashMap<>();
        action.put("type", "添加前缀");
        action.put("value", "prefix_");
        actions.add(action);
        rule.put("actions", actions);
        
        rules.add(rule);
        
        config.setValue("rules", rules);

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals("SKIPPED", records.get(0).getStatus());
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
    public void testExecute_WithCrossDriveOperation() throws IOException {
        File testFile = tempDir.resolve("test.txt").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        config.setValue("crossDriveMode", "复制");

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_WithProcessScopeFilesOnly() throws IOException {
        File testFile = tempDir.resolve("test.txt").toFile();
        File testDir = tempDir.resolve("testdir").toFile();
        testDir.mkdirs();
        
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());
        filePaths.add(testDir.getAbsolutePath());

        config.setValue("processScope", "仅处理文件");

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(2, records.size());
        assertEquals("SKIPPED", records.get(1).getStatus());
    }

    @Test
    public void testExecute_WithProcessScopeDirectoriesOnly() throws IOException {
        File testFile = tempDir.resolve("test.txt").toFile();
        File testDir = tempDir.resolve("testdir").toFile();
        testDir.mkdirs();
        
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());
        filePaths.add(testDir.getAbsolutePath());

        config.setValue("processScope", "仅处理文件夹");

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(2, records.size());
        assertEquals("SKIPPED", records.get(0).getStatus());
    }
}

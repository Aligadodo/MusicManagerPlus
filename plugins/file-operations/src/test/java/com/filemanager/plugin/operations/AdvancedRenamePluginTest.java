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
        assertEquals("高级文件重命名", plugin.getName());
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
    public void testExecute_WithFiles() throws IOException {
        File testFile = tempDir.resolve("test.txt").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_WithRules() throws IOException {
        File testFile = tempDir.resolve("oldname.txt").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        config.setValue("rules", List.of(
            Map.of("type", "replace", "find", "old", "replace", "new"),
            Map.of("type", "prefix", "prefix", "prefix_")
        ));

        List<ChangeRecord> records = plugin.execute(filePaths, config, context);
        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    public void testExecute_WithConditions() throws IOException {
        File testFile = tempDir.resolve("test.txt").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());

        List<String> filePaths = new ArrayList<>();
        filePaths.add(testFile.getAbsolutePath());

        config.setValue("conditions", List.of(
            Map.of("type", "extension", "value", ".txt")
        ));

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
    public void testRenameRuleEngine_Replace() {
        RenameRuleEngine engine = new RenameRuleEngine();
        engine.addRule(new RenameRuleEngine.RenameRule("replace", Map.of(
            "find", "test",
            "replace", "example"
        )));
        
        RenameRuleEngine.RuleResult result = engine.apply("test_file.txt");
        assertNotNull(result);
        assertEquals("example_file.txt", result.getNewName());
    }

    @Test
    public void testRenameRuleEngine_Prefix() {
        RenameRuleEngine engine = new RenameRuleEngine();
        engine.addRule(new RenameRuleEngine.RenameRule("prefix", Map.of(
            "prefix", "prefix_"
        )));
        
        RenameRuleEngine.RuleResult result = engine.apply("file.txt");
        assertNotNull(result);
        assertEquals("prefix_file.txt", result.getNewName());
    }

    @Test
    public void testRenameRuleEngine_Suffix() {
        RenameRuleEngine engine = new RenameRuleEngine();
        engine.addRule(new RenameRuleEngine.RenameRule("suffix", Map.of(
            "suffix", "_suffix"
        )));
        
        RenameRuleEngine.RuleResult result = engine.apply("file.txt");
        assertNotNull(result);
        assertEquals("file_suffix.txt", result.getNewName());
    }

    @Test
    public void testRenameRuleEngine_Remove() {
        RenameRuleEngine engine = new RenameRuleEngine();
        engine.addRule(new RenameRuleEngine.RenameRule("remove", Map.of(
            "text", "test"
        )));
        
        RenameRuleEngine.RuleResult result = engine.apply("test_file.txt");
        assertNotNull(result);
        assertEquals("_file.txt", result.getNewName());
    }

    @Test
    public void testRenameRuleEngine_CaseChange() {
        RenameRuleEngine engine = new RenameRuleEngine();
        engine.addRule(new RenameRuleEngine.RenameRule("case_change", Map.of(
            "case", "uppercase"
        )));
        
        RenameRuleEngine.RuleResult result = engine.apply("file.txt");
        assertNotNull(result);
        assertEquals("FILE.TXT", result.getNewName());
    }

    @Test
    public void testRenameRuleEngine_Numbering() {
        RenameRuleEngine engine = new RenameRuleEngine();
        engine.addRule(new RenameRuleEngine.RenameRule("numbering", Map.of(
            "start", 1,
            "format", "000"
        )));
        
        RenameRuleEngine.RuleResult result = engine.apply("file.txt");
        assertNotNull(result);
        assertEquals("001_file.txt", result.getNewName());
    }

    @Test
    public void testRenameRuleEngine_Metadata() {
        RenameRuleEngine engine = new RenameRuleEngine();
        engine.addRule(new RenameRuleEngine.RenameRule("metadata", Map.of(
            "pattern", "{artist} - {title}"
        )));
        
        Map<String, String> metadata = new HashMap<>();
        metadata.put("artist", "Artist");
        metadata.put("title", "Title");
        engine.setMetadata(metadata);
        
        RenameRuleEngine.RuleResult result = engine.apply("file.txt");
        assertNotNull(result);
        assertEquals("Artist - Title.txt", result.getNewName());
    }

    @Test
    public void testRenameRuleEngine_RegexReplace() {
        RenameRuleEngine engine = new RenameRuleEngine();
        engine.addRule(new RenameRuleEngine.RenameRule("regex_replace", Map.of(
            "pattern", "(test)(.*)",
            "replacement", "example$2"
        )));
        
        RenameRuleEngine.RuleResult result = engine.apply("test_file.txt");
        assertNotNull(result);
        assertEquals("example_file.txt", result.getNewName());
    }

    @Test
    public void testRenameConditionEvaluator_Extension() {
        RenameConditionEvaluator evaluator = new RenameConditionEvaluator();
        Map<String, Object> condition = Map.of(
            "type", "extension",
            "value", ".txt"
        );
        
        boolean matches = evaluator.evaluate("test.txt", condition);
        assertTrue(matches);
    }

    @Test
    public void testRenameConditionEvaluator_FileName() {
        RenameConditionEvaluator evaluator = new RenameConditionEvaluator();
        Map<String, Object> condition = Map.of(
            "type", "filename",
            "value", "test",
            "operator", "contains"
        );
        
        boolean matches = evaluator.evaluate("test_file.txt", condition);
        assertTrue(matches);
    }

    @Test
    public void testRenameConditionEvaluator_FileSize() throws IOException {
        RenameConditionEvaluator evaluator = new RenameConditionEvaluator();
        File testFile = tempDir.resolve("test.txt").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());
        
        Map<String, Object> condition = Map.of(
            "type", "filesize",
            "value", 10,
            "operator", ">="
        );
        
        boolean matches = evaluator.evaluate(testFile.getAbsolutePath(), condition);
        assertTrue(matches);
    }

    @Test
    public void testRenameConditionEvaluator_Combined() {
        RenameConditionEvaluator evaluator = new RenameConditionEvaluator();
        List<Map<String, Object>> conditions = List.of(
            Map.of("type", "extension", "value", ".txt"),
            Map.of("type", "filename", "value", "test", "operator", "contains")
        );
        
        boolean matches = evaluator.evaluateCombined("test.txt", conditions, "AND");
        assertTrue(matches);
    }

    @Test
    public void testRenameActionExecutor_Rename() throws IOException {
        File testFile = tempDir.resolve("oldname.txt").toFile();
        Files.write(testFile.toPath(), "content".getBytes());
        
        RenameActionExecutor executor = new RenameActionExecutor();
        boolean success = executor.executeAction("rename", testFile.getAbsolutePath(), "newname.txt");
        
        assertTrue(success);
        assertFalse(testFile.exists());
        assertTrue(new File(tempDir.resolve("newname.txt").toAbsolutePath().toString()).exists());
    }

    @Test
    public void testRenameActionExecutor_Copy() throws IOException {
        File testFile = tempDir.resolve("original.txt").toFile();
        Files.write(testFile.toPath(), "content".getBytes());
        
        RenameActionExecutor executor = new RenameActionExecutor();
        boolean success = executor.executeAction("copy", testFile.getAbsolutePath(), "copy.txt");
        
        assertTrue(success);
        assertTrue(testFile.exists());
        assertTrue(new File(tempDir.resolve("copy.txt").toAbsolutePath().toString()).exists());
    }

    @Test
    public void testRenameActionExecutor_Move() throws IOException {
        File testFile = tempDir.resolve("original.txt").toFile();
        Files.write(testFile.toPath(), "content".getBytes());
        
        File destDir = tempDir.resolve("dest").toFile();
        destDir.mkdirs();
        
        RenameActionExecutor executor = new RenameActionExecutor();
        boolean success = executor.executeAction("move", testFile.getAbsolutePath(), destDir.getAbsolutePath() + "/moved.txt");
        
        assertTrue(success);
        assertFalse(testFile.exists());
        assertTrue(new File(destDir, "moved.txt").exists());
    }

    @Test
    public void testRenameActionExecutor_Delete() throws IOException {
        File testFile = tempDir.resolve("to_delete.txt").toFile();
        Files.write(testFile.toPath(), "content".getBytes());
        
        RenameActionExecutor executor = new RenameActionExecutor();
        boolean success = executor.executeAction("delete", testFile.getAbsolutePath(), null);
        
        assertTrue(success);
        assertFalse(testFile.exists());
    }

    @Test
    public void testRuleChain() {
        RenameRuleEngine engine = new RenameRuleEngine();
        engine.addRule(new RenameRuleEngine.RenameRule("prefix", Map.of("prefix", "prefix_")));
        engine.addRule(new RenameRuleEngine.RenameRule("suffix", Map.of("suffix", "_suffix")));
        
        RenameRuleEngine.RuleResult result = engine.apply("file.txt");
        assertNotNull(result);
        assertEquals("prefix_file_suffix.txt", result.getNewName());
    }

    @Test
    public void testConditionChain() {
        RenameConditionEvaluator evaluator = new RenameConditionEvaluator();
        List<Map<String, Object>> conditions = List.of(
            Map.of("type", "extension", "value", ".txt"),
            Map.of("type", "filename", "value", "test", "operator", "contains"),
            Map.of("type", "filename", "value", "invalid", "operator", "not_contains")
        );
        
        boolean matches = evaluator.evaluateCombined("test_file.txt", conditions, "AND");
        assertTrue(matches);
    }

    @Test
    public void testInvalidRule() {
        RenameRuleEngine engine = new RenameRuleEngine();
        engine.addRule(new RenameRuleEngine.RenameRule("invalid_type", Map.of()));
        
        RenameRuleEngine.RuleResult result = engine.apply("file.txt");
        assertNotNull(result);
        assertEquals("file.txt", result.getNewName());
    }

    @Test
    public void testInvalidCondition() {
        RenameConditionEvaluator evaluator = new RenameConditionEvaluator();
        Map<String, Object> condition = Map.of(
            "type", "invalid_type",
            "value", "test"
        );
        
        boolean matches = evaluator.evaluate("test.txt", condition);
        assertFalse(matches);
    }

    @Test
    public void testPreserveExtension() {
        RenameRuleEngine engine = new RenameRuleEngine();
        engine.setPreserveExtension(true);
        engine.addRule(new RenameRuleEngine.RuleResult("replace", Map.of(
            "find", "file",
            "replace", "renamed"
        )));
        
        RenameRuleEngine.RuleResult result = engine.apply("file.txt");
        assertNotNull(result);
        assertEquals("renamed.txt", result.getNewName());
    }

    @Test
    public void testStopOnFirstMatch() {
        RenameRuleEngine engine = new RenameRuleEngine();
        engine.setStopOnFirstMatch(true);
        engine.addRule(new RenameRuleEngine.RuleResult("prefix", Map.of("prefix", "first_")));
        engine.addRule(new RenameRuleEngine.RuleResult("prefix", Map.of("prefix", "second_")));
        
        RenameRuleEngine.RuleResult result = engine.apply("file.txt");
        assertNotNull(result);
        assertEquals("first_file.txt", result.getNewName());
    }
}

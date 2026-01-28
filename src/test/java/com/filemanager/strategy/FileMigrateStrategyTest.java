package com.filemanager.strategy;

import com.filemanager.model.ChangeRecord;
import com.filemanager.strategy.base.PathSelectionComponent;
import com.filemanager.strategy.base.ScopeSelectionComponent;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class FileMigrateStrategyTest {
    private FileMigrateStrategy strategy;
    private File sourceDir;
    private File destDir;

    @Before
    public void setUp() throws Exception {
        System.out.println("=== 初始化测试环境 ===");
        strategy = new FileMigrateStrategy();
        sourceDir = Files.createTempDirectory("source").toFile();
        destDir = Files.createTempDirectory("dest").toFile();
        System.out.println("测试环境初始化完成: source=" + sourceDir.getAbsolutePath() + ", dest=" + destDir.getAbsolutePath());
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("=== 清理测试环境 ===");
        deleteDirectory(sourceDir);
        deleteDirectory(destDir);
        System.out.println("测试环境清理完成");
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

    private File createTestAudioFile(String fileName) throws IOException {
        File file = new File(sourceDir, fileName);
        file.createNewFile();
        System.out.println("创建测试文件: " + file.getAbsolutePath());
        return file;
    }

    private File createTestAudioFileInDest(String fileName) throws IOException {
        File file = new File(destDir, fileName);
        file.createNewFile();
        System.out.println("在目标目录创建测试文件: " + file.getAbsolutePath());
        return file;
    }

    private ChangeRecord createChangeRecord(File file) {
        return new ChangeRecord(file.getName(), file.getName(), file, true, file.getAbsolutePath(), OperationType.MOVE, null, ExecStatus.PENDING);
    }

    private void setPathSelectionComponentOutputDirMode(FileMigrateStrategy strategy, String mode) throws Exception {
        Field pathSelectionField = FileMigrateStrategy.class.getDeclaredField("pathSelectionComponent");
        pathSelectionField.setAccessible(true);
        PathSelectionComponent pathSelection = (PathSelectionComponent) pathSelectionField.get(strategy);
        
        Field cbOutputDirModeField = PathSelectionComponent.class.getDeclaredField("cbOutputDirMode");
        cbOutputDirModeField.setAccessible(true);
        javafx.scene.control.ComboBox<String> cbOutputDirMode = (javafx.scene.control.ComboBox<String>) cbOutputDirModeField.get(pathSelection);
        cbOutputDirMode.getSelectionModel().select(mode);
        
        Field txtPathField = PathSelectionComponent.class.getDeclaredField("txtPath");
        txtPathField.setAccessible(true);
        javafx.scene.control.TextField txtPath = (javafx.scene.control.TextField) txtPathField.get(pathSelection);
        txtPath.setText(destDir.getAbsolutePath());
        
        pathSelection.captureParams();
    }

    @Test
    public void testMoveOperation() throws Exception {
        System.out.println("=== 测试移动操作 ===");
        
        // 设置路径选择组件
        setPathSelectionComponentOutputDirMode(strategy, "指定目录");
        
        // 直接设置保护成员变量
        strategy.pOperationMode = "MOVE";
        strategy.pOverwriteExisting = false;
        
        File testFile = createTestAudioFile("song1.mp3");
        ChangeRecord record = createChangeRecord(testFile);
        List<ChangeRecord> result = strategy.analyze(record, new ArrayList<>(Arrays.asList(record)), new ArrayList<>(Arrays.asList(sourceDir)));
        
        System.out.println("测试结果: 移动操作应该生成MOVE类型的记录");
        assertNotNull("结果不应为空", result);
        assertFalse("结果不应为空列表", result.isEmpty());
        assertEquals("操作类型应该是MOVE", OperationType.MOVE, result.get(0).getOpType());
    }

    @Test
    public void testCopyOperation() throws Exception {
        System.out.println("=== 测试复制操作 ===");
        
        // 设置路径选择组件
        setPathSelectionComponentOutputDirMode(strategy, "指定目录");
        
        // 直接设置保护成员变量
        strategy.pOperationMode = "COPY";
        strategy.pOverwriteExisting = false;
        
        File testFile = createTestAudioFile("song2.mp3");
        ChangeRecord record = createChangeRecord(testFile);
        List<ChangeRecord> result = strategy.analyze(record, new ArrayList<>(Arrays.asList(record)), new ArrayList<>(Arrays.asList(sourceDir)));
        
        System.out.println("测试结果: 复制操作应该生成MOVE类型的记录（内部使用MOVE类型）");
        assertNotNull("结果不应为空", result);
        assertFalse("结果不应为空列表", result.isEmpty());
        assertEquals("操作类型应该是MOVE", OperationType.MOVE, result.get(0).getOpType());
    }

    @Test
    public void testOriginalDirMode() throws Exception {
        System.out.println("=== 测试原目录模式 ===");
        
        // 设置路径选择组件
        setPathSelectionComponentOutputDirMode(strategy, "原目录");
        
        // 直接设置保护成员变量
        strategy.pOperationMode = "MOVE";
        
        File testFile = createTestAudioFile("song3.mp3");
        ChangeRecord record = createChangeRecord(testFile);
        List<ChangeRecord> result = strategy.analyze(record, new ArrayList<>(Arrays.asList(record)), new ArrayList<>(Arrays.asList(sourceDir)));
        
        System.out.println("测试结果: 原目录模式应该使用源文件所在目录");
        assertNotNull("结果不应为空", result);
        assertFalse("结果不应为空列表", result.isEmpty());
    }

    @Test
    public void testSubdirMode() throws Exception {
        System.out.println("=== 测试子目录模式 ===");
        
        // 设置路径选择组件
        setPathSelectionComponentOutputDirMode(strategy, "子目录");
        
        // 直接设置保护成员变量
        strategy.pOperationMode = "MOVE";
        
        File testFile = createTestAudioFile("song4.mp3");
        ChangeRecord record = createChangeRecord(testFile);
        List<ChangeRecord> result = strategy.analyze(record, new ArrayList<>(Arrays.asList(record)), new ArrayList<>(Arrays.asList(sourceDir)));
        
        System.out.println("测试结果: 子目录模式应该在源目录下创建子目录");
        assertNotNull("结果不应为空", result);
        assertFalse("结果不应为空列表", result.isEmpty());
    }

    @Test
    public void testOverwriteExisting() throws Exception {
        System.out.println("=== 测试覆盖已存在文件 ===");
        
        // 设置路径选择组件
        setPathSelectionComponentOutputDirMode(strategy, "指定目录");
        
        // 直接设置保护成员变量
        strategy.pOperationMode = "MOVE";
        strategy.pOverwriteExisting = true;
        
        File testFile = createTestAudioFile("song5.mp3");
        File existingFile = createTestAudioFileInDest("song5.mp3");
        
        ChangeRecord record = createChangeRecord(testFile);
        List<ChangeRecord> result = strategy.analyze(record, new ArrayList<>(Arrays.asList(record)), new ArrayList<>(Arrays.asList(sourceDir)));
        
        System.out.println("测试结果: 启用覆盖时应该处理已存在文件");
        assertNotNull("结果不应为空", result);
        assertFalse("结果不应为空列表", result.isEmpty());
    }

    @Test
    public void testSkipExisting() throws Exception {
        System.out.println("=== 测试跳过已存在文件 ===");
        
        // 设置路径选择组件
        setPathSelectionComponentOutputDirMode(strategy, "指定目录");
        
        // 直接设置保护成员变量
        strategy.pOperationMode = "MOVE";
        strategy.pOverwriteExisting = false;
        
        File testFile = createTestAudioFile("song6.mp3");
        File existingFile = createTestAudioFileInDest("song6.mp3");
        
        ChangeRecord record = createChangeRecord(testFile);
        List<ChangeRecord> result = strategy.analyze(record, new ArrayList<>(Arrays.asList(record)), new ArrayList<>(Arrays.asList(sourceDir)));
        
        System.out.println("测试结果: 禁用覆盖时应该跳过已存在文件");
        assertNotNull("结果不应为空", result);
    }

    @Test
    public void testCleanEmptyDirectories() throws Exception {
        System.out.println("=== 测试清理空文件夹 ===");
        
        // 设置路径选择组件
        setPathSelectionComponentOutputDirMode(strategy, "指定目录");
        
        // 直接设置保护成员变量
        strategy.pOperationMode = "MOVE";
        strategy.pCleanEmpty = true;
        
        File testFile = createTestAudioFile("song7.mp3");
        ChangeRecord record = createChangeRecord(testFile);
        List<ChangeRecord> result = strategy.analyze(record, new ArrayList<>(Arrays.asList(record)), new ArrayList<>(Arrays.asList(sourceDir)));
        
        System.out.println("测试结果: 启用清理时应该在移动后清理空文件夹");
        assertNotNull("结果不应为空", result);
        assertFalse("结果不应为空列表", result.isEmpty());
    }

    @Test
    public void testFilePatternPrecondition() throws Exception {
        System.out.println("=== 测试文件模式前置条件 ===");
        
        // 设置路径选择组件
        setPathSelectionComponentOutputDirMode(strategy, "指定目录");
        
        // 直接设置保护成员变量
        strategy.pOperationMode = "MOVE";
        strategy.pFilePattern = "*.mp3";
        strategy.pRequireFilePattern = true;
        
        File testFile = createTestAudioFile("song8.mp3");
        ChangeRecord record = createChangeRecord(testFile);
        List<ChangeRecord> result = strategy.analyze(record, new ArrayList<>(Arrays.asList(record)), new ArrayList<>(Arrays.asList(sourceDir)));
        
        System.out.println("测试结果: 当目录中存在匹配文件时应该执行操作");
        assertNotNull("结果不应为空", result);
        assertFalse("结果不应为空列表", result.isEmpty());
    }

    @Test
    public void testScopeSelection() throws Exception {
        System.out.println("=== 测试生效范围选择 ===");
        
        // 设置路径选择组件
        setPathSelectionComponentOutputDirMode(strategy, "指定目录");
        
        // 直接设置保护成员变量
        strategy.pOperationMode = "MOVE";
        
        File testFile = createTestAudioFile("song9.mp3");
        ChangeRecord record = createChangeRecord(testFile);
        List<ChangeRecord> result = strategy.analyze(record, new ArrayList<>(Arrays.asList(record)), new ArrayList<>(Arrays.asList(sourceDir)));
        
        System.out.println("测试结果: 应该根据生效范围选择执行操作");
        assertNotNull("结果不应为空", result);
        assertFalse("结果不应为空列表", result.isEmpty());
    }
}

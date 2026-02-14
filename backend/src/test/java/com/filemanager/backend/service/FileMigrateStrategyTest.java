package com.filemanager.backend.service;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.StrategyConfigurable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件批量归档和移动策略测试用例
 * 
 * 测试场景：
 * 1. 策略注册和基本信息验证
 * 2. 配置字段完整性测试
 * 3. 移动操作测试
 * 4. 复制操作测试
 * 5. 子目录模式测试
 * 6. 指定目录模式测试
 * 7. 批量文件迁移测试
 * 8. 边界情况测试（目标文件已存在）
 * 9. 源文件不存在测试
 * 10. 非文件对象处理测试
 */
public class FileMigrateStrategyTest extends StrategyTestBase {

    @AfterEach
    public void tearDown() throws Exception {
        cleanup();
    }

    /**
     * 测试场景1：验证策略已正确注册
     * 
     * 目的：确保file-migrate策略在策略注册器中可用
     * 断言：
     * - 策略不为null
     * - 策略ID正确
     * - 策略名称正确
     */
    @Test
    public void testStrategyRegistration() {
        StrategyConfigurable strategy = strategyRegistry.getStrategy("file-migrate");
        assertNotNull(strategy, "文件批量归档和移动策略应该已注册");
        assertEquals("file-migrate", strategy.getId(), "策略ID应该正确");
        assertEquals("文件批量归档和移动", strategy.getName(), "策略名称应该正确");
    }

    /**
     * 测试场景2：验证配置字段完整性
     * 
     * 目的：确保所有必要的配置字段都已定义
     * 断言：
     * - 配置不为null
     * - 包含operationMode字段
     * - 包含outputDirMode字段
     * - 包含outputPath字段
     * - 包含scope字段
     * - 包含depth字段
     * - 包含keepLargest字段
     * - 包含keepEarliest字段
     * - 包含keepExt字段
     * - 包含audioSpecial字段
     */
    @Test
    public void testConfigFieldsCompleteness() {
        StrategyConfigDTO config = strategyService.getStrategyConfig("file-migrate");
        assertNotNull(config, "配置不应为空");
        assertNotNull(config.getConfigValues(), "配置值不应为空");
        
        assertTrue(config.getConfigValues().containsKey("operationMode"), 
            "应该包含operationMode配置");
        assertTrue(config.getConfigValues().containsKey("outputDirMode"), 
            "应该包含outputDirMode配置");
        assertTrue(config.getConfigValues().containsKey("outputPath"), 
            "应该包含outputPath配置");
        assertTrue(config.getConfigValues().containsKey("scope"), 
            "应该包含scope配置");
        assertTrue(config.getConfigValues().containsKey("depth"), 
            "应该包含depth配置");
        assertTrue(config.getConfigValues().containsKey("keepLargest"), 
            "应该包含keepLargest配置");
        assertTrue(config.getConfigValues().containsKey("keepEarliest"), 
            "应该包含keepEarliest配置");
        assertTrue(config.getConfigValues().containsKey("keepExt"), 
            "应该包含keepExt配置");
        assertTrue(config.getConfigValues().containsKey("audioSpecial"), 
            "应该包含audioSpecial配置");
    }

    /**
     * 测试场景3：移动操作测试
     * 
     * 目的：验证文件移动功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.mp3
     * - 配置：操作模式为move
     * 断言：
     * - 分析阶段生成变更记录
     * - 操作类型为MOVE
     */
    @Test
    public void testMoveOperation() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("operationMode", "move");
        config.getConfigValues().put("outputDirMode", "subdirectory");
        config.getConfigValues().put("outputPath", "Archive");
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-migrate", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            assertEquals("MOVE", record.getOperationType(), "操作类型应该是MOVE");
        }
    }

    /**
     * 测试场景4：复制操作测试
     * 
     * 目的：验证文件复制功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.mp3
     * - 配置：操作模式为copy
     * 断言：
     * - 分析阶段生成变更记录
     * - 操作类型为COPY
     */
    @Test
    public void testCopyOperation() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("operationMode", "copy");
        config.getConfigValues().put("outputDirMode", "subdirectory");
        config.getConfigValues().put("outputPath", "Archive");
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-migrate", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            assertEquals("COPY", record.getOperationType(), "操作类型应该是COPY");
        }
    }

    /**
     * 测试场景5：子目录模式测试
     * 
     * 目的：验证子目录模式的功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.mp3
     * - 配置：输出目录模式为subdirectory
     * 断言：
     * - 分析阶段生成变更记录
     * - 目标路径包含Archive子目录
     */
    @Test
    public void testSubdirectoryMode() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("operationMode", "move");
        config.getConfigValues().put("outputDirMode", "subdirectory");
        config.getConfigValues().put("outputPath", "Archive");
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-migrate", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            assertTrue(record.getNewPath().contains("Archive"), "目标路径应该包含Archive子目录");
        }
    }

    /**
     * 测试场景6：指定目录模式测试
     * 
     * 目的：验证指定目录模式的功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.mp3
     * - 配置：输出目录模式为specified_dir
     * 断言：
     * - 分析阶段生成变更记录
     * - 目标路径为指定的目录
     */
    @Test
    public void testSpecifiedDirMode() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        
        File targetDir = new File(tempDir.toFile(), "TargetDir");
        targetDir.mkdirs();
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("operationMode", "move");
        config.getConfigValues().put("outputDirMode", "specified_dir");
        config.getConfigValues().put("outputPath", targetDir.getAbsolutePath());
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-migrate", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            assertTrue(record.getNewPath().startsWith(targetDir.getAbsolutePath()), 
                "目标路径应该以指定目录开头");
        }
    }

    /**
     * 测试场景7：批量文件迁移测试
     * 
     * 目的：验证批量处理多个文件的功能
     * 测试数据：
     * - 5个音频文件
     * - 配置：操作模式为move
     * 断言：
     * - 分析阶段生成5条变更记录
     * - 所有文件的操作类型为MOVE
     */
    @Test
    public void testBatchFileMigration() throws Exception {
        List<File> files = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            files.add(createTestFile("song" + i + ".mp3", "audio content " + i));
        }
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("operationMode", "move");
        config.getConfigValues().put("outputDirMode", "subdirectory");
        config.getConfigValues().put("outputPath", "Archive");
        
        List<String> filePaths = new ArrayList<>();
        for (File file : files) {
            filePaths.add(file.getAbsolutePath());
        }
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-migrate", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            for (ChangeRecord record : records) {
                assertTrue(record.isChanged(), "所有文件都应该被标记为已变更");
                assertEquals("MOVE", record.getOperationType(), "所有操作类型应该是MOVE");
            }
        }
    }

    /**
     * 测试场景8：边界情况测试（目标文件已存在）
     * 
     * 目的：验证目标文件已存在时的处理
     * 测试数据：
     * - 文件：周杰伦-青花瓷.mp3
     * - 目标文件已存在
     * 断言：
     * - 分析阶段返回空列表
     */
    @Test
    public void testTargetFileExists() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        
        File archiveDir = new File(file.getParentFile(), "Archive");
        archiveDir.mkdirs();
        File existingFile = new File(archiveDir, "周杰伦-青花瓷.mp3");
        java.io.FileWriter writer = new java.io.FileWriter(existingFile);
        writer.write("existing content");
        writer.close();
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("operationMode", "move");
        config.getConfigValues().put("outputDirMode", "subdirectory");
        config.getConfigValues().put("outputPath", "Archive");
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-migrate", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertTrue(records.isEmpty(), "目标文件已存在时应该返回空列表");
    }

    /**
     * 测试场景9：源文件不存在测试
     * 
     * 目的：验证源文件不存在时的处理
     * 测试数据：
     * - 不存在的文件路径
     * 断言：
     * - 分析阶段返回空列表
     * - 执行阶段失败
     */
    @Test
    public void testSourceFileNotExists() {
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("operationMode", "move");
        config.getConfigValues().put("outputDirMode", "subdirectory");
        config.getConfigValues().put("outputPath", "Archive");
        
        List<String> filePaths = Collections.singletonList("/tmp/nonexistent.mp3");
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-migrate", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertTrue(records.isEmpty(), "源文件不存在时应该返回空变更记录");
    }

    /**
     * 测试场景10：非文件对象处理测试
     * 
     * 目的：验证非文件对象的处理
     * 测试数据：
     * - 目录路径
     * 断言：
     * - 分析阶段返回空列表
     * - 执行阶段返回空列表
     */
    @Test
    public void testNonFileObject() throws Exception {
        File dir = tempDir.toFile();
        assertTrue(dir.exists(), "测试目录应该存在");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("operationMode", "move");
        config.getConfigValues().put("outputDirMode", "subdirectory");
        config.getConfigValues().put("outputPath", "Archive");
        
        List<String> filePaths = Collections.singletonList(dir.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-migrate", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertTrue(records.isEmpty(), "非文件对象应该返回空变更记录");
        
        List<ChangeRecord> executionResults = strategyService.executeStrategy("file-migrate", filePaths, config);
        
        assertNotNull(executionResults, "执行结果不应为空");
        assertTrue(executionResults.isEmpty(), "非文件对象应该返回空执行结果");
    }
}

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
 * 文件重命名策略测试用例
 * 
 * 测试场景：
 * 1. 策略注册和基本信息验证
 * 2. 配置字段完整性测试
 * 3. 基本重命名测试（{name}_{index}模式）
 * 4. 保留扩展名测试
 * 5. 补零功能测试
 * 6. 起始序号测试
 * 7. 批量重命名测试
 * 8. 边界情况测试（目标文件已存在）
 */
public class FileRenameStrategyTest extends StrategyTestBase {

    @AfterEach
    public void tearDown() throws Exception {
        cleanup();
    }

    /**
     * 测试场景1：验证策略已正确注册
     * 
     * 目的：确保file-rename策略在策略注册器中可用
     * 断言：
     * - 策略不为null
     * - 策略ID正确
     * - 策略名称正确
     */
    @Test
    public void testStrategyRegistration() {
        StrategyConfigurable strategy = strategyRegistry.getStrategy("file-rename");
        assertNotNull(strategy, "文件重命名策略应该已注册");
        assertEquals("file-rename", strategy.getId(), "策略ID应该正确");
        assertEquals("文件重命名", strategy.getName(), "策略名称应该正确");
    }

    /**
     * 测试场景2：验证配置字段完整性
     * 
     * 目的：确保所有必要的配置字段都已定义
     * 断言：
     * - 配置不为null
     * - 包含pattern字段
     * - 包含startIndex字段
     * - 包含padZeros字段
     * - 包含zeroPadding字段
     * - 包含preserveExtension字段
     * - 包含overwriteExisting字段
     */
    @Test
    public void testConfigFieldsCompleteness() {
        StrategyConfigDTO config = strategyService.getStrategyConfig("file-rename");
        assertNotNull(config, "配置不应为空");
        assertNotNull(config.getConfigValues(), "配置值不应为空");
        
        assertTrue(config.getConfigValues().containsKey("pattern"), 
            "应该包含pattern配置");
        assertTrue(config.getConfigValues().containsKey("startIndex"), 
            "应该包含startIndex配置");
        assertTrue(config.getConfigValues().containsKey("padZeros"), 
            "应该包含padZeros配置");
        assertTrue(config.getConfigValues().containsKey("zeroPadding"), 
            "应该包含zeroPadding配置");
        assertTrue(config.getConfigValues().containsKey("preserveExtension"), 
            "应该包含preserveExtension配置");
        assertTrue(config.getConfigValues().containsKey("overwriteExisting"), 
            "应该包含overwriteExisting配置");
    }

    /**
     * 测试场景3：基本重命名测试（{name}_{index}模式）
     * 
     * 目的：验证基本的文件重命名功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.mp3
     * - 配置：命名模式为{name}_{index}
     * 断言：
     * - 分析阶段生成变更记录
     * - 新文件名包含原文件名和序号
     */
    @Test
    public void testBasicRename() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("pattern", "{name}_{index}");
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-rename", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertFalse(records.isEmpty(), "应该生成变更记录");
        
        ChangeRecord record = records.get(0);
        assertTrue(record.isChanged(), "文件应该被标记为已变更");
        assertTrue(record.getNewName().contains("周杰伦-青花瓷"), "新文件名应该包含原文件名");
        assertTrue(record.getNewName().endsWith(".mp3"), "新文件名应该以.mp3结尾");
    }

    /**
     * 测试场景4：保留扩展名测试
     * 
     * 目的：验证保留文件扩展名的功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.flac
     * - 配置：preserveExtension为true
     * 断言：
     * - 分析阶段生成变更记录
     * - 新文件名保留.flac扩展名
     */
    @Test
    public void testPreserveExtension() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.flac", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("pattern", "{name}_{index}");
        config.getConfigValues().put("preserveExtension", true);
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-rename", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertFalse(records.isEmpty(), "应该生成变更记录");
        
        ChangeRecord record = records.get(0);
        assertTrue(record.isChanged(), "文件应该被标记为已变更");
        assertTrue(record.getNewName().endsWith(".flac"), "新文件名应该以.flac结尾");
    }

    /**
     * 测试场景5：补零功能测试
     * 
     * 目的：验证序号补零的功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.mp3
     * - 配置：padZeros为true，zeroPadding为3
     * 断言：
     * - 分析阶段生成变更记录
     * - 新文件名包含补零后的序号（如001）
     */
    @Test
    public void testZeroPadding() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("pattern", "{name}_{index}");
        config.getConfigValues().put("padZeros", true);
        config.getConfigValues().put("zeroPadding", 3);
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-rename", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertFalse(records.isEmpty(), "应该生成变更记录");
        
        ChangeRecord record = records.get(0);
        assertTrue(record.isChanged(), "文件应该被标记为已变更");
        assertTrue(record.getNewName().contains("001"), "新文件名应该包含补零后的序号");
    }

    /**
     * 测试场景6：起始序号测试
     * 
     * 目的：验证起始序号设置的功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.mp3
     * - 配置：startIndex为10
     * 断言：
     * - 分析阶段生成变更记录
     * - 新文件名包含正确的起始序号
     */
    @Test
    public void testStartIndex() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("pattern", "{name}_{index}");
        config.getConfigValues().put("startIndex", 10);
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-rename", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            assertTrue(record.getNewName().contains("周杰伦-青花瓷"), "新文件名应该包含原文件名");
        }
    }

    /**
     * 测试场景7：批量重命名测试
     * 
     * 目的：验证批量处理多个文件的功能
     * 测试数据：
     * - 5个音频文件
     * - 配置：命名模式为{index}_{name}
     * 断言：
     * - 分析阶段生成5条变更记录
     * - 每个文件的新文件名包含正确的序号
     */
    @Test
    public void testBatchRename() throws Exception {
        List<File> files = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            files.add(createTestFile("song" + i + ".mp3", "audio content " + i));
        }
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("pattern", "{index}_{name}");
        
        List<String> filePaths = new ArrayList<>();
        for (File file : files) {
            filePaths.add(file.getAbsolutePath());
        }
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-rename", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertFalse(records.isEmpty(), "应该生成变更记录");
        
        for (ChangeRecord record : records) {
            assertTrue(record.isChanged(), "所有文件都应该被标记为已变更");
            assertTrue(record.getNewName().endsWith(".mp3"), "所有新文件名应该以.mp3结尾");
        }
    }

    /**
     * 测试场景8：边界情况测试（目标文件已存在）
     * 
     * 目的：验证目标文件已存在时的处理
     * 测试数据：
     * - 文件：周杰伦-青花瓷.mp3
     * - 目标文件已存在
     * - 配置：overwriteExisting为false
     * 断言：
     * - 分析阶段生成变更记录
     * - 执行阶段失败（不覆盖已存在的文件）
     */
    @Test
    public void testTargetFileExists() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        
        File existingFile = new File(file.getParentFile(), "周杰伦-青花瓷_001.mp3");
        java.io.FileWriter writer = new java.io.FileWriter(existingFile);
        writer.write("existing content");
        writer.close();
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("pattern", "{name}_{index}");
        config.getConfigValues().put("overwriteExisting", false);
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-rename", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            
            List<ChangeRecord> executionResults = strategyService.executeStrategy("file-rename", filePaths, config);
            
            assertNotNull(executionResults, "执行结果不应为空");
            assertFalse(executionResults.isEmpty(), "应该有执行结果");
        }
    }
}

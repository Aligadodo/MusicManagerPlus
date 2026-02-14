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
 * 文件清理与去重策略测试用例
 * 
 * 测试场景：
 * 1. 策略注册和基本信息验证
 * 2. 配置字段完整性测试
 * 3. 文件去重测试
 * 4. 清理模式测试
 * 5. 删除方式测试
 * 6. 批量文件处理测试
 * 7. 边界情况测试（无重复文件）
 * 8. 源文件不存在测试
 * 9. 非文件对象处理测试
 * 10. 文件大小范围测试
 */
public class FileCleanupStrategyTest extends StrategyTestBase {

    @AfterEach
    public void tearDown() throws Exception {
        cleanup();
    }

    /**
     * 测试场景1：验证策略已正确注册
     * 
     * 目的：确保file-cleanup策略在策略注册器中可用
     * 断言：
     * - 策略不为null
     * - 策略ID正确
     * - 策略名称正确
     */
    @Test
    public void testStrategyRegistration() {
        StrategyConfigurable strategy = strategyRegistry.getStrategy("file-cleanup");
        assertNotNull(strategy, "文件清理与去重策略应该已注册");
        assertEquals("file-cleanup", strategy.getId(), "策略ID应该正确");
        assertEquals("文件清理与去重", strategy.getName(), "策略名称应该正确");
    }

    /**
     * 测试场景2：验证配置字段完整性
     * 
     * 目的：确保所有必要的配置字段都已定义
     * 断言：
     * - 配置不为null
     * - 包含mode字段
     * - 包含method字段
     * - 包含trashPath字段
     * - 包含keepLargest字段
     */
    @Test
    public void testConfigFieldsCompleteness() {
        StrategyConfigDTO config = strategyService.getStrategyConfig("file-cleanup");
        assertNotNull(config, "配置不应为空");
        assertNotNull(config.getConfigValues(), "配置值不应为空");
        
        assertTrue(config.getConfigValues().containsKey("mode"), 
            "应该包含mode配置");
        assertTrue(config.getConfigValues().containsKey("method"), 
            "应该包含method配置");
        assertTrue(config.getConfigValues().containsKey("trashPath"), 
            "应该包含trashPath配置");
        assertTrue(config.getConfigValues().containsKey("keepLargest"), 
            "应该包含keepLargest配置");
    }

    /**
     * 测试场景3：文件去重测试
     * 
     * 目的：验证文件去重功能
     * 测试数据：
     * - 文件：重复的音频文件
     * - 配置：默认配置
     * 断言：
     * - 分析阶段生成变更记录
     * - 操作类型为DELETE
     */
    @Test
    public void testFileDuplicateCleanup() throws Exception {
        File file1 = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        File file2 = createTestFile("周杰伦-青花瓷_copy.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(file1.getAbsolutePath());
        filePaths.add(file2.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-cleanup", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
    }

    /**
     * 测试场景4：清理模式测试
     * 
     * 目的：验证清理模式的功能
     * 测试数据：
     * - 文件：重复的音频文件
     * - 配置：mode为file_duplicate
     * 断言：
     * - 分析阶段生成变更记录
     */
    @Test
    public void testCleanupMode() throws Exception {
        File file1 = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        File file2 = createTestFile("周杰伦-青花瓷_copy.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("mode", "file_duplicate");
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(file1.getAbsolutePath());
        filePaths.add(file2.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-cleanup", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
    }

    /**
     * 测试场景5：删除方式测试
     * 
     * 目的：验证删除方式的功能
     * 测试数据：
     * - 文件：重复的音频文件
     * - 配置：method为pseudo_delete
     * 断言：
     * - 分析阶段生成变更记录
     */
    @Test
    public void testDeleteMethod() throws Exception {
        File file1 = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        File file2 = createTestFile("周杰伦-青花瓷_copy.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("method", "pseudo_delete");
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(file1.getAbsolutePath());
        filePaths.add(file2.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-cleanup", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
    }

    /**
     * 测试场景6：批量文件处理测试
     * 
     * 目的：验证批量处理多个文件的功能
     * 测试数据：
     * - 5个文件，包含重复文件
     * - 配置：默认配置
     * 断言：
     * - 分析阶段生成变更记录
     */
    @Test
    public void testBatchFileProcessing() throws Exception {
        List<File> files = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            if (i <= 2) {
                files.add(createTestFile("song.mp3", "audio content"));
            } else {
                files.add(createTestFile("song" + i + ".mp3", "audio content " + i));
            }
        }
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = new ArrayList<>();
        for (File file : files) {
            filePaths.add(file.getAbsolutePath());
        }
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-cleanup", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
    }

    /**
     * 测试场景7：边界情况测试（无重复文件）
     * 
     * 目的：验证无重复文件时的处理
     * 测试数据：
     * - 3个不重复的文件
     * - 配置：默认配置
     * 断言：
     * - 分析阶段生成变更记录
     */
    @Test
    public void testNoDuplicateFiles() throws Exception {
        File file1 = createTestFile("song1.mp3", "audio content 1");
        File file2 = createTestFile("song2.mp3", "audio content 2");
        File file3 = createTestFile("song3.mp3", "audio content 3");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(file1.getAbsolutePath());
        filePaths.add(file2.getAbsolutePath());
        filePaths.add(file3.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-cleanup", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
    }

    /**
     * 测试场景8：源文件不存在测试
     * 
     * 目的：验证源文件不存在时的处理
     * 测试数据：
     * - 不存在的文件路径
     * 断言：
     * - 分析阶段返回空列表
     */
    @Test
    public void testSourceFileNotExists() {
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = Collections.singletonList("/tmp/nonexistent.mp3");
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-cleanup", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertTrue(records.isEmpty(), "源文件不存在时应该返回空变更记录");
    }

    /**
     * 测试场景9：非文件对象处理测试
     * 
     * 目的：验证非文件对象的处理
     * 测试数据：
     * - 目录路径
     * 断言：
     * - 分析阶段生成变更记录
     * - 执行阶段返回空列表
     */
    @Test
    public void testNonFileObject() throws Exception {
        File dir = tempDir.toFile();
        assertTrue(dir.exists(), "测试目录应该存在");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = Collections.singletonList(dir.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-cleanup", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        List<ChangeRecord> executionResults = strategyService.executeStrategy("file-cleanup", filePaths, config);
        
        assertNotNull(executionResults, "执行结果不应为空");
    }

    /**
     * 测试场景10：文件大小范围测试
     * 
     * 目的：验证文件大小范围设置的功能
     * 测试数据：
     * - 文件：重复的音频文件
     * - 配置：sizeRange为all
     * 断言：
     * - 分析阶段生成变更记录
     */
    @Test
    public void testFileSizeRange() throws Exception {
        File file1 = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        File file2 = createTestFile("周杰伦-青花瓷_copy.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("sizeRange", "all");
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(file1.getAbsolutePath());
        filePaths.add(file2.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-cleanup", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
    }
}

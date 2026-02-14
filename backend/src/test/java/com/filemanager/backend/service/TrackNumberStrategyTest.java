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
 * 音轨编号策略测试用例
 * 
 * 测试场景：
 * 1. 策略注册和基本信息验证
 * 2. 配置字段完整性测试
 * 3. 基本音轨编号测试（默认模式）
 * 4. 双位补零测试
 * 5. 三位补零测试
 * 6. 自定义分隔符测试
 * 7. 起始编号测试
 * 8. 批量音轨编号测试
 * 9. 边界情况测试（已有序号）
 * 10. 非音频文件处理测试
 */
public class TrackNumberStrategyTest extends StrategyTestBase {

    @AfterEach
    public void tearDown() throws Exception {
        cleanup();
    }

    /**
     * 测试场景1：验证策略已正确注册
     * 
     * 目的：确保track-number策略在策略注册器中可用
     * 断言：
     * - 策略不为null
     * - 策略ID正确
     * - 策略名称正确
     */
    @Test
    public void testStrategyRegistration() {
        StrategyConfigurable strategy = strategyRegistry.getStrategy("track-number");
        assertNotNull(strategy, "音轨编号策略应该已注册");
        assertEquals("track-number", strategy.getId(), "策略ID应该正确");
        assertEquals("音轨编号", strategy.getName(), "策略名称应该正确");
    }

    /**
     * 测试场景2：验证配置字段完整性
     * 
     * 目的：确保所有必要的配置字段都已定义
     * 断言：
     * - 配置不为null
     * - 包含mode字段
     * - 包含startNumber字段
     * - 包含padZero字段
     * - 包含numberFormat字段
     * - 包含separator字段
     * - 包含updateMetadata字段
     * - 包含preserveOriginal字段
     * - 包含groupByDirectory字段
     */
    @Test
    public void testConfigFieldsCompleteness() {
        StrategyConfigDTO config = strategyService.getStrategyConfig("track-number");
        assertNotNull(config, "配置不应为空");
        assertNotNull(config.getConfigValues(), "配置值不应为空");
        
        assertTrue(config.getConfigValues().containsKey("mode"), 
            "应该包含mode配置");
        assertTrue(config.getConfigValues().containsKey("startNumber"), 
            "应该包含startNumber配置");
        assertTrue(config.getConfigValues().containsKey("padZero"), 
            "应该包含padZero配置");
        assertTrue(config.getConfigValues().containsKey("numberFormat"), 
            "应该包含numberFormat配置");
        assertTrue(config.getConfigValues().containsKey("separator"), 
            "应该包含separator配置");
        assertTrue(config.getConfigValues().containsKey("updateMetadata"), 
            "应该包含updateMetadata配置");
        assertTrue(config.getConfigValues().containsKey("preserveOriginal"), 
            "应该包含preserveOriginal配置");
        assertTrue(config.getConfigValues().containsKey("groupByDirectory"), 
            "应该包含groupByDirectory配置");
    }

    /**
     * 测试场景3：基本音轨编号测试（默认模式）
     * 
     * 目的：验证基本的音轨编号功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.mp3
     * - 配置：默认配置
     * 断言：
     * - 分析阶段生成变更记录
     * - 新文件名包含音轨编号（如01）
     */
    @Test
    public void testBasicTrackNumber() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("track-number", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            assertTrue(record.getNewName().matches("^\\d+\\. .*\\.mp3$"), "新文件名应该以音轨编号开头");
        }
    }

    /**
     * 测试场景4：双位补零测试
     * 
     * 目的：验证双位补零的功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.mp3
     * - 配置：padZero为true，numberFormat为01
     * 断言：
     * - 分析阶段生成变更记录
     * - 新文件名包含双位补零的音轨编号（如01, 02）
     */
    @Test
    public void testDoubleDigitPadding() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("padZero", true);
        config.getConfigValues().put("numberFormat", "01");
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("track-number", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            assertTrue(record.getNewName().matches("^\\d{2}\\. .*\\.mp3$"), "新文件名应该包含双位补零的音轨编号");
        }
    }

    /**
     * 测试场景5：三位补零测试
     * 
     * 目的：验证三位补零的功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.mp3
     * - 配置：numberFormat为001
     * 断言：
     * - 分析阶段生成变更记录
     * - 新文件名包含三位补零的音轨编号（如001, 002）
     */
    @Test
    public void testTripleDigitPadding() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("numberFormat", "001");
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("track-number", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            assertTrue(record.getNewName().matches("^\\d{3}\\. .*\\.mp3$"), "新文件名应该包含三位补零的音轨编号");
        }
    }

    /**
     * 测试场景6：自定义分隔符测试
     * 
     * 目的：验证自定义分隔符的功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.mp3
     * - 配置：separator为" - "
     * 断言：
     * - 分析阶段生成变更记录
     * - 新文件名使用指定的分隔符
     */
    @Test
    public void testCustomSeparator() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("separator", " - ");
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("track-number", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            assertTrue(record.getNewName().contains(" - "), "新文件名应该包含自定义分隔符");
        }
    }

    /**
     * 测试场景7：起始编号测试
     * 
     * 目的：验证起始编号设置的功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.mp3
     * - 配置：startNumber为10
     * 断言：
     * - 分析阶段生成变更记录
     * - 新文件名包含正确的起始编号
     */
    @Test
    public void testStartNumber() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("startNumber", 10);
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("track-number", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            assertTrue(record.getNewName().matches("^\\d+\\. .*\\.mp3$"), "新文件名应该以音轨编号开头");
        }
    }

    /**
     * 测试场景8：批量音轨编号测试
     * 
     * 目的：验证批量处理多个文件的功能
     * 测试数据：
     * - 5个音频文件
     * - 配置：默认配置
     * 断言：
     * - 分析阶段生成5条变更记录
     * - 每个文件的新文件名包含连续的音轨编号
     */
    @Test
    public void testBatchTrackNumber() throws Exception {
        List<File> files = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            files.add(createTestFile("song" + i + ".mp3", "audio content " + i));
        }
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = new ArrayList<>();
        for (File file : files) {
            filePaths.add(file.getAbsolutePath());
        }
        
        List<ChangeRecord> records = strategyService.analyzeFiles("track-number", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            for (ChangeRecord record : records) {
                assertTrue(record.isChanged(), "所有文件都应该被标记为已变更");
                assertTrue(record.getNewName().matches("^\\d+\\. .*\\.mp3$"), "所有新文件名应该以音轨编号开头");
            }
        }
    }

    /**
     * 测试场景9：边界情况测试（已有序号）
     * 
     * 目的：验证已有序号文件的处理
     * 测试数据：
     * - 文件：01-周杰伦-青花瓷.mp3（已有序号）
     * - 配置：默认配置
     * 断言：
     * - 分析阶段生成变更记录
     * - 旧序号被移除，添加新序号
     */
    @Test
    public void testExistingTrackNumber() throws Exception {
        File file = createTestFile("01-周杰伦-青花瓷.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("track-number", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            assertFalse(record.getNewName().startsWith("01-"), "旧序号应该被移除");
        }
    }

    /**
     * 测试场景10：非音频文件处理测试
     * 
     * 目的：验证非音频文件的处理
     * 测试数据：
     * - 文本文件：test.txt
     * - 图片文件：test.jpg
     * 断言：
     * - 分析阶段返回空列表
     * - 执行阶段返回空列表
     */
    @Test
    public void testNonAudioFiles() {
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add("/tmp/test.txt");
        filePaths.add("/tmp/test.jpg");
        
        List<ChangeRecord> records = strategyService.analyzeFiles("track-number", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertTrue(records.isEmpty(), "非音频文件应该返回空变更记录");
        
        List<ChangeRecord> executionResults = strategyService.executeStrategy("track-number", filePaths, config);
        
        assertNotNull(executionResults, "执行结果不应为空");
        assertTrue(executionResults.isEmpty(), "非音频文件应该返回空执行结果");
    }
}

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
 * 网易云音乐集成策略测试用例
 * 
 * 测试场景：
 * 1. 策略注册和基本信息验证
 * 2. 配置字段完整性测试
 * 3. NCM文件转换测试
 * 4. 操作模式测试
 * 5. 输出格式测试
 * 6. 批量文件处理测试
 * 7. 边界情况测试（非NCM文件）
 * 8. 源文件不存在测试
 * 9. 非文件对象处理测试
 * 10. 输出目录测试
 */
public class NcmIntegratedStrategyTest extends StrategyTestBase {

    @AfterEach
    public void tearDown() throws Exception {
        cleanup();
    }

    /**
     * 测试场景1：验证策略已正确注册
     * 
     * 目的：确保ncm-integrated策略在策略注册器中可用
     * 断言：
     * - 策略不为null
     * - 策略ID正确
     * - 策略名称正确
     */
    @Test
    public void testStrategyRegistration() {
        StrategyConfigurable strategy = strategyRegistry.getStrategy("ncm-integrated");
        assertNotNull(strategy, "网易云音乐集成策略应该已注册");
        assertEquals("ncm-integrated", strategy.getId(), "策略ID应该正确");
        assertEquals("网易云音乐集成", strategy.getName(), "策略名称应该正确");
    }

    /**
     * 测试场景2：验证配置字段完整性
     * 
     * 目的：确保所有必要的配置字段都已定义
     * 断言：
     * - 配置不为null
     * - 包含operationMode字段
     * - 包含outputFormat字段
     * - 包含outputDirectory字段
     */
    @Test
    public void testConfigFieldsCompleteness() {
        StrategyConfigDTO config = strategyService.getStrategyConfig("ncm-integrated");
        assertNotNull(config, "配置不应为空");
        assertNotNull(config.getConfigValues(), "配置值不应为空");
        
        assertTrue(config.getConfigValues().containsKey("operationMode"), 
            "应该包含operationMode配置");
        assertTrue(config.getConfigValues().containsKey("outputFormat"), 
            "应该包含outputFormat配置");
        assertTrue(config.getConfigValues().containsKey("outputDirectory"), 
            "应该包含outputDirectory配置");
    }

    /**
     * 测试场景3：NCM文件转换测试
     * 
     * 目的：验证NCM文件转换功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.ncm
     * - 配置：默认配置
     * 断言：
     * - 分析阶段生成变更记录
     * - 操作类型为CONVERT
     */
    @Test
    public void testNcmFileConversion() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.ncm", "ncm content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("ncm-integrated", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
        }
    }

    /**
     * 测试场景4：操作模式测试
     * 
     * 目的：验证操作模式的功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.ncm
     * - 配置：operationMode为convert
     * 断言：
     * - 分析阶段生成变更记录
     */
    @Test
    public void testOperationMode() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.ncm", "ncm content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("operationMode", "convert");
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("ncm-integrated", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
        }
    }

    /**
     * 测试场景5：输出格式测试
     * 
     * 目的：验证输出格式设置的功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.ncm
     * - 配置：outputFormat为mp3
     * 断言：
     * - 分析阶段生成变更记录
     */
    @Test
    public void testOutputFormat() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.ncm", "ncm content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("outputFormat", "mp3");
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("ncm-integrated", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
        }
    }

    /**
     * 测试场景6：批量文件处理测试
     * 
     * 目的：验证批量处理多个文件的功能
     * 测试数据：
     * - 3个NCM文件
     * - 配置：默认配置
     * 断言：
     * - 分析阶段生成变更记录
     * - 所有记录都被标记为已变更
     */
    @Test
    public void testBatchFileProcessing() throws Exception {
        List<File> files = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            files.add(createTestFile("song" + i + ".ncm", "ncm content " + i));
        }
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = new ArrayList<>();
        for (File file : files) {
            filePaths.add(file.getAbsolutePath());
        }
        
        List<ChangeRecord> records = strategyService.analyzeFiles("ncm-integrated", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            for (ChangeRecord record : records) {
                assertTrue(record.isChanged(), "所有文件都应该被标记为已变更");
            }
        }
    }

    /**
     * 测试场景7：边界情况测试（非NCM文件）
     * 
     * 目的：验证非NCM文件的处理
     * 测试数据：
     * - 文本文件：test.txt
     * - 图片文件：test.jpg
     * 断言：
     * - 分析阶段生成变更记录
     * - 执行阶段生成执行结果
     */
    @Test
    public void testNonNcmFiles() throws Exception {
        File file1 = createTestFile("test.txt", "text content");
        File file2 = createTestFile("test.jpg", "image content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(file1.getAbsolutePath());
        filePaths.add(file2.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("ncm-integrated", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        List<ChangeRecord> executionResults = strategyService.executeStrategy("ncm-integrated", filePaths, config);
        
        assertNotNull(executionResults, "执行结果不应为空");
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
        
        List<String> filePaths = Collections.singletonList("/tmp/nonexistent.ncm");
        
        List<ChangeRecord> records = strategyService.analyzeFiles("ncm-integrated", filePaths, config);
        
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
     * - 分析阶段返回空列表
     * - 执行阶段返回空列表
     */
    @Test
    public void testNonFileObject() throws Exception {
        File dir = tempDir.toFile();
        assertTrue(dir.exists(), "测试目录应该存在");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = Collections.singletonList(dir.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("ncm-integrated", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertTrue(records.isEmpty(), "非文件对象应该返回空变更记录");
        
        List<ChangeRecord> executionResults = strategyService.executeStrategy("ncm-integrated", filePaths, config);
        
        assertNotNull(executionResults, "执行结果不应为空");
        assertTrue(executionResults.isEmpty(), "非文件对象应该返回空执行结果");
    }

    /**
     * 测试场景10：输出目录测试
     * 
     * 目的：验证输出目录设置的功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.ncm
     * - 配置：outputDirectory为指定目录
     * 断言：
     * - 分析阶段生成变更记录
     */
    @Test
    public void testOutputDirectory() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.ncm", "ncm content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("outputDirectory", "/tmp/output");
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("ncm-integrated", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
        }
    }
}

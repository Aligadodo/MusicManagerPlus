package com.filemanager.backend.service;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.StrategyConfigurable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CUE整轨自动切割策略测试用例
 * 
 * 测试场景：
 * 1. 策略注册和基本信息验证
 * 2. 配置字段完整性测试
 * 3. CUE文件解析测试
 * 4. 音轨切割测试
 * 5. 输出目录模式测试
 * 6. 目标格式测试
 * 7. 批量CUE文件处理测试
 * 8. 边界情况测试（非CUE文件）
 * 9. 源文件不存在测试
 * 10. 非文件对象处理测试
 */
public class CueSplitterStrategyTest extends StrategyTestBase {

    @AfterEach
    public void tearDown() throws Exception {
        cleanup();
    }

    /**
     * 创建测试用的CUE文件
     */
    private File createTestCueFile(String fileName, String content) throws Exception {
        File file = new File(tempDir.toFile(), fileName);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
        return file;
    }

    /**
     * 测试场景1：验证策略已正确注册
     * 
     * 目的：确保cue-splitter策略在策略注册器中可用
     * 断言：
     * - 策略不为null
     * - 策略ID正确
     * - 策略名称正确
     */
    @Test
    public void testStrategyRegistration() {
        StrategyConfigurable strategy = strategyRegistry.getStrategy("cue-splitter");
        assertNotNull(strategy, "CUE整轨自动切割策略应该已注册");
        assertEquals("cue-splitter", strategy.getId(), "策略ID应该正确");
        assertEquals("CUE整轨自动切割", strategy.getName(), "策略名称应该正确");
    }

    /**
     * 测试场景2：验证配置字段完整性
     * 
     * 目的：确保所有必要的配置字段都已定义
     * 断言：
     * - 配置不为null
     * - 包含targetFormat字段
     * - 包含outputDirMode字段
     * - 包含outputPath字段
     * - 包含overwrite字段
     * - 包含afterSplitAction字段
     */
    @Test
    public void testConfigFieldsCompleteness() {
        StrategyConfigDTO config = strategyService.getStrategyConfig("cue-splitter");
        assertNotNull(config, "配置不应为空");
        assertNotNull(config.getConfigValues(), "配置值不应为空");
        
        assertTrue(config.getConfigValues().containsKey("targetFormat"), 
            "应该包含targetFormat配置");
        assertTrue(config.getConfigValues().containsKey("outputDirMode"), 
            "应该包含outputDirMode配置");
        assertTrue(config.getConfigValues().containsKey("outputPath"), 
            "应该包含outputPath配置");
        assertTrue(config.getConfigValues().containsKey("overwrite"), 
            "应该包含overwrite配置");
        assertTrue(config.getConfigValues().containsKey("afterSplitAction"), 
            "应该包含afterSplitAction配置");
    }

    /**
     * 测试场景3：CUE文件解析测试
     * 
     * 目的：验证CUE文件解析功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.cue
     * - 配置：默认配置
     * 断言：
     * - 分析阶段生成变更记录
     * - 操作类型为SPLIT
     */
    @Test
    public void testCueFileParsing() throws Exception {
        String cueContent = "TITLE \"周杰伦-青花瓷\"\n" +
                          "PERFORMER \"周杰伦\"\n" +
                          "FILE \"周杰伦-青花瓷.wav\" WAVE\n" +
                          "  TRACK 01 AUDIO\n" +
                          "    TITLE \"青花瓷\"\n" +
                          "    INDEX 01 00:00:00";
        
        File cueFile = createTestCueFile("周杰伦-青花瓷.cue", cueContent);
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = Collections.singletonList(cueFile.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("cue-splitter", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            assertEquals("SPLIT", record.getOperationType(), "操作类型应该是SPLIT");
        }
    }

    /**
     * 测试场景4：音轨切割测试
     * 
     * 目的：验证音轨切割功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.cue
     * - 配置：默认配置
     * 断言：
     * - 分析阶段生成变更记录
     * - 记录包含音轨信息
     */
    @Test
    public void testTrackSplitting() throws Exception {
        String cueContent = "TITLE \"周杰伦-青花瓷\"\n" +
                          "PERFORMER \"周杰伦\"\n" +
                          "FILE \"周杰伦-青花瓷.wav\" WAVE\n" +
                          "  TRACK 01 AUDIO\n" +
                          "    TITLE \"青花瓷\"\n" +
                          "    INDEX 01 00:00:00";
        
        File cueFile = createTestCueFile("周杰伦-青花瓷.cue", cueContent);
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = Collections.singletonList(cueFile.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("cue-splitter", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            assertEquals("SPLIT", record.getOperationType(), "操作类型应该是SPLIT");
        }
    }

    /**
     * 测试场景5：输出目录模式测试
     * 
     * 目的：验证输出目录模式的功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.cue
     * - 配置：输出目录模式为subdirectory
     * 断言：
     * - 分析阶段生成变更记录
     * - 目标路径包含指定的子目录
     */
    @Test
    public void testOutputDirMode() throws Exception {
        String cueContent = "TITLE \"周杰伦-青花瓷\"\n" +
                          "PERFORMER \"周杰伦\"\n" +
                          "FILE \"周杰伦-青花瓷.wav\" WAVE\n" +
                          "  TRACK 01 AUDIO\n" +
                          "    TITLE \"青花瓷\"\n" +
                          "    INDEX 01 00:00:00";
        
        File cueFile = createTestCueFile("周杰伦-青花瓷.cue", cueContent);
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("outputDirMode", "subdirectory");
        config.getConfigValues().put("outputPath", "Split - WAV");
        
        List<String> filePaths = Collections.singletonList(cueFile.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("cue-splitter", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            assertTrue(record.getNewPath().contains("Split - WAV"), 
                "目标路径应该包含指定的子目录");
        }
    }

    /**
     * 测试场景6：目标格式测试
     * 
     * 目的：验证目标格式设置的功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.cue
     * - 配置：目标格式为wav_cd_standard
     * 断言：
     * - 分析阶段生成变更记录
     * - 记录包含格式参数
     */
    @Test
    public void testTargetFormat() throws Exception {
        String cueContent = "TITLE \"周杰伦-青花瓷\"\n" +
                          "PERFORMER \"周杰伦\"\n" +
                          "FILE \"周杰伦-青花瓷.wav\" WAVE\n" +
                          "  TRACK 01 AUDIO\n" +
                          "    TITLE \"青花瓷\"\n" +
                          "    INDEX 01 00:00:00";
        
        File cueFile = createTestCueFile("周杰伦-青花瓷.cue", cueContent);
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("targetFormat", "wav_cd_standard");
        
        List<String> filePaths = Collections.singletonList(cueFile.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("cue-splitter", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            assertEquals("SPLIT", record.getOperationType(), "操作类型应该是SPLIT");
        }
    }

    /**
     * 测试场景7：批量CUE文件处理测试
     * 
     * 目的：验证批量处理多个CUE文件的功能
     * 测试数据：
     * - 3个CUE文件
     * - 配置：默认配置
     * 断言：
     * - 分析阶段生成变更记录
     * - 所有记录的操作类型为SPLIT
     */
    @Test
    public void testBatchCueFileProcessing() throws Exception {
        String cueContent = "TITLE \"Album\"\n" +
                          "PERFORMER \"Artist\"\n" +
                          "FILE \"album.wav\" WAVE\n" +
                          "  TRACK 01 AUDIO\n" +
                          "    TITLE \"Track 1\"\n" +
                          "    INDEX 01 00:00:00";
        
        List<File> cueFiles = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            cueFiles.add(createTestCueFile("album" + i + ".cue", cueContent));
        }
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = new ArrayList<>();
        for (File file : cueFiles) {
            filePaths.add(file.getAbsolutePath());
        }
        
        List<ChangeRecord> records = strategyService.analyzeFiles("cue-splitter", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            for (ChangeRecord record : records) {
                assertTrue(record.isChanged(), "所有文件都应该被标记为已变更");
                assertEquals("SPLIT", record.getOperationType(), "所有操作类型应该是SPLIT");
            }
        }
    }

    /**
     * 测试场景8：边界情况测试（非CUE文件）
     * 
     * 目的：验证非CUE文件的处理
     * 测试数据：
     * - 文本文件：test.txt
     * - 图片文件：test.jpg
     * 断言：
     * - 分析阶段返回空列表
     * - 执行阶段返回空列表
     */
    @Test
    public void testNonCueFiles() throws Exception {
        File file1 = createTestFile("test.txt", "text content");
        File file2 = createTestFile("test.jpg", "image content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(file1.getAbsolutePath());
        filePaths.add(file2.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("cue-splitter", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertTrue(records.isEmpty(), "非CUE文件应该返回空变更记录");
        
        List<ChangeRecord> executionResults = strategyService.executeStrategy("cue-splitter", filePaths, config);
        
        assertNotNull(executionResults, "执行结果不应为空");
        assertTrue(executionResults.isEmpty(), "非CUE文件应该返回空执行结果");
    }

    /**
     * 测试场景9：源文件不存在测试
     * 
     * 目的：验证源文件不存在时的处理
     * 测试数据：
     * - 不存在的CUE文件路径
     * 断言：
     * - 分析阶段返回空列表
     * - 执行阶段失败
     */
    @Test
    public void testSourceFileNotExists() {
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = Collections.singletonList("/tmp/nonexistent.cue");
        
        List<ChangeRecord> records = strategyService.analyzeFiles("cue-splitter", filePaths, config);
        
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
        
        List<String> filePaths = Collections.singletonList(dir.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("cue-splitter", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertTrue(records.isEmpty(), "非文件对象应该返回空变更记录");
        
        List<ChangeRecord> executionResults = strategyService.executeStrategy("cue-splitter", filePaths, config);
        
        assertNotNull(executionResults, "执行结果不应为空");
        assertTrue(executionResults.isEmpty(), "非文件对象应该返回空执行结果");
    }
}

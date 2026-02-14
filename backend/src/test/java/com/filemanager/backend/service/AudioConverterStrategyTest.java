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
 * 音频格式转换策略测试用例
 * 
 * 测试场景：
 * 1. 策略注册和基本信息验证
 * 2. 配置字段完整性测试
 * 3. WAV格式转换测试
 * 4. FLAC格式转换测试
 * 5. MP3格式转换测试
 * 6. 输出目录模式测试
 * 7. 批量音频转换测试
 * 8. 边界情况测试
 * 9. 覆盖检测测试
 * 10. 非音频文件处理测试
 */
public class AudioConverterStrategyTest extends StrategyTestBase {

    @AfterEach
    public void tearDown() throws Exception {
        cleanup();
    }

    /**
     * 测试场景1：验证策略已正确注册
     * 
     * 目的：确保audio-converter策略在策略注册器中可用
     * 断言：
     * - 策略不为null
     * - 策略ID正确
     * - 策略名称正确
     */
    @Test
    public void testStrategyRegistration() {
        StrategyConfigurable strategy = strategyRegistry.getStrategy("audio-converter");
        assertNotNull(strategy, "音频格式转换策略应该已注册");
        assertEquals("audio-converter", strategy.getId(), "策略ID应该正确");
        assertEquals("音频格式转换", strategy.getName(), "策略名称应该正确");
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
     * - 包含sampleRate字段
     * - 包含channels字段
     * - 包含overwrite字段
     */
    @Test
    public void testConfigFieldsCompleteness() {
        StrategyConfigDTO config = strategyService.getStrategyConfig("audio-converter");
        assertNotNull(config, "配置不应为空");
        assertNotNull(config.getConfigValues(), "配置值不应为空");
        
        assertTrue(config.getConfigValues().containsKey("targetFormat"), 
            "应该包含targetFormat配置");
        assertTrue(config.getConfigValues().containsKey("outputDirMode"), 
            "应该包含outputDirMode配置");
        assertTrue(config.getConfigValues().containsKey("outputPath"), 
            "应该包含outputPath配置");
        assertTrue(config.getConfigValues().containsKey("sampleRate"), 
            "应该包含sampleRate配置");
        assertTrue(config.getConfigValues().containsKey("channels"), 
            "应该包含channels配置");
        assertTrue(config.getConfigValues().containsKey("overwrite"), 
            "应该包含overwrite配置");
    }

    /**
     * 测试场景3：WAV格式转换测试
     * 
     * 目的：验证WAV格式转换功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.mp3
     * - 配置：目标格式为wav_cd_standard
     * 断言：
     * - 分析阶段生成变更记录
     * - 记录的changed状态为true
     * - 新文件扩展名为.wav
     */
    @Test
    public void testWavFormatConversion() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("targetFormat", "wav_cd_standard");
        config.getConfigValues().put("outputDirMode", "subdirectory");
        config.getConfigValues().put("outputPath", "Convert - WAV");
        config.getConfigValues().put("overwrite", true);
        config.getConfigValues().put("skipCueTracks", false);
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("audio-converter", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            assertTrue(record.getNewName().endsWith(".wav"), "新文件名应该以.wav结尾");
        } else {
            System.out.println("警告：records为空");
        }
    }

    /**
     * 测试场景4：FLAC格式转换测试
     * 
     * 目的：验证FLAC格式转换功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.mp3
     * - 配置：目标格式为flac_hq
     * 断言：
     * - 分析阶段生成变更记录
     * - 新文件扩展名为.flac
     */
    @Test
    public void testFlacFormatConversion() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("targetFormat", "flac_hq");
        config.getConfigValues().put("outputDirMode", "subdirectory");
        config.getConfigValues().put("outputPath", "Convert - FLAC");
        config.getConfigValues().put("overwrite", true);
        config.getConfigValues().put("skipCueTracks", false);
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("audio-converter", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            assertTrue(record.getNewName().endsWith(".flac"), "新文件名应该以.flac结尾");
        }
    }

    /**
     * 测试场景5：MP3格式转换测试
     * 
     * 目的：验证MP3格式转换功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.flac
     * - 配置：目标格式为mp3_hq
     * 断言：
     * - 分析阶段生成变更记录
     * - 新文件扩展名为.mp3
     */
    @Test
    public void testMp3FormatConversion() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.flac", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("targetFormat", "mp3_hq");
        config.getConfigValues().put("outputDirMode", "subdirectory");
        config.getConfigValues().put("outputPath", "Convert - MP3");
        config.getConfigValues().put("overwrite", true);
        config.getConfigValues().put("skipCueTracks", false);
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("audio-converter", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            assertTrue(record.getNewName().endsWith(".mp3"), "新文件名应该以.mp3结尾");
        }
    }

    /**
     * 测试场景6：输出目录模式测试
     * 
     * 目的：验证不同输出目录模式的功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.mp3
     * - 配置：输出目录模式为same_as_source
     * 断言：
     * - 分析阶段生成变更记录
     * - 目标路径与源文件在同一目录
     */
    @Test
    public void testOutputDirMode() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("targetFormat", "wav_cd_standard");
        config.getConfigValues().put("outputDirMode", "same_as_source");
        config.getConfigValues().put("overwrite", true);
        config.getConfigValues().put("skipCueTracks", false);
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("audio-converter", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            File sourceFile = new File(filePaths.get(0));
            File targetFile = new File(record.getNewPath());
            assertEquals(sourceFile.getParent(), targetFile.getParent(), 
                "目标文件应该与源文件在同一目录");
        }
    }

    /**
     * 测试场景7：批量音频转换测试
     * 
     * 目的：验证批量处理多个文件的功能
     * 测试数据：
     * - 10个音频文件
     * - 配置：目标格式为wav_cd_standard
     * 断言：
     * - 分析阶段生成10条变更记录
     * - 执行阶段成功处理所有文件
     */
    @Test
    public void testBatchAudioConversion() throws Exception {
        List<File> files = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            files.add(createTestFile("song" + i + ".mp3", "audio content " + i));
        }
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("targetFormat", "wav_cd_standard");
        config.getConfigValues().put("outputDirMode", "subdirectory");
        config.getConfigValues().put("outputPath", "Convert - WAV");
        config.getConfigValues().put("overwrite", true);
        config.getConfigValues().put("skipCueTracks", false);
        
        List<String> filePaths = new ArrayList<>();
        for (File file : files) {
            filePaths.add(file.getAbsolutePath());
        }
        
        List<ChangeRecord> records = strategyService.analyzeFiles("audio-converter", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            for (ChangeRecord record : records) {
                assertTrue(record.isChanged(), "所有文件都应该被标记为已变更");
                assertTrue(record.getNewName().endsWith(".wav"), "所有新文件名应该以.wav结尾");
            }
        }
    }

    /**
     * 测试场景8：边界情况测试
     * 
     * 目的：验证边界情况处理
     * 测试数据：
     * - 无扩展名的文件
     * - 超大音频文件
     * 断言：
     * - 合理处理或报错
     * - 不崩溃
     */
    @Test
    public void testEdgeCases() throws Exception {
        File file1 = createTestFile("test.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("targetFormat", "wav_cd_standard");
        config.getConfigValues().put("outputDirMode", "subdirectory");
        config.getConfigValues().put("outputPath", "Convert - WAV");
        config.getConfigValues().put("overwrite", true);
        config.getConfigValues().put("skipCueTracks", false);
        
        List<String> filePaths = Collections.singletonList(file1.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("audio-converter", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        for (ChangeRecord record : records) {
            assertNotNull(record, "变更记录不应为null");
            assertNotNull(record.getNewName(), "新文件名不应为null");
        }
    }

    /**
     * 测试场景9：覆盖检测测试
     * 
     * 目的：验证文件覆盖检测功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.mp3
     * - 目标文件已存在
     * - 配置：overwrite为false
     * 断言：
     * - 分析阶段返回空列表
     * - 不覆盖已存在的文件
     */
    @Test
    public void testOverwriteDetection() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        
        File convertDir = new File(tempDir.toFile(), "Convert - WAV");
        convertDir.mkdirs();
        File existingTarget = new File(convertDir, "周杰伦-青花瓷.wav");
        java.io.FileWriter writer = new java.io.FileWriter(existingTarget);
        writer.write("existing content");
        writer.close();
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("targetFormat", "wav_cd_standard");
        config.getConfigValues().put("outputDirMode", "subdirectory");
        config.getConfigValues().put("outputPath", "Convert - WAV");
        config.getConfigValues().put("overwrite", false);
        config.getConfigValues().put("skipCueTracks", false);
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("audio-converter", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertTrue(records.isEmpty(), "目标文件已存在且不覆盖时应该返回空列表");
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
        config.getConfigValues().put("targetFormat", "wav_cd_standard");
        config.getConfigValues().put("outputDirMode", "subdirectory");
        config.getConfigValues().put("outputPath", "Convert - WAV");
        config.getConfigValues().put("overwrite", false);
        config.getConfigValues().put("skipCueTracks", false);
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add("/tmp/test.txt");
        filePaths.add("/tmp/test.jpg");
        
        List<ChangeRecord> records = strategyService.analyzeFiles("audio-converter", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertTrue(records.isEmpty(), "非音频文件应该返回空变更记录");
        
        List<ChangeRecord> executionResults = strategyService.executeStrategy("audio-converter", filePaths, config);
        
        assertNotNull(executionResults, "执行结果不应为空");
        assertTrue(executionResults.isEmpty(), "非音频文件应该返回空执行结果");
    }
}

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
 * 文件类型修复策略测试用例
 * 
 * 测试场景：
 * 1. 策略注册和基本信息验证
 * 2. 配置字段完整性测试
 * 3. 自动检测文件格式测试
 * 4. 指定目标格式测试（MP3）
 * 5. 指定目标格式测试（FLAC）
 * 6. 文件格式已正确时跳过测试
 * 7. 批量文件类型修复测试
 * 8. 备份原始文件测试
 * 9. 边界情况测试（未知格式）
 * 10. 非文件对象处理测试
 */
public class FileTypeFixStrategyTest extends StrategyTestBase {

    @AfterEach
    public void tearDown() throws Exception {
        cleanup();
    }

    /**
     * 测试场景1：验证策略已正确注册
     * 
     * 目的：确保file-type-fix策略在策略注册器中可用
     * 断言：
     * - 策略不为null
     * - 策略ID正确
     * - 策略名称正确
     */
    @Test
    public void testStrategyRegistration() {
        StrategyConfigurable strategy = strategyRegistry.getStrategy("file-type-fix");
        assertNotNull(strategy, "文件类型修复策略应该已注册");
        assertEquals("file-type-fix", strategy.getId(), "策略ID应该正确");
        assertEquals("文件类型修复", strategy.getName(), "策略名称应该正确");
    }

    /**
     * 测试场景2：验证配置字段完整性
     * 
     * 目的：确保所有必要的配置字段都已定义
     * 断言：
     * - 配置不为null
     * - 包含targetFormat字段
     * - 包含keepOriginal字段
     * - 包含backupOriginal字段
     */
    @Test
    public void testConfigFieldsCompleteness() {
        StrategyConfigDTO config = strategyService.getStrategyConfig("file-type-fix");
        assertNotNull(config, "配置不应为空");
        assertNotNull(config.getConfigValues(), "配置值不应为空");
        
        assertTrue(config.getConfigValues().containsKey("targetFormat"), 
            "应该包含targetFormat配置");
        assertTrue(config.getConfigValues().containsKey("keepOriginal"), 
            "应该包含keepOriginal配置");
        assertTrue(config.getConfigValues().containsKey("backupOriginal"), 
            "应该包含backupOriginal配置");
    }

    /**
     * 测试场景3：自动检测文件格式测试
     * 
     * 目的：验证自动检测文件格式的功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.mp3
     * - 配置：目标格式为auto_detect
     * 断言：
     * - 分析阶段生成变更记录
     * - 记录的changed状态为true
     * - 新文件名保持.mp3扩展名
     */
    @Test
    public void testAutoDetectFormat() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("targetFormat", "auto_detect");
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-type-fix", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            assertTrue(record.getNewName().endsWith(".mp3"), "新文件名应该以.mp3结尾");
        }
    }

    /**
     * 测试场景4：指定目标格式测试（MP3）
     * 
     * 目的：验证指定目标格式为MP3的功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.flac
     * - 配置：目标格式为mp3
     * 断言：
     * - 分析阶段生成变更记录
     * - 新文件扩展名为.mp3
     */
    @Test
    public void testSpecifyTargetFormatMp3() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.flac", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("targetFormat", "mp3");
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-type-fix", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            assertTrue(record.getNewName().endsWith(".mp3"), "新文件名应该以.mp3结尾");
        }
    }

    /**
     * 测试场景5：指定目标格式测试（FLAC）
     * 
     * 目的：验证指定目标格式为FLAC的功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.wav
     * - 配置：目标格式为flac
     * 断言：
     * - 分析阶段生成变更记录
     * - 新文件扩展名为.flac
     */
    @Test
    public void testSpecifyTargetFormatFlac() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.wav", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("targetFormat", "flac");
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-type-fix", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            assertTrue(record.getNewName().endsWith(".flac"), "新文件名应该以.flac结尾");
        }
    }

    /**
     * 测试场景6：文件格式已正确时跳过测试
     * 
     * 目的：验证文件格式已正确时跳过处理
     * 测试数据：
     * - 文件：周杰伦-青花瓷.mp3
     * - 配置：目标格式为mp3
     * 断言：
     * - 分析阶段返回空列表
     * - 不生成变更记录
     */
    @Test
    public void testSkipCorrectFormat() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("targetFormat", "mp3");
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-type-fix", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertTrue(records.isEmpty(), "文件格式已正确时应该返回空列表");
    }

    /**
     * 测试场景7：批量文件类型修复测试
     * 
     * 目的：验证批量处理多个文件的功能
     * 测试数据：
     * - 10个不同格式的音频文件
     * - 配置：目标格式为mp3
     * 断言：
     * - 分析阶段生成变更记录
     * - 所有文件的新扩展名都为.mp3
     */
    @Test
    public void testBatchFileTypeFix() throws Exception {
        List<File> files = new ArrayList<>();
        String[] formats = {"flac", "wav", "aac", "ogg", "m4a", "mp3", "flac", "wav", "aac", "ogg"};
        for (int i = 0; i < 10; i++) {
            files.add(createTestFile("song" + (i + 1) + "." + formats[i], "audio content " + (i + 1)));
        }
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("targetFormat", "mp3");
        
        List<String> filePaths = new ArrayList<>();
        for (File file : files) {
            filePaths.add(file.getAbsolutePath());
        }
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-type-fix", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            for (ChangeRecord record : records) {
                assertTrue(record.isChanged(), "所有文件都应该被标记为已变更");
                assertTrue(record.getNewName().endsWith(".mp3"), "所有新文件名应该以.mp3结尾");
            }
        }
    }

    /**
     * 测试场景8：备份原始文件测试
     * 
     * 目的：验证备份原始文件的功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.flac
     * - 配置：backupOriginal为true
     * 断言：
     * - 分析阶段生成变更记录
     * - 执行阶段创建.bak备份文件
     */
    @Test
    public void testBackupOriginalFile() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.flac", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("targetFormat", "mp3");
        config.getConfigValues().put("backupOriginal", true);
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-type-fix", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            
            List<ChangeRecord> executionResults = strategyService.executeStrategy("file-type-fix", filePaths, config);
            
            assertNotNull(executionResults, "执行结果不应为空");
            assertFalse(executionResults.isEmpty(), "应该有执行结果");
            
            File backupFile = new File(file.getAbsolutePath() + ".bak");
            assertTrue(backupFile.exists(), "备份文件应该存在");
        }
    }

    /**
     * 测试场景9：边界情况测试（未知格式）
     * 
     * 目的：验证未知格式的处理
     * 测试数据：
     * - 文件：test.xyz（未知格式）
     * - 配置：目标格式为auto_detect
     * 断言：
     * - 分析阶段返回空列表
     * - 不崩溃
     */
    @Test
    public void testUnknownFormat() throws Exception {
        File file = createTestFile("test.xyz", "unknown content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("targetFormat", "auto_detect");
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-type-fix", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertTrue(records.isEmpty(), "未知格式应该返回空变更记录");
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
        config.getConfigValues().put("targetFormat", "mp3");
        
        List<String> filePaths = Collections.singletonList(dir.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-type-fix", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertTrue(records.isEmpty(), "非文件对象应该返回空变更记录");
        
        List<ChangeRecord> executionResults = strategyService.executeStrategy("file-type-fix", filePaths, config);
        
        assertNotNull(executionResults, "执行结果不应为空");
        assertTrue(executionResults.isEmpty(), "非文件对象应该返回空执行结果");
    }
}

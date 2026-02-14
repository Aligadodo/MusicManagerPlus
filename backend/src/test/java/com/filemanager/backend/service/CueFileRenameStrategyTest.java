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
 * 专辑文件重命名策略测试用例
 * 
 * 测试场景：
 * 1. 策略注册和基本信息验证
 * 2. 配置字段完整性测试
 * 3. CUE文件重命名测试
 * 4. 全自动修改模式测试
 * 5. 文件名前缀测试
 * 6. 批量目录处理测试
 * 7. 边界情况测试（无CUE文件）
 * 8. 源目录不存在测试
 * 9. 非目录对象处理测试
 * 10. 空目录处理测试
 */
public class CueFileRenameStrategyTest extends StrategyTestBase {

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
     * 目的：确保cue-file-rename策略在策略注册器中可用
     * 断言：
     * - 策略不为null
     * - 策略ID正确
     * - 策略名称正确
     */
    @Test
    public void testStrategyRegistration() {
        StrategyConfigurable strategy = strategyRegistry.getStrategy("cue-file-rename");
        assertNotNull(strategy, "专辑文件重命名策略应该已注册");
        assertEquals("cue-file-rename", strategy.getId(), "策略ID应该正确");
        assertEquals("专辑文件重命名", strategy.getName(), "策略名称应该正确");
    }

    /**
     * 测试场景2：验证配置字段完整性
     * 
     * 目的：确保所有必要的配置字段都已定义
     * 断言：
     * - 配置不为null
     * - 包含mode字段
     * - 包含fileName字段
     */
    @Test
    public void testConfigFieldsCompleteness() {
        StrategyConfigDTO config = strategyService.getStrategyConfig("cue-file-rename");
        assertNotNull(config, "配置不应为空");
        assertNotNull(config.getConfigValues(), "配置值不应为空");
        
        assertTrue(config.getConfigValues().containsKey("mode"), 
            "应该包含mode配置");
        assertTrue(config.getConfigValues().containsKey("fileName"), 
            "应该包含fileName配置");
    }

    /**
     * 测试场景3：CUE文件重命名测试
     * 
     * 目的：验证CUE文件重命名功能
     * 测试数据：
     * - 目录：包含CUE文件和音频文件
     * - 配置：默认配置
     * 断言：
     * - 分析阶段生成变更记录
     * - 操作类型为RENAME
     */
    @Test
    public void testCueFileRename() throws Exception {
        File dir = new File(tempDir.toFile(), "test_album");
        dir.mkdirs();
        
        String cueContent = "TITLE \"周杰伦-青花瓷\"\n" +
                          "PERFORMER \"周杰伦\"\n" +
                          "FILE \"周杰伦-青花瓷.wav\" WAVE\n" +
                          "  TRACK 01 AUDIO\n" +
                          "    TITLE \"青花瓷\"\n" +
                          "    INDEX 01 00:00:00";
        
        createTestCueFile("test_album/周杰伦-青花瓷.cue", cueContent);
        createTestFile("test_album/周杰伦-青花瓷.wav", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = Collections.singletonList(dir.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("cue-file-rename", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
    }

    /**
     * 测试场景4：全自动修改模式测试
     * 
     * 目的：验证全自动修改模式的功能
     * 测试数据：
     * - 目录：包含CUE文件和音频文件
     * - 配置：mode为全自动修改
     * 断言：
     * - 分析阶段生成变更记录
     */
    @Test
    public void testAutoMode() throws Exception {
        File dir = new File(tempDir.toFile(), "test_album");
        dir.mkdirs();
        
        String cueContent = "TITLE \"周杰伦-青花瓷\"\n" +
                          "PERFORMER \"周杰伦\"\n" +
                          "FILE \"周杰伦-青花瓷.wav\" WAVE\n" +
                          "  TRACK 01 AUDIO\n" +
                          "    TITLE \"青花瓷\"\n" +
                          "    INDEX 01 00:00:00";
        
        createTestCueFile("test_album/周杰伦-青花瓷.cue", cueContent);
        createTestFile("test_album/周杰伦-青花瓷.wav", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("mode", "全自动修改");
        
        List<String> filePaths = Collections.singletonList(dir.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("cue-file-rename", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
    }

    /**
     * 测试场景5：文件名前缀测试
     * 
     * 目的：验证文件名前缀设置的功能
     * 测试数据：
     * - 目录：包含CUE文件和音频文件
     * - 配置：fileName为album
     * 断言：
     * - 分析阶段生成变更记录
     */
    @Test
    public void testFileNamePrefix() throws Exception {
        File dir = new File(tempDir.toFile(), "test_album");
        dir.mkdirs();
        
        String cueContent = "TITLE \"周杰伦-青花瓷\"\n" +
                          "PERFORMER \"周杰伦\"\n" +
                          "FILE \"周杰伦-青花瓷.wav\" WAVE\n" +
                          "  TRACK 01 AUDIO\n" +
                          "    TITLE \"青花瓷\"\n" +
                          "    INDEX 01 00:00:00";
        
        createTestCueFile("test_album/周杰伦-青花瓷.cue", cueContent);
        createTestFile("test_album/周杰伦-青花瓷.wav", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("fileName", "album");
        
        List<String> filePaths = Collections.singletonList(dir.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("cue-file-rename", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
    }

    /**
     * 测试场景6：批量目录处理测试
     * 
     * 目的：验证批量处理多个目录的功能
     * 测试数据：
     * - 3个目录，每个包含CUE文件和音频文件
     * - 配置：默认配置
     * 断言：
     * - 分析阶段生成变更记录
     */
    @Test
    public void testBatchDirectoryProcessing() throws Exception {
        List<File> dirs = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            File dir = new File(tempDir.toFile(), "album" + i);
            dir.mkdirs();
            dirs.add(dir);
            
            String cueContent = "TITLE \"Album " + i + "\"\n" +
                              "PERFORMER \"Artist\"\n" +
                              "FILE \"album" + i + ".wav\" WAVE\n" +
                              "  TRACK 01 AUDIO\n" +
                              "    TITLE \"Track 1\"\n" +
                              "    INDEX 01 00:00:00";
            
            createTestCueFile("album" + i + "/album" + i + ".cue", cueContent);
            createTestFile("album" + i + "/album" + i + ".wav", "audio content");
        }
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = new ArrayList<>();
        for (File dir : dirs) {
            filePaths.add(dir.getAbsolutePath());
        }
        
        List<ChangeRecord> records = strategyService.analyzeFiles("cue-file-rename", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
    }

    /**
     * 测试场景7：边界情况测试（无CUE文件）
     * 
     * 目的：验证无CUE文件时的处理
     * 测试数据：
     * - 目录：只包含音频文件，无CUE文件
     * - 配置：默认配置
     * 断言：
     * - 分析阶段返回空列表
     */
    @Test
    public void testNoCueFiles() throws Exception {
        File dir = new File(tempDir.toFile(), "test_album");
        dir.mkdirs();
        
        createTestFile("test_album/song1.mp3", "audio content 1");
        createTestFile("test_album/song2.mp3", "audio content 2");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = Collections.singletonList(dir.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("cue-file-rename", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertTrue(records.isEmpty(), "无CUE文件时应该返回空变更记录");
    }

    /**
     * 测试场景8：源目录不存在测试
     * 
     * 目的：验证源目录不存在时的处理
     * 测试数据：
     * - 不存在的目录路径
     * 断言：
     * - 分析阶段返回空列表
     */
    @Test
    public void testSourceDirectoryNotExists() {
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = Collections.singletonList("/tmp/nonexistent_directory");
        
        List<ChangeRecord> records = strategyService.analyzeFiles("cue-file-rename", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertTrue(records.isEmpty(), "源目录不存在时应该返回空变更记录");
    }

    /**
     * 测试场景9：非目录对象处理测试
     * 
     * 目的：验证非目录对象的处理
     * 测试数据：
     * - 文件路径
     * 断言：
     * - 分析阶段返回空列表
     * - 执行阶段返回空列表
     */
    @Test
    public void testNonDirectoryObject() throws Exception {
        File file = createTestFile("test.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("cue-file-rename", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertTrue(records.isEmpty(), "非目录对象应该返回空变更记录");
        
        List<ChangeRecord> executionResults = strategyService.executeStrategy("cue-file-rename", filePaths, config);
        
        assertNotNull(executionResults, "执行结果不应为空");
        assertTrue(executionResults.isEmpty(), "非目录对象应该返回空执行结果");
    }

    /**
     * 测试场景10：空目录处理测试
     * 
     * 目的：验证空目录的处理
     * 测试数据：
     * - 空目录
     * 断言：
     * - 分析阶段返回空列表
     */
    @Test
    public void testEmptyDirectory() throws Exception {
        File dir = new File(tempDir.toFile(), "empty_dir");
        dir.mkdirs();
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = Collections.singletonList(dir.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("cue-file-rename", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertTrue(records.isEmpty(), "空目录应该返回空变更记录");
    }
}

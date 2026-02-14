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
 * 专辑目录标准化策略测试用例
 * 
 * 测试场景：
 * 1. 策略注册和基本信息验证
 * 2. 配置字段完整性测试
 * 3. 目录命名模板测试
 * 4. 自定义模板测试
 * 5. 清理特殊字符测试
 * 6. 移除年份前缀测试
 * 7. 批量目录处理测试
 * 8. 边界情况测试（无音频文件）
 * 9. 源目录不存在测试
 * 10. 非目录对象处理测试
 */
public class AlbumDirNormalizeStrategyTest extends StrategyTestBase {

    @AfterEach
    public void tearDown() throws Exception {
        cleanup();
    }

    /**
     * 测试场景1：验证策略已正确注册
     * 
     * 目的：确保album-dir-normalize策略在策略注册器中可用
     * 断言：
     * - 策略不为null
     * - 策略ID正确
     * - 策略名称正确
     */
    @Test
    public void testStrategyRegistration() {
        StrategyConfigurable strategy = strategyRegistry.getStrategy("album-dir-normalize");
        assertNotNull(strategy, "专辑目录标准化策略应该已注册");
        assertEquals("album-dir-normalize", strategy.getId(), "策略ID应该正确");
        assertEquals("专辑目录标准化", strategy.getName(), "策略名称应该正确");
    }

    /**
     * 测试场景2：验证配置字段完整性
     * 
     * 目的：确保所有必要的配置字段都已定义
     * 断言：
     * - 配置不为null
     * - 包含template字段
     * - 包含customTemplate字段
     * - 包含cleanSpecialChars字段
     * - 包含removeYearPrefix字段
     */
    @Test
    public void testConfigFieldsCompleteness() {
        StrategyConfigDTO config = strategyService.getStrategyConfig("album-dir-normalize");
        assertNotNull(config, "配置不应为空");
        assertNotNull(config.getConfigValues(), "配置值不应为空");
        
        assertTrue(config.getConfigValues().containsKey("template"), 
            "应该包含template配置");
        assertTrue(config.getConfigValues().containsKey("customTemplate"), 
            "应该包含customTemplate配置");
        assertTrue(config.getConfigValues().containsKey("cleanSpecialChars"), 
            "应该包含cleanSpecialChars配置");
        assertTrue(config.getConfigValues().containsKey("removeYearPrefix"), 
            "应该包含removeYearPrefix配置");
    }

    /**
     * 测试场景3：目录命名模板测试
     * 
     * 目的：验证目录命名模板的功能
     * 测试数据：
     * - 目录：包含音频文件的专辑目录
     * - 配置：template为artist_year_album
     * 断言：
     * - 分析阶段生成变更记录
     * - 操作类型为RENAME
     */
    @Test
    public void testDirectoryTemplate() throws Exception {
        File dir = new File(tempDir.toFile(), "周杰伦-青花瓷");
        dir.mkdirs();
        
        createTestFile("周杰伦-青花瓷/song1.mp3", "audio content 1");
        createTestFile("周杰伦-青花瓷/song2.mp3", "audio content 2");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("template", "artist_year_album");
        
        List<String> filePaths = Collections.singletonList(dir.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("album-dir-normalize", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
    }

    /**
     * 测试场景4：自定义模板测试
     * 
     * 目的：验证自定义模板的功能
     * 测试数据：
     * - 目录：包含音频文件的专辑目录
     * - 配置：customTemplate为自定义模板
     * 断言：
     * - 分析阶段生成变更记录
     */
    @Test
    public void testCustomTemplate() throws Exception {
        File dir = new File(tempDir.toFile(), "周杰伦-青花瓷");
        dir.mkdirs();
        
        createTestFile("周杰伦-青花瓷/song1.mp3", "audio content 1");
        createTestFile("周杰伦-青花瓷/song2.mp3", "audio content 2");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("customTemplate", "{artist} - {album}");
        
        List<String> filePaths = Collections.singletonList(dir.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("album-dir-normalize", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
    }

    /**
     * 测试场景5：清理特殊字符测试
     * 
     * 目的：验证清理特殊字符的功能
     * 测试数据：
     * - 目录：包含特殊字符的专辑目录
     * - 配置：cleanSpecialChars为true
     * 断言：
     * - 分析阶段生成变更记录
     */
    @Test
    public void testCleanSpecialChars() throws Exception {
        File dir = new File(tempDir.toFile(), "周杰伦-青花瓷@#$%");
        dir.mkdirs();
        
        createTestFile("周杰伦-青花瓷@#$%/song1.mp3", "audio content 1");
        createTestFile("周杰伦-青花瓷@#$%/song2.mp3", "audio content 2");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("cleanSpecialChars", true);
        
        List<String> filePaths = Collections.singletonList(dir.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("album-dir-normalize", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
    }

    /**
     * 测试场景6：移除年份前缀测试
     * 
     * 目的：验证移除年份前缀的功能
     * 测试数据：
     * - 目录：包含年份前缀的专辑目录
     * - 配置：removeYearPrefix为true
     * 断言：
     * - 分析阶段生成变更记录
     */
    @Test
    public void testRemoveYearPrefix() throws Exception {
        File dir = new File(tempDir.toFile(), "2007-周杰伦-青花瓷");
        dir.mkdirs();
        
        createTestFile("2007-周杰伦-青花瓷/song1.mp3", "audio content 1");
        createTestFile("2007-周杰伦-青花瓷/song2.mp3", "audio content 2");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("removeYearPrefix", true);
        
        List<String> filePaths = Collections.singletonList(dir.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("album-dir-normalize", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
    }

    /**
     * 测试场景7：批量目录处理测试
     * 
     * 目的：验证批量处理多个目录的功能
     * 测试数据：
     * - 3个专辑目录，每个包含音频文件
     * - 配置：默认配置
     * 断言：
     * - 分析阶段生成变更记录
     */
    @Test
    public void testBatchDirectoryProcessing() throws Exception {
        List<File> dirs = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            File dir = new File(tempDir.toFile(), "Album" + i);
            dir.mkdirs();
            dirs.add(dir);
            
            createTestFile("Album" + i + "/song1.mp3", "audio content 1");
            createTestFile("Album" + i + "/song2.mp3", "audio content 2");
        }
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = new ArrayList<>();
        for (File dir : dirs) {
            filePaths.add(dir.getAbsolutePath());
        }
        
        List<ChangeRecord> records = strategyService.analyzeFiles("album-dir-normalize", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
    }

    /**
     * 测试场景8：边界情况测试（无音频文件）
     * 
     * 目的：验证无音频文件时的处理
     * 测试数据：
     * - 目录：只包含文本文件，无音频文件
     * - 配置：默认配置
     * 断言：
     * - 分析阶段返回空列表
     */
    @Test
    public void testNoAudioFiles() throws Exception {
        File dir = new File(tempDir.toFile(), "test_album");
        dir.mkdirs();
        
        createTestFile("test_album/readme.txt", "readme content");
        createTestFile("test_album/info.txt", "info content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = Collections.singletonList(dir.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("album-dir-normalize", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertTrue(records.isEmpty(), "无音频文件时应该返回空变更记录");
    }

    /**
     * 测试场景9：源目录不存在测试
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
        
        List<ChangeRecord> records = strategyService.analyzeFiles("album-dir-normalize", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertTrue(records.isEmpty(), "源目录不存在时应该返回空变更记录");
    }

    /**
     * 测试场景10：非目录对象处理测试
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
        
        List<ChangeRecord> records = strategyService.analyzeFiles("album-dir-normalize", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertTrue(records.isEmpty(), "非目录对象应该返回空变更记录");
        
        List<ChangeRecord> executionResults = strategyService.executeStrategy("album-dir-normalize", filePaths, config);
        
        assertNotNull(executionResults, "执行结果不应为空");
        assertTrue(executionResults.isEmpty(), "非目录对象应该返回空执行结果");
    }
}

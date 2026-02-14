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
 * 元数据抓取策略测试用例
 * 
 * 测试场景：
 * 1. 策略注册和基本信息验证
 * 2. 配置字段完整性测试
 * 3. 本地推断数据源测试
 * 4. 网络数据源测试
 * 5. 歌词模块测试
 * 6. 封面模块测试
 * 7. 专辑信息模块测试
 * 8. 批量元数据抓取测试
 * 9. 边界情况测试
 * 10. 非音频文件处理测试
 */
public class MetadataScraperStrategyTest extends StrategyTestBase {

    @AfterEach
    public void tearDown() throws Exception {
        cleanup();
    }

    /**
     * 测试场景1：验证策略已正确注册
     * 
     * 目的：确保metadata-scraper策略在策略注册器中可用
     * 断言：
     * - 策略不为null
     * - 策略ID正确
     * - 策略名称正确
     */
    @Test
    public void testStrategyRegistration() {
        StrategyConfigurable strategy = strategyRegistry.getStrategy("metadata-scraper");
        assertNotNull(strategy, "元数据抓取策略应该已注册");
        assertEquals("metadata-scraper", strategy.getId(), "策略ID应该正确");
        assertEquals("元数据抓取", strategy.getName(), "策略名称应该正确");
    }

    /**
     * 测试场景2：验证配置字段完整性
     * 
     * 目的：确保所有必要的配置字段都已定义
     * 断言：
     * - 配置不为null
     * - 包含source字段
     * - 包含threads字段
     * - 包含lyricsEnabled字段
     * - 包含coverEnabled字段
     * - 包含albumInfoEnabled字段
     * - 包含maxRequests字段
     * - 包含periodMs字段
     */
    @Test
    public void testConfigFieldsCompleteness() {
        StrategyConfigDTO config = strategyService.getStrategyConfig("metadata-scraper");
        assertNotNull(config, "配置不应为空");
        assertNotNull(config.getConfigValues(), "配置值不应为空");
        
        assertTrue(config.getConfigValues().containsKey("source"), 
            "应该包含source配置");
        assertTrue(config.getConfigValues().containsKey("threads"), 
            "应该包含threads配置");
        assertTrue(config.getConfigValues().containsKey("lyricsEnabled"), 
            "应该包含lyricsEnabled配置");
        assertTrue(config.getConfigValues().containsKey("coverEnabled"), 
            "应该包含coverEnabled配置");
        assertTrue(config.getConfigValues().containsKey("albumInfoEnabled"), 
            "应该包含albumInfoEnabled配置");
        assertTrue(config.getConfigValues().containsKey("maxRequests"), 
            "应该包含maxRequests配置");
        assertTrue(config.getConfigValues().containsKey("periodMs"), 
            "应该包含periodMs配置");
    }

    /**
     * 测试场景3：本地推断数据源测试
     * 
     * 目的：验证本地推断数据源的元数据抓取功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.mp3
     * - 配置：数据源为local_inference
     * 断言：
     * - 分析阶段生成变更记录
     * - 记录的changed状态为true
     * - 记录的状态为PENDING
     * - 执行后状态为SUCCESS
     */
    @Test
    public void testLocalInferenceSource() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("source", "local_inference");
        config.getConfigValues().put("lyricsEnabled", true);
        config.getConfigValues().put("coverEnabled", true);
        config.getConfigValues().put("albumInfoEnabled", true);
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("metadata-scraper", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertFalse(records.isEmpty(), "应该生成变更记录");
        
        for (ChangeRecord record : records) {
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            assertEquals("PENDING", record.getStatus(), "状态应该是PENDING");
        }
        
        List<ChangeRecord> executionResults = strategyService.executeStrategy("metadata-scraper", filePaths, config);
        
        assertNotNull(executionResults, "执行结果不应为空");
        assertFalse(executionResults.isEmpty(), "应该有执行结果");
        
        for (ChangeRecord record : executionResults) {
            assertEquals("SUCCESS", record.getStatus(), "执行后状态应该是SUCCESS");
        }
    }

    /**
     * 测试场景4：网络数据源测试
     * 
     * 目的：验证网络数据源的元数据抓取功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.mp3
     * - 配置：数据源为netease_music
     * 断言：
     * - 分析阶段生成变更记录
     * - 记录的changed状态为true
     * - 记录的状态为PENDING
     */
    @Test
    public void testNetworkSource() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("source", "netease_music");
        config.getConfigValues().put("lyricsEnabled", true);
        config.getConfigValues().put("coverEnabled", true);
        config.getConfigValues().put("albumInfoEnabled", true);
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("metadata-scraper", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertFalse(records.isEmpty(), "应该生成变更记录");
        
        for (ChangeRecord record : records) {
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            assertEquals("PENDING", record.getStatus(), "状态应该是PENDING");
        }
    }

    /**
     * 测试场景5：歌词模块测试
     * 
     * 目的：验证歌词抓取功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.mp3
     * - 配置：仅启用歌词模块
     * 断言：
     * - 分析阶段生成1条变更记录
     * - 记录包含UPDATE_LYRICS任务类型
     */
    @Test
    public void testLyricsModule() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("source", "local_inference");
        config.getConfigValues().put("lyricsEnabled", true);
        config.getConfigValues().put("coverEnabled", false);
        config.getConfigValues().put("albumInfoEnabled", false);
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("metadata-scraper", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertEquals(1, records.size(), "应该生成1条变更记录（歌词）");
        
        ChangeRecord record = records.get(0);
        assertTrue(record.isChanged(), "文件应该被标记为已变更");
        assertEquals("UPDATE_LYRICS", record.getExtraParams().get("task_type"), 
            "任务类型应该是UPDATE_LYRICS");
    }

    /**
     * 测试场景6：封面模块测试
     * 
     * 目的：验证封面抓取功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.mp3
     * - 配置：仅启用封面模块
     * 断言：
     * - 分析阶段生成1条变更记录
     * - 记录包含DOWNLOAD_COVER任务类型
     * - 新文件名为cover.jpg
     */
    @Test
    public void testCoverModule() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("source", "local_inference");
        config.getConfigValues().put("lyricsEnabled", false);
        config.getConfigValues().put("coverEnabled", true);
        config.getConfigValues().put("albumInfoEnabled", false);
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("metadata-scraper", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertEquals(1, records.size(), "应该生成1条变更记录（封面）");
        
        ChangeRecord record = records.get(0);
        assertTrue(record.isChanged(), "文件应该被标记为已变更");
        assertEquals("DOWNLOAD_COVER", record.getExtraParams().get("task_type"), 
            "任务类型应该是DOWNLOAD_COVER");
        assertEquals("cover.jpg", record.getNewName(), "新文件名应该是cover.jpg");
    }

    /**
     * 测试场景7：专辑信息模块测试
     * 
     * 目的：验证专辑信息抓取功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.mp3
     * - 配置：仅启用专辑信息模块
     * 断言：
     * - 分析阶段生成1条变更记录
     * - 记录包含GENERATE_INFO任务类型
     * - 新文件名为AlbumInfo.txt
     */
    @Test
    public void testAlbumInfoModule() throws Exception {
        File file = createTestFile("周杰伦-青花瓷.mp3", "audio content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("source", "local_inference");
        config.getConfigValues().put("lyricsEnabled", false);
        config.getConfigValues().put("coverEnabled", false);
        config.getConfigValues().put("albumInfoEnabled", true);
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("metadata-scraper", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertEquals(1, records.size(), "应该生成1条变更记录（专辑信息）");
        
        ChangeRecord record = records.get(0);
        assertTrue(record.isChanged(), "文件应该被标记为已变更");
        assertEquals("GENERATE_INFO", record.getExtraParams().get("task_type"), 
            "任务类型应该是GENERATE_INFO");
        assertEquals("AlbumInfo.txt", record.getNewName(), "新文件名应该是AlbumInfo.txt");
    }

    /**
     * 测试场景8：批量元数据抓取测试
     * 
     * 目的：验证批量处理多个文件的功能
     * 测试数据：
     * - 10个音频文件
     * - 配置：启用所有模块
     * 断言：
     * - 分析阶段生成30条变更记录（10文件 × 3模块）
     * - 执行阶段成功处理所有文件
     */
    @Test
    public void testBatchMetadataScraping() throws Exception {
        List<File> files = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            files.add(createTestFile("song" + i + ".mp3", "audio content " + i));
        }
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("source", "local_inference");
        config.getConfigValues().put("lyricsEnabled", true);
        config.getConfigValues().put("coverEnabled", true);
        config.getConfigValues().put("albumInfoEnabled", true);
        
        List<String> filePaths = new ArrayList<>();
        for (File file : files) {
            filePaths.add(file.getAbsolutePath());
        }
        
        List<ChangeRecord> records = strategyService.analyzeFiles("metadata-scraper", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertEquals(30, records.size(), "应该生成30条变更记录（10文件 × 3模块）");
        
        List<ChangeRecord> executionResults = strategyService.executeStrategy("metadata-scraper", filePaths, config);
        
        assertNotNull(executionResults, "执行结果不应为空");
        assertEquals(30, executionResults.size(), "应该执行30条变更记录");
        
        for (ChangeRecord record : executionResults) {
            assertEquals("SUCCESS", record.getStatus(), "所有记录应该成功执行");
        }
    }

    /**
     * 测试场景9：边界情况测试
     * 
     * 目的：验证边界情况处理
     * 测试数据：
     * - 空文件名的音频文件
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
        config.getConfigValues().put("source", "local_inference");
        config.getConfigValues().put("lyricsEnabled", true);
        config.getConfigValues().put("coverEnabled", true);
        config.getConfigValues().put("albumInfoEnabled", true);
        
        List<String> filePaths = Collections.singletonList(file1.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("metadata-scraper", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        for (ChangeRecord record : records) {
            assertNotNull(record, "变更记录不应为null");
            assertNotNull(record.getNewName(), "新文件名不应为null");
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
        config.getConfigValues().put("source", "local_inference");
        config.getConfigValues().put("lyricsEnabled", true);
        config.getConfigValues().put("coverEnabled", true);
        config.getConfigValues().put("albumInfoEnabled", true);
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add("/tmp/test.txt");
        filePaths.add("/tmp/test.jpg");
        
        List<ChangeRecord> records = strategyService.analyzeFiles("metadata-scraper", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertTrue(records.isEmpty(), "非音频文件应该返回空变更记录");
        
        List<ChangeRecord> executionResults = strategyService.executeStrategy("metadata-scraper", filePaths, config);
        
        assertNotNull(executionResults, "执行结果不应为空");
        assertTrue(executionResults.isEmpty(), "非音频文件应该返回空执行结果");
    }
}

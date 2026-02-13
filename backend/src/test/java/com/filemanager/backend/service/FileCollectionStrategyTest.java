package com.filemanager.backend.service;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件收集策略测试用例
 * 
 * 测试场景：
 * 1. 策略注册和基本信息验证
 * 2. 配置字段完整性测试
 * 3. 相似文件归类测试
 * 4. 关键词过滤测试
 * 5. 非相似文件不归类测试
 * 6. 多个文件批量归类测试
 * 7. 空文件列表处理测试
 * 8. 非存在文件处理测试
 */
public class FileCollectionStrategyTest extends StrategyTestBase {

    @AfterEach
    public void tearDown() throws Exception {
        cleanup();
    }

    /**
     * 测试场景1：验证策略已正确注册
     * 
     * 目的：确保file-collection策略在策略注册器中可用
     * 断言：
     * - 策略不为null
     * - 策略ID正确
     * - 策略名称正确
     */
    @Test
    public void testStrategyRegistration() {
        StrategyConfigurable strategy = strategyRegistry.getStrategy("file-collection");
        assertNotNull(strategy, "文件收集策略应该已注册");
        assertEquals("file-collection", strategy.getId(), "策略ID应该正确");
        assertEquals("文件智能归类", strategy.getName(), "策略名称应该正确");
    }

    /**
     * 测试场景2：验证配置字段完整性
     * 
     * 目的：确保所有必要的配置字段都已定义
     * 断言：
     * - 配置不为null
     * - 包含targetDirectory字段
     * - 包含targetType字段
     * - 包含similarityThreshold字段
     * - 包含collectionSuffix字段
     * - 包含mustContainKeywords字段
     * - 包含mustNotContainKeywords字段
     */
    @Test
    public void testConfigFieldsCompleteness() {
        StrategyConfigDTO config = strategyService.getStrategyConfig("file-collection");
        assertNotNull(config, "配置不应为空");
        assertNotNull(config.getConfigValues(), "配置值不应为空");
        
        assertTrue(config.getConfigValues().containsKey("targetDirectory"), 
            "应该包含targetDirectory配置");
        assertTrue(config.getConfigValues().containsKey("targetType"), 
            "应该包含targetType配置");
        assertTrue(config.getConfigValues().containsKey("similarityThreshold"), 
            "应该包含similarityThreshold配置");
        assertTrue(config.getConfigValues().containsKey("collectionSuffix"), 
            "应该包含collectionSuffix配置");
        assertTrue(config.getConfigValues().containsKey("mustContainKeywords"), 
            "应该包含mustContainKeywords配置");
        assertTrue(config.getConfigValues().containsKey("mustNotContainKeywords"), 
            "应该包含mustNotContainKeywords配置");
    }

    /**
     * 测试场景3：相似文件归类测试
     * 
     * 目的：验证相似文件能够被正确归类到合集文件夹
     * 测试数据：
     * - 创建3个相似文件：周杰伦-青花瓷.mp3, 周杰伦-青花瓷.flac, 周杰伦-青花瓷.wav
     * - 相似度阈值：0.9
     * 断言：
     * - 分析阶段生成3条变更记录
     * - 所有记录的changed状态为true
     * - 所有记录的状态为PENDING
     * - 执行后文件被移动到合集文件夹
     * - 源文件不存在
     */
    @Test
    public void testSimilarFilesCollection() throws Exception {
        File file1 = createTestFile("周杰伦-青花瓷.mp3", "content1");
        File file2 = createTestFile("周杰伦-青花瓷.flac", "content2");
        File file3 = createTestFile("周杰伦-青花瓷.wav", "content3");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new java.util.HashMap<>());
        config.getConfigValues().put("targetDirectory", tempDir.toString() + "/collected");
        config.getConfigValues().put("targetType", "FOLDERS_ONLY");
        config.getConfigValues().put("similarityThreshold", "0.9");
        config.getConfigValues().put("collectionSuffix", "【合集】");
        config.getConfigValues().put("mustContainKeywords", "");
        config.getConfigValues().put("mustNotContainKeywords", "");
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(file1.getAbsolutePath());
        filePaths.add(file2.getAbsolutePath());
        filePaths.add(file3.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-collection", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertEquals(3, records.size(), "应该生成3条变更记录");
        
        for (ChangeRecord record : records) {
            assertChangeRecord(record, true, "PENDING");
            assertEquals("MOVE", record.getOperationType(), "操作类型应该是MOVE");
        }
        
        List<ChangeRecord> executionResults = strategyService.executeStrategy("file-collection", filePaths, config);
        
        assertEquals(3, executionResults.size(), "应该执行3条变更记录");
        for (ChangeRecord record : executionResults) {
            assertChangeRecord(record, true, "SUCCESS");
        }
        
        assertFileExists(file1, false);
        assertFileExists(file2, false);
        assertFileExists(file3, false);
    }

    /**
     * 测试场景4：关键词过滤测试
     * 
     * 目的：验证必须包含和必须不包含的关键词过滤功能
     * 测试数据：
     * - 创建4个文件：
     *   - CD1-歌曲1.mp3（包含CD关键词）
     *   - CD2-歌曲2.mp3（包含CD关键词）
     *   - Album-歌曲3.mp3（包含Album关键词）
     *   - 普通歌曲.mp3（不包含任何关键词）
     * - 必须包含关键词：CD,系列,合集
     * - 必须不包含关键词：下载,Album,群星
     * 断言：
     * - 只有CD1-歌曲1.mp3和CD2-歌曲2.mp3被归类
     * - Album-歌曲3.mp3被排除（包含必须不包含的关键词）
     * - 普通歌曲.mp3被排除（不包含必须包含的关键词）
     */
    @Test
    public void testKeywordFiltering() throws Exception {
        File file1 = createTestFile("CD1-歌曲1.mp3", "content1");
        File file2 = createTestFile("CD2-歌曲2.mp3", "content2");
        File file3 = createTestFile("Album-歌曲3.mp3", "content3");
        File file4 = createTestFile("普通歌曲.mp3", "content4");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new java.util.HashMap<>());
        config.getConfigValues().put("targetDirectory", tempDir.toString() + "/collected");
        config.getConfigValues().put("targetType", "FOLDERS_ONLY");
        config.getConfigValues().put("similarityThreshold", "0.9");
        config.getConfigValues().put("collectionSuffix", "【合集】");
        config.getConfigValues().put("mustContainKeywords", "CD,系列,合集");
        config.getConfigValues().put("mustNotContainKeywords", "下载,Album,群星");
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(file1.getAbsolutePath());
        filePaths.add(file2.getAbsolutePath());
        filePaths.add(file3.getAbsolutePath());
        filePaths.add(file4.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-collection", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertEquals(2, records.size(), "应该只有2个文件被归类（包含CD关键词）");
        
        List<String> originalNames = new ArrayList<>();
        for (ChangeRecord record : records) {
            originalNames.add(record.getOriginalName());
        }
        
        assertTrue(originalNames.contains("CD1-歌曲1.mp3"), "CD1-歌曲1.mp3应该被归类");
        assertTrue(originalNames.contains("CD2-歌曲2.mp3"), "CD2-歌曲2.mp3应该被归类");
        assertFalse(originalNames.contains("Album-歌曲3.mp3"), "Album-歌曲3.mp3应该被排除");
        assertFalse(originalNames.contains("普通歌曲.mp3"), "普通歌曲.mp3应该被排除");
    }

    /**
     * 测试场景5：非相似文件不归类测试
     * 
     * 目的：验证不相似的文件不会被归类
     * 测试数据：
     * - 创建3个不相似的文件：
     *   - 周杰伦-青花瓷.mp3
     *   - 林俊杰-江南.mp3
     *   - 蔡依林-倒带.mp3
     * - 相似度阈值：0.9
     * 断言：
     * - 分析阶段生成3条变更记录
     * - 所有记录的changed状态为false
     * - 执行后文件保持原位置
     */
    @Test
    public void testNonSimilarFilesNotCollected() throws Exception {
        File file1 = createTestFile("周杰伦-青花瓷.mp3", "content1");
        File file2 = createTestFile("林俊杰-江南.mp3", "content2");
        File file3 = createTestFile("蔡依林-倒带.mp3", "content3");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new java.util.HashMap<>());
        config.getConfigValues().put("targetDirectory", tempDir.toString() + "/collected");
        config.getConfigValues().put("targetType", "FOLDERS_ONLY");
        config.getConfigValues().put("similarityThreshold", "0.9");
        config.getConfigValues().put("collectionSuffix", "【合集】");
        config.getConfigValues().put("mustContainKeywords", "");
        config.getConfigValues().put("mustNotContainKeywords", "");
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(file1.getAbsolutePath());
        filePaths.add(file2.getAbsolutePath());
        filePaths.add(file3.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-collection", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertEquals(3, records.size(), "应该生成3条变更记录");
        
        for (ChangeRecord record : records) {
            assertFalse(record.isChanged(), "不相似的文件不应该被归类");
        }
        
        List<ChangeRecord> executionResults = strategyService.executeStrategy("file-collection", filePaths, config);
        
        for (ChangeRecord record : executionResults) {
            assertEquals("SKIPPED", record.getStatus(), "不相似的文件应该被跳过");
        }
        
        assertFileExists(file1, true);
        assertFileExists(file2, true);
        assertFileExists(file3, true);
    }

    /**
     * 测试场景6：多个文件批量归类测试
     * 
     * 目的：验证批量处理多个文件的功能
     * 测试数据：
     * - 创建10个相似文件
     * 断言：
     * - 分析阶段生成10条变更记录
     * - 执行阶段成功处理所有文件
     * - 所有文件被移动到合集文件夹
     */
    @Test
    public void testBatchFileCollection() throws Exception {
        List<File> files = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            File file = createTestFile("周杰伦-青花瓷-" + i + ".mp3", "content" + i);
            files.add(file);
        }
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new java.util.HashMap<>());
        config.getConfigValues().put("targetDirectory", tempDir.toString() + "/collected");
        config.getConfigValues().put("targetType", "FOLDERS_ONLY");
        config.getConfigValues().put("similarityThreshold", "0.9");
        config.getConfigValues().put("collectionSuffix", "【合集】");
        config.getConfigValues().put("mustContainKeywords", "");
        config.getConfigValues().put("mustNotContainKeywords", "");
        
        List<String> filePaths = new ArrayList<>();
        for (File file : files) {
            filePaths.add(file.getAbsolutePath());
        }
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-collection", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertEquals(10, records.size(), "应该生成10条变更记录");
        
        List<ChangeRecord> executionResults = strategyService.executeStrategy("file-collection", filePaths, config);
        
        assertEquals(10, executionResults.size(), "应该执行10条变更记录");
        
        for (File file : files) {
            assertFileExists(file, false);
        }
    }

    /**
     * 测试场景7：空文件列表处理测试
     * 
     * 目的：验证空文件列表的处理
     * 断言：
     * - 分析阶段返回空列表
     * - 执行阶段返回空列表
     */
    @Test
    public void testEmptyFileList() {
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new java.util.HashMap<>());
        config.getConfigValues().put("targetDirectory", tempDir.toString() + "/collected");
        config.getConfigValues().put("targetType", "FOLDERS_ONLY");
        config.getConfigValues().put("similarityThreshold", "0.9");
        
        List<String> filePaths = new ArrayList<>();
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-collection", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertTrue(records.isEmpty(), "空文件列表应该返回空变更记录");
        
        List<ChangeRecord> executionResults = strategyService.executeStrategy("file-collection", filePaths, config);
        
        assertNotNull(executionResults, "执行结果不应为空");
        assertTrue(executionResults.isEmpty(), "空文件列表应该返回空执行结果");
    }

    /**
     * 测试场景8：非存在文件处理测试
     * 
     * 目的：验证非存在文件的处理
     * 断言：
     * - 分析阶段返回空列表
     * - 执行阶段返回空列表
     */
    @Test
    public void testNonExistentFiles() {
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new java.util.HashMap<>());
        config.getConfigValues().put("targetDirectory", tempDir.toString() + "/collected");
        config.getConfigValues().put("targetType", "FOLDERS_ONLY");
        config.getConfigValues().put("similarityThreshold", "0.9");
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add("/tmp/non_existent_file1.txt");
        filePaths.add("/tmp/non_existent_file2.txt");
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-collection", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertTrue(records.isEmpty(), "非存在文件应该返回空变更记录");
        
        List<ChangeRecord> executionResults = strategyService.executeStrategy("file-collection", filePaths, config);
        
        assertNotNull(executionResults, "执行结果不应为空");
        assertTrue(executionResults.isEmpty(), "非存在文件应该返回空执行结果");
    }
}

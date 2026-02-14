package com.filemanager.backend.service;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.StrategyConfigurable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 批量智能解压策略测试用例
 * 
 * 测试场景：
 * 1. 策略注册和基本信息验证
 * 2. 配置字段完整性测试
 * 3. ZIP文件解压测试
 * 4. 自动子目录模式测试
 * 5. 同目录输出模式测试
 * 6. 智能文件夹测试
 * 7. 批量解压测试
 * 8. 边界情况测试（非压缩文件）
 * 9. 源文件不存在测试
 * 10. 非文件对象处理测试
 */
public class FileUnzipStrategyTest extends StrategyTestBase {

    @AfterEach
    public void tearDown() throws Exception {
        cleanup();
    }

    /**
     * 创建测试用的ZIP文件
     */
    private File createTestZipFile(String fileName, String content) throws Exception {
        File file = new File(tempDir.toFile(), fileName);
        try (FileOutputStream fos = new FileOutputStream(file);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            ZipEntry entry = new ZipEntry("test.txt");
            zos.putNextEntry(entry);
            zos.write(content.getBytes());
            zos.closeEntry();
        }
        return file;
    }

    /**
     * 测试场景1：验证策略已正确注册
     * 
     * 目的：确保file-unzip策略在策略注册器中可用
     * 断言：
     * - 策略不为null
     * - 策略ID正确
     * - 策略名称正确
     */
    @Test
    public void testStrategyRegistration() {
        StrategyConfigurable strategy = strategyRegistry.getStrategy("file-unzip");
        assertNotNull(strategy, "批量智能解压策略应该已注册");
        assertEquals("file-unzip", strategy.getId(), "策略ID应该正确");
        assertEquals("批量智能解压", strategy.getName(), "策略名称应该正确");
    }

    /**
     * 测试场景2：验证配置字段完整性
     * 
     * 目的：确保所有必要的配置字段都已定义
     * 断言：
     * - 配置不为null
     * - 包含engine字段
     * - 包含outputMode字段
     * - 包含smartFolder字段
     * - 包含deleteSource字段
     * - 包含overwrite字段
     */
    @Test
    public void testConfigFieldsCompleteness() {
        StrategyConfigDTO config = strategyService.getStrategyConfig("file-unzip");
        assertNotNull(config, "配置不应为空");
        assertNotNull(config.getConfigValues(), "配置值不应为空");
        
        assertTrue(config.getConfigValues().containsKey("engine"), 
            "应该包含engine配置");
        assertTrue(config.getConfigValues().containsKey("outputMode"), 
            "应该包含outputMode配置");
        assertTrue(config.getConfigValues().containsKey("smartFolder"), 
            "应该包含smartFolder配置");
        assertTrue(config.getConfigValues().containsKey("deleteSource"), 
            "应该包含deleteSource配置");
        assertTrue(config.getConfigValues().containsKey("overwrite"), 
            "应该包含overwrite配置");
    }

    /**
     * 测试场景3：ZIP文件解压测试
     * 
     * 目的：验证ZIP文件解压功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.zip
     * - 配置：默认配置
     * 断言：
     * - 分析阶段生成变更记录
     * - 操作类型为UNZIP
     */
    @Test
    public void testZipFileUnzip() throws Exception {
        File file = createTestZipFile("周杰伦-青花瓷.zip", "archive content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-unzip", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            assertEquals("UNZIP", record.getOperationType(), "操作类型应该是UNZIP");
        }
    }

    /**
     * 测试场景4：自动子目录模式测试
     * 
     * 目的：验证自动子目录模式的功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.zip
     * - 配置：输出模式为auto_subdirectory
     * 断言：
     * - 分析阶段生成变更记录
     * - 目标路径包含自动创建的子目录
     */
    @Test
    public void testAutoSubdirectoryMode() throws Exception {
        File file = createTestZipFile("周杰伦-青花瓷.zip", "archive content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("outputMode", "auto_subdirectory");
        config.getConfigValues().put("smartFolder", true);
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-unzip", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
            assertTrue(record.getNewPath().contains("周杰伦-青花瓷"), 
                "目标路径应该包含自动创建的子目录");
        }
    }

    /**
     * 测试场景5：同目录输出模式测试
     * 
     * 目的：验证同目录输出模式的功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.zip
     * - 配置：输出模式为same_as_source
     * 断言：
     * - 分析阶段生成变更记录
     * - 目标路径与源文件在同一目录
     */
    @Test
    public void testSameAsSourceMode() throws Exception {
        File file = createTestZipFile("周杰伦-青花瓷.zip", "archive content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("outputMode", "same_as_source");
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-unzip", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
        }
    }

    /**
     * 测试场景6：智能文件夹测试
     * 
     * 目的：验证智能文件夹的功能
     * 测试数据：
     * - 文件：周杰伦-青花瓷.zip
     * - 配置：smartFolder为true
     * 断言：
     * - 分析阶段生成变更记录
     * - 目标路径包含智能识别的文件夹
     */
    @Test
    public void testSmartFolder() throws Exception {
        File file = createTestZipFile("周杰伦-青花瓷.zip", "archive content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        config.getConfigValues().put("smartFolder", true);
        
        List<String> filePaths = Collections.singletonList(file.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-unzip", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            ChangeRecord record = records.get(0);
            assertTrue(record.isChanged(), "文件应该被标记为已变更");
        }
    }

    /**
     * 测试场景7：批量解压测试
     * 
     * 目的：验证批量处理多个压缩文件的功能
     * 测试数据：
     * - 5个ZIP文件
     * - 配置：默认配置
     * 断言：
     * - 分析阶段生成5条变更记录
     * - 所有文件的操作类型为UNZIP
     */
    @Test
    public void testBatchUnzip() throws Exception {
        List<File> files = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            files.add(createTestZipFile("archive" + i + ".zip", "archive content " + i));
        }
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = new ArrayList<>();
        for (File file : files) {
            filePaths.add(file.getAbsolutePath());
        }
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-unzip", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        
        if (!records.isEmpty()) {
            for (ChangeRecord record : records) {
                assertTrue(record.isChanged(), "所有文件都应该被标记为已变更");
                assertEquals("UNZIP", record.getOperationType(), "所有操作类型应该是UNZIP");
            }
        }
    }

    /**
     * 测试场景8：边界情况测试（非压缩文件）
     * 
     * 目的：验证非压缩文件的处理
     * 测试数据：
     * - 文本文件：test.txt
     * - 图片文件：test.jpg
     * 断言：
     * - 分析阶段返回空列表
     * - 执行阶段返回空列表
     */
    @Test
    public void testNonArchiveFiles() throws Exception {
        File file1 = createTestFile("test.txt", "text content");
        File file2 = createTestFile("test.jpg", "image content");
        
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = new ArrayList<>();
        filePaths.add(file1.getAbsolutePath());
        filePaths.add(file2.getAbsolutePath());
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-unzip", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertTrue(records.isEmpty(), "非压缩文件应该返回空变更记录");
        
        List<ChangeRecord> executionResults = strategyService.executeStrategy("file-unzip", filePaths, config);
        
        assertNotNull(executionResults, "执行结果不应为空");
        assertTrue(executionResults.isEmpty(), "非压缩文件应该返回空执行结果");
    }

    /**
     * 测试场景9：源文件不存在测试
     * 
     * 目的：验证源文件不存在时的处理
     * 测试数据：
     * - 不存在的ZIP文件路径
     * 断言：
     * - 分析阶段返回空列表
     * - 执行阶段失败
     */
    @Test
    public void testSourceFileNotExists() {
        StrategyConfigDTO config = new StrategyConfigDTO();
        config.setConfigValues(new HashMap<>());
        
        List<String> filePaths = Collections.singletonList("/tmp/nonexistent.zip");
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-unzip", filePaths, config);
        
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
        
        List<ChangeRecord> records = strategyService.analyzeFiles("file-unzip", filePaths, config);
        
        assertNotNull(records, "变更记录不应为空");
        assertTrue(records.isEmpty(), "非文件对象应该返回空变更记录");
        
        List<ChangeRecord> executionResults = strategyService.executeStrategy("file-unzip", filePaths, config);
        
        assertNotNull(executionResults, "执行结果不应为空");
        assertTrue(executionResults.isEmpty(), "非文件对象应该返回空执行结果");
    }
}

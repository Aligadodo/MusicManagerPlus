# 老架构测试用例迁移总结

## 概述

本文档总结了从老架构迁移到新架构的关键测试用例，特别是文件归类策略的测试用例。

## 迁移的测试用例

### 1. FileCollectionAlgorithmTest

**文件位置**: `backend/src/test/java/com/filemanager/backend/service/FileCollectionAlgorithmTest.java`

**测试范围**:
- 相似度计算
- 文件名标准化
- 特殊符号和数字处理
- 相同标题不同序号识别
- 关键词提取
- 文件聚类
- 合集判断

**测试用例数量**: 10个

#### 测试场景1：相似度计算

**目的**: 验证文本相似度计算算法的准确性

**测试数据**:
- 相似文件：张平福《古筝天地①月圆花好》 vs 张平福《古筝天地②草原之夜》
- 不相似文件：张平福《古筝天地①月圆花好》 vs 张平福《萨克斯ChaCha浪漫旋律》

**断言**:
- 相似文件的相似度 > 0.8
- 不相似文件的相似度 < 0.6

#### 测试场景2：相同标题不同序号识别

**目的**: 验证算法能够识别相同标题不同序号的文件

**测试数据**:
- 相同标题不同序号：张平福《古筝天地①月圆花好》 vs 张平福《古筝天地②草原之夜》
- 不同标题：张平福《古筝天地①月圆花好》 vs 张平福《萨克斯ChaCha浪漫旋律》

**断言**:
- 相同标题不同序号的文件应该被识别为系列
- 不同标题的文件不应该被识别为系列

#### 测试场景3：关键词提取

**目的**: 验证算法能够从文件名中提取核心关键词

**测试数据**:
- 文件名：张平福《古筝天地①月圆花好》专辑.(FLAC)

**断言**:
- 应该提取出核心关键词（张平福、古筝天地、月圆花好）

#### 测试场景4：特殊符号和数字处理

**目的**: 验证算法能够正确处理特殊符号和数字

**测试数据**:
- 输入：张平福《古筝天地①月圆花好》VOL.01

**断言**:
- 应该保留核心内容（张平福古筝天地）

#### 测试场景5：龙音文件名标准化

**目的**: 验证算法能够正确处理龙音唱片格式的文件名

**测试数据**:
- 望秦川-王中山古筝专辑之四
- 溟山-王中山古筝专辑(一)
- 黄河魂-王中山古筝专辑(二)
- 夜深沉-王中山古筝专辑之三

**断言**:
- 标准化后的文件名应该相似
- 相似度应该能够识别为同一系列

#### 测试场景6：滚石文件名标准化

**目的**: 验证算法能够正确处理滚石唱片格式的文件名

**测试数据**:
- 群星.2001 - 文艺民歌时代【滚石】【WAV+CUE】
- 群星.2002 - 文艺民歌时代2【滚石】【WAV+CUE】

**断言**:
- 标准化后的文件名应该相似
- 相似度应该能够识别为同一系列

#### 测试场景7：15首精选滚石年度强打金曲文件名标准化

**目的**: 验证算法能够正确处理复杂的专辑名称

**测试数据**:
- 滚石群星200雀巢咖啡时尚精选 15首精选滚石年度强打金曲[滚石][WAV+CUE]
- 群星2000-雀巢咖啡时尚精选 15首精选滚石年度强打金曲[引进版][WAV+CUE]

**断言**:
- 标准化后的文件名应该相似
- 相似度应该能够识别为同一系列

#### 测试场景8：龙音文采华音版-轻舟随波系列的相似度计算

**目的**: 验证算法能够正确识别龙音文采华音版-轻舟随波系列

**测试数据**:
- 龙音唱片.-.[龙音文采华音版-轻舟随波系列④]排箫爱情篇-罗密欧与朱丽叶
- 龙音唱片.-.[龙音文采华音版-轻舟随波系列⑤钢琴弄潮篇-爱情故事
- 龙音唱片.-.[龙音文采华音版-轻舟随波系列⑥华夏风情篇-睡莲
- 龙音唱片.-.[龙音文采华音版-轻舟随波系列⑦-异国风情篇-美丽的梭罗河

**断言**:
- 所有文件之间的相似度都应该 > 0.7
- 所有文件都应该被识别为相似

#### 测试场景9：不同相似度阈值的影响

**目的**: 验证不同相似度阈值对聚类结果的影响

**测试数据**:
- 相似度阈值0.9：应该生成较少的集群
- 相似度阈值0.7：应该生成较多的集群

**断言**:
- 较高的阈值应该生成较少的集群
- 较低的阈值应该生成较多的集群

#### 测试场景10：边界条件测试

**目的**: 验证算法在边界条件下的行为

**测试数据**:
- 空字符串
- 单个字符
- 完全相同的字符串
- 完全不同的字符串

**断言**:
- 空字符串的相似度应该为0
- 完全相同的字符串的相似度应该为1
- 完全不同的字符串的相似度应该很低

## 老架构测试框架

### TestFramework

**文件位置**: `src/test/java/com/filemanager/strategy/collection/test/TestFramework.java`

**功能**:
- 整合所有测试组件
- 验证测试用例
- 生成合集
- 评估结果

**主要方法**:
- `validateTestCase(TestCase testCase)`: 验证测试用例
- `validateFromTestDataDir()`: 从测试数据目录验证所有测试用例

### TestCase

**文件位置**: `src/test/java/com/filemanager/strategy/collection/test/TestCase.java`

**功能**:
- 定义测试用例结构
- 包含文件夹列表
- 包含预期合集组合
- 支持命名策略

**主要属性**:
- `testName`: 测试名称
- `description`: 测试描述
- `allFolders`: 所有文件夹
- `expectedCollections`: 预期合集
- `namingStrategy`: 命名策略

### TestCasePersister

**文件位置**: `src/test/java/com/filemanager/strategy/collection/test/TestCasePersister.java`

**功能**:
- 保存测试用例到JSON文件
- 从JSON文件加载测试用例
- 加载指定目录下的所有测试用例

**主要方法**:
- `saveTestCase(TestCase testCase, String filename)`: 保存测试用例
- `loadTestCase(String filename)`: 加载测试用例
- `loadAllTestCases()`: 加载所有测试用例

### RegressionTest

**文件位置**: `src/test/java/com/filemanager/strategy/collection/test/RegressionTest.java`

**功能**:
- 运行回归测试
- 验证所有测试用例
- 生成测试报告

**主要方法**:
- `runRegressionTests()`: 运行回归测试

## 其他老架构测试用例

### FilenameNormalization测试

**文件位置**: `src/test/java/com/filemanager/strategy/collection/TestFilenameNormalization.java`

**测试内容**:
- 龙音文件名标准化
- 滚石文件名标准化
- 相似度计算

### 15SongsNormalization测试

**文件位置**: `src/test/java/com/filemanager/strategy/collection/Test15SongsNormalization.java`

**测试内容**:
- 15首精选滚石年度强打金曲文件名标准化
- 相似度计算

### LongyinWencaiSimilarity测试

**文件位置**: `src/test/java/com/filemanager/strategy/collection/test/LongyinWencaiSimilarityTest.java`

**测试内容**:
- 龙音文采华音版-轻舟随波系列的相似度计算
- 多个文件之间的相似度比较

## 迁移策略

### 1. 直接迁移

对于简单的测试用例，直接从老架构复制到新架构，修改包名和导入。

**示例**:
```java
// 老架构
package com.filemanager.strategy;
import com.filemanager.strategy.collection.TextSimilarityCalculator;

// 新架构
package com.filemanager.backend.service;
import com.filemanager.plugin.impl.filecollection.collection.TextSimilarityCalculator;
```

### 2. 适配迁移

对于需要适配的测试用例，修改测试方法以适应新架构的API。

**示例**:
```java
// 老架构
TextSimilarityCalculator calculator = new TextSimilarityCalculator(0.8);
double similarity = calculator.calculateSimilarity(s1, s2);

// 新架构
double similarity = TextSimilarityCalculator.calculateSimilarity(s1, s2);
```

### 3. 重构迁移

对于需要重构的测试用例，重新设计测试结构以适应新架构。

**示例**:
```java
// 老架构
@Before
public void setUp() {
    testDir = new File(System.getProperty("java.io.tmpdir"), "test_file_collection");
    testDir.mkdirs();
}

// 新架构
@BeforeEach
public void setUp() {
    testDir = new File(System.getProperty("java.io.tmpdir"), "test_file_collection_" + System.currentTimeMillis());
    testDir.mkdirs();
}
```

## 测试用例对比

### 老架构测试用例

| 测试类 | 测试用例数量 | 状态 |
|---------|-------------|------|
| FileCollectionStrategyTest | 5 | ✅ 已迁移 |
| AudioConverterStrategyTest | 5 | ⚠️ 部分迁移 |
| FileCleanupStrategyTest | 5 | ⚠️ 部分迁移 |
| FileMigrateStrategyTest | 5 | ❌ 未迁移 |
| FileTypeFixStrategyTest | 5 | ❌ 未迁移 |
| TestFilenameNormalization | 1 | ✅ 已迁移 |
| Test15SongsNormalization | 1 | ✅ 已迁移 |
| LongyinWencaiSimilarityTest | 1 | ✅ 已迁移 |
| TestFramework | 1 | ❌ 未迁移 |
| TestCase | 1 | ❌ 未迁移 |
| TestCasePersister | 1 | ❌ 未迁移 |
| RegressionTest | 1 | ❌ 未迁移 |

### 新架构测试用例

| 测试类 | 测试用例数量 | 状态 |
|---------|-------------|------|
| FileCollectionStrategyTest | 8 | ✅ 已完成 |
| FileCollectionAlgorithmTest | 10 | ✅ 已完成 |
| StrategyTestBase | 1 | ✅ 已完成 |

## 迁移进度

### 已完成

- ✅ FileCollectionStrategyTest（8个测试用例）
- ✅ FileCollectionAlgorithmTest（10个测试用例）
- ✅ StrategyTestBase（测试基类）

### 进行中

- ⚠️ AudioConverterStrategyTest（部分迁移）
- ⚠️ FileCleanupStrategyTest（部分迁移）

### 待迁移

- ❌ FileMigrateStrategyTest
- ❌ FileTypeFixStrategyTest
- ❌ FileUnzipStrategyTest
- ❌ MetadataScraperStrategyTest
- ❌ CueSplitterStrategyTest
- ❌ CueFileRenameStrategyTest
- ❌ NcmIntegratedStrategyTest
- ❌ TrackNumberStrategyTest
- ❌ FileRenameStrategyTest
- ❌ AlbumDirNormalizeStrategyTest
- ❌ AdvancedRenameStrategyTest

## 下一步计划

### 第一优先级

1. **完成FileCleanupStrategy测试用例迁移**
   - 迁移重复文件分析器测试
   - 迁移去重策略管理器测试
   - 迁移清理模式测试

2. **完成AudioConverterStrategy测试用例迁移**
   - 迁移格式转换测试
   - 迁移质量设置测试
   - 迁移批量处理测试

### 第二优先级

3. **完成FileMigrateStrategy测试用例迁移**
   - 迁移移动操作测试
   - 迁移复制操作测试
   - 迁移跨磁盘处理测试

4. **完成FileTypeFixStrategy测试用例迁移**
   - 迁移类型检测测试
   - 迁移类型修复测试
   - 迁移批量处理测试

### 第三优先级

5. **完成其他策略测试用例迁移**
   - 迁移所有剩余策略的测试用例
   - 确保测试覆盖率≥80%
   - 确保分支覆盖率≥75%

## 总结

从老架构迁移的测试用例主要集中在文件归类策略的算法测试，包括：

1. **相似度计算测试**: 验证文本相似度计算算法的准确性
2. **文件名标准化测试**: 验证算法能够正确处理各种格式的文件名
3. **特殊符号和数字处理测试**: 验证算法能够正确处理特殊符号和数字
4. **相同标题不同序号识别测试**: 验证算法能够识别相同标题不同序号的文件
5. **关键词提取测试**: 验证算法能够从文件名中提取核心关键词
6. **边界条件测试**: 验证算法在边界条件下的行为
7. **不同相似度阈值影响测试**: 验证不同相似度阈值对聚类结果的影响

这些测试用例覆盖了文件归类策略的核心算法，确保了算法的正确性和稳定性。

---

**最后更新**: 2026-02-14
**版本**: 1.0.0

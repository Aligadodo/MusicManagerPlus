# 合集自动化测试开发指南

## 目录
- [概述](#概述)
- [设计思路](#设计思路)
- [架构设计](#架构设计)
- [核心组件](#核心组件)
- [迭代流程](#迭代流程)
- [关键技术点](#关键技术点)
- [注意事项](#注意事项)
- [最佳实践](#最佳实践)
- [常见问题](#常见问题)
- [示例](#示例)

## 概述

### 目标
构建一套完整的自动化测试框架，用于验证和优化音乐文件合集生成算法的准确性和稳定性。该框架能够：
- 自动扫描文件夹并生成测试用例
- 持久化测试用例用于回归测试
- 评估算法效果并提供量化指标
- 支持多版本算法对比和优化

### 核心价值
- **自动化**: 减少手动测试工作量，提高测试效率
- **可重复**: 确保测试结果的一致性和可追溯性
- **可量化**: 提供明确的匹配率、相似度等评估指标
- **可扩展**: 支持新增测试用例和评估维度

## 设计思路

### 1. 测试用例抽象
将测试用例抽象为三个核心要素：
- **文件夹列表**: 待测试的原始文件夹
- **预期合集**: 人工标注的正确合集分组
- **评估指标**: 用于量化算法效果的指标体系

### 2. 分层架构
采用分层设计，各层职责清晰：
- **数据层**: 负责文件夹扫描和测试用例持久化
- **算法层**: 负责合集生成和名称计算
- **评估层**: 负责结果验证和指标计算
- **测试层**: 负责测试执行和结果汇总

### 3. 持久化策略
使用JSON格式持久化测试用例和测试结果：
- 测试用例文件: `test-cases/test-data/{test_name}.json`
- 测试结果文件: `test-cases/{test_name}_{timestamp}_result.json`

## 架构设计

### 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                     TestFramework                            │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │FolderScanner │  │TestCaseGen   │  │AlgorithmEval │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │TestCasePers  │  │TestValidator │  │RegressionTest│     │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                  Algorithm Layer                             │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────────┐  ┌──────────────────┐                │
│  │FileClusteringAlgo│  │FilenameNormalizer │                │
│  └──────────────────┘  └──────────────────┘                │
│  ┌──────────────────┐  ┌──────────────────┐                │
│  │TextSimilarityCalc│  │CollectionDeterm  │                │
│  └──────────────────┘  └──────────────────┘                │
└─────────────────────────────────────────────────────────────┘
```

### 目录结构

```
src/test/java/com/filemanager/strategy/collection/test/
├── TestCase.java                    # 测试用例数据模型
├── TestCaseBuilder.java             # 测试用例构建器
├── TestCasePersister.java           # 测试用例持久化
├── TestValidationResult.java       # 验证结果模型
├── ExpectedCollection.java         # 预期合集模型
├── FolderScanner.java               # 文件夹扫描器
├── TestCaseGenerator.java           # 测试用例生成器
├── AlgorithmEvaluator.java          # 算法评估器
├── TestFramework.java               # 测试框架核心
├── RegressionTest.java              # JUnit回归测试
└── RegressionTestRunner.java       # 回归测试运行器

test-cases/
├── test-data/                       # 测试用例数据
│   ├── rolling_stone_test.json
│   ├── polygram_test.json
│   ├── longyin_test.json
│   └── longyin_full_test.json
└── *_result.json                    # 测试结果文件
```

## 核心组件

### 1. TestCase (测试用例)

**职责**: 存储测试用例的所有信息

**核心字段**:
```java
public class TestCase {
    private String testName;              // 测试用例名称
    private String sourcePath;            // 源路径
    private List<String> folders;         // 文件夹列表
    private List<ExpectedCollection> expectedCollections; // 预期合集
    private long createdAt;               // 创建时间
}
```

**使用场景**:
- 作为测试用例的基本数据结构
- 支持JSON序列化和反序列化
- 用于回归测试的输入数据

### 2. TestCasePersister (测试用例持久化)

**职责**: 负责测试用例和测试结果的持久化

**核心方法**:
```java
public class TestCasePersister {
    public boolean saveTestCase(TestCase testCase, String filename);
    public TestCase loadTestCase(String filename);
    public boolean saveValidationResult(TestValidationResult result, String filename);
}
```

**关键实现**:
- 使用FastJSON进行JSON序列化
- 使用UTF-8编码确保中文正确显示
- 避免使用`SerializerFeature.BrowserCompatible`以防止Unicode转义

**注意事项**:
```java
// 正确的序列化方式
String jsonString = JSON.toJSONString(object, 
    SerializerFeature.PrettyFormat, 
    SerializerFeature.WriteMapNullValue, 
    SerializerFeature.WriteDateUseDateFormat);

// 使用UTF-8编码写入文件
try (OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8")) {
    writer.write(jsonString);
}
```

### 3. FolderScanner (文件夹扫描器)

**职责**: 扫描指定目录下的所有文件夹

**核心方法**:
```java
public class FolderScanner {
    public List<String> scanFolders(String directoryPath);
}
```

**使用场景**:
- 生成测试用例时扫描文件夹
- 获取实际的文件夹列表用于算法验证

### 4. TestCaseGenerator (测试用例生成器)

**职责**: 基于文件夹列表生成测试用例

**核心方法**:
```java
public class TestCaseGenerator {
    public TestCase generateTestCase(String testName, String sourcePath, 
                                     List<String> folders);
}
```

**使用场景**:
- 自动化生成测试用例框架
- 为人工标注提供基础数据

### 5. AlgorithmEvaluator (算法评估器)

**职责**: 执行合集生成算法并评估结果

**核心方法**:
```java
public class AlgorithmEvaluator {
    public TestValidationResult evaluate(TestCase testCase, 
                                          FileClusteringAlgorithm algorithm);
}
```

**评估指标**:
- 匹配率 (Match Rate): 匹配的合集数 / 预期合集数
- 名称相似度 (Name Similarity): 实际名称与预期名称的相似度
- 文件夹重叠率 (Folder Overlap Rate): 实际文件夹与预期文件夹的重叠比例

### 6. TestValidationResult (验证结果)

**职责**: 存储验证结果和评估指标

**核心字段**:
```java
public class TestValidationResult {
    private String testName;
    private long timestamp;
    private int totalExpectedCollections;
    private int totalActualCollections;
    private int matchedCollections;
    private int missedCollections;
    private int extraCollections;
    private double matchRate;
    private String scoreLevel;
    private List<CollectionMatch> collectionMatches;
    private List<String> missedCollectionNames;
    private List<String> extraCollectionNames;
}
```

**评分等级**:
- 优秀: 匹配率 >= 90%
- 良好: 匹配率 >= 75%
- 一般: 匹配率 >= 60%
- 及格: 匹配率 >= 50%
- 不及格: 匹配率 < 50%

### 7. RegressionTest (回归测试)

**职责**: JUnit测试类，执行所有测试用例

**核心方法**:
```java
public class RegressionTest {
    @Test
    public void runAllRegressionTests();
}
```

**使用场景**:
- 每次修改算法后运行回归测试
- 确保算法改进不会降低已有测试用例的准确率

## 迭代流程

### 标准迭代流程

```
1. 准备测试数据
   ↓
2. 创建测试用例
   ↓
3. 运行测试并分析结果
   ↓
4. 识别问题并优化算法
   ↓
5. 重新运行测试验证
   ↓
6. 持续迭代直到达到目标
```

### 详细步骤

#### 步骤1: 准备测试数据

**目标**: 收集具有代表性的文件夹数据

**操作**:
```bash
# 使用FolderScanner扫描目标目录
FolderScanner scanner = new FolderScanner();
List<String> folders = scanner.scanFolders("V:\\Z - 中文厂牌\\L - 龙音港版唱片");
```

**注意事项**:
- 使用完整数据集，避免过度拟合
- 确保数据覆盖各种命名模式
- 记录数据来源和特征

#### 步骤2: 创建测试用例

**目标**: 生成测试用例框架并人工标注

**操作**:
```java
// 生成测试用例框架
TestCase testCase = TestCaseBuilder.builder()
    .testName("longyin_full_test")
    .sourcePath("V:\\Z - 中文厂牌\\L - 龙音港版唱片")
    .folders(folders)
    .build();

// 人工标注预期合集
List<ExpectedCollection> expectedCollections = new ArrayList<>();
expectedCollections.add(new ExpectedCollection(
    "邬娜钢琴人50年", 
    Arrays.asList("[龙音海文版 CD-006]邬娜钢琴人50年-1", 
                  "[龙音海文版 CD-007]邬娜钢琴人50年-2", 
                  "[龙音海文版 CD-008]邬娜钢琴人50年-3")
));

testCase.setExpectedCollections(expectedCollections);

// 保存测试用例
TestCasePersister persister = new TestCasePersister("test-cases/test-data");
persister.saveTestCase(testCase, "longyin_full_test");
```

**注意事项**:
- 预期合集名称应简洁准确
- 确保文件夹名称与实际数据一致
- 考虑边界情况和异常数据

#### 步骤3: 运行测试并分析结果

**目标**: 执行测试并生成评估报告

**操作**:
```bash
# 运行回归测试
mvn test -Dtest=RegressionTest

# 或运行特定测试用例
mvn test -Dtest=RegressionTest#testSpecificCase
```

**结果分析**:
- 查看匹配率是否达到预期
- 分析遗漏和多余的合集
- 检查名称相似度和文件夹重叠率
- 识别算法的薄弱环节

#### 步骤4: 识别问题并优化算法

**目标**: 基于测试结果优化算法

**常见问题及解决方案**:

**问题1: 合集名称包含过多前缀**
```java
// 优化前
"龙音唱片.-.邬娜钢琴人50年"

// 优化后: 添加cleanCollectionName方法
private String cleanCollectionName(String name) {
    // 去除常见前缀
    String[] prefixesToRemove = {
        "龙音唱片\\.-\\.", "龙音唱片\\.-", "龙音唱片\\.", "龙音唱片",
        "滚石合集\\.-\\.", "滚石合集\\.-", "滚石合集\\.", "滚石"
    };
    
    for (String prefix : prefixesToRemove) {
        if (name.matches(prefix + ".*")) {
            name = name.replaceFirst(prefix, "");
            break;
        }
    }
    
    return name;
}
```

**问题2: 版本信息未被识别**
```java
// 在FilenameNormalizer中添加版本信息模式
private static final Pattern LONGYIN_VERSION_PATTERN = 
    Pattern.compile("\\[(?:海文版|龙音海文版|龙音香港版|龙音)(?:\\s*CD-\\d+)?(?:\\s*RA-\\d+)?\\]", 
                    Pattern.CASE_INSENSITIVE);

private String removeLongyinVersionInfo(String filename) {
    return LONGYIN_VERSION_PATTERN.matcher(filename).replaceAll("");
}
```

**问题3: 中文字符编码问题**
```java
// 确保使用UTF-8编码
try (OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8")) {
    writer.write(jsonString);
}

// 避免使用BrowserCompatible特性
SerializerFeature.PrettyFormat, 
SerializerFeature.WriteMapNullValue, 
SerializerFeature.WriteDateUseDateFormat
```

#### 步骤5: 重新运行测试验证

**目标**: 验证优化效果

**操作**:
```bash
# 重新编译
mvn compile

# 运行回归测试
mvn test -Dtest=RegressionTest

# 对比优化前后的结果
```

**验证要点**:
- 匹配率是否提升
- 是否引入新的问题
- 整体测试用例的平均匹配率是否保持或提升

#### 步骤6: 持续迭代

**目标**: 不断优化算法直到达到目标

**迭代策略**:
1. 优先解决影响最大的问题
2. 每次迭代只修改一个维度
3. 保持测试用例的多样性
4. 记录每次优化的效果

## 关键技术点

### 1. 文本相似度计算

**算法**: 编辑距离算法

**实现**:
```java
public class TextSimilarityCalculator {
    public static double calculateSimilarity(String s1, String s2) {
        int distance = calculateLevenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        return maxLength == 0 ? 1.0 : 1.0 - (double) distance / maxLength;
    }
    
    private static int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j], dp[i][j - 1]),
                        dp[i - 1][j - 1]
                    ) + 1;
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
}
```

### 2. 文件夹聚类算法

**核心思想**: 基于文件夹名称的相似度进行聚类

**关键步骤**:
1. 标准化文件夹名称
2. 计算名称相似度
3. 基于相似度阈值进行聚类
4. 生成合集名称

**实现要点**:
```java
public class FileClusteringAlgorithm {
    private static final double SIMILARITY_THRESHOLD = 0.6;
    
    public List<Collection> generateCollections(List<String> folders) {
        // 1. 标准化文件夹名称
        List<NormalizedFolder> normalizedFolders = normalizeFolders(folders);
        
        // 2. 计算相似度矩阵
        double[][] similarityMatrix = calculateSimilarityMatrix(normalizedFolders);
        
        // 3. 基于相似度进行聚类
        List<Cluster> clusters = clusterFolders(normalizedFolders, similarityMatrix);
        
        // 4. 生成合集名称
        return generateCollectionNames(clusters);
    }
}
```

### 3. 文件夹名称标准化

**目标**: 消除命名差异，提高相似度计算的准确性

**标准化步骤**:
1. 移除版本信息
2. 移除特殊字符
3. 统一大小写
4. 移除多余空格

**实现**:
```java
public class FilenameNormalizer {
    public String normalize(String filename) {
        // 移除版本信息
        String result = removeVersionInfo(filename);
        
        // 移除特殊字符
        result = removeSpecialCharacters(result);
        
        // 统一大小写
        result = result.toLowerCase();
        
        // 移除多余空格
        result = result.trim().replaceAll("\\s+", " ");
        
        return result;
    }
}
```

### 4. 合集名称生成

**策略**: 基于最长公共前缀生成合集名称

**实现**:
```java
private String findLongestCommonPrefix(List<String> names) {
    if (names == null || names.isEmpty()) {
        return "";
    }
    
    String prefix = names.get(0);
    for (int i = 1; i < names.size(); i++) {
        while (names.get(i).indexOf(prefix) != 0) {
            prefix = prefix.substring(0, prefix.length() - 1);
            if (prefix.isEmpty()) {
                break;
            }
        }
        if (prefix.isEmpty()) {
            break;
        }
    }
    
    // 清理合集名称
    return cleanCollectionName(prefix);
}
```

### 5. 评估指标计算

**匹配率**:
```java
double matchRate = (double) matchedCollections / totalExpectedCollections;
```

**名称相似度**:
```java
double nameSimilarity = TextSimilarityCalculator.calculateSimilarity(
    expectedName, actualName
);
```

**文件夹重叠率**:
```java
Set<String> expectedFolders = new HashSet<>(expectedCollection.getFolders());
Set<String> actualFolders = new HashSet<>(actualCollection.getFolders());
Set<String> intersection = new HashSet<>(expectedFolders);
intersection.retainAll(actualFolders);

double overlapRate = (double) intersection.size() / expectedFolders.size();
```

## 注意事项

### 1. 测试用例设计

**原则**:
- **完整性**: 覆盖各种命名模式和边界情况
- **代表性**: 反映真实数据的分布特征
- **可维护性**: 便于后续更新和扩展
- **独立性**: 各测试用例之间相互独立

**避免**:
- 过度拟合特定数据集
- 测试用例数量过少
- 测试用例过于相似
- 忽略边界情况和异常数据

### 2. 算法优化

**原则**:
- **渐进式**: 每次只修改一个维度
- **可验证**: 每次优化都有明确的验证指标
- **可回滚**: 保留优化前的版本以便回退
- **文档化**: 记录每次优化的原因和效果

**避免**:
- 一次性修改多个维度
- 没有验证就提交代码
- 忽略对其他测试用例的影响
- 过度优化导致算法复杂度过高

### 3. 数据持久化

**关键点**:
- 使用UTF-8编码确保中文正确显示
- 避免使用`SerializerFeature.BrowserCompatible`
- 使用`SerializerFeature.PrettyFormat`便于阅读
- 定期备份测试用例和测试结果

**示例**:
```java
// 正确的序列化配置
String jsonString = JSON.toJSONString(object, 
    SerializerFeature.PrettyFormat, 
    SerializerFeature.WriteMapNullValue, 
    SerializerFeature.WriteDateUseDateFormat);

// 使用UTF-8编码
try (OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8")) {
    writer.write(jsonString);
}
```

### 4. 回归测试

**执行时机**:
- 修改算法核心逻辑后
- 添加新的测试用例后
- 优化算法参数后
- 修复Bug后

**验证要点**:
- 整体匹配率是否保持或提升
- 是否引入新的问题
- 各测试用例的表现是否均衡
- 算法的泛化能力是否增强

### 5. 性能考虑

**优化方向**:
- 相似度计算算法优化
- 聚类算法效率提升
- 大规模数据处理能力
- 内存使用优化

**监控指标**:
- 测试执行时间
- 内存占用
- CPU使用率
- 磁盘I/O

## 最佳实践

### 1. 测试用例管理

**命名规范**:
- 使用描述性名称: `{record_label}_{dataset_type}_test`
- 示例: `rolling_stone_full_test`, `longyin_sample_test`

**版本控制**:
- 将测试用例纳入版本控制
- 记录每次修改的原因和影响
- 定期清理过期的测试结果文件

**分类管理**:
```
test-cases/test-data/
├── rolling_stone/          # 滚石唱片测试用例
│   ├── rolling_stone_full_test.json
│   └── rolling_stone_sample_test.json
├── polygram/               # 宝丽金测试用例
├── longyin/                # 龙音唱片测试用例
└── classical/              # 古典音乐测试用例
```

### 2. 算法优化流程

**优化前**:
1. 分析测试结果，识别问题
2. 确定优化目标和预期效果
3. 设计优化方案
4. 评估方案的风险和影响

**优化中**:
1. 实现优化代码
2. 单元测试验证
3. 运行回归测试
4. 分析测试结果

**优化后**:
1. 对比优化前后的效果
2. 记录优化过程和结果
3. 更新相关文档
4. 提交代码并标记版本

### 3. 问题诊断方法

**常见问题**:
1. **匹配率低**
   - 检查文件夹名称标准化是否充分
   - 检查相似度阈值是否合理
   - 检查合集名称生成逻辑

2. **名称相似度低**
   - 检查cleanCollectionName方法
   - 检查前缀和后缀的移除逻辑
   - 检查特殊字符的处理

3. **文件夹重叠率低**
   - 检查聚类算法的逻辑
   - 检查相似度计算的准确性
   - 检查文件夹名称的标准化

4. **中文乱码**
   - 检查文件编码是否为UTF-8
   - 检查JSON序列化配置
   - 检查控制台编码设置

### 4. 文档维护

**更新时机**:
- 添加新的测试用例后
- 修改算法核心逻辑后
- 发现新的问题或解决方案后
- 定期回顾和更新

**文档内容**:
- 设计思路和架构
- 核心组件说明
- 迭代流程和最佳实践
- 常见问题和解决方案
- 示例代码和使用说明

## 常见问题

### Q1: 如何添加新的测试用例？

**步骤**:
1. 使用FolderScanner扫描目标目录
2. 人工标注预期合集
3. 使用TestCaseBuilder构建测试用例
4. 使用TestCasePersister保存测试用例
5. 运行回归测试验证

**示例**:
```java
FolderScanner scanner = new FolderScanner();
List<String> folders = scanner.scanFolders("path/to/directory");

TestCase testCase = TestCaseBuilder.builder()
    .testName("new_test")
    .sourcePath("path/to/directory")
    .folders(folders)
    .expectedCollections(expectedCollections)
    .build();

TestCasePersister persister = new TestCasePersister("test-cases/test-data");
persister.saveTestCase(testCase, "new_test");
```

### Q2: 如何调整相似度阈值？

**位置**: FileClusteringAlgorithm.java

**方法**:
```java
private static final double SIMILARITY_THRESHOLD = 0.6; // 调整此值
```

**建议**:
- 阈值越高，聚类越严格，合集数量越多
- 阈值越低，聚类越宽松，合集数量越少
- 通过回归测试找到最佳阈值

### Q3: 如何处理中文乱码？

**解决方案**:
1. 确保文件使用UTF-8编码
2. 避免使用`SerializerFeature.BrowserCompatible`
3. 使用UTF-8编码写入文件

**示例**:
```java
try (OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8")) {
    writer.write(jsonString);
}
```

### Q4: 如何提高匹配率？

**优化方向**:
1. 改进文件夹名称标准化
2. 优化合集名称生成逻辑
3. 添加特定唱片公司的命名模式
4. 调整相似度阈值
5. 改进聚类算法

**示例**:
```java
// 添加特定前缀处理
private String cleanCollectionName(String name) {
    String[] prefixesToRemove = {
        "龙音唱片\\.-\\.", "龙音唱片\\.-", "龙音唱片\\.", "龙音唱片"
    };
    
    for (String prefix : prefixesToRemove) {
        if (name.matches(prefix + ".*")) {
            name = name.replaceFirst(prefix, "");
            break;
        }
    }
    
    return name;
}
```

### Q5: 如何回滚到之前的算法版本？

**方法**:
1. 使用Git查看历史版本
2. 恢复到之前的提交
3. 重新编译和测试

**示例**:
```bash
# 查看历史提交
git log --oneline

# 恢复到指定版本
git checkout <commit-hash>

# 重新编译
mvn compile

# 运行测试
mvn test -Dtest=RegressionTest
```

## 示例

### 示例1: 创建滚石唱片测试用例

```java
// 扫描文件夹
FolderScanner scanner = new FolderScanner();
List<String> folders = scanner.scanFolders("G:\\F - 发行商系列\\G - 滚石唱片 (Rock Records)\\滚石唱片");

// 构建预期合集
List<ExpectedCollection> expectedCollections = new ArrayList<>();
expectedCollections.add(new ExpectedCollection(
    "滚石爱情故事",
    Arrays.asList(
        "滚石爱情故事 (1)",
        "滚石爱情故事 (2)",
        "滚石爱情故事 (3)"
    )
));

expectedCollections.add(new ExpectedCollection(
    "滚石华语流行歌曲精选",
    Arrays.asList(
        "滚石华语流行歌曲精选 (1)",
        "滚石华语流行歌曲精选 (2)"
    )
));

// 构建测试用例
TestCase testCase = TestCaseBuilder.builder()
    .testName("rolling_stone_test")
    .sourcePath("G:\\F - 发行商系列\\G - 滚石唱片 (Rock Records)\\滚石唱片")
    .folders(folders)
    .expectedCollections(expectedCollections)
    .build();

// 保存测试用例
TestCasePersister persister = new TestCasePersister("test-cases/test-data");
persister.saveTestCase(testCase, "rolling_stone_test");
```

### 示例2: 运行回归测试

```bash
# 编译项目
mvn compile

# 运行所有回归测试
mvn test -Dtest=RegressionTest

# 运行特定测试用例
mvn test -Dtest=RegressionTest#testSpecificCase

# 查看测试结果
cat test-cases/*_result.json
```

### 示例3: 优化合集名称生成

```java
// 在FileClusteringAlgorithm中添加cleanCollectionName方法
private String cleanCollectionName(String name) {
    if (name == null || name.isEmpty()) {
        return name;
    }

    // 去除常见的不必要前缀
    String[] prefixesToRemove = {
        "龙音唱片\\.-\\.", "龙音唱片\\.-", "龙音唱片\\.", "龙音唱片", 
        "滚石合集\\.-\\.", "滚石合集\\.-", "滚石合集\\.", "滚石",
        "合集\\.-\\.", "合集\\.-", "合集\\.", "合集"
    };

    for (String prefix : prefixesToRemove) {
        if (name.matches(prefix + ".*")) {
            name = name.replaceFirst(prefix, "");
            break;
        }
    }

    // 去除年份前缀
    name = name.replaceAll("^[.\\s]*\\d{4}\\s*-\\s*", "");

    // 去除括号内的内容
    name = name.replaceAll("\\[.*?\\]", "");
    name = name.replaceAll("\\(.*?\\)", "");

    // 去除多余空格
    name = name.trim();
    name = name.replaceAll("\\s+", " ");

    return name;
}

// 在findLongestCommonPrefix方法中使用
private String findLongestCommonPrefix(List<String> names) {
    String prefix = names.get(0);
    for (int i = 1; i < names.size(); i++) {
        while (names.get(i).indexOf(prefix) != 0) {
            prefix = prefix.substring(0, prefix.length() - 1);
            if (prefix.isEmpty()) {
                break;
            }
        }
    }
    
    return cleanCollectionName(prefix);
}
```

### 示例4: 添加版本信息处理

```java
// 在FilenameNormalizer中添加版本信息模式
private static final Pattern LONGYIN_VERSION_PATTERN = 
    Pattern.compile("\\[(?:海文版|龙音海文版|龙音香港版|龙音)(?:\\s*CD-\\d+)?(?:\\s*RA-\\d+)?\\]", 
                    Pattern.CASE_INSENSITIVE);

private static final Pattern LONGYIN_CD_PATTERN = 
    Pattern.compile("\\[.*?CD-\\d+\\]", Pattern.CASE_INSENSITIVE);

private static final Pattern LONGYIN_RA_PATTERN = 
    Pattern.compile("\\[.*?RA-\\d+\\]", Pattern.CASE_INSENSITIVE);

// 在normalize方法中添加
public String normalize(String filename) {
    String result = filename;
    
    // 移除龙音港版唱片版本信息
    result = removeLongyinVersionInfo(result);
    
    // 其他标准化处理...
    return result;
}

private String removeLongyinVersionInfo(String filename) {
    String result = filename;
    
    // 移除版本信息
    result = LONGYIN_VERSION_PATTERN.matcher(result).replaceAll("");
    
    // 移除CD序号
    result = LONGYIN_CD_PATTERN.matcher(result).replaceAll("");
    
    // 移除RA序号
    result = LONGYIN_RA_PATTERN.matcher(result).replaceAll("");
    
    return result;
}
```

### 示例5: 分析测试结果

```java
// 加载测试结果
TestCasePersister persister = new TestCasePersister("test-cases");
TestValidationResult result = persister.loadValidationResult("longyin_full_test_1769452112302_result");

// 分析结果
System.out.println("测试名称: " + result.getTestName());
System.out.println("匹配率: " + (result.getMatchRate() * 100) + "%");
System.out.println("评分等级: " + result.getScoreLevel());
System.out.println("匹配的合集数: " + result.getMatchedCollections());
System.out.println("遗漏的合集数: " + result.getMissedCollections());
System.out.println("多余的合集数: " + result.getExtraCollections());

// 分析匹配详情
for (CollectionMatch match : result.getCollectionMatches()) {
    System.out.println("预期: " + match.getExpectedName() + 
                      " (" + match.getExpectedFolderCount() + "个文件夹)");
    System.out.println("实际: " + match.getActualName() + 
                      " (" + match.getActualFolderCount() + "个文件夹)");
    System.out.println("名称相似度: " + (match.getNameSimilarity() * 100) + "%");
    System.out.println("文件夹重叠率: " + (match.getFolderOverlapRate() * 100) + "%");
    System.out.println();
}

// 分析遗漏的合集
System.out.println("遗漏的合集:");
for (String name : result.getMissedCollectionNames()) {
    System.out.println("  - " + name);
}

// 分析多余的合集
System.out.println("多余的合集:");
for (String name : result.getExtraCollectionNames()) {
    System.out.println("  - " + name);
}
```

## 总结

本指南提供了合集自动化测试开发的完整流程和最佳实践，包括：

1. **设计思路**: 抽象测试用例、分层架构、持久化策略
2. **架构设计**: 整体架构、目录结构、核心组件
3. **迭代流程**: 准备数据、创建用例、运行测试、优化算法、验证效果
4. **关键技术点**: 文本相似度、聚类算法、名称标准化、评估指标
5. **注意事项**: 测试用例设计、算法优化、数据持久化、回归测试
6. **最佳实践**: 测试用例管理、算法优化流程、问题诊断、文档维护
7. **常见问题**: 添加测试用例、调整阈值、处理乱码、提高匹配率、回滚版本
8. **示例**: 创建测试用例、运行测试、优化算法、处理版本信息、分析结果

遵循本指南，可以高效地开发和维护合集自动化测试框架，持续优化算法效果，确保系统的稳定性和可靠性。

## 附录

### A. 相关文件路径

**核心算法文件**:
- `src/main/java/com/filemanager/strategy/collection/FileClusteringAlgorithm.java`
- `src/main/java/com/filemanager/strategy/collection/FilenameNormalizer.java`
- `src/main/java/com/filemanager/strategy/collection/TextSimilarityCalculator.java`

**测试框架文件**:
- `src/test/java/com/filemanager/strategy/collection/test/TestCase.java`
- `src/test/java/com/filemanager/strategy/collection/test/TestCasePersister.java`
- `src/test/java/com/filemanager/strategy/collection/test/RegressionTest.java`

**测试数据文件**:
- `test-cases/test-data/rolling_stone_test.json`
- `test-cases/test-data/polygram_test.json`
- `test-cases/test-data/longyin_test.json`
- `test-cases/test-data/longyin_full_test.json`

### B. 常用命令

```bash
# 编译项目
mvn compile

# 运行回归测试
mvn test -Dtest=RegressionTest

# 运行特定测试用例
mvn test -Dtest=RegressionTest#testSpecificCase

# 清理测试结果
rm test-cases/*_result.json

# 查看测试结果
cat test-cases/*_result.json

# 查看测试用例
cat test-cases/test-data/*.json
```

### C. 依赖配置

**pom.xml**:
```xml
<dependencies>
    <!-- FastJSON for JSON serialization -->
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>fastjson</artifactId>
        <version>1.2.83</version>
    </dependency>
    
    <!-- JUnit for testing -->
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>4.13.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### D. 版本历史

- **v1.0**: 初始版本，实现基本的测试框架
- **v1.1**: 添加中文编码支持，修复乱码问题
- **v1.2**: 优化合集名称生成，添加cleanCollectionName方法
- **v1.3**: 添加龙音唱片特定模式处理
- **v1.4**: 完善测试用例，添加完整数据集测试

---

**文档版本**: 1.0  
**最后更新**: 2026-01-27  
**维护者**: FileEditTools Team

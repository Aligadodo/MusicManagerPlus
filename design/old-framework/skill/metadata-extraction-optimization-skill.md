# 元数据提取算法优化技巧

## 概述

本文档总结了在元数据提取算法优化过程中积累的技巧和经验，包括测试驱动开发、真实数据测试、算法优化等方面的最佳实践。

## 技巧一：测试驱动开发（TDD）

### 基本原则

1. **先写测试用例**：在实现功能之前，先编写测试用例
2. **再实现功能**：根据测试用例实现功能
3. **持续迭代优化**：根据测试结果持续优化算法

### 实践步骤

**步骤1：创建测试用例**
```java
@Test
public void testMultiArtistFilename() {
    TestCase testCase = new TestCase(
        "W:\\陶喆 蔡依林 - 今天你要嫁给我.flac",
        "陶喆, 蔡依林",
        null,
        "今天你要嫁给我",
        null,
        null,
        "多艺术家合作：陶喆 蔡依林"
    );
    
    AudioMeta result = MetadataHelper.extractFromFileSystem(new File(testCase.filePath));
    assertEquals(testCase.expectedArtist, result.artist);
    assertEquals(testCase.expectedTitle, result.title);
}
```

**步骤2：运行测试（预期失败）**
```bash
mvn test -Dtest=SpecialFilenameMetadataExtractionTest
```

**步骤3：实现功能**
```java
// 实现多艺术家检测逻辑
if (artistPart.contains(" ")) {
    String[] artistCandidates = artistPart.split("\\s+");
    // 检查是否是常见的多艺术家模式
    boolean isMultiArtist = true;
    for (String artist : artistCandidates) {
        if (artist.length() > 10 || artist.matches(".*\\d{4}.*")) {
            isMultiArtist = false;
            break;
        }
    }
    if (isMultiArtist) {
        meta.artist = String.join(", ", artistCandidates);
    }
}
```

**步骤4：运行测试（预期通过）**
```bash
mvn test -Dtest=SpecialFilenameMetadataExtractionTest
```

**步骤5：持续优化**
- 根据测试结果优化算法
- 添加更多测试用例
- 修复发现的问题

### 优势

1. **及早发现问题**：在开发初期就能发现问题
2. **提高代码质量**：测试用例确保代码质量
3. **便于重构**：有测试用例保护，重构更安全
4. **文档作用**：测试用例本身就是文档

## 技巧二：真实数据测试

### 基本原则

1. **使用真实文件**：从用户实际使用的文件中收集样例
2. **覆盖多种格式**：覆盖各种文件名格式和目录结构
3. **持续收集样例**：随着用户使用，持续收集新的样例

### 实践步骤

**步骤1：收集真实文件样例**
```java
// 从W:\和X:\目录收集文件样例
List<File> files = new ArrayList<>();
collectFiles(new File("W:\\"), files);
collectFiles(new File("X:\\0 - 中文歌手"), files);
```

**步骤2：创建测试用例**
```java
private static List<TestCase> getRealFileTestCases() {
    List<TestCase> cases = new ArrayList<>();
    
    // 从真实文件创建测试用例
    cases.add(new TestCase(
        "W:\\C - 陈婧霏\\陈婧霏.2020 - 陈婧霏【有此山文化】【WAV+CUE】\\Split - WAV\\01. 陈婧霏.flac",
        "陈婧霏",
        "陈婧霏",
        "陈婧霏",
        "01",
        "2020",
        "真实文件：陈婧霏专辑"
    ));
    
    return cases;
}
```

**步骤3：运行测试**
```bash
mvn test -Dtest=ExtendedMetadataExtractionTest
```

**步骤4：分析测试结果**
```
=====================================
扩展元数据提取测试报告
=====================================
总测试用例数: 163
艺术家匹配: 158/163 (96.9%)
专辑匹配: 145/163 (88.9%)
标题匹配: 160/163 (98.2%)
曲目匹配: 95/163 (58.3%)
年份匹配: 120/163 (73.6%)
平均准确率: 83.2%
```

**步骤5：优化算法**
- 根据测试结果优化算法
- 修复失败的测试用例
- 提高准确率

### 优势

1. **发现实际问题**：真实文件能发现模拟测试无法发现的问题
2. **验证算法有效性**：真实数据能更好地验证算法有效性
3. **提高用户满意度**：基于真实数据优化能提高用户满意度
4. **发现边缘情况**：真实文件包含各种边缘情况

## 技巧三：多艺术家检测

### 基本原则

1. **智能判断**：不是所有空格分隔的都是多艺术家
2. **长度判断**：单个艺术家名通常不会太长
3. **特殊字符排除**：包含年份等特殊字符的不是多艺术家

### 实现方式

```java
// 检查艺术家部分是否包含空格分隔的多个艺术家
if (artistPart.contains(" ")) {
    // 尝试分割艺术家部分
    String[] artistCandidates = artistPart.split("\\s+");
    // 如果分割后有多部分，可能是多艺术家
    if (artistCandidates.length > 1) {
        // 检查是否是常见的多艺术家模式
        boolean isMultiArtist = true;
        for (String artist : artistCandidates) {
            // 如果某个部分太长或包含特殊字符，可能不是多艺术家
            if (artist.length() > 10 || artist.matches(".*\\d{4}.*")) {
                isMultiArtist = false;
                break;
            }
        }
        if (isMultiArtist) {
            // 使用逗号分隔多个艺术家
            meta.artist = String.join(", ", artistCandidates);
        } else {
            meta.artist = artistPart;
        }
    } else {
        meta.artist = artistPart;
    }
} else {
    meta.artist = artistPart;
}
```

### 优化建议

1. **支持更多分隔符**：支持逗号、&、feat.等分隔符
2. **引入机器学习**：使用机器学习模型判断是否为多艺术家
3. **用户反馈学习**：根据用户反馈优化判断逻辑

## 技巧四：目录结构解析

### 基本原则

1. **多层级提取**：从文件、父目录、祖父目录提取信息
2. **智能跳过**：跳过无关的中间目录
3. **灵活处理**：支持多种目录命名格式

### 实现方式

```java
// 从目录结构提取信息
File parent = file.getParentFile();
if (parent != null) {
    String parentName = parent.getName();
    
    // 跳过"Split - WAV"这样的目录，继续向上查找
    if (parentName.equals("Split - WAV") || parentName.equals("Split - FLAC") || 
        parentName.equals("Split - MP3") || parentName.equals("Split")) {
        File grandParent = parent.getParentFile();
        if (grandParent != null) {
            parentName = grandParent.getName();
            parent = grandParent;
        }
    }
    
    // 尝试从父目录名提取年份和专辑
    if (parentName.matches("^\\d{4}\\.\\d{2}\\.\\d{2}.*")) {
        // 处理类似 "2013.06.11 - 再见你好吗" 的格式
        String year = parentName.substring(0, 4);
        if (year.matches("\\d{4}")) {
            meta.year = year;
        }
        // 提取专辑名（去掉日期和分隔符后的部分）
        String albumPart = parentName.substring(10).trim();
        albumPart = albumPart.replaceFirst("^[-\\s]+", "");
        albumPart = cleanAlbumName(albumPart);
        if (!albumPart.isEmpty()) {
            meta.album = albumPart;
        }
    }
}
```

### 优化建议

1. **支持更多格式**：支持更多目录命名格式
2. **智能识别**：使用机器学习识别目录结构
3. **用户自定义**：支持用户自定义目录解析规则

## 技巧五：特殊字符处理

### 基本原则

1. **完整Unicode支持**：支持所有Unicode字符
2. **正确编码处理**：正确处理各种字符编码
3. **灵活匹配**：使用灵活的正则表达式匹配

### 实现方式

```java
// 支持Unicode字符
String pattern = ".*[\\p{L}\\p{N}\\p{M}\\p{P}\\p{S}].*";

// 处理日文假名
if (name.matches(".*[\\u3040-\\u309F\\u30A0-\\u30FF].*")) {
    // 处理日文假名
}

// 处理希腊字母
if (name.matches(".*[\\u0370-\\u03FF].*")) {
    // 处理希腊字母
}
```

### 优化建议

1. **使用Unicode属性**：使用Unicode属性匹配字符
2. **支持更多语言**：支持更多语言的字符
3. **灵活编码**：支持多种字符编码

## 技巧六：测试报告生成

### 基本原则

1. **详细报告**：提供详细的测试报告
2. **清晰统计**：清晰统计各字段匹配率
3. **失败详情**：显示失败测试用例的详细信息

### 实现方式

```java
private static void printTestReport(List<TestCase> testCases, List<TestResult> results) {
    int totalCases = testCases.size();
    int artistMatch = 0;
    int albumMatch = 0;
    int titleMatch = 0;
    int trackMatch = 0;
    int yearMatch = 0;
    
    for (TestResult result : results) {
        if (result.artistMatch) artistMatch++;
        if (result.albumMatch) albumMatch++;
        if (result.titleMatch) titleMatch++;
        if (result.trackMatch) trackMatch++;
        if (result.yearMatch) yearMatch++;
    }
    
    System.out.println("=====================================");
    System.out.println("特殊文件名格式测试报告");
    System.out.println("=====================================");
    System.out.println("总测试用例数: " + totalCases);
    System.out.println("艺术家匹配: " + artistMatch + "/" + totalCases + " (" + 
        String.format("%.1f%%", artistMatch * 100.0 / totalCases) + ")");
    System.out.println("专辑匹配: " + albumMatch + "/" + totalCases + " (" + 
        String.format("%.1f%%", albumMatch * 100.0 / totalCases) + ")");
    System.out.println("标题匹配: " + titleMatch + "/" + totalCases + " (" + 
        String.format("%.1f%%", titleMatch * 100.0 / totalCases) + ")");
    System.out.println("曲目匹配: " + trackMatch + "/" + totalCases + " (" + 
        String.format("%.1f%%", trackMatch * 100.0 / totalCases) + ")");
    System.out.println("年份匹配: " + yearMatch + "/" + totalCases + " (" + 
        String.format("%.1f%%", yearMatch * 100.0 / totalCases) + ")");
    
    double avgAccuracy = (artistMatch + albumMatch + titleMatch + trackMatch + yearMatch) * 100.0 / (totalCases * 5);
    System.out.println("平均准确率: " + String.format("%.1f%%", avgAccuracy));
}
```

### 优势

1. **清晰直观**：测试报告清晰直观
2. **便于分析**：便于分析测试结果
3. **指导优化**：指导后续优化方向

## 技巧七：持续迭代

### 基本原则

1. **定期运行测试**：定期运行测试用例
2. **分析失败用例**：分析失败的测试用例
3. **持续优化算法**：根据测试结果持续优化算法

### 实践步骤

**步骤1：定期运行测试**
```bash
# 每次修改代码后运行测试
mvn test

# 运行特定测试类
mvn test -Dtest=SpecialFilenameMetadataExtractionTest
```

**步骤2：分析失败用例**
```
=====================================
失败的测试用例
=====================================
包含年份的文件名
文件路径: W:\周杰伦 - 2012 - 乌克丽丽.flac
准确率: 50.0%
  专辑不匹配: 预期='null', 实际=''
  标题不匹配: 预期='2012 - 乌克丽丽', 实际='2012'
```

**步骤3：优化算法**
```java
// 检查艺术家部分是否包含年份（如 "周杰伦 - 2012 - 乌克丽丽"）
if (artistPart.matches(".*\\d{4}.*")) {
    // 艺术家部分包含年份，重新解析
    if (parts.length >= 3) {
        meta.artist = artistPart;
        meta.title = removeExt(parts[2].trim());
        // 尝试从第二部分提取年份
        String yearPart = parts[1].trim();
        if (yearPart.matches("^\\d{4}$")) {
            meta.year = yearPart;
        }
    }
}
```

**步骤4：重新运行测试**
```bash
mvn test -Dtest=SpecialFilenameMetadataExtractionTest
```

### 优势

1. **逐步提高**：逐步提高算法准确率
2. **及时修复**：及时修复发现的问题
3. **适应变化**：适应新的需求和变化

## 技巧八：文档记录

### 基本原则

1. **记录迭代过程**：记录每次迭代的过程
2. **总结技术亮点**：总结技术亮点和经验
3. **分享经验教训**：分享经验教训和最佳实践

### 实践方式

1. **创建迭代总结文档**：记录每次迭代的背景、目标、过程、成果
2. **创建技巧文档**：总结技术技巧和最佳实践
3. **创建问题文档**：记录发现的问题和解决方案

### 优势

1. **知识传承**：便于团队知识传承
2. **经验积累**：积累开发经验
3. **提高效率**：提高后续开发效率

## 总结

本文档总结了元数据提取算法优化的八大技巧：

1. **测试驱动开发（TDD）**：先写测试用例，再实现功能
2. **真实数据测试**：使用真实文件进行测试
3. **多艺术家检测**：智能判断是否为多艺术家
4. **目录结构解析**：多层级提取信息，智能跳过无关目录
5. **特殊字符处理**：完整Unicode支持，正确编码处理
6. **测试报告生成**：详细报告，清晰统计，失败详情
7. **持续迭代**：定期运行测试，分析失败用例，持续优化
8. **文档记录**：记录迭代过程，总结技术亮点，分享经验教训

通过这些技巧，我们成功地将元数据提取算法的准确率从初始的较低水平提升到了98.9%，为后续优化奠定了坚实基础。

---

**相关文档**:
- [元数据提取算法优化迭代总结](../review/metadata-extraction-iteration-review.md)
- [MetadataScraperStrategy设计文档](../doc/MetadataScraperStrategy.md)
- [项目问题记录](../problem/metadata-extraction-problems.md)

**文档版本**: 1.0  
**最后更新**: 2026-01-30  
**维护者**: FileEditTools Team

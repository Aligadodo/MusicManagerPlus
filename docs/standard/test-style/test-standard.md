# 测试标准

## 概述

本文档定义了FileManager Plus项目的测试标准，确保测试覆盖率、测试质量和测试效率。

## 测试分类

### 1. 单元测试

**目标**: 测试单个方法或类的功能

**要求**:
- 测试覆盖率 >= 70%
- 测试正常流程和异常情况
- 使用Mock对象隔离依赖

**示例**:
```java
@Test
public void testScanDirectory() {
    FileScanner scanner = new FileScanner();
    List<File> files = scanner.scan("/test/path", false);
    assertNotNull(files);
    assertTrue(files.size() > 0);
}
```

### 2. 集成测试

**目标**: 测试多个模块协作的功能

**要求**:
- 测试核心业务流程
- 测试API接口
- 测试数据库操作

**示例**:
```java
@Test
public void testCreateTask() {
    TaskRequest request = new TaskRequest();
    request.setStrategyId("rename");
    
    TaskResponse response = taskService.createTask(request);
    assertNotNull(response);
    assertEquals("PENDING", response.getStatus());
}
```

### 3. 回归测试

**目标**: 确保新代码不破坏已有功能

**要求**:
- 每次代码变更后执行
- 覆盖所有核心功能
- 记录测试结果

**示例**:
```java
@Test
public void testRegression() {
    TestCase testCase = loadTestCase("test_case.json");
    TestValidationResult result = evaluate(testCase);
    assertTrue(result.getMatchRate() >= 0.9);
}
```

### 4. 性能测试

**目标**: 测试系统性能指标

**要求**:
- 测试响应时间
- 测试并发能力
- 测试资源占用

**示例**:
```java
@Test
public void testPerformance() {
    long startTime = System.currentTimeMillis();
    scanner.scan("/large/path", true);
    long endTime = System.currentTimeMillis();
    assertTrue(endTime - startTime < 5000);
}
```

## 测试编写规范

### 测试命名

**规则**: 使用test前缀，描述测试内容

```java
// 正确
public void testScanDirectory() {}
public void testScanWithFilter() {}
public void testScanEmptyDirectory() {}

// 错误
public void test1() {}
public void testScan() {}
public void scanTest() {}
```

### 测试结构

**规则**: 使用Given-When-Then模式

```java
// 正确
@Test
public void testRenameFile() {
    // Given: 准备测试数据
    File source = new File("/test/source.txt");
    
    // When: 执行操作
    File target = renameFile(source, "target.txt");
    
    // Then: 验证结果
    assertTrue(target.exists());
    assertEquals("target.txt", target.getName());
}

// 错误
@Test
public void testRenameFile() {
    File source = new File("/test/source.txt");
    File target = renameFile(source, "target.txt");
    assertTrue(target.exists());
}
```

### 断言使用

**规则**: 使用具体的断言方法

```java
// 正确
assertEquals(expected, actual);
assertTrue(condition);
assertNotNull(object);
assertNull(object);

// 错误
assertTrue(expected.equals(actual));
assertTrue(condition == true);
assertTrue(object != null);
assertTrue(object == null);
```

## 测试覆盖率要求

### 代码覆盖率

**目标**: 单元测试覆盖率 >= 70%

**工具**: JaCoCo

**报告**: 每次构建生成覆盖率报告

### 分支覆盖率

**目标**: 分支覆盖率 >= 60%

**工具**: JaCoCo

**报告**: 每次构建生成覆盖率报告

### 行覆盖率

**目标**: 行覆盖率 >= 80%

**工具**: JaCoCo

**报告**: 每次构建生成覆盖率报告

## 测试数据管理

### 测试用例

**规则**: 使用JSON格式存储测试用例

```json
{
  "testName": "file_rename_test",
  "sourcePath": "/test/source",
  "expectedResult": {
    "success": true,
    "targetPath": "/test/target"
  }
}
```

### 测试数据

**规则**: 使用test目录存放测试数据

```
src/test/resources/
├── test-data/
│   ├── file1.txt
│   ├── file2.txt
│   └── config.json
└── test-cases/
    ├── case1.json
    └── case2.json
```

## 测试执行规范

### 执行时机

**规则**: 在以下时机执行测试

```
必须执行:
- 提交代码前
- 合并代码前
- 发布版本前

建议执行:
- 每次代码变更后
- 每天定时执行
```

### 执行顺序

**规则**: 按照依赖关系执行

```
执行顺序:
1. 单元测试
2. 集成测试
3. 回归测试
4. 性能测试
```

### 测试报告

**规则**: 生成详细的测试报告

```
报告内容:
- 测试用例总数
- 通过用例数
- 失败用例数
- 跳过用例数
- 代码覆盖率
- 执行时间
```

## 测试维护规范

### 测试用例更新

**规则**: 代码变更时同步更新测试用例

```
需要更新测试的情况:
- 新增功能
- 修改接口
- 变更配置
- 修复Bug
```

### 测试用例清理

**规则**: 定期清理过期的测试用例

```
清理标准:
- 测试用例已失效
- 测试用例重复
- 测试用例无意义
```

### 测试用例优化

**规则**: 优化测试用例的执行效率

```
优化方向:
- 减少测试数据量
- 使用Mock对象
- 并行执行测试
- 缓存测试结果
```

## AI提示词

当AI助手编写测试时，请遵循以下指导：

```
你正在为FileManager Plus项目编写测试。请遵循以下测试标准：

1. 测试分类：
   - 单元测试：测试单个方法或类的功能，覆盖率 >= 70%
   - 集成测试：测试多个模块协作的功能
   - 回归测试：确保新代码不破坏已有功能
   - 性能测试：测试系统性能指标

2. 测试编写规范：
   - 测试命名：使用test前缀，描述测试内容
   - 测试结构：使用Given-When-Then模式
   - 断言使用：使用具体的断言方法（assertEquals, assertTrue, assertNotNull）

3. 测试覆盖率要求：
   - 代码覆盖率 >= 70%
   - 分支覆盖率 >= 60%
   - 行覆盖率 >= 80%

4. 测试数据管理：
   - 测试用例使用JSON格式存储
   - 测试数据存放在src/test/resources/目录
   - 使用test-data/存放测试数据文件
   - 使用test-cases/存放测试用例文件

5. 测试执行规范：
   - 执行时机：提交代码前、合并代码前、发布版本前
   - 执行顺序：单元测试 -> 集成测试 -> 回归测试 -> 性能测试
   - 测试报告：包含测试用例总数、通过数、失败数、覆盖率、执行时间

6. 测试维护规范：
   - 代码变更时同步更新测试用例
   - 定期清理过期的测试用例
   - 优化测试用例的执行效率

7. 测试编写示例：

单元测试示例：
```java
@Test
public void testScanDirectory() {
    // Given: 准备测试数据
    FileScanner scanner = new FileScanner();
    
    // When: 执行操作
    List<File> files = scanner.scan("/test/path", false);
    
    // Then: 验证结果
    assertNotNull(files);
    assertTrue(files.size() > 0);
}
```

集成测试示例：
```java
@Test
public void testCreateTask() {
    // Given: 准备测试数据
    TaskRequest request = new TaskRequest();
    request.setStrategyId("rename");
    
    // When: 执行操作
    TaskResponse response = taskService.createTask(request);
    
    // Then: 验证结果
    assertNotNull(response);
    assertEquals("PENDING", response.getStatus());
}
```

请确保测试符合上述标准，并保持测试的质量和可维护性。
```

## 相关文档

- [代码规范](../code-style/)
- [设计规范](../design-style/)
- [迭代流程](../process/)

---

**文档版本**: 1.0  
**最后更新**: 2026-02-03  
**维护者**: FileManager Plus Team

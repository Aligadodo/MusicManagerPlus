# 测试开发技能

## 概述

本文档提供了FileManager Plus项目的测试开发技能指导，帮助开发者快速掌握测试编写技巧。

## 测试框架

### JUnit

**用途**: 单元测试和集成测试

**依赖**:
```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.8.2</version>
    <scope>test</scope>
</dependency>
```

**示例**:
```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FileScannerTest {
    
    @Test
    public void testScanDirectory() {
        FileScanner scanner = new FileScanner();
        List<File> files = scanner.scan("/test/path", false);
        assertNotNull(files);
        assertTrue(files.size() > 0);
    }
}
```

### Mockito

**用途**: Mock对象和依赖隔离

**依赖**:
```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>4.5.1</version>
    <scope>test</scope>
</dependency>
```

**示例**:
```java
import org.mockito.Mock;
import org.mockito.Mockito;

public class TaskServiceTest {
    
    @Mock
    private FileScanner fileScanner;
    
    @Test
    public void testCreateTask() {
        Mockito.when(fileScanner.scan("/test", false))
                .thenReturn(Arrays.asList(new File("test.txt")));
        
        TaskService service = new TaskService(fileScanner);
        Task task = service.createTask("/test", false);
        
        assertNotNull(task);
        assertEquals(1, task.getFileCount());
    }
}
```

## 测试技巧

### 1. 参数化测试

**用途**: 使用多组数据测试同一逻辑

**示例**:
```java
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class StringValidatorTest {
    
    @ParameterizedTest
    @ValueSource(strings = {"test.txt", "file.mp3", "data.json"})
    public void testValidFileNames(String fileName) {
        assertTrue(StringValidator.isValidFileName(fileName));
    }
}
```

### 2. 异常测试

**用途**: 测试异常情况

**示例**:
```java
@Test
public void testInvalidPath() {
    FileScanner scanner = new FileScanner();
    assertThrows(IOException.class, () -> {
        scanner.scan("/invalid/path", false);
    });
}
```

### 3. 超时测试

**用途**: 测试方法执行时间

**示例**:
```java
@Test
@Timeout(5)
public void testPerformance() {
    FileScanner scanner = new FileScanner();
    scanner.scan("/large/path", true);
}
```

### 4. 临时文件测试

**用途**: 使用临时文件进行测试

**示例**:
```java
import org.junit.jupiter.api.io.TempDir;

public class FileOperationTest {
    
    @Test
    public void testFileCopy(@TempDir Path tempDir) throws IOException {
        Path source = tempDir.resolve("source.txt");
        Path target = tempDir.resolve("target.txt");
        
        Files.writeString(source, "test content");
        FileOperation.copy(source, target);
        
        assertTrue(Files.exists(target));
        assertEquals("test content", Files.readString(target));
    }
}
```

## Mock技巧

### 1. 方法返回值

**用途**: 设置Mock方法的返回值

**示例**:
```java
Mockito.when(fileScanner.scan("/test", false))
        .thenReturn(Arrays.asList(new File("test.txt")));
```

### 2. 异常抛出

**用途**: 设置Mock方法抛出异常

**示例**:
```java
Mockito.when(fileScanner.scan("/invalid", false))
        .thenThrow(new IOException("Invalid path"));
```

### 3. 参数验证

**用途**: 验证方法调用的参数

**示例**:
```java
Mockito.verify(fileScanner).scan("/test", false);
Mockito.verify(fileScanner, Mockito.never()).scan("/invalid", true);
```

### 4. 顺序验证

**用途**: 验证方法调用的顺序

**示例**:
```java
InOrder inOrder = Mockito.inOrder(fileScanner, taskExecutor);
inOrder.verify(fileScanner).scan("/test", false);
inOrder.verify(taskExecutor).execute(Mockito.any());
```

## 测试数据管理

### 1. 使用JSON文件

**用途**: 存储测试用例数据

**示例**:
```java
public class TestCaseLoader {
    
    public static TestCase loadTestCase(String filename) throws IOException {
        String json = Files.readString(Path.of("src/test/resources/" + filename));
        return JSON.parseObject(json, TestCase.class);
    }
}
```

### 2. 使用资源文件

**用途**: 存储测试数据文件

**示例**:
```java
@Test
public void testParseFile() throws IOException {
    InputStream is = getClass().getResourceAsStream("/test-data/sample.txt");
    String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
    
    List<String> lines = Arrays.asList(content.split("\n"));
    assertTrue(lines.size() > 0);
}
```

## 测试覆盖率

### 1. 使用JaCoCo

**配置**:
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.8</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**生成报告**:
```bash
mvn clean test
```

**查看报告**:
```
target/site/jacoco/index.html
```

## AI提示词

当AI助手编写测试时，请遵循以下指导：

```
你正在为FileManager Plus项目编写测试。请按照以下技巧进行：

1. 使用测试框架：
   - JUnit: 用于单元测试和集成测试
   - Mockito: 用于Mock对象和依赖隔离

2. 测试技巧：
   - 参数化测试：使用@ParameterizedTest和@ValueSource
   - 异常测试：使用assertThrows测试异常情况
   - 超时测试：使用@Timeout测试方法执行时间
   - 临时文件测试：使用@TempDir创建临时文件

3. Mock技巧：
   - 方法返回值：使用Mockito.when().thenReturn()
   - 异常抛出：使用Mockito.when().thenThrow()
   - 参数验证：使用Mockito.verify()
   - 顺序验证：使用InOrder验证方法调用顺序

4. 测试数据管理：
   - 使用JSON文件存储测试用例数据
   - 使用资源文件存储测试数据文件
   - 使用TestCaseLoader加载测试用例

5. 测试覆盖率：
   - 使用JaCoCo生成覆盖率报告
   - 目标：代码覆盖率 >= 70%
   - 查看报告：target/site/jacoco/index.html

6. 测试编写示例：

参数化测试示例：
```java
@ParameterizedTest
@ValueSource(strings = {"test.txt", "file.mp3", "data.json"})
public void testValidFileNames(String fileName) {
    assertTrue(StringValidator.isValidFileName(fileName));
}
```

异常测试示例：
```java
@Test
public void testInvalidPath() {
    FileScanner scanner = new FileScanner();
    assertThrows(IOException.class, () -> {
        scanner.scan("/invalid/path", false);
    });
}
```

Mock测试示例：
```java
@Test
public void testCreateTask() {
    Mockito.when(fileScanner.scan("/test", false))
            .thenReturn(Arrays.asList(new File("test.txt")));
    
    TaskService service = new TaskService(fileScanner);
    Task task = service.createTask("/test", false);
    
    assertNotNull(task);
    assertEquals(1, task.getFileCount());
}
```

请确保测试符合上述技巧，并保持测试的质量和可维护性。
```

## 相关文档

- [测试标准](../../standard/test-style/)
- [代码规范](../../standard/code-style/)
- [策略开发技能](../development/strategy-development.md)

---

**文档版本**: 1.0  
**最后更新**: 2026-02-03  
**维护者**: FileManager Plus Team

# 性能优化技能

## 概述

本文档提供了FileManager Plus项目的性能优化技能指导，帮助开发者快速掌握性能优化技巧。

## 优化策略

### 1. 算法优化

#### 时间复杂度优化

**目标**: 降低算法的时间复杂度

**示例**:
```java
// 优化前：O(n^2)
public List<File> findDuplicates(List<File> files) {
    List<File> duplicates = new ArrayList<>();
    for (File f1 : files) {
        for (File f2 : files) {
            if (f1 != f2 && f1.getName().equals(f2.getName())) {
                duplicates.add(f1);
                break;
            }
        }
    }
    return duplicates;
}

// 优化后：O(n)
public List<File> findDuplicates(List<File> files) {
    Map<String, File> fileMap = new HashMap<>();
    List<File> duplicates = new ArrayList<>();
    
    for (File file : files) {
        String name = file.getName();
        if (fileMap.containsKey(name)) {
            duplicates.add(file);
        } else {
            fileMap.put(name, file);
        }
    }
    return duplicates;
}
```

#### 空间复杂度优化

**目标**: 降低算法的空间复杂度

**示例**:
```java
// 优化前：O(n)空间
public List<String> filterFiles(List<File> files) {
    List<String> names = new ArrayList<>();
    for (File file : files) {
        if (file.getName().endsWith(".mp3")) {
            names.add(file.getName());
        }
    }
    return names;
}

// 优化后：O(1)额外空间
public List<String> filterFiles(List<File> files) {
    return files.stream()
        .filter(f -> f.getName().endsWith(".mp3"))
        .map(File::getName)
        .collect(Collectors.toList());
}
```

### 2. I/O优化

#### 批量操作

**目标**: 减少I/O次数

**示例**:
```java
// 优化前：每次读取一个文件
public void processFiles(List<File> files) throws IOException {
    for (File file : files) {
        List<String> lines = Files.readAllLines(file.toPath());
        processLines(lines);
    }
}

// 优化后：批量读取文件
public void processFiles(List<File> files) throws IOException {
    Map<File, List<String>> fileContents = new HashMap<>();
    for (File file : files) {
        fileContents.put(file, Files.readAllLines(file.toPath()));
    }
    
    for (Map.Entry<File, List<String>> entry : fileContents.entrySet()) {
        processLines(entry.getValue());
    }
}
```

#### 缓冲区优化

**目标**: 使用合适的缓冲区大小

**示例**:
```java
// 优化前：使用默认缓冲区
try (InputStream is = new FileInputStream(file)) {
    // 读取数据
}

// 优化后：使用8KB缓冲区
try (InputStream is = new BufferedInputStream(
        new FileInputStream(file), 8192)) {
    // 读取数据
}
```

### 3. 并发优化

#### 线程池使用

**目标**: 合理使用线程池

**示例**:
```java
// 优化前：每次创建新线程
public void processFiles(List<File> files) {
    for (File file : files) {
        new Thread(() -> processFile(file)).start();
    }
}

// 优化后：使用线程池
public void processFiles(List<File> files) {
    ExecutorService executor = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors());
    
    List<Future<?>> futures = new ArrayList<>();
    for (File file : files) {
        futures.add(executor.submit(() -> processFile(file)));
    }
    
    for (Future<?> future : futures) {
        try {
            future.get();
        } catch (Exception e) {
            log.error("处理文件失败", e);
        }
    }
    
    executor.shutdown();
}
```

#### 并行流使用

**目标**: 使用并行流提高处理速度

**示例**:
```java
// 优化前：顺序处理
public List<File> filterFiles(List<File> files) {
    return files.stream()
        .filter(f -> f.getName().endsWith(".mp3"))
        .collect(Collectors.toList());
}

// 优化后：并行处理
public List<File> filterFiles(List<File> files) {
    return files.parallelStream()
        .filter(f -> f.getName().endsWith(".mp3"))
        .collect(Collectors.toList());
}
```

### 4. 内存优化

#### 对象复用

**目标**: 减少对象创建

**示例**:
```java
// 优化前：每次创建新对象
public void processFiles(List<File> files) {
    for (File file : files) {
        StringBuilder sb = new StringBuilder();
        sb.append("Processing: ");
        sb.append(file.getName());
        log(sb.toString());
    }
}

// 优化后：复用StringBuilder
public void processFiles(List<File> files) {
    StringBuilder sb = new StringBuilder();
    for (File file : files) {
        sb.setLength(0);
        sb.append("Processing: ");
        sb.append(file.getName());
        log(sb.toString());
    }
}
```

#### 缓存使用

**目标**: 使用缓存避免重复计算

**示例**:
```java
// 优化前：每次重新计算
public String getNormalizedFilename(String filename) {
    String result = filename;
    result = removeSpecialChars(result);
    result = toLowerCase(result);
    result = trimSpaces(result);
    return result;
}

// 优化后：使用缓存
private final Map<String, String> cache = new ConcurrentHashMap<>();

public String getNormalizedFilename(String filename) {
    return cache.computeIfAbsent(filename, key -> {
        String result = key;
        result = removeSpecialChars(result);
        result = toLowerCase(result);
        result = trimSpaces(result);
        return result;
    });
}
```

## 性能分析

### 1. 使用JProfiler

**用途**: 分析CPU和内存使用情况

**步骤**:
1. 启动JProfiler
2. 附加到Java进程
3. 执行操作
4. 分析结果

### 2. 使用VisualVM

**用途**: 监控Java应用性能

**步骤**:
1. 启动VisualVM
2. 连接到Java进程
3. 监控CPU、内存、线程
4. 分析性能瓶颈

### 3. 使用日志分析

**用途**: 分析操作耗时

**示例**:
```java
long startTime = System.currentTimeMillis();
processFile(file);
long endTime = System.currentTimeMillis();
log.info("处理文件耗时: {}ms", endTime - startTime);
```

## 优化检查清单

### 算法优化

- [ ] 时间复杂度是否合理
- [ ] 空间复杂度是否合理
- [ ] 是否使用了合适的数据结构
- [ ] 是否避免了嵌套循环

### I/O优化

- [ ] 是否减少了I/O次数
- [ ] 是否使用了合适的缓冲区
- [ ] 是否使用了批量操作
- [ ] 是否避免了频繁的小文件读写

### 并发优化

- [ ] 是否使用了线程池
- [ ] 是否使用了并行流
- [ ] 是否避免了线程竞争
- [ ] 是否正确处理了异常

### 内存优化

- [ ] 是否复用了对象
- [ ] 是否使用了缓存
- [ ] 是否及时释放了资源
- [ ] 是否避免了内存泄漏

## AI提示词

当AI助手进行性能优化时，请遵循以下指导：

```
你正在为FileManager Plus项目进行性能优化。请按照以下策略进行：

1. 算法优化：
   - 降低时间复杂度：O(n^2) -> O(n) 或 O(n log n)
   - 降低空间复杂度：O(n) -> O(1) 或 O(log n)
   - 使用合适的数据结构：HashMap, HashSet, TreeMap
   - 避免嵌套循环：使用Map或Set替代

2. I/O优化：
   - 减少I/O次数：批量操作、缓冲读取
   - 使用合适的缓冲区大小：8KB, 16KB, 32KB
   - 使用NIO提高性能：FileChannel, MappedByteBuffer
   - 避免频繁的小文件读写：合并操作

3. 并发优化：
   - 使用线程池：Executors.newFixedThreadPool()
   - 使用并行流：parallelStream()
   - 避免线程竞争：使用并发集合、同步机制
   - 正确处理异常：Future.get()捕获异常

4. 内存优化：
   - 复用对象：StringBuilder, 对象池
   - 使用缓存：ConcurrentHashMap, Caffeine
   - 及时释放资源：try-with-resources
   - 避免内存泄漏：清理引用、弱引用

5. 性能分析：
   - 使用JProfiler分析CPU和内存
   - 使用VisualVM监控应用性能
   - 使用日志分析操作耗时
   - 识别性能瓶颈

6. 优化检查清单：
   - 算法优化：时间复杂度、空间复杂度、数据结构、嵌套循环
   - I/O优化：I/O次数、缓冲区、批量操作、小文件读写
   - 并发优化：线程池、并行流、线程竞争、异常处理
   - 内存优化：对象复用、缓存、资源释放、内存泄漏

7. 优化示例：

算法优化示例：
```java
// 优化前：O(n^2)
for (File f1 : files) {
    for (File f2 : files) {
        if (f1.getName().equals(f2.getName())) {
            duplicates.add(f1);
        }
    }
}

// 优化后：O(n)
Map<String, File> fileMap = new HashMap<>();
for (File file : files) {
    if (fileMap.containsKey(file.getName())) {
        duplicates.add(file);
    } else {
        fileMap.put(file.getName(), file);
    }
}
```

I/O优化示例：
```java
// 优化前：默认缓冲区
try (InputStream is = new FileInputStream(file)) {
}

// 优化后：8KB缓冲区
try (InputStream is = new BufferedInputStream(
        new FileInputStream(file), 8192)) {
}
```

并发优化示例：
```java
// 优化前：创建新线程
new Thread(() -> processFile(file)).start();

// 优化后：使用线程池
executor.submit(() -> processFile(file));
```

请确保优化符合上述策略，并保持代码的正确性和可维护性。
```

## 相关文档

- [代码规范](../../standard/code-style/)
- [测试标准](../../standard/test-style/)
- [策略开发技能](../development/strategy-development.md)

---

**文档版本**: 1.0  
**最后更新**: 2026-02-03  
**维护者**: FileManager Plus Team

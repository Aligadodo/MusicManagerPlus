# Windows命令使用说明

本文档总结了在Windows环境下开发、测试、部署过程中好用的命令及其用途，以及不推荐使用的命令。

## 推荐使用的命令

### Maven命令

#### mvn clean
**用途**: 清理项目构建输出目录

**语法**:
```bash
mvn clean
```

**使用示例**:
```bash
# 清理项目
mvn clean

# 清理并重新编译
mvn clean compile
```

**适用场景**: 需要重新构建项目时

---

#### mvn compile
**用途**: 编译项目源代码

**语法**:
```bash
mvn compile
```

**使用示例**:
```bash
# 编译项目
mvn compile

# 清理并编译
mvn clean compile
```

**适用场景**: 编译Java源代码

---

#### mvn test
**用途**: 运行项目测试

**语法**:
```bash
mvn test [options]
```

**常用参数**:
| 参数 | 说明 |
|------|------|
| -Dtest | 指定测试类或测试方法 |

**使用示例**:
```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=RegressionTest

# 运行特定测试方法
mvn test -Dtest=RegressionTest#testSpecificCase
```

**适用场景**: 运行单元测试和集成测试

---

#### mvn package
**用途**: 打包项目

**语法**:
```bash
mvn package
```

**使用示例**:
```bash
# 打包项目
mvn package

# 清理并打包
mvn clean package

# 跳过测试打包
mvn package -DskipTests
```

**适用场景**: 构建JAR包

---

### Git命令

#### git status
**用途**: 查看工作区状态

**语法**:
```bash
git status
```

**使用示例**:
```bash
# 查看状态
git status

# 查看简洁状态
git status -s
```

**适用场景**: 查看文件修改状态

---

#### git add
**用途**: 添加文件到暂存区

**语法**:
```bash
git add [files]
```

**使用示例**:
```bash
# 添加所有文件
git add .

# 添加特定文件
git add file.java

# 添加所有修改的文件
git add -u
```

**适用场景**: 提交前添加文件

---

#### git commit
**用途**: 提交更改

**语法**:
```bash
git commit -m "message"
```

**使用示例**:
```bash
# 提交更改
git commit -m "feat: 添加新功能"

# 添加并提交
git commit -am "fix: 修复bug"
```

**适用场景**: 提交代码更改

---

#### git log
**用途**: 查看提交历史

**语法**:
```bash
git log [options]
```

**常用参数**:
| 参数 | 说明 |
|------|------|
| --oneline | 单行显示 |
| -n | 显示最近n条 |
| --graph | 图形化显示 |

**使用示例**:
```bash
# 查看最近10条提交
git log -n 10

# 单行显示
git log --oneline

# 图形化显示
git log --graph --oneline
```

**适用场景**: 查看提交历史

---

### 文件操作命令

#### dir
**用途**: 列出目录内容

**语法**:
```bash
dir [path] [options]
```

**常用参数**:
| 参数 | 说明 |
|------|------|
| /s | 递归显示子目录 |
| /b | 简洁格式 |
| /a | 显示所有文件（包括隐藏文件） |

**使用示例**:
```bash
# 列出当前目录
dir

# 递归列出所有文件
dir /s /b

# 列出所有文件包括隐藏文件
dir /a
```

**适用场景**: 查看目录结构和文件

---

#### copy
**用途**: 复制文件

**语法**:
```bash
copy source destination
```

**使用示例**:
```bash
# 复制文件
copy file1.txt file2.txt

# 复制到目录
copy file.txt D:\backup\
```

**适用场景**: 复制单个文件

---

#### xcopy
**用途**: 复制目录和文件

**语法**:
```bash
xcopy source destination [options]
```

**常用参数**:
| 参数 | 说明 |
|------|------|
| /e | 复制所有子目录（包括空目录） |
| /i | 如果目标不存在，假设为目标目录 |
| /y | 不提示覆盖确认 |
| /s | 复制非空目录 |

**使用示例**:
```bash
# 复制目录及其子目录
xcopy source\ destination\ /e /i

# 复制并覆盖不提示
xcopy source\ destination\ /e /i /y
```

**适用场景**: 复制目录和多个文件

---

#### del
**用途**: 删除文件

**语法**:
```bash
del [options] files
```

**常用参数**:
| 参数 | 说明 |
|------|------|
| /f | 强制删除只读文件 |
| /q | 静默模式 |
| /s | 删除指定文件的所有实例 |

**使用示例**:
```bash
# 删除文件
del file.txt

# 强制删除
del /f file.txt

# 删除所有txt文件
del *.txt
```

**适用场景**: 删除文件

---

### 系统命令

#### tasklist
**用途**: 显示运行中的进程

**语法**:
```bash
tasklist [options]
```

**常用参数**:
| 参数 | 说明 |
|------|------|
| /fi | 筛选进程 |
| /fo | 输出格式 |

**使用示例**:
```bash
# 显示所有进程
tasklist

# 查找Java进程
tasklist /fi "imagename eq java.exe"

# 查找特定端口占用（需要结合netstat）
```

**适用场景**: 查看运行中的进程

---

#### taskkill
**用途**: 终止进程

**语法**:
```bash
taskkill [options]
```

**常用参数**:
| 参数 | 说明 |
|------|------|
| /pid | 指定进程ID |
| /im | 指定进程名 |
| /f | 强制终止 |

**使用示例**:
```bash
# 根据进程ID终止
taskkill /pid 1234 /f

# 根据进程名终止
taskkill /im java.exe /f
```

**适用场景**: 终止卡死的进程

---

#### where
**用途**: 定位文件位置

**语法**:
```bash
where filename
```

**使用示例**:
```bash
# 查找Java位置
where java

# 查找Maven位置
where mvn

# 查找所有匹配文件
where *.exe
```

**适用场景**: 查找可执行文件位置

---

### PowerShell命令

#### Get-Process
**用途**: 获取进程信息

**语法**:
```powershell
Get-Process [options]
```

**使用示例**:
```powershell
# 获取所有进程
Get-Process

# 获取特定进程
Get-Process java

# 按CPU排序
Get-Process | Sort-Object CPU -Descending
```

**适用场景**: 查看和管理进程

---

#### Stop-Process
**用途**: 停止进程

**语法**:
```powershell
Stop-Process [options]
```

**使用示例**:
```powershell
# 根据ID停止进程
Stop-Process -Id 1234 -Force

# 根据名称停止进程
Stop-Process -Name java -Force
```

**适用场景**: 终止进程

---

#### Get-ChildItem
**用途**: 获取目录内容

**语法**:
```powershell
Get-ChildItem [options]
```

**常用参数**:
| 参数 | 说明 |
|------|------|
| -Recurse | 递归显示子目录 |
| -Force | 显示隐藏文件 |
| -File | 只显示文件 |

**使用示例**:
```powershell
# 列出当前目录
Get-ChildItem

# 递归列出所有文件
Get-ChildItem -Recurse -File

# 查找特定文件
Get-ChildItem -Recurse -Filter "*.java"
```

**适用场景**: 查看目录结构和文件

---

## 不推荐使用的命令

### rmdir /s
**原因**: 危险命令，容易误删重要文件

**替代方案**: 使用资源管理器手动删除，或使用更安全的工具

---

### format
**原因**: 格式化磁盘，数据无法恢复

**替代方案**: 使用磁盘管理工具或专业格式化软件

---

### del /s /q
**原因**: 递归删除文件，风险极高

**替代方案**: 先使用dir命令确认要删除的文件，再谨慎删除

---

## 注意事项

### 1. 路径处理
- Windows路径使用反斜杠 `\`
- 路径中包含空格时需要用引号括起来
- 使用相对路径时注意当前工作目录

### 2. 大小写
- Windows文件系统不区分大小写
- 但命令参数区分大小写

### 3. 权限问题
- 某些操作需要管理员权限
- 使用PowerShell时注意执行策略

### 4. 编码问题
- Windows命令行默认使用GBK编码
- 处理中文文件时注意编码问题

### 5. 命令提示符 vs PowerShell
- 命令提示符(cmd)兼容性更好
- PowerShell功能更强大
- 建议根据场景选择合适的工具

## 常用组合命令

### 清理并重新构建
```bash
mvn clean package -DskipTests
```

### 查看Java进程并终止
```bash
tasklist /fi "imagename eq java.exe"
taskkill /im java.exe /f
```

### 备份项目
```bash
xcopy project\ backup\project\ /e /i /y
```

### 查找特定文件
```powershell
Get-ChildItem -Recurse -Filter "*.java"
```

## 相关文档

- [Linux命令使用说明](linux-commands.md)
- [macOS命令使用说明](macos-commands.md)

---

**文档版本**: 1.0  
**最后更新**: 2026-01-30  
**维护者**: FileEditTools Team

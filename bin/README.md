# FileManager Plus 脚本说明

## 目录结构

```
bin/
├── macos/              # macOS 脚本
│   ├── start-backend.sh    # 启动后端服务
│   ├── start-frontend.sh   # 启动前端服务
│   ├── start-all.sh        # 一键启动所有服务
│   ├── stop-all.sh         # 停止所有服务
│   ├── restart-backend.sh  # 重启后端服务
│   ├── restart-frontend.sh # 重启前端服务
│   └── restart-all.sh      # 重启所有服务
├── windows/            # Windows 脚本
│   ├── start-backend.bat    # 启动后端服务
│   ├── start-frontend.bat   # 启动前端服务
│   ├── start-all.bat        # 一键启动所有服务
│   ├── stop-all.bat         # 停止所有服务
│   ├── restart-backend.bat  # 重启后端服务
│   ├── restart-frontend.bat # 重启前端服务
│   └── restart-all.bat      # 重启所有服务
├── old/                # 旧版本脚本
│   └── build_dist.bat       # 旧版构建脚本
├── build_dist_macos.sh  # macOS 构建脚本
├── build_dist_windows.bat # Windows 构建脚本
└── README.md            # 脚本使用说明文档
```

## 脚本说明

### macOS 脚本

#### start-backend.sh
启动后端服务，端口 8080

**功能：**
- 停止已运行的后端服务（端口 8080）
- 启动新的后端服务
- 使用相对路径定位 jdk 和 backend.jar

**使用方法：**
```bash
cd dist/bin/macos
./start-backend.sh
```

#### start-frontend.sh
启动前端服务，端口 8081

**功能：**
- 停止已运行的前端服务（端口 8081）
- 启动新的前端服务
- 使用相对路径定位 frontend 目录

**使用方法：**
```bash
cd dist/bin/macos
./start-frontend.sh
```

#### start-all.sh
一键启动所有服务

**功能：**
- 在新的终端窗口中启动后端服务
- 在新的终端窗口中启动前端服务
- 等待服务启动完成

**使用方法：**
```bash
cd dist/bin/macos
./start-all.sh
```

#### stop-all.sh
停止所有运行的服务

**功能：**
- 检查后端服务状态（端口 8080）
- 检查前端服务状态（端口 8081）
- 停止运行中的服务
- 验证服务停止状态

**使用方法：**
```bash
cd dist/bin/macos
./stop-all.sh
```

#### restart-backend.sh
重启后端服务，端口 8080

**功能：**
- 停止已运行的后端服务（端口 8080）
- 检查服务停止状态
- 启动新的后端服务
- 使用相对路径定位 jdk 和 backend.jar

**使用方法：**
```bash
cd dist/bin/macos
./restart-backend.sh
```

#### restart-frontend.sh
重启前端服务，端口 8081

**功能：**
- 停止已运行的前端服务（端口 8081）
- 检查服务停止状态
- 启动新的前端服务
- 使用相对路径定位 frontend 目录

**使用方法：**
```bash
cd dist/bin/macos
./restart-frontend.sh
```

#### restart-all.sh
重启所有服务

**功能：**
- 停止所有运行的服务
- 检查服务停止状态
- 在新的终端窗口中启动后端服务
- 在新的终端窗口中启动前端服务
- 验证服务启动状态

**使用方法：**
```bash
cd dist/bin/macos
./restart-all.sh
```

### Windows 脚本

#### start-backend.bat
启动后端服务，端口 8080

**功能：**
- 停止已运行的后端服务（端口 8080）
- 启动新的后端服务
- 使用相对路径定位 jdk 和 backend.jar

**使用方法：**
```cmd
cd dist\bin\windows
start-backend.bat
```

#### start-frontend.bat
启动前端服务，端口 8081

**功能：**
- 停止已运行的前端服务（端口 8081）
- 启动新的前端服务
- 使用相对路径定位 frontend 目录

**使用方法：**
```cmd
cd dist\bin\windows
start-frontend.bat
```

#### start-all.bat
一键启动所有服务

**功能：**
- 在新的命令行窗口中启动后端服务
- 在新的命令行窗口中启动前端服务
- 等待服务启动完成

**使用方法：**
```cmd
cd dist\bin\windows
start-all.bat
```

#### stop-all.bat
停止所有运行的服务

**功能：**
- 检查后端服务状态（端口 8080）
- 检查前端服务状态（端口 8081）
- 停止运行中的服务
- 验证服务停止状态

**使用方法：**
```cmd
cd dist\bin\windows
stop-all.bat
```

#### restart-backend.bat
重启后端服务，端口 8080

**功能：**
- 停止已运行的后端服务（端口 8080）
- 检查服务停止状态
- 启动新的后端服务
- 使用相对路径定位 jdk 和 backend.jar

**使用方法：**
```cmd
cd dist\bin\windows
restart-backend.bat
```

#### restart-frontend.bat
重启前端服务，端口 8081

**功能：**
- 停止已运行的前端服务（端口 8081）
- 检查服务停止状态
- 启动新的前端服务
- 使用相对路径定位 frontend 目录

**使用方法：**
```cmd
cd dist\bin\windows
restart-frontend.bat
```

#### restart-all.bat
重启所有服务

**功能：**
- 停止所有运行的服务
- 检查服务停止状态
- 在新的命令行窗口中启动后端服务
- 在新的命令行窗口中启动前端服务
- 验证服务启动状态

**使用方法：**
```cmd
cd dist\bin\windows
restart-all.bat
```

## 构建脚本

### macOS 构建脚本

**文件名：** `build_dist_macos.sh`

**功能：**
- 检查构建环境（JDK、Flutter、Maven、Python）
- 清理旧文件
- 构建后端服务（Maven）
- 构建前端应用（Flutter）
- 复制后端文件到 dist 目录
- 复制前端文件到 dist 目录
- 复制 JDK 运行时到 dist 目录
- 复制启动和管理脚本到 dist 目录

**使用方法：**
```bash
./build_dist_macos.sh
```

**配置：**
在脚本开头修改以下路径：
- `SOURCE_JDK`: JDK 21 路径
- `FLUTTER_SDK`: Flutter SDK 路径

### Windows 构建脚本

**文件名：** `build_dist_windows.bat`

**功能：**
- 检查构建环境（JDK、Flutter、Maven、Python）
- 清理旧文件
- 构建后端服务（Maven）
- 构建前端应用（Flutter）
- 复制后端文件到 dist 目录
- 复制前端文件到 dist 目录
- 复制 JDK 运行时到 dist 目录
- 复制启动和管理脚本到 dist 目录

**使用方法：**
```cmd
build_dist_windows.bat
```

**配置：**
在脚本开头修改以下路径：
- `SOURCE_JDK`: JDK 21 路径
- `FLUTTER_SDK`: Flutter SDK 路径

## dist 目录结构

构建完成后，dist 目录结构如下：

```
dist/
├── backend/
│   └── backend.jar          # 后端服务 JAR 包
├── frontend/                  # 前端应用文件
│   ├── index.html
│   ├── main.dart.js
│   └── ...
├── jdk/                       # JDK 运行时
│   ├── bin/
│   │   └── java
│   └── lib/
└── bin/                       # 启动和管理脚本
    ├── macos/              # macOS 脚本
    │   ├── start-backend.sh
    │   ├── start-frontend.sh
    │   ├── start-all.sh
    │   ├── stop-all.sh
    │   ├── restart-backend.sh
    │   ├── restart-frontend.sh
    │   └── restart-all.sh
    └── windows/            # Windows 脚本
        ├── start-backend.bat
        ├── start-frontend.bat
        ├── start-all.bat
        ├── stop-all.bat
        ├── restart-backend.bat
        ├── restart-frontend.bat
        └── restart-all.bat
```

## 使用流程

### 1. 构建项目

**macOS:**
```bash
./build_dist_macos.sh
```

**Windows:**
```cmd
build_dist_windows.bat
```

### 2. 运行服务

**macOS:**
```bash
cd dist/bin/macos
./start-all.sh
```

**Windows:**
```cmd
cd dist\bin\windows
start-all.bat
```

### 3. 访问应用

- **后端服务地址：** http://localhost:8080
- **前端访问地址：** http://localhost:8081

### 4. 停止服务

**macOS:**
```bash
cd dist/bin/macos
./stop-all.sh
```

**Windows:**
```cmd
cd dist\bin\windows
stop-all.bat
```

### 5. 重启服务

**macOS:**
```bash
cd dist/bin/macos
./restart-all.sh
```

**Windows:**
```cmd
cd dist\bin\windows
restart-all.bat
```

**单独重启后端服务：**

**macOS:**
```bash
cd dist/bin/macos
./restart-backend.sh
```

**Windows:**
```cmd
cd dist\bin\windows
restart-backend.bat
```

**单独重启前端服务：**

**macOS:**
```bash
cd dist/bin/macos
./restart-frontend.sh
```

**Windows:**
```cmd
cd dist\bin\windows
restart-frontend.bat
```

## 注意事项

1. **相对路径**：所有脚本都使用相对路径，确保在不同位置运行时都能正常工作
2. **端口占用**：脚本会自动检测并停止占用端口的进程
3. **权限设置**：macOS 脚本已设置执行权限，Windows 脚本可直接运行
4. **环境检查**：构建脚本会检查所有必需的环境是否正确配置
5. **独立运行**：每个脚本都可以独立运行，不依赖其他脚本

## 故障排除

### 后端无法启动
- 检查 JDK 路径是否正确
- 检查端口 8080 是否被占用
- 查看后端日志输出

### 前端无法启动
- 检查 Python 是否正确安装
- 检查端口 8081 是否被占用
- 确认前端文件是否正确复制

### 服务无法停止
- 手动检查进程是否仍在运行
- 使用系统任务管理器强制结束进程
- 检查是否有其他程序占用端口

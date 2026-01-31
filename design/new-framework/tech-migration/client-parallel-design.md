# 客户端模块（JavaFX、Flutter）并行方案

## 1. 客户端架构概述

### 1.1 设计目标
- **并行使用**：JavaFX 客户端和 Flutter Web 客户端可同时使用
- **共享后端**：两个客户端共享同一个后端 API
- **平滑过渡**：支持从 JavaFX 客户端逐步迁移到 Flutter Web 客户端
- **功能一致**：两个客户端提供相同的核心功能
- **独立演进**：两个客户端可独立开发和部署

### 1.2 技术选型

| 客户端 | 技术栈 | 版本 | 主要特点 |
|-------|--------|------|----------|
| JavaFX | Java 21+, JavaFX 21+ | Java 21+ | 原生桌面应用，现有代码基础 |
| Flutter Web | Flutter 3.16+, Dart 3.2+ | Flutter 3.16+ | 跨平台 Web 应用，现代化 UI |

## 2. 模块结构

### 2.1 整体结构

```
clients/
├── javafx-cli/         # JavaFX 客户端
│   ├── src/main/java/com/filemanager/client/javafx/
│   │   ├── MainApp.java
│   │   ├── controller/
│   │   ├── view/
│   │   ├── model/
│   │   ├── service/
│   │   ├── util/
│   │   └── api/
│   ├── src/main/resources/
│   └── pom.xml
├── flutter-web/        # Flutter Web 客户端
│   ├── lib/
│   │   ├── main.dart
│   │   ├── app/
│   │   │   ├── app.dart
│   │   │   ├── routes.dart
│   │   │   └── theme.dart
│   │   ├── pages/
│   │   ├── components/
│   │   ├── services/
│   │   ├── models/
│   │   ├── utils/
│   │   └── api/
│   ├── web/
│   ├── pubspec.yaml
│   └── README.md
└── shared/             # 共享资源
    ├── config/
    ├── assets/
    ├── documentation/
    └── api-definitions/
```

### 2.2 JavaFX 客户端结构

```
javafx-cli/src/main/java/com/filemanager/client/javafx/
├── MainApp.java            # 应用入口
├── controller/             # 控制器
│   ├── MainController.java
│   ├── FileController.java
│   ├── StrategyController.java
│   ├── TaskController.java
│   └── ConfigController.java
├── view/                   # 视图
│   ├── MainView.java
│   ├── FileView.java
│   ├── StrategyView.java
│   ├── TaskView.java
│   └── ConfigView.java
├── model/                  # 数据模型
│   ├── FileInfo.java
│   ├── StrategyInfo.java
│   ├── TaskStatus.java
│   └── Config.java
├── service/                # 服务
│   ├── FileService.java
│   ├── StrategyService.java
│   ├── TaskService.java
│   └── ConfigService.java
├── util/                   # 工具类
│   ├── FileUtil.java
│   ├── UIUtil.java
│   └── ValidationUtil.java
└── api/                    # API 客户端
    ├── ApiClient.java
    ├── FileApi.java
    ├── StrategyApi.java
    ├── TaskApi.java
    └── WebSocketClient.java
```

### 2.3 Flutter Web 客户端结构

```
flutter-web/lib/
├── main.dart               # 应用入口
├── app/
│   ├── app.dart            # 根组件
│   ├── routes.dart         # 路由配置
│   └── theme.dart          # 主题配置
├── pages/                  # 页面
│   ├── home/
│   │   ├── home_page.dart
│   │   └── home_controller.dart
│   ├── file/
│   │   ├── file_page.dart
│   │   ├── file_controller.dart
│   │   └── file_view.dart
│   ├── strategy/
│   │   ├── strategy_page.dart
│   │   ├── strategy_controller.dart
│   │   └── strategy_view.dart
│   ├── task/
│   │   ├── task_page.dart
│   │   ├── task_controller.dart
│   │   └── task_view.dart
│   └── config/
│       ├── config_page.dart
│       ├── config_controller.dart
│       └── config_view.dart
├── components/             # 组件
│   ├── file_tree/
│   ├── strategy_card/
│   ├── task_list/
│   └── common/
├── services/               # 服务
│   ├── file_service.dart
│   ├── strategy_service.dart
│   ├── task_service.dart
│   └── config_service.dart
├── models/                 # 数据模型
│   ├── file_info.dart
│   ├── strategy_info.dart
│   ├── task_status.dart
│   └── config.dart
├── utils/                  # 工具类
│   ├── file_util.dart
│   ├── ui_util.dart
│   └── validation_util.dart
└── api/                    # API 客户端
    ├── api_client.dart
    ├── file_api.dart
    ├── strategy_api.dart
    ├── task_api.dart
    └── websocket_client.dart
```

## 3. API 客户端设计

### 3.1 JavaFX API 客户端

#### 3.1.1 ApiClient

```java
package com.filemanager.client.javafx.api;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    
    private static final String BASE_URL = "http://localhost:8080/api";
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    
    public ApiClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    public ApiClient(String baseUrl) {
        this();
        BASE_URL = baseUrl;
    }
    
    public <T> T get(String endpoint, Class<T> responseType) throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + endpoint)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return objectMapper.readValue(response.body().string(), responseType);
        }
    }
    
    public <T> T post(String endpoint, Object requestBody, Class<T> responseType) throws IOException {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        
        Request request = new Request.Builder()
                .url(BASE_URL + endpoint)
                .post(RequestBody.create(jsonBody, JSON))
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return objectMapper.readValue(response.body().string(), responseType);
        }
    }
    
    // 其他 HTTP 方法...
}
```

#### 3.1.2 WebSocketClient

```java
package com.filemanager.client.javafx.api;

import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okio.ByteString;

import java.util.concurrent.CountDownLatch;

public class WebSocketClient {
    
    private WebSocket webSocket;
    private final OkHttpClient client;
    private final String url;
    private final WebSocketListener listener;
    
    public WebSocketClient(String url, WebSocketListener listener) {
        this.client = new OkHttpClient();
        this.url = url;
        this.listener = listener;
    }
    
    public void connect() {
        Request request = new Request.Builder()
                .url(url)
                .build();
        
        webSocket = client.newWebSocket(request, listener);
    }
    
    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Disconnected by client");
        }
    }
    
    public void send(String message) {
        if (webSocket != null) {
            webSocket.send(message);
        }
    }
    
    public boolean isConnected() {
        return webSocket != null;
    }
}
```

### 3.2 Flutter Web API 客户端

#### 3.2.1 ApiClient

```dart
import 'dart:convert';
import 'package:http/http.dart' as http;

class ApiClient {
  static const String _baseUrl = 'http://localhost:8080/api';
  final http.Client _client;

  ApiClient() : _client = http.Client();

  ApiClient.withBaseUrl(String baseUrl) : _client = http.Client() {
    _baseUrl = baseUrl;
  }

  Future<T> get<T>(String endpoint, T Function(dynamic) fromJson) async {
    final response = await _client.get(Uri.parse(_baseUrl + endpoint));
    
    if (response.statusCode != 200) {
      throw Exception('Failed to load data: ${response.statusCode}');
    }
    
    return fromJson(jsonDecode(response.body));
  }

  Future<T> post<T>(String endpoint, dynamic data, T Function(dynamic) fromJson) async {
    final response = await _client.post(
      Uri.parse(_baseUrl + endpoint),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode(data),
    );
    
    if (response.statusCode != 200) {
      throw Exception('Failed to post data: ${response.statusCode}');
    }
    
    return fromJson(jsonDecode(response.body));
  }

  // 其他 HTTP 方法...
}
```

#### 3.2.2 WebSocketClient

```dart
import 'dart:async';
import 'dart:convert';
import 'package:web_socket_channel/web_socket_channel.dart';
import 'package:web_socket_channel/io.dart';

class WebSocketClient {
  WebSocketChannel? _channel;
  StreamSubscription? _subscription;
  final String _url;
  final Function(dynamic)? _onMessage;
  final Function()? _onConnected;
  final Function(dynamic)? _onError;
  final Function()? _onDisconnected;

  WebSocketClient(
    this._url,
    {
      Function(dynamic)? onMessage,
      Function()? onConnected,
      Function(dynamic)? onError,
      Function()? onDisconnected,
    }
  ) : 
    _onMessage = onMessage,
    _onConnected = onConnected,
    _onError = onError,
    _onDisconnected = onDisconnected;

  void connect() {
    try {
      _channel = IOWebSocketChannel.connect(_url);
      
      _subscription = _channel!.stream.listen(
        (message) {
          if (_onMessage != null) {
            _onMessage!(message);
          }
        },
        onDone: () {
          if (_onDisconnected != null) {
            _onDisconnected!();
          }
        },
        onError: (error) {
          if (_onError != null) {
            _onError!(error);
          }
        },
      );
      
      if (_onConnected != null) {
        _onConnected!();
      }
    } catch (e) {
      if (_onError != null) {
        _onError!(e);
      }
    }
  }

  void disconnect() {
    _subscription?.cancel();
    _channel?.sink.close();
  }

  void send(dynamic message) {
    if (_channel != null && _channel!.sink != null) {
      _channel!.sink.add(message);
    }
  }

  bool get isConnected => _channel != null;
}
```

## 4. 客户端服务实现

### 4.1 JavaFX 客户端服务

#### 4.1.1 FileService

```java
package com.filemanager.client.javafx.service;

import com.filemanager.client.javafx.api.ApiClient;
import com.filemanager.client.javafx.model.FileInfo;

import java.util.List;
import java.util.Map;

public class FileService {
    
    private final ApiClient apiClient;
    
    public FileService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }
    
    public List<FileInfo> scanDirectory(String path, int minDepth, int maxDepth, String pattern) {
        try {
            String endpoint = "/files/scan?path=" + path + "&minDepth=" + minDepth + "&maxDepth=" + maxDepth;
            if (pattern != null) {
                endpoint += "&pattern=" + pattern;
            }
            return apiClient.get(endpoint, new TypeReference<List<FileInfo>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }
    
    public FileInfo getFileInfo(String path) {
        try {
            String endpoint = "/files/info?path=" + path;
            return apiClient.get(endpoint, FileInfo.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public Map<String, Boolean> checkExists(List<String> paths) {
        try {
            String endpoint = "/files/exists";
            Map<String, List<String>> request = Map.of("paths", paths);
            return apiClient.post(endpoint, request, new TypeReference<Map<String, Boolean>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of();
        }
    }
    
    // 其他文件操作方法...
}
```

#### 4.1.2 StrategyService

```java
package com.filemanager.client.javafx.service;

import com.filemanager.client.javafx.api.ApiClient;
import com.filemanager.client.javafx.model.StrategyInfo;
import com.filemanager.client.javafx.model.StrategyConfig;
import com.filemanager.client.javafx.model.ChangeRecord;

import java.util.List;

public class StrategyService {
    
    private final ApiClient apiClient;
    
    public StrategyService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }
    
    public List<StrategyInfo> getAvailableStrategies() {
        try {
            return apiClient.get("/strategies", new TypeReference<List<StrategyInfo>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }
    
    public StrategyConfig getStrategyConfig(String strategyId) {
        try {
            return apiClient.get("/strategies/" + strategyId + "/config", StrategyConfig.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public List<ChangeRecord> analyzeFiles(String strategyId, List<String> filePaths, StrategyConfig config) {
        try {
            Map<String, Object> request = Map.of(
                "files", filePaths,
                "config", config
            );
            return apiClient.post("/strategies/" + strategyId + "/analyze", request, new TypeReference<List<ChangeRecord>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }
    
    // 其他策略操作方法...
}
```

### 4.2 Flutter Web 客户端服务

#### 4.2.1 FileService

```dart
import 'package:filemanager/models/file_info.dart';
import 'package:filemanager/api/api_client.dart';

class FileService {
  final ApiClient _apiClient;

  FileService(this._apiClient);

  Future<List<FileInfo>> scanDirectory(String path, int minDepth, int maxDepth, String? pattern) async {
    try {
      var endpoint = '/files/scan?path=$path&minDepth=$minDepth&maxDepth=$maxDepth';
      if (pattern != null) {
        endpoint += '&pattern=$pattern';
      }
      return await _apiClient.get(
        endpoint,
        (json) => (json as List).map((item) => FileInfo.fromJson(item)).toList(),
      );
    } catch (e) {
      print('Error scanning directory: $e');
      return [];
    }
  }

  Future<FileInfo?> getFileInfo(String path) async {
    try {
      return await _apiClient.get(
        '/files/info?path=$path',
        (json) => FileInfo.fromJson(json),
      );
    } catch (e) {
      print('Error getting file info: $e');
      return null;
    }
  }

  Future<Map<String, bool>> checkExists(List<String> paths) async {
    try {
      return await _apiClient.post(
        '/files/exists',
        {'paths': paths},
        (json) => (json as Map).map(
          (key, value) => MapEntry(key as String, value as bool),
        ),
      );
    } catch (e) {
      print('Error checking file existence: $e');
      return {};
    }
  }

  // 其他文件操作方法...
}
```

#### 4.2.2 StrategyService

```dart
import 'package:filemanager/models/strategy_info.dart';
import 'package:filemanager/models/strategy_config.dart';
import 'package:filemanager/models/change_record.dart';
import 'package:filemanager/api/api_client.dart';

class StrategyService {
  final ApiClient _apiClient;

  StrategyService(this._apiClient);

  Future<List<StrategyInfo>> getAvailableStrategies() async {
    try {
      return await _apiClient.get(
        '/strategies',
        (json) => (json as List).map((item) => StrategyInfo.fromJson(item)).toList(),
      );
    } catch (e) {
      print('Error getting strategies: $e');
      return [];
    }
  }

  Future<StrategyConfig?> getStrategyConfig(String strategyId) async {
    try {
      return await _apiClient.get(
        '/strategies/$strategyId/config',
        (json) => StrategyConfig.fromJson(json),
      );
    } catch (e) {
      print('Error getting strategy config: $e');
      return null;
    }
  }

  Future<List<ChangeRecord>> analyzeFiles(String strategyId, List<String> filePaths, StrategyConfig config) async {
    try {
      return await _apiClient.post(
        '/strategies/$strategyId/analyze',
        {
          'files': filePaths,
          'config': config.toJson(),
        },
        (json) => (json as List).map((item) => ChangeRecord.fromJson(item)).toList(),
      );
    } catch (e) {
      print('Error analyzing files: $e');
      return [];
    }
  }

  // 其他策略操作方法...
}
```

## 5. 客户端 UI 实现

### 5.1 JavaFX 客户端 UI

#### 5.1.1 MainView

```java
package com.filemanager.client.javafx.view;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MainView {
    
    private final BorderPane root;
    private final MenuBar menuBar;
    private final TabPane tabPane;
    
    public MainView() {
        root = new BorderPane();
        menuBar = createMenuBar();
        tabPane = createTabPane();
        
        root.setTop(menuBar);
        root.setCenter(tabPane);
    }
    
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        Menu fileMenu = new Menu("文件");
        MenuItem openItem = new MenuItem("打开");
        MenuItem exitItem = new MenuItem("退出");
        fileMenu.getItems().addAll(openItem, new SeparatorMenuItem(), exitItem);
        
        Menu strategyMenu = new Menu("策略");
        MenuItem manageItem = new MenuItem("管理策略");
        MenuItem executeItem = new MenuItem("执行策略");
        strategyMenu.getItems().addAll(manageItem, executeItem);
        
        Menu taskMenu = new Menu("任务");
        MenuItem listItem = new MenuItem("任务列表");
        MenuItem historyItem = new MenuItem("任务历史");
        taskMenu.getItems().addAll(listItem, historyItem);
        
        Menu configMenu = new Menu("配置");
        MenuItem settingsItem = new MenuItem("设置");
        configMenu.getItems().add(settingsItem);
        
        menuBar.getMenus().addAll(fileMenu, strategyMenu, taskMenu, configMenu);
        return menuBar;
    }
    
    private TabPane createTabPane() {
        TabPane tabPane = new TabPane();
        
        Tab fileTab = new Tab("文件");
        fileTab.setContent(new FileView());
        
        Tab strategyTab = new Tab("策略");
        strategyTab.setContent(new StrategyView());
        
        Tab taskTab = new Tab("任务");
        taskTab.setContent(new TaskView());
        
        Tab configTab = new Tab("配置");
        configTab.setContent(new ConfigView());
        
        tabPane.getTabs().addAll(fileTab, strategyTab, taskTab, configTab);
        return tabPane;
    }
    
    public Scene createScene() {
        return new Scene(root, 1200, 800);
    }
    
    // Getters and setters...
}
```

#### 5.1.2 FileView

```java
package com.filemanager.client.javafx.view;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;

public class FileView extends VBox {
    
    private final TextField pathField;
    private final Button scanButton;
    private final TreeView<String> fileTree;
    private final TableView<FileInfo> fileTable;
    
    public FileView() {
        setPadding(new Insets(10));
        setSpacing(10);
        
        pathField = new TextField();
        pathField.setPromptText("输入目录路径");
        
        scanButton = new Button("扫描");
        
        HBox pathBox = new HBox(10, pathField, scanButton);
        
        fileTree = new TreeView<>();
        fileTree.setPrefHeight(300);
        
        fileTable = new TableView<>();
        fileTable.setPrefHeight(300);
        
        // 初始化表格列
        TableColumn<FileInfo, String> nameColumn = new TableColumn<>("名称");
        TableColumn<FileInfo, String> pathColumn = new TableColumn<>("路径");
        TableColumn<FileInfo, Long> sizeColumn = new TableColumn<>("大小");
        TableColumn<FileInfo, String> typeColumn = new TableColumn<>("类型");
        
        fileTable.getColumns().addAll(nameColumn, pathColumn, sizeColumn, typeColumn);
        
        getChildren().addAll(pathBox, fileTree, fileTable);
    }
    
    // Getters and setters...
}
```

### 5.2 Flutter Web 客户端 UI

#### 5.2.1 HomePage

```dart
import 'package:flutter/material.dart';
import 'package:filemanager/pages/file/file_page.dart';
import 'package:filemanager/pages/strategy/strategy_page.dart';
import 'package:filemanager/pages/task/task_page.dart';
import 'package:filemanager/pages/config/config_page.dart';

class HomePage extends StatefulWidget {
  @override
  _HomePageState createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  int _selectedIndex = 0;

  static const List<Widget> _pages = <Widget>[
    FilePage(),
    StrategyPage(),
    TaskPage(),
    ConfigPage(),
  ];

  void _onItemTapped(int index) {
    setState(() {
      _selectedIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('FileManager Plus'),
      ),
      body: Center(
        child: _pages.elementAt(_selectedIndex),
      ),
      bottomNavigationBar: BottomNavigationBar(
        items: const <BottomNavigationBarItem>[
          BottomNavigationBarItem(
            icon: Icon(Icons.file_copy),
            label: '文件',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.auto_awesome),
            label: '策略',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.task),
            label: '任务',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.settings),
            label: '配置',
          ),
        ],
        currentIndex: _selectedIndex,
        selectedItemColor: Colors.blue,
        onTap: _onItemTapped,
      ),
    );
  }
}
```

#### 5.2.2 FilePage

```dart
import 'package:flutter/material.dart';
import 'package:filemanager/controllers/file_controller.dart';
import 'package:filemanager/models/file_info.dart';

class FilePage extends StatefulWidget {
  @override
  _FilePageState createState() => _FilePageState();
}

class _FilePageState extends State<FilePage> {
  final FileController _controller = FileController();
  final TextEditingController _pathController = TextEditingController();
  List<FileInfo> _files = [];
  bool _isLoading = false;

  Future<void> _scanDirectory() async {
    setState(() {
      _isLoading = true;
    });

    try {
      final files = await _controller.scanDirectory(
        _pathController.text,
        0,
        3,
        null,
      );
      setState(() {
        _files = files;
      });
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('扫描失败: $e')),
      );
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        children: [
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _pathController,
                  decoration: InputDecoration(
                    labelText: '目录路径',
                    hintText: '输入要扫描的目录',
                  ),
                ),
              ),
              SizedBox(width: 16),
              ElevatedButton(
                onPressed: _scanDirectory,
                child: _isLoading ? CircularProgressIndicator() : Text('扫描'),
              ),
            ],
          ),
          SizedBox(height: 16),
          Expanded(
            child: ListView.builder(
              itemCount: _files.length,
              itemBuilder: (context, index) {
                final file = _files[index];
                return ListTile(
                  leading: Icon(
                    file.isDirectory ? Icons.folder : Icons.file_copy,
                  ),
                  title: Text(file.name),
                  subtitle: Text(file.path),
                  trailing: file.isDirectory ? null : Text('${file.size} bytes'),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
```

## 6. 并行使用策略

### 6.1 配置管理

#### 6.1.1 共享配置
- **配置存储**：后端统一存储配置信息
- **配置同步**：两个客户端从后端获取最新配置
- **配置版本**：支持配置版本控制和回滚

#### 6.1.2 客户端特定配置
- **JavaFX 配置**：桌面特定配置（窗口大小、位置等）
- **Flutter Web 配置**：Web 特定配置（主题、布局等）
- **本地存储**：客户端特定配置存储在本地

### 6.2 任务管理

#### 6.2.1 任务共享
- **任务队列**：后端统一管理任务队列
- **任务状态**：两个客户端可查看所有任务状态
- **任务操作**：两个客户端可操作任务（暂停、取消等）

#### 6.2.2 任务通知
- **WebSocket 推送**：任务状态变更实时推送到所有客户端
- **通知中心**：客户端统一的通知管理
- **历史记录**：任务历史记录持久化

### 6.3 用户体验

#### 6.3.1 一致的界面
- **核心功能**：两个客户端提供相同的核心功能
- **操作流程**：保持相似的操作流程
- **视觉风格**：统一的品牌和视觉元素

#### 6.3.2 平台特定优化
- **JavaFX**：利用桌面特性（文件拖拽、系统托盘等）
- **Flutter Web**：优化 Web 体验（响应式布局、浏览器集成等）
- **性能优化**：针对不同平台进行性能优化

## 7. 迁移策略

### 7.1 阶段一：后端 API 开发
1. 开发后端 API 接口
2. 实现核心服务和插件系统
3. 测试 API 功能

### 7.2 阶段二：JavaFX 客户端改造
1. 添加 API 客户端模块
2. 改造现有服务层使用 API
3. 保持现有 UI 不变
4. 测试与后端集成

### 7.3 阶段三：Flutter Web 客户端开发
1. 创建 Flutter Web 项目结构
2. 实现核心页面和组件
3. 集成 API 客户端
4. 测试功能完整性

### 7.4 阶段四：并行测试
1. 部署两个客户端
2. 测试并行使用场景
3. 收集用户反馈
4. 优化性能和体验

### 7.5 阶段五：逐步迁移
1. 向用户推荐 Flutter Web 客户端
2. 提供迁移指南和工具
3. 逐步减少 JavaFX 客户端的开发
4. 最终保留 Flutter Web 客户端

## 8. 技术挑战与解决方案

### 8.1 技术挑战

| 挑战 | 描述 | 解决方案 |
|-----|------|----------|
| API 兼容性 | 确保两个客户端与同一 API 兼容 | 严格的 API 版本控制和文档 |
| 性能差异 | 不同客户端的性能特性不同 | 针对各平台优化代码 |
| 功能同步 | 确保两个客户端功能同步更新 | 统一的功能规划和测试 |
| 数据一致性 | 确保两个客户端的数据视图一致 | 实时数据同步和缓存策略 |
| 开发维护 | 维护两个客户端的开发和维护 | 共享代码和工具，自动化测试 |

### 8.2 解决方案

#### 8.2.1 API 兼容性解决方案
- **版本控制**：API 接口版本化
- **向后兼容**：保持 API 向后兼容
- **客户端检测**：服务端检测客户端版本
- **降级策略**：不支持的功能优雅降级

#### 8.2.2 性能优化解决方案
- **缓存策略**：合理使用缓存
- **异步操作**：避免阻塞 UI 线程
- **懒加载**：按需加载数据和组件
- **资源优化**：优化图片和资源

#### 8.2.3 功能同步解决方案
- **功能标记**：使用功能标记控制特性
- **统一测试**：共享测试用例
- **自动化部署**：CI/CD 管道
- **版本同步**：客户端和服务端版本同步

#### 8.2.4 数据一致性解决方案
- **实时同步**：WebSocket 实时更新
- **乐观更新**：本地乐观更新，后端验证
- **冲突解决**：定义冲突解决策略
- **数据验证**：服务端数据验证

## 9. 部署与分发

### 9.1 JavaFX 客户端部署

#### 9.1.1 打包方式
- **可执行 JAR**：包含所有依赖的可执行 JAR
- **原生打包**：使用 jlink 或 jpackage 生成原生安装包
- **跨平台**：支持 Windows、macOS、Linux

#### 9.1.2 分发渠道
- **官网下载**：项目官网提供下载
- **GitHub Releases**：通过 GitHub 发布
- **包管理器**：可选，如 Chocolatey、Homebrew

### 9.2 Flutter Web 客户端部署

#### 9.2.1 构建方式
- **Web 构建**：`flutter build web`
- **优化构建**：启用代码分割和树摇
- **PWA 支持**：配置为渐进式 Web 应用

#### 9.2.2 部署渠道
- **静态网站**：部署到静态网站托管服务
- **容器化**：使用 Docker 容器部署
- **CDN 加速**：使用 CDN 加速静态资源
- **域名配置**：配置自定义域名

### 9.3 后端 API 部署

#### 9.3.1 部署方式
- **独立服务**：部署为独立的 Spring Boot 服务
- **容器化**：使用 Docker 容器部署
- **云服务**：部署到云平台

#### 9.3.2 扩展性
- **水平扩展**：支持多实例部署
- **负载均衡**：使用负载均衡器
- **自动缩放**：根据负载自动缩放

## 10. 监控与维护

### 10.1 客户端监控

#### 10.1.1 JavaFX 客户端监控
- **日志系统**：本地日志记录
- **错误报告**：自动错误报告
- **性能监控**：资源使用监控

#### 10.1.2 Flutter Web 客户端监控
- **Web Analytics**：使用 Google Analytics 等
- **错误跟踪**：使用 Sentry 等错误跟踪服务
- **性能监控**：使用 Lighthouse 等工具

### 10.2 后端监控

#### 10.2.1 服务监控
- **健康检查**：Spring Boot Actuator 健康检查
- **指标监控**：使用 Prometheus 监控
- **日志聚合**：使用 ELK Stack 聚合日志

#### 10.2.2 API 监控
- **请求监控**：监控 API 请求量和响应时间
- **错误率监控**：监控 API 错误率
- **性能分析**：分析 API 性能瓶颈

### 10.3 维护策略

#### 10.3.1 版本管理
- **语义化版本**：使用语义化版本控制
- **发布计划**：定期发布计划
- **版本兼容性**：确保版本间兼容性

#### 10.3.2 故障处理
- **故障检测**：自动故障检测
- **故障恢复**：自动故障恢复机制
- **应急响应**：建立应急响应流程

#### 10.3.3 文档维护
- **API 文档**：使用 OpenAPI 规范
- **用户文档**：维护用户指南
- **开发者文档**：维护开发者文档

## 11. 结论

### 11.1 设计优势

- **并行使用**：JavaFX 和 Flutter Web 客户端可同时使用，满足不同用户的需求
- **平滑过渡**：支持从 JavaFX 客户端逐步迁移到 Flutter Web 客户端，减少用户影响
- **统一后端**：两个客户端共享同一个后端 API，减少开发和维护成本
- **平台优化**：针对不同平台进行优化，提供最佳用户体验
- **可扩展性**：模块化设计，便于未来功能扩展和技术升级

### 11.2 实施建议

1. **优先级**：先开发后端 API，再改造 JavaFX 客户端，最后开发 Flutter Web 客户端
2. **测试**：每个阶段都要进行充分的测试，确保功能正确性和稳定性
3. **用户反馈**：在迁移过程中积极收集用户反馈，及时调整策略
4. **文档**：保持文档的及时更新，包括 API 文档、用户指南和开发文档
5. **培训**：为开发团队提供必要的培训，确保团队成员熟悉新的技术栈

### 11.3 未来展望

- **Flutter 桌面**：考虑使用 Flutter 桌面版本替代 JavaFX 客户端，实现技术栈的统一
- **移动客户端**：利用 Flutter 的跨平台特性，开发移动客户端
- **云集成**：集成云存储服务，提供更广泛的文件管理能力
- **AI 辅助**：集成 AI 技术，提供智能文件管理建议
- **插件生态**：建立插件生态系统，鼓励第三方开发插件

通过实施本方案，FileManager Plus 将实现从 JavaFX 到 Flutter Web 的平滑过渡，同时保持系统的稳定性和功能完整性。新的架构将为用户提供更加现代化、灵活和高效的文件管理体验。

---

**设计文档**：客户端模块（JavaFX、Flutter）并行方案  
**版本**：1.0  
**日期**：2026-01-30  
**适用范围**：FileManager Plus 项目技术迁移
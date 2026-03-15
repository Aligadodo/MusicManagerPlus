import 'dart:convert';
import 'dart:async';
import 'package:http/http.dart' as http;
import 'package:web_socket_channel/web_socket_channel.dart';

class ApiException implements Exception {
  final int statusCode;
  final String message;
  final String endpoint;

  ApiException({
    required this.statusCode,
    required this.message,
    required this.endpoint,
  });

  @override
  String toString() {
    return 'ApiException: $message (Status: $statusCode, Endpoint: $endpoint)';
  }
}

class ApiClient {
  // API地址配置，可根据不同环境修改
  static const String baseUrl = const String.fromEnvironment('API_BASE_URL', defaultValue: 'http://localhost:8080');
  static const String wsBaseUrl = const String.fromEnvironment('API_WS_BASE_URL', defaultValue: 'ws://localhost:8080');

  // 单例实例
  static final ApiClient _instance = ApiClient._internal();
  factory ApiClient() => _instance;

  final http.Client _client;

  ApiClient._internal() : _client = http.Client();

  Future<http.Response> get(String endpoint, {Map<String, String>? headers, Map<String, String>? queryParams}) async {
    try {
      Uri uri = Uri.parse('$baseUrl$endpoint');
      if (queryParams != null && queryParams.isNotEmpty) {
        uri = uri.replace(queryParameters: queryParams.map((key, value) => MapEntry(key, value)));
      }
      final response = await _client.get(
        uri,
        headers: headers ?? _getDefaultHeaders(),
      );
      return _handleResponse(response, endpoint);
    } catch (e) {
      throw _handleError(e, endpoint);
    }
  }

  Future<http.Response> post(String endpoint, {dynamic body, Map<String, String>? headers}) async {
    try {
      final response = await _client.post(
        Uri.parse('$baseUrl$endpoint'),
        headers: headers ?? _getDefaultHeaders(),
        body: body != null ? jsonEncode(body) : null,
      );
      return _handleResponse(response, endpoint);
    } catch (e) {
      throw _handleError(e, endpoint);
    }
  }

  Future<void> logFrontendError({
    required String action,
    required String message,
    String? stackTrace,
    String? url,
    String? userAgent,
  }) async {
    try {
      await post('/api/logs/frontend-error', body: {
        'action': action,
        'message': message,
        if (stackTrace != null && stackTrace.isNotEmpty) 'stackTrace': stackTrace,
        if (url != null && url.isNotEmpty) 'url': url,
        if (userAgent != null && userAgent.isNotEmpty) 'userAgent': userAgent,
      });
    } catch (e) {
      print('Failed to log frontend error: $e');
    }
  }

  Future<http.Response> put(String endpoint, {dynamic body, Map<String, String>? headers}) async {
    try {
      final response = await _client.put(
        Uri.parse('$baseUrl$endpoint'),
        headers: headers ?? _getDefaultHeaders(),
        body: body != null ? jsonEncode(body) : null,
      );
      return _handleResponse(response, endpoint);
    } catch (e) {
      throw _handleError(e, endpoint);
    }
  }

  Future<http.Response> delete(String endpoint, {Map<String, String>? headers}) async {
    try {
      final response = await _client.delete(
        Uri.parse('$baseUrl$endpoint'),
        headers: headers ?? _getDefaultHeaders(),
      );
      return _handleResponse(response, endpoint);
    } catch (e) {
      throw _handleError(e, endpoint);
    }
  }

  http.Response _handleResponse(http.Response response, String endpoint) {
    if (response.statusCode < 200 || response.statusCode >= 300) {
      String errorMessage = '请求失败';
      try {
        final errorData = jsonDecode(response.body);
        errorMessage = errorData['message'] ?? errorData['error'] ?? errorMessage;
      } catch (e) {
        errorMessage = 'HTTP错误: ${response.statusCode}';
      }
      throw ApiException(
        statusCode: response.statusCode,
        message: errorMessage,
        endpoint: endpoint,
      );
    }
    return response;
  }

  Exception _handleError(dynamic error, String endpoint) {
    if (error is ApiException) {
      return error;
    }
    if (error is http.ClientException) {
      return ApiException(
        statusCode: 0,
        message: '网络连接失败: ${error.message}',
        endpoint: endpoint,
      );
    }
    if (error is FormatException) {
      return ApiException(
        statusCode: 0,
        message: '数据格式错误: ${error.message}',
        endpoint: endpoint,
      );
    }
    return ApiException(
      statusCode: 0,
      message: '未知错误: ${error.toString()}',
      endpoint: endpoint,
    );
  }

  WebSocketChannel connectWebSocket(String endpoint) {
    final channel = WebSocketChannel.connect(
      Uri.parse('$wsBaseUrl$endpoint'),
    );
    
    // 添加心跳机制
    _setupHeartbeat(channel);
    
    return channel;
  }

  void _setupHeartbeat(WebSocketChannel channel) {
    // 心跳间隔（秒）
    const int heartbeatInterval = 30;
    // 心跳超时时间（秒）
    const int heartbeatTimeout = 60;
    
    // 发送心跳的定时器
    late Timer heartbeatTimer;
    // 检查心跳响应的定时器
    late Timer timeoutTimer;
    
    // 重置心跳超时定时器
    void resetTimeoutTimer() {
      timeoutTimer?.cancel();
      timeoutTimer = Timer(Duration(seconds: heartbeatTimeout), () {
        // 心跳超时，关闭连接
        print('WebSocket heartbeat timeout, closing connection');
        channel.sink.close();
        heartbeatTimer.cancel();
      });
    }
    
    // 发送心跳
    void sendHeartbeat() {
      try {
        channel.sink.add(jsonEncode({'type': 'heartbeat', 'timestamp': DateTime.now().millisecondsSinceEpoch}));
        resetTimeoutTimer();
      } catch (e) {
        print('Failed to send heartbeat: $e');
        heartbeatTimer.cancel();
        timeoutTimer.cancel();
      }
    }
    
    // 监听消息，处理心跳响应
    channel.stream.listen(
      (message) {
        try {
          final data = jsonDecode(message);
          if (data['type'] == 'heartbeat') {
            // 收到心跳响应，重置超时定时器
            resetTimeoutTimer();
          }
        } catch (e) {
          // 非JSON消息，忽略
        }
      },
      onError: (error) {
        print('WebSocket error: $error');
        heartbeatTimer.cancel();
        timeoutTimer.cancel();
      },
      onDone: () {
        print('WebSocket connection closed');
        heartbeatTimer.cancel();
        timeoutTimer.cancel();
      },
    );
    
    // 启动心跳定时器
    heartbeatTimer = Timer.periodic(Duration(seconds: heartbeatInterval), (_) {
      sendHeartbeat();
    });
    
    // 发送初始心跳
    sendHeartbeat();
  }

  Map<String, String> _getDefaultHeaders() {
    return {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };
  }

  void dispose() {
    _client.close();
  }
}

import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:web_socket_channel/web_socket_channel.dart';

class ApiClient {
  static const String baseUrl = 'http://localhost:8080/api';
  static const String wsBaseUrl = 'ws://localhost:8080/api';

  final http.Client _client;

  ApiClient() : _client = http.Client();

  Future<http.Response> get(String endpoint, {Map<String, String>? headers}) async {
    final response = await _client.get(
      Uri.parse('$baseUrl$endpoint'),
      headers: headers ?? _getDefaultHeaders(),
    );
    return response;
  }

  Future<http.Response> post(String endpoint, {dynamic body, Map<String, String>? headers}) async {
    final response = await _client.post(
      Uri.parse('$baseUrl$endpoint'),
      headers: headers ?? _getDefaultHeaders(),
      body: jsonEncode(body),
    );
    return response;
  }

  Future<http.Response> put(String endpoint, {dynamic body, Map<String, String>? headers}) async {
    final response = await _client.put(
      Uri.parse('$baseUrl$endpoint'),
      headers: headers ?? _getDefaultHeaders(),
      body: jsonEncode(body),
    );
    return response;
  }

  Future<http.Response> delete(String endpoint, {Map<String, String>? headers}) async {
    final response = await _client.delete(
      Uri.parse('$baseUrl$endpoint'),
      headers: headers ?? _getDefaultHeaders(),
    );
    return response;
  }

  WebSocketChannel connectWebSocket(String endpoint) {
    final channel = WebSocketChannel.connect(
      Uri.parse('$wsBaseUrl$endpoint'),
    );
    return channel;
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

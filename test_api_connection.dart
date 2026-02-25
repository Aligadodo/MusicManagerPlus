import 'dart:convert';
import 'package:http/http.dart' as http;

void main() async {
  print('测试后端API连接...');
  
  // 测试 /api/config 端点
  print('\n1. 测试 /api/config 端点:');
  try {
    final response = await http.get(Uri.parse('http://localhost:8080/api/config'));
    print('状态码: ${response.statusCode}');
    print('响应体长度: ${response.body.length} 字符');
    if (response.statusCode == 200) {
      print('✓ 配置API调用成功');
    } else {
      print('✗ 配置API调用失败');
    }
  } catch (e) {
    print('✗ 配置API调用异常: $e');
  }
  
  // 测试 /api/tasks 端点
  print('\n2. 测试 /api/tasks 端点:');
  try {
    final response = await http.get(Uri.parse('http://localhost:8080/api/tasks?page=1&size=10'));
    print('状态码: ${response.statusCode}');
    print('响应体长度: ${response.body.length} 字符');
    if (response.statusCode == 200) {
      print('✓ 任务API调用成功');
    } else {
      print('✗ 任务API调用失败');
    }
  } catch (e) {
    print('✗ 任务API调用异常: $e');
  }
  
  // 测试 /api/pipeline 端点
  print('\n3. 测试 /api/pipeline 端点:');
  try {
    final response = await http.get(Uri.parse('http://localhost:8080/api/pipeline'));
    print('状态码: ${response.statusCode}');
    print('响应体长度: ${response.body.length} 字符');
    if (response.statusCode == 200) {
      print('✓ 流水线API调用成功');
    } else {
      print('✗ 流水线API调用失败');
    }
  } catch (e) {
    print('✗ 流水线API调用异常: $e');
  }
  
  print('\n测试完成!');
}

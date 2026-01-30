import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:filemanager_flutter/api/api_client.dart';
import 'package:filemanager_flutter/api/source_directory_service.dart';
import 'package:filemanager_flutter/models/source_directory.dart';

class SourceDirectoriesPage extends ConsumerStatefulWidget {
  const SourceDirectoriesPage({super.key});

  @override
  ConsumerState<SourceDirectoriesPage> createState() => _SourceDirectoriesPageState();
}

class _SourceDirectoriesPageState extends ConsumerState<SourceDirectoriesPage> {
  late ApiClient _apiClient;
  late SourceDirectoryService _sourceDirectoryService;
  late TextEditingController _pathController;
  late TextEditingController _threadCountController;
  List<SourceDirectory> _sourceDirectories = [];
  bool _isLoading = false;
  String _errorMessage = '';

  @override
  void initState() {
    super.initState();
    _apiClient = ApiClient();
    _sourceDirectoryService = SourceDirectoryService(_apiClient);
    _pathController = TextEditingController();
    _threadCountController = TextEditingController(text: '4');
    _loadSourceDirectories();
  }

  @override
  void dispose() {
    _pathController.dispose();
    _threadCountController.dispose();
    super.dispose();
  }

  Future<void> _loadSourceDirectories() async {
    setState(() {
      _isLoading = true;
      _errorMessage = '';
    });

    try {
      final directories = await _sourceDirectoryService.getSourceDirectories();
      setState(() {
        _sourceDirectories = directories;
      });
    } catch (e) {
      setState(() {
        _errorMessage = '加载源目录失败: $e';
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _addSourceDirectory() async {
    setState(() {
      _isLoading = true;
      _errorMessage = '';
    });

    try {
      final path = _pathController.text.trim();
      final threadCount = int.tryParse(_threadCountController.text.trim()) ?? 4;

      if (path.isEmpty) {
        setState(() {
          _errorMessage = '路径不能为空';
        });
        return;
      }

      final directory = SourceDirectory(path: path, threadCount: threadCount);
      await _sourceDirectoryService.addSourceDirectory(directory);
      await _loadSourceDirectories();
      _pathController.clear();
      _threadCountController.text = '4';
    } catch (e) {
      setState(() {
        _errorMessage = '添加源目录失败: $e';
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _removeSourceDirectory(String path) async {
    setState(() {
      _isLoading = true;
      _errorMessage = '';
    });

    try {
      await _sourceDirectoryService.removeSourceDirectory(path);
      await _loadSourceDirectories();
    } catch (e) {
      setState(() {
        _errorMessage = '移除源目录失败: $e';
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _clearSourceDirectories() async {
    setState(() {
      _isLoading = true;
      _errorMessage = '';
    });

    try {
      await _sourceDirectoryService.clearSourceDirectories();
      await _loadSourceDirectories();
    } catch (e) {
      setState(() {
        _errorMessage = '清空源目录失败: $e';
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('源目录管理'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () {
            Navigator.pop(context);
          },
        ),
      ),
      body: Container(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          children: [
            Card(
              elevation: 4,
              margin: const EdgeInsets.only(bottom: 20),
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  children: [
                    const Text(
                      '添加源目录',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 16),
                    TextField(
                      controller: _pathController,
                      decoration: const InputDecoration(
                        labelText: '目录路径',
                        border: OutlineInputBorder(),
                      ),
                    ),
                    const SizedBox(height: 16),
                    TextField(
                      controller: _threadCountController,
                      decoration: const InputDecoration(
                        labelText: '线程数',
                        border: OutlineInputBorder(),
                      ),
                      keyboardType: TextInputType.number,
                    ),
                    const SizedBox(height: 16),
                    ElevatedButton(
                      onPressed: _addSourceDirectory,
                      child: const Text('添加'),
                    ),
                  ],
                ),
              ),
            ),
            if (_errorMessage.isNotEmpty)
              Container(
                padding: const EdgeInsets.all(10),
                color: Colors.red[100],
                child: Text(
                  _errorMessage,
                  style: const TextStyle(color: Colors.red),
                ),
              ),
            const SizedBox(height: 20),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  '源目录列表',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                ElevatedButton(
                  onPressed: _clearSourceDirectories,
                  child: const Text('清空'),
                ),
              ],
            ),
            const SizedBox(height: 16),
            if (_isLoading)
              const Center(
                child: CircularProgressIndicator(),
              )
            else if (_sourceDirectories.isEmpty)
              const Center(
                child: Text('暂无源目录'),
              )
            else
              Expanded(
                child: ListView.builder(
                  itemCount: _sourceDirectories.length,
                  itemBuilder: (context, index) {
                    final directory = _sourceDirectories[index];
                    return Card(
                      elevation: 2,
                      margin: const EdgeInsets.only(bottom: 10),
                      child: ListTile(
                        title: Text(directory.path),
                        subtitle: Text('线程数: ${directory.threadCount}'),
                        trailing: IconButton(
                          icon: const Icon(Icons.delete, color: Colors.red),
                          onPressed: () {
                            _removeSourceDirectory(directory.path);
                          },
                        ),
                      ),
                    );
                  },
                ),
              ),
          ],
        ),
      ),
    );
  }
}

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:filemanager_flutter/pages/home_page.dart';
import 'package:filemanager_flutter/models/file_info.dart';

class FileBrowserPage extends ConsumerStatefulWidget {
  const FileBrowserPage({super.key});

  @override
  ConsumerState<FileBrowserPage> createState() => _FileBrowserPageState();
}

class _FileBrowserPageState extends ConsumerState<FileBrowserPage> {
  late TextEditingController _pathController;
  List<FileInfo> _files = [];
  bool _isLoading = false;
  String _errorMessage = '';

  @override
  void initState() {
    super.initState();
    _pathController = TextEditingController(text: '/');
    _loadFiles('/');
  }

  @override
  void dispose() {
    _pathController.dispose();
    super.dispose();
  }

  Future<void> _loadFiles(String path) async {
    setState(() {
      _isLoading = true;
      _errorMessage = '';
    });

    try {
      final fileService = ref.read(fileServiceProvider);
      final files = await fileService.scanDirectory(path);
      setState(() {
        _files = files;
      });
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to load files: $e';
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  void _navigateTo(String path) {
    _pathController.text = path;
    _loadFiles(path);
  }

  void _navigateUp() {
    String currentPath = _pathController.text;
    if (currentPath != '/' && currentPath.isNotEmpty) {
      final parts = currentPath.split('/');
      final parentPath = parts.sublist(0, parts.length - 1).join('/') + (parts.length > 1 ? '' : '/');
      _navigateTo(parentPath);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('文件浏览器'),
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
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _pathController,
                    decoration: const InputDecoration(
                      labelText: '路径',
                      border: OutlineInputBorder(),
                    ),
                  ),
                ),
                IconButton(
                  icon: const Icon(Icons.arrow_upward),
                  onPressed: _navigateUp,
                ),
                ElevatedButton(
                  onPressed: () {
                    _loadFiles(_pathController.text);
                  },
                  child: const Text('浏览'),
                ),
              ],
            ),
            const SizedBox(height: 20),
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
            if (_isLoading)
              const Center(
                child: CircularProgressIndicator(),
              )
            else
              Expanded(
                child: ListView.builder(
                  itemCount: _files.length,
                  itemBuilder: (context, index) {
                    final file = _files[index];
                    return ListTile(
                      leading: Icon(
                        file.directory ? Icons.folder : Icons.file_copy,
                        color: file.directory ? Colors.blue : Colors.grey,
                      ),
                      title: Text(file.name),
                      subtitle: Text(
                        file.directory
                            ? '目录'
                            : '${(file.size / 1024).toStringAsFixed(2)} KB',
                      ),
                      trailing: Text(
                        DateTime.fromMillisecondsSinceEpoch(file.lastModified)
                            .toString(),
                        style: const TextStyle(fontSize: 12, color: Colors.grey),
                      ),
                      onTap: () {
                        if (file.directory) {
                          _navigateTo(file.path);
                        }
                      },
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

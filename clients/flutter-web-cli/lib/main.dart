import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:filemanager_flutter/pages/home_page.dart';
import 'package:filemanager_flutter/pages/file_browser.dart';
import 'package:filemanager_flutter/pages/strategy_config.dart';
import 'package:filemanager_flutter/pages/task_monitor.dart';

void main() {
  runApp(
    const ProviderScope(
      child: FileManagerApp(),
    ),
  );
}

class FileManagerApp extends StatelessWidget {
  const FileManagerApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'FileManager Plus',
      theme: ThemeData(
        primarySwatch: Colors.blue,
        useMaterial3: true,
      ),
      initialRoute: '/',
      routes: {
        '/': (context) => const HomePage(),
        '/file-browser': (context) => const FileBrowserPage(),
        '/strategy-config': (context) => const StrategyConfigPage(),
        '/task-monitor': (context) => const TaskMonitorPage(),
      },
    );
  }
}

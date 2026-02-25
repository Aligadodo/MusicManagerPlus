import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:filemanager_flutter/api/api_client.dart';
import 'package:filemanager_flutter/providers/theme_provider.dart';
import 'package:filemanager_flutter/widgets/main_layout.dart';

final apiClientProvider = Provider<ApiClient>((ref) => ApiClient());

void main() {
  runApp(
    const ProviderScope(
      child: FileManagerApp(),
    ),
  );
}

class FileManagerApp extends ConsumerWidget {
  const FileManagerApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return MaterialApp(
      title: 'MUSIC MANAGER PLUS - By chrse1997@163.com',
      theme: ThemeData(
        useMaterial3: true,
      ),
      home: ThemeProvider(
        child: const MainLayout(),
      ),
      debugShowCheckedModeBanner: false,
    );
  }


}

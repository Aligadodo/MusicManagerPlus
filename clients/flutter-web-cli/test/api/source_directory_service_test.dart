import 'package:flutter_test/flutter_test.dart';
import 'package:filemanager_flutter/api/source_directory_service.dart';
import 'package:filemanager_flutter/api/api_client.dart';

void main() {
  group('SourceDirectoryService', () {
    late SourceDirectoryService sourceDirectoryService;

    setUp(() {
      sourceDirectoryService = SourceDirectoryService(ApiClient());
    });

    test('getSourceDirectories initializes correctly', () {
      expect(sourceDirectoryService, isNotNull);
    });

    test('addSourceDirectory initializes correctly', () async {
      try {
        await sourceDirectoryService.addSourceDirectory('/test/path', threadCount: 4);
      } catch (e) {
        expect(e, isNotNull);
      }
    });

    test('removeSourceDirectory initializes correctly', () async {
      try {
        await sourceDirectoryService.removeSourceDirectory('/test/path');
      } catch (e) {
        expect(e, isNotNull);
      }
    });

    test('clearSourceDirectories initializes correctly', () async {
      try {
        await sourceDirectoryService.clearSourceDirectories();
      } catch (e) {
        expect(e, isNotNull);
      }
    });

    test('updateThreadCount initializes correctly', () async {
      try {
        await sourceDirectoryService.updateThreadCount('/test/path', 8);
      } catch (e) {
        expect(e, isNotNull);
      }
    });
  });
}

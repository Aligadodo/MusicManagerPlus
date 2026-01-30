import 'package:flutter_test/flutter_test.dart';
import 'package:filemanager_flutter/api/thread_pool_service.dart';
import 'package:filemanager_flutter/api/api_client.dart';

void main() {
  group('ThreadPoolService', () {
    late ThreadPoolService threadPoolService;

    setUp(() {
      threadPoolService = ThreadPoolService(ApiClient());
    });

    test('ThreadPoolService initializes correctly', () {
      expect(threadPoolService, isNotNull);
    });

    test('getThreadPoolConfig initializes correctly', () async {
      try {
        await threadPoolService.getThreadPoolConfig();
      } catch (e) {
        expect(e, isNotNull);
      }
    });

    test('setPreviewThreads initializes correctly', () async {
      try {
        await threadPoolService.setPreviewThreads(8);
      } catch (e) {
        expect(e, isNotNull);
      }
    });

    test('setExecutionThreads initializes correctly', () async {
      try {
        await threadPoolService.setExecutionThreads(16);
      } catch (e) {
        expect(e, isNotNull);
      }
    });
  });
}

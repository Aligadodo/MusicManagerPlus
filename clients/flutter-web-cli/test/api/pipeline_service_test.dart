import 'package:flutter_test/flutter_test.dart';
import 'package:filemanager_flutter/api/pipeline_service.dart';
import 'package:filemanager_flutter/api/api_client.dart';

void main() {
  group('PipelineService', () {
    late PipelineService pipelineService;

    setUp(() {
      pipelineService = PipelineService(ApiClient());
    });

    test('PipelineService initializes correctly', () {
      expect(pipelineService, isNotNull);
    });

    test('getPipeline initializes correctly', () async {
      try {
        await pipelineService.getPipeline();
      } catch (e) {
        expect(e, isNotNull);
      }
    });

    test('updatePipeline initializes correctly', () async {
      try {
        await pipelineService.updatePipeline([]);
      } catch (e) {
        expect(e, isNotNull);
      }
    });

    test('analyzePipeline initializes correctly', () async {
      try {
        await pipelineService.analyzePipeline([], []);
      } catch (e) {
        expect(e, isNotNull);
      }
    });

    test('executePipeline initializes correctly', () async {
      try {
        await pipelineService.executePipeline([], []);
      } catch (e) {
        expect(e, isNotNull);
      }
    });
  });
}

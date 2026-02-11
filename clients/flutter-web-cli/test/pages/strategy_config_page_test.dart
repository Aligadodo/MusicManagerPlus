import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:filemanager_flutter/pages/strategy/strategy_config_page.dart';
import 'package:filemanager_flutter/models/strategy_info.dart';
import 'package:filemanager_flutter/models/strategy_config.dart';
import 'package:filemanager_flutter/models/config_field.dart';
import 'package:filemanager_flutter/models/precondition_group.dart';

void main() {
  group('策略配置页面测试', () {
    testWidgets('SCP-001: 策略配置页面加载测试', (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: StrategyConfigPage(),
        ),
      );

      await tester.pumpAndSettle();

      expect(find.text('策略配置'), findsOneWidget);
    });

    testWidgets('SCP-002: 策略参数渲染测试 - 文本参数', (WidgetTester tester) async {
      final strategyInfo = StrategyInfo(
        id: 'test-strategy',
        name: '测试策略',
        description: '这是一个测试策略',
        configFields: [
          ConfigField(
            name: 'sourcePath',
            label: '源路径',
            type: 'text',
            description: '源文件路径',
            defaultValue: '',
            required: false,
          ),
        ],
        enabled: true,
      );

      final strategyConfig = StrategyConfig({
        'sourcePath': '/test/path',
      });

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Builder(
              builder: (context) {
                return Column(
                  children: [
                    Text(strategyInfo.name),
                    Text(strategyInfo.description),
                    ...strategyInfo.configFields.map((field) {
                      return TextField(
                        decoration: InputDecoration(
                          labelText: field.label,
                          hintText: field.description,
                        ),
                        controller: TextEditingController(
                          text: strategyConfig.getValue(field.name)?.toString() ?? '',
                        ),
                      );
                    }),
                  ],
                );
              },
            ),
          ),
        ),
      );

      await tester.pumpAndSettle();

      expect(find.text('测试策略'), findsOneWidget);
      expect(find.text('这是一个测试策略'), findsOneWidget);
      expect(find.text('源路径'), findsOneWidget);
    });

    testWidgets('SCP-002: 策略参数渲染测试 - 数字参数', (WidgetTester tester) async {
      final strategyInfo = StrategyInfo(
        id: 'test-strategy',
        name: '测试策略',
        description: '这是一个测试策略',
        configFields: [
          ConfigField(
            name: 'maxDepth',
            label: '最大深度',
            type: 'number',
            description: '扫描深度',
            defaultValue: 5,
            required: false,
          ),
        ],
        enabled: true,
      );

      final strategyConfig = StrategyConfig({
        'maxDepth': 10,
      });

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Builder(
              builder: (context) {
                return Column(
                  children: [
                    Text(strategyInfo.name),
                    ...strategyInfo.configFields.map((field) {
                      return TextField(
                        decoration: InputDecoration(
                          labelText: field.label,
                          hintText: field.description,
                        ),
                        controller: TextEditingController(
                          text: strategyConfig.getValue(field.name)?.toString() ?? '',
                        ),
                        keyboardType: TextInputType.number,
                      );
                    }),
                  ],
                );
              },
            ),
          ),
        ),
      );

      await tester.pumpAndSettle();

      expect(find.text('最大深度'), findsOneWidget);
      expect(find.text('10'), findsOneWidget);
    });

    testWidgets('SCP-002: 策略参数渲染测试 - 布尔参数', (WidgetTester tester) async {
      final strategyInfo = StrategyInfo(
        id: 'test-strategy',
        name: '测试策略',
        description: '这是一个测试策略',
        configFields: [
          ConfigField(
            name: 'enabled',
            label: '启用',
            type: 'boolean',
            description: '是否启用',
            defaultValue: false,
            required: false,
          ),
        ],
        enabled: true,
      );

      final strategyConfig = StrategyConfig({
        'enabled': true,
      });

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Builder(
              builder: (context) {
                return Column(
                  children: [
                    Text(strategyInfo.name),
                    ...strategyInfo.configFields.map((field) {
                      return CheckboxListTile(
                        title: Text(field.label),
                        subtitle: Text(field.description ?? ''),
                        value: strategyConfig.getValue(field.name) ?? field.defaultValue ?? false,
                        onChanged: (value) {},
                      );
                    }),
                  ],
                );
              },
            ),
          ),
        ),
      );

      await tester.pumpAndSettle();

      expect(find.text('启用'), findsOneWidget);
      expect(find.byType(Checkbox), findsOneWidget);
    });

    testWidgets('SCP-003: 策略参数修改测试 - 文本参数', (WidgetTester tester) async {
      final strategyConfig = StrategyConfig({
        'sourcePath': '/old/path',
      });

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: TextField(
              decoration: const InputDecoration(labelText: '源路径'),
              controller: TextEditingController(
                text: strategyConfig.getValue('sourcePath')?.toString() ?? '',
              ),
              onChanged: (value) {
                strategyConfig.setValue('sourcePath', value);
              },
            ),
          ),
        ),
      );

      await tester.pumpAndSettle();

      expect(find.text('/old/path'), findsOneWidget);

      await tester.enterText(find.byType(TextField), '/new/path');
      await tester.pumpAndSettle();

      expect(strategyConfig.getValue('sourcePath'), '/new/path');
    });

    testWidgets('SCP-003: 策略参数修改测试 - 数字参数', (WidgetTester tester) async {
      final strategyConfig = StrategyConfig({
        'maxDepth': 5,
      });

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: TextField(
              decoration: const InputDecoration(labelText: '最大深度'),
              controller: TextEditingController(
                text: strategyConfig.getValue('maxDepth')?.toString() ?? '',
              ),
              keyboardType: TextInputType.number,
              onChanged: (value) {
                strategyConfig.setValue('maxDepth', int.tryParse(value));
              },
            ),
          ),
        ),
      );

      await tester.pumpAndSettle();

      expect(find.text('5'), findsOneWidget);

      await tester.enterText(find.byType(TextField), '10');
      await tester.pumpAndSettle();

      expect(strategyConfig.getValue('maxDepth'), 10);
    });

    testWidgets('SCP-003: 策略参数修改测试 - 布尔参数', (WidgetTester tester) async {
      final strategyConfig = StrategyConfig({
        'enabled': false,
      });

      bool? currentValue = strategyConfig.getValue('enabled');

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: CheckboxListTile(
              title: const Text('启用'),
              value: currentValue ?? false,
              onChanged: (value) {
                strategyConfig.setValue('enabled', value);
                currentValue = value;
              },
            ),
          ),
        ),
      );

      await tester.pumpAndSettle();

      expect(find.byType(Checkbox), findsOneWidget);

      await tester.tap(find.byType(Checkbox));
      await tester.pumpAndSettle();

      expect(strategyConfig.getValue('enabled'), true);
    });

    testWidgets('SCP-004: 策略配置保存测试', (WidgetTester tester) async {
      final strategyConfig = StrategyConfig({
        'sourcePath': '/test/path',
        'maxDepth': 10,
        'enabled': true,
      });

      bool saved = false;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Column(
              children: [
                TextField(
                  decoration: const InputDecoration(labelText: '源路径'),
                  controller: TextEditingController(
                    text: strategyConfig.getValue('sourcePath')?.toString() ?? '',
                  ),
                ),
                ElevatedButton(
                  onPressed: () {
                    saved = true;
                  },
                  child: const Text('保存'),
                ),
              ],
            ),
          ),
        ),
      );

      await tester.pumpAndSettle();

      expect(find.text('保存'), findsOneWidget);

      await tester.tap(find.text('保存'));
      await tester.pumpAndSettle();

      expect(saved, true);
    });

    testWidgets('SCP-005: 策略配置重置测试', (WidgetTester tester) async {
      final strategyConfig = StrategyConfig({
        'sourcePath': '/modified/path',
      });

      final defaultValues = {
        'sourcePath': '/default/path',
      };

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Column(
              children: [
                TextField(
                  decoration: const InputDecoration(labelText: '源路径'),
                  controller: TextEditingController(
                    text: strategyConfig.getValue('sourcePath')?.toString() ?? '',
                  ),
                ),
                ElevatedButton(
                  onPressed: () {
                    defaultValues.forEach((key, value) {
                      strategyConfig.setValue(key, value);
                    });
                  },
                  child: const Text('重置'),
                ),
              ],
            ),
          ),
        ),
      );

      await tester.pumpAndSettle();

      expect(find.text('/modified/path'), findsOneWidget);

      await tester.tap(find.text('重置'));
      await tester.pumpAndSettle();

      expect(strategyConfig.getValue('sourcePath'), '/default/path');
    });

    testWidgets('策略配置多参数渲染测试', (WidgetTester tester) async {
      final strategyInfo = StrategyInfo(
        id: 'test-strategy',
        name: '测试策略',
        description: '这是一个测试策略',
        configFields: [
          ConfigField(
            name: 'sourcePath',
            label: '源路径',
            type: 'text',
            description: '源文件路径',
            defaultValue: '',
            required: false,
          ),
          ConfigField(
            name: 'maxDepth',
            label: '最大深度',
            type: 'number',
            description: '扫描深度',
            defaultValue: 5,
            required: false,
          ),
          ConfigField(
            name: 'enabled',
            label: '启用',
            type: 'boolean',
            description: '是否启用',
            defaultValue: false,
            required: false,
          ),
        ],
        enabled: true,
      );

      final strategyConfig = StrategyConfig({
        'sourcePath': '/test/path',
        'maxDepth': 10,
        'enabled': true,
      });

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Builder(
              builder: (context) {
                return Column(
                  children: [
                    Text(strategyInfo.name),
                    ...strategyInfo.configFields.map((field) {
                      switch (field.type) {
                        case 'text':
                          return TextField(
                            decoration: InputDecoration(
                              labelText: field.label,
                              hintText: field.description,
                            ),
                            controller: TextEditingController(
                              text: strategyConfig.getValue(field.name)?.toString() ?? '',
                            ),
                          );
                        case 'number':
                          return TextField(
                            decoration: InputDecoration(
                              labelText: field.label,
                              hintText: field.description,
                            ),
                            controller: TextEditingController(
                              text: strategyConfig.getValue(field.name)?.toString() ?? '',
                            ),
                            keyboardType: TextInputType.number,
                          );
                        case 'boolean':
                          return CheckboxListTile(
                            title: Text(field.label),
                            subtitle: Text(field.description ?? ''),
                            value: strategyConfig.getValue(field.name) ?? field.defaultValue ?? false,
                            onChanged: (value) {},
                          );
                        default:
                          return const SizedBox.shrink();
                      }
                    }),
                  ],
                );
              },
            ),
          ),
        ),
      );

      await tester.pumpAndSettle();

      expect(find.text('测试策略'), findsOneWidget);
      expect(find.text('源路径'), findsOneWidget);
      expect(find.text('最大深度'), findsOneWidget);
      expect(find.text('启用'), findsOneWidget);
      expect(find.text('/test/path'), findsOneWidget);
      expect(find.text('10'), findsOneWidget);
    });

    testWidgets('策略配置空值处理测试', (WidgetTester tester) async {
      final strategyConfig = StrategyConfig({});

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: TextField(
              decoration: const InputDecoration(labelText: '源路径'),
              controller: TextEditingController(
                text: strategyConfig.getValue('sourcePath')?.toString() ?? '',
              ),
            ),
          ),
        ),
      );

      await tester.pumpAndSettle();

      expect(find.text('源路径'), findsOneWidget);
      expect(find.text(''), findsOneWidget);
    });

    testWidgets('策略配置前置条件组渲染测试', (WidgetTester tester) async {
      final strategyConfig = StrategyConfig(
        {
          'sourcePath': '/test/path',
        },
        preconditionGroups: [
          PreconditionGroup(
            id: 'test-group',
            name: '测试组',
            description: '测试组描述',
            logicType: 'AND',
            preconditions: [],
          ),
        ],
      );

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Column(
              children: [
                Text('策略配置'),
                ...strategyConfig.preconditionGroups.map((group) {
                  return Card(
                    child: Padding(
                      padding: const EdgeInsets.all(8.0),
                      child: Text(group.name),
                    ),
                  );
                }),
              ],
            ),
          ),
        ),
      );

      await tester.pumpAndSettle();

      expect(find.text('策略配置'), findsOneWidget);
      expect(find.text('测试组'), findsOneWidget);
    });
  });
}

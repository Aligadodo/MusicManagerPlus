import 'dart:convert';
import 'dart:html' as html;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:filemanager_flutter/api/api_client.dart';
import 'package:filemanager_flutter/api/source_directory_service.dart';
import 'package:filemanager_flutter/api/strategy_service.dart';
import 'package:filemanager_flutter/api/pipeline_service.dart';
import 'package:filemanager_flutter/models/source_directory.dart';
import 'package:filemanager_flutter/models/strategy_info.dart';
import 'package:filemanager_flutter/models/strategy_config.dart';
import 'package:filemanager_flutter/models/config_field.dart';
import 'package:filemanager_flutter/models/precondition.dart';
import 'package:filemanager_flutter/models/precondition_group.dart';
import 'package:filemanager_flutter/pages/preview_page.dart';

class ComposePage extends ConsumerStatefulWidget {
  const ComposePage({super.key});

  @override
  ConsumerState<ComposePage> createState() => _ComposePageState();
}

class _ComposePageState extends ConsumerState<ComposePage> {
  late ApiClient _apiClient;
  late SourceDirectoryService _sourceDirectoryService;
  late StrategyService _strategyService;
  late PipelineService _pipelineService;

  List<SourceDirectory> _sourceDirectories = [];
  List<StrategyInfo> _pipelineStrategies = [];
  List<StrategyInfo> _availableStrategies = [];
  StrategyInfo? _selectedStrategy;
  StrategyInfo? _selectedPipelineStrategy;
  StrategyConfig? _strategyConfig;
  List<PreconditionGroup> _preconditionGroups = [];
  bool _autoRun = false;

  bool _isLoading = false;
  String _errorMessage = '';
  bool _isDisposed = false;

  @override
  void initState() {
    super.initState();
    _apiClient = ApiClient();
    _sourceDirectoryService = SourceDirectoryService(_apiClient);
    _strategyService = StrategyService(_apiClient);
    _pipelineService = PipelineService(_apiClient);
    _isDisposed = false;
    _loadData();
  }

  @override
  void dispose() {
    _isDisposed = true;
    super.dispose();
  }

  Future<void> _loadData() async {
    if (_isDisposed) return;
    
    setState(() {
      _isLoading = true;
      _errorMessage = '';
    });

    try {
      final sources = await _sourceDirectoryService.getSourceDirectories();
      if (_isDisposed) return;
      
      final strategies = await _strategyService.getAvailableStrategies();
      if (_isDisposed) return;
      
      final pipeline = await _pipelineService.getPipeline();
      if (_isDisposed) return;

      setState(() {
        _sourceDirectories = sources;
        _availableStrategies = strategies;
        _pipelineStrategies = pipeline;
        _isLoading = false;
      });
    } catch (e) {
      if (_isDisposed) return;
      
      setState(() {
        _errorMessage = '加载数据失败: $e';
        _isLoading = false;
      });
    }
  }

  Future<void> _addDirectory() async {
    try {
      final input = html.InputElement(type: 'file')
        ..attributes['webkitdirectory'] = 'true'
        ..attributes['directory'] = 'true'
        ..multiple = false;

      input.onChange.listen((event) {
        if (input.files?.isNotEmpty == true) {
          final file = input.files![0];
          String path = '';
          
          // 尝试从relativePath获取目录路径
          if (file.relativePath != null && file.relativePath!.isNotEmpty) {
            // relativePath格式: "目录/子目录/文件"
            // 提取第一个斜杠之前的部分作为目录名
            final firstSlashIndex = file.relativePath!.indexOf('/');
            if (firstSlashIndex != -1) {
              path = file.relativePath!.substring(0, firstSlashIndex);
            } else {
              // 如果没有斜杠，说明只选择了一个文件，使用文件名
              path = file.name;
            }
          } else {
            // 最后的回退方案
            path = file.name;
          }
          
          if (path.isNotEmpty) {
            _doAddDirectory(path);
          }
        }
      });

      input.click();
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('选择目录失败: $e')),
      );
    }
  }

  Future<void> _doAddDirectory(String path) async {
    try {
      // 直接使用API客户端发送请求，以正确处理后端响应格式
      final response = await _apiClient.post('/source-directories', body: {
        'path': path,
        'threadCount': 4,
      });
      if (response.statusCode == 200) {
        final jsonResponse = json.decode(response.body);
        if (jsonResponse['success'] == true) {
          // 重新加载目录列表以确保数据一致性
          final sources = await _sourceDirectoryService.getSourceDirectories();
          if (!_isDisposed) {
            setState(() {
              _sourceDirectories = sources;
            });
          }
        } else {
          throw Exception(jsonResponse['message'] ?? '添加目录失败');
        }
      } else {
        throw Exception('Failed to add source directory: ${response.statusCode}');
      }
    } catch (e) {
      if (!_isDisposed) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('添加目录失败: $e')),
        );
      }
    }
  }

  void _removeDirectory(SourceDirectory directory) {
    setState(() {
      _sourceDirectories.remove(directory);
    });
  }

  Future<void> _addStrategyStep() async {
    try {
      StrategyInfo? strategy;
      if (_selectedPipelineStrategy != null) {
        strategy = _selectedPipelineStrategy;
      } else if (_availableStrategies.isNotEmpty) {
        strategy = _availableStrategies.first;
      } else {
        throw Exception('没有可用的策略');
      }
      
      setState(() {
        _pipelineStrategies.add(strategy!);
      });
      await _pipelineService.updatePipeline(_pipelineStrategies);
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('添加步骤失败: $e')),
      );
    }
  }

  void _removeStrategy(StrategyInfo strategy) {
    setState(() {
      _pipelineStrategies.remove(strategy);
      _selectedStrategy = null;
    });
    _pipelineService.updatePipeline(_pipelineStrategies);
  }

  void _moveStrategy(int index, int direction) {
    final newIndex = index + direction;
    if (newIndex >= 0 && newIndex < _pipelineStrategies.length) {
      setState(() {
        final strategy = _pipelineStrategies.removeAt(index);
        _pipelineStrategies.insert(newIndex, strategy);
      });
      _pipelineService.updatePipeline(_pipelineStrategies);
    }
  }

  Future<void> _loadStrategyConfig(StrategyInfo strategy) async {
    setState(() {
      _selectedStrategy = strategy;
    });

    try {
      final config = await _strategyService.getStrategyConfig(strategy.id);
      setState(() {
        _strategyConfig = config;
      });
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('加载配置失败: $e')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage.isNotEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            SelectableText(_errorMessage, style: const TextStyle(color: Colors.red)),
            const SizedBox(height: 20),
            ElevatedButton(
              onPressed: _loadData,
              child: const Text('重试'),
            ),
          ],
        ),
      );
    }

    return Column(
      children: [
        _buildSectionHeaders(),
        Expanded(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                flex: 3,
                child: _buildLeftPanel(),
              ),
              Expanded(
                flex: 3,
                child: _buildMidPanel(),
              ),
              Expanded(
                flex: 4,
                child: _buildRightPanel(),
              ),
            ],
          ),
        ),
        _buildActionButtons(),
      ],
    );
  }

  Widget _buildSectionHeaders() {
    return Container(
      key: const ValueKey('section_headers_container'),
      padding: const EdgeInsets.symmetric(vertical: 10),
      child: Row(
        key: const ValueKey('section_headers_row'),
        children: [
          Expanded(
            flex: 3,
            child: _buildSectionHeader(
              'Step1-选择目录',
              '通过弹窗或者拖拽至空白处来添加需要处理的文件或文件夹。',
              'step1_header',
            ),
          ),
          Expanded(
            flex: 3,
            child: _buildSectionHeader(
              'Step2-流水线配置',
              '添加必要的处理流程，可同时应用不同的操作。点击任意项目，可打开详细的配置界面。（同一文件只会被修改一次）。',
              'step2_header',
            ),
          ),
          Expanded(
            flex: 4,
            child: _buildSectionHeader(
              'Step3-参数配置',
              '支持选中步骤并编辑步骤下的参数。支持配置步骤的前置条件，以在满足特定条件下才执行特定操作，用于更精细化的操作控制。',
              'step3_header',
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildActionButtons() {
    return Container(
      key: const ValueKey('action_buttons_container'),
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.9),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.grey.shade300),
      ),
      child: Row(
        key: const ValueKey('action_buttons_row'),
        mainAxisAlignment: MainAxisAlignment.end,
        children: [
          ElevatedButton.icon(
            key: const ValueKey('load_config_button'),
            onPressed: _loadConfigFromFile,
            icon: const Icon(Icons.file_upload),
            label: const Text('加载配置'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.blue,
              foregroundColor: Colors.white,
            ),
          ),
          const SizedBox(width: 10),
          ElevatedButton.icon(
            key: const ValueKey('save_config_button'),
            onPressed: _saveConfigToFile,
            icon: const Icon(Icons.file_download),
            label: const Text('保存配置'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.green,
              foregroundColor: Colors.white,
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _loadConfigFromFile() async {
    try {
      final input = html.InputElement(type: 'file')
        ..accept = '.json';
      input.onChange.listen((event) {
        if (input.files?.isNotEmpty == true) {
          final file = input.files![0];
          final reader = html.FileReader();
          reader.onLoad.listen((event) {
            try {
              final json = jsonDecode(reader.result as String) as Map<String, dynamic>;
              setState(() {
                if (json['sourceDirectories'] != null) {
                  _sourceDirectories = (json['sourceDirectories'] as List)
                      .map((e) => SourceDirectory.fromJson(e))
                      .toList();
                }
                if (json['pipelineStrategies'] != null) {
                  _pipelineStrategies = (json['pipelineStrategies'] as List)
                      .map((e) => StrategyInfo.fromJson(e))
                      .toList();
                }
              });
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('配置加载成功')),
              );
            } catch (e) {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(content: Text('配置文件解析失败: $e')),
              );
            }
          });
          reader.readAsText(file);
        }
      });
      input.click();
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('加载配置失败: $e')),
      );
    }
  }

  Future<void> _saveConfigToFile() async {
    try {
      final config = {
        'sourceDirectories': _sourceDirectories.map((e) => e.toJson()).toList(),
        'pipelineStrategies': _pipelineStrategies.map((e) => e.toJson()).toList(),
      };
      final jsonStr = jsonEncode(config);
      final blob = html.Blob([jsonStr], 'application/json');
      final url = html.Url.createObjectUrlFromBlob(blob);
      final anchor = html.AnchorElement(href: url)
        ..download = 'pipeline_config_${DateTime.now().millisecondsSinceEpoch}.json'
        ..click();
      html.Url.revokeObjectUrl(url);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('配置保存成功')),
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('保存配置失败: $e')),
      );
    }
  }

  Widget _buildSectionHeader(String title, String description, String key) {
    return Container(
      key: ValueKey('section_header_$key'),
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.9),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.grey.shade300),
      ),
      child: Column(
        key: ValueKey('section_header_column_$key'),
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 5),
          Text(
            description,
            style: TextStyle(
              fontSize: 12,
              color: Colors.grey.shade600,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildLeftPanel() {
    return Container(
      padding: const EdgeInsets.all(15),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.9),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.grey.shade300),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildSourceTools(),
          const SizedBox(height: 10),
          Expanded(
            child: _buildSourceList(),
          ),
        ],
      ),
    );
  }

  Widget _buildSourceTools() {
    return Row(
      key: const ValueKey('source_tools_row'),
      children: [
        ElevatedButton.icon(
          onPressed: _addDirectory,
          icon: const Icon(Icons.add),
          label: const Text('添加目录'),
          style: ElevatedButton.styleFrom(
            backgroundColor: Colors.blue,
            foregroundColor: Colors.white,
          ),
        ),
        const SizedBox(width: 10),
        ElevatedButton.icon(
          onPressed: () async {
            try {
              // 调用后端API清空目录
              await _sourceDirectoryService.clearSourceDirectories();
              // 清空本地状态
              if (!_isDisposed) {
                setState(() {
                  _sourceDirectories.clear();
                });
              }
            } catch (e) {
              if (!_isDisposed) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('清空目录失败: $e')),
                );
              }
            }
          },
          icon: const Icon(Icons.clear),
          label: const Text('清空'),
          style: ElevatedButton.styleFrom(
            backgroundColor: Colors.red,
            foregroundColor: Colors.white,
          ),
        ),
      ],
    );
  }

  Widget _buildSourceList() {
    if (_sourceDirectories.isEmpty) {
      return Container(
        alignment: Alignment.center,
        child: Text(
          '拖拽文件夹到此',
          style: TextStyle(
            color: Colors.grey.shade400,
            fontSize: 16,
          ),
        ),
      );
    }

    return ListView.builder(
      itemCount: _sourceDirectories.length,
      itemBuilder: (context, index) {
        final directory = _sourceDirectories[index];
        return _buildSourceListItem(directory, index);
      },
    );
  }

  Widget _buildSourceListItem(SourceDirectory directory, int index) {
    return Container(
      key: ValueKey('source_directory_item_$index'),
      padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 12),
      decoration: BoxDecoration(
        border: Border(
          bottom: BorderSide(color: Colors.grey.shade300),
        ),
      ),
      child: Row(
        key: const ValueKey('source_directory_row'),
        children: [
          Expanded(
            child: Column(
              key: const ValueKey('source_directory_info_column'),
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  directory.path.split('/').last,
                  style: const TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 14,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  directory.path,
                  style: TextStyle(
                    color: Colors.grey.shade600,
                    fontSize: 12,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          IconButton(
            key: const ValueKey('source_directory_up_button'),
            icon: const Icon(Icons.arrow_upward, size: 20),
            onPressed: index > 0
                ? () {
                    setState(() {
                      final temp = _sourceDirectories[index - 1];
                      _sourceDirectories[index - 1] = directory;
                      _sourceDirectories[index] = temp;
                    });
                  }
                : null,
            padding: EdgeInsets.zero,
          ),
          IconButton(
            key: const ValueKey('source_directory_down_button'),
            icon: const Icon(Icons.arrow_downward, size: 20),
            onPressed: index < _sourceDirectories.length - 1
                ? () {
                    setState(() {
                      final temp = _sourceDirectories[index + 1];
                      _sourceDirectories[index + 1] = directory;
                      _sourceDirectories[index] = temp;
                    });
                  }
                : null,
            padding: EdgeInsets.zero,
          ),
          IconButton(
            key: const ValueKey('source_directory_delete_button'),
            icon: const Icon(Icons.delete, size: 20, color: Colors.red),
            onPressed: () => _removeDirectory(directory),
            padding: EdgeInsets.zero,
          ),
        ],
      ),
    );
  }



  Widget _buildMidPanel() {
    return Container(
      padding: const EdgeInsets.all(15),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.9),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.grey.shade300),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildPipelineTools(),
          const SizedBox(height: 10),
          Expanded(
            child: _buildPipelineList(),
          ),
        ],
      ),
    );
  }

  Widget _buildPipelineTools() {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        DropdownButton<StrategyInfo>(
          hint: const Text('选择功能...'),
          value: _selectedPipelineStrategy,
          items: _availableStrategies.map((strategy) {
            return DropdownMenuItem<StrategyInfo>(
              value: strategy,
              child: Text(strategy.name),
            );
          }).toList(),
          onChanged: (value) {
            setState(() {
              _selectedPipelineStrategy = value;
            });
          },
        ),
        const SizedBox(width: 10),
        ElevatedButton.icon(
          onPressed: _addStrategyStep,
          icon: const Icon(Icons.add),
          label: const Text('添加步骤'),
          style: ElevatedButton.styleFrom(
            backgroundColor: Colors.blue,
            foregroundColor: Colors.white,
          ),
        ),
      ],
    );
  }

  Widget _buildPipelineList() {
    if (_pipelineStrategies.isEmpty) {
      return Container(
        alignment: Alignment.center,
        child: Text(
          '暂无流水线步骤',
          style: TextStyle(
            color: Colors.grey.shade400,
            fontSize: 16,
          ),
        ),
      );
    }

    return ListView.builder(
      itemCount: _pipelineStrategies.length,
      itemBuilder: (context, index) {
        final strategy = _pipelineStrategies[index];
        final isSelected = _selectedStrategy?.id == strategy.id;
        return _buildPipelineListItem(strategy, index, isSelected);
      },
    );
  }

  Widget _buildPipelineListItem(StrategyInfo strategy, int index, bool isSelected) {
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      decoration: BoxDecoration(
        color: isSelected ? Colors.blue.withOpacity(0.1) : Colors.transparent,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(
          color: isSelected ? Colors.blue : Colors.grey.shade300,
        ),
      ),
      child: InkWell(
        onTap: () => _loadStrategyConfig(strategy),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
          child: Row(
            children: [
              CircleAvatar(
                backgroundColor: Colors.blue,
                child: Text(
                  '${index + 1}',
                  style: const TextStyle(color: Colors.white),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      strategy.name,
                      style: const TextStyle(fontWeight: FontWeight.bold),
                    ),
                    Text(
                      strategy.description,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(fontSize: 12),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 12),
              Row(
                key: ValueKey('pipeline_actions_row_$index'),
                mainAxisSize: MainAxisSize.min,
                children: [
                  IconButton(
                    key: ValueKey('pipeline_up_button_$index'),
                    icon: const Icon(Icons.arrow_upward, size: 20),
                    onPressed: index > 0 ? () => _moveStrategy(index, -1) : null,
                    padding: EdgeInsets.zero,
                  ),
                  IconButton(
                    key: ValueKey('pipeline_down_button_$index'),
                    icon: const Icon(Icons.arrow_downward, size: 20),
                    onPressed: index < _pipelineStrategies.length - 1
                        ? () => _moveStrategy(index, 1)
                        : null,
                    padding: EdgeInsets.zero,
                  ),
                  IconButton(
                    key: ValueKey('pipeline_delete_button_$index'),
                    icon: const Icon(Icons.delete, size: 20, color: Colors.red),
                    onPressed: () => _removeStrategy(strategy),
                    padding: EdgeInsets.zero,
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildRightPanel() {
    return Container(
      padding: const EdgeInsets.all(15),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.9),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.grey.shade300),
      ),
      child: _selectedStrategy == null
          ? Center(
              child: Text(
                '请选择一个步骤以查看配置',
                style: TextStyle(
                  color: Colors.grey.shade400,
                  fontSize: 16,
                ),
              ),
            )
          : _buildStrategyConfig(),
    );
  }

  Widget _buildStrategyConfig() {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            _selectedStrategy!.name,
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 10),
          Text(
            _selectedStrategy!.description,
            style: TextStyle(
              fontSize: 14,
              color: Colors.grey.shade600,
            ),
          ),
          const Divider(height: 30),
          const Text(
            '🔶 [前置条件]',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 10),
          _buildConditionsUI(),
          const Divider(height: 30),
          const Text(
            '🔶 [处理参数]',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 10),
          _buildParametersUI(),
        ],
      ),
    );
  }

  Widget _buildConditionsUI() {
    return Container(
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: Colors.grey.shade100,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (_preconditionGroups.isEmpty)
            const Text(
              '(点击下方按钮添加条件组)',
              style: TextStyle(
                color: Colors.grey,
                fontSize: 14,
              ),
            ),
          if (_preconditionGroups.isNotEmpty)
            Column(
              children: _preconditionGroups.asMap().entries.map((entry) {
                int index = entry.key;
                PreconditionGroup group = entry.value;
                return _buildPreconditionGroup(index, group);
              }).toList(),
            ),
          const SizedBox(height: 10),
          ElevatedButton.icon(
            onPressed: _addPreconditionGroup,
            icon: const Icon(Icons.add),
            label: const Text('添加条件组'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.blue,
              foregroundColor: Colors.white,
            ),
          ),
        ],
      ),
    );
  }

  void _addPreconditionGroup() {
    setState(() {
      _preconditionGroups.add(PreconditionGroup(
        id: 'group_${DateTime.now().millisecondsSinceEpoch}',
        name: '条件组 ${_preconditionGroups.length + 1}',
        description: '条件组描述',
        logicType: 'AND',
        preconditions: [],
      ));
    });
  }

  void _removePreconditionGroup(int index) {
    setState(() {
      _preconditionGroups.removeAt(index);
    });
  }

  Widget _buildPreconditionGroup(int index, PreconditionGroup group) {
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.grey.shade300),
      ),
      child: Column(
          key: ValueKey('precondition_group_column_$index'),
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              key: ValueKey('precondition_group_header_row_$index'),
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  group.name,
                  style: const TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 14,
                  ),
                ),
                IconButton(
                  key: ValueKey('precondition_group_delete_$index'),
                  icon: const Icon(Icons.delete, color: Colors.red),
                  onPressed: () => _removePreconditionGroup(index),
                ),
              ],
            ),
            const SizedBox(height: 10),
            Row(
              key: ValueKey('precondition_logic_type_row_$index'),
              children: [
              const Text('逻辑类型:'),
              const SizedBox(width: 10),
              DropdownButton<String>(
                value: group.logicType,
                items: const [
                  DropdownMenuItem(value: 'AND', child: Text('AND')),
                  DropdownMenuItem(value: 'OR', child: Text('OR')),
                ],
                onChanged: (value) {
                  setState(() {
                    _preconditionGroups[index] = PreconditionGroup(
                      id: group.id,
                      name: group.name,
                      description: group.description,
                      logicType: value ?? 'AND',
                      preconditions: group.preconditions,
                    );
                  });
                },
              ),
            ],
          ),
          const SizedBox(height: 10),
          if (group.preconditions.isEmpty)
            const Text(
              '暂无条件，点击下方按钮添加',
              style: TextStyle(
                color: Colors.grey,
                fontSize: 12,
              ),
            ),
          if (group.preconditions.isNotEmpty)
            Column(
              children: group.preconditions.asMap().entries.map((entry) {
                int conditionIndex = entry.key;
                Precondition condition = entry.value;
                return _buildPrecondition(index, conditionIndex, condition);
              }).toList(),
            ),
          ElevatedButton.icon(
            onPressed: () => _addPrecondition(index),
            icon: const Icon(Icons.add, size: 16),
            label: const Text('添加条件'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.green,
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
              minimumSize: const Size(0, 0),
            ),
          ),
        ],
      ),
    );
  }

  void _addPrecondition(int groupIndex) {
    setState(() {
      final group = _preconditionGroups[groupIndex];
      final newPreconditions = List<Precondition>.from(group.preconditions);
      newPreconditions.add(Precondition(
        id: 'condition_${DateTime.now().millisecondsSinceEpoch}',
        field: 'file',
        operator: 'contains',
        value: '',
        description: '条件描述',
      ));
      _preconditionGroups[groupIndex] = PreconditionGroup(
        id: group.id,
        name: group.name,
        description: group.description,
        logicType: group.logicType,
        preconditions: newPreconditions,
      );
    });
  }

  void _removePrecondition(int groupIndex, int conditionIndex) {
    setState(() {
      final group = _preconditionGroups[groupIndex];
      final newPreconditions = List<Precondition>.from(group.preconditions);
      newPreconditions.removeAt(conditionIndex);
      _preconditionGroups[groupIndex] = PreconditionGroup(
        id: group.id,
        name: group.name,
        description: group.description,
        logicType: group.logicType,
        preconditions: newPreconditions,
      );
    });
  }

  Widget _buildPrecondition(int groupIndex, int conditionIndex, Precondition condition) {
    return Container(
      margin: const EdgeInsets.only(bottom: 5),
      padding: const EdgeInsets.all(8),
      decoration: BoxDecoration(
        color: Colors.grey.shade50,
        borderRadius: BorderRadius.circular(6),
      ),
      child: Column(
        key: ValueKey('precondition_column_${groupIndex}_$conditionIndex'),
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            key: ValueKey('precondition_field_row_${groupIndex}_$conditionIndex'),
            mainAxisSize: MainAxisSize.min,
            children: [
              DropdownButton<String>(
                value: condition.field,
                items: const [
                  DropdownMenuItem(value: 'file', child: Text('文件')),
                  DropdownMenuItem(value: 'directory', child: Text('目录')),
                  DropdownMenuItem(value: 'extension', child: Text('扩展名')),
                  DropdownMenuItem(value: 'size', child: Text('文件大小')),
                  DropdownMenuItem(value: 'modified', child: Text('修改时间')),
                  DropdownMenuItem(value: 'created', child: Text('创建时间')),
                  DropdownMenuItem(value: 'name', child: Text('文件名')),
                  DropdownMenuItem(value: 'path', child: Text('文件路径')),
                ],
                onChanged: (value) {
                  setState(() {
                    final group = _preconditionGroups[groupIndex];
                    final newPreconditions = List<Precondition>.from(group.preconditions);
                    newPreconditions[conditionIndex] = Precondition(
                      id: condition.id,
                      field: value ?? 'file',
                      operator: condition.operator,
                      value: condition.value,
                      description: condition.description,
                    );
                    _preconditionGroups[groupIndex] = PreconditionGroup(
                      id: group.id,
                      name: group.name,
                      description: group.description,
                      logicType: group.logicType,
                      preconditions: newPreconditions,
                    );
                  });
                },
              ),
              const SizedBox(width: 10),
              DropdownButton<String>(
                value: condition.operator,
                items: const [
                  DropdownMenuItem(value: 'contains', child: Text('包含')),
                  DropdownMenuItem(value: 'equals', child: Text('等于')),
                  DropdownMenuItem(value: 'startsWith', child: Text('以...开头')),
                  DropdownMenuItem(value: 'endsWith', child: Text('以...结尾')),
                  DropdownMenuItem(value: 'greaterThan', child: Text('大于')),
                  DropdownMenuItem(value: 'lessThan', child: Text('小于')),
                  DropdownMenuItem(value: 'greaterThanOrEqual', child: Text('大于等于')),
                  DropdownMenuItem(value: 'lessThanOrEqual', child: Text('小于等于')),
                  DropdownMenuItem(value: 'notEquals', child: Text('不等于')),
                ],
                onChanged: (value) {
                  setState(() {
                    final group = _preconditionGroups[groupIndex];
                    final newPreconditions = List<Precondition>.from(group.preconditions);
                    newPreconditions[conditionIndex] = Precondition(
                      id: condition.id,
                      field: condition.field,
                      operator: value ?? 'contains',
                      value: condition.value,
                      description: condition.description,
                    );
                    _preconditionGroups[groupIndex] = PreconditionGroup(
                      id: group.id,
                      name: group.name,
                      description: group.description,
                      logicType: group.logicType,
                      preconditions: newPreconditions,
                    );
                  });
                },
              ),
            ],
          ),
          const SizedBox(height: 5),
          Row(
            key: ValueKey('precondition_value_row_${groupIndex}_$conditionIndex'),
            children: [
              Expanded(
                flex: 3,
                child: TextField(
                  controller: TextEditingController(text: condition.value.toString()),
                  onChanged: (value) {
                    setState(() {
                      final group = _preconditionGroups[groupIndex];
                      final newPreconditions = List<Precondition>.from(group.preconditions);
                      newPreconditions[conditionIndex] = Precondition(
                        id: condition.id,
                        field: condition.field,
                        operator: condition.operator,
                        value: value,
                        description: condition.description,
                      );
                      _preconditionGroups[groupIndex] = PreconditionGroup(
                        id: group.id,
                        name: group.name,
                        description: group.description,
                        logicType: group.logicType,
                        preconditions: newPreconditions,
                      );
                    });
                  },
                  decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    contentPadding: EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  ),
                ),
              ),
              const SizedBox(width: 5),
              Expanded(
                flex: 2,
                child: TextField(
                  controller: TextEditingController(text: condition.description),
                  onChanged: (value) {
                    setState(() {
                      final group = _preconditionGroups[groupIndex];
                      final newPreconditions = List<Precondition>.from(group.preconditions);
                      newPreconditions[conditionIndex] = Precondition(
                        id: condition.id,
                        field: condition.field,
                        operator: condition.operator,
                        value: condition.value,
                        description: value,
                      );
                      _preconditionGroups[groupIndex] = PreconditionGroup(
                        id: group.id,
                        name: group.name,
                        description: group.description,
                        logicType: group.logicType,
                        preconditions: newPreconditions,
                      );
                    });
                  },
                  decoration: const InputDecoration(
                    labelText: '描述',
                    border: OutlineInputBorder(),
                    contentPadding: EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  ),
                ),
              ),
              IconButton(
                icon: const Icon(Icons.delete, color: Colors.red, size: 16),
                onPressed: () => _removePrecondition(groupIndex, conditionIndex),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildParametersUI() {
    if (_selectedStrategy == null || _selectedStrategy!.configFields.isEmpty) {
      return const Text('无');
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: _selectedStrategy!.configFields.where((field) {
        // 检查条件参数是否满足
        if (field.dependsOn != null && field.dependsValue != null) {
          final dependentValue = _strategyConfig?.getValue(field.dependsOn!);
          return dependentValue?.toString() == field.dependsValue;
        }
        return true;
      }).map((field) {
        return _buildParameterField(field);
      }).toList(),
    );
  }

  Widget _buildParameterField(ConfigField field) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.grey.shade200),
        boxShadow: [
          BoxShadow(
            color: Colors.grey.shade100,
            blurRadius: 2,
            offset: Offset(0, 1),
          ),
        ],
      ),
      padding: const EdgeInsets.all(12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            field.label,
            style: TextStyle(
              fontWeight: FontWeight.bold,
              fontSize: 14,
              color: Colors.grey.shade800,
            ),
          ),
          const SizedBox(height: 8),
          _buildParameterInput(field),
        ],
      ),
    );
  }

  Widget _buildParameterInput(ConfigField field) {
    switch (field.type) {
      case 'string':
        return TextField(
          decoration: InputDecoration(
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(6),
              borderSide: BorderSide(color: Colors.grey.shade300),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(6),
              borderSide: BorderSide(color: Colors.grey.shade300),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(6),
              borderSide: BorderSide(color: Colors.blue, width: 2),
            ),
            hintText: field.defaultValue?.toString(),
            hintStyle: TextStyle(color: Colors.grey.shade400),
            contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
          ),
          onChanged: (value) {
            _updateConfigValue(field.name, value);
          },
        );
      case 'number':
        return TextField(
          keyboardType: TextInputType.number,
          decoration: InputDecoration(
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(6),
              borderSide: BorderSide(color: Colors.grey.shade300),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(6),
              borderSide: BorderSide(color: Colors.grey.shade300),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(6),
              borderSide: BorderSide(color: Colors.blue, width: 2),
            ),
            hintText: field.defaultValue?.toString(),
            hintStyle: TextStyle(color: Colors.grey.shade400),
            contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
          ),
          onChanged: (value) {
            _updateConfigValue(field.name, int.tryParse(value) ?? 0);
          },
        );
      case 'boolean':
        return Container(
          padding: const EdgeInsets.symmetric(vertical: 4),
          child: Row(
            children: [
              Checkbox(
                value: field.defaultValue == true,
                onChanged: (value) {
                  _updateConfigValue(field.name, value ?? false);
                },
                activeColor: Colors.blue,
                checkColor: Colors.white,
              ),
              const SizedBox(width: 8),
              Text(
                '启用',
                style: TextStyle(color: Colors.grey.shade700),
              ),
            ],
          ),
        );
      case 'directory':
        return Row(
          children: [
            Expanded(
              child: TextField(
                decoration: InputDecoration(
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(6),
                    borderSide: BorderSide(color: Colors.grey.shade300),
                  ),
                  enabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(6),
                    borderSide: BorderSide(color: Colors.grey.shade300),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(6),
                    borderSide: BorderSide(color: Colors.blue, width: 2),
                  ),
                  hintText: field.defaultValue?.toString(),
                  hintStyle: TextStyle(color: Colors.grey.shade400),
                  contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                ),
                onChanged: (value) {
                  _updateConfigValue(field.name, value);
                },
              ),
            ),
            const SizedBox(width: 10),
            Container(
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(6),
                border: Border.all(color: Colors.grey.shade300),
              ),
              child: IconButton(
                icon: const Icon(Icons.folder_open, color: Colors.blue),
                onPressed: () {},
                padding: const EdgeInsets.all(8),
              ),
            ),
          ],
        );
      case 'select':
        if (field.options != null && field.options!.isNotEmpty) {
          return Container(
            decoration: BoxDecoration(
              border: Border.all(color: Colors.grey.shade300),
              borderRadius: BorderRadius.circular(6),
            ),
            padding: const EdgeInsets.symmetric(horizontal: 8),
            child: DropdownButton<String>(
              value: field.defaultValue?.toString(),
              items: field.options!.map((option) {
                return DropdownMenuItem<String>(
                  value: option,
                  child: Text(option, style: TextStyle(color: Colors.grey.shade700)),
                );
              }).toList(),
              onChanged: (value) {
                _updateConfigValue(field.name, value);
              },
              isExpanded: true,
              underline: const SizedBox(),
              icon: Icon(Icons.keyboard_arrow_down, color: Colors.grey.shade600),
              hint: Text('请选择...', style: TextStyle(color: Colors.grey.shade400)),
            ),
          );
        }
        return TextField(
          decoration: InputDecoration(
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(6),
              borderSide: BorderSide(color: Colors.grey.shade300),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(6),
              borderSide: BorderSide(color: Colors.grey.shade300),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(6),
              borderSide: BorderSide(color: Colors.blue, width: 2),
            ),
            hintText: field.defaultValue?.toString(),
            hintStyle: TextStyle(color: Colors.grey.shade400),
            contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
          ),
          onChanged: (value) {
            _updateConfigValue(field.name, value);
          },
        );
      case 'list':
        final List<String> items = (field.defaultValue as List<String>?) ?? [];
        return Container(
          padding: const EdgeInsets.all(10),
          decoration: BoxDecoration(
            border: Border.all(color: Colors.grey.shade300),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    '列表项 (${items.length})',
                    style: const TextStyle(fontSize: 12, color: Colors.grey),
                  ),
                  ElevatedButton.icon(
                    onPressed: () => _showAddListItemDialog(field.name, items),
                    icon: const Icon(Icons.add, size: 16),
                    label: const Text('添加项目'),
                    style: ElevatedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                      minimumSize: const Size(0, 0),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(6),
                      ),
                    ),
                  ),
                ],
              ),
              if (items.isNotEmpty) ...[
                const SizedBox(height: 10),
                ...items.asMap().entries.map((entry) {
                  int index = entry.key;
                  String item = entry.value;
                  return Container(
                    margin: const EdgeInsets.only(bottom: 5),
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
                    decoration: BoxDecoration(
                      color: Colors.grey.shade50,
                      borderRadius: BorderRadius.circular(4),
                      border: Border.all(color: Colors.grey.shade200),
                    ),
                    child: Row(
                      children: [
                        Expanded(
                          child: Text(
                            item,
                            style: const TextStyle(fontSize: 13),
                          ),
                        ),
                        IconButton(
                          icon: const Icon(Icons.edit, size: 16, color: Colors.blue),
                          onPressed: () => _showEditListItemDialog(field.name, items, index),
                          padding: EdgeInsets.zero,
                          constraints: const BoxConstraints(),
                        ),
                        const SizedBox(width: 8),
                        IconButton(
                          icon: const Icon(Icons.delete, size: 16, color: Colors.red),
                          onPressed: () => _removeListItem(field.name, items, index),
                          padding: EdgeInsets.zero,
                          constraints: const BoxConstraints(),
                        ),
                      ],
                    ),
                  );
                }).toList(),
              ],
            ],
          ),
        );
      default:
        return TextField(
          decoration: InputDecoration(
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(6),
              borderSide: BorderSide(color: Colors.grey.shade300),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(6),
              borderSide: BorderSide(color: Colors.grey.shade300),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(6),
              borderSide: BorderSide(color: Colors.blue, width: 2),
            ),
            hintText: field.defaultValue?.toString(),
            hintStyle: TextStyle(color: Colors.grey.shade400),
            contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
          ),
          onChanged: (value) {
            _updateConfigValue(field.name, value);
          },
        );
    }
  }

  void _showAddListItemDialog(String fieldName, List<String> items) {
    final TextEditingController controller = TextEditingController();
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('添加项目'),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(
            hintText: '请输入内容',
            border: OutlineInputBorder(),
          ),
          autofocus: true,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          ElevatedButton(
            onPressed: () {
              if (controller.text.isNotEmpty) {
                setState(() {
                  final newItems = List<String>.from(items);
                  newItems.add(controller.text);
                  _updateConfigValue(fieldName, newItems);
                });
                Navigator.pop(context);
              }
            },
            child: const Text('添加'),
          ),
        ],
      ),
    );
  }

  void _showEditListItemDialog(String fieldName, List<String> items, int index) {
    final TextEditingController controller = TextEditingController(text: items[index]);
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('编辑项目'),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(
            hintText: '请输入内容',
            border: OutlineInputBorder(),
          ),
          autofocus: true,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          ElevatedButton(
            onPressed: () {
              if (controller.text.isNotEmpty) {
                setState(() {
                  final newItems = List<String>.from(items);
                  newItems[index] = controller.text;
                  _updateConfigValue(fieldName, newItems);
                });
                Navigator.pop(context);
              }
            },
            child: const Text('保存'),
          ),
        ],
      ),
    );
  }

  void _removeListItem(String fieldName, List<String> items, int index) {
    setState(() {
      final newItems = List<String>.from(items);
      newItems.removeAt(index);
      _updateConfigValue(fieldName, newItems);
    });
  }

  void _updateConfigValue(String fieldName, dynamic value) {
    if (_strategyConfig == null) {
      _strategyConfig = StrategyConfig({});
    }
    _strategyConfig!.setValue(fieldName, value);
  }
}

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
import 'package:filemanager_flutter/models/precondition_field_config.dart';
import 'package:filemanager_flutter/pages/preview_page.dart';
import 'package:filemanager_flutter/widgets/compose_directory_panel.dart';
import 'package:filemanager_flutter/widgets/compose_pipeline_panel.dart';
import 'package:filemanager_flutter/widgets/compose_config_panel.dart';

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
  StrategyConfig? _strategyConfig;
  List<PreconditionGroup> _preconditionGroups = [];

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
      print('开始加载数据...');
      
      print('正在加载源目录...');
      final sources = await _sourceDirectoryService.getSourceDirectories();
      print('源目录加载完成，数量: ${sources.length}');
      if (_isDisposed) return;
      
      print('正在加载策略列表...');
      final strategies = await _strategyService.getAvailableStrategies();
      print('策略列表加载完成，数量: ${strategies.length}');
      if (_isDisposed) return;
      
      print('正在加载流水线配置...');
      final pipeline = await _pipelineService.getPipeline();
      print('流水线配置加载完成，数量: ${pipeline.length}');
      if (_isDisposed) return;

      setState(() {
        _sourceDirectories = sources;
        _availableStrategies = strategies;
        _pipelineStrategies = pipeline;
        _isLoading = false;
      });
      
      print('数据加载完成');
    } catch (e) {
      if (_isDisposed) return;
      
      print('加载数据失败: $e');
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
        // 创建一个带有唯一流水线ID的策略实例
        strategy = _selectedPipelineStrategy!.copyWithPipelineId();
      } else if (_availableStrategies.isNotEmpty) {
        // 创建一个带有唯一流水线ID的策略实例
        strategy = _availableStrategies.first.copyWithPipelineId();
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
    return ComposeDirectoryPanel(
      sourceDirectories: _sourceDirectories,
      onDirectoriesChanged: (directories) {
        if (!_isDisposed) {
          setState(() {
            _sourceDirectories = directories;
          });
        }
      },
      sourceDirectoryService: _sourceDirectoryService,
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
    return ComposePipelinePanel(
      pipelineStrategies: _pipelineStrategies,
      availableStrategies: _availableStrategies,
      onPipelineChanged: (pipeline) {
        if (!_isDisposed) {
          setState(() {
            _pipelineStrategies = pipeline;
          });
        }
      },
      onStrategySelected: (strategy) {
        _loadStrategyConfig(strategy);
      },
      selectedStrategy: _selectedStrategy,
      pipelineService: _pipelineService,
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
        // 使用pipelineId或id来判断选中状态，确保同一策略的多个实例能被独立选择
        final isSelected = _selectedStrategy != null && 
                          (_selectedStrategy?.pipelineId == strategy.pipelineId || 
                          (_selectedStrategy?.pipelineId == null && _selectedStrategy?.id == strategy.id));
        print('策略 ${strategy.name} (id: ${strategy.id}, pipelineId: ${strategy.pipelineId}) 选中状态: $isSelected, 当前选中策略: ${_selectedStrategy?.name} (id: ${_selectedStrategy?.id}, pipelineId: ${_selectedStrategy?.pipelineId})');
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
    return ComposeConfigPanel(
      strategyConfig: _strategyConfig,
      preconditionGroups: _preconditionGroups,
      onConfigChanged: (config) {
        if (!_isDisposed) {
          setState(() {
            _strategyConfig = config;
          });
        }
      },
      onPreconditionGroupsChanged: (groups) {
        if (!_isDisposed) {
          setState(() {
            _preconditionGroups = groups;
          });
        }
      },
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
      padding: const EdgeInsets.all(15),
      decoration: BoxDecoration(
        color: Colors.grey.shade50,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.filter_list, color: Colors.blue.shade700, size: 20),
              const SizedBox(width: 8),
              const Text(
                '前置条件配置',
                style: TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 16,
                  color: Colors.black87,
                ),
              ),
              const SizedBox(width: 8),
              Tooltip(
                message: '设置文件处理的前置条件，只有满足条件的文件才会被处理',
                child: Icon(Icons.help_outline, color: Colors.grey.shade600, size: 18),
              ),
            ],
          ),
          const SizedBox(height: 15),
          if (_preconditionGroups.isEmpty)
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: Colors.grey.shade300, style: BorderStyle.solid),
              ),
              child: Column(
                children: [
                  Icon(Icons.inbox, color: Colors.grey.shade400, size: 48),
                  const SizedBox(height: 12),
                  Text(
                    '暂无前置条件',
                    style: TextStyle(
                      color: Colors.grey.shade600,
                      fontSize: 14,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '点击下方按钮添加条件组开始配置',
                    style: TextStyle(
                      color: Colors.grey.shade500,
                      fontSize: 12,
                    ),
                  ),
                ],
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
          const SizedBox(height: 15),
          ElevatedButton.icon(
            onPressed: _addPreconditionGroup,
            icon: const Icon(Icons.add_circle_outline),
            label: const Text('添加条件组'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.blue.shade700,
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(8),
              ),
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
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: Colors.blue.shade100),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 4,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
          key: ValueKey('precondition_group_column_$index'),
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              key: ValueKey('precondition_group_header_row_$index'),
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    Icon(Icons.folder_open, color: Colors.blue.shade600, size: 20),
                    const SizedBox(width: 8),
                    Text(
                      group.name,
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 15,
                        color: Colors.black87,
                      ),
                    ),
                  ],
                ),
                IconButton(
                  key: ValueKey('precondition_group_delete_$index'),
                  icon: Icon(Icons.delete_outline, color: Colors.red.shade400),
                  onPressed: () => _removePreconditionGroup(index),
                  tooltip: '删除条件组',
                ),
              ],
            ),
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.grey.shade50,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Row(
                key: ValueKey('precondition_logic_type_row_$index'),
                children: [
                  Icon(Icons.merge_type, color: Colors.grey.shade600, size: 18),
                  const SizedBox(width: 8),
                  const Text(
                    '逻辑关系:',
                    style: TextStyle(
                      fontWeight: FontWeight.w500,
                      fontSize: 14,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(6),
                      border: Border.all(color: Colors.grey.shade300),
                    ),
                    child: DropdownButton<String>(
                      value: group.logicType,
                      items: const [
                        DropdownMenuItem(value: 'AND', child: Text('AND (全部满足)')),
                        DropdownMenuItem(value: 'OR', child: Text('OR (任一满足)')),
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
                      style: const TextStyle(fontSize: 14),
                      dropdownColor: Colors.white,
                      underline: const SizedBox.shrink(),
                      icon: Icon(Icons.arrow_drop_down, color: Colors.grey.shade600),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Tooltip(
                    message: group.logicType == 'AND' 
                      ? '组内所有条件都必须满足' 
                      : '组内任一条件满足即可',
                    child: Icon(Icons.info_outline, color: Colors.blue.shade400, size: 16),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 12),
            if (group.preconditions.isEmpty)
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Colors.grey.shade50,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.grey.shade200, style: BorderStyle.solid),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(Icons.rule, color: Colors.grey.shade400, size: 24),
                    const SizedBox(width: 8),
                    Text(
                      '暂无条件，请添加条件',
                      style: TextStyle(
                        color: Colors.grey.shade600,
                        fontSize: 13,
                      ),
                    ),
                  ],
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
          const SizedBox(height: 12),
          Container(
            width: double.infinity,
            child: OutlinedButton.icon(
              onPressed: () => _addPrecondition(index),
              icon: Icon(Icons.add, size: 18, color: Colors.green.shade700),
              label: Text('添加条件', style: TextStyle(color: Colors.green.shade700)),
              style: OutlinedButton.styleFrom(
                side: BorderSide(color: Colors.green.shade300),
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(6),
                ),
              ),
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
      String newConditionId = 'condition_${DateTime.now().millisecondsSinceEpoch}';
      PreconditionFieldConfig defaultField = PreconditionFieldConfigs.fields.first;
      String defaultOperator = defaultField.operators.first.code;
      newPreconditions.add(Precondition(
        id: newConditionId,
        field: defaultField.code,
        operator: defaultOperator,
        value: '',
        description: '条件描述',
      ));
      _editingConditionIds.add(newConditionId);
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

  String? _validatePrecondition(Precondition condition) {
    PreconditionFieldConfig? fieldConfig = PreconditionFieldConfigs.getFieldConfig(condition.field);
    if (fieldConfig == null) return '无效的字段类型';

    if (!fieldConfig.requiresValue) {
      return null;
    }

    if (condition.value == null || condition.value.toString().isEmpty) {
      return '条件值不能为空';
    }
    
    String field = condition.field;
    String operator = condition.operator;
    String value = condition.value.toString();

    switch (field) {
      case 'size':
      case 'modified':
        if (operator == 'contains' || operator == 'startsWith' || operator == 'endsWith') {
          return '数值类型字段不支持包含、以...开头、以...结尾操作';
        }
        try {
          double.parse(value);
        } catch (e) {
          return '请输入有效的数值';
        }
        break;
      case 'extension':
        if (operator == 'equals' && !value.startsWith('.')) {
          return '扩展名应以点号开头，如 .mp3';
        }
        if (operator == 'in') {
          List<String> extensions = value.split(',').map((e) => e.trim()).toList();
          for (String ext in extensions) {
            if (!ext.startsWith('.')) {
              return '扩展名列表中的每一项都应以点号开头，如 .mp3,.wav';
            }
          }
        }
        break;
    }

    return null;
  }

  Widget _buildPrecondition(int groupIndex, int conditionIndex, Precondition condition) {
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.blue.shade50,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.blue.shade100),
      ),
      child: Column(
        key: ValueKey('precondition_column_${groupIndex}_$conditionIndex'),
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.tune, color: Colors.blue.shade600, size: 16),
              const SizedBox(width: 6),
              Text(
                '条件 ${conditionIndex + 1}',
                style: TextStyle(
                  fontWeight: FontWeight.w600,
                  fontSize: 13,
                  color: Colors.blue.shade700,
                ),
              ),
              const Spacer(),
              IconButton(
                icon: Icon(Icons.close, color: Colors.red.shade400, size: 18),
                onPressed: () => _removePrecondition(groupIndex, conditionIndex),
                tooltip: '删除条件',
                padding: EdgeInsets.zero,
                constraints: const BoxConstraints(),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(6),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  key: ValueKey('precondition_field_row_${groupIndex}_$conditionIndex'),
                  children: [
                    Icon(Icons.label, color: Colors.grey.shade600, size: 16),
                    const SizedBox(width: 8),
                    const Text(
                      '字段:',
                      style: TextStyle(
                        fontWeight: FontWeight.w500,
                        fontSize: 13,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(
                          color: Colors.grey.shade50,
                          borderRadius: BorderRadius.circular(4),
                          border: Border.all(color: Colors.grey.shade300),
                        ),
                        child: DropdownButton<String>(
                          value: condition.field,
                          isExpanded: true,
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
                          style: const TextStyle(fontSize: 13),
                          dropdownColor: Colors.white,
                          underline: const SizedBox.shrink(),
                          icon: Icon(Icons.arrow_drop_down, color: Colors.grey.shade600),
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Row(
                  key: ValueKey('precondition_operator_row_${groupIndex}_$conditionIndex'),
                  children: [
                    Icon(Icons.compare_arrows, color: Colors.grey.shade600, size: 16),
                    const SizedBox(width: 8),
                    const Text(
                      '操作符:',
                      style: TextStyle(
                        fontWeight: FontWeight.w500,
                        fontSize: 13,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(
                          color: Colors.grey.shade50,
                          borderRadius: BorderRadius.circular(4),
                          border: Border.all(color: Colors.grey.shade300),
                        ),
                        child: DropdownButton<String>(
                          value: condition.operator,
                          isExpanded: true,
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
                          style: const TextStyle(fontSize: 13),
                          dropdownColor: Colors.white,
                          underline: const SizedBox.shrink(),
                          icon: Icon(Icons.arrow_drop_down, color: Colors.grey.shade600),
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Row(
                  key: ValueKey('precondition_value_row_${groupIndex}_$conditionIndex'),
                  children: [
                    Icon(Icons.input, color: Colors.grey.shade600, size: 16),
                    const SizedBox(width: 8),
                    const Text(
                      '值:',
                      style: TextStyle(
                        fontWeight: FontWeight.w500,
                        fontSize: 13,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
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
                        decoration: InputDecoration(
                          hintText: '请输入值',
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(4),
                            borderSide: BorderSide(color: Colors.grey.shade300),
                          ),
                          contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                          isDense: true,
                          errorText: _validatePrecondition(condition),
                          errorStyle: TextStyle(fontSize: 11, color: Colors.red.shade700),
                        ),
                        style: const TextStyle(fontSize: 13),
                      ),
                    ),
                  ],
                ),
                if (_validatePrecondition(condition) != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 4),
                    child: Row(
                      children: [
                        Icon(Icons.warning_amber_rounded, color: Colors.amber.shade700, size: 14),
                        const SizedBox(width: 4),
                        Text(
                          _validatePrecondition(condition)!,
                          style: TextStyle(
                            fontSize: 11,
                            color: Colors.amber.shade700,
                          ),
                        ),
                      ],
                    ),
                  ),
              ],
            ),
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
          try {
            final dependentValue = _strategyConfig?.getValue(field.dependsOn!);
            return dependentValue?.toString() == field.dependsValue;
          } catch (e) {
            // 如果获取依赖值失败，默认显示该字段
            return true;
          }
        }
        return true;
      }).map((field) {
        try {
          return _buildParameterField(field);
        } catch (e) {
          // 如果构建字段失败，返回一个错误提示
          return Container(
            margin: const EdgeInsets.only(bottom: 12),
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.red.shade50,
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: Colors.red.shade200),
            ),
            child: Text(
              '构建参数字段失败: $e',
              style: const TextStyle(color: Colors.red),
            ),
          );
        }
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
            offset: const Offset(0, 1),
          ),
        ],
      ),
      padding: const EdgeInsets.all(12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text(
                field.label,
                style: TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 14,
                  color: Colors.grey.shade800,
                ),
              ),
              if (field.description.isNotEmpty) ...[
                const SizedBox(width: 8),
                Tooltip(
                  message: field.description,
                  child: Icon(
                    Icons.help_outline,
                    color: Colors.grey.shade500,
                    size: 16,
                  ),
                ),
              ],
            ],
          ),
          const SizedBox(height: 8),
          _buildParameterInput(field),
        ],
      ),
    );
  }

  Widget _buildParameterInput(ConfigField field) {
    try {
      final fieldType = field.type ?? 'string';
      
      switch (fieldType) {
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
                borderSide: const BorderSide(color: Colors.blue, width: 2),
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
                borderSide: const BorderSide(color: Colors.blue, width: 2),
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
                      borderSide: const BorderSide(color: Colors.blue, width: 2),
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
                borderSide: const BorderSide(color: Colors.blue, width: 2),
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
          List<String> items = [];
          try {
            if (field.defaultValue is List) {
              items = (field.defaultValue as List).map((item) => item?.toString() ?? '').toList();
            }
          } catch (e) {
            items = [];
          }
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
                  }),
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
                borderSide: const BorderSide(color: Colors.blue, width: 2),
              ),
              hintText: field.defaultValue?.toString() ?? '请输入...',
              hintStyle: TextStyle(color: Colors.grey.shade400),
              contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
            ),
            onChanged: (value) {
              _updateConfigValue(field.name, value);
            },
          );
      }
    } catch (e) {
      return Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: Colors.red.shade50,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: Colors.red.shade200),
        ),
        child: Text('构建输入控件失败: $e', style: const TextStyle(color: Colors.red)),
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
    _strategyConfig ??= StrategyConfig({});
    _strategyConfig!.setValue(fieldName, value);
  }
}

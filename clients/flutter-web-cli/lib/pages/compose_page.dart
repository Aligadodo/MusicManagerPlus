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

  @override
  void initState() {
    super.initState();
    _apiClient = ApiClient();
    _sourceDirectoryService = SourceDirectoryService(_apiClient);
    _strategyService = StrategyService(_apiClient);
    _pipelineService = PipelineService(_apiClient);
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() {
      _isLoading = true;
      _errorMessage = '';
    });

    try {
      final sources = await _sourceDirectoryService.getSourceDirectories();
      final strategies = await _strategyService.getAvailableStrategies();
      final pipeline = await _pipelineService.getPipeline();

      setState(() {
        _sourceDirectories = sources;
        _availableStrategies = strategies;
        _pipelineStrategies = pipeline;
        _isLoading = false;
      });
    } catch (e) {
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
          final path = file.relativePath ?? '';
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
      final directory = await _sourceDirectoryService.addSourceDirectory(
        SourceDirectory(path: path, threadCount: 4),
      );
      setState(() {
        _sourceDirectories.add(directory);
      });
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('添加目录失败: $e')),
      );
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
        _buildActionButtons(),
        _buildSectionHeaders(),
        Expanded(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                flex: 4,
                child: _buildLeftPanel(),
              ),
              Expanded(
                flex: 2,
                child: _buildMidPanel(),
              ),
              Expanded(
                flex: 4,
                child: _buildRightPanel(),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildActionButtons() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.9),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.grey.shade300),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.end,
        children: [
          ElevatedButton.icon(
            onPressed: _previewAction,
            icon: const Icon(Icons.visibility),
            label: const Text('预览'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.green,
              foregroundColor: Colors.white,
            ),
          ),
          const SizedBox(width: 10),
          ElevatedButton.icon(
            onPressed: _runAction,
            icon: const Icon(Icons.play_arrow),
            label: const Text('运行'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.blue,
              foregroundColor: Colors.white,
            ),
          ),
          const SizedBox(width: 10),
          ElevatedButton.icon(
            onPressed: _abortAction,
            icon: const Icon(Icons.stop),
            label: const Text('中止'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.red,
              foregroundColor: Colors.white,
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _previewAction() async {
    try {
      // 跳转到预览页面
      Navigator.push(
        context,
        MaterialPageRoute(builder: (context) => const PreviewPage()),
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('预览失败: $e')),
      );
    }
  }

  Future<void> _runAction() async {
    try {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('运行功能已触发')),
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('运行失败: $e')),
      );
    }
  }

  Future<void> _abortAction() async {
    try {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('中止功能已触发')),
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('中止失败: $e')),
      );
    }
  }

  Widget _buildSectionHeaders() {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 10),
      child: Row(
        children: [
          Expanded(
            flex: 1,
            child: _buildSectionHeader(
              'Step1-选择目录',
              '通过弹窗或者拖拽至空白处来添加需要处理的文件或文件夹。',
            ),
          ),
          Expanded(
            flex: 2,
            child: _buildSectionHeader(
              'Step2-流水线配置',
              '添加必要的处理流程，可同时应用不同的操作。点击任意项目，可打开详细的配置界面。（同一文件只会被修改一次）。',
            ),
          ),
          Expanded(
            flex: 2,
            child: _buildSectionHeader(
              'Step3-参数配置',
              '支持选中步骤并编辑步骤下的参数。支持配置步骤的前置条件，以在满足特定条件下才执行特定操作，用于更精细化的操作控制。',
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSectionHeader(String title, String description) {
    return Container(
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.9),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.grey.shade300),
      ),
      child: Column(
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
          const SizedBox(height: 10),
          _buildGlobalFilters(),
        ],
      ),
    );
  }

  Widget _buildSourceTools() {
    return Row(
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
          onPressed: () {
            setState(() {
              _sourceDirectories.clear();
            });
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
      padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 12),
      decoration: BoxDecoration(
        border: Border(
          bottom: BorderSide(color: Colors.grey.shade300),
        ),
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
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
          Row(
            children: [
              IconButton(
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
                constraints: const BoxConstraints(),
              ),
              IconButton(
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
                constraints: const BoxConstraints(),
              ),
              IconButton(
                icon: const Icon(Icons.delete, size: 20, color: Colors.red),
                onPressed: () => _removeDirectory(directory),
                padding: EdgeInsets.zero,
                constraints: const BoxConstraints(),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildGlobalFilters() {
    return Container(
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: Colors.grey.shade100,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            '全局筛选',
            style: TextStyle(
              fontWeight: FontWeight.bold,
              fontSize: 14,
            ),
          ),
          const SizedBox(height: 10),
          _buildFilterField('扫描模式', DropdownButton<String>(
            items: const [
              DropdownMenuItem(value: 'all', child: Text('全部文件')),
              DropdownMenuItem(value: 'current', child: Text('当前目录')),
              DropdownMenuItem(value: 'specified', child: Text('指定目录层级')),
              DropdownMenuItem(value: 'range', child: Text('目录层级范围')),
            ],
            onChanged: (value) {},
            isExpanded: true,
          )),
          const SizedBox(height: 10),
          _buildFilterField('扫描层级', TextField(
            keyboardType: TextInputType.number,
            decoration: const InputDecoration(
              border: OutlineInputBorder(),
              contentPadding: EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            ),
          )),
          const SizedBox(height: 10),
          _buildFilterField('目录层级范围', Row(
            children: [
              const Text('最小: '),
              SizedBox(
                width: 60,
                child: TextField(
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    contentPadding: EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  ),
                ),
              ),
              const SizedBox(width: 10),
              const Text('最大: '),
              SizedBox(
                width: 60,
                child: TextField(
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    contentPadding: EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  ),
                ),
              ),
            ],
          )),
          const SizedBox(height: 15),
          const Text(
            '文件类型过滤',
            style: TextStyle(
              fontWeight: FontWeight.bold,
              fontSize: 14,
            ),
          ),
          const SizedBox(height: 10),
          _buildFilterField('文件类型', Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              Chip(label: Text('音频')),
              Chip(label: Text('视频')),
              Chip(label: Text('图片')),
              Chip(label: Text('文档')),
              Chip(label: Text('压缩包')),
            ],
          )),
          const SizedBox(height: 15),
          const Text(
            '路径过滤规则',
            style: TextStyle(
              fontWeight: FontWeight.bold,
              fontSize: 14,
            ),
          ),
          const SizedBox(height: 10),
          TextField(
            decoration: const InputDecoration(
              border: OutlineInputBorder(),
              hintText: '输入过滤规则（如：*Convert*），按回车添加',
              contentPadding: EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            ),
          ),
          const SizedBox(height: 10),
          Container(
            height: 150,
            decoration: BoxDecoration(
              border: Border.all(color: Colors.grey.shade300),
              borderRadius: BorderRadius.circular(4),
            ),
            child: ListView.builder(
              itemCount: 5,
              itemBuilder: (context, index) {
                return Container(
                  padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 12),
                  decoration: BoxDecoration(
                    border: Border(
                      bottom: BorderSide(color: Colors.grey.shade300),
                    ),
                  ),
                  child: Row(
                    children: [
                      Expanded(
                        child: Text([
                          '*Convert*',
                          '*Temp*',
                          '*Cache*',
                          '*Log*',
                          '*/Windows/*'
                        ][index]),
                      ),
                      Row(
                        children: [
                          IconButton(
                            icon: const Icon(Icons.arrow_upward, size: 16),
                            onPressed: () {},
                          ),
                          IconButton(
                            icon: const Icon(Icons.arrow_downward, size: 16),
                            onPressed: () {},
                          ),
                          IconButton(
                            icon: const Icon(Icons.delete, size: 16, color: Colors.red),
                            onPressed: () {},
                          ),
                        ],
                      ),
                    ],
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFilterField(String label, Widget child) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: const TextStyle(fontSize: 12),
        ),
        const SizedBox(height: 5),
        child,
      ],
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
      children: [
        Expanded(
          child: DropdownButton<StrategyInfo>(
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
            isExpanded: true,
          ),
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
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
        leading: CircleAvatar(
          backgroundColor: Colors.blue,
          child: Text(
            '${index + 1}',
            style: const TextStyle(color: Colors.white),
          ),
        ),
        title: Text(
          strategy.name,
          style: const TextStyle(fontWeight: FontWeight.bold),
        ),
        subtitle: Text(
          strategy.description,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            IconButton(
              icon: const Icon(Icons.arrow_upward, size: 20),
              onPressed: index > 0 ? () => _moveStrategy(index, -1) : null,
              padding: EdgeInsets.zero,
              constraints: const BoxConstraints(),
            ),
            IconButton(
              icon: const Icon(Icons.arrow_downward, size: 20),
              onPressed: index < _pipelineStrategies.length - 1
                  ? () => _moveStrategy(index, 1)
                  : null,
              padding: EdgeInsets.zero,
              constraints: const BoxConstraints(),
            ),
            IconButton(
              icon: const Icon(Icons.delete, size: 20, color: Colors.red),
              onPressed: () => _removeStrategy(strategy),
              padding: EdgeInsets.zero,
              constraints: const BoxConstraints(),
            ),
          ],
        ),
        onTap: () => _loadStrategyConfig(strategy),
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
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
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
                icon: const Icon(Icons.delete, color: Colors.red),
                onPressed: () => _removePreconditionGroup(index),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Row(
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
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: DropdownButton<String>(
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
              ),
              Expanded(
                child: DropdownButton<String>(
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
              ),
            ],
          ),
          const SizedBox(height: 5),
          Row(
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
                constraints: const BoxConstraints(),
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
        return Container(
          padding: const EdgeInsets.all(10),
          decoration: BoxDecoration(
            border: Border.all(color: Colors.grey.shade300),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                '列表项 (${field.defaultValue != null ? (field.defaultValue as List).length : 0})',
                style: const TextStyle(fontSize: 12, color: Colors.grey),
              ),
              const SizedBox(height: 10),
              ElevatedButton.icon(
                onPressed: () {},
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

  void _updateConfigValue(String fieldName, dynamic value) {
    if (_strategyConfig == null) {
      _strategyConfig = StrategyConfig({});
    }
    _strategyConfig!.setValue(fieldName, value);
  }
}

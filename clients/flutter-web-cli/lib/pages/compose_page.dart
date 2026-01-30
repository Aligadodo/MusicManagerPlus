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
      final directory = await _sourceDirectoryService.addSourceDirectory(
        SourceDirectory(path: '/tmp/test', threadCount: 4),
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
      final strategy = _availableStrategies.firstWhere(
        (s) => s.id == 'file-cleanup',
        orElse: () => _availableStrategies.first,
      );
      setState(() {
        _pipelineStrategies.add(strategy);
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
            Text(_errorMessage, style: const TextStyle(color: Colors.red)),
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
                flex: 35,
                child: _buildMidPanel(),
              ),
              Expanded(
                flex: 35,
                child: _buildRightPanel(),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildSectionHeaders() {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 10),
      child: Row(
        children: [
          Expanded(
            flex: 3,
            child: _buildSectionHeader(
              'Step1-选择目录',
              '通过弹窗或者拖拽至空白处来添加需要处理的文件或文件夹。',
            ),
          ),
          Expanded(
            flex: 35,
            child: _buildSectionHeader(
              'Step2-流水线配置',
              '添加必要的处理流程，可同时应用不同的操作。点击任意项目，可打开详细的配置界面。（同一文件只会被修改一次）。',
            ),
          ),
          Expanded(
            flex: 35,
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
          _buildFilterField('递归模式', DropdownButton<String>(
            items: const [
              DropdownMenuItem(value: 'all', child: Text('全部')),
              DropdownMenuItem(value: 'files', child: Text('仅文件')),
              DropdownMenuItem(value: 'directories', child: Text('仅目录')),
            ],
            onChanged: (value) {},
            isExpanded: true,
          )),
          const SizedBox(height: 10),
          _buildFilterField('递归深度', Row(
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
            items: _availableStrategies.map((strategy) {
              return DropdownMenuItem<StrategyInfo>(
                value: strategy,
                child: Text(strategy.name),
              );
            }).toList(),
            onChanged: (value) {},
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
          const Text(
            '(点击下方按钮添加条件组)',
            style: TextStyle(
              color: Colors.grey,
              fontSize: 14,
            ),
          ),
          const SizedBox(height: 10),
          ElevatedButton.icon(
            onPressed: () {},
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

  Widget _buildParametersUI() {
    if (_selectedStrategy == null || _selectedStrategy!.configFields.isEmpty) {
      return const Text('无');
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: _selectedStrategy!.configFields.map((field) {
        return _buildParameterField(field);
      }).toList(),
    );
  }

  Widget _buildParameterField(ConfigField field) {
    return Container(
      margin: const EdgeInsets.only(bottom: 15),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            field.name,
            style: const TextStyle(
              fontWeight: FontWeight.bold,
              fontSize: 14,
            ),
          ),
          if (field.description != null && field.description!.isNotEmpty)
            Padding(
              padding: const EdgeInsets.only(bottom: 5),
              child: Text(
                field.description!,
                style: TextStyle(
                  fontSize: 12,
                  color: Colors.grey.shade600,
                ),
              ),
            ),
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
            border: const OutlineInputBorder(),
            hintText: field.defaultValue?.toString(),
          ),
          onChanged: (value) {},
        );
      case 'number':
        return TextField(
          keyboardType: TextInputType.number,
          decoration: InputDecoration(
            border: const OutlineInputBorder(),
            hintText: field.defaultValue?.toString(),
          ),
          onChanged: (value) {},
        );
      case 'boolean':
        return CheckboxListTile(
          title: const Text('启用'),
          value: field.defaultValue == true,
          onChanged: (value) {},
          controlAffinity: ListTileControlAffinity.leading,
          contentPadding: EdgeInsets.zero,
        );
      case 'directory':
        return Row(
          children: [
            Expanded(
              child: TextField(
                decoration: InputDecoration(
                  border: const OutlineInputBorder(),
                  hintText: field.defaultValue?.toString(),
                ),
                onChanged: (value) {},
              ),
            ),
            const SizedBox(width: 10),
            IconButton(
              icon: const Icon(Icons.folder_open),
              onPressed: () {},
            ),
          ],
        );
      default:
        return TextField(
          decoration: InputDecoration(
            border: const OutlineInputBorder(),
            hintText: field.defaultValue?.toString(),
          ),
          onChanged: (value) {},
        );
    }
  }
}

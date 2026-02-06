import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:filemanager_flutter/api/api_client.dart';
import 'package:filemanager_flutter/api/source_directory_service.dart';
import 'package:filemanager_flutter/api/strategy_service.dart';
import 'package:filemanager_flutter/api/pipeline_service.dart';
import 'package:filemanager_flutter/models/source_directory.dart';
import 'package:filemanager_flutter/models/strategy_info.dart';
import 'package:filemanager_flutter/models/strategy_config.dart';
import 'package:filemanager_flutter/models/precondition_group.dart';
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

  Widget _buildSectionHeader(String title, String description, String key) {
    return Container(
      key: ValueKey(key),
      padding: const EdgeInsets.all(15),
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
              color: Colors.black87,
            ),
          ),
          const SizedBox(height: 8),
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
            onPressed: () async {
              try {
                final pipeline = await _pipelineService.getPipeline();
                if (!_isDisposed) {
                  setState(() {
                    _pipelineStrategies = pipeline;
                  });
                }
              } catch (e) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('加载配置失败: $e')),
                );
              }
            },
            icon: const Icon(Icons.upload_file),
            label: const Text('加载配置'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.orange,
              foregroundColor: Colors.white,
            ),
          ),
          const SizedBox(width: 10),
          ElevatedButton.icon(
            key: const ValueKey('save_config_button'),
            onPressed: () async {
              try {
                await _pipelineService.updatePipeline(_pipelineStrategies);
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('配置已保存')),
                );
              } catch (e) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('保存配置失败: $e')),
                );
              }
            },
            icon: const Icon(Icons.save),
            label: const Text('保存配置'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.green,
              foregroundColor: Colors.white,
            ),
          ),
          const SizedBox(width: 10),
          ElevatedButton.icon(
            key: const ValueKey('preview_button'),
            onPressed: _sourceDirectories.isEmpty || _pipelineStrategies.isEmpty
                ? null
                : () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (context) => const PreviewPage(),
                      ),
                    );
                  },
            icon: const Icon(Icons.visibility),
            label: const Text('预览'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.blue,
              foregroundColor: Colors.white,
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

  Widget _buildRightPanel() {
    return ComposeConfigPanel(
      strategyInfo: _selectedStrategy,
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
}

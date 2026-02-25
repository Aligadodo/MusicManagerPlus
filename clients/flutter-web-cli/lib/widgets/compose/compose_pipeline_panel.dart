import 'package:flutter/material.dart';
import 'package:filemanager_flutter/models/strategy_info.dart';
import 'package:filemanager_flutter/api/pipeline_service.dart';
import 'package:filemanager_flutter/widgets/config/pipeline_list_item.dart';
import 'package:filemanager_flutter/widgets/common/selectable_text_widget.dart';
import 'package:filemanager_flutter/utils/theme_utils.dart';

class ComposePipelinePanel extends StatefulWidget {
  final List<StrategyInfo> pipelineStrategies;
  final List<StrategyInfo> availableStrategies;
  final Function(List<StrategyInfo>) onPipelineChanged;
  final Function(StrategyInfo) onStrategySelected;
  final StrategyInfo? selectedStrategy;
  final PipelineService pipelineService;

  const ComposePipelinePanel({
    super.key,
    required this.pipelineStrategies,
    required this.availableStrategies,
    required this.onPipelineChanged,
    required this.onStrategySelected,
    required this.selectedStrategy,
    required this.pipelineService,
  });

  @override
  State<ComposePipelinePanel> createState() => _ComposePipelinePanelState();
}

class _ComposePipelinePanelState extends State<ComposePipelinePanel> {
  StrategyInfo? _selectedPipelineStrategy;

  Future<void> _addStrategyStep() async {
    try {
      StrategyInfo? strategy;
      if (_selectedPipelineStrategy != null) {
        strategy = _selectedPipelineStrategy!.copyWithPipelineId();
      } else if (widget.availableStrategies.isNotEmpty) {
        strategy = widget.availableStrategies.first.copyWithPipelineId();
      } else {
        throw Exception('没有可用的策略');
      }
      
      final newPipeline = List<StrategyInfo>.from(widget.pipelineStrategies);
      newPipeline.add(strategy!);
      widget.onPipelineChanged(newPipeline);
      await widget.pipelineService.updatePipeline(newPipeline);
    } catch (e) {
      print('添加步骤失败: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: SelectableTextWidget(
              text: '添加步骤失败: $e',
              style: const TextStyle(color: Colors.white),
              maxLines: 5,
            ),
            backgroundColor: Colors.red,
            duration: const Duration(seconds: 5),
          ),
        );
      }
    }
  }

  void _removeStrategy(StrategyInfo strategy) {
    final newPipeline = List<StrategyInfo>.from(widget.pipelineStrategies);
    newPipeline.remove(strategy);
    widget.onPipelineChanged(newPipeline);
    widget.pipelineService.updatePipeline(newPipeline);
  }

  void _moveStrategy(int index, int direction) {
    final newIndex = index + direction;
    if (newIndex >= 0 && newIndex < widget.pipelineStrategies.length) {
      final newPipeline = List<StrategyInfo>.from(widget.pipelineStrategies);
      final strategy = newPipeline.removeAt(index);
      newPipeline.insert(newIndex, strategy);
      widget.onPipelineChanged(newPipeline);
      widget.pipelineService.updatePipeline(newPipeline);
    }
  }

  void _loadStrategyConfig(StrategyInfo strategy) {
    widget.onStrategySelected(strategy);
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: ThemeUtils.getCardDecoration(context),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildHeader(),
          const SizedBox(height: 10),
          _buildTools(),
          const SizedBox(height: 10),
          Expanded(
            child: _buildPipelineList(),
          ),
        ],
      ),
    );
  }

  Widget _buildHeader() {
    return Row(
      children: [
        Icon(Icons.account_tree, color: ThemeUtils.getPrimaryColor(context), size: 18),
        const SizedBox(width: 8),
        const Text(
          '流水线配置',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 15,
            color: Colors.black87,
          ),
        ),
        const SizedBox(width: 8),
        Tooltip(
          message: '配置文件处理的流水线步骤，按顺序执行',
          child: Icon(Icons.help_outline, color: ThemeUtils.getTextSecondaryColor(context), size: 16),
        ),
      ],
    );
  }

  Widget _buildTools() {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(4),
            border: Border.all(color: ThemeUtils.getBorderColor(context)),
          ),
          child: DropdownButton<StrategyInfo>(
            hint: const Text('选择功能...', style: TextStyle(fontSize: 12)),
            value: _selectedPipelineStrategy,
            items: widget.availableStrategies.map((strategy) {
              return DropdownMenuItem<StrategyInfo>(
                value: strategy,
                child: Text(strategy.name, style: const TextStyle(fontSize: 12)),
              );
            }).toList(),
            onChanged: (value) {
              setState(() {
                _selectedPipelineStrategy = value;
              });
            },
            style: const TextStyle(fontSize: 12),
            dropdownColor: Colors.white,
            underline: const SizedBox.shrink(),
            icon: Icon(Icons.arrow_drop_down, color: ThemeUtils.getTextSecondaryColor(context), size: 20),
            isDense: true,
          ),
        ),
        const SizedBox(width: 8),
        ElevatedButton.icon(
          onPressed: _addStrategyStep,
          icon: const Icon(Icons.add, size: 16),
          label: const Text('添加步骤', style: TextStyle(fontSize: 12)),
          style: ElevatedButton.styleFrom(
            backgroundColor: ThemeUtils.getPrimaryColor(context),
            foregroundColor: Colors.white,
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(4),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildPipelineList() {
    if (widget.pipelineStrategies.isEmpty) {
      return Container(
        alignment: Alignment.center,
        child: Text(
          '暂无流水线步骤',
          style: TextStyle(
            color: ThemeUtils.getTextSecondaryColor(context),
            fontSize: 13,
          ),
        ),
      );
    }

    return ListView.builder(
      itemCount: widget.pipelineStrategies.length,
      itemBuilder: (context, index) {
        final strategy = widget.pipelineStrategies[index];
        final isSelected = widget.selectedStrategy != null && 
                          (widget.selectedStrategy?.pipelineId == strategy.pipelineId || 
                          (widget.selectedStrategy?.pipelineId == null && widget.selectedStrategy?.id == strategy.id));
        return PipelineListItem(
          strategy: strategy,
          index: index,
          isSelected: isSelected,
          onTap: () => _loadStrategyConfig(strategy),
          onMoveUp: index > 0 ? () => _moveStrategy(index, -1) : () {},
          onMoveDown: index < widget.pipelineStrategies.length - 1 ? () => _moveStrategy(index, 1) : () {},
          onDelete: () => _removeStrategy(strategy),
        );
      },
    );
  }
}

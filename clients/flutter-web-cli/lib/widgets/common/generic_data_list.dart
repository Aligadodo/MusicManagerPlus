import 'package:flutter/material.dart';
import '../../models/task_record.dart';
import 'column_config.dart';

/// 数据加载回调
/// 返回分页响应数据
typedef DataLoadCallback = Future<PaginatedResponse<TaskRecord>> Function(PaginationParams params);

/// 行选择回调
/// [record] 选中的记录
/// [selected] 是否选中
/// [isShiftClick] 是否按住Shift键点击
typedef RowSelectCallback = void Function(TaskRecord record, bool selected, bool isShiftClick);

/// 行双击回调
/// [record] 双击的记录
typedef RowDoubleTapCallback = void Function(TaskRecord record);

/// 行右键回调
/// [record] 右键的记录
/// [position] 右键点击位置
typedef RowContextMenuCallback = void Function(TaskRecord record, Offset position);

/// 通用数据列表组件
/// 支持分页、搜索、排序、筛选、列自定义等功能
class GenericDataList extends StatefulWidget {
  // 列配置列表
  final List<ColumnConfig> columns;
  
  // 数据加载回调
  final DataLoadCallback onLoadData;
  
  // 是否支持多选
  final bool multiSelect;
  
  // 是否支持行选择
  final bool enableRowSelection;
  
  // 行选择回调
  final RowSelectCallback? onRowSelect;
  
  // 行双击回调
  final RowDoubleTapCallback? onRowDoubleTap;
  
  // 行右键回调
  final RowContextMenuCallback? onRowContextMenu;
  
  // 空数据提示
  final Widget? emptyWidget;
  
  // 加载中提示
  final Widget? loadingWidget;
  
  // 标题
  final String? title;
  
  // 是否显示搜索框
  final bool showSearch;
  
  // 是否显示分页
  final bool showPagination;
  
  // 默认页大小
  final int defaultPageSize;
  
  // 页大小选项
  final List<int> pageSizeOptions;
  
  // 是否显示列设置按钮
  final bool showColumnSettings;
  
  // 是否显示刷新按钮
  final bool showRefresh;
  
  // 自定义工具栏
  final List<Widget>? toolbarActions;
  
  // 行高
  final double rowHeight;
  
  // 表头高度
  final double headerHeight;

  const GenericDataList({
    Key? key,
    required this.columns,
    required this.onLoadData,
    this.multiSelect = false,
    this.enableRowSelection = true,
    this.onRowSelect,
    this.onRowDoubleTap,
    this.onRowContextMenu,
    this.emptyWidget,
    this.loadingWidget,
    this.title,
    this.showSearch = true,
    this.showPagination = true,
    this.defaultPageSize = 20,
    this.pageSizeOptions = const [10, 20, 50, 100],
    this.showColumnSettings = true,
    this.showRefresh = true,
    this.toolbarActions,
    this.rowHeight = 48,
    this.headerHeight = 48,
  }) : super(key: key);

  @override
  State<GenericDataList> createState() => _GenericDataListState();
}

class _GenericDataListState extends State<GenericDataList> {
  // 数据状态
  List<TaskRecord> _data = [];
  int _total = 0;
  int _currentPage = 1;
  int _pageSize = 20;
  bool _isLoading = false;
  String? _errorMessage;

  // 分页状态
  int get _totalPages => (_total / _pageSize).ceil();

  // 选择状态
  final Set<String> _selectedIds = {};
  String? _lastSelectedId;

  // 搜索和排序状态
  String? _searchKeyword;
  String? _sortField;
  String _sortOrder = 'asc';

  // 列配置状态
  late List<ColumnConfig> _visibleColumns;

  @override
  void initState() {
    super.initState();
    _pageSize = widget.defaultPageSize;
    _visibleColumns = widget.columns.where((c) => c.visible).toList();
    _loadData();
  }

  @override
  void didUpdateWidget(GenericDataList oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.columns != widget.columns) {
      _visibleColumns = widget.columns.where((c) => c.visible).toList();
    }
  }

  /// 加载数据
  Future<void> _loadData() async {
    if (_isLoading) return;

    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final params = PaginationParams(
        page: _currentPage,
        pageSize: _pageSize,
        search: _searchKeyword,
        sortField: _sortField,
        sortOrder: _sortOrder,
      );

      final response = await widget.onLoadData(params);

      setState(() {
        _data = response.list;
        _total = response.total;
        _currentPage = response.page;
        _pageSize = response.pageSize;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _errorMessage = '加载数据失败: $e';
        _isLoading = false;
      });
    }
  }

  /// 刷新数据
  Future<void> refresh() async {
    await _loadData();
  }

  /// 跳转到指定页
  void _goToPage(int page) {
    if (page < 1 || page > _totalPages) return;
    setState(() {
      _currentPage = page;
    });
    _loadData();
  }

  /// 改变页大小
  void _changePageSize(int size) {
    setState(() {
      _pageSize = size;
      _currentPage = 1;
    });
    _loadData();
  }

  /// 处理搜索
  void _handleSearch(String keyword) {
    setState(() {
      _searchKeyword = keyword.isEmpty ? null : keyword;
      _currentPage = 1;
    });
    _loadData();
  }

  /// 处理排序
  void _handleSort(String field) {
    setState(() {
      if (_sortField == field) {
        _sortOrder = _sortOrder == 'asc' ? 'desc' : 'asc';
      } else {
        _sortField = field;
        _sortOrder = 'asc';
      }
      _currentPage = 1;
    });
    _loadData();
  }

  /// 处理行选择
  void _handleRowSelect(TaskRecord record, bool selected, bool isShiftClick) {
    if (!widget.enableRowSelection) return;

    setState(() {
      if (widget.multiSelect) {
        if (isShiftClick && _lastSelectedId != null) {
          // Shift+点击：选择范围内的所有行
          final lastIndex = _data.indexWhere((r) => r.id == _lastSelectedId);
          final currentIndex = _data.indexWhere((r) => r.id == record.id);
          
          if (lastIndex != -1 && currentIndex != -1) {
            final start = lastIndex < currentIndex ? lastIndex : currentIndex;
            final end = lastIndex < currentIndex ? currentIndex : lastIndex;
            
            for (int i = start; i <= end; i++) {
              if (selected) {
                _selectedIds.add(_data[i].id);
              } else {
                _selectedIds.remove(_data[i].id);
              }
            }
          }
        } else {
          // 普通点击：切换选择状态
          if (selected) {
            _selectedIds.add(record.id);
          } else {
            _selectedIds.remove(record.id);
          }
        }
      } else {
        // 单选模式
        _selectedIds.clear();
        if (selected) {
          _selectedIds.add(record.id);
        }
      }

      _lastSelectedId = record.id;
    });

    widget.onRowSelect?.call(record, selected, isShiftClick);
  }

  /// 全选/取消全选
  void _toggleSelectAll(bool selected) {
    setState(() {
      if (selected) {
        _selectedIds.addAll(_data.map((r) => r.id));
      } else {
        _selectedIds.clear();
      }
    });
  }

  /// 获取选中的记录
  List<TaskRecord> getSelectedRecords() {
    return _data.where((r) => _selectedIds.contains(r.id)).toList();
  }

  /// 清空选择
  void clearSelection() {
    setState(() {
      _selectedIds.clear();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // 工具栏
        _buildToolbar(),
        
        // 表格内容
        Expanded(
          child: _buildTableContent(),
        ),
        
        // 分页栏
        if (widget.showPagination) _buildPagination(),
      ],
    );
  }

  /// 构建工具栏
  Widget _buildToolbar() {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        border: Border(
          bottom: BorderSide(color: Colors.grey.shade300),
        ),
      ),
      child: Row(
        children: [
          // 标题
          if (widget.title != null)
            Padding(
              padding: const EdgeInsets.only(right: 16),
              child: Text(
                widget.title!,
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          
          // 搜索框
          if (widget.showSearch)
            Expanded(
              child: TextField(
                decoration: InputDecoration(
                  hintText: '搜索...',
                  prefixIcon: const Icon(Icons.search),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(4),
                  ),
                  contentPadding: const EdgeInsets.symmetric(horizontal: 12),
                ),
                onSubmitted: _handleSearch,
              ),
            ),
          
          const SizedBox(width: 12),
          
          // 自定义操作按钮
          if (widget.toolbarActions != null) ...widget.toolbarActions!,
          
          // 列设置按钮
          if (widget.showColumnSettings)
            IconButton(
              icon: const Icon(Icons.view_column),
              tooltip: '列设置',
              onPressed: _showColumnSettings,
            ),
          
          // 刷新按钮
          if (widget.showRefresh)
            IconButton(
              icon: const Icon(Icons.refresh),
              tooltip: '刷新',
              onPressed: refresh,
            ),
        ],
      ),
    );
  }

  /// 构建表格内容
  Widget _buildTableContent() {
    if (_isLoading) {
      return widget.loadingWidget ??
          const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.error_outline, size: 48, color: Colors.red),
            const SizedBox(height: 16),
            Text(_errorMessage!, style: const TextStyle(color: Colors.red)),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: refresh,
              child: const Text('重试'),
            ),
          ],
        ),
      );
    }

    if (_data.isEmpty) {
      return widget.emptyWidget ??
          const Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.inbox, size: 48, color: Colors.grey),
                SizedBox(height: 16),
                Text('暂无数据', style: TextStyle(color: Colors.grey)),
              ],
            ),
          );
    }

    return LayoutBuilder(
      builder: (context, constraints) {
        return SingleChildScrollView(
          scrollDirection: Axis.vertical,
          child: SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: ConstrainedBox(
              constraints: BoxConstraints(minWidth: constraints.maxWidth),
              child: DataTable(
                columns: _buildDataColumns(),
                rows: _buildDataRows(),
                headingRowHeight: widget.headerHeight,
                dataRowHeight: widget.rowHeight,
                showCheckboxColumn: widget.multiSelect,
              ),
            ),
          ),
        );
      },
    );
  }

  /// 构建数据列
  List<DataColumn> _buildDataColumns() {
    final columns = <DataColumn>[];

    // 全选列
    if (widget.multiSelect) {
      final allSelected = _data.isNotEmpty && _data.every((r) => _selectedIds.contains(r.id));
      columns.add(
        DataColumn(
          label: Checkbox(
            value: allSelected,
            onChanged: (value) => _toggleSelectAll(value ?? false),
          ),
        ),
      );
    }

    // 数据列
    for (final column in _visibleColumns) {
      columns.add(
        DataColumn(
          label: InkWell(
            onTap: column.sortable ? () => _handleSort(column.key) : null,
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(column.title),
                if (column.sortable)
                  Icon(
                    _sortField == column.key
                        ? (_sortOrder == 'asc' ? Icons.arrow_upward : Icons.arrow_downward)
                        : Icons.unfold_more,
                    size: 16,
                  ),
              ],
            ),
          ),
        ),
      );
    }

    return columns;
  }

  /// 构建数据行
  List<DataRow> _buildDataRows() {
    return _data.map((record) {
      final isSelected = _selectedIds.contains(record.id);

      return DataRow(
        selected: isSelected,
        onSelectChanged: widget.enableRowSelection
            ? (selected) => _handleRowSelect(record, selected ?? false, false)
            : null,
        cells: _buildDataCells(record),
      );
    }).toList();
  }

  /// 构建数据单元格
  List<DataCell> _buildDataCells(TaskRecord record) {
    final cells = <DataCell>[];

    // 选择列已在 DataRow.onSelectChanged 中处理

    // 数据单元格
    for (final column in _visibleColumns) {
      final value = _getFieldValue(record, column.key);
      
      Widget cellContent;
      if (column.customRender != null) {
        cellContent = column.customRender!(value, record.toJson());
      } else if (column.formatter != null) {
        cellContent = Text(column.formatter!(value));
      } else {
        cellContent = Text(value?.toString() ?? '-');
      }

      cells.add(
        DataCell(
          GestureDetector(
            onSecondaryTapDown: (details) {
              if (widget.onRowContextMenu != null) {
                widget.onRowContextMenu!(record, details.globalPosition);
              }
            },
            onDoubleTap: () {
              if (widget.onRowDoubleTap != null) {
                widget.onRowDoubleTap!(record);
              }
            },
            child: Container(
              alignment: column.alignment,
              child: cellContent,
            ),
          ),
        ),
      );
    }

    return cells;
  }

  /// 获取字段值
  dynamic _getFieldValue(TaskRecord record, String field) {
    switch (field) {
      case 'id':
        return record.id;
      case 'originalName':
        return record.originalName;
      case 'newName':
        return record.newName;
      case 'originalPath':
        return record.originalPath;
      case 'newPath':
        return record.newPath;
      case 'fileSize':
        return record.fileSize;
      case 'fileType':
        return record.fileType;
      case 'lastModified':
        return record.lastModified;
      case 'metadata':
        return record.metadata;
      case 'operationType':
        return record.operationType;
      case 'status':
        return record.status;
      case 'reason':
        return record.reason;
      case 'failReason':
        return record.failReason;
      case 'extraParams':
        return record.extraParams;
      case 'changed':
        return record.changed;
      case 'isCreate':
        return record.isCreate;
      case 'isDeleteOrMove':
        return record.isDeleteOrMove;
      case 'selected':
        return record.selected;
      case 'analyzeTime':
        return record.analyzeTime;
      case 'executeTime':
        return record.executeTime;
      case 'duration':
        return record.duration;
      case 'retryCount':
        return record.retryCount;
      case 'processInfo':
        return record.processInfo;
      default:
        return null;
    }
  }

  /// 构建分页栏
  Widget _buildPagination() {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        border: Border(
          top: BorderSide(color: Colors.grey.shade300),
        ),
      ),
      child: Row(
        children: [
          // 统计信息
          Text('共 $_total 条记录，$_totalPages 页'),
          
          const Spacer(),
          
          // 页大小选择
          DropdownButton<int>(
            value: _pageSize,
            items: widget.pageSizeOptions.map((size) {
              return DropdownMenuItem(
                value: size,
                child: Text('$size 条/页'),
              );
            }).toList(),
            onChanged: (value) {
              if (value != null) _changePageSize(value);
            },
          ),
          
          const SizedBox(width: 16),
          
          // 分页按钮
          IconButton(
            icon: const Icon(Icons.first_page),
            onPressed: _currentPage > 1 ? () => _goToPage(1) : null,
          ),
          IconButton(
            icon: const Icon(Icons.chevron_left),
            onPressed: _currentPage > 1 ? () => _goToPage(_currentPage - 1) : null,
          ),
          
          // 页码输入
          SizedBox(
            width: 60,
            child: TextField(
              textAlign: TextAlign.center,
              decoration: const InputDecoration(
                contentPadding: EdgeInsets.symmetric(horizontal: 8),
              ),
              controller: TextEditingController(text: _currentPage.toString()),
              onSubmitted: (value) {
                final page = int.tryParse(value);
                if (page != null) _goToPage(page);
              },
            ),
          ),
          Text(' / $_totalPages'),
          
          IconButton(
            icon: const Icon(Icons.chevron_right),
            onPressed: _currentPage < _totalPages ? () => _goToPage(_currentPage + 1) : null,
          ),
          IconButton(
            icon: const Icon(Icons.last_page),
            onPressed: _currentPage < _totalPages ? () => _goToPage(_totalPages) : null,
          ),
        ],
      ),
    );
  }

  /// 显示列设置对话框
  void _showColumnSettings() {
    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text('列设置'),
          content: SizedBox(
            width: 300,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: widget.columns.map((column) {
                final isVisible = _visibleColumns.any((c) => c.key == column.key);
                return CheckboxListTile(
                  title: Text(column.title),
                  value: isVisible,
                  onChanged: column.hideable
                      ? (value) {
                          setState(() {
                            if (value == true) {
                              _visibleColumns.add(column);
                              _visibleColumns.sort((a, b) {
                                final indexA = widget.columns.indexWhere((c) => c.key == a.key);
                                final indexB = widget.columns.indexWhere((c) => c.key == b.key);
                                return indexA.compareTo(indexB);
                              });
                            } else {
                              _visibleColumns.removeWhere((c) => c.key == column.key);
                            }
                          });
                        }
                      : null,
                );
              }).toList(),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('确定'),
            ),
          ],
        );
      },
    );
  }
}

import 'package:flutter/material.dart';
import '../api/api_client.dart';
import '../api/plugin_service.dart';
import '../models/plugin_info.dart';
import 'plugin_config.dart';

class PluginListPage extends StatefulWidget {
  const PluginListPage({super.key});

  @override
  State<PluginListPage> createState() => _PluginListPageState();
}

class _PluginListPageState extends State<PluginListPage> {
  final PluginService _pluginService = PluginService(ApiClient());
  List<PluginInfo> _plugins = [];
  bool _isLoading = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadPlugins();
  }

  Future<void> _loadPlugins() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final plugins = await _pluginService.getPlugins();
      setState(() {
        _plugins = plugins;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = '加载插件失败: $e';
        _isLoading = false;
      });
    }
  }

  void _navigateToPluginConfig(PluginInfo plugin) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => PluginConfigPage(pluginId: plugin.id, pluginName: plugin.name),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('插件管理'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            if (_isLoading)
              const Center(
                child: CircularProgressIndicator(),
              )
            else if (_error != null)
              Center(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(_error!, style: const TextStyle(color: Colors.red)),
                    ElevatedButton(
                      onPressed: _loadPlugins,
                      child: const Text('重试'),
                    ),
                  ],
                ),
              )
            else
              Expanded(
                child: GridView.builder(
                  gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 2,
                    crossAxisSpacing: 16.0,
                    mainAxisSpacing: 16.0,
                    childAspectRatio: 1.5,
                  ),
                  itemCount: _plugins.length,
                  itemBuilder: (context, index) {
                    final plugin = _plugins[index];
                    return Card(
                      elevation: 3,
                      child: InkWell(
                        onTap: () => _navigateToPluginConfig(plugin),
                        child: Padding(
                          padding: const EdgeInsets.all(16.0),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                plugin.name,
                                style: const TextStyle(
                                  fontSize: 18,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                              const SizedBox(height: 8),
                              Text(
                                plugin.description,
                                style: const TextStyle(
                                  fontSize: 14,
                                  color: Colors.grey,
                                ),
                                maxLines: 3,
                                overflow: TextOverflow.ellipsis,
                              ),
                              const Spacer(),
                              Row(
                                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                children: [
                                  Text(
                                    '版本: ${plugin.version}',
                                    style: const TextStyle(
                                      fontSize: 12,
                                      color: Colors.blue,
                                    ),
                                  ),
                                  const Icon(
                                    Icons.arrow_forward,
                                    color: Colors.blue,
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),
                      ),
                    );
                  },
                ),
              ),
          ],
        ),
      ),
    );
  }
}

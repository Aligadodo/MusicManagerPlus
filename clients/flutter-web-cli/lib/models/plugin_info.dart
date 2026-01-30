class PluginInfo {
  final String id;
  final String name;
  final String description;
  final String version;

  PluginInfo({
    required this.id,
    required this.name,
    required this.description,
    required this.version,
  });

  factory PluginInfo.fromJson(Map<String, dynamic> json) {
    return PluginInfo(
      id: json['id'] ?? '',
      name: json['name'] ?? '',
      description: json['description'] ?? '',
      version: json['version'] ?? '',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'description': description,
      'version': version,
    };
  }
}

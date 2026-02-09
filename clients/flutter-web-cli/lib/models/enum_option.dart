class EnumOption {
  final String code;
  final String nameZh;
  final String nameEn;
  final String descriptionZh;
  final String descriptionEn;
  final dynamic value;

  EnumOption({
    required this.code,
    required this.nameZh,
    required this.nameEn,
    required this.descriptionZh,
    required this.descriptionEn,
    this.value,
  });

  factory EnumOption.fromJson(Map<String, dynamic> json) {
    return EnumOption(
      code: json['code'] as String? ?? json['value'] as String? ?? '',
      nameZh: json['nameZh'] as String? ?? json['label'] as String? ?? '',
      nameEn: json['nameEn'] as String? ?? '',
      descriptionZh: json['descriptionZh'] as String? ?? '',
      descriptionEn: json['descriptionEn'] as String? ?? '',
      value: json['value'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'code': code,
      'nameZh': nameZh,
      'nameEn': nameEn,
      'descriptionZh': descriptionZh,
      'descriptionEn': descriptionEn,
      'value': value,
    };
  }

  String get displayName => nameZh;
  String get displayDescription => descriptionZh;
}
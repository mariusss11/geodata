class MapItem {
  final int id;
  final String name;
  final int year;
  final bool isEnabled;
  final String availabilityStatus;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  MapItem({
    required this.id,
    required this.name,
    required this.year,
    required this.isEnabled,
    required this.availabilityStatus,
    this.createdAt,
    this.updatedAt,
  });

  bool get isAvailable => availabilityStatus.toUpperCase() == 'AVAILABLE';
  bool get isBorrowed => availabilityStatus.toUpperCase() == 'BORROWED';

  factory MapItem.fromJson(Map<String, dynamic> json) {
    return MapItem(
      id: (json['id'] ?? json['mapId']) as int,
      name: json['name'] as String? ?? '',
      year: (json['year'] as num?)?.toInt() ?? 0,
      isEnabled: json['isEnabled'] as bool? ?? json['enabled'] as bool? ?? true,
      availabilityStatus: json['availabilityStatus'] as String? ?? 'AVAILABLE',
      createdAt: _parseDate(json['createdAt']),
      updatedAt: _parseDate(json['updatedAt']),
    );
  }

  static DateTime? _parseDate(dynamic v) {
    if (v == null) return null;
    if (v is String) return DateTime.tryParse(v);
    return null;
  }
}

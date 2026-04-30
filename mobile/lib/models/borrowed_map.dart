class BorrowedMap {
  final int borrowId;
  final int mapId;
  final int userId;
  final String name;
  final int year;
  final String availabilityStatus;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  BorrowedMap({
    required this.borrowId,
    required this.mapId,
    required this.userId,
    required this.name,
    required this.year,
    required this.availabilityStatus,
    this.createdAt,
    this.updatedAt,
  });

  factory BorrowedMap.fromJson(Map<String, dynamic> json) {
    return BorrowedMap(
      borrowId: (json['borrowId'] as num?)?.toInt() ?? 0,
      mapId: (json['mapId'] as num?)?.toInt() ?? 0,
      userId: (json['userId'] as num?)?.toInt() ?? 0,
      name: json['name'] as String? ?? '',
      year: (json['year'] as num?)?.toInt() ?? 0,
      availabilityStatus: json['availabilityStatus'] as String? ?? '',
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

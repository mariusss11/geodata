import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/models/borrowed_map.dart';

void main() {
  group('BorrowedMap Model', () {
    test('fromJson creates BorrowedMap with valid data', () {
      final json = {
        'borrowId': 1,
        'mapId': 100,
        'userId': 5,
        'name': 'Test Map',
        'year': 2020,
        'availabilityStatus': 'BORROWED',
        'createdAt': '2024-01-10T10:00:00Z',
      };

      final borrowed = BorrowedMap.fromJson(json);

      expect(borrowed.borrowId, 1);
      expect(borrowed.mapId, 100);
      expect(borrowed.userId, 5);
      expect(borrowed.name, 'Test Map');
      expect(borrowed.year, 2020);
      expect(borrowed.availabilityStatus, 'BORROWED');
    });

    test('fromJson handles returned maps with dates', () {
      final json = {
        'borrowId': 2,
        'mapId': 200,
        'userId': 10,
        'name': 'Returned Map',
        'year': 2019,
        'availabilityStatus': 'AVAILABLE',
        'createdAt': '2024-01-01T10:00:00Z',
        'updatedAt': '2024-01-12T14:30:00Z',
      };

      final borrowed = BorrowedMap.fromJson(json);

      expect(borrowed.availabilityStatus, 'AVAILABLE');
      expect(borrowed.updatedAt, isNotNull);
      expect(borrowed.createdAt, isNotNull);
    });

    test('fromJson provides defaults for missing fields', () {
      final json = {
        'borrowId': 3,
        'mapId': 300,
        'userId': 15,
        'name': 'Minimal Map',
        'year': 2021,
        'availabilityStatus': 'BORROWED',
      };

      final borrowed = BorrowedMap.fromJson(json);

      expect(borrowed.borrowId, 3);
      expect(borrowed.name, 'Minimal Map');
      expect(borrowed.createdAt, isNull);
      expect(borrowed.updatedAt, isNull);
    });

    test('fromJson handles numeric year values', () {
      final json = {
        'borrowId': 4,
        'mapId': 400,
        'userId': 20,
        'name': 'Year Map',
        'year': 1995,
        'availabilityStatus': 'AVAILABLE',
      };

      final borrowed = BorrowedMap.fromJson(json);

      expect(borrowed.year, 1995);
    });

    test('fromJson converts null dates correctly', () {
      final json = {
        'borrowId': 5,
        'mapId': 500,
        'userId': 25,
        'name': 'No Dates Map',
        'year': 2023,
        'availabilityStatus': 'BORROWED',
        'createdAt': null,
        'updatedAt': null,
      };

      final borrowed = BorrowedMap.fromJson(json);

      expect(borrowed.createdAt, isNull);
      expect(borrowed.updatedAt, isNull);
    });

    test('Constructor creates BorrowedMap with all fields', () {
      final now = DateTime.now();
      final borrowed = BorrowedMap(
        borrowId: 10,
        mapId: 1000,
        userId: 50,
        name: 'Constructor Map',
        year: 2024,
        availabilityStatus: 'BORROWED',
        createdAt: now,
        updatedAt: now,
      );

      expect(borrowed.borrowId, 10);
      expect(borrowed.mapId, 1000);
      expect(borrowed.name, 'Constructor Map');
      expect(borrowed.createdAt, now);
    });

    test('fromJson handles missing optional dates', () {
      final json = {
        'borrowId': 6,
        'mapId': 600,
        'userId': 30,
        'name': 'Map Without Dates',
        'year': 2020,
        'availabilityStatus': 'AVAILABLE',
      };

      final borrowed = BorrowedMap.fromJson(json);

      expect(borrowed.createdAt, isNull);
      expect(borrowed.updatedAt, isNull);
      expect(borrowed.name, 'Map Without Dates');
    });
  });
}

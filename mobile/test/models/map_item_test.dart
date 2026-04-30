import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/models/map_item.dart';

void main() {
  group('MapItem Model', () {
    test('fromJson creates MapItem with valid data', () {
      final json = {
        'id': 1,
        'name': 'Test Map',
        'year': 2020,
        'isEnabled': true,
        'availabilityStatus': 'AVAILABLE',
        'createdAt': '2024-01-01T10:00:00Z',
      };

      final map = MapItem.fromJson(json);

      expect(map.id, 1);
      expect(map.name, 'Test Map');
      expect(map.year, 2020);
      expect(map.isEnabled, true);
      expect(map.availabilityStatus, 'AVAILABLE');
    });

    test('fromJson handles mapId instead of id', () {
      final json = {
        'mapId': 2,
        'name': 'Europe Map',
        'year': 2021,
        'isEnabled': true,
        'availabilityStatus': 'BORROWED',
      };

      final map = MapItem.fromJson(json);

      expect(map.id, 2);
      expect(map.availabilityStatus, 'BORROWED');
    });

    test('fromJson provides defaults for optional fields', () {
      final json = {
        'id': 3,
        'name': 'Minimal Map',
      };

      final map = MapItem.fromJson(json);

      expect(map.id, 3);
      expect(map.name, 'Minimal Map');
      expect(map.year, 0);
      expect(map.isEnabled, true);
      expect(map.availabilityStatus, 'AVAILABLE');
    });

    test('fromJson handles enabled field as fallback', () {
      final json = {
        'id': 4,
        'name': 'Old Format Map',
        'enabled': false,
      };

      final map = MapItem.fromJson(json);

      expect(map.isEnabled, false);
    });

    test('isAvailable getter works correctly', () {
      final mapAvailable = MapItem(
        id: 5,
        name: 'Available',
        year: 2022,
        isEnabled: true,
        availabilityStatus: 'AVAILABLE',
      );

      final mapBorrowed = MapItem(
        id: 6,
        name: 'Borrowed',
        year: 2022,
        isEnabled: true,
        availabilityStatus: 'BORROWED',
      );

      expect(mapAvailable.isAvailable, true);
      expect(mapBorrowed.isAvailable, false);
    });

    test('isBorrowed getter works correctly', () {
      final mapAvailable = MapItem(
        id: 7,
        name: 'Available',
        year: 2022,
        isEnabled: true,
        availabilityStatus: 'AVAILABLE',
      );

      final mapBorrowed = MapItem(
        id: 8,
        name: 'Borrowed',
        year: 2022,
        isEnabled: true,
        availabilityStatus: 'BORROWED',
      );

      expect(mapBorrowed.isBorrowed, true);
      expect(mapAvailable.isBorrowed, false);
    });

    test('fromJson handles dates correctly', () {
      final json = {
        'id': 9,
        'name': 'Dated Map',
        'year': 2023,
        'isEnabled': true,
        'availabilityStatus': 'AVAILABLE',
        'createdAt': '2024-01-15T14:30:00Z',
        'updatedAt': '2024-01-20T10:00:00Z',
      };

      final map = MapItem.fromJson(json);

      expect(map.createdAt, isNotNull);
      expect(map.updatedAt, isNotNull);
      expect(map.createdAt?.year, 2024);
    });

    test('fromJson handles null dates gracefully', () {
      final json = {
        'id': 10,
        'name': 'No Dates',
        'year': 2024,
        'isEnabled': true,
        'availabilityStatus': 'AVAILABLE',
        'createdAt': null,
      };

      final map = MapItem.fromJson(json);

      expect(map.createdAt, isNull);
    });

    test('isEnabled defaults to true when not specified', () {
      final json = {
        'id': 11,
        'name': 'Default Enabled',
        'year': 2024,
      };

      final map = MapItem.fromJson(json);

      expect(map.isEnabled, true);
    });

    test('Constructor creates MapItem with all fields', () {
      final map = MapItem(
        id: 100,
        name: 'Constructor Map',
        year: 2024,
        isEnabled: true,
        availabilityStatus: 'AVAILABLE',
      );

      expect(map.id, 100);
      expect(map.name, 'Constructor Map');
      expect(map.isAvailable, true);
    });
  });
}

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/models/paged_response.dart';

void main() {
  group('PagedResponse Model', () {
    test('fromJson creates PagedResponse with valid data', () {
      final json = {
        'content': [
          {'id': '1', 'title': 'Item 1'},
          {'id': '2', 'title': 'Item 2'},
        ],
        'page': 0,
        'size': 2,
        'totalElements': 10,
        'totalPages': 5,
        'hasNext': true,
        'hasPrevious': false,
        'first': true,
        'last': false,
      };

      final response = PagedResponse.fromJson(json, (item) => item as Map);

      expect(response.content, hasLength(2));
      expect(response.hasNext, true);
      expect(response.hasPrevious, false);
      expect(response.page, 0);
      expect(response.size, 2);
      expect(response.totalElements, 10);
      expect(response.totalPages, 5);
      expect(response.first, true);
      expect(response.last, false);
    });

    test('fromJson handles empty content', () {
      final json = {
        'content': [],
        'page': 5,
        'size': 20,
        'totalElements': 100,
        'totalPages': 5,
        'hasNext': false,
        'hasPrevious': true,
        'first': false,
        'last': true,
      };

      final response = PagedResponse.fromJson(json, (item) => item as Map);

      expect(response.content, isEmpty);
      expect(response.hasNext, false);
      expect(response.last, true);
    });

    test('fromJson handles last page', () {
      final json = {
        'content': [
          {'id': '1'},
        ],
        'page': 4,
        'size': 20,
        'totalElements': 95,
        'totalPages': 5,
        'hasNext': false,
        'hasPrevious': true,
        'first': false,
        'last': true,
      };

      final response = PagedResponse.fromJson(json, (item) => item as Map);

      expect(response.hasNext, false);
      expect(response.page, 4);
      expect(response.last, true);
    });

    test('Constructor creates PagedResponse with all fields', () {
      final response = PagedResponse(
        content: [
          {'id': '1', 'name': 'Item 1'},
          {'id': '2', 'name': 'Item 2'},
        ],
        page: 0,
        size: 2,
        totalElements: 50,
        totalPages: 25,
        hasNext: true,
        hasPrevious: false,
        first: true,
        last: false,
      );

      expect(response.content, hasLength(2));
      expect(response.hasNext, true);
      expect(response.totalElements, 50);
    });

    test('first property is true for first page', () {
      final response = PagedResponse(
        content: [],
        page: 0,
        size: 20,
        totalElements: 100,
        totalPages: 5,
        hasNext: true,
        hasPrevious: false,
        first: true,
        last: false,
      );

      expect(response.first, true);
    });

    test('first property is false for other pages', () {
      final response = PagedResponse(
        content: [],
        page: 3,
        size: 20,
        totalElements: 100,
        totalPages: 5,
        hasNext: false,
        hasPrevious: true,
        first: false,
        last: false,
      );

      expect(response.first, false);
    });

    test('last property correctly identifies last page', () {
      final response = PagedResponse(
        content: [],
        page: 4,
        size: 20,
        totalElements: 100,
        totalPages: 5,
        hasNext: false,
        hasPrevious: true,
        first: false,
        last: true,
      );

      expect(response.last, true);
    });

    test('fromJson provides defaults for missing optional fields', () {
      final json = {
        'content': [],
        'page': 0,
        'size': 20,
        'totalElements': 0,
        'totalPages': 0,
      };

      final response = PagedResponse.fromJson(json, (item) => item as Map);

      expect(response.first, true); // Defaults to true
      expect(response.last, true); // Defaults to true
      expect(response.hasNext, false); // Defaults to false
      expect(response.hasPrevious, false); // Defaults to false
    });
  });
}

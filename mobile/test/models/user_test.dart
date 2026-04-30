import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/models/user.dart';

void main() {
  group('User Model', () {
    test('fromJson creates User with valid data', () {
      final json = {
        'userId': 123,
        'username': 'testuser',
        'name': 'Test User',
        'role': 'USER',
      };

      final user = User.fromJson(json);

      expect(user.userId, 123);
      expect(user.username, 'testuser');
      expect(user.name, 'Test User');
      expect(user.role, 'USER');
    });

    test('fromJson handles missing optional fields', () {
      final json = {
        'userId': 456,
        'username': 'newuser',
        'name': 'New User',
      };

      final user = User.fromJson(json);

      expect(user.userId, 456);
      expect(user.username, 'newuser');
      expect(user.name, 'New User');
      expect(user.role, isNull);
    });

    test('toJson converts User to JSON', () {
      final user = User(
        userId: 789,
        username: 'admin',
        name: 'Admin User',
        role: 'ADMIN',
      );

      final json = user.toJson();

      expect(json['userId'], 789);
      expect(json['username'], 'admin');
      expect(json['name'], 'Admin User');
      expect(json['role'], 'ADMIN');
    });

    test('fromJson with null userId creates User', () {
      final json = {
        'username': 'minimal',
        'name': 'Minimal User',
      };

      final user = User.fromJson(json);

      expect(user.userId, isNull);
      expect(user.username, 'minimal');
      expect(user.name, 'Minimal User');
    });

    test('User with default values constructs correctly', () {
      final user = User(
        username: 'testuser',
        name: 'Test User',
      );

      expect(user.username, 'testuser');
      expect(user.name, 'Test User');
      expect(user.userId, isNull);
      expect(user.role, isNull);
    });

    test('fromJson handles empty strings gracefully', () {
      final json = {
        'userId': 100,
        'username': '',
        'name': '',
      };

      final user = User.fromJson(json);

      expect(user.username, isEmpty);
      expect(user.name, isEmpty);
    });
  });
}

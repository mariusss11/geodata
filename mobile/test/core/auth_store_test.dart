import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/auth_store.dart';
import 'package:mobile/models/user.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  group('AuthStore', () {
    setUp(() {
      SharedPreferences.setMockInitialValues({});
    });

    test('initial state has no token or user', () async {
      final authStore = AuthStore();
      await authStore.load();

      expect(authStore.token, isNull);
      expect(authStore.user, isNull);
      expect(authStore.isAuthenticated, false);
    });

    test('save stores token and user', () async {
      final authStore = AuthStore();
      await authStore.load();

      const token = 'test-token-123';
      final user = User(
        userId: 1,
        username: 'testuser',
        name: 'Test User',
        role: 'USER',
      );

      await authStore.save(token: token, user: user);

      expect(authStore.token, token);
      expect(authStore.user, isNotNull);
      expect(authStore.user!.username, 'testuser');
      expect(authStore.isAuthenticated, true);
    });

    test('load retrieves stored token and user', () async {
      final authStore1 = AuthStore();
      const token = 'persisted-token';
      final user = User(
        userId: 2,
        username: 'persistuser',
        name: 'Persisted User',
        role: 'LIBRARIAN',
      );
      await authStore1.save(token: token, user: user);

      // Simulate app restart
      final authStore2 = AuthStore();
      await authStore2.load();

      expect(authStore2.token, token);
      expect(authStore2.user?.username, 'persistuser');
      expect(authStore2.isAuthenticated, true);
    });

    test('clear removes token and user', () async {
      final authStore = AuthStore();
      await authStore.load();

      const token = 'token-to-clear';
      final user = User(
        userId: 3,
        username: 'cleanuser',
        name: 'Clean User',
        role: 'USER',
      );
      await authStore.save(token: token, user: user);
      expect(authStore.isAuthenticated, true);

      await authStore.clear();

      expect(authStore.token, isNull);
      expect(authStore.user, isNull);
      expect(authStore.isAuthenticated, false);
    });

    test('updateUser updates stored user', () async {
      final authStore = AuthStore();
      await authStore.load();

      const token = 'token-123';
      final initialUser = User(
        userId: 4,
        username: 'user1',
        name: 'John Doe',
        role: 'USER',
      );
      await authStore.save(token: token, user: initialUser);

      final updatedUser = User(
        userId: 4,
        username: 'user1updated',
        name: 'Jane Smith',
        role: 'LIBRARIAN',
      );
      await authStore.updateUser(updatedUser);

      expect(authStore.user?.username, 'user1updated');
      expect(authStore.user?.name, 'Jane Smith');
      expect(authStore.token, token); // Token unchanged
    });

    test('isAuthenticated reflects auth state correctly', () async {
      final authStore = AuthStore();
      await authStore.load();

      expect(authStore.isAuthenticated, false);

      final user = User(
        userId: 5,
        username: 'authuser',
        name: 'Auth Test User',
        role: 'USER',
      );
      await authStore.save(token: 'token', user: user);

      expect(authStore.isAuthenticated, true);

      await authStore.clear();

      expect(authStore.isAuthenticated, false);
    });
  });
}

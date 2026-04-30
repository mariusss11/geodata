import '../core/api_client.dart';
import '../core/api_config.dart';
import '../core/auth_store.dart';
import '../models/user.dart';

class AuthService {
  final ApiClient client;
  final AuthStore store;

  AuthService(this.client, this.store);

  Future<User> login({required String username, required String password}) async {
    final body = {'username': username, 'password': password};
    final res = await client.post(ApiConfig.identityBase, '/api/auth/login', body: body);
    if (res is! Map<String, dynamic>) {
      throw ApiException(500, 'Unexpected login response');

    }
    final token = res['message'] as String?;
    final data = res['data'];
    if (token == null || data is! Map<String, dynamic>) {
      throw ApiException(401, res['message']?.toString() ?? 'Login failed');
    }
    final user = User.fromJson(data);
    await store.save(token: token, user: user);
    return user;
  }

  Future<void> register({
    required String username,
    required String name,
    required String password,
  }) async {
    await client.post(
      ApiConfig.identityBase,
      '/api/auth/register',
      body: {
        'username': username,
        'name': name,
        'password': password,
        'role': 'USER',
      },
    );
  }

  Future<User> updateProfile({required String name}) async {
    final res = await client.put(
      ApiConfig.identityBase,
      '/api/home/profile',
      body: {'name': name},
    );
    final updated = User.fromJson(res as Map<String, dynamic>);
    await store.updateUser(updated);
    return updated;
  }

  Future<void> changePassword({
    required String currentPassword,
    required String newPassword,
  }) async {
    await client.put(
      ApiConfig.identityBase,
      '/api/home/password',
      body: {
        'currentPassword': currentPassword,
        'newPassword': newPassword,
      },
    );
  }

  Future<void> logout() => store.clear();
}

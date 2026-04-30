import '../core/api_client.dart';
import '../core/api_config.dart';
import '../models/paged_response.dart';
import '../models/user.dart';

class UserService {
  final ApiClient client;

  UserService(this.client);

  Future<PagedResponse<User>> searchUsers({
    required int pageNumber,
    required int pageSize,
    String? search,
  }) async {
    final query = <String, dynamic>{
      'pageNumber': pageNumber,
      'pageSize': pageSize,
    };
    if (search != null && search.trim().isNotEmpty) {
      query['search'] = search.trim();
    }
    final res = await client.get(
      ApiConfig.identityBase,
      '/api/users/paginated/exclude-me',
      query: query,
    );
    return PagedResponse.fromJson(
      res as Map<String, dynamic>,
      User.fromJson,
    );
  }
}

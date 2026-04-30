import '../core/api_client.dart';
import '../core/api_config.dart';
import '../models/borrowed_map.dart';
import '../models/paged_response.dart';

class BorrowsService {
  final ApiClient client;

  BorrowsService(this.client);

  Future<String> borrowMap({required int mapId, required DateTime returnDate}) async {
    final iso =
        '${returnDate.year.toString().padLeft(4, '0')}-${returnDate.month.toString().padLeft(2, '0')}-${returnDate.day.toString().padLeft(2, '0')}';
    final res = await client.post(
      ApiConfig.borrowsBase,
      '/api/borrows/create',
      body: {'mapId': mapId, 'returnDate': iso},
    );
    return res?.toString() ?? '';
  }

  Future<String> returnMap({required int mapId}) async {
    final res = await client.post(
      ApiConfig.borrowsBase,
      '/api/borrows/return',
      body: {'mapId': mapId},
    );
    return res?.toString() ?? '';
  }

  Future<void> transferBorrow({
    required int borrowId,
    required int mapId,
    required int userIdToTransfer,
  }) async {
    await client.post(
      ApiConfig.borrowsBase,
      '/api/borrows/transfer',
      body: {
        'borrowId': borrowId,
        'mapId': mapId,
        'userIdToTransfer': userIdToTransfer,
      },
    );
  }

  Future<PagedResponse<BorrowedMap>> currentBorrows({
    required int pageNumber,
    required int pageSize,
    String? searchQuery,
  }) async {
    final query = <String, dynamic>{
      'pageNumber': pageNumber,
      'pageSize': pageSize,
    };
    if (searchQuery != null && searchQuery.trim().isNotEmpty) {
      query['searchQuery'] = searchQuery.trim();
    }
    final res = await client.get(
      ApiConfig.borrowsBase,
      '/api/borrows/current',
      query: query,
    );
    return PagedResponse.fromJson(
      res as Map<String, dynamic>,
      BorrowedMap.fromJson,
    );
  }
}

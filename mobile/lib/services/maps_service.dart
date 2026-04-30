import '../core/api_client.dart';
import '../core/api_config.dart';
import '../models/map_item.dart';
import '../models/paged_response.dart';

class MapsStats {
  final int totalMaps;
  final int availableMaps;
  final int borrowedMaps;

  MapsStats({
    required this.totalMaps,
    required this.availableMaps,
    required this.borrowedMaps,
  });

  factory MapsStats.fromJson(Map<String, dynamic> json) => MapsStats(
        totalMaps: (json['totalMaps'] as num?)?.toInt() ?? 0,
        availableMaps: (json['availableMaps'] as num?)?.toInt() ?? 0,
        borrowedMaps: (json['borrowedMaps'] as num?)?.toInt() ?? 0,
      );
}

class MapsService {
  final ApiClient client;

  MapsService(this.client);

  Future<List<MapItem>> recent() async {
    final res = await client.get(ApiConfig.mapsBase, '/api/maps/recent');
    return (res as List)
        .map((e) => MapItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<MapsStats> stats() async {
    final res = await client.get(ApiConfig.mapsBase, '/api/maps/stats');
    return MapsStats.fromJson(res as Map<String, dynamic>);
  }

  Future<PagedResponse<MapItem>> search({
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
      ApiConfig.mapsBase,
      '/api/maps/search/pagination',
      query: query,
    );
    return PagedResponse.fromJson(
      res as Map<String, dynamic>,
      MapItem.fromJson,
    );
  }

  Future<MapItem> getById(int mapId) async {
    final res = await client.get(ApiConfig.mapsBase, '/api/maps/$mapId');
    return MapItem.fromJson(res as Map<String, dynamic>);
  }

  Future<MapItem> createMap({required String name, required int year}) async {
    final res = await client.post(
      ApiConfig.mapsBase,
      '/api/maps/manager/create',
      body: {'name': name, 'yearPublished': year},
    );
    return MapItem.fromJson(res as Map<String, dynamic>);
  }

  Future<MapItem> updateMap({
    required int id,
    required String name,
    required int year,
  }) async {
    final res = await client.put(
      ApiConfig.mapsBase,
      '/api/maps/manager/$id',
      body: {'name': name, 'yearPublished': year},
    );
    return MapItem.fromJson(res as Map<String, dynamic>);
  }
}

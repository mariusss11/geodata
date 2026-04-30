import 'dart:convert';

import 'package:http/http.dart' as http;

import 'api_config.dart';
import 'auth_store.dart';

class ApiException implements Exception {
  final int statusCode;
  final String message;

  ApiException(this.statusCode, this.message);

  @override
  String toString() => 'ApiException($statusCode): $message';
}

class ApiClient {
  final AuthStore authStore;

  ApiClient(this.authStore);

  Map<String, String> _headers({bool jsonBody = false}) {
    final headers = <String, String>{};
    if (jsonBody) headers['Content-Type'] = 'application/json';
    final token = authStore.token;
    if (token != null) headers['Authorization'] = 'Bearer $token';
    return headers;
  }

  Uri _uri(String base, String path, [Map<String, dynamic>? query]) {
    final qp = query?.map((k, v) => MapEntry(k, v.toString()));
    return Uri.parse('$base$path').replace(queryParameters: qp);
  }

  Future<dynamic> get(String base, String path, {Map<String, dynamic>? query}) async {
    final res = await http
        .get(_uri(base, path, query), headers: _headers())
        .timeout(ApiConfig.connectTimeout);
    return _decode(res);
  }

  Future<dynamic> post(String base, String path, {Object? body}) async {
    final res = await http
        .post(
          _uri(base, path),
          headers: _headers(jsonBody: true),
          body: body == null ? null : jsonEncode(body),
        )
        .timeout(ApiConfig.connectTimeout);
    return _decode(res);
  }

  Future<dynamic> put(String base, String path, {Object? body}) async {
    final res = await http
        .put(
          _uri(base, path),
          headers: _headers(jsonBody: true),
          body: body == null ? null : jsonEncode(body),
        )
        .timeout(ApiConfig.connectTimeout);
    return _decode(res);
  }

  dynamic _decode(http.Response res) {
    if (res.statusCode >= 200 && res.statusCode < 300) {
      if (res.body.isEmpty) return null;
      try {
        return jsonDecode(res.body);
      } on FormatException {
        return res.body;
      }
    }
    String message = res.body;
    try {
      final decoded = jsonDecode(res.body);
      if (decoded is Map && decoded['message'] is String) {
        message = decoded['message'] as String;
      }
    } catch (_) {}
    throw ApiException(res.statusCode, message);
  }
}

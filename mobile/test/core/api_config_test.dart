import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/api_config.dart';

void main() {
  group('ApiConfig', () {
    setUp(() {
      // Reset to dev before each test
      ApiConfig.setEnvironment(Environment.dev);
    });

    test('dev environment uses HTTP protocol', () {
      ApiConfig.setEnvironment(Environment.dev);
      expect(ApiConfig.identityBase, contains('http://'));
    });

    test('prod environment uses HTTPS protocol', () {
      ApiConfig.setEnvironment(Environment.prod);
      expect(ApiConfig.identityBase, contains('https://'));
    });

    test('dev identity base includes port 8010', () {
      ApiConfig.setEnvironment(Environment.dev);
      final base = ApiConfig.identityBase;
      expect(base, contains('8010'));
    });

    test('dev maps base includes port 8020', () {
      ApiConfig.setEnvironment(Environment.dev);
      final base = ApiConfig.mapsBase;
      expect(base, contains('8020'));
    });

    test('dev borrows base includes port 8030', () {
      ApiConfig.setEnvironment(Environment.dev);
      final base = ApiConfig.borrowsBase;
      expect(base, contains('8030'));
    });

    test('prod uses API domain without ports', () {
      ApiConfig.setEnvironment(Environment.prod);
      final identityBase = ApiConfig.identityBase;
      expect(identityBase, isNot(contains(':8010')));
      expect(identityBase, contains('api.geodata.app'));
    });

    test('prod maps base uses ingress path', () {
      ApiConfig.setEnvironment(Environment.prod);
      final mapsBase = ApiConfig.mapsBase;
      expect(mapsBase, contains('/api/maps'));
    });

    test('prod borrows base uses ingress path', () {
      ApiConfig.setEnvironment(Environment.prod);
      final borrowsBase = ApiConfig.borrowsBase;
      expect(borrowsBase, contains('/api/borrows'));
    });

    test('environment can be changed dynamically', () {
      ApiConfig.setEnvironment(Environment.dev);
      expect(ApiConfig.identityBase, contains('http://'));

      ApiConfig.setEnvironment(Environment.prod);
      expect(ApiConfig.identityBase, contains('https://'));

      ApiConfig.setEnvironment(Environment.dev);
      expect(ApiConfig.identityBase, contains('http://'));
    });

    test('timeout constants are reasonable', () {
      expect(
        ApiConfig.connectTimeout,
        equals(Duration(seconds: 10)),
      );
      expect(
        ApiConfig.receiveTimeout,
        equals(Duration(seconds: 10)),
      );
    });

    test('API prefix is consistent', () {
      expect(ApiConfig.apiPrefix, '/api');
    });
  });
}

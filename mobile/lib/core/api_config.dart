import 'dart:io' show Platform;

enum Environment { dev, prod }

class ApiConfig {
  // Default to dev; override via setEnvironment() at app startup
  static Environment _environment = Environment.dev;

  static void setEnvironment(Environment env) {
    _environment = env;
  }

  static String _host() {
    switch (_environment) {
      case Environment.prod:
        // Production: use actual backend domain/IP (e.g., from kubernetes ingress)
        return 'api.geodata.app'; // Replace with your actual prod domain
      case Environment.dev:
        if (Platform.isAndroid) return '10.0.2.2';
        return 'localhost';
    }
  }

  static String _protocol() {
    return _environment == Environment.prod ? 'https' : 'http';
  }

  static String _port() {
    return _environment == Environment.prod ? '' : ':${_portNumber()}';
  }

  static String _portNumber() {
    if (Platform.isWindows || Platform.isLinux || Platform.isMacOS) {
      return '8010'; // Desktop dev
    }
    return '';
  }

  static String get identityBase {
    return '${_protocol()}://${_host()}${_port()}';
  }

  static String get mapsBase {
    if (_environment == Environment.prod) {
      return '${_protocol()}://api.geodata.app/api/maps';
    }
    return 'http://${_host()}:8020';
  }

  static String get borrowsBase {
    if (_environment == Environment.prod) {
      return '${_protocol()}://api.geodata.app/api/borrows';
    }
    return 'http://${_host()}:8030';
  }

  // For production, services communicate via ingress paths
  static const String apiPrefix = '/api';

  // Timeout configurations
  static const Duration connectTimeout = Duration(seconds: 10);
  static const Duration receiveTimeout = Duration(seconds: 10);
}

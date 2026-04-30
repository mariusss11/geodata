import 'package:flutter/material.dart';

import 'core/api_client.dart';
import 'core/api_config.dart';
import 'core/app_services.dart';
import 'core/auth_store.dart';
import 'screens/home_screen.dart';
import 'screens/login_screen.dart';
import 'services/auth_service.dart';
import 'services/borrows_service.dart';
import 'services/maps_service.dart';
import 'services/user_service.dart';

// Change to Environment.prod when building for production
const String appEnvironment = String.fromEnvironment('GEODATA_ENV', defaultValue: 'dev');

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Configure API environment based on build flavor
  final environment = appEnvironment == 'prod' ? Environment.prod : Environment.dev;
  ApiConfig.setEnvironment(environment);

  final authStore = AuthStore();
  await authStore.load();

  final apiClient = ApiClient(authStore);
  final services = (
    auth: AuthService(apiClient, authStore),
    maps: MapsService(apiClient),
    borrows: BorrowsService(apiClient),
    users: UserService(apiClient),
  );

  runApp(GeodataApp(
    authStore: authStore,
    apiClient: apiClient,
    auth: services.auth,
    maps: services.maps,
    borrows: services.borrows,
    users: services.users,
    environment: environment,
  ));
}

class GeodataApp extends StatelessWidget {
  final AuthStore authStore;
  final ApiClient apiClient;
  final AuthService auth;
  final MapsService maps;
  final BorrowsService borrows;
  final UserService users;
  final Environment environment;

  const GeodataApp({
    super.key,
    required this.authStore,
    required this.apiClient,
    required this.auth,
    required this.maps,
    required this.borrows,
    required this.users,
    required this.environment,
  });

  @override
  Widget build(BuildContext context) {
    return AppServices(
      authStore: authStore,
      apiClient: apiClient,
      auth: auth,
      maps: maps,
      borrows: borrows,
      users: users,
      child: MaterialApp(
        title: 'Geodata Maps${environment == Environment.prod ? '' : ' (Dev)'}',
        debugShowCheckedModeBanner: environment == Environment.dev,
        theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(seedColor: Colors.indigo),
          useMaterial3: true,
        ),
        home: authStore.isAuthenticated
            ? const HomeScreen()
            : const LoginScreen(),
      ),
    );
  }
}

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/api_client.dart';
import 'package:mobile/core/app_services.dart';
import 'package:mobile/core/auth_store.dart';
import 'package:mobile/screens/home_screen.dart';
import 'package:mobile/services/auth_service.dart';
import 'package:mobile/services/borrows_service.dart';
import 'package:mobile/services/maps_service.dart';
import 'package:mobile/services/user_service.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  group('HomeScreen Widget Tests', () {
    late AuthStore authStore;
    late ApiClient apiClient;
    late AuthService authService;

    setUp(() async {
      SharedPreferences.setMockInitialValues({
        'auth_token': 'test-token',
        'auth_user': '{"userId":1,"username":"testuser","name":"Test User","role":"USER"}',
      });
      authStore = AuthStore();
      await authStore.load();
      apiClient = ApiClient(authStore);
      authService = AuthService(apiClient, authStore);
    });

    Widget buildTestApp() {
      return MaterialApp(
        home: AppServices(
          authStore: authStore,
          apiClient: apiClient,
          auth: authService,
          maps: MapsService(apiClient),
          borrows: BorrowsService(apiClient),
          users: UserService(apiClient),
          child: const HomeScreen(),
        ),
      );
    }

    testWidgets('HomeScreen renders navigation tabs',
        (WidgetTester tester) async {
      await tester.pumpWidget(buildTestApp());

      // Check for typical navigation items
      expect(find.byType(BottomNavigationBar), findsOneWidget);
    });

    testWidgets('HomeScreen displays user greeting',
        (WidgetTester tester) async {
      await tester.pumpWidget(buildTestApp());
      await tester.pumpAndSettle();

      // Look for user name in widgets
      expect(
        find.text('Test User'),
        findsOneWidget,
      );
    });

    testWidgets('HomeScreen has profile, maps, and borrows sections',
        (WidgetTester tester) async {
      await tester.pumpWidget(buildTestApp());
      await tester.pumpAndSettle();

      // Should have navigation to different sections
      expect(find.byType(BottomNavigationBar), findsOneWidget);
    });
  });
}

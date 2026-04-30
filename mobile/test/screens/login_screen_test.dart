import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/api_client.dart';
import 'package:mobile/core/app_services.dart';
import 'package:mobile/core/auth_store.dart';
import 'package:mobile/models/user.dart';
import 'package:mobile/screens/login_screen.dart';
import 'package:mobile/services/auth_service.dart';
import 'package:mobile/services/borrows_service.dart';
import 'package:mobile/services/maps_service.dart';
import 'package:mobile/services/user_service.dart';
import 'package:shared_preferences/shared_preferences.dart';

class MockAuthService extends AuthService {
  bool shouldFail = false;

  MockAuthService(super.apiClient, super.authStore);

  @override
  Future<User> login({
    required String username,
    required String password,
  }) async {
    if (shouldFail) {
      throw ApiException(401, 'Invalid credentials');
    }
    return User(
      userId: 1,
      username: username,
      name: 'Test User',
      role: 'USER',
    );
  }
}

void main() {
  group('LoginScreen Widget Tests', () {
    late MockAuthService mockAuthService;
    late AuthStore authStore;
    late ApiClient apiClient;

    setUp(() async {
      SharedPreferences.setMockInitialValues({});
      authStore = AuthStore();
      await authStore.load();
      apiClient = ApiClient(authStore);
      mockAuthService = MockAuthService(apiClient, authStore);
    });

    Widget buildTestApp({required MockAuthService authService}) {
      return MaterialApp(
        home: AppServices(
          authStore: authStore,
          apiClient: apiClient,
          auth: authService,
          maps: MapsService(apiClient),
          borrows: BorrowsService(apiClient),
          users: UserService(apiClient),
          child: const LoginScreen(),
        ),
      );
    }

    testWidgets('LoginScreen renders email and password fields',
        (WidgetTester tester) async {
      await tester.pumpWidget(buildTestApp(authService: mockAuthService));

      expect(find.byType(TextFormField), findsWidgets);
      expect(find.byKey(const Key('emailField')), findsOneWidget);
      expect(find.byKey(const Key('passwordField')), findsOneWidget);
    });

    testWidgets('LoginScreen renders login button',
        (WidgetTester tester) async {
      await tester.pumpWidget(buildTestApp(authService: mockAuthService));

      expect(find.byKey(const Key('loginButton')), findsOneWidget);
    });

    testWidgets('LoginScreen shows error on invalid credentials',
        (WidgetTester tester) async {
      mockAuthService.shouldFail = true;

      await tester.pumpWidget(buildTestApp(authService: mockAuthService));

      await tester.enterText(
        find.byKey(const Key('emailField')),
        'test@example.com',
      );
      await tester.enterText(
        find.byKey(const Key('passwordField')),
        'wrongpassword',
      );

      await tester.tap(find.byKey(const Key('loginButton')));
      await tester.pumpAndSettle();

      expect(find.byType(SnackBar), findsOneWidget);
    });

    testWidgets('LoginScreen has register link', (WidgetTester tester) async {
      await tester.pumpWidget(buildTestApp(authService: mockAuthService));

      expect(find.text('Create account'), findsOneWidget);
    });

    testWidgets('Email field validates empty input',
        (WidgetTester tester) async {
      await tester.pumpWidget(buildTestApp(authService: mockAuthService));

      await tester.tap(find.byKey(const Key('loginButton')));
      await tester.pumpAndSettle();

      expect(find.text('Email is required'), findsWidgets);
    });

    testWidgets('Password field validates empty input',
        (WidgetTester tester) async {
      await tester.pumpWidget(buildTestApp(authService: mockAuthService));

      await tester.enterText(
        find.byKey(const Key('emailField')),
        'test@example.com',
      );

      await tester.tap(find.byKey(const Key('loginButton')));
      await tester.pumpAndSettle();

      expect(find.text('Password is required'), findsWidgets);
    });
  });
}

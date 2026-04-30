# Flutter Mobile App Testing Guide

This guide covers unit testing, widget testing, and integration testing for the Geodata Flutter app.

## Quick Start

### Run All Tests

```bash
cd mobile
flutter test
```

### Run Tests with Coverage

```bash
flutter test --coverage

# View coverage report
open coverage/index.html  # macOS
coverage/index.html      # Linux/Windows
```

### Run Specific Test File

```bash
flutter test test/models/user_test.dart
```

### Run Tests Matching Pattern

```bash
flutter test --name="AuthStore"
```

## Test Structure

```
test/
├── models/           # Unit tests for data models
│   ├── user_test.dart
│   ├── map_item_test.dart
│   ├── borrowed_map_test.dart
│   └── paged_response_test.dart
├── core/             # Unit tests for core services
│   ├── api_config_test.dart
│   └── auth_store_test.dart
├── screens/          # Widget tests for UI
│   ├── login_screen_test.dart
│   └── home_screen_test.dart
└── services/         # Service layer tests (optional)
```

## Types of Tests

### 1. Unit Tests (Models & Utilities)

Test individual classes in isolation. No UI, no external dependencies.

**Example:** `test/models/user_test.dart`

```dart
test('fromJson creates User with valid data', () {
  final json = {'id': '123', 'email': 'test@example.com'};
  final user = User.fromJson(json);
  
  expect(user.id, '123');
  expect(user.email, 'test@example.com');
});
```

**Run unit tests:**
```bash
flutter test test/models/
flutter test test/core/
```

### 2. Widget Tests (UI Components)

Test widgets, interactions, rendering, and navigation. Slower than unit tests.

**Example:** `test/screens/login_screen_test.dart`

```dart
testWidgets('LoginScreen renders email field', (WidgetTester tester) async {
  await tester.pumpWidget(buildTestApp());
  
  expect(find.byKey(const Key('emailField')), findsOneWidget);
});
```

**Run widget tests:**
```bash
flutter test test/screens/
```

### 3. Integration Tests (End-to-End)

Test complete user flows across multiple screens. Run on real device/emulator.

```bash
flutter test integration_test/login_flow_test.dart
```

## Test Coverage Goals

Aim for these coverage targets:

| Component        | Target | Priority |
|------------------|--------|----------|
| Models           | 85%+   | High     |
| Services         | 80%+   | High     |
| Core utilities   | 90%+   | High     |
| Screens          | 70%+   | Medium   |
| Helpers/Utils    | 75%+   | Medium   |

Generate and view coverage:

```bash
flutter test --coverage
lcov --list coverage/lcov.info  # macOS/Linux
```

## Mocking & Dependencies

### Mock Services

```dart
class MockAuthService extends AuthService {
  @override
  Future<User> login(String email, String password) async {
    return User(id: '1', email: email, ...);
  }
}
```

### Mock API Client

```dart
class MockApiClient extends ApiClient {
  @override
  Future<dynamic> get(String base, String path, {Map? query}) async {
    return {'status': 'ok'};
  }
}
```

### Use shared_preferences in Tests

```dart
setUp(() {
  SharedPreferences.setMockInitialValues({});
});
```

## Common Test Patterns

### Testing State Changes

```dart
testWidgets('Button tap updates state', (WidgetTester tester) async {
  await tester.pumpWidget(buildTestApp());
  
  expect(find.text('Initial'), findsOneWidget);
  
  await tester.tap(find.byType(ElevatedButton));
  await tester.pumpAndSettle();
  
  expect(find.text('Updated'), findsOneWidget);
});
```

### Testing Navigation

```dart
testWidgets('Tap leads to next screen', (WidgetTester tester) async {
  await tester.pumpWidget(buildTestApp());
  
  await tester.tap(find.text('Go to Details'));
  await tester.pumpAndSettle();
  
  expect(find.byType(DetailScreen), findsOneWidget);
});
```

### Testing Async Operations

```dart
testWidgets('Loading state shown during async call', (WidgetTester tester) async {
  await tester.pumpWidget(buildTestApp());
  
  await tester.tap(find.byType(ElevatedButton));
  await tester.pump();  // Show loading
  
  expect(find.byType(CircularProgressIndicator), findsOneWidget);
  
  await tester.pumpAndSettle();  // Wait for completion
  
  expect(find.text('Loaded'), findsOneWidget);
});
```

### Testing Form Validation

```dart
testWidgets('Form validates required fields', (WidgetTester tester) async {
  await tester.pumpWidget(buildTestApp());
  
  await tester.tap(find.byType(SubmitButton));
  await tester.pumpAndSettle();
  
  expect(find.text('Email is required'), findsOneWidget);
});
```

## Debugging Tests

### Run with verbose output

```bash
flutter test --verbose
```

### Run single test with debugging

```bash
flutter test test/models/user_test.dart --verbose
```

### Check test names

```bash
flutter test --list-tests
```

### Run test with observer

```dart
void main() {
  // Add this to test file
  testWidgets('...', (tester) async {
    addTearDown(tester.binding.window.physicalSizeTestValue = Size(400, 400));
  });
}
```

## GitHub Actions Integration

Tests run automatically on:

- ✅ Every push to `main` or `develop`
- ✅ Every pull request to `main` or `develop`
- ✅ Before building Android APK/AAB
- ✅ Coverage reports uploaded to Codecov

**View results:** Check GitHub Actions tab in your repository

## Best Practices

1. **Test behavior, not implementation**
   - ❌ Test internal state variables
   - ✅ Test user-visible outcomes

2. **Use meaningful test names**
   - ❌ `test('test1', ...)`
   - ✅ `test('login fails with invalid credentials', ...)`

3. **Keep tests isolated**
   - Each test should be independent
   - Use `setUp()` and `tearDown()` for cleanup

4. **Avoid test interdependencies**
   - Don't rely on test execution order
   - Each test should work in isolation

5. **Mock external dependencies**
   - Don't hit real APIs in tests
   - Use mocks for http, databases, files

6. **Test edge cases**
   - Empty strings, null values
   - Network errors, timeouts
   - Boundary conditions

## Performance Tips

- Unit tests should complete in <100ms
- Widget tests should complete in <1s
- Run tests locally before pushing

```bash
# Run tests in parallel (faster)
flutter test -j 4

# Run specific directory (faster than all)
flutter test test/models/
```

## Troubleshooting

### Tests timeout

Increase timeout:
```dart
testWidgets('...', (tester) async {
  // Test code
}, timeout: Timeout(Duration(seconds: 30)));
```

### SharedPreferences not working

Always mock in setUp:
```dart
setUp(() {
  SharedPreferences.setMockInitialValues({});
});
```

### Widget not found

Use `find.byType()` or `find.byKey()`:
```dart
expect(find.byKey(const Key('emailField')), findsOneWidget);
```

### Async issues

Always use `pumpAndSettle()` for async operations:
```dart
await tester.pumpAndSettle();  // Waits for all animations/futures
```

## Continuous Improvement

- Review coverage reports weekly
- Add tests for new features **before** writing code
- Keep test code maintainable and DRY
- Run tests locally before pushing
- Fix flaky tests immediately

---

For more information, see the [Flutter Testing Docs](https://docs.flutter.dev/testing).

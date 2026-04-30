import 'package:flutter/widgets.dart';

import '../services/auth_service.dart';
import '../services/borrows_service.dart';
import '../services/maps_service.dart';
import '../services/user_service.dart';
import 'api_client.dart';
import 'auth_store.dart';

class AppServices extends InheritedWidget {
  final AuthStore authStore;
  final ApiClient apiClient;
  final AuthService auth;
  final MapsService maps;
  final BorrowsService borrows;
  final UserService users;

  const AppServices({
    super.key,
    required this.authStore,
    required this.apiClient,
    required this.auth,
    required this.maps,
    required this.borrows,
    required this.users,
    required super.child,
  });

  static AppServices of(BuildContext context) {
    final widget = context.dependOnInheritedWidgetOfExactType<AppServices>();
    assert(widget != null, 'AppServices not found in widget tree');
    return widget!;
  }

  @override
  bool updateShouldNotify(AppServices oldWidget) => false;
}

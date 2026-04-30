import 'package:flutter/material.dart';

import '../core/app_services.dart';
import 'borrowed_maps_screen.dart';
import 'dashboard_screen.dart';
import 'login_screen.dart';
import 'maps_list_screen.dart';
import 'profile_screen.dart';
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _index = 0;

  static const _pages = <Widget>[
    MapsListScreen(),
    BorrowedMapsScreen(),
    DashboardScreen(),
  ];

  Future<void> _logout() async {
    await AppServices.of(context).auth.logout();
    if (!mounted) return;
    Navigator.of(context).pushAndRemoveUntil(
      MaterialPageRoute(builder: (_) => const LoginScreen()),
      (_) => false,
    );
  }

  @override
  Widget build(BuildContext context) {
    final user = AppServices.of(context).authStore.user;
    return Scaffold(
      appBar: AppBar(
        title: Text(_titleFor(_index)),
        actions: [
          if (user != null)
            Padding(
              padding: const EdgeInsets.only(left: 8),
              child: Center(
                child: Text(
                  user.name,
                  style: Theme.of(context).textTheme.bodyMedium,
                ),
              ),
            ),
          IconButton(
            tooltip: 'My profile',
            icon: const Icon(Icons.person_outline),
            onPressed: () => Navigator.of(context).push(
              MaterialPageRoute(builder: (_) => const ProfileScreen()),
            ),
          ),
          IconButton(
            tooltip: 'Sign out',
            icon: const Icon(Icons.logout),
            onPressed: _logout,
          ),
        ],
      ),
      body: IndexedStack(index: _index, children: _pages),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (i) => setState(() => _index = i),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.map_outlined),
            selectedIcon: Icon(Icons.map),
            label: 'Maps',
          ),
          NavigationDestination(
            icon: Icon(Icons.bookmark_outline),
            selectedIcon: Icon(Icons.bookmark),
            label: 'Borrowed',
          ),
          NavigationDestination(
            icon: Icon(Icons.bar_chart_outlined),
            selectedIcon: Icon(Icons.bar_chart),
            label: 'Dashboard',
          ),
        ],
      ),
    );
  }

  String _titleFor(int i) {
    switch (i) {
      case 1:
        return 'My borrows';
      case 2:
        return 'Dashboard';
      default:
        return 'Browse maps';
    }
  }
}

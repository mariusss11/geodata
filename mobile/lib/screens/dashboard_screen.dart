import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../core/api_client.dart';
import '../core/app_services.dart';
import '../models/map_item.dart';
import '../services/maps_service.dart';

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  MapsStats? _stats;
  List<MapItem> _recent = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final svc = AppServices.of(context).maps;
      final results = await Future.wait([svc.stats(), svc.recent()]);
      if (!mounted) return;
      setState(() {
        _stats = results[0] as MapsStats;
        _recent = results[1] as List<MapItem>;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _error = e.message);
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = 'Failed to load dashboard: $e');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());

    if (_error != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.error_outline, size: 48),
              const SizedBox(height: 8),
              Text(_error!, textAlign: TextAlign.center),
              const SizedBox(height: 16),
              FilledButton(onPressed: _load, child: const Text('Retry')),
            ],
          ),
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 20),
        children: [
          _buildStatsRow(),
          const SizedBox(height: 24),
          _buildRecentSection(),
        ],
      ),
    );
  }

  Widget _buildStatsRow() {
    final s = _stats;
    if (s == null) return const SizedBox.shrink();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Library overview',
          style: Theme.of(context).textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.w600,
              ),
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(
              child: _StatCard(
                label: 'Total',
                value: s.totalMaps,
                icon: Icons.map_outlined,
                color: Theme.of(context).colorScheme.primaryContainer,
                onColor: Theme.of(context).colorScheme.onPrimaryContainer,
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: _StatCard(
                label: 'Available',
                value: s.availableMaps,
                icon: Icons.check_circle_outline,
                color: Theme.of(context).colorScheme.secondaryContainer,
                onColor: Theme.of(context).colorScheme.onSecondaryContainer,
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: _StatCard(
                label: 'Borrowed',
                value: s.borrowedMaps,
                icon: Icons.bookmark_outline,
                color: Theme.of(context).colorScheme.tertiaryContainer,
                onColor: Theme.of(context).colorScheme.onTertiaryContainer,
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildRecentSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Recently added',
          style: Theme.of(context).textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.w600,
              ),
        ),
        const SizedBox(height: 8),
        if (_recent.isEmpty)
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 16),
            child: Center(child: Text('No maps added in the last 30 days.')),
          )
        else
          Card(
            margin: EdgeInsets.zero,
            child: Column(
              children: [
                for (int i = 0; i < _recent.length; i++) ...[
                  if (i != 0) const Divider(height: 0),
                  _RecentMapTile(map: _recent[i]),
                ],
              ],
            ),
          ),
      ],
    );
  }
}

class _StatCard extends StatelessWidget {
  final String label;
  final int value;
  final IconData icon;
  final Color color;
  final Color onColor;

  const _StatCard({
    required this.label,
    required this.value,
    required this.icon,
    required this.color,
    required this.onColor,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      color: color,
      margin: EdgeInsets.zero,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 8),
        child: Column(
          children: [
            Icon(icon, color: onColor, size: 28),
            const SizedBox(height: 8),
            Text(
              '$value',
              style: TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.bold,
                color: onColor,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              label,
              style: TextStyle(fontSize: 12, color: onColor),
            ),
          ],
        ),
      ),
    );
  }
}

class _RecentMapTile extends StatelessWidget {
  final MapItem map;

  const _RecentMapTile({required this.map});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final dateLabel = map.createdAt != null
        ? DateFormat.yMMMd().format(map.createdAt!)
        : '${map.year}';
    return ListTile(
      leading: const Icon(Icons.map_outlined),
      title: Text(map.name),
      subtitle: Text('${map.year} · Added $dateLabel'),
      trailing: Chip(
        label: Text(map.availabilityStatus),
        backgroundColor:
            map.isAvailable ? scheme.secondaryContainer : scheme.errorContainer,
        labelStyle: TextStyle(
          fontSize: 11,
          color: map.isAvailable
              ? scheme.onSecondaryContainer
              : scheme.onErrorContainer,
        ),
        padding: EdgeInsets.zero,
        visualDensity: VisualDensity.compact,
      ),
    );
  }
}

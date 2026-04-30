import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../core/api_client.dart';
import '../core/app_services.dart';
import '../models/map_item.dart';
import 'map_form_screen.dart';

class MapDetailScreen extends StatefulWidget {
  final MapItem item;

  const MapDetailScreen({super.key, required this.item});

  @override
  State<MapDetailScreen> createState() => _MapDetailScreenState();
}

class _MapDetailScreenState extends State<MapDetailScreen> {
  late MapItem _item = widget.item;
  bool _busy = false;

  Future<void> _refresh() async {
    try {
      final updated = await AppServices.of(context).maps.getById(_item.id);
      if (mounted) setState(() => _item = updated);
    } on ApiException catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Refresh failed: ${e.message}')),
        );
      }
    }
  }

  Future<void> _borrow() async {
    final returnDate = await showDatePicker(
      context: context,
      initialDate: DateTime.now().add(const Duration(days: 14)),
      firstDate: DateTime.now().add(const Duration(days: 1)),
      lastDate: DateTime.now().add(const Duration(days: 365)),
      helpText: 'Return by',
    );
    if (returnDate == null) return;
    if (!mounted) return;
    final borrows = AppServices.of(context).borrows;
    final messenger = ScaffoldMessenger.of(context);
    final navigator = Navigator.of(context);
    setState(() => _busy = true);
    try {
      final msg = await borrows.borrowMap(
        mapId: _item.id,
        returnDate: returnDate,
      );
      if (!mounted) return;
      messenger.showSnackBar(
        SnackBar(content: Text(msg.isEmpty ? 'Borrow request submitted' : msg)),
      );
      await _refresh();
      if (mounted) navigator.pop(true);
    } on ApiException catch (e) {
      if (mounted) {
        messenger.showSnackBar(SnackBar(content: Text(e.message)));
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _openEdit() async {
    final saved = await Navigator.of(context).push<bool>(
      MaterialPageRoute(builder: (_) => MapFormScreen(item: _item)),
    );
    if (saved == true) {
      await _refresh();
      if (mounted) Navigator.of(context).pop(true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final df = DateFormat.yMMMd();
    return Scaffold(
      appBar: AppBar(
        title: const Text('Map details'),
        actions: [
          IconButton(
            tooltip: 'Edit map',
            icon: const Icon(Icons.edit_outlined),
            onPressed: _openEdit,
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Row(
            children: [
              const Icon(Icons.map, size: 48),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _item.name,
                      style: Theme.of(context).textTheme.headlineSmall,
                    ),
                    const SizedBox(height: 4),
                    Text('Year: ${_item.year}'),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 24),
          _InfoRow(label: 'Status', value: _item.availabilityStatus),
          if (_item.createdAt != null)
            _InfoRow(label: 'Added', value: df.format(_item.createdAt!)),
          if (_item.updatedAt != null)
            _InfoRow(label: 'Updated', value: df.format(_item.updatedAt!)),
          const SizedBox(height: 32),
          FilledButton.icon(
            onPressed: _busy || !_item.isAvailable ? null : _borrow,
            icon: const Icon(Icons.bookmark_add_outlined),
            label: Text(_item.isAvailable ? 'Borrow this map' : 'Not available'),
          ),
          const SizedBox(height: 12),
        ],
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  final String label;
  final String value;

  const _InfoRow({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        children: [
          SizedBox(
            width: 100,
            child: Text(
              label,
              style: TextStyle(color: Theme.of(context).colorScheme.outline),
            ),
          ),
          Expanded(child: Text(value)),
        ],
      ),
    );
  }
}

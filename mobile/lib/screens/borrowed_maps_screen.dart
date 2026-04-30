import 'dart:async';

import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../core/api_client.dart';
import '../core/app_services.dart';
import '../models/borrowed_map.dart';
import '../models/user.dart';
import 'user_picker_dialog.dart';

class BorrowedMapsScreen extends StatefulWidget {
  const BorrowedMapsScreen({super.key});

  @override
  State<BorrowedMapsScreen> createState() => _BorrowedMapsScreenState();
}

class _BorrowedMapsScreenState extends State<BorrowedMapsScreen> {
  static const _pageSize = 20;

  final _searchController = TextEditingController();
  Timer? _debounce;

  final List<BorrowedMap> _items = [];
  bool _loading = false;
  bool _initialLoadDone = false;
  String? _error;
  String _query = '';

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _searchController.dispose();
    _debounce?.cancel();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final page = await AppServices.of(context).borrows.currentBorrows(
            pageNumber: 0,
            pageSize: _pageSize,
            searchQuery: _query.isEmpty ? null : _query,
          );
      if (!mounted) return;
      setState(() {
        _items
          ..clear()
          ..addAll(page.content);
        _initialLoadDone = true;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _error = e.message);
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = 'Failed to load borrows: $e');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _returnItem(BorrowedMap b) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Return map?'),
        content: Text('Return "${b.name}" to the library?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Return'),
          ),
        ],
      ),
    );
    if (confirm != true) return;
    if (!mounted) return;
    final borrows = AppServices.of(context).borrows;
    final messenger = ScaffoldMessenger.of(context);
    try {
      final msg = await borrows.returnMap(mapId: b.mapId);
      if (!mounted) return;
      messenger.showSnackBar(
        SnackBar(content: Text(msg.isEmpty ? 'Return submitted' : msg)),
      );
      _load();
    } on ApiException catch (e) {
      if (!mounted) return;
      messenger.showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  Future<void> _transferItem(BorrowedMap b) async {
    final selectedUser = await showDialog<User>(
      context: context,
      builder: (_) => UserPickerDialog(
        onSelected: (u) => Navigator.of(context).pop(u),
      ),
    );
    if (selectedUser == null || !mounted) return;

    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Transfer borrow?'),
        content: Text(
          'Transfer "${b.name}" to ${selectedUser.name} (@${selectedUser.username})?',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Transfer'),
          ),
        ],
      ),
    );
    if (confirm != true || !mounted) return;

    final borrows = AppServices.of(context).borrows;
    final messenger = ScaffoldMessenger.of(context);
    try {
      await borrows.transferBorrow(
        borrowId: b.borrowId,
        mapId: b.mapId,
        userIdToTransfer: selectedUser.userId ?? 0,
      );
      if (!mounted) return;
      messenger.showSnackBar(
        SnackBar(
          content: Text(
            'Transferred to ${selectedUser.name}',
          ),
        ),
      );
      _load();
    } on ApiException catch (e) {
      if (!mounted) return;
      messenger.showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  void _onSearchChanged(String v) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 350), () {
      setState(() => _query = v.trim());
      _load();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
          child: TextField(
            controller: _searchController,
            decoration: InputDecoration(
              hintText: 'Search my borrows…',
              prefixIcon: const Icon(Icons.search),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(24),
              ),
              contentPadding:
                  const EdgeInsets.symmetric(horizontal: 16, vertical: 0),
            ),
            onChanged: _onSearchChanged,
          ),
        ),
        Expanded(child: _buildBody()),
      ],
    );
  }

  Widget _buildBody() {
    if (!_initialLoadDone && _loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error != null && _items.isEmpty) {
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
    if (_items.isEmpty) {
      return const Center(child: Text("You haven't borrowed any maps yet."));
    }
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView.separated(
        padding: const EdgeInsets.symmetric(vertical: 8),
        itemCount: _items.length,
        separatorBuilder: (_, _) => const Divider(height: 0),
        itemBuilder: (context, i) {
          final b = _items[i];
          final df = DateFormat.yMMMd();
          final dateLabel = b.createdAt != null
              ? 'Added ${df.format(b.createdAt!)}'
              : 'Year: ${b.year}';
          return ListTile(
            leading: const Icon(Icons.bookmark),
            title: Text(b.name),
            subtitle: Text('$dateLabel • ${b.availabilityStatus}'),
            trailing: PopupMenuButton<_BorrowAction>(
              onSelected: (action) {
                if (action == _BorrowAction.returnMap) {
                  _returnItem(b);
                } else {
                  _transferItem(b);
                }
              },
              itemBuilder: (_) => const [
                PopupMenuItem(
                  value: _BorrowAction.returnMap,
                  child: ListTile(
                    leading: Icon(Icons.assignment_return_outlined),
                    title: Text('Return'),
                    contentPadding: EdgeInsets.zero,
                  ),
                ),
                PopupMenuItem(
                  value: _BorrowAction.transfer,
                  child: ListTile(
                    leading: Icon(Icons.swap_horiz),
                    title: Text('Transfer'),
                    contentPadding: EdgeInsets.zero,
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}

enum _BorrowAction { returnMap, transfer }

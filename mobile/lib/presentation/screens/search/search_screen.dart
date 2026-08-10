import 'package:flutter/material.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

class SearchScreen extends StatefulWidget {
  const SearchScreen({super.key});

  @override
  State<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends State<SearchScreen> {
  final _searchController = TextEditingController();
  final _apiService = ApiService();
  List<dynamic> _results = [];
  bool _isSearching = false;

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _search(String query) async {
    if (query.trim().isEmpty) return;
    setState(() => _isSearching = true);
    try {
      final response = await _apiService.get('/search', params: {'q': query.trim()});
      if (mounted) {
        setState(() {
          _results = response.data is List ? response.data as List<dynamic> : [];
          _isSearching = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isSearching = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Recherche')),
      drawer: const AppDrawer(),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(16),
            child: TextField(
              controller: _searchController,
              decoration: InputDecoration(
                hintText: 'Nom, email, téléphone...',
                prefixIcon: const Icon(Icons.search),
                suffixIcon: _searchController.text.isNotEmpty
                    ? IconButton(
                        icon: const Icon(Icons.clear),
                        onPressed: () {
                          _searchController.clear();
                          setState(() => _results = []);
                        },
                      )
                    : null,
              ),
              style: const TextStyle(color: Colors.white),
              onSubmitted: _search,
              textInputAction: TextInputAction.search,
            ),
          ),
          if (_isSearching)
            const Center(child: Padding(padding: EdgeInsets.all(32), child: CircularProgressIndicator()))
          else if (_results.isEmpty && _searchController.text.isNotEmpty)
            Center(
              child: Padding(
                padding: const EdgeInsets.all(32),
                child: Text('Aucun résultat', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
              ),
            )
          else
            Expanded(
              child: ListView.builder(
                itemCount: _results.length,
                padding: const EdgeInsets.symmetric(horizontal: 16),
                itemBuilder: (_, i) {
                  final item = _results[i] as Map<String, dynamic>;
                  return Container(
                    margin: const EdgeInsets.only(bottom: 8),
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.04),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
                    ),
                    child: Row(
                      children: [
                        CircleAvatar(
                          backgroundColor: Colors.teal.withValues(alpha: 0.2),
                          child: Text(
                            (item['nom'] as String? ?? '?')[0].toUpperCase(),
                            style: const TextStyle(color: Colors.teal, fontWeight: FontWeight.bold),
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(item['nom'] ?? '', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                              if (item['type'] != null)
                                Text(item['type'].toString(), style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                            ],
                          ),
                        ),
                        Icon(Icons.chevron_right, color: Colors.white.withValues(alpha: 0.3)),
                      ],
                    ),
                  );
                },
              ),
            ),
        ],
      ),
    );
  }
}

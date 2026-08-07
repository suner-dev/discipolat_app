import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';

class NotificationsScreen extends StatefulWidget {
  const NotificationsScreen({super.key});

  @override
  State<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends State<NotificationsScreen> {
  final _apiService = ApiService();
  List<Map<String, dynamic>> _notifications = [];
  bool _isLoading = true;
  int _unreadCount = 0;

  @override
  void initState() { super.initState(); _loadNotifications(); }

  Future<void> _loadNotifications() async {
    try {
      final response = await _apiService.get('/notifications', params: {'size': '50', 'sort': 'createdAt,desc'});
      final data = response.data as Map<String, dynamic>;
      if (mounted) {
        setState(() {
          _notifications = (data['content'] as List).map((e) => e as Map<String, dynamic>).toList();
          _unreadCount = _notifications.where((n) => n['lu'] == false).length;
          _isLoading = false;
        });
      }
    } catch (e) { if (mounted) setState(() => _isLoading = false); }
  }

  Color _getColor(String type) {
    switch (type) {
      case 'ABSENCE_48H': return Colors.orange;
      case 'RAPPORT_NON_SOUMIS': return Colors.blue;
      case 'RAPPORT_FAMILLE_NON_SOUMIS': return Colors.purple;
      case 'ALERTE_ABSENCE': return Colors.red;
      default: return Colors.grey;
    }
  }

  IconData _getIcon(String type) {
    switch (type) {
      case 'ABSENCE_48H': return Icons.warning_amber;
      case 'RAPPORT_NON_SOUMIS': return Icons.description;
      case 'ALERTE_ABSENCE': return Icons.person_off;
      default: return Icons.info;
    }
  }

  Future<void> _markAllRead() async {
    try {
      await _apiService.post('/notifications/mark-all-read');
      if (mounted) {
        setState(() => _unreadCount = 0);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Notifications marquées comme lues')),
        );
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erreur lors du marquage')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Notifications'),
        actions: [
          if (_unreadCount > 0) TextButton(onPressed: _markAllRead, child: const Text('Tout marquer lu', style: TextStyle(fontSize: 12))),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 5)
          : RefreshIndicator(
              onRefresh: _loadNotifications,
              child: _notifications.isEmpty
                  ? Center(child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
                      Icon(Icons.notifications_off, size: 64, color: Colors.white.withValues(alpha: 0.15)),
                      const SizedBox(height: 16),
                      Text('Aucune notification', style: TextStyle(color: Colors.white.withValues(alpha: 0.4))),
                    ]))
                  : ListView.builder(
                      padding: const EdgeInsets.all(16),
                      itemCount: _notifications.length,
                      itemBuilder: (context, index) {
                        final notif = _notifications[index];
                        final isRead = notif['lu'] == true;
                        final color = _getColor(notif['type'] ?? '');

                        return GlassCard(
                          margin: const EdgeInsets.only(bottom: 8),
                          padding: const EdgeInsets.all(12),
                          borderColor: isRead ? null : color.withValues(alpha: 0.3),
                          child: Row(children: [
                            Container(
                              padding: const EdgeInsets.all(10),
                              decoration: BoxDecoration(
                                color: color.withValues(alpha: 0.15),
                                borderRadius: BorderRadius.circular(12),
                              ),
                              child: Icon(_getIcon(notif['type'] ?? ''), color: color, size: 22),
                            ),
                            const SizedBox(width: 12),
                            Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                              Text(notif['titre'] ?? '', style: TextStyle(
                                color: Colors.white,
                                fontSize: 14,
                                fontWeight: isRead ? FontWeight.normal : FontWeight.bold,
                              )),
                              const SizedBox(height: 4),
                              Text(notif['message'] ?? '', maxLines: 2, overflow: TextOverflow.ellipsis,
                                style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                              const SizedBox(height: 4),
                              Text('Il y a quelques instants', style: TextStyle(color: Colors.white.withValues(alpha: 0.25), fontSize: 10)),
                            ])),
                            if (!isRead)
                              Container(
                                width: 8, height: 8,
                                decoration: BoxDecoration(color: color, shape: BoxShape.circle, boxShadow: [BoxShadow(color: color, blurRadius: 4)]),
                              ),
                          ]),
                        );
                      },
                    ),
            ),
    );
  }
}

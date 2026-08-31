import '../../models/scheduled_announcement.dart';
import 'api_service.dart';

/// Service for ScheduledAnnouncement operations (mobile).
/// Backend: AnnouncementController (/api/v1/announcements)
class AnnouncementService {
  final ApiService _api;

  AnnouncementService(this._api);

  /// Fetch all announcements for the current tenant.
  Future<List<ScheduledAnnouncement>> fetchAll() async {
    try {
      final res = await _api.get('/announcements');
      final payload = _asList(res.data);
      return payload
          .map((e) => ScheduledAnnouncement.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList();
    } catch (_) {
      return [];
    }
  }

  /// Publish a scheduled announcement.
  Future<void> publish(String id) async {
    await _api.post('/announcements/$id/publish');
  }

  /// Cancel a scheduled announcement.
  Future<void> cancel(String id) async {
    await _api.post('/announcements/$id/cancel');
  }

  /// Delete an announcement.
  Future<void> delete(String id) async {
    await _api.delete('/announcements/$id');
  }

  List<dynamic> _asList(dynamic data) {
    if (data is List) return data;
    if (data is Map<String, dynamic>) {
      final items = data['content'] ?? data['items'];
      if (items is List) return items;
    }
    return [];
  }
}

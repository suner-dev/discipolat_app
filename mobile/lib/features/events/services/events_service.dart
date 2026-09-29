import 'package:dio/dio.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/data/services/providers.dart';
import 'package:discipolat_mobile/features/events/models/event_model.dart';

part 'events_service.g.dart';

@riverpod
EventsService eventsService(EventsServiceRef ref) {
  final api = ref.watch(apiServiceProvider);
  return EventsService(api);
}


class EventsService {
  /// Frontière backend -> application.
  ///
  /// Le backend expose `statut` comme une chaîne libre. `$enumDecode` (généré)
  /// lève sur une valeur inattendue, ce qui ferait échouer TOUTE la liste
  /// d'événements pour un enregistrement atypique. On normalise donc ici :
  /// le modèle reste un modèle, la tolérance vit à la frontière.
  static Map<String, dynamic> _normalize(Map<String, dynamic> json) {
    final raw = json['statut'] ?? json['status'];
    if (raw is String) {
      return {...json, 'status': EventStatusWire.decode(raw).name};
    }
    return {...json, 'status': EventStatus.unknown.name};
  }

  Event _event(dynamic raw) =>
      Event.fromJson(_normalize(raw as Map<String, dynamic>));

  final ApiService _api;

  EventsService(this._api);

  // Events CRUD
  Future<List<Event>> getEvents({
    int page = 0,
    int size = 20,
    EventStatus? status,
    EventType? type,
    DateTime? fromDate,
    DateTime? toDate,
    String? spaceId,
    bool? myEvents,
    String? search,
  }) async {
    try {
      final queryParams = <String, dynamic>{
        'page': page,
        'size': size,
        if (status != null) 'status': status.name,
        if (type != null) 'type': type.name,
        if (fromDate != null) 'fromDate': fromDate.toIso8601String(),
        if (toDate != null) 'toDate': toDate.toIso8601String(),
        if (spaceId != null) 'spaceId': spaceId,
        if (myEvents == true) 'myEvents': 'true',
        if (search != null && search.isNotEmpty) 'search': search,
      };
      final response = await _api.get('/events', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => Event.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des événements: $e');
    }
  }

  Future<List<Event>> getUpcomingEvents({int limit = 10}) async {
    try {
      final response = await _api.get('/events/upcoming', queryParameters: {'limit': limit});
      final data = response.data as List;
      return data.map((json) => Event.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des événements à venir: $e');
    }
  }

  Future<Event> getEvent(String id) async {
    try {
      final response = await _api.get('/events/$id');
      return _event(response.data);
    } catch (e) {
      throw Exception('Erreur lors du chargement de l\'événement: $e');
    }
  }

  Future<Event> createEvent(Event event) async {
    try {
      final response = await _api.post('/events', data: event.toJson());
      return _event(response.data);
    } catch (e) {
      throw Exception('Erreur lors de la création: $e');
    }
  }

  Future<Event> updateEvent(String id, Event event) async {
    try {
      final response = await _api.put('/events/$id', data: event.toJson());
      return _event(response.data);
    } catch (e) {
      throw Exception('Erreur lors de la mise à jour: $e');
    }
  }

  /// Annule un evenement.
  ///
  /// Le backend accepte un **patch partiel** : `EventService.update` n'ecrit
  /// que les champs non nuls, donc envoyer uniquement `statut` laisse le
  /// titre, les dates et le lieu intacts. La valeur envoyee est `ANNULE`, le
  /// vocabulaire du backend (et celui du web : `StatutEvenement`).
  Future<Event> cancelEvent(String id) async {
    try {
      final response = await _api.put('/events/$id', data: {'statut': 'ANNULE'});
      return _event(response.data);
    } catch (e) {
      throw Exception('Erreur lors de l\'annulation: $e');
    }
  }

  Future<void> deleteEvent(String id) async {
    try {
      await _api.delete('/events/$id');
    } catch (e) {
      throw Exception('Erreur lors de la suppression: $e');
    }
  }

  // Registration
  Future<EventRegistration> registerForEvent(String eventId, {bool hasGuest = false, int guestCount = 0}) async {
    try {
      final response = await _api.post('/events/$eventId/register', data: {
        'hasGuest': hasGuest,
        'guestCount': guestCount,
      });
      return EventRegistration.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de l\'inscription: $e');
    }
  }

  Future<void> cancelRegistration(String eventId) async {
    try {
      await _api.delete('/events/$eventId/register');
    } catch (e) {
      throw Exception('Erreur lors de l\'annulation: $e');
    }
  }

  Future<EventRegistration> getMyRegistration(String eventId) async {
    try {
      final response = await _api.get('/events/$eventId/my-registration');
      return EventRegistration.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du chargement de l\'inscription: $e');
    }
  }

  Future<List<EventRegistration>> getEventRegistrations(String eventId, {int page = 0, int size = 50, RegistrationStatus? status}) async {
    try {
      final queryParams = <String, dynamic>{
        'page': page,
        'size': size,
        if (status != null) 'status': status.name,
      };
      final response = await _api.get('/events/$eventId/registrations', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => EventRegistration.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des inscriptions: $e');
    }
  }

  // Check-in
  Future<EventRegistration> checkIn(String eventId, {String? method, String? qrCode}) async {
    try {
      final response = await _api.post('/events/$eventId/check-in', data: {
        'method': method ?? 'MANUAL',
        'qrCode': qrCode,
      });
      return EventRegistration.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du check-in: $e');
    }
  }

  Future<EventRegistration> checkInByQr(String eventId, String qrCode) async {
    try {
      final response = await _api.post('/events/$eventId/check-in/qr', data: {
        'qrCode': qrCode,
      });
      return EventRegistration.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du check-in QR: $e');
    }
  }

  // Team
  Future<List<EventTeamMember>> getEventTeam(String eventId) async {
    try {
      final response = await _api.get('/events/$eventId/team');
      final data = response.data as List;
      return data.map((json) => EventTeamMember.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement de l\'équipe: $e');
    }
  }

  Future<EventTeamMember> addTeamMember(String eventId, String personId, TeamRole role, {String? responsibilities}) async {
    try {
      final response = await _api.post('/events/$eventId/team', data: {
        'personId': personId,
        'role': role.name,
        'responsibilities': responsibilities,
      });
      return EventTeamMember.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de l\'ajout au team: $e');
    }
  }

  Future<void> removeTeamMember(String eventId, String memberId) async {
    try {
      await _api.delete('/events/$eventId/team/$memberId');
    } catch (e) {
      throw Exception('Erreur lors de la suppression du membre: $e');
    }
  }

  // Checklist
  Future<List<EventChecklist>> getChecklist(String eventId) async {
    try {
      final response = await _api.get('/events/$eventId/checklist');
      final data = response.data as List;
      return data.map((json) => EventChecklist.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement de la checklist: $e');
    }
  }

  Future<EventChecklist> addChecklistItem(String eventId, EventChecklist item) async {
    try {
      final response = await _api.post('/events/$eventId/checklist', data: item.toJson());
      return EventChecklist.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de l\'ajout: $e');
    }
  }

  Future<EventChecklist> updateChecklistItem(String eventId, String itemId, EventChecklist item) async {
    try {
      final response = await _api.put('/events/$eventId/checklist/$itemId', data: item.toJson());
      return EventChecklist.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la mise à jour: $e');
    }
  }

  Future<void> deleteChecklistItem(String eventId, String itemId) async {
    try {
      await _api.delete('/events/$eventId/checklist/$itemId');
    } catch (e) {
      throw Exception('Erreur lors de la suppression: $e');
    }
  }

  // Dress Code
  Future<DressCode?> getDressCode(String eventId) async {
    try {
      final response = await _api.get('/events/$eventId/dress-code');
      if (response.data == null) return null;
      return DressCode.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      return null;
    }
  }

  Future<DressCode> assignDressCode(String eventId, String dressCodeId) async {
    try {
      final response = await _api.post('/events/$eventId/dress-code', data: {
        'dressCodeId': dressCodeId,
      });
      return DressCode.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de l\'assignation: $e');
    }
  }

  // Statistics
  Future<Map<String, dynamic>> getEventStats(String eventId) async {
    try {
      final response = await _api.get('/events/$eventId/stats');
      return response.data as Map<String, dynamic>;
    } catch (e) {
      throw Exception('Erreur lors du chargement des stats: $e');
    }
  }
}
import 'package:dio/dio.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/features/streaming/models/stream_model.dart';

part 'streaming_service.g.dart';

@riverpod
StreamingService streamingService(StreamingServiceRef ref) {
  final api = ref.watch(apiServiceProvider);
  return StreamingService(api);
}

class StreamingService {
  final ApiService _api;

  StreamingService(this._api);

  // Stream CRUD
  Future<List<StreamModel>> getStreams({String? status}) async {
    try {
      final queryParams = <String, dynamic>{};
      if (status != null) queryParams['status'] = status;
      final response = await _api.get('/streams', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => StreamModel.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des streams: $e');
    }
  }

  Future<List<StreamModel>> getLiveStreams() async {
    try {
      final response = await _api.get('/streams/live');
      final data = response.data as List;
      return data.map((json) => StreamModel.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des streams en direct: $e');
    }
  }

  Future<StreamModel> getStream(int id) async {
    try {
      final response = await _api.get('/streams/$id');
      return StreamModel.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du chargement du stream: $e');
    }
  }

  Future<StreamModel> createStream(StreamModel stream) async {
    try {
      final response = await _api.post('/streams', data: stream.toJson());
      return StreamModel.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création du stream: $e');
    }
  }

  Future<StreamModel> goLive(int id) async {
    try {
      final response = await _api.post('/streams/$id/go-live');
      return StreamModel.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du lancement du stream: $e');
    }
  }

  Future<StreamModel> endStream(int id) async {
    try {
      final response = await _api.post('/streams/$id/end');
      return StreamModel.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de l\'arrêt du stream: $e');
    }
  }

  Future<void> incrementViewer(int id) async {
    try {
      await _api.post('/streams/$id/viewer');
    } catch (e) {
      // Silently fail for viewer count
    }
  }

  // Chat
  Future<List<StreamChatMessage>> getChatMessages(int streamId, {int page = 0, int size = 50}) async {
    try {
      final response = await _api.get('/stream-chat/$streamId', queryParameters: {'page': page, 'size': size});
      final data = response.data as List;
      return data.map((json) => StreamChatMessage.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des messages: $e');
    }
  }

  Future<void> sendChatMessage(int streamId, String content, {String? emoji}) async {
    try {
      await _api.post('/stream-chat/$streamId', data: {
        'content': content,
        'emoji': emoji,
        'senderName': 'Vous',
      });
    } catch (e) {
      throw Exception('Erreur lors de l\'envoi du message: $e');
    }
  }

  Future<StreamViewerCount> getViewerCount(int streamId) async {
    try {
      final response = await _api.get('/stream-chat/$streamId/count');
      return StreamViewerCount.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du chargement du nombre de spectateurs: $e');
    }
  }
}

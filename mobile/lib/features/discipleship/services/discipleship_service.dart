import 'package:dio/dio.dart';
import 'package:riverpod_annotation/riverpod_annotation.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/data/services/providers.dart';
import 'package:discipolat_mobile/features/discipleship/models/discipleship_model.dart';

part 'discipleship_service.g.dart';

@riverpod
DiscipleshipService discipleshipService(DiscipleshipServiceRef ref) {
  final api = ref.watch(apiServiceProvider);
  return DiscipleshipService(api);
}

class DiscipleshipService {
  final ApiService _api;

  DiscipleshipService(this._api);

  // Journeys
  Future<List<DiscipleshipJourney>> getJourneys({bool? isActive}) async {
    try {
      final queryParams = <String, dynamic>{};
      if (isActive != null) queryParams['isActive'] = isActive.toString();
      final response = await _api.get('/discipleship/journeys', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => DiscipleshipJourney.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des parcours: $e');
    }
  }

  Future<DiscipleshipJourney> getJourney(int id) async {
    try {
      final response = await _api.get('/discipleship/journeys/$id');
      return DiscipleshipJourney.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du chargement du parcours: $e');
    }
  }

  Future<DiscipleshipJourney> createJourney(DiscipleshipJourney journey) async {
    try {
      final response = await _api.post('/discipleship/journeys', data: journey.toJson());
      return DiscipleshipJourney.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création: $e');
    }
  }

  // Stages
  Future<List<DiscipleshipStage>> getStages(int journeyId) async {
    try {
      final response = await _api.get('/discipleship/journeys/$journeyId/stages');
      final data = response.data as List;
      return data.map((json) => DiscipleshipStage.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des étapes: $e');
    }
  }

  Future<DiscipleshipStage> getStage(int id) async {
    try {
      final response = await _api.get('/discipleship/stages/$id');
      return DiscipleshipStage.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du chargement de l\'étape: $e');
    }
  }

  Future<DiscipleshipStage> createStage(DiscipleshipStage stage) async {
    try {
      final response = await _api.post('/discipleship/stages', data: stage.toJson());
      return DiscipleshipStage.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création: $e');
    }
  }

  // Progress
  Future<List<DiscipleProgress>> getProgress({
    int page = 0,
    int size = 20,
    int? journeyId,
    int? discipleId,
    ProgressStatus? status,
  }) async {
    try {
      final queryParams = <String, dynamic>{
        'page': page,
        'size': size,
        if (journeyId != null) 'journeyId': journeyId,
        if (discipleId != null) 'discipleId': discipleId,
        if (status != null) 'status': status.name,
      };
      final response = await _api.get('/discipleship/progress', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => DiscipleProgress.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement de la progression: $e');
    }
  }

  Future<DiscipleProgress> getProgressById(int id) async {
    try {
      final response = await _api.get('/discipleship/progress/$id');
      return DiscipleProgress.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du chargement de la progression: $e');
    }
  }

  Future<DiscipleProgress> createProgress(DiscipleProgress progress) async {
    try {
      final response = await _api.post('/discipleship/progress', data: progress.toJson());
      return DiscipleProgress.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création: $e');
    }
  }

  Future<DiscipleProgress> updateRequirementProgress(int progressId, int requirementId, RequirementStatus status, {String? evidence, String? notes, int? verifiedById}) async {
    try {
      final response = await _api.patch('/discipleship/progress/$progressId/requirements/$requirementId', data: {
        'status': status.name,
        if (evidence != null) 'evidence': evidence,
        if (notes != null) 'notes': notes,
        if (verifiedById != null) 'verifiedById': verifiedById,
      });
      return DiscipleProgress.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la mise à jour: $e');
    }
  }

  Future<DiscipleProgress> completeStage(int progressId, int stageId) async {
    try {
      final response = await _api.post('/discipleship/progress/$progressId/stages/$stageId/complete');
      return DiscipleProgress.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la complétion: $e');
    }
  }

  // Mentor Assignments
  Future<List<MentorAssignment>> getMentorAssignments({
    int page = 0,
    int size = 20,
    int? mentorId,
    int? discipleId,
    int? journeyId,
    AssignmentStatus? status,
  }) async {
    try {
      final queryParams = <String, dynamic>{
        'page': page,
        'size': size,
        if (mentorId != null) 'mentorId': mentorId,
        if (discipleId != null) 'discipleId': discipleId,
        if (journeyId != null) 'journeyId': journeyId,
        if (status != null) 'status': status.name,
      };
      final response = await _api.get('/discipleship/assignments', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => MentorAssignment.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des assignations: $e');
    }
  }

  Future<MentorAssignment> createAssignment(MentorAssignment assignment) async {
    try {
      final response = await _api.post('/discipleship/assignments', data: assignment.toJson());
      return MentorAssignment.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la création: $e');
    }
  }

  Future<MentorAssignment> endAssignment(int id) async {
    try {
      final response = await _api.post('/discipleship/assignments/$id/end');
      return MentorAssignment.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la fin: $e');
    }
  }

  // Meetings
  Future<List<MentorMeeting>> getMeetings({
    int page = 0,
    int size = 20,
    int? assignmentId,
    int? mentorId,
    DateTime? fromDate,
    DateTime? toDate,
    MeetingStatus? status,
  }) async {
    try {
      final queryParams = <String, dynamic>{
        'page': page,
        'size': size,
        if (assignmentId != null) 'assignmentId': assignmentId,
        if (mentorId != null) 'mentorId': mentorId,
        if (fromDate != null) 'fromDate': fromDate.toIso8601String(),
        if (toDate != null) 'toDate': toDate.toIso8601String(),
        if (status != null) 'status': status.name,
      };
      final response = await _api.get('/discipleship/meetings', queryParameters: queryParams);
      final data = response.data as List;
      return data.map((json) => MentorMeeting.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des réunions: $e');
    }
  }

  Future<MentorMeeting> scheduleMeeting(MentorMeeting meeting) async {
    try {
      final response = await _api.post('/discipleship/meetings', data: meeting.toJson());
      return MentorMeeting.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la planification: $e');
    }
  }

  Future<MentorMeeting> completeMeeting(int id, {String? notes, String? actionItems, String? nextSteps, int? durationMinutes}) async {
    try {
      final response = await _api.post('/discipleship/meetings/$id/complete', data: {
        'notes': notes,
        'actionItems': actionItems,
        'nextSteps': nextSteps,
        'durationMinutes': durationMinutes,
      });
      return MentorMeeting.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors de la complétion: $e');
    }
  }

  // Reports
  Future<DiscipleshipReport> getReport(int journeyId) async {
    try {
      final response = await _api.get('/discipleship/reports/$journeyId');
      return DiscipleshipReport.fromJson(response.data as Map<String, dynamic>);
    } catch (e) {
      throw Exception('Erreur lors du chargement du rapport: $e');
    }
  }

  Future<List<TopMentor>> getTopMentors(int journeyId, {int limit = 10}) async {
    try {
      final response = await _api.get('/discipleship/reports/$journeyId/top-mentors', queryParameters: {'limit': limit});
      final data = response.data as List;
      return data.map((json) => TopMentor.fromJson(json as Map<String, dynamic>)).toList();
    } catch (e) {
      throw Exception('Erreur lors du chargement des meilleurs mentors: $e');
    }
  }
}
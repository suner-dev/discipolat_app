import 'dart:collection';
import 'package:flutter_tts/flutter_tts.dart';

/// Moteur TTS mobile — flutter_tts avec file d'attente FIFO.
///
/// Gère :
/// - La synthèse vocale native (Android/iOS)
/// - Une file d'attente FIFO pour la lecture séquentielle
/// - La configuration voix (langue, vitesse, pitch)
/// - Le cycle de vie de l'application (pause en arrière-plan)
class TtsService {
  final FlutterTts _tts = FlutterTts();
  final Queue<String> _queue = Queue<String>();
  bool _isPlaying = false;
  bool _isInitialized = false;

  /// Callback appelé après chaque lecture complète.
  VoidCallback? onComplete;

  /// Initialise le moteur TTS.
  Future<void> initialize() async {
    if (_isInitialized) return;

    try {
      await _tts.setLanguage('fr-FR');
      await _tts.setSpeechRate(0.5);
      await _tts.setPitch(1.0);
      await _tts.setVolume(1.0);

      _tts.setCompletionHandler(() {
        _isPlaying = false;
        onComplete?.();
        _processQueue();
      });

      _tts.setErrorHandler((msg) {
        print('[TTS] Erreur: $msg');
        _isPlaying = false;
        _processQueue();
      });

      _tts.setCancelHandler(() {
        _isPlaying = false;
      });

      _tts.setPauseHandler(() {
        _isPlaying = false;
      });

      _tts.setContinueHandler(() {
        _isPlaying = true;
      });

      _isInitialized = true;
    } catch (e) {
      print('[TTS] Erreur initialisation: $e');
    }
  }

  /// Lit un texte vocalement (ajouté à la file d'attente).
  Future<void> speak(String text) async {
    if (text.trim().isEmpty) return;
    await initialize();

    _queue.add(text.trim());
    _processQueue();
  }

  /// Lit un texte en priorité (ajouté en tête de file).
  Future<void> speakPriority(String text) async {
    if (text.trim().isEmpty) return;
    await initialize();

    // Ajouter en tête de file
    final tempQueue = Queue<String>.from(_queue);
    _queue.clear();
    _queue.add(text.trim());
    _queue.addAll(tempQueue);

    // Interrompre la lecture en cours si nécessaire
    if (_isPlaying) {
      await stop();
    }
    _processQueue();
  }

  /// Arrête la lecture et vide la file.
  Future<void> stop() async {
    await _tts.stop();
    _queue.clear();
    _isPlaying = false;
  }

  /// Met en pause la lecture.
  Future<void> pause() async {
    await _tts.pause();
    _isPlaying = false;
  }

  /// Vide la file d'attente sans arrêter la lecture en cours.
  void clearQueue() {
    _queue.clear();
  }

  /// Retourne le nombre d'éléments en attente.
  int get queueLength => _queue.length;

  /// Vérifie si le moteur est en train de parler.
  bool get isPlaying => _isPlaying;

  /// Vérifie si le moteur est disponible.
  bool get isAvailable => _isInitialized;

  /// Configure la langue.
  Future<void> setLanguage(String lang) async {
    await _tts.setLanguage(lang);
  }

  /// Configure la vitesse de lecture (0.0 à 1.0).
  Future<void> setRate(double rate) async {
    await _tts.setSpeechRate(rate);
  }

  /// Configure le pitch (0.5 à 2.0).
  Future<void> setPitch(double pitch) async {
    await _tts.setPitch(pitch);
  }

  /// Libère les ressources.
  void dispose() {
    _tts.stop();
    _queue.clear();
    _isPlaying = false;
  }

  /// Traitement de la file d'attente FIFO.
  void _processQueue() {
    if (_isPlaying || _queue.isEmpty) return;

    final text = _queue.removeFirst();
    _isPlaying = true;

    try {
      _tts.speak(text);
    } catch (e) {
      print('[TTS] Erreur lecture: $e');
      _isPlaying = false;
      _processQueue();
    }
  }
}

/// Callback type pour les fonctions void.
typedef VoidCallback = void Function();

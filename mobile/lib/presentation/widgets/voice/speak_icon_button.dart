import 'package:flutter/material.dart';
import '../../../core/tts/tts_service.dart';

/// Bouton icône réutilisable pour déclencher la lecture vocale d'un texte.
///
/// Utilise flutter_tts pour la synthèse vocale native.
///
/// @example
/// ```dart
/// SpeakIconButton(text: "Bienvenue sur Discipolat")
/// SpeakIconButton(text: "Nouveau membre ajouté", size: 24, color: Colors.blue)
/// ```
class SpeakIconButton extends StatefulWidget {
  final String text;
  final double size;
  final Color? color;
  final TtsService? ttsService;

  const SpeakIconButton({
    super.key,
    required this.text,
    this.size = 20,
    this.color,
    this.ttsService,
  });

  @override
  State<SpeakIconButton> createState() => _SpeakIconButtonState();
}

class _SpeakIconButtonState extends State<SpeakIconButton> {
  late final TtsService _tts = widget.ttsService ?? TtsService();
  bool _isSpeaking = false;

  @override
  void initState() {
    super.initState();
    _tts.initialize();
  }

  Future<void> _handleSpeak() async {
    if (_isSpeaking) {
      await _tts.stop();
      if (mounted) setState(() => _isSpeaking = false);
      return;
    }

    if (mounted) setState(() => _isSpeaking = true);

    _tts.onComplete = () {
      if (mounted) setState(() => _isSpeaking = false);
    };

    await _tts.speak(widget.text);
  }

  @override
  Widget build(BuildContext context) {
    return IconButton(
      icon: Icon(
        _isSpeaking ? Icons.volume_off : Icons.volume_up,
        size: widget.size,
        color: widget.color ?? Theme.of(context).iconTheme.color?.withAlpha(150),
      ),
      onPressed: _handleSpeak,
      tooltip: _isSpeaking ? 'Arrêter' : 'Lire vocalement',
      padding: EdgeInsets.zero,
      constraints: const BoxConstraints(),
    );
  }
}

/// Widget compact avec bouton de lecture vocale intégré.
class SpeakableText extends StatelessWidget {
  final String text;
  final TextStyle? style;
  final TextAlign? textAlign;
  final int? maxLines;
  final TextOverflow? overflow;
  final TtsService? ttsService;

  const SpeakableText({
    super.key,
    required this.text,
    this.style,
    this.textAlign,
    this.maxLines,
    this.overflow,
    this.ttsService,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: Text(
            text,
            style: style,
            textAlign: textAlign,
            maxLines: maxLines,
            overflow: overflow,
          ),
        ),
        const SizedBox(width: 4),
        SpeakIconButton(
          text: text,
          size: 18,
          ttsService: ttsService,
        ),
      ],
    );
  }
}

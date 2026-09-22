import 'package:freezed_annotation/freezed_annotation.dart';

part 'typing_indicator.freezed.dart';
part 'typing_indicator.g.dart';

@freezed
class TypingIndicator with _$TypingIndicator {
  const factory TypingIndicator({
    required int conversationId,
    required int userId,
    required String userName,
    required bool isTyping,
  }) = _TypingIndicator;

  factory TypingIndicator.fromJson(Map<String, dynamic> json) => _$TypingIndicatorFromJson(json);
}

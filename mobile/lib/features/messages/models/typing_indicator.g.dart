// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'typing_indicator.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$TypingIndicatorImpl _$$TypingIndicatorImplFromJson(
        Map<String, dynamic> json) =>
    _$TypingIndicatorImpl(
      conversationId: (json['conversationId'] as num).toInt(),
      userId: (json['userId'] as num).toInt(),
      userName: json['userName'] as String,
      isTyping: json['isTyping'] as bool,
    );

Map<String, dynamic> _$$TypingIndicatorImplToJson(
        _$TypingIndicatorImpl instance) =>
    <String, dynamic>{
      'conversationId': instance.conversationId,
      'userId': instance.userId,
      'userName': instance.userName,
      'isTyping': instance.isTyping,
    };

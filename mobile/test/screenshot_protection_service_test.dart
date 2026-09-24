import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/screenshot_protection_service.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel('discipolat/secure_screen');
  final calls = <MethodCall>[];

  setUp(() {
    calls.clear();
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
      calls.add(call);
      return null;
    });
  });

  tearDown(() async {
    await ScreenshotProtectionService.disable();
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });

  test('uses the native FLAG_SECURE channel contract', () async {
    await ScreenshotProtectionService.enable();
    expect(ScreenshotProtectionService.isEnabled, isTrue);
    await ScreenshotProtectionService.disable();

    expect(ScreenshotProtectionService.isEnabled, isFalse);
    expect(calls.map((call) => call.method), [
      'setSecureFlag',
      'setSecureFlag',
    ]);
    expect(calls.first.arguments, {'enabled': true});
    expect(calls.last.arguments, {'enabled': false});
  });
}

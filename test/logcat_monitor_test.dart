import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:logcat_monitor/logcat_monitor.dart';

void main() {
  const MethodChannel channel = MethodChannel('logcat_monitor');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await LogcatMonitor.platformVersion, '42');
  });
}

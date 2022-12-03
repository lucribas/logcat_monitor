import 'dart:async';

import 'package:flutter/services.dart';

class LogcatMonitor {
  static const MethodChannel _channel =
      const MethodChannel('logcat_monitor/methods');
  static const EventChannel _stream =
      const EventChannel('logcat_monitor/events');
  static StreamSubscription _streamSubscription;

  static void addListen(void Function(dynamic) onData) {
    _streamSubscription = _stream.receiveBroadcastStream().listen(onData);
  }

  static void cancelListen() {
    _streamSubscription.cancel();
  }

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<bool> startMonitor(String options) async {
    bool result = await _channel.invokeMethod('startMonitor',
        <String, String>{'options': options});
    return result;
  }
}

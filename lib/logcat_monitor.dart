import 'dart:async';

import 'package:flutter/services.dart';

class LogcatMonitor {
  static const MethodChannel _channel =
      const MethodChannel('logcat_monitor/methods');
  static const EventChannel _stream =
      const EventChannel('logcat_monitor/events');
  static late StreamSubscription _streamSubscription;

  static void addListen(void Function(dynamic) onData) {
    _streamSubscription = _stream.receiveBroadcastStream().listen(onData);
  }

  static void cancelListen() {
    _streamSubscription.cancel();
  }

  static Future<bool?> startMonitor(String options) async {
    bool? result = await _channel
        .invokeMethod('startMonitor', <String, String>{'options': options});
    return result;
  }

  static Future<bool?> stopMonitor() async {
    bool? result = await _channel.invokeMethod('stopMonitor');
    return result;
  }

  static Future<String?> get getLogcatDump async {
    final String? result = await _channel
        .invokeMethod('runLogcat', <String, String>{'options': "-d"});
    return result;
  }

  static Future<String?> get clearLogcat async {
    final String? result = await _channel
        .invokeMethod('runLogcat', <String, String>{'options': "-c"});
    return result;
  }
}

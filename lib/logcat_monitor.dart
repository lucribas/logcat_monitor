
import 'dart:async';

import 'package:flutter/services.dart';

class LogcatMonitor {
  static const MethodChannel _channel =
      const MethodChannel('logcat_monitor');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}

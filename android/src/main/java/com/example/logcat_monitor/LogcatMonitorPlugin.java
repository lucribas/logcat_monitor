package com.example.logcat_monitor;

import androidx.annotation.NonNull;

import java.util.Date;
import java.text.SimpleDateFormat;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

public class LogcatMonitorPlugin implements FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler {

	private static final String TAG_NAME = "LogcatMonPlugin";

	private MethodChannel channel;
	private EventChannel eventChannel;
	private EventChannel.EventSink eventSink;
	private int count = 0;

	@Override
	public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
		channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "logcat_monitor/methods");
		channel.setMethodCallHandler(this);

		eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "logcat_monitor/events");
		eventChannel.setStreamHandler(this);
	}

	@Override
	public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
		if (call.method.equals("getPlatformVersion")) {
			result.success("Android " + android.os.Build.VERSION.RELEASE);
			if (eventSink != null) {
				count ++;
				String date = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss.SSS] ").format(new Date());
				eventSink.success(date + "Event received from native java " + count);
			}
		} else {
			result.notImplemented();
		}
	}

	@Override
	public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
		channel.setMethodCallHandler(null);
		eventChannel.setStreamHandler(null);
	}

	@Override
	public void onListen(Object arguments, EventChannel.EventSink eventSink) {
		this.eventSink = eventSink;
	}

	@Override
	public void onCancel(Object arguments) {
		this.eventSink = null;
	}
}

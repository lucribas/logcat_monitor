package com.example.logcat_monitor;

import androidx.annotation.NonNull;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

public class LogcatMonitorPlugin implements FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler {

	private static final String TAG_NAME = "LogcatMonPlugin";

	private Executor logcatExecutor = Executors.newSingleThreadExecutor();
	private Handler uiThreadHandler = new Handler(Looper.getMainLooper());
	private MethodChannel channel;
	private EventChannel eventChannel;
	private EventChannel.EventSink eventSink;
	private String logcatOptions;
	private Process logcatProcess;

	private long sleepIntervalThread = 1000;
	private long sleepTimeThread = 200;
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

		switch (call.method) {

			case "getPlatformVersion":
				Log.d(TAG_NAME, "run getPlatformVersion()");
				result.success("Android " + android.os.Build.VERSION.RELEASE);
				break;

			case "startMonitor":
				if (logcatProcess != null) {
					Log.d(TAG_NAME, "close previous Monitor");
					logcatProcess.destroyForcibly();
					logcatProcess = null;
				}
				Log.d(TAG_NAME, "run startMonitor()");
				String options = call.argument("options");
				if (options != null && options.length() > 0) {
					logcatOptions = options;
					Log.d(TAG_NAME, "logcatOptions=" + options);
				}
				logcatMonitorThread();
				result.success(true);
				break;

			default:
				result.notImplemented();
				return;
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

	public void logcatMonitorThread() {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				logcatMonitor();
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
			}
		}.execute();
	}

	public void logcatMonitor() {
		String logcatCmd = "logcat";
		try {
			if (logcatOptions != null && logcatOptions.length() > 0) {
				logcatCmd = "logcat " + logcatOptions;
			}
			Log.d(TAG_NAME, "run command: " + logcatCmd);
			logcatProcess = Runtime.getRuntime().exec(logcatCmd);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(logcatProcess.getInputStream()));
			// process.destroyForcibly();

			String line;
			long startTime = SystemClock.elapsedRealtime();
			while ((line = bufferedReader.readLine()) != null) {
				long timeInterval = SystemClock.elapsedRealtime() - startTime;
				if (timeInterval > sleepIntervalThread)
					Thread.sleep(sleepTimeThread);
				sendEvent(line);
				startTime = SystemClock.elapsedRealtime();
			}

		} catch (IOException e) {
			sendEvent("EXCEPTION" + e.toString());
		} catch (InterruptedException e) {
			sendEvent("EXCEPTION" + e.toString());
		}
		Log.d(TAG_NAME, "closed command: " + logcatCmd);
	}

	public void sendEvent(final String message) {
		uiThreadHandler.post(
				new Runnable() {
					@Override
					public void run() {
						eventSink.success(message);
					}
				});
	}

}

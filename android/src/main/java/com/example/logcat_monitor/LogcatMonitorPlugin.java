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
	private static final long sleepIntervalThread = 1000;
	private static final long sleepTimeThread = 200;

	private Executor logcatExecutor = Executors.newSingleThreadExecutor();
	private Handler uiThreadHandler = new Handler(Looper.getMainLooper());
	private MethodChannel channel;
	private EventChannel eventChannel;
	private EventChannel.EventSink eventSink;
	private Process logcatProcess;

	@Override
	public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
		channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "logcat_monitor/methods");
		channel.setMethodCallHandler(this);

		eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "logcat_monitor/events");
		eventChannel.setStreamHandler(this);
	}

	@Override
	public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
		String options;
		StringBuffer log = new StringBuffer();
		boolean previousIsRunning = (logcatProcess != null);

		options = call.argument("options");
		if (options == null || options.length() < 1) {
			options = "";
		}

		switch (call.method) {
			case "startMonitor":
				closePrevious();
				Log.d(TAG_NAME, "run startMonitor()");
				logcatMonitorThread(options);
				result.success(true);
				break;

			case "stopMonitor":
				Log.d(TAG_NAME, "run stopMonitor()");
				closePrevious();
				result.success(true);
				break;

			case "runLogcat":
				Log.d(TAG_NAME, "run runLogcat(" + options + ")");
				try {
					String cmd = "logcat " + options;
					Process process = Runtime.getRuntime().exec(cmd);
					BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
					String line;
					while ((line = bufferedReader.readLine()) != null) {
						log.append(line);
					}
				} catch (IOException e) {
					log.append("EXCEPTION" + e.toString());
				}
				result.success(log.toString());
				break;

			default:
				result.notImplemented();
				return;
		}
	}

	private void closePrevious() {
		if (logcatProcess != null) {
			logcatProcess.destroyForcibly();
			logcatProcess = null;
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

	public void logcatMonitorThread(final String logcatOptions) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				logcatMonitor(logcatOptions);
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
			}
		}.execute();
	}

	public void logcatMonitor(String logcatOptions) {
		String logcatCmd = "logcat " + logcatOptions;
		try {
			Log.d(TAG_NAME, "running command: " + logcatCmd);
			logcatProcess = Runtime.getRuntime().exec(logcatCmd);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(logcatProcess.getInputStream()));

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
			sendEvent("logcatMonitor interrupted");
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
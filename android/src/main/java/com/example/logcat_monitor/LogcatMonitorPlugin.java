package com.example.logcat_monitor;

import androidx.annotation.NonNull;
import android.os.Handler;
import android.os.Looper;
import android.os.AsyncTask;
import android.app.Activity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import android.util.Log;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.text.SimpleDateFormat;

// import com.google.android.gms.tasks.Tasks;

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
	private MethodChannel channel;
	private EventChannel eventChannel;
	private EventChannel.EventSink eventSink;
	private int count = 0;

	private Handler uiThreadHandler = new Handler(Looper.getMainLooper());

	@Override
	public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
		channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "logcat_monitor/methods");
		channel.setMethodCallHandler(this);

		eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "logcat_monitor/events");
		eventChannel.setStreamHandler(this);
	}

	@Override
	public void onMethodCall(@NonNull MethodCall call, final @NonNull Result result) {

		switch (call.method) {

			case "getPlatformVersion":
				result.success("Android " + android.os.Build.VERSION.RELEASE);
				break;

			case "testEvent":
				result.success(true);
				if (eventSink != null) {
					count++;
					String date = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss.SSS] ").format(new Date());
					eventSink.success(date + "Event received from native java " + count);
				}
				break;

			case "startMonitor":
				// Tasks.call(logcatExecutor, logcatMonitorThread);
				Log.d(TAG_NAME, "ANTES DO logcatMonitorThread");

				// runOnUiThread(() -> {
				// logcatMonitor();
				// // result.success(true);
				// });



				// runOnUiThread(new Runnable() {
				// @Override
				// public void run() {
				// logcatMonitor();
				// }
				// });

				// new Thread(new Runnable() {
				// public void run() {
				// try {
				// Thread.sleep(1000);
				// logcatMonitor();
				// } catch (InterruptedException e) {
				// e.printStackTrace();
				// }
				// // result.success("Good morning!");
				// }
				// }).start();

				// logcatMonitor();
				// logcatMonitorThread();
				logcatMonitorThread2();
				Log.d(TAG_NAME, "DEPOIS DO logcatMonitorThread");
				// result.success(true);
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
		Thread t = new Thread(new Runnable() {
			public void run() {
				new Handler(Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						logcatMonitor();
					}
				});
			}
		});
		t.start();
	}

	public void logcatMonitorThread2() {
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


	public void ppost(final String message) {
	uiThreadHandler.post(
		new Runnable() {
			@Override
			public void run() {
				eventSink.success(message);
			}
		});
	}

	public void logcatMonitor() {
		try {
			// String logcatCmd = "logcat | grep -i
			// 'flutter\\|FlutterMainActivity\\|AoaLibPlugin\\|BootImageRepository\\|DiagnosticRepository\\|UefiAccessory'";
			// String logcatCmd = "ls";
			String logcatCmd = "logcat";
			// String logcatCmd = "logcat -d";
			Process process = Runtime.getRuntime().exec(logcatCmd);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			String line;
			ppost("prepare..");
			while ((line = bufferedReader.readLine()) != null) {
				ppost(line);
				Log.d(TAG_NAME, "LINE===>" + line);
				Thread.sleep(10);
			}
			Thread.sleep(100);
			ppost("ended");

		} catch (IOException e) {
			ppost("EXCEPTION" + e.toString());
		} catch (InterruptedException e) {
			ppost("EXCEPTION" + e.toString());
		}

	}
}

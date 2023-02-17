package com.example.logcat_monitor

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.Executors

class LogcatMonitorPlugin : FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler {
    private val executor = Executors.newSingleThreadExecutor()
    private val uiThreadHandler = Handler(Looper.getMainLooper())
    private var channel: MethodChannel? = null
    private var eventChannel: EventChannel? = null
    private var eventSink: EventChannel.EventSink? = null
    private var logcatProcess: Process? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "logcat_monitor/methods")
        channel?.setMethodCallHandler(this)
        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "logcat_monitor/events")
        eventChannel?.setStreamHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        val log = StringBuffer()
        var options = call.argument<String>("options")
        if (options == null || options.isEmpty()) {
            options = ""
        }
        when (call.method) {
            "startMonitor" -> {
                closePrevious()
                Log.d(TAG_NAME, "run startMonitor()")
                logcatMonitorThread(options)
                result.success(true)
            }
            "stopMonitor" -> {
                Log.d(TAG_NAME, "run stopMonitor()")
                closePrevious()
                result.success(true)
            }
            "runLogcat" -> {
                Log.d(TAG_NAME, "run runLogcat($options)")
                try {
                    val cmd = "logcat $options"
                    val process = Runtime.getRuntime().exec(cmd)
                    val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
                    bufferedReader.forEachLine {
                        log.append(
                            """
                            $it
                            
                            """.trimIndent()
                        )
                    }
                } catch (e: IOException) {
                    log.append("EXCEPTION$e")
                }
                result.success(log.toString())
            }
            else -> {
                result.notImplemented()
                return
            }
        }
    }

    private fun closePrevious() {
        if (logcatProcess != null) {
            logcatProcess!!.destroyForcibly()
            logcatProcess = null
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel?.setMethodCallHandler(null)
        eventChannel?.setStreamHandler(null)
    }

    override fun onListen(arguments: Any?, eventSink: EventChannel.EventSink?) {
        this.eventSink = eventSink
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
    }

    private fun logcatMonitorThread(logcatOptions: String) {
        executor.execute {
            run {
                logcatMonitor(logcatOptions)
            }
        }
    }

    private fun logcatMonitor(logcatOptions: String) {
        val logcatCmd = "logcat $logcatOptions"
        try {
            Log.d(TAG_NAME, "running command: $logcatCmd")
            logcatProcess = Runtime.getRuntime().exec(logcatCmd)
            val bufferedReader = BufferedReader(InputStreamReader(logcatProcess!!.inputStream))
            var line: String?
            var startTime = SystemClock.elapsedRealtime()
            while (bufferedReader.readLine().also { line = it } != null) {
                val timeInterval = SystemClock.elapsedRealtime() - startTime
                if (timeInterval > sleepIntervalThread) Thread.sleep(sleepTimeThread)
                sendEvent(line)
                startTime = SystemClock.elapsedRealtime()
            }
        } catch (e: IOException) {
            sendEvent("EXCEPTION$e")
        } catch (e: InterruptedException) {
            sendEvent("logcatMonitor interrupted")
        }
        Log.d(TAG_NAME, "closed command: $logcatCmd")
    }

    private fun sendEvent(message: String?) {
        uiThreadHandler.post { eventSink?.success(message) }
    }

    companion object {
        private const val TAG_NAME = "LogcatMonPlugin"
        private const val sleepIntervalThread: Long = 1000
        private const val sleepTimeThread: Long = 200
    }
}
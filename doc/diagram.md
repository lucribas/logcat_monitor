


<!-- ## Logcat Monitor -->


```plantuml
scale 500 width
rectangle "Logcat Monitor" {


package package:logcat_monitor {

	component shell {
		card logcat
	}

	component LogcatMonitor {
		card logcat_monitor.dart
	}

	component LogcatMonitorPlugin {
		card LogcatMonitorPlugin.java
	}

	LogcatMonitorPlugin.java <--> logcat_monitor.dart : event/method channel
	LogcatMonitorPlugin.java <-> logcat : run\nbackground\n thread
}


package example {

	component Example {
		card main.dart
	}

	logcat_monitor.dart <--> main.dart : Stream
}

```


# CrashHandler Module - Android Crash Logging

A simple, modular crash logging utility for Android that saves crash stack traces to local files.

## Files to Copy

Copy these files to your project:

```
crashlog/
└── CrashHandler.java    # The main crash handler (change package name)
```

## Setup Instructions

### Step 1: Copy CrashHandler.java

Copy `CrashHandler.java` to your project and update the package name at the top of the file.

### Step 2: Create or Update Application Class

If you don't have an Application class, create one:

```java
package com.yourpackage;

import android.app.Application;
import com.yourpackage.crashlog.CrashHandler;

public class YourApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.init(this);
    }
}
```

### Step 3: Register in AndroidManifest.xml

Add the `android:name` attribute to your `<application>` tag:

```xml
<application
    android:name=".YourApplication"
    ... >
```

## Retrieving Crash Logs

### Option 1: ADB Command
```bash
adb pull /sdcard/Android/data/com.yourpackage/files/crash_logs/
```

### Option 2: Device File Manager
Navigate to:
```
Internal Storage/Android/data/com.yourpackage/files/crash_logs/
```

### Option 3: Programmatically in Your App
```java
// Get all crash log files
File[] logs = CrashHandler.getCrashLogFiles(context);

// Get crash log directory path
File dir = CrashHandler.getCrashLogDir(context);

// Clear all crash logs
int deleted = CrashHandler.clearCrashLogs(context);
```

## Example Crash Log Output

```
=====================================
       CRASH REPORT
=====================================

Time: 2026-02-27 14:30:45

--- DEVICE INFO ---
Device: Samsung SM-A525F
Brand: samsung
Android Version: 12
SDK Level: 31
Product: a52q

--- APP INFO ---
Package: com.epictech.treeinfo
Version Name: 1.0.3
Version Code: 5

--- STACK TRACE ---
java.lang.NullPointerException: Attempt to invoke virtual method 'java.lang.String java.lang.Object.toString()' on a null object reference
    at com.epictech.treeinfo.InfoActivity.updateUIWithTreeData(InfoActivity.java:245)
    at com.epictech.treeinfo.InfoActivity$1.onSuccess(InfoActivity.java:189)
    at com.epictech.treeinfo.InfoActivity.lambda$fetchTreeInfo$0(InfoActivity.java:372)
    ...
```

## API Reference

| Method | Description |
|--------|-------------|
| `CrashHandler.init(Context)` | Initialize crash handler (call in Application.onCreate) |
| `CrashHandler.getCrashLogDir(Context)` | Returns File pointing to crash log directory |
| `CrashHandler.getCrashLogFiles(Context)` | Returns array of crash log files |
| `CrashHandler.clearCrashLogs(Context)` | Deletes all crash logs, returns count deleted |

## Notes

- Crash logs are stored in the app's external files directory, so they persist across app restarts
- Files are named with timestamp: `crash_2026-02-27_14-30-45.txt`
- No external dependencies required
- Works offline
- Does not interfere with normal crash behavior (app still crashes after logging)

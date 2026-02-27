package com.epictech.treeinfo.crashlog;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * CrashHandler - A modular crash logging utility for Android
 * 
 * USAGE:
 * 1. Copy this file to your project
 * 2. In your Application class onCreate(): CrashHandler.init(this);
 * 3. Crash logs will be saved to: /sdcard/Android/data/{package}/files/crash_logs/
 * 
 * RETRIEVE LOGS:
 * - Via ADB: adb pull /sdcard/Android/data/{package}/files/crash_logs/
 * - Via File Manager on device
 * - Via CrashHandler.getCrashLogDir(context) in your app
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "CrashHandler";
    private static final String CRASH_LOG_FOLDER = "crash_logs";
    
    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultHandler;

    private CrashHandler(Context context) {
        this.context = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    /**
     * Initialize the crash handler. Call this in Application.onCreate()
     * @param context Application context
     */
    public static void init(Context context) {
        CrashHandler handler = new CrashHandler(context);
        Thread.setDefaultUncaughtExceptionHandler(handler);
        Log.i(TAG, "CrashHandler initialized. Logs will be saved to: " + getCrashLogDir(context));
    }

    /**
     * Get the directory where crash logs are stored
     * @param context Application context
     * @return File object pointing to crash log directory
     */
    public static File getCrashLogDir(Context context) {
        File dir = new File(context.getExternalFilesDir(null), CRASH_LOG_FOLDER);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Get all crash log files
     * @param context Application context
     * @return Array of crash log files, or empty array if none
     */
    public static File[] getCrashLogFiles(Context context) {
        File dir = getCrashLogDir(context);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
        return files != null ? files : new File[0];
    }

    /**
     * Delete all crash logs
     * @param context Application context
     * @return Number of files deleted
     */
    public static int clearCrashLogs(Context context) {
        File[] files = getCrashLogFiles(context);
        int deleted = 0;
        for (File file : files) {
            if (file.delete()) deleted++;
        }
        return deleted;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        try {
            saveCrashLog(throwable);
        } catch (Exception e) {
            Log.e(TAG, "Failed to save crash log", e);
        }

        // Call the default handler to let the app crash normally
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, throwable);
        } else {
            System.exit(1);
        }
    }

    private void saveCrashLog(Throwable throwable) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date());
        String filename = "crash_" + timestamp + ".txt";
        
        File crashDir = getCrashLogDir(context);
        File crashFile = new File(crashDir, filename);

        StringBuilder sb = new StringBuilder();
        
        // Header
        sb.append("=====================================\n");
        sb.append("       CRASH REPORT\n");
        sb.append("=====================================\n\n");

        // Timestamp
        String readableTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        sb.append("Time: ").append(readableTime).append("\n\n");

        // Device Info
        sb.append("--- DEVICE INFO ---\n");
        sb.append("Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
        sb.append("Brand: ").append(Build.BRAND).append("\n");
        sb.append("Android Version: ").append(Build.VERSION.RELEASE).append("\n");
        sb.append("SDK Level: ").append(Build.VERSION.SDK_INT).append("\n");
        sb.append("Product: ").append(Build.PRODUCT).append("\n\n");

        // App Info
        sb.append("--- APP INFO ---\n");
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            sb.append("Package: ").append(context.getPackageName()).append("\n");
            sb.append("Version Name: ").append(pInfo.versionName).append("\n");
            sb.append("Version Code: ").append(pInfo.versionCode).append("\n");
        } catch (PackageManager.NameNotFoundException e) {
            sb.append("Package: ").append(context.getPackageName()).append("\n");
            sb.append("Version: Unknown\n");
        }
        sb.append("\n");

        // Stack Trace
        sb.append("--- STACK TRACE ---\n");
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        sb.append(sw.toString());
        sb.append("\n");

        // Cause chain (if any)
        Throwable cause = throwable.getCause();
        while (cause != null) {
            sb.append("\n--- CAUSED BY ---\n");
            sw = new StringWriter();
            pw = new PrintWriter(sw);
            cause.printStackTrace(pw);
            sb.append(sw.toString());
            cause = cause.getCause();
        }

        // Write to file
        try (FileOutputStream fos = new FileOutputStream(crashFile)) {
            fos.write(sb.toString().getBytes());
            fos.flush();
            Log.i(TAG, "Crash log saved to: " + crashFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Failed to write crash log", e);
        }
    }
}

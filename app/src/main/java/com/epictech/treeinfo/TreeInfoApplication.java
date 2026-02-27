package com.epictech.treeinfo;

import android.app.Application;

import com.epictech.treeinfo.crashlog.CrashHandler;

/**
 * TreeInfoApplication - Main Application class
 * 
 * Initializes crash logging on app startup.
 */
public class TreeInfoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize crash handler - logs will be saved to external files directory
        CrashHandler.init(this);
    }
}

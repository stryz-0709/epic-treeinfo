# UpdateManager Module - Google Drive APK Updates for Android

A modular, configurable Google Drive-based APK update utility for Android apps.

## Features

- Check for updates from a Google Drive folder
- Download APK with progress dialog
- Automatic installation prompt
- Builder pattern for easy configuration
- Optional callbacks for custom handling
- No hardcoded values - fully configurable

## Files to Copy

Copy this folder to your project:

```
updatemanager/
└── UpdateManager.java    # The main update manager (change package name)
```

## Prerequisites

### 1. Google Drive API Dependency

In your `build.gradle`:
```gradle
implementation 'com.google.apis:google-api-services-drive:v3-rev197-1.25.0'
implementation 'com.google.api-client:google-api-client-android:1.33.0'
```

### 2. FileProvider Configuration

In `AndroidManifest.xml`:
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

In `res/xml/file_paths.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-files-path name="downloads" path="Download/" />
</paths>
```

### 3. Permissions

In `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

### 4. APK Naming Convention

Upload APK files to your Drive folder with this naming pattern:
```
{prefix}{version}.apk
```

Examples:
- `myapp_v1.apk`
- `myapp_v2.apk`
- `myapp_v12.apk`

The version number is extracted from the filename and compared with `VERSION_CODE`.

## Usage

### Basic Usage

```java
// Get Drive folder ID from URL: https://drive.google.com/drive/folders/{FOLDER_ID}
private static final String UPDATE_FOLDER_ID = "1SIh7bECh-9mHKC4lPsOjOg_FnmgKj1sr";

new UpdateManager.Builder(activity, driveService)
    .setFolderId(UPDATE_FOLDER_ID)
    .setFilePrefix("myapp_v")
    .setCurrentVersionCode(BuildConfig.VERSION_CODE)
    .build()
    .checkForUpdates();
```

### Silent Check (No "Already Latest" Toast)

```java
new UpdateManager.Builder(activity, driveService)
    .setFolderId(UPDATE_FOLDER_ID)
    .setFilePrefix("myapp_v")
    .setCurrentVersionCode(BuildConfig.VERSION_CODE)
    .build()
    .checkForUpdates(false);  // false = don't show message if already latest
```

### With Callback

```java
new UpdateManager.Builder(activity, driveService)
    .setFolderId(UPDATE_FOLDER_ID)
    .setFilePrefix("myapp_v")
    .setCurrentVersionCode(BuildConfig.VERSION_CODE)
    .setCallback(new UpdateManager.UpdateCallback() {
        @Override
        public void onUpdateAvailable(int newVersion, String fileName) {
            Log.d("Update", "New version available: v" + newVersion);
        }

        @Override
        public void onNoUpdateAvailable() {
            Log.d("Update", "Already on latest version");
        }

        @Override
        public void onDownloadComplete(File apkFile) {
            Log.d("Update", "Downloaded to: " + apkFile.getPath());
        }

        @Override
        public void onError(Exception e) {
            Log.e("Update", "Error: " + e.getMessage());
        }
    })
    .build()
    .checkForUpdates();
```

## Configuration Options

| Method | Required | Description |
|--------|----------|-------------|
| `setFolderId(String)` | Yes | Google Drive folder ID containing APKs |
| `setFilePrefix(String)` | No | Filename prefix (default: "app_v") |
| `setCurrentVersionCode(int)` | No | Current app version (default: 1) |
| `setCallback(UpdateCallback)` | No | Callback for update events |

## How It Works

1. **Check**: Queries Drive folder for APK files matching the prefix
2. **Compare**: Extracts version from filename (e.g., "v12" → 12) and compares with current
3. **Dialog**: Shows update dialog if newer version found
4. **Download**: Downloads APK with progress indicator
5. **Install**: Opens system installer via FileProvider

## Google Drive Setup

1. Create a folder in Google Drive
2. Upload APK files with naming convention: `{prefix}{version}.apk`
3. Get folder ID from URL: `https://drive.google.com/drive/folders/{FOLDER_ID}`
4. Share folder with your service account email (if using service account auth)

## Example Flow

```
User opens app
    ↓
checkForUpdates() called
    ↓
Queries: 'FOLDER_ID' in parents and name contains 'myapp_v' and name contains '.apk'
    ↓
Found: myapp_v15.apk (VERSION_CODE = 12)
    ↓
15 > 12 → Show update dialog
    ↓
User taps "Update"
    ↓
Download with progress bar
    ↓
Open system installer
```

## Notes

- APK files should be directly in the folder (not in subfolders)
- Only the latest APK (by creation time) is considered
- Requires authenticated Drive service instance
- Works with both OAuth and Service Account authentication

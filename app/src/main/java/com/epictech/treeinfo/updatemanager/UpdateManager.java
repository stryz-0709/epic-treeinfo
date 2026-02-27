package com.epictech.treeinfo.updatemanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * UpdateManager - A modular Google Drive-based APK update utility for Android
 * 
 * USAGE:
 * 1. Copy this file to your project
 * 2. Configure with Builder pattern
 * 3. Call checkForUpdates()
 * 
 * EXAMPLE:
 * new UpdateManager.Builder(activity, driveService)
 *     .setFolderId("YOUR_DRIVE_FOLDER_ID")
 *     .setFilePrefix("myapp_v")
 *     .setCurrentVersionCode(BuildConfig.VERSION_CODE)
 *     .build()
 *     .checkForUpdates();
 * 
 * REQUIREMENTS:
 * - Google Drive API dependency
 * - FileProvider configured in AndroidManifest
 * - REQUEST_INSTALL_PACKAGES permission for Android 8+
 */
public class UpdateManager {

    private static final String TAG = "UpdateManager";

    private final Activity activity;
    private final Drive driveService;
    private final String folderId;
    private final String filePrefix;
    private final int currentVersionCode;
    private final UpdateCallback callback;

    private UpdateManager(Builder builder) {
        this.activity = builder.activity;
        this.driveService = builder.driveService;
        this.folderId = builder.folderId;
        this.filePrefix = builder.filePrefix;
        this.currentVersionCode = builder.currentVersionCode;
        this.callback = builder.callback;
    }

    /**
     * Check for updates on Google Drive
     */
    public void checkForUpdates() {
        checkForUpdates(true);
    }

    /**
     * Check for updates on Google Drive
     * @param showNoUpdateMessage Whether to show toast when already on latest version
     */
    public void checkForUpdates(boolean showNoUpdateMessage) {
        new Thread(() -> {
            try {
                // Find the latest APK file in the specific folder
                String apkQuery = "'" + folderId + "' in parents and name contains '" + filePrefix + "' and name contains '.apk' and trashed=false";
                FileList apkResult = driveService.files().list()
                        .setQ(apkQuery)
                        .setFields("files(id, name, size, createdTime)")
                        .setOrderBy("createdTime desc")
                        .execute();

                List<File> files = apkResult.getFiles();
                if (files == null || files.isEmpty()) {
                    if (showNoUpdateMessage) {
                        activity.runOnUiThread(() -> 
                            Toast.makeText(activity, "Không tìm thấy file cập nhật.", Toast.LENGTH_SHORT).show()
                        );
                    }
                    if (callback != null) callback.onNoUpdateAvailable();
                    return;
                }

                File latestApk = files.get(0);
                int remoteVersion = extractVersion(latestApk.getName());

                Log.d(TAG, "Remote version: " + remoteVersion + ", Current version: " + currentVersionCode);

                if (remoteVersion > currentVersionCode) {
                    activity.runOnUiThread(() -> showUpdateDialog(latestApk, remoteVersion));
                    if (callback != null) callback.onUpdateAvailable(remoteVersion, latestApk.getName());
                } else {
                    if (showNoUpdateMessage) {
                        activity.runOnUiThread(() -> 
                            Toast.makeText(activity, "Phiên bản hiện tại (v" + currentVersionCode + ") là mới nhất.", Toast.LENGTH_SHORT).show()
                        );
                    }
                    if (callback != null) callback.onNoUpdateAvailable();
                }

            } catch (Exception e) {
                Log.e(TAG, "Error checking for updates", e);
                activity.runOnUiThread(() -> 
                    Toast.makeText(activity, "Lỗi kiểm tra cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
                if (callback != null) callback.onError(e);
            }
        }).start();
    }

    /**
     * Extract version number from filename
     * E.g., "myapp_v12.apk" returns 12
     */
    private int extractVersion(String filename) {
        Pattern p = Pattern.compile("v(\\d+)");
        Matcher m = p.matcher(filename);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 0;
    }

    private void showUpdateDialog(File apkFile, int newVersion) {
        new AlertDialog.Builder(activity)
                .setTitle("Cập nhật mới")
                .setMessage("Đã có phiên bản mới v" + newVersion + " (" + apkFile.getName() + ").\n\nBạn có muốn tải về và cài đặt không?")
                .setPositiveButton("Cập nhật", (d, w) -> downloadAndInstall(apkFile))
                .setNegativeButton("Để sau", null)
                .show();
    }

    private void downloadAndInstall(File apkFile) {
        ProgressDialog pd = new ProgressDialog(activity);
        pd.setMessage("Đang tải xuống...");
        pd.setIndeterminate(false);
        pd.setMax(100);
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            try {
                java.io.File path = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                java.io.File file = new java.io.File(path, "update.apk");

                InputStream is = driveService.files().get(apkFile.getId()).executeMediaAsInputStream();
                OutputStream os = new FileOutputStream(file);

                byte[] buffer = new byte[8192];
                int length;
                long total = 0;
                long fileSize = apkFile.getSize() != null ? apkFile.getSize() : 10000000;

                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                    total += length;
                    int progress = (int) (total * 100 / fileSize);
                    activity.runOnUiThread(() -> pd.setProgress(progress));
                }

                os.flush();
                os.close();
                is.close();

                activity.runOnUiThread(() -> {
                    pd.dismiss();
                    installApk(file);
                });

                if (callback != null) callback.onDownloadComplete(file);

            } catch (Exception e) {
                Log.e(TAG, "Error downloading update", e);
                activity.runOnUiThread(() -> {
                    pd.dismiss();
                    Toast.makeText(activity, "Lỗi tải xuống: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
                if (callback != null) callback.onError(e);
            }
        }).start();
    }

    private void installApk(java.io.File file) {
        try {
            Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error installing APK", e);
            Toast.makeText(activity, "Lỗi cài đặt: " + e.getMessage(), Toast.LENGTH_LONG).show();
            if (callback != null) callback.onError(e);
        }
    }

    /**
     * Callback interface for update events
     */
    public interface UpdateCallback {
        default void onUpdateAvailable(int newVersion, String fileName) {}
        default void onNoUpdateAvailable() {}
        default void onDownloadComplete(java.io.File apkFile) {}
        default void onError(Exception e) {}
    }

    /**
     * Builder for UpdateManager
     */
    public static class Builder {
        private final Activity activity;
        private final Drive driveService;
        private String folderId;
        private String filePrefix = "app_v";
        private int currentVersionCode = 1;
        private UpdateCallback callback;

        public Builder(Activity activity, Drive driveService) {
            this.activity = activity;
            this.driveService = driveService;
        }

        /**
         * Set the Google Drive folder ID containing APK files
         * @param folderId The folder ID from Drive URL
         */
        public Builder setFolderId(String folderId) {
            this.folderId = folderId;
            return this;
        }

        /**
         * Set the file prefix pattern (default: "app_v")
         * Files should be named like: prefix + version + ".apk"
         * E.g., "myapp_v12.apk" with prefix "myapp_v"
         */
        public Builder setFilePrefix(String filePrefix) {
            this.filePrefix = filePrefix;
            return this;
        }

        /**
         * Set current app version code (usually BuildConfig.VERSION_CODE)
         */
        public Builder setCurrentVersionCode(int versionCode) {
            this.currentVersionCode = versionCode;
            return this;
        }

        /**
         * Set callback for update events (optional)
         */
        public Builder setCallback(UpdateCallback callback) {
            this.callback = callback;
            return this;
        }

        public UpdateManager build() {
            if (folderId == null || folderId.isEmpty()) {
                throw new IllegalStateException("Folder ID must be set");
            }
            return new UpdateManager(this);
        }
    }
}

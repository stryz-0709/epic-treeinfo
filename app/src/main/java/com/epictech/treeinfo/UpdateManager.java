package com.epictech.treeinfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.epictech.treeinfo.BuildConfig; // Explicit Import

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateManager {

    private final Activity activity;
    private final Drive driveService;
    private static final String UPDATE_FOLDER_ID = "1SIh7bECh-9mHKC4lPsOjOg_FnmgKj1sr";

    public UpdateManager(Activity activity, Drive driveService) {
        this.activity = activity;
        this.driveService = driveService;
    }

    public void checkForUpdates() {
        new Thread(() -> {
            try {
                // Find the latest APK file in the specific folder
                // Name format expected: "cowinfo_v12.apk"
                String apkQuery = "'" + UPDATE_FOLDER_ID + "' in parents and name contains 'cowinfo_v' and name contains '.apk' and trashed=false";
                FileList apkResult = driveService.files().list()
                        .setQ(apkQuery)
                        .setOrderBy("createdTime desc") // Get newest first
                        .execute();

                List<File> files = apkResult.getFiles();
                if (files == null || files.isEmpty()) return;

                File latestApk = files.get(0);
                int remoteVersion = extractVersion(latestApk.getName());
                int currentVersion = BuildConfig.VERSION_CODE;

                Log.d("UpdateManager", "Remote: " + remoteVersion + ", Current: " + currentVersion + ", Pkg: " + BuildConfig.APPLICATION_ID);

                if (remoteVersion > currentVersion) {
                    activity.runOnUiThread(() -> showUpdateDialog(latestApk));
                } else {
                    activity.runOnUiThread(() -> Toast.makeText(activity, "Phiên bản hiện tại (v" + currentVersion + ") là mới nhất.", Toast.LENGTH_SHORT).show());
                }

            } catch (Exception e) {
                Log.e("UpdateManager", "Error checking for updates", e);
                activity.runOnUiThread(() -> Toast.makeText(activity, "Lỗi kiểm tra cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private int extractVersion(String filename) {
        // Regex to extract '12' from 'cowinfo_v12.apk'
        Pattern p = Pattern.compile("v(\\d+)");
        Matcher m = p.matcher(filename);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 0;
    }

    private void showUpdateDialog(File apkFile) {
        new AlertDialog.Builder(activity)
                .setTitle("Cập nhật mới")
                .setMessage("Đã có phiên bản mới (" + apkFile.getName() + "). Bạn có muốn tải về và cài đặt không?")
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

                byte[] buffer = new byte[1024];
                int length;
                long total = 0;
                long fileSize = apkFile.getSize() != null ? apkFile.getSize() : 10000000; // Approx if null

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

            } catch (Exception e) {
                activity.runOnUiThread(() -> {
                    pd.dismiss();
                    Toast.makeText(activity, "Lỗi tải xuống: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
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
            Toast.makeText(activity, "Lỗi cài đặt: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}

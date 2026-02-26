package com.epictech.treeinfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.content.res.AppCompatResources; // Import for AppCompatResources
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InfoActivity extends AppCompatActivity {
    private static final String TAG = "InfoActivity";

    // UI Elements
    private TextView treeIdTextView;
    private TextView speciesTextView;
    private TextView ageTextView;
    private TextView heightTextView;
    private TextView diameterTextView;
    private TextView canopyTextView;
    private TextView conditionTextView;
    private TextView latitudeTextView;
    private TextView longitudeTextView;
    private TextView createdAtTextView;
    private TextView lastReportedTextView;
    private TextView eventStatusTextView;
    private TextView snTextView;
    private TextView syncedAtTextView;
    private TextView nfcIdTextView;

    private Button editInfoButton;
    private Button addTreeButton;
    private Button linkTreeButton;
    private ProgressBar progressBar;
    private LinearLayout treeDetailsLayout;
    private LinearLayout notFoundLayout;
    private Button backButton;
    
    // Main Info Slots (View Only)
    private ImageView[] infoSlotImages = new ImageView[4];
    private TextView[] infoSlotPlaceholders = new TextView[4];
    
    private SwipeRefreshLayout swipeRefreshLayout;

    // Data
    private String currentNfcId;
    private List<Object> currentTreeData;
    private List<String> currentImageUrls = new ArrayList<>();

    // Google API Services
    private static final String SPREADSHEET_ID = "1uCnIkjx8GzFgOkzbIIHOgunalvm81zrZv0aidZaYgOk";
    private static final String APPLICATION_NAME = "TreeInfoApp";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Sheets sheetsService; // For Service Account

    // Column Indices (0-based)
    private static final int COL_TREE_ID = 0;
    private static final int COL_SPECIES = 1;
    private static final int COL_AGE = 2;
    private static final int COL_HEIGHT = 3;
    private static final int COL_DIAMETER = 4;
    private static final int COL_CANOPY = 5;
    private static final int COL_CONDITION = 6;
    private static final int COL_LATITUDE = 7;
    private static final int COL_LONGITUDE = 8;
    private static final int COL_IMAGES = 9;
    private static final int COL_CREATED_AT = 10;
    private static final int COL_LAST_REPORTED = 11;
    private static final int COL_EVENT_STATUS = 12;
    private static final int COL_SN = 13;
    private static final int COL_SYNCED_AT = 14;
    private static final int COL_NFC_ID = 15;
    private static final int TOTAL_COLUMNS = 16;

    // Listener Interfaces
    public interface OnDataFetchedListener { void onSuccess(List<List<Object>> data); void onFailure(Exception e); }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Thông tin Cây");
        }

        initializeUiElements();
        
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (currentNfcId != null) fetchTreeInfo(currentNfcId);
            else swipeRefreshLayout.setRefreshing(false);
        });

        // Initialize Sheets with Service Account
        initializeSheetsService();

        currentNfcId = getIntent().getStringExtra("NFC_SERIAL");
        if (currentNfcId != null && sheetsService != null) {
            fetchTreeInfo(currentNfcId);
        } else if (sheetsService == null) {
             Toast.makeText(this, "Không thể khởi tạo dịch vụ của Google.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Không có mã NFC được cung cấp.", Toast.LENGTH_SHORT).show();
            finish();
        }

        editInfoButton.setOnClickListener(v -> {
            Toast.makeText(InfoActivity.this, "Chức năng chỉnh sửa đang được cập nhật cho hệ thống cây.", Toast.LENGTH_SHORT).show();
        });

        addTreeButton.setOnClickListener(v -> {
            Toast.makeText(InfoActivity.this, "Chức năng thêm cây đang được cập nhật.", Toast.LENGTH_SHORT).show();
        });
        
        linkTreeButton.setOnClickListener(v -> {
            Toast.makeText(InfoActivity.this, "Chức năng liên kết đang được cập nhật.", Toast.LENGTH_SHORT).show();
        });
        
        backButton.setOnClickListener(v -> finish());
    }

    private void initializeUiElements() {
        treeIdTextView = findViewById(R.id.tv_tree_id);
        speciesTextView = findViewById(R.id.tv_species);
        ageTextView = findViewById(R.id.tv_age);
        heightTextView = findViewById(R.id.tv_height);
        diameterTextView = findViewById(R.id.tv_diameter);
        canopyTextView = findViewById(R.id.tv_canopy);
        conditionTextView = findViewById(R.id.tv_condition);
        latitudeTextView = findViewById(R.id.tv_latitude);
        longitudeTextView = findViewById(R.id.tv_longitude);
        createdAtTextView = findViewById(R.id.tv_created_at);
        lastReportedTextView = findViewById(R.id.tv_last_reported);
        eventStatusTextView = findViewById(R.id.tv_event_status);
        snTextView = findViewById(R.id.tv_sn);
        syncedAtTextView = findViewById(R.id.tv_synced_at);
        nfcIdTextView = findViewById(R.id.tv_nfc_id);

        editInfoButton = findViewById(R.id.btn_edit_info);
        addTreeButton = findViewById(R.id.btn_add_cow); // Kept ID same for simplicity
        linkTreeButton = findViewById(R.id.btn_link_cow); // Kept ID same for simplicity
        progressBar = findViewById(R.id.progress_bar);
        treeDetailsLayout = findViewById(R.id.cow_details_layout); // Kept ID same
        notFoundLayout = findViewById(R.id.not_found_layout);
        backButton = findViewById(R.id.btn_back);
        
        // Initialize Info Slots
        infoSlotImages[0] = findViewById(R.id.iv_info_slot_1);
        infoSlotImages[1] = findViewById(R.id.iv_info_slot_2);
        infoSlotImages[2] = findViewById(R.id.iv_info_slot_3);
        infoSlotImages[3] = findViewById(R.id.iv_info_slot_4);

        infoSlotPlaceholders[0] = findViewById(R.id.tv_info_placeholder_1);
        infoSlotPlaceholders[1] = findViewById(R.id.tv_info_placeholder_2);
        infoSlotPlaceholders[2] = findViewById(R.id.tv_info_placeholder_3);
        infoSlotPlaceholders[3] = findViewById(R.id.tv_info_placeholder_4);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_info);
        
        // Hide edit/add buttons for now as requested just to fetch data
        editInfoButton.setVisibility(View.GONE);
        addTreeButton.setVisibility(View.GONE);
        linkTreeButton.setVisibility(View.GONE);
    }

    private void initializeSheetsService() {
        try {
            InputStream credentialsStream = getAssets().open("credentials.json");
            GoogleCredential credential = GoogleCredential.fromStream(credentialsStream)
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

            sheetsService = new Sheets.Builder(
                    new NetHttpTransport(),
                    JacksonFactory.getDefaultInstance(),
                    credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (IOException e) {
            Log.e(TAG, "Error initializing Sheets service", e);
            Toast.makeText(this, "Lỗi tải thông tin xác thực cho Sheets.", Toast.LENGTH_LONG).show();
        }
    }

    private GlideUrl getAuthGlideUrl(String url) {
        if (url == null) return null;
        String token = EarthRangerAuth.getCachedToken();
        if (token == null) return new GlideUrl(url);
        
        String auth = "Bearer " + token;
        return new GlideUrl(url, new LazyHeaders.Builder()
                .addHeader("Authorization", auth)
                .build());
    }

    private void updateUIWithTreeData(List<Object> treeData) {
        treeIdTextView.setText(treeData.size() > COL_TREE_ID ? treeData.get(COL_TREE_ID).toString() : "N/A");
        speciesTextView.setText(treeData.size() > COL_SPECIES ? treeData.get(COL_SPECIES).toString() : "N/A");
        ageTextView.setText(treeData.size() > COL_AGE ? treeData.get(COL_AGE).toString() : "N/A");
        heightTextView.setText(treeData.size() > COL_HEIGHT ? treeData.get(COL_HEIGHT).toString() : "N/A");
        diameterTextView.setText(treeData.size() > COL_DIAMETER ? treeData.get(COL_DIAMETER).toString() : "N/A");
        canopyTextView.setText(treeData.size() > COL_CANOPY ? treeData.get(COL_CANOPY).toString() : "N/A");
        
        String condition = (treeData.size() > COL_CONDITION ? treeData.get(COL_CONDITION).toString() : "N/A");
        conditionTextView.setText(condition);
        if (condition.toLowerCase().contains("bad") || condition.toLowerCase().contains("tệ") || condition.toLowerCase().contains("xấu")) {
            conditionTextView.setBackgroundResource(R.drawable.bg_badge_red);
        } else {
            conditionTextView.setBackgroundResource(R.drawable.bg_badge_green);
        }

        latitudeTextView.setText(treeData.size() > COL_LATITUDE ? treeData.get(COL_LATITUDE).toString() : "N/A");
        longitudeTextView.setText(treeData.size() > COL_LONGITUDE ? treeData.get(COL_LONGITUDE).toString() : "N/A");
        createdAtTextView.setText(treeData.size() > COL_CREATED_AT ? treeData.get(COL_CREATED_AT).toString() : "N/A");
        lastReportedTextView.setText(treeData.size() > COL_LAST_REPORTED ? treeData.get(COL_LAST_REPORTED).toString() : "N/A");
        eventStatusTextView.setText(treeData.size() > COL_EVENT_STATUS ? treeData.get(COL_EVENT_STATUS).toString() : "N/A");
        snTextView.setText(treeData.size() > COL_SN ? treeData.get(COL_SN).toString() : "N/A");
        syncedAtTextView.setText(treeData.size() > COL_SYNCED_AT ? treeData.get(COL_SYNCED_AT).toString() : "N/A");
        nfcIdTextView.setText(treeData.size() > COL_NFC_ID ? treeData.get(COL_NFC_ID).toString() : "N/A");

        if (treeData.size() > COL_IMAGES && treeData.get(COL_IMAGES) != null && !treeData.get(COL_IMAGES).toString().isEmpty()) {
            String imagesStr = treeData.get(COL_IMAGES).toString();
            String[] urls = imagesStr.split("\n");
            
            currentImageUrls.clear();
            for (String url : urls) {
                String cleanUrl = url.trim().replace("\"", "");
                
                // If the URL in the spreadsheet literally starts with "GET /api...", clean it up
                if (cleanUrl.toUpperCase().startsWith("GET ")) {
                    cleanUrl = cleanUrl.substring(4).trim();
                }
                
                // Prepend base URL if it's a relative path
                if (cleanUrl.startsWith("/api/")) {
                    cleanUrl = "https://epictech.pamdas.org" + cleanUrl;
                }
                
                if (!cleanUrl.isEmpty()) {
                    currentImageUrls.add(cleanUrl);
                }
            }
            
            for (int i = 0; i < 4; i++) {
                if (i < currentImageUrls.size()) {
                    infoSlotImages[i].setVisibility(View.VISIBLE);
                    infoSlotPlaceholders[i].setVisibility(View.GONE);
                    GlideUrl glideUrl = getAuthGlideUrl(currentImageUrls.get(i));
                    Glide.with(this).load(glideUrl).into(infoSlotImages[i]);
                    final int listIndex = i;
                    infoSlotImages[i].setOnClickListener(v -> showFullscreenGallery(listIndex));
                } else {
                    infoSlotImages[i].setVisibility(View.GONE);
                    infoSlotPlaceholders[i].setVisibility(View.VISIBLE);
                    if (i == 0) infoSlotPlaceholders[0].setText("Chưa có ảnh chính");
                    else infoSlotPlaceholders[i].setText("Chưa có ảnh");
                }
            }
        } else {
            // Empty
            for(int i=0; i<4; i++) {
                infoSlotImages[i].setVisibility(View.GONE);
                infoSlotPlaceholders[i].setVisibility(View.VISIBLE);
                if (i == 0) infoSlotPlaceholders[0].setText("Chưa có ảnh chính");
                else infoSlotPlaceholders[i].setText("Chưa có ảnh");
            }
        }
    }

    private void showFullscreenGallery(int startIndex) {
        if (currentImageUrls.isEmpty()) return;
        
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_fullscreen_slider);
        
        ViewPager2 viewPager = dialog.findViewById(R.id.vp_fullscreen_slider);
        View closeButton = dialog.findViewById(R.id.btn_close_slider);
        
        FullscreenImageAdapter adapter = new FullscreenImageAdapter(this, currentImageUrls);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(4);
        viewPager.setCurrentItem(startIndex, false);
        
        closeButton.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    private void fetchTreeInfo(String nfcId) {
        progressBar.setVisibility(View.VISIBLE);
        treeDetailsLayout.setVisibility(View.GONE);
        editInfoButton.setVisibility(View.GONE);
        addTreeButton.setVisibility(View.GONE);
        linkTreeButton.setVisibility(View.GONE);
        notFoundLayout.setVisibility(View.GONE);

        getTreeDataByNfc(nfcId, new OnDataFetchedListener() {
            @Override
            public void onSuccess(List<List<Object>> data) {
                executor.execute(() -> {
                    // Pre-fetch token in background thread so it's ready for UI thread to use without blocking
                    EarthRangerAuth.getAccessToken();
                    
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);
                        if (data != null && !data.isEmpty()) {
                            currentTreeData = data.get(0);
                            
                            // Update currentNfcId to the real NFC ID from the sheet (in case we searched by Tree ID)
                            if (currentTreeData.size() > COL_NFC_ID) {
                                currentNfcId = currentTreeData.get(COL_NFC_ID).toString();
                            }

                            treeDetailsLayout.setVisibility(View.VISIBLE);
                            updateUIWithTreeData(currentTreeData);
                        } else {
                            notFoundLayout.setVisibility(View.VISIBLE);
                        }
                    });
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    Log.e(TAG, "Error fetching tree data: ", e);
                    notFoundLayout.setVisibility(View.VISIBLE);
                     Toast.makeText(InfoActivity.this, "Lấy dữ liệu cây thất bại.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    public void getTreeDataByNfc(String queryId, OnDataFetchedListener listener) {
        executor.execute(() -> {
            try {
                String range = "Tree!A:P";
                ValueRange response = sheetsService.spreadsheets().values().get(SPREADSHEET_ID, range).execute();
                List<List<Object>> values = response.getValues();
                if (values == null || values.isEmpty()) {
                    listener.onSuccess(null);
                    return;
                }
                for (List<Object> row : values) {
                    if (!row.isEmpty()) {
                        String rowTreeId = row.size() > COL_TREE_ID ? row.get(COL_TREE_ID).toString() : "";
                        String rowNfcId = row.size() > COL_NFC_ID ? row.get(COL_NFC_ID).toString() : "";
                        
                        // Check match for NFC ID or Tree ID
                        if (rowNfcId.equalsIgnoreCase(queryId) || rowTreeId.equalsIgnoreCase(queryId)) {
                            listener.onSuccess(Collections.singletonList(row));
                            return;
                        }
                    }
                }
                listener.onSuccess(null);
            } catch (Exception e) {
                listener.onFailure(e);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}

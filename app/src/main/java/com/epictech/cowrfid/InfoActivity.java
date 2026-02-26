package com.epictech.cowrfid;

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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
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
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
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
    private TextView idTextView;
    private TextView importDateTextView;
    private TextView dobTextView;
    private TextView weightTextView;
    private TextView healthTextView;
    private TextView statusTextView;
    
    // New Fields
    private TextView breedTextView;
    private TextView sexTextView;
    // Removed: prevWeightTextView
    // Removed: prevHealthTextView
    private TextView healthDetailTextView;
    private TextView pregTextView;
    // Removed: prevPregTextView
    private TextView pregDateTextView;
    private TextView numPregTextView;
    // Removed: prevPositionTextView
    private TextView healthDateTextView;

    private Button editInfoButton;
    private Button addCowButton;
    private Button linkCowButton;
    private ProgressBar progressBar;
    private LinearLayout cowDetailsLayout;
    private LinearLayout notFoundLayout;
    private Button backButton;
    
    // Main Info Slots (View Only)
    private ImageView[] infoSlotImages = new ImageView[4];
    private TextView[] infoSlotPlaceholders = new TextView[4];
    
    // Dialog Image Slots (Editable)
    private ImageView[] dialogSlotImages = new ImageView[4];
    private TextView[] dialogSlotPlaceholders = new TextView[4];
    private int currentSelectedSlot = 0; // 0 = None, 1-4 = Specific Slot

    private ImageView dialogCowPhotoImageView; // Legacy/unused but kept for safety if needed
    private TextView dialogCowPhotoPlaceholder;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Data
    private String currentEpc;
    private List<Object> currentCowData;
    private List<String> currentImageUrls = new ArrayList<>();
    private File[] cachedSlotFiles; // Cache for dialog
    private Uri selectedImageUri;
    private Uri cameraImageUri;

    // Google API Services
    private static final String SPREADSHEET_ID = "1erRn4acsoNmOWgSiKHpcCqeUZpyLh8hv3x7a3kMgoYI";
    private static final String DRIVE_FOLDER_ID = "1B8xnFvLFUO0s56Jn0uHqcAhhOi6Xnqsw"; // <-- IMPORTANT: REPLACE WITH YOUR FOLDER ID
    private static final String APPLICATION_NAME = "CowInfoApp";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Sheets sheetsService; // For Service Account
    private Drive driveService;    // For User Account (OAuth)

    // Request Codes
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAPTURE_IMAGE_REQUEST = 2;
    private static final int REQUEST_CODE_SIGN_IN = 3;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int LINK_COW_REQUEST_CODE = 4;

    // Column Indices (0-based)
    private static final int COL_EPC = 0;
    private static final int COL_ID = 1;
    private static final int COL_IMPORT_DATE = 2;
    private static final int COL_DOB = 3;
    private static final int COL_BREED = 4;
    private static final int COL_SEX = 5;
    private static final int COL_WEIGHT = 6;
    private static final int COL_PREV_WEIGHT = 7;
    private static final int COL_HEALTH = 8;
    private static final int COL_PREV_HEALTH = 9;
    private static final int COL_HEALTH_DETAIL = 10;
    private static final int COL_PREG = 11;
    private static final int COL_PREV_PREG = 12;
    private static final int COL_PREG_DATE = 13;
    private static final int COL_NUM_PREG = 14;
    private static final int COL_POSITION = 15;
    private static final int COL_PREV_POSITION = 16;
    private static final int COL_COW_IMAGE = 17;
    private static final int COL_HEALTH_DATE = 18;
    private static final int TOTAL_COLUMNS = 19;

    // Listener Interfaces
    public interface OnDataFetchedListener { void onSuccess(List<List<Object>> data); void onFailure(Exception e); }
    public interface OnDataUpdatedListener { void onSuccess(); void onFailure(Exception e); }
    public interface OnDataAppendedListener { void onSuccess(); void onFailure(Exception e); }
    public interface OnImageUploadedListener { void onSuccess(String imageUrl); void onFailure(Exception e); }
    public interface OnFolderReadyListener { void onSuccess(String folderId); void onFailure(Exception e); }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Thông tin Bò");
        }

        initializeUiElements();
        
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (currentEpc != null) fetchCowInfo(currentEpc);
            else swipeRefreshLayout.setRefreshing(false);
        });

        // Initialize Sheets with Service Account
        initializeSheetsService();
        
        // Check for existing Google Sign-In for Drive access (Images)
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            if (GoogleSignIn.hasPermissions(account, new Scope(DriveScopes.DRIVE))) {
                initializeDriveService(account);
            } else {
                requestDriveSignIn();
            }
        }

        currentEpc = getIntent().getStringExtra("EPC_SERIAL");
        if (currentEpc != null && sheetsService != null) {
            fetchCowInfo(currentEpc);
        } else if (sheetsService == null) {
             Toast.makeText(this, "Không thể khởi tạo dịch vụ của Google.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Không có mã EPC được cung cấp.", Toast.LENGTH_SHORT).show();
            finish();
        }

        editInfoButton.setOnClickListener(v -> {
            if (currentCowData != null) showEditCowDialog();
            else Toast.makeText(InfoActivity.this, "Chưa có dữ liệu bò để chỉnh sửa.", Toast.LENGTH_SHORT).show();
        });

        addCowButton.setOnClickListener(v -> showAddCowDialog());
        linkCowButton.setOnClickListener(v -> {
            Intent intent = new Intent(InfoActivity.this, LinkCowActivity.class);
            startActivityForResult(intent, LINK_COW_REQUEST_CODE);
        });
        backButton.setOnClickListener(v -> finish());
    }

    private void initializeUiElements() {
        idTextView = findViewById(R.id.id);
        importDateTextView = findViewById(R.id.import_date);
        dobTextView = findViewById(R.id.dob);
        weightTextView = findViewById(R.id.weight);
        healthTextView = findViewById(R.id.health);
        statusTextView = findViewById(R.id.status);
        
        // Initialize New Fields
        breedTextView = findViewById(R.id.breed);
        sexTextView = findViewById(R.id.sex);
        // Removed: prevWeightTextView
        // Removed: prevHealthTextView
        healthDetailTextView = findViewById(R.id.health_detail);
        pregTextView = findViewById(R.id.preg);
        // Removed: prevPregTextView
        pregDateTextView = findViewById(R.id.preg_date);
        numPregTextView = findViewById(R.id.num_preg);
        // Removed: prevPositionTextView
        healthDateTextView = findViewById(R.id.health_date);

        editInfoButton = findViewById(R.id.btn_edit_info);
        addCowButton = findViewById(R.id.btn_add_cow);
        linkCowButton = findViewById(R.id.btn_link_cow);
        progressBar = findViewById(R.id.progress_bar);
        cowDetailsLayout = findViewById(R.id.cow_details_layout);
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
    }

    private void handleSlotClick(int slotIndex) {
        currentSelectedSlot = slotIndex;
        handleImageUploadClick();
    }
    
    // ... (rest of methods until updateUIWithCowData) ...

    /**
     * Initializes the Sheets service using the service account credentials.
     * This allows background access to the spreadsheet.
     */
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

    /**
     * Initializes the Drive service using the user's signed-in account.
     * This is required to upload files to their personal Drive.
     */
    private void initializeDriveService(GoogleSignInAccount account) {
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                this, Collections.singleton(DriveScopes.DRIVE));
        credential.setSelectedAccount(account.getAccount());

        driveService = new Drive.Builder(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance(),
                credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Starts the Google Sign-In flow to get user permission for Google Drive.
     */
    private void requestDriveSignIn() {
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE))
                .build();
        GoogleSignInClient signInClient = GoogleSignIn.getClient(this, signInOptions);
        startActivityForResult(signInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    /**
     * Handles the result of the Google Sign-In flow.
     */
    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(googleAccount -> {
                    Log.d(TAG, "Signed in for Drive access as: " + googleAccount.getEmail());
                    initializeDriveService(googleAccount);
                    // Now that sign-in is complete, proceed with showing image source dialog
                    showImageSourceDialog();
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Unable to sign in for Drive.", exception);
                    Toast.makeText(this, "Đăng nhập Google Drive thất bại.", Toast.LENGTH_SHORT).show();
                });
    }

    private void handleImageUploadClick() {
        // Check if the user is already signed in
        GoogleSignInAccount lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (lastSignedInAccount == null) {
            // If not signed in, start the sign-in flow.
            // The result will trigger handleSignInResult, which will then call showImageSourceDialog.
            requestDriveSignIn();
        } else {
            // If already signed in, initialize the Drive service and show the dialog.
            if (driveService == null) {
                initializeDriveService(lastSignedInAccount);
            }
            showImageSourceDialog();
        }
    }

    private void updateUIWithCowData(List<Object> cowData) {
        // ID Badge - Set text directly, background is set in XML
        idTextView.setText(cowData.size() > COL_ID ? cowData.get(COL_ID).toString() : "N/A");

        breedTextView.setText(cowData.size() > COL_BREED ? cowData.get(COL_BREED).toString() : "N/A");
        
        String sex = cowData.size() > COL_SEX ? cowData.get(COL_SEX).toString() : "N/A";
        sexTextView.setText(sex);
        
        importDateTextView.setText(cowData.size() > COL_IMPORT_DATE ? cowData.get(COL_IMPORT_DATE).toString() : "N/A");
        dobTextView.setText(cowData.size() > COL_DOB ? cowData.get(COL_DOB).toString() : "N/A");
        weightTextView.setText((cowData.size() > COL_WEIGHT ? cowData.get(COL_WEIGHT).toString() : "N/A") + " KG");
        
        // Health Logic
        String health = (cowData.size() > COL_HEALTH ? cowData.get(COL_HEALTH).toString() : "N/A");
        healthTextView.setText(health);
        
        if (health.toLowerCase().contains("bệnh")) {
            healthTextView.setBackgroundResource(R.drawable.bg_badge_red);
        } else {
            healthTextView.setBackgroundResource(R.drawable.bg_badge_green);
        }

        // Logic for Health Details
        if (health.equalsIgnoreCase("Bệnh")) {
            if (healthDateTextView.getParent() instanceof View) ((View)healthDateTextView.getParent()).setVisibility(View.VISIBLE);
            if (healthDetailTextView.getParent() instanceof View) ((View)healthDetailTextView.getParent()).setVisibility(View.VISIBLE);
            
            healthDateTextView.setText(cowData.size() > COL_HEALTH_DATE ? cowData.get(COL_HEALTH_DATE).toString() : "N/A");
            healthDetailTextView.setText(cowData.size() > COL_HEALTH_DETAIL ? cowData.get(COL_HEALTH_DETAIL).toString() : "N/A");
        } else {
            if (healthDateTextView.getParent() instanceof View) ((View)healthDateTextView.getParent()).setVisibility(View.GONE);
            if (healthDetailTextView.getParent() instanceof View) ((View)healthDetailTextView.getParent()).setVisibility(View.GONE);
        }

        String preg = (cowData.size() > COL_PREG ? cowData.get(COL_PREG).toString() : "N/A");
        pregTextView.setText(preg);

        if ("Đực".equalsIgnoreCase(sex)) {
            // Hide all pregnancy details for Male
            if (pregTextView.getParent() instanceof View) ((View)pregTextView.getParent()).setVisibility(View.GONE);
            if (pregDateTextView.getParent() instanceof View) ((View)pregDateTextView.getParent()).setVisibility(View.GONE);
            if (numPregTextView.getParent() instanceof View) ((View)numPregTextView.getParent()).setVisibility(View.GONE);
        } else {
            // Show/Manage pregnancy details for Female
            if (pregTextView.getParent() instanceof View) ((View)pregTextView.getParent()).setVisibility(View.VISIBLE);
            if (numPregTextView.getParent() instanceof View) ((View)numPregTextView.getParent()).setVisibility(View.VISIBLE);

            if (preg.equalsIgnoreCase("Có")) {
                pregTextView.setBackgroundResource(R.drawable.bg_badge_green);
                
                if (pregDateTextView.getParent() instanceof View) ((View)pregDateTextView.getParent()).setVisibility(View.VISIBLE);
                pregDateTextView.setText(cowData.size() > COL_PREG_DATE ? cowData.get(COL_PREG_DATE).toString() : "N/A");
            } else {
                pregTextView.setBackgroundResource(R.drawable.bg_badge_gray);
                
                if (pregDateTextView.getParent() instanceof View) ((View)pregDateTextView.getParent()).setVisibility(View.GONE);
            }
        }

        numPregTextView.setText(cowData.size() > COL_NUM_PREG ? cowData.get(COL_NUM_PREG).toString() : "N/A");
        
        // Status/Position Logic with Badges
        String position = (cowData.size() > COL_POSITION ? cowData.get(COL_POSITION).toString() : "N/A");
        statusTextView.setText(position);
        
        if (position.toLowerCase().contains("trong chuồng")) {
            statusTextView.setBackgroundResource(R.drawable.bg_badge_green);
        } else {
            statusTextView.setBackgroundResource(R.drawable.bg_badge_orange);
        }

        if (cowData.size() > COL_COW_IMAGE && cowData.get(COL_COW_IMAGE) != null && !cowData.get(COL_COW_IMAGE).toString().isEmpty()) {
            String imageVal = cowData.get(COL_COW_IMAGE).toString();
            String folderId = extractFolderId(imageVal);
            
            if (folderId != null) {
                // New Logic: Fetch images from folder
                fetchCowImages(folderId);
            } else {
                // Old Logic: Single Image URL -> Slot 1
                infoSlotImages[0].setVisibility(View.VISIBLE);
                infoSlotPlaceholders[0].setVisibility(View.GONE);
                
                for(int i=1; i<4; i++) {
                    infoSlotImages[i].setVisibility(View.GONE);
                    infoSlotPlaceholders[i].setVisibility(View.VISIBLE);
                    infoSlotPlaceholders[i].setText("Chưa có ảnh");
                }
                
                currentImageUrls.clear();
                currentImageUrls.add(imageVal);
                
                Glide.with(this).load(imageVal).into(infoSlotImages[0]);
                infoSlotImages[0].setOnClickListener(v -> showFullscreenGallery(0));
            }
        } else {
            // No Image
            for(int i=0; i<4; i++) {
                infoSlotImages[i].setVisibility(View.GONE);
                infoSlotPlaceholders[i].setVisibility(View.VISIBLE);
                infoSlotPlaceholders[i].setText("Chưa có ảnh");
            }
        }
    }

    private boolean validateInput(EditText idInput, EditText importDateInput, EditText dobInput, EditText weightInput, 
                                  Spinner healthSpinner, EditText healthDetailInput, Spinner pregSpinner, EditText pregDateInput, EditText healthDateInput) {
        
        if (TextUtils.isEmpty(idInput.getText())) {
            idInput.setError("Vui lòng nhập ID");
            return false;
        }

        if (!TextUtils.isEmpty(importDateInput.getText()) && !isValidDate(importDateInput.getText().toString())) {
            importDateInput.setError("Ngày không hợp lệ (dd/MM/yyyy)");
            return false;
        }

        if (!TextUtils.isEmpty(dobInput.getText()) && !isValidDate(dobInput.getText().toString())) {
            dobInput.setError("Ngày không hợp lệ (dd/MM/yyyy)");
            return false;
        }

        if (healthDateInput.getVisibility() == View.VISIBLE && !TextUtils.isEmpty(healthDateInput.getText()) && !isValidDate(healthDateInput.getText().toString())) {
            healthDateInput.setError("Ngày không hợp lệ (dd/MM/yyyy)");
            return false;
        }

        if (pregDateInput.getVisibility() == View.VISIBLE && !TextUtils.isEmpty(pregDateInput.getText()) && !isValidDate(pregDateInput.getText().toString())) {
            pregDateInput.setError("Ngày không hợp lệ (dd/MM/yyyy)");
            return false;
        }

        return true;
    }

    private boolean isValidDate(String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        sdf.setLenient(false);
        try {
            sdf.parse(dateStr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void showEditCowDialog() {
        // ... (Dialog setup is the same)
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_cow, null);
        builder.setView(dialogView);

        // ... (Find views)
        TextView dialogEpcSerialTextView = dialogView.findViewById(R.id.tv_edit_epc_serial);
        EditText idEditText = dialogView.findViewById(R.id.et_edit_id);
        EditText importDateEditText = dialogView.findViewById(R.id.et_edit_import_date);
        EditText dobEditText = dialogView.findViewById(R.id.et_edit_dob);
        EditText breedEditText = dialogView.findViewById(R.id.et_edit_breed);
        Spinner sexSpinner = dialogView.findViewById(R.id.spinner_edit_sex);
        EditText weightInputEditText = dialogView.findViewById(R.id.et_edit_weight_input);
        Spinner healthSpinner = dialogView.findViewById(R.id.spinner_edit_health);
        EditText healthDetailEditText = dialogView.findViewById(R.id.et_edit_health_detail);
        TextView healthDetailLabel = dialogView.findViewById(R.id.tv_edit_health_detail_label);
        EditText healthDateEditText = dialogView.findViewById(R.id.et_edit_health_date);
        TextView healthDateLabel = dialogView.findViewById(R.id.tv_edit_health_date_label);
        TextView pregLabel = dialogView.findViewById(R.id.tv_edit_preg_label);
        Spinner pregSpinner = dialogView.findViewById(R.id.spinner_edit_preg);
        EditText pregDateEditText = dialogView.findViewById(R.id.et_edit_preg_date);
        TextView pregDateLabel = dialogView.findViewById(R.id.tv_edit_preg_date_label);
        TextView numPregLabel = dialogView.findViewById(R.id.tv_edit_num_preg_label);
        EditText numPregEditText = dialogView.findViewById(R.id.et_edit_num_preg);
        Spinner statusSpinner = dialogView.findViewById(R.id.spinner_edit_status);
        
        Button saveButton = dialogView.findViewById(R.id.btn_save_cow);
        Button cancelButton = dialogView.findViewById(R.id.btn_cancel_edit);
        // Initialize Dialog Slots
        dialogSlotImages[0] = dialogView.findViewById(R.id.iv_dialog_slot_1);
        dialogSlotImages[1] = dialogView.findViewById(R.id.iv_dialog_slot_2);
        dialogSlotImages[2] = dialogView.findViewById(R.id.iv_dialog_slot_3);
        dialogSlotImages[3] = dialogView.findViewById(R.id.iv_dialog_slot_4);

        dialogSlotPlaceholders[0] = dialogView.findViewById(R.id.tv_dialog_placeholder_1);
        dialogSlotPlaceholders[1] = dialogView.findViewById(R.id.tv_dialog_placeholder_2);
        dialogSlotPlaceholders[2] = dialogView.findViewById(R.id.tv_dialog_placeholder_3);
        dialogSlotPlaceholders[3] = dialogView.findViewById(R.id.tv_dialog_placeholder_4);

        for (int i = 0; i < 4; i++) {
            final int slotIndex = i + 1;
            View.OnClickListener listener = v -> handleSlotClick(slotIndex);
            dialogSlotImages[i].setOnClickListener(listener);
            dialogSlotPlaceholders[i].setOnClickListener(listener);
        }

        // Apply clear button functionality to dialog EditTexts
        addClearButton(idEditText);
        addClearButton(importDateEditText);
        addClearButton(dobEditText);
        addClearButton(breedEditText);
        addClearButton(weightInputEditText);
        addClearButton(healthDetailEditText);
        addClearButton(numPregEditText);
        addClearButton(healthDateEditText);
        addClearButton(pregDateEditText);
        
        // --- Spinners Setup ---
        
        // Status Spinner
        String[] statusItems = new String[]{"Trong chuồng", "Ngoài chuồng"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statusItems);
        statusSpinner.setAdapter(statusAdapter);

        // Sex Spinner
        String[] sexItems = new String[]{"Đực", "Cái"};
        ArrayAdapter<String> sexAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sexItems);
        sexSpinner.setAdapter(sexAdapter);

        sexSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedSex = parent.getItemAtPosition(position).toString();
                if (selectedSex.equals("Đực")) {
                    pregLabel.setVisibility(View.GONE);
                    pregSpinner.setVisibility(View.GONE);
                    pregDateLabel.setVisibility(View.GONE);
                    pregDateEditText.setVisibility(View.GONE);
                    numPregLabel.setVisibility(View.GONE);
                    numPregEditText.setVisibility(View.GONE);
                } else {
                    pregLabel.setVisibility(View.VISIBLE);
                    pregSpinner.setVisibility(View.VISIBLE);
                    numPregLabel.setVisibility(View.VISIBLE);
                    numPregEditText.setVisibility(View.VISIBLE);
                    
                    // Re-evaluate pregnancy date visibility based on current spinner selection
                    String selectedPreg = (pregSpinner.getSelectedItem() != null) ? pregSpinner.getSelectedItem().toString() : "Không";
                    if (selectedPreg.equals("Có")) {
                        pregDateLabel.setVisibility(View.VISIBLE);
                        pregDateEditText.setVisibility(View.VISIBLE);
                    } else {
                        pregDateLabel.setVisibility(View.GONE);
                        pregDateEditText.setVisibility(View.GONE);
                    }
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Initialize hidden state for pregnancy fields (default assumption, will be overridden by setSelection if data exists)
        pregLabel.setVisibility(View.GONE);
        pregSpinner.setVisibility(View.GONE);
        pregDateLabel.setVisibility(View.GONE);
        pregDateEditText.setVisibility(View.GONE);
        numPregLabel.setVisibility(View.GONE);
        numPregEditText.setVisibility(View.GONE);

        // Health Spinner
        String[] healthItems = new String[]{"Khỏe", "Bệnh", "Đã chết", "Đã thanh lý"};
        ArrayAdapter<String> healthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, healthItems);
        healthSpinner.setAdapter(healthAdapter);

        // Pregnancy Spinner
        String[] pregItems = new String[]{"Không", "Có"};
        ArrayAdapter<String> pregAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, pregItems);
        pregSpinner.setAdapter(pregAdapter);
        
        // ... (Populate fields)
        dialogEpcSerialTextView.setText("EPC: " + currentEpc);
        if (currentCowData.size() > COL_ID && currentCowData.get(COL_ID) != null) idEditText.setText(currentCowData.get(COL_ID).toString()); else idEditText.setText("");
        if (currentCowData.size() > COL_IMPORT_DATE && currentCowData.get(COL_IMPORT_DATE) != null) importDateEditText.setText(currentCowData.get(COL_IMPORT_DATE).toString()); else importDateEditText.setText("");
        if (currentCowData.size() > COL_DOB && currentCowData.get(COL_DOB) != null) dobEditText.setText(currentCowData.get(COL_DOB).toString()); else dobEditText.setText("");
        if (currentCowData.size() > COL_BREED && currentCowData.get(COL_BREED) != null) breedEditText.setText(currentCowData.get(COL_BREED).toString()); else breedEditText.setText("");
        if (currentCowData.size() > COL_SEX && currentCowData.get(COL_SEX) != null) {
            String val = currentCowData.get(COL_SEX).toString();
            int pos = sexAdapter.getPosition(val);
            if (pos >= 0) sexSpinner.setSelection(pos);
        }
        if (currentCowData.size() > COL_WEIGHT && currentCowData.get(COL_WEIGHT) != null) weightInputEditText.setText(currentCowData.get(COL_WEIGHT).toString()); else weightInputEditText.setText("");
        if (currentCowData.size() > COL_HEALTH && currentCowData.get(COL_HEALTH) != null) {
            String val = currentCowData.get(COL_HEALTH).toString();
            int pos = healthAdapter.getPosition(val);
            if (pos >= 0) healthSpinner.setSelection(pos);
        }
        if (currentCowData.size() > COL_HEALTH_DETAIL && currentCowData.get(COL_HEALTH_DETAIL) != null) healthDetailEditText.setText(currentCowData.get(COL_HEALTH_DETAIL).toString()); else healthDetailEditText.setText("");
        if (currentCowData.size() > COL_PREG && currentCowData.get(COL_PREG) != null) {
            String val = currentCowData.get(COL_PREG).toString();
            int pos = pregAdapter.getPosition(val);
            if (pos >= 0) pregSpinner.setSelection(pos);
        }
        if (currentCowData.size() > COL_PREG_DATE && currentCowData.get(COL_PREG_DATE) != null) pregDateEditText.setText(currentCowData.get(COL_PREG_DATE).toString()); else pregDateEditText.setText("");
        if (currentCowData.size() > COL_NUM_PREG && currentCowData.get(COL_NUM_PREG) != null) numPregEditText.setText(currentCowData.get(COL_NUM_PREG).toString()); else numPregEditText.setText("");
        if (currentCowData.size() > COL_POSITION && currentCowData.get(COL_POSITION) != null) {
            String status = currentCowData.get(COL_POSITION).toString();
            int spinnerPosition = statusAdapter.getPosition(status);
            if (spinnerPosition >= 0) {
                statusSpinner.setSelection(spinnerPosition);
            }
        }
        if (currentCowData.size() > COL_HEALTH_DATE && currentCowData.get(COL_HEALTH_DATE) != null) healthDateEditText.setText(currentCowData.get(COL_HEALTH_DATE).toString()); else healthDateEditText.setText("");
        
        if (currentCowData.size() > COL_COW_IMAGE && currentCowData.get(COL_COW_IMAGE) != null && !currentCowData.get(COL_COW_IMAGE).toString().isEmpty()) {
            String imageVal = currentCowData.get(COL_COW_IMAGE).toString();
            String folderId = extractFolderId(imageVal);
            if (folderId != null) {
                populateDialogSlots(folderId);
            } else {
                // Legacy
                if (dialogSlotImages[0] != null) {
                    dialogSlotImages[0].setVisibility(View.VISIBLE);
                    dialogSlotPlaceholders[0].setVisibility(View.GONE);
                    Glide.with(this).load(imageVal).into(dialogSlotImages[0]);
                }
            }
        } else {
            // Empty
            for(int i=0; i<4; i++) {
                if (dialogSlotImages[i] != null) dialogSlotImages[i].setVisibility(View.GONE);
                if (dialogSlotPlaceholders[i] != null) dialogSlotPlaceholders[i].setVisibility(View.VISIBLE);
            }
        }

        setupDatePicker(importDateEditText);
        setupDatePicker(dobEditText);
        setupDatePicker(healthDateEditText);
        setupDatePicker(pregDateEditText);

        // --- Dynamic Date Logic ---
        
        healthSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                String original = (currentCowData.size() > COL_HEALTH && currentCowData.get(COL_HEALTH) != null) ? currentCowData.get(COL_HEALTH).toString() : "";
                String todayDate = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date());
                
                // Logic for Health Date
                if (selected.equals("Đã chết")) {
                    healthDateLabel.setText("Ngày chết:");
                    healthDateLabel.setVisibility(View.VISIBLE);
                    healthDateEditText.setVisibility(View.VISIBLE);
                    if (!original.equals("Đã chết")) healthDateEditText.setText(todayDate);
                } else if (selected.equals("Đã thanh lý")) {
                    healthDateLabel.setText("Ngày thanh lý:");
                    healthDateLabel.setVisibility(View.VISIBLE);
                    healthDateEditText.setVisibility(View.VISIBLE);
                    if (!original.equals("Đã thanh lý")) healthDateEditText.setText(todayDate);
                } else if (selected.equals("Bệnh")) {
                    if (original.equalsIgnoreCase("Bệnh")) {
                        // Same state -> Hide Date
                        healthDateLabel.setVisibility(View.GONE);
                        healthDateEditText.setVisibility(View.GONE);
                    } else {
                        // Transition to Bệnh -> Show Date (Sick Date)
                        healthDateLabel.setText("Ngày bị bệnh:");
                        healthDateLabel.setVisibility(View.VISIBLE);
                        healthDateEditText.setVisibility(View.VISIBLE);
                        healthDateEditText.setText(todayDate);
                    }
                } else if (original.equalsIgnoreCase("Bệnh") && selected.equals("Khỏe")) {
                    healthDateLabel.setText("Ngày hết bệnh:");
                    healthDateLabel.setVisibility(View.VISIBLE);
                    healthDateEditText.setVisibility(View.VISIBLE);
                    healthDateEditText.setText(todayDate);
                } else {
                    healthDateLabel.setVisibility(View.GONE);
                    healthDateEditText.setVisibility(View.GONE);
                }
                
                // Logic for Health Detail
                if (selected.equals("Bệnh")) {
                    healthDetailLabel.setVisibility(View.VISIBLE);
                    healthDetailEditText.setVisibility(View.VISIBLE);
                } else {
                    healthDetailLabel.setVisibility(View.GONE);
                    healthDetailEditText.setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        pregSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                String original = (currentCowData.size() > COL_PREG && currentCowData.get(COL_PREG) != null) ? currentCowData.get(COL_PREG).toString() : "Không";
                String todayDate = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date());
                
                if (selected.equalsIgnoreCase(original)) {
                    pregDateLabel.setVisibility(View.GONE);
                    pregDateEditText.setVisibility(View.GONE);
                } else {
                    if (original.equalsIgnoreCase("Có") && selected.equals("Không")) {
                        pregDateLabel.setText("Ngày sinh con:");
                        pregDateLabel.setVisibility(View.VISIBLE);
                        pregDateEditText.setVisibility(View.VISIBLE);
                        pregDateEditText.setText(todayDate);
                    } else if (original.equalsIgnoreCase("Không") && selected.equals("Có")) {
                        pregDateLabel.setText("Ngày có thai:");
                        pregDateLabel.setVisibility(View.VISIBLE);
                        pregDateEditText.setVisibility(View.VISIBLE);
                        pregDateEditText.setText(todayDate);
                    } else {
                        pregDateLabel.setVisibility(View.GONE);
                        pregDateEditText.setVisibility(View.GONE);
                    }
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(d -> {
            for(int i=0; i<4; i++) {
                dialogSlotImages[i] = null;
                dialogSlotPlaceholders[i] = null;
            }
        });

        saveButton.setOnClickListener(v -> {
            if (!validateInput(idEditText, importDateEditText, dobEditText, weightInputEditText, 
                               healthSpinner, healthDetailEditText, pregSpinner, pregDateEditText, healthDateEditText)) {
                return;
            }

            List<Object> updatedData = new ArrayList<>(Collections.nCopies(TOTAL_COLUMNS, ""));
            // Preserve existing data first
            if (currentCowData != null) {
                for (int i = 0; i < currentCowData.size() && i < TOTAL_COLUMNS; i++) {
                     updatedData.set(i, currentCowData.get(i));
                }
            }
            
            updatedData.set(COL_EPC, currentEpc);
            updatedData.set(COL_ID, idEditText.getText().toString());
            updatedData.set(COL_IMPORT_DATE, importDateEditText.getText().toString());
            updatedData.set(COL_DOB, dobEditText.getText().toString());
            updatedData.set(COL_BREED, breedEditText.getText().toString());
            updatedData.set(COL_SEX, sexSpinner.getSelectedItem().toString());
            updatedData.set(COL_WEIGHT, TextUtils.isEmpty(weightInputEditText.getText()) ? "0" : weightInputEditText.getText().toString());
            updatedData.set(COL_HEALTH, healthSpinner.getSelectedItem().toString());
            if (healthSpinner.getSelectedItem().toString().equals("Bệnh")) {
                updatedData.set(COL_HEALTH_DETAIL, healthDetailEditText.getText().toString());
            } else {
                updatedData.set(COL_HEALTH_DETAIL, "");
            }
            updatedData.set(COL_PREG, pregSpinner.getSelectedItem().toString());
            
            String todayDate = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date());

            String pregDateVal = pregDateEditText.getText().toString();
            if (TextUtils.isEmpty(pregDateVal)) {
                pregDateVal = todayDate;
            }
            updatedData.set(COL_PREG_DATE, pregDateVal);

            updatedData.set(COL_NUM_PREG, TextUtils.isEmpty(numPregEditText.getText()) ? "0" : numPregEditText.getText().toString());
            updatedData.set(COL_POSITION, statusSpinner.getSelectedItem().toString());

            String healthDateVal = healthDateEditText.getText().toString();
            if (TextUtils.isEmpty(healthDateVal)) {
                 healthDateVal = todayDate;
            }
            updatedData.set(COL_HEALTH_DATE, healthDateVal);

            if (selectedImageUri != null) {
                uploadImageAndUpdateSheet(selectedImageUri, idEditText.getText().toString(), updatedData, new OnDataUpdatedListener() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            Toast.makeText(InfoActivity.this, "Dữ liệu bò đã được cập nhật thành công!", Toast.LENGTH_SHORT).show();
                            fetchCowInfo(currentEpc);
                            dialog.dismiss();
                        });
                    }
                    @Override
                    public void onFailure(Exception e) {
                        runOnUiThread(() -> {
                            Log.e(TAG, "Error updating cow data with image: ", e);
                            Toast.makeText(InfoActivity.this, "Cập nhật dữ liệu bò thất bại.", Toast.LENGTH_LONG).show();
                        });
                    }
                });
            } else {
                updateCowData(currentEpc, updatedData, new OnDataUpdatedListener() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            Toast.makeText(InfoActivity.this, "Dữ liệu bò đã được cập nhật thành công!", Toast.LENGTH_SHORT).show();
                            fetchCowInfo(currentEpc);
                            dialog.dismiss();
                        });
                    }
                    @Override
                    public void onFailure(Exception e) {
                        runOnUiThread(() -> {
                            Log.e(TAG, "Error updating cow data: ", e);
                            Toast.makeText(InfoActivity.this, "Cập nhật dữ liệu bò thất bại.", Toast.LENGTH_LONG).show();
                        });
                    }
                });
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showAddCowDialog() {
        // ... (Dialog setup is the same)
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_cow, null);
        builder.setView(dialogView);

        // ... (Find views)
        TextView dialogEpcSerialTextView = dialogView.findViewById(R.id.tv_edit_epc_serial);
        EditText idEditText = dialogView.findViewById(R.id.et_edit_id);
        EditText importDateEditText = dialogView.findViewById(R.id.et_edit_import_date);
        EditText dobEditText = dialogView.findViewById(R.id.et_edit_dob);
        EditText breedEditText = dialogView.findViewById(R.id.et_edit_breed);
        Spinner sexSpinner = dialogView.findViewById(R.id.spinner_edit_sex);
        EditText weightInputEditText = dialogView.findViewById(R.id.et_edit_weight_input);
        Spinner healthSpinner = dialogView.findViewById(R.id.spinner_edit_health);
        EditText healthDetailEditText = dialogView.findViewById(R.id.et_edit_health_detail);
        TextView healthDetailLabel = dialogView.findViewById(R.id.tv_edit_health_detail_label);
        EditText healthDateEditText = dialogView.findViewById(R.id.et_edit_health_date);
        TextView healthDateLabel = dialogView.findViewById(R.id.tv_edit_health_date_label);
        TextView pregLabel = dialogView.findViewById(R.id.tv_edit_preg_label);
        Spinner pregSpinner = dialogView.findViewById(R.id.spinner_edit_preg);
        EditText pregDateEditText = dialogView.findViewById(R.id.et_edit_preg_date);
        TextView pregDateLabel = dialogView.findViewById(R.id.tv_edit_preg_date_label);
        TextView numPregLabel = dialogView.findViewById(R.id.tv_edit_num_preg_label);
        EditText numPregEditText = dialogView.findViewById(R.id.et_edit_num_preg);
        Spinner statusSpinner = dialogView.findViewById(R.id.spinner_edit_status);
        
        Button saveButton = dialogView.findViewById(R.id.btn_save_cow);
        Button cancelButton = dialogView.findViewById(R.id.btn_cancel_edit);
        // Initialize Dialog Slots
        dialogSlotImages[0] = dialogView.findViewById(R.id.iv_dialog_slot_1);
        dialogSlotImages[1] = dialogView.findViewById(R.id.iv_dialog_slot_2);
        dialogSlotImages[2] = dialogView.findViewById(R.id.iv_dialog_slot_3);
        dialogSlotImages[3] = dialogView.findViewById(R.id.iv_dialog_slot_4);

        dialogSlotPlaceholders[0] = dialogView.findViewById(R.id.tv_dialog_placeholder_1);
        dialogSlotPlaceholders[1] = dialogView.findViewById(R.id.tv_dialog_placeholder_2);
        dialogSlotPlaceholders[2] = dialogView.findViewById(R.id.tv_dialog_placeholder_3);
        dialogSlotPlaceholders[3] = dialogView.findViewById(R.id.tv_dialog_placeholder_4);

        for (int i = 0; i < 4; i++) {
            final int slotIndex = i + 1;
            View.OnClickListener listener = v -> handleSlotClick(slotIndex);
            dialogSlotImages[i].setOnClickListener(listener);
            dialogSlotPlaceholders[i].setOnClickListener(listener);
        }

        // Apply clear button functionality to dialog EditTexts
        addClearButton(idEditText);
        addClearButton(importDateEditText);
        addClearButton(dobEditText);
        addClearButton(breedEditText);
        addClearButton(weightInputEditText);
        addClearButton(healthDetailEditText);
        addClearButton(numPregEditText);
        addClearButton(healthDateEditText);
        addClearButton(pregDateEditText);

        // --- Spinners Setup ---
        
        // Status Spinner
        String[] statusItems = new String[]{"Trong chuồng", "Ngoài chuồng"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statusItems);
        statusSpinner.setAdapter(statusAdapter);

        // Sex Spinner
        String[] sexItems = new String[]{"Đực", "Cái"};
        ArrayAdapter<String> sexAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sexItems);
        sexSpinner.setAdapter(sexAdapter);

        sexSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedSex = parent.getItemAtPosition(position).toString();
                if (selectedSex.equals("Đực")) {
                    pregLabel.setVisibility(View.GONE);
                    pregSpinner.setVisibility(View.GONE);
                    pregDateLabel.setVisibility(View.GONE);
                    pregDateEditText.setVisibility(View.GONE);
                    numPregLabel.setVisibility(View.GONE);
                    numPregEditText.setVisibility(View.GONE);
                } else {
                    pregLabel.setVisibility(View.VISIBLE);
                    pregSpinner.setVisibility(View.VISIBLE);
                    numPregLabel.setVisibility(View.VISIBLE);
                    numPregEditText.setVisibility(View.VISIBLE);
                    
                    // Re-evaluate pregnancy date visibility based on current spinner selection
                    String selectedPreg = (pregSpinner.getSelectedItem() != null) ? pregSpinner.getSelectedItem().toString() : "Không";
                    if (selectedPreg.equals("Có")) {
                        pregDateLabel.setVisibility(View.VISIBLE);
                        pregDateEditText.setVisibility(View.VISIBLE);
                    } else {
                        pregDateLabel.setVisibility(View.GONE);
                        pregDateEditText.setVisibility(View.GONE);
                    }
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Initialize hidden state for pregnancy fields (default assumption, will be overridden by setSelection if data exists)
        pregLabel.setVisibility(View.GONE);
        pregSpinner.setVisibility(View.GONE);
        pregDateLabel.setVisibility(View.GONE);
        pregDateEditText.setVisibility(View.GONE);
        numPregLabel.setVisibility(View.GONE);
        numPregEditText.setVisibility(View.GONE);

        // Health Spinner
        String[] healthItems = new String[]{"Khỏe", "Bệnh", "Đã chết", "Đã thanh lý"};
        ArrayAdapter<String> healthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, healthItems);
        healthSpinner.setAdapter(healthAdapter);

        // Pregnancy Spinner
        String[] pregItems = new String[]{"Không", "Có"};
        ArrayAdapter<String> pregAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, pregItems);
        pregSpinner.setAdapter(pregAdapter);

        // Hide conditional fields initially for Add Dialog
        healthDetailLabel.setVisibility(View.GONE);
        healthDetailEditText.setVisibility(View.GONE);
        healthDateLabel.setVisibility(View.GONE);
        healthDateEditText.setVisibility(View.GONE);
        
        // Default Sex is "Đực" so hide pregnancy fields
        pregLabel.setVisibility(View.GONE);
        pregSpinner.setVisibility(View.GONE);
        pregDateLabel.setVisibility(View.GONE);
        pregDateEditText.setVisibility(View.GONE);
        numPregLabel.setVisibility(View.GONE);
        numPregEditText.setVisibility(View.GONE);

        dialogEpcSerialTextView.setText("EPC: " + currentEpc);
        for(int i=0; i<4; i++) {
            if (dialogSlotImages[i] != null) dialogSlotImages[i].setVisibility(View.GONE);
            if (dialogSlotPlaceholders[i] != null) dialogSlotPlaceholders[i].setVisibility(View.VISIBLE);
        }

        setupDatePicker(importDateEditText);
        setupDatePicker(dobEditText);
        setupDatePicker(healthDateEditText);
        setupDatePicker(pregDateEditText);

        // --- Dynamic Date Logic ---
        
        healthSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                String todayDate = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date());
                
                if (selected.equals("Đã chết")) {
                    healthDateLabel.setText("Ngày chết:");
                    healthDateLabel.setVisibility(View.VISIBLE);
                    healthDateEditText.setVisibility(View.VISIBLE);
                    healthDateEditText.setText(todayDate);
                } else if (selected.equals("Đã thanh lý")) {
                    healthDateLabel.setText("Ngày thanh lý:");
                    healthDateLabel.setVisibility(View.VISIBLE);
                    healthDateEditText.setVisibility(View.VISIBLE);
                    healthDateEditText.setText(todayDate);
                } else if (selected.equals("Bệnh")) {
                    healthDateLabel.setText("Ngày bị bệnh:");
                    healthDateLabel.setVisibility(View.VISIBLE);
                    healthDateEditText.setVisibility(View.VISIBLE);
                    healthDateEditText.setText(todayDate);
                } else {
                    healthDateLabel.setVisibility(View.GONE);
                    healthDateEditText.setVisibility(View.GONE);
                }
                
                if (selected.equals("Bệnh")) {
                    healthDetailLabel.setVisibility(View.VISIBLE);
                    healthDetailEditText.setVisibility(View.VISIBLE);
                } else {
                    healthDetailLabel.setVisibility(View.GONE);
                    healthDetailEditText.setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        pregSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                String todayDate = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date());
                
                if (selected.equals("Có")) {
                    pregDateLabel.setText("Ngày có thai:");
                    pregDateLabel.setVisibility(View.VISIBLE);
                    pregDateEditText.setVisibility(View.VISIBLE);
                    pregDateEditText.setText(todayDate);
                } else {
                    pregDateLabel.setVisibility(View.GONE);
                    pregDateEditText.setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        
        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(d -> {
            for(int i=0; i<4; i++) {
                dialogSlotImages[i] = null;
                dialogSlotPlaceholders[i] = null;
            }
        });

        saveButton.setOnClickListener(v -> {
            if (!validateInput(idEditText, importDateEditText, dobEditText, weightInputEditText, 
                               healthSpinner, healthDetailEditText, pregSpinner, pregDateEditText, healthDateEditText)) {
                return;
            }

            List<Object> newCowData = new ArrayList<>(Collections.nCopies(TOTAL_COLUMNS, ""));
            newCowData.set(COL_EPC, currentEpc);
            newCowData.set(COL_ID, idEditText.getText().toString());
            newCowData.set(COL_IMPORT_DATE, importDateEditText.getText().toString());
            newCowData.set(COL_DOB, dobEditText.getText().toString());
            newCowData.set(COL_BREED, breedEditText.getText().toString());
            newCowData.set(COL_SEX, sexSpinner.getSelectedItem().toString());
            newCowData.set(COL_WEIGHT, TextUtils.isEmpty(weightInputEditText.getText()) ? "0" : weightInputEditText.getText().toString());

            newCowData.set(COL_HEALTH, healthSpinner.getSelectedItem().toString());

            if (healthSpinner.getSelectedItem().toString().equals("Bệnh")) {
                newCowData.set(COL_HEALTH_DETAIL, healthDetailEditText.getText().toString());
            } else {
                newCowData.set(COL_HEALTH_DETAIL, "");
            }
            newCowData.set(COL_PREG, pregSpinner.getSelectedItem().toString());

            String todayDate = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date());

            String pregDateVal = pregDateEditText.getText().toString();
            if (TextUtils.isEmpty(pregDateVal)) {
                pregDateVal = todayDate;
            }
            newCowData.set(COL_PREG_DATE, pregDateVal);

            newCowData.set(COL_NUM_PREG, TextUtils.isEmpty(numPregEditText.getText()) ? "0" : numPregEditText.getText().toString());
            newCowData.set(COL_POSITION, statusSpinner.getSelectedItem().toString());

            String healthDateVal = healthDateEditText.getText().toString();
            if (TextUtils.isEmpty(healthDateVal)) {
                 healthDateVal = todayDate;
            }
            newCowData.set(COL_HEALTH_DATE, healthDateVal);
            newCowData.set(COL_COW_IMAGE, ""); // Placeholder for image URL

            if (selectedImageUri != null) {
                uploadImageAndAppendSheet(selectedImageUri, idEditText.getText().toString(), newCowData, new OnDataAppendedListener() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            Toast.makeText(InfoActivity.this, "Đã thêm bò mới thành công!", Toast.LENGTH_SHORT).show();
                            fetchCowInfo(currentEpc);
                            dialog.dismiss();
                        });
                    }
                    @Override
                    public void onFailure(Exception e) {
                        runOnUiThread(() -> {
                            Log.e(TAG, "Error adding new cow with image: ", e);
                            Toast.makeText(InfoActivity.this, "Thêm bò mới thất bại.", Toast.LENGTH_LONG).show();
                        });
                    }
                });
            } else {
                appendCowData(newCowData, new OnDataAppendedListener() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            Toast.makeText(InfoActivity.this, "Đã thêm bò mới thành công!", Toast.LENGTH_SHORT).show();
                            fetchCowInfo(currentEpc);
                            dialog.dismiss();
                        });
                    }
                    @Override
                    public void onFailure(Exception e) {
                        runOnUiThread(() -> {
                            Log.e(TAG, "Error adding new cow: ", e);
                            Toast.makeText(InfoActivity.this, "Thêm bò mới thất bại.", Toast.LENGTH_LONG).show();
                        });
                    }
                });
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showImageSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn ảnh");
        builder.setItems(new CharSequence[]{"Chụp ảnh mới", "Chọn từ thư viện"}, (dialog, which) -> {
            switch (which) {
                case 0:
                    checkCameraPermissionAndOpenCamera();
                    break;
                case 1:
                    Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(pickPhotoIntent, PICK_IMAGE_REQUEST);
                    break;
            }
        });
        builder.create().show();
    }

    private void checkCameraPermissionAndOpenCamera() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            java.io.File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Error creating image file", ex);
                Toast.makeText(this, "Lỗi tạo tệp hình ảnh.", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                cameraImageUri = FileProvider.getUriForFile(this, "com.epictech.cowrfid.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                startActivityForResult(takePictureIntent, CAPTURE_IMAGE_REQUEST);
            }
        }
    }

    private java.io.File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        java.io.File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return java.io.File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Cần có quyền truy cập máy ảnh để chụp ảnh.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupDatePicker(final EditText editText) {
        editText.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog datePickerDialog = new DatePickerDialog(InfoActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(selectedYear, selectedMonth, selectedDay);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
                        editText.setText(sdf.format(selectedDate.getTime()));
                    }, year, month, day);
            datePickerDialog.show();
        });
    }

    private void fetchCowInfo(String epc) {
        progressBar.setVisibility(View.VISIBLE);
        cowDetailsLayout.setVisibility(View.GONE);
        editInfoButton.setVisibility(View.GONE);
        addCowButton.setVisibility(View.GONE);
        linkCowButton.setVisibility(View.GONE);
        notFoundLayout.setVisibility(View.GONE);

        getCowDataByEpc(epc, new OnDataFetchedListener() {
            @Override
            public void onSuccess(List<List<Object>> data) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    if (data != null && !data.isEmpty()) {
                        currentCowData = data.get(0);
                        
                        // Update currentEpc to the real EPC from the sheet (in case we searched by ID)
                        if (currentCowData.size() > COL_EPC) {
                            currentEpc = currentCowData.get(COL_EPC).toString();
                        }

                        cowDetailsLayout.setVisibility(View.VISIBLE);
                        editInfoButton.setVisibility(View.VISIBLE);
                        updateUIWithCowData(currentCowData);
                    } else {
                        addCowButton.setVisibility(View.VISIBLE);
                        linkCowButton.setVisibility(View.VISIBLE);
                        notFoundLayout.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    Log.e(TAG, "Error fetching cow data: ", e);
                    notFoundLayout.setVisibility(View.VISIBLE);
                     Toast.makeText(InfoActivity.this, "Lấy dữ liệu bò thất bại.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }



    // --- Google Sheets & Drive API Methods ---

    public void getCowDataByEpc(String epc, OnDataFetchedListener listener) {
        executor.execute(() -> {
            try {
                String range = "CowInfo!A:Z";
                ValueRange response = sheetsService.spreadsheets().values().get(SPREADSHEET_ID, range).execute();
                List<List<Object>> values = response.getValues();
                if (values == null || values.isEmpty()) {
                    listener.onSuccess(null);
                    return;
                }
                for (List<Object> row : values) {
                    if (!row.isEmpty()) {
                        String rowEpc = row.get(0).toString();
                        String rowId = (row.size() > 1) ? row.get(1).toString() : "";
                        
                        // Check match for EPC or ID
                        if (rowEpc.equalsIgnoreCase(epc) || rowId.equalsIgnoreCase(epc)) {
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

    public void updateCowData(String epc, List<Object> updatedCowData, OnDataUpdatedListener listener) {
        executor.execute(() -> {
            try {
                ValueRange response = sheetsService.spreadsheets().values().get(SPREADSHEET_ID, "CowInfo!A:A").execute();
                List<List<Object>> values = response.getValues();
                int rowIndexToUpdate = -1;
                if (values != null) {
                    for (int i = 0; i < values.size(); i++) {
                        if (!values.get(i).isEmpty() && values.get(i).get(0).toString().equals(epc)) {
                            rowIndexToUpdate = i;
                            break;
                        }
                    }
                }
                if (rowIndexToUpdate != -1) {
                    String updateRange = "CowInfo!A" + (rowIndexToUpdate + 1);
                    ValueRange body = new ValueRange().setValues(Collections.singletonList(updatedCowData));
                    sheetsService.spreadsheets().values()
                            .update(SPREADSHEET_ID, updateRange, body)
                            .setValueInputOption("RAW")
                            .execute();
                    listener.onSuccess();
                } else {
                    listener.onFailure(new Exception("Không tìm thấy bò với mã EPC " + epc + "."));
                }
            } catch (Exception e) {
                listener.onFailure(e);
            }
        });
    }

    public void appendCowData(List<Object> newCowData, OnDataAppendedListener listener) {
        executor.execute(() -> {
            try {
                ValueRange body = new ValueRange().setValues(Collections.singletonList(newCowData));
                sheetsService.spreadsheets().values()
                        .append(SPREADSHEET_ID, "CowInfo", body)
                        .setValueInputOption("RAW")
                        .setInsertDataOption("INSERT_ROWS")
                        .execute();
                listener.onSuccess();
            } catch (Exception e) {
                listener.onFailure(e);
            }
        });
    }

    public void fetchAllCows(OnDataFetchedListener listener) {
        executor.execute(() -> {
            try {
                String range = "CowInfo!A:Z";
                ValueRange response = sheetsService.spreadsheets().values().get(SPREADSHEET_ID, range).execute();
                List<List<Object>> values = response.getValues();
                listener.onSuccess(values);
            } catch (Exception e) {
                listener.onFailure(e);
            }
        });
    }

    public void updateCowEpc(int rowIndex, String newEpc, OnDataUpdatedListener listener) {
        executor.execute(() -> {
            try {
                String range = "CowInfo!A" + (rowIndex + 1);
                ValueRange body = new ValueRange().setValues(Collections.singletonList(Collections.singletonList(newEpc)));
                sheetsService.spreadsheets().values()
                        .update(SPREADSHEET_ID, range, body)
                        .setValueInputOption("RAW")
                        .execute();
                listener.onSuccess();
            } catch (Exception e) {
                listener.onFailure(e);
            }
        });
    }

    private void uploadImageAndUpdateSheet(Uri imageUri, String cowId, List<Object> updatedData, OnDataUpdatedListener listener) {
        uploadImageToDrive(imageUri, cowId, "SLOT1_", new OnImageUploadedListener() {
            @Override
            public void onSuccess(String imageUrl) {
                updatedData.set(COL_COW_IMAGE, imageUrl);
                updateCowData(currentEpc, updatedData, listener);
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    private void uploadImageAndAppendSheet(Uri imageUri, String cowId, List<Object> newData, OnDataAppendedListener listener) {
        uploadImageToDrive(imageUri, cowId, "SLOT1_", new OnImageUploadedListener() {
            @Override
            public void onSuccess(String imageUrl) {
                newData.set(COL_COW_IMAGE, imageUrl);
                appendCowData(newData, listener);
            }
            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }



    private String extractFolderId(String url) {
        if (url == null) return null;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("folders/([a-zA-Z0-9-_]+)");
        java.util.regex.Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private void fetchCowImages(String folderId) {
        if (driveService == null) {
            infoSlotPlaceholders[0].setText("Đăng nhập để xem ảnh");
            return;
        }

        executor.execute(() -> {
            try {
                String query = "'" + folderId + "' in parents and mimeType contains 'image/' and trashed = false";
                com.google.api.services.drive.model.FileList result = driveService.files().list()
                        .setQ(query)
                        .setFields("files(id, name, thumbnailLink, webContentLink, createdTime)")
                        .setOrderBy("createdTime desc")
                        .execute();
                List<File> files = result.getFiles();
                
                File[] slotFiles = new File[4];
                if (files != null) {
                    for (File f : files) {
                        String name = f.getName();
                        if (name == null) continue;
                        
                        if (name.startsWith("SLOT1_") && slotFiles[0] == null) slotFiles[0] = f;
                        else if (name.startsWith("SLOT2_") && slotFiles[1] == null) slotFiles[1] = f;
                        else if (name.startsWith("SLOT3_") && slotFiles[2] == null) slotFiles[2] = f;
                        else if (name.startsWith("SLOT4_") && slotFiles[3] == null) slotFiles[3] = f;
                        else if (name.startsWith("MAIN_") && slotFiles[0] == null) slotFiles[0] = f;
                    }
                }
                
                cachedSlotFiles = slotFiles; // Cache for dialog usage

                runOnUiThread(() -> {
                    currentImageUrls.clear();
                    java.util.Map<Integer, Integer> slotToListIndex = new java.util.HashMap<>();
                    
                    // Build gallery list
                    for (int i = 0; i < 4; i++) {
                        if (slotFiles[i] != null) {
                            String link = slotFiles[i].getThumbnailLink();
                            if (link != null && link.contains("=s")) {
                                link = link.substring(0, link.lastIndexOf("=")) + "=s2000";
                            } else if (link == null) {
                                link = slotFiles[i].getWebContentLink();
                            }
                            currentImageUrls.add(link);
                            // Preload full resolution image
                            Glide.with(InfoActivity.this).load(link).preload();
                            slotToListIndex.put(i, currentImageUrls.size() - 1);
                        }
                    }

                    // Update UI
                    for (int i = 0; i < 4; i++) {
                        File f = slotFiles[i];
                        if (f != null) {
                            infoSlotImages[i].setVisibility(View.VISIBLE);
                            infoSlotPlaceholders[i].setVisibility(View.GONE);
                            
                            // Progressive Loading: s50 -> s400
                            String baseLink = f.getThumbnailLink();
                            String thumbLink = baseLink;
                            String tinyLink = baseLink;
                            
                            if (baseLink != null && baseLink.contains("=s")) {
                                String base = baseLink.substring(0, baseLink.lastIndexOf("="));
                                thumbLink = base + "=s400";
                                tinyLink = base + "=s50";
                            } else if (baseLink == null) {
                                thumbLink = f.getWebContentLink();
                                tinyLink = thumbLink;
                            }

                            Glide.with(InfoActivity.this)
                                .load(thumbLink)
                                .thumbnail(Glide.with(InfoActivity.this).load(tinyLink))
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(infoSlotImages[i]);
                            
                            if (slotToListIndex.containsKey(i)) {
                                final int listIndex = slotToListIndex.get(i);
                                infoSlotImages[i].setOnClickListener(v -> showFullscreenGallery(listIndex));
                            }
                        } else {
                            infoSlotImages[i].setVisibility(View.GONE);
                            infoSlotPlaceholders[i].setVisibility(View.VISIBLE);
                            if (i == 0) infoSlotPlaceholders[0].setText("Chưa có ảnh chính");
                            else infoSlotPlaceholders[i].setText("Chưa có ảnh");
                        }
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error fetching images", e);
                runOnUiThread(() -> {
                     infoSlotPlaceholders[0].setText("Lỗi tải ảnh");
                });
            }
        });
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
        // Preload all images (max 4 slots) to avoid loading delay on swipe
        viewPager.setOffscreenPageLimit(4);
        viewPager.setCurrentItem(startIndex, false);
        
        closeButton.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    private void populateDialogSlots(String folderId) {
        if (driveService == null) return;

        if (cachedSlotFiles != null) {
            updateDialogSlotsUI(cachedSlotFiles);
            return;
        }

        executor.execute(() -> {
            try {
                String query = "'" + folderId + "' in parents and mimeType contains 'image/' and trashed = false";
                com.google.api.services.drive.model.FileList result = driveService.files().list()
                        .setQ(query)
                        .setFields("files(id, name, thumbnailLink, webContentLink, createdTime)")
                        .setOrderBy("createdTime desc")
                        .execute();
                List<File> files = result.getFiles();
                
                File[] slotFiles = new File[4];
                if (files != null) {
                    for (File f : files) {
                        String name = f.getName();
                        if (name == null) continue;
                        if (name.startsWith("SLOT1_") && slotFiles[0] == null) slotFiles[0] = f;
                        else if (name.startsWith("SLOT2_") && slotFiles[1] == null) slotFiles[1] = f;
                        else if (name.startsWith("SLOT3_") && slotFiles[2] == null) slotFiles[2] = f;
                        else if (name.startsWith("SLOT4_") && slotFiles[3] == null) slotFiles[3] = f;
                        else if (name.startsWith("MAIN_") && slotFiles[0] == null) slotFiles[0] = f;
                    }
                }
                
                cachedSlotFiles = slotFiles;
                updateDialogSlotsUI(slotFiles);

            } catch (Exception e) {
                Log.e(TAG, "Error populating dialog slots", e);
            }
        });
    }

    private void updateDialogSlotsUI(File[] slotFiles) {
        runOnUiThread(() -> {
            if (dialogSlotImages[0] == null) return; 

            for (int i = 0; i < 4; i++) {
                File f = slotFiles[i];
                if (f != null) {
                    dialogSlotImages[i].setVisibility(View.VISIBLE);
                    dialogSlotPlaceholders[i].setVisibility(View.GONE);
                    
                    String thumbLink = f.getThumbnailLink();
                    if (thumbLink != null && thumbLink.contains("=s")) {
                         thumbLink = thumbLink.substring(0, thumbLink.lastIndexOf("=")) + "=s400";
                    } else if (thumbLink == null) {
                        thumbLink = f.getWebContentLink();
                    }
                    Glide.with(InfoActivity.this).load(thumbLink).into(dialogSlotImages[i]);
                } else {
                    dialogSlotImages[i].setVisibility(View.GONE);
                    dialogSlotPlaceholders[i].setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void linkEpcToRow(int rowIndex, String epc) {
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Đang liên kết...");
        progressDialog.show();

        updateCowEpc(rowIndex, epc, new OnDataUpdatedListener() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(InfoActivity.this, "Liên kết thành công!", Toast.LENGTH_SHORT).show();
                    fetchCowInfo(epc);
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(InfoActivity.this, "Lỗi liên kết EPC.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void uploadImageToDrive(Uri imageUri, String cowId, String fileNamePrefix, OnImageUploadedListener listener) {
        if (driveService == null) {
            listener.onFailure(new IllegalStateException("Dịch vụ Drive chưa được khởi tạo. Vui lòng đăng nhập."));
            return;
        }

        getOrCreateFolder(cowId, DRIVE_FOLDER_ID, new OnFolderReadyListener() {
            @Override
            public void onSuccess(String folderId) {
                executor.execute(() -> {
                    try {
                        String fileName = fileNamePrefix + System.currentTimeMillis() + ".jpg";
                        
                        // Compression Logic
                        InputStream inputStream = getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        if (inputStream != null) inputStream.close();

                        if (bitmap == null) {
                            listener.onFailure(new IOException("Không thể đọc hình ảnh."));
                            return;
                        }

                        // Auto-cut to 3:4 Aspect Ratio
                        int originalWidth = bitmap.getWidth();
                        int originalHeight = bitmap.getHeight();
                        float originalRatio = (float) originalWidth / originalHeight;
                        float targetRatio = 3f / 4f;

                        int cropWidth = originalWidth;
                        int cropHeight = originalHeight;

                        if (originalRatio > targetRatio) {
                            // Image is wider than 3:4, crop width
                            cropWidth = Math.round(originalHeight * targetRatio);
                        } else {
                            // Image is taller than 3:4, crop height
                            cropHeight = Math.round(originalWidth / targetRatio);
                        }

                        // Center Crop
                        int cropX = (originalWidth - cropWidth) / 2;
                        int cropY = (originalHeight - cropHeight) / 2;

                        // Ensure coordinates are valid (prevent rounding errors < 0)
                        cropX = Math.max(0, cropX);
                        cropY = Math.max(0, cropY);
                        cropWidth = Math.min(cropWidth, originalWidth - cropX);
                        cropHeight = Math.min(cropHeight, originalHeight - cropY);

                        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, cropX, cropY, cropWidth, cropHeight);
                        if (croppedBitmap != bitmap) {
                            bitmap.recycle();
                            bitmap = croppedBitmap;
                        }

                        // Force scale to 1440x1920
                        if (bitmap.getWidth() != 1440 || bitmap.getHeight() != 1920) {
                            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 1440, 1920, true);
                            if (scaledBitmap != bitmap) {
                                bitmap.recycle();
                                bitmap = scaledBitmap;
                            }
                        }

                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos); // 80% Quality
                        byte[] bitmapData = bos.toByteArray();
                        ByteArrayInputStream compressedStream = new ByteArrayInputStream(bitmapData);

                        File fileMetadata = new File();
                        fileMetadata.setName(fileName);
                        fileMetadata.setParents(Collections.singletonList(folderId));

                        InputStreamContent mediaContent = new InputStreamContent("image/jpeg", compressedStream);
                        File file = driveService.files().create(fileMetadata, mediaContent)
                                .setFields("id")
                                .execute();

                        Permission permission = new Permission().setType("anyone").setRole("reader");
                        driveService.permissions().create(file.getId(), permission).execute();

                        // Return the FOLDER URL instead of the image URL
                        String folderUrl = "https://drive.google.com/drive/folders/" + folderId;
                        listener.onSuccess(folderUrl);

                    } catch (Exception e) {
                        listener.onFailure(e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    private void getOrCreateFolder(String cowId, String parentFolderId, OnFolderReadyListener listener) {
        String folderName = "Cow " + cowId;
        executor.execute(() -> {
            try {
                // Search for the folder
                String query = "mimeType='application/vnd.google-apps.folder' and trashed=false and name='" + folderName + "' and '" + parentFolderId + "' in parents";
                com.google.api.services.drive.model.FileList result = driveService.files().list().setQ(query).setSpaces("drive").execute();
                List<File> files = result.getFiles();

                if (files != null && !files.isEmpty()) {
                    // Folder found, return its ID
                    listener.onSuccess(files.get(0).getId());
                } else {
                    // Folder not found, create it
                    File folderMetadata = new File();
                    folderMetadata.setName(folderName);
                    folderMetadata.setMimeType("application/vnd.google-apps.folder");
                    folderMetadata.setParents(Collections.singletonList(parentFolderId));

                    File folder = driveService.files().create(folderMetadata).setFields("id").execute();
                    
                    // Share the folder
                    Permission permission = new Permission().setType("anyone").setRole("reader");
                    driveService.permissions().create(folder.getId(), permission).execute();
                    
                    listener.onSuccess(folder.getId());
                }
            } catch (Exception e) {
                listener.onFailure(e);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SIGN_IN) {
            if (data != null) {
                handleSignInResult(data);
            } else {
                Toast.makeText(this, "Quá trình đăng nhập đã bị hủy.", Toast.LENGTH_SHORT).show();
            }
            return; // Exit after handling sign-in
        }

        if (requestCode == LINK_COW_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                int rowIndex = data.getIntExtra("ROW_INDEX", -1);
                String existingEpc = data.getStringExtra("EXISTING_EPC");

                if (rowIndex != -1) {
                    if (existingEpc != null && !existingEpc.isEmpty()) {
                        new AlertDialog.Builder(this)
                                .setTitle("Xác nhận ghi đè")
                                .setMessage("Bò này đã có mã EPC (" + existingEpc + "). Bạn có chắc muốn ghi đè bằng mã mới không?")
                                .setPositiveButton("Ghi đè", (d, w) -> linkEpcToRow(rowIndex, currentEpc))
                                .setNegativeButton("Hủy", null)
                                .show();
                    } else {
                        linkEpcToRow(rowIndex, currentEpc);
                    }
                }
            }
            return;
        }

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case PICK_IMAGE_REQUEST:
                    if (data != null && data.getData() != null) {
                        selectedImageUri = data.getData();
                    }
                    break;
                case CAPTURE_IMAGE_REQUEST:
                    selectedImageUri = cameraImageUri;
                    break;
            }

            // Update the preview only if an image was successfully selected or captured
            if (selectedImageUri != null) {
                if (currentSelectedSlot > 0) {
                     // Direct upload for slot
                     String prefix = "SLOT" + currentSelectedSlot + "_";
                     Toast.makeText(this, "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();
                     
                     String cowId = "Unknown";
                     if (currentCowData != null && currentCowData.size() > COL_ID) cowId = currentCowData.get(COL_ID).toString();
                     else if (idTextView != null) cowId = idTextView.getText().toString();
                     
                     uploadImageToDrive(selectedImageUri, cowId, prefix, new OnImageUploadedListener() {
                         @Override
                         public void onSuccess(String folderUrl) {
                             // Check if we need to update sheet
                             boolean needsUpdate = false;
                             if (currentCowData == null || currentCowData.size() <= COL_COW_IMAGE) needsUpdate = true;
                             else {
                                 String currentImg = currentCowData.get(COL_COW_IMAGE).toString();
                                 if (!folderUrl.equals(currentImg)) needsUpdate = true;
                             }
                             
                             if (needsUpdate) {
                                  List<Object> updateRow = new ArrayList<>(currentCowData);
                                  while (updateRow.size() <= COL_COW_IMAGE) updateRow.add("");
                                  updateRow.set(COL_COW_IMAGE, folderUrl);
                                  
                                  updateCowData(currentEpc, updateRow, new OnDataUpdatedListener() {
                                      @Override
                                      public void onSuccess() {
                                          runOnUiThread(() -> {
                                              Toast.makeText(InfoActivity.this, "Đã cập nhật ảnh!", Toast.LENGTH_SHORT).show();
                                              fetchCowInfo(currentEpc);
                                          });
                                      }
                                      @Override
                                      public void onFailure(Exception e) {
                                          runOnUiThread(() -> Toast.makeText(InfoActivity.this, "Lỗi cập nhật Sheet", Toast.LENGTH_SHORT).show());
                                      }
                                  });
                             } else {
                                 // Just refresh folder view
                                 String folderId = extractFolderId(folderUrl);
                                 fetchCowImages(folderId);
                                 populateDialogSlots(folderId);
                                 runOnUiThread(() -> Toast.makeText(InfoActivity.this, "Đã cập nhật ảnh!", Toast.LENGTH_SHORT).show());
                             }
                             currentSelectedSlot = 0;
                         }
                         @Override
                         public void onFailure(Exception e) {
                             runOnUiThread(() -> Toast.makeText(InfoActivity.this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                             currentSelectedSlot = 0;
                         }
                     });
                } else if (dialogCowPhotoImageView != null && dialogCowPhotoPlaceholder != null) {
                    dialogCowPhotoImageView.setVisibility(View.VISIBLE);
                    dialogCowPhotoPlaceholder.setVisibility(View.GONE);
                    dialogCowPhotoImageView.setImageURI(selectedImageUri);
                }
            }
        } else {
            // If the user cancels the image picker or camera, clear the selection
            selectedImageUri = null;
            currentSelectedSlot = 0;
        }
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

    /**
     * Adds a clear button (an 'x' icon) to the right of an EditText.
     * The button appears when text is present and clears the EditText when tapped.
     *
     * @param editText The EditText to which the clear button functionality will be added.
     */
    private void addClearButton(final EditText editText) {
        // Get the clear icon drawable
        final Drawable clearDrawable = AppCompatResources.getDrawable(this, R.drawable.close_circle);
        if (clearDrawable != null) {
            clearDrawable.setBounds(0, 0, clearDrawable.getIntrinsicWidth(), clearDrawable.getIntrinsicHeight());
        }

        // Method to set or clear the clear icon
        final Runnable setClearIconVisible = () -> {
            if (editText.getText().length() > 0) {
                editText.setCompoundDrawables(null, null, clearDrawable, null);
            } else {
                editText.setCompoundDrawables(null, null, null, null);
            }
        };

        // Initially set visibility based on current text
        setClearIconVisible.run();

        // Add TextWatcher to show/hide the clear button
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setClearIconVisible.run();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Add OnTouchListener to handle clicks on the clear button
        editText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (editText.getCompoundDrawables()[2] != null) { // Check if clear drawable is visible
                    // Check if touch event is within the bounds of the clear icon
                    if (event.getRawX() >= (editText.getRight() - editText.getCompoundDrawables()[2].getBounds().width() - editText.getPaddingEnd())) {
                        editText.setText(""); // Clear text
                        return true; // Consume the event
                    }
                }
            }
            return false; // Let other touch events pass through
        });
    }
}
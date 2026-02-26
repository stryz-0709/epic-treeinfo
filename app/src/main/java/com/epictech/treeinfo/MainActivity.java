package com.epictech.treeinfo;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private EditText epcSerialInput;
    private Button searchCowButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ScrollView scrollView;
    private Drive driveService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final String APPLICATION_NAME = "TreeInfoApp";
    private static final int REQUEST_CODE_SIGN_IN = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Quản lý Cây EarthRanger");
        }

        epcSerialInput = findViewById(R.id.et_epc_serial_input);
        searchCowButton = findViewById(R.id.btn_search_cow);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_main);

        scrollView = findViewById(R.id.sv_main);

        // Apply clear button functionality to epcSerialInput
        addClearButton(epcSerialInput);

        epcSerialInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) scrollToBottom();
        });
        epcSerialInput.setOnClickListener(v -> scrollToBottom());

        searchCowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String epcSerial = epcSerialInput.getText().toString().trim();
                if (TextUtils.isEmpty(epcSerial)) {
                    epcSerialInput.setError("Vui lòng nhập mã NFC.");
                    return;
                }

                Intent intent = new Intent(MainActivity.this, InfoActivity.class);
                intent.putExtra("NFC_SERIAL", epcSerial);
                startActivity(intent);
            }
        });
        // Check for updates
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null && GoogleSignIn.hasPermissions(account, new Scope(DriveScopes.DRIVE))) {
            initializeDriveService(account);
            new UpdateManager(this, driveService).checkForUpdates();
        } else {
            requestDriveSignIn();
        }


    }

    private void showCowListDialog(String title, java.util.List<String> ids) {
        String message;
        if (ids.isEmpty()) {
            message = "Danh sách trống";
        } else {
            java.util.Collections.sort(ids, (o1, o2) -> {
                try {
                    return Integer.compare(Integer.parseInt(o1), Integer.parseInt(o2));
                } catch (NumberFormatException e) {
                    return o1.compareTo(o2);
                }
            });
            message = TextUtils.join(", ", ids);
        }

        new android.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Đóng", null)
                .show();
    }

    private void scrollToBottom() {
        if (scrollView != null) {
            scrollView.postDelayed(() -> {
                int scrollY = searchCowButton.getBottom() + 50 - scrollView.getHeight();
                scrollView.smoothScrollTo(0, scrollY);
            }, 300);
        }
    }


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

    private void requestDriveSignIn() {
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE))
                .build();
        GoogleSignInClient signInClient = GoogleSignIn.getClient(this, signInOptions);
        startActivityForResult(signInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SIGN_IN) {
            if (data != null) {
                GoogleSignIn.getSignedInAccountFromIntent(data)
                        .addOnSuccessListener(googleAccount -> {
                            initializeDriveService(googleAccount);
                            new UpdateManager(this, driveService).checkForUpdates();
                        });
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        epcSerialInput.setText("");
    }

    @Override
    protected void onPause() {
        super.onPause();
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
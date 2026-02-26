package com.epictech.cowrfid;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;

import com.bumptech.glide.Glide;
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
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LinkCowActivity extends AppCompatActivity {

    private ListView listView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private Button backButton;
    private SearchView searchView;
    private TextView emptyView;
    
    private List<List<Object>> masterData = new java.util.ArrayList<>();
    
    private Sheets sheetsService;
    private Drive driveService;
    private Map<String, String> folderCoverCache = new HashMap<>();
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final String SPREADSHEET_ID = "1erRn4acsoNmOWgSiKHpcCqeUZpyLh8hv3x7a3kMgoYI";
    private static final String APPLICATION_NAME = "CowInfoApp";

    // Column Indices (Same as InfoActivity)
    private static final int COL_EPC = 0;
    private static final int COL_ID = 1;
    private static final int COL_IMPORT_DATE = 2;
    private static final int COL_DOB = 3;
    private static final int COL_BREED = 4;
    private static final int COL_SEX = 5;
    private static final int COL_WEIGHT = 6;
    private static final int COL_COW_IMAGE = 17;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link_cow);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Chọn bò để liên kết");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        listView = findViewById(R.id.lv_cows);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_link);
        progressBar = findViewById(R.id.progress_bar_link);
        backButton = findViewById(R.id.btn_back_link);
        searchView = findViewById(R.id.search_view_link);
        emptyView = findViewById(R.id.tv_empty_view);
        
        listView.setEmptyView(emptyView);

        backButton.setOnClickListener(v -> finish());
        swipeRefreshLayout.setOnRefreshListener(this::fetchCows);
        
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                updateList(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                updateList(newText);
                return true;
            }
        });

        initializeSheetsService();
        initializeDriveService();
        fetchCows();
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
            Toast.makeText(this, "Lỗi khởi tạo Google Sheets.", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchCows() {
        progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            try {
                String range = "CowInfo!A:Z";
                ValueRange response = sheetsService.spreadsheets().values().get(SPREADSHEET_ID, range).execute();
                List<List<Object>> values = response.getValues();
                
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    if (values != null && !values.isEmpty()) {
                        // Remove header if present
                        if (values.get(0).size() > 0 && values.get(0).get(0).toString().equalsIgnoreCase("EPC")) {
                            values.remove(0);
                        }
                        masterData = new java.util.ArrayList<>(values);
                        updateList("");
                    } else {
                        masterData.clear();
                        updateList(""); // Will show empty view
                        Toast.makeText(LinkCowActivity.this, "Không có dữ liệu.", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(LinkCowActivity.this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private static class CowItem {
        List<Object> row;
        int originalIndex;
        int type;
        String title;

        static final int TYPE_ITEM = 0;
        static final int TYPE_HEADER = 1;

        CowItem(List<Object> row, int originalIndex) {
            this.row = row;
            this.originalIndex = originalIndex;
            this.type = TYPE_ITEM;
        }

        CowItem(String title) {
            this.title = title;
            this.type = TYPE_HEADER;
        }
    }

    private void updateList(String query) {
        List<CowItem> unlinkedCows = new java.util.ArrayList<>();
        List<CowItem> linkedCows = new java.util.ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (int i = 0; i < masterData.size(); i++) {
            List<Object> row = masterData.get(i);
            String epc = (row.size() > COL_EPC) ? row.get(COL_EPC).toString() : "";
            
            if (epc.startsWith("OLD")) continue;

            String id = (row.size() > COL_ID) ? row.get(COL_ID).toString().toLowerCase() : "";
            if (!id.contains(lowerQuery)) continue;

            CowItem item = new CowItem(row, i + 1);
            
            if (epc.isEmpty()) {
                unlinkedCows.add(item);
            } else {
                linkedCows.add(item);
            }
        }

        List<CowItem> cowItems = new java.util.ArrayList<>();
        
        if (!unlinkedCows.isEmpty()) {
            cowItems.add(new CowItem("Bò chưa liên kết"));
            cowItems.addAll(unlinkedCows);
        }
        
        if (!linkedCows.isEmpty()) {
            cowItems.add(new CowItem("Bò đã liên kết"));
            cowItems.addAll(linkedCows);
        }

        CowAdapter adapter = new CowAdapter(cowItems);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            CowItem item = cowItems.get(position);
            if (item.type == CowItem.TYPE_HEADER) return;

            int sheetIndex = item.originalIndex;
            List<Object> row = item.row;
            String existingEpc = (row.size() > COL_EPC) ? row.get(COL_EPC).toString() : "";

            Intent resultIntent = new Intent();
            resultIntent.putExtra("ROW_INDEX", sheetIndex);
            resultIntent.putExtra("EXISTING_EPC", existingEpc);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private void initializeDriveService() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null && GoogleSignIn.hasPermissions(account, new Scope(DriveScopes.DRIVE))) {
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

    private void loadCowImage(String url, ImageView imageView) {
        String folderId = extractFolderId(url);
        if (folderId == null) {
            Glide.with(this).load(url).placeholder(android.R.color.darker_gray).into(imageView);
            return;
        }

        if (folderCoverCache.containsKey(folderId)) {
            String coverUrl = folderCoverCache.get(folderId);
            if (coverUrl != null && !coverUrl.isEmpty() && !coverUrl.equals("FETCHING")) {
                Glide.with(this).load(coverUrl).placeholder(android.R.color.darker_gray).into(imageView);
            } else {
                imageView.setImageResource(android.R.color.darker_gray);
            }
        } else {
            imageView.setImageResource(android.R.color.darker_gray);
            fetchFolderCover(folderId);
        }
    }

    private void fetchFolderCover(String folderId) {
        if (driveService == null) return;
        if (folderCoverCache.containsKey(folderId)) return;
        
        folderCoverCache.put(folderId, "FETCHING");

        executor.execute(() -> {
            try {
                String query = "'" + folderId + "' in parents and mimeType contains 'image/' and trashed = false";
                com.google.api.services.drive.model.FileList result = driveService.files().list()
                        .setQ(query)
                        .setFields("files(thumbnailLink)")
                        .setPageSize(1)
                        .setOrderBy("createdTime desc")
                        .execute();
                
                List<File> files = result.getFiles();
                if (files != null && !files.isEmpty()) {
                    String link = files.get(0).getThumbnailLink();
                    if (link != null && link.contains("=s")) {
                         link = link.substring(0, link.lastIndexOf("=")) + "=s100";
                    }
                    folderCoverCache.put(folderId, link);
                    runOnUiThread(() -> ((BaseAdapter)listView.getAdapter()).notifyDataSetChanged());
                } else {
                    folderCoverCache.put(folderId, "");
                }
            } catch (Exception e) {
                folderCoverCache.remove(folderId);
            }
        });
    }

    private class CowAdapter extends BaseAdapter {
        private List<CowItem> data;

        public CowAdapter(List<CowItem> data) {
            this.data = data;
        }

        @Override
        public int getCount() { return data.size(); }

        @Override
        public Object getItem(int position) { return data.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public int getViewTypeCount() { return 2; }

        @Override
        public int getItemViewType(int position) {
            return data.get(position).type;
        }

        @Override
        public boolean isEnabled(int position) {
            return data.get(position).type == CowItem.TYPE_ITEM;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            CowItem item = data.get(position);
            int type = getItemViewType(position);

            if (convertView == null) {
                if (type == CowItem.TYPE_HEADER) {
                    convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
                    TextView tv = convertView.findViewById(android.R.id.text1);
                    tv.setTypeface(null, android.graphics.Typeface.BOLD);
                    tv.setTextSize(18);
                    tv.setPadding(32, 16, 16, 16);
                    tv.setTextColor(getResources().getColor(android.R.color.black));
                } else {
                    convertView = getLayoutInflater().inflate(R.layout.item_cow_link, parent, false);
                }
            }

            if (type == CowItem.TYPE_HEADER) {
                TextView tv = convertView.findViewById(android.R.id.text1);
                tv.setText(item.title);
            } else {
                List<Object> row = item.row;

                ImageView thumb = convertView.findViewById(R.id.iv_cow_thumb);
                TextView idTv = convertView.findViewById(R.id.tv_cow_id);
                TextView sexTv = convertView.findViewById(R.id.tv_cow_sex);
                TextView weightTv = convertView.findViewById(R.id.tv_cow_weight);
                TextView epcStatusTv = convertView.findViewById(R.id.tv_cow_epc_status);

                String id = (row.size() > COL_ID) ? row.get(COL_ID).toString() : "N/A";
                String sex = (row.size() > COL_SEX) ? row.get(COL_SEX).toString() : "N/A";
                String importDate = (row.size() > COL_IMPORT_DATE) ? row.get(COL_IMPORT_DATE).toString() : "N/A";
                String dob = (row.size() > COL_DOB) ? row.get(COL_DOB).toString() : "N/A";
                String weight = (row.size() > COL_WEIGHT) ? row.get(COL_WEIGHT).toString() : "N/A";
                String epc = (row.size() > COL_EPC) ? row.get(COL_EPC).toString() : "";
                String imgUrl = (row.size() > COL_COW_IMAGE) ? row.get(COL_COW_IMAGE).toString() : "";

                idTv.setText(id);
                sexTv.setText(sex);
                weightTv.setText(weight + " KG");

                if (epc.isEmpty()) {
                    epcStatusTv.setText("Chưa có EPC");
                    epcStatusTv.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                } else {
                    epcStatusTv.setText("Đã có EPC");
                    epcStatusTv.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                }

                if (!imgUrl.isEmpty()) {
                    loadCowImage(imgUrl, thumb);
                } else {
                    thumb.setImageResource(android.R.color.darker_gray);
                }
            }

            return convertView;
        }
    }
}
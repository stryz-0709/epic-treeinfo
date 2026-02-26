package com.epictech.treeinfo;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class GuideActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Hướng dẫn");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        findViewById(R.id.btn_guide_add_cow).setOnClickListener(v -> showRichGuide("Thêm bò mới", 
            "1. Tại màn hình chính, nhập mã EPC bằng đầu đọc RFID.\n" +
            "2. Nhấn nút 'Tìm kiếm'.", 
            "guide_add_cow1",
            "3. Nếu bò chưa có trong hệ thống, nút 'Thêm bò' sẽ hiện ra.\n",
            "guide_add_cow2",
            "4. Nhấn 'Thêm bò', nhập đầy đủ thông tin (ID, Giống, Cân nặng...).\n" +
            "5. Nhấn vào các ô ảnh để chụp hoặc chọn ảnh từ thư viện.\n",
            "guide_add_cow3",
            "6. Nhấn 'Lưu' để đồng bộ dữ liệu lên Google Sheets.",
            "guide_add_cow4"));

        findViewById(R.id.btn_guide_edit_cow).setOnClickListener(v -> showRichGuide("Sửa thông tin",
    "1. Tại màn hình chính, nhập mã EPC bằng đầu đọc RFID hoặc điền ID trên tai bò.\n" +
            "2. Nhấn nút 'Tìm kiếm'.",
            "guide_add_cow1",
            "3. Tại màn hình thông tin chi tiết, nhấn nút 'Chỉnh sửa'.",
            "guide_edit_cow1",
            "4. Nhấn vào các ô ảnh để chụp hoặc chọn ảnh từ thư viện.\n",
            "guide_edit_cow2",
            "5. Thay đổi các thông tin cần thiết của bò (ID, Giống, Cân nặng...)..\n" +
            "6. Nếu bò chuyển trạng thái (VD: Bị bệnh), hãy cập nhật trạng thái và ngày.\n" +
            "7. Nhấn 'Lưu' để cập nhật thay đổi.",
            "guide_edit_cow3"));
            
        findViewById(R.id.btn_guide_link_epc).setOnClickListener(v -> showRichGuide("Liên kết EPC",
            "1. Dùng chức năng này khi bò đã có thông tin trên Google Sheets nhưng chưa gắn thẻ chip.\n" +
            "2. Tại màn hình chính, nhập mã EPC bằng đầu đọc RFID.\n" +
            "3. Nhấn nút 'Tìm kiếm'.",
            "guide_add_cow1",
            "3. Chọn nút 'Liên kết bò có sẵn'.\n",
            "guide_add_cow5",
            "4. Chọn con bò tương ứng từ danh sách hiện ra.\n" +
            "5. Xác nhận liên kết để gán mã EPC cho bò đó.", 
            "guide_link_epc"));

        findViewById(R.id.btn_guide_images).setOnClickListener(v -> showRichGuide("Quy định về Hình ảnh",
            "1. Ảnh 1 (Lớn): Là ảnh đại diện chính. Ưu tiên chụp rõ mặt và số tai.\n" +
            "2. Ảnh 2, 3, 4 (Nhỏ): Dùng cho các góc chụp khác (bên hông, chân, vết thương).", 
            "guide_images",
            "3. Xem ảnh: Nhấn vào ảnh để xem phóng to. Vuốt qua lại để xem các ảnh khác.\n" +
            "4. Sửa ảnh: Vào chế độ 'Chỉnh sửa' và nhấn vào ô ảnh tương ứng để thay thế/chụp mới."));


        findViewById(R.id.btn_guide_view_list).setOnClickListener(v -> showRichGuide("Xem danh sách bò",
            "1. Tại màn hình chính, nhấn vào khu vực 'Trong chuồng' hoặc 'Ngoài chuồng'.",
            "guide_view_list",
            "2. Một danh sách chứa các mã số (ID) của bò trong nhóm đó sẽ hiện ra"
                , "guide_view_list2"));


        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }
    
    private void showRichGuide(String title, String... items) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_guide_detail, null);
        builder.setView(view);
        android.app.AlertDialog dialog = builder.create();

        android.widget.TextView tvTitle = view.findViewById(R.id.tv_guide_title);
        android.widget.LinearLayout container = view.findViewById(R.id.ll_guide_container);
        android.widget.Button btnClose = view.findViewById(R.id.btn_close_guide);

        tvTitle.setText(title);

        for (String item : items) {
            if (item == null) continue;
            
            // Check if it's an image resource
            int resId = 0;
            if (item.startsWith("guide_")) {
                resId = getResources().getIdentifier(item, "drawable", getPackageName());
            }
            
            if (resId != 0) {
                // Add Image
                android.widget.ImageView iv = new android.widget.ImageView(this);
                iv.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
                iv.setAdjustViewBounds(true);
                iv.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
                iv.setImageResource(resId);
                
                android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) iv.getLayoutParams();
                params.setMargins(0, 16, 0, 16);
                iv.setLayoutParams(params);
                
                container.addView(iv);
            } else {
                // Add Text
                android.widget.TextView tv = new android.widget.TextView(this);
                tv.setText(item);
                tv.setTextSize(16);
                tv.setTextColor(getResources().getColor(android.R.color.black));
                tv.setLineSpacing(0, 1.2f);
                
                android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 0, 0, 16);
                tv.setLayoutParams(params);
                
                container.addView(tv);
            }
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

package com.epictech.treeinfo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etPassword;
    private Button btnLoginAdmin;
    private Button btnLoginUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLoginAdmin = findViewById(R.id.btn_login_admin);
        btnLoginUser = findViewById(R.id.btn_login_user);

        btnLoginAdmin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.equals("admin") && password.equals("admin123")) {
                login(true);
            } else {
                Toast.makeText(this, "Sai tên đăng nhập hoặc mật khẩu!", Toast.LENGTH_SHORT).show();
            }
        });

        btnLoginUser.setOnClickListener(v -> {
            login(false);
        });
    }

    private void login(boolean isAdmin) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("IS_ADMIN", isAdmin);
        startActivity(intent);
        finish();
    }
}
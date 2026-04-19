package com.example.musicapp.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.musicapp.R;
import com.example.musicapp.database.DatabaseHelper;

public class AddAlbumActivity extends AppCompatActivity {

    private EditText etAlbumName;
    private Button btnSave;
    private ImageButton btnBack;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_album);

        dbHelper = new DatabaseHelper(this);
        etAlbumName = findViewById(R.id.et_album_name);
        btnSave = findViewById(R.id.btn_save_album);
        btnBack = findViewById(R.id.btn_back_add_album);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        btnSave.setOnClickListener(v -> {
            String name = etAlbumName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên album!", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String username = pref.getString("username", "");
            long userId = dbHelper.getUserId(username);

            if (userId != -1) {
                dbHelper.addAlbum(name, userId);
                Toast.makeText(this, "Đã tạo album thành công!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Lỗi xác thực người dùng!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

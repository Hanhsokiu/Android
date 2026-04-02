package com.example.musicapp.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.musicapp.R;
import com.example.musicapp.database.DatabaseHelper;

public class AddAlbumActivity extends AppCompatActivity {

    private EditText etName;
    private Button btnSave;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_album);

        dbHelper = new DatabaseHelper(this);
        etName = findViewById(R.id.et_album_name);
        btnSave = findViewById(R.id.btn_save_album);

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên Album!", Toast.LENGTH_SHORT).show();
                return;
            }

            dbHelper.addAlbum(name);
            Toast.makeText(this, "Đã tạo Album: " + name, Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        });
    }
}

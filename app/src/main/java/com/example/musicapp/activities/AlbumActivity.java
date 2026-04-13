package com.example.musicapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musicapp.R;
import com.example.musicapp.adapters.AlbumAdapter;
import com.example.musicapp.database.DatabaseHelper;
import com.example.musicapp.models.Album;
import com.example.musicapp.utils.NavigationUtils;
import java.util.List;

public class AlbumActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AlbumAdapter albumAdapter;
    private DatabaseHelper dbHelper;
    private List<Album> albumList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        dbHelper = new DatabaseHelper(this);
        loadAlbums();

        recyclerView = findViewById(R.id.rv_albums);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        albumAdapter = new AlbumAdapter(albumList, album -> {
            Intent intent = new Intent(AlbumActivity.this, AlbumDetailActivity.class);
            intent.putExtra("album", album);
            startActivity(intent);
        });
        recyclerView.setAdapter(albumAdapter);

        ImageButton btnBack = findViewById(R.id.btn_back_album);
        btnBack.setOnClickListener(v -> finish());

        // QUAN TRỌNG: Thiết lập Bottom Navigation
        NavigationUtils.setupBottomNavigation(this);
    }

    private void loadAlbums() {
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String username = pref.getString("username", "");
        long userId = dbHelper.getUserId(username);
        
        if (userId != -1) {
            albumList = dbHelper.getAllAlbums(userId);
        } else {
            Toast.makeText(this, "Lỗi xác thực người dùng!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAlbums();
        if (albumAdapter != null) {
            albumAdapter.updateList(albumList);
        }
    }
}

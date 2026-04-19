package com.example.musicapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musicapp.R;
import com.example.musicapp.adapters.SongAdapter;
import com.example.musicapp.database.DatabaseHelper;
import com.example.musicapp.models.Song;
import com.example.musicapp.utils.NavigationUtils;
import java.util.List;

public class RecentActivity extends AppCompatActivity implements SongAdapter.OnSongActionListener {

    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    private List<Song> recentSongs;
    private DatabaseHelper dbHelper;
    private TextView txtNoRecent;
    private long currentUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent);

        dbHelper = new DatabaseHelper(this);
        
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String username = pref.getString("username", "");
        currentUserId = dbHelper.getUserId(username);

        if (currentUserId == -1) {
            Toast.makeText(this, "Vui lòng đăng nhập lại!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadRecentSongs();

        NavigationUtils.setupBottomNavigation(this);
    }

    private void initViews() {
        recyclerView = findViewById(R.id.rv_recent_songs);
        txtNoRecent = findViewById(R.id.txtNoRecent);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        ImageButton btnBack = findViewById(R.id.btn_back_recent);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void loadRecentSongs() {
        recentSongs = dbHelper.getRecentSongs(currentUserId);
        if (recentSongs == null || recentSongs.isEmpty()) {
            txtNoRecent.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            txtNoRecent.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            songAdapter = new SongAdapter(recentSongs, this, true);
            recyclerView.setAdapter(songAdapter);
        }
    }

    @Override
    public void onSongClick(Song song) {
        startActivity(new Intent(this, PlayerActivity.class));
    }

    @Override
    public void onSongPlayClick(Song song) {
        // Logic phát nhạc có thể thêm ở đây
    }

    @Override
    public void onSongEdit(Song song, int position) {}

    @Override
    public void onSongDelete(Song song, int position) {
        // Có thể thêm chức năng xóa khỏi lịch sử nếu muốn
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecentSongs();
    }
}

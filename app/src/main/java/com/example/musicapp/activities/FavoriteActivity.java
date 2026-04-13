package com.example.musicapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musicapp.R;
import com.example.musicapp.adapters.SongAdapter;
import com.example.musicapp.database.DatabaseHelper;
import com.example.musicapp.models.Song;
import com.example.musicapp.utils.NavigationUtils;
import java.util.List;

public class FavoriteActivity extends AppCompatActivity implements SongAdapter.OnSongActionListener {

    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    private List<Song> favoriteSongs;
    private DatabaseHelper dbHelper;
    private TextView txtNoFavorites;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        dbHelper = new DatabaseHelper(this);
        initViews();
        loadFavoriteSongs();

        // QUAN TRỌNG: Thiết lập Bottom Navigation
        NavigationUtils.setupBottomNavigation(this);
    }

    private void initViews() {
        recyclerView = findViewById(R.id.rv_favorite_songs);
        txtNoFavorites = findViewById(R.id.txtNoFavorites); // Sửa ID tại đây
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        ImageButton btnBack = findViewById(R.id.btn_back_favorite);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void loadFavoriteSongs() {
        favoriteSongs = dbHelper.getFavoriteSongs();
        if (favoriteSongs == null || favoriteSongs.isEmpty()) {
            txtNoFavorites.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            txtNoFavorites.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            songAdapter = new SongAdapter(favoriteSongs, this, true);
            recyclerView.setAdapter(songAdapter);
        }
    }

    @Override
    public void onSongClick(Song song) {
        startActivity(new Intent(this, PlayerActivity.class));
    }

    @Override
    public void onSongPlayClick(Song song) {
        // Có thể thêm logic phát nhạc tại đây
    }

    @Override
    public void onSongEdit(Song song, int position) {}

    @Override
    public void onSongDelete(Song song, int position) {
        dbHelper.setFavorite(song.getId(), false);
        loadFavoriteSongs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavoriteSongs();
    }
}

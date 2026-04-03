package com.example.musicapp.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musicapp.R;
import com.example.musicapp.adapters.SongAdapter;
import com.example.musicapp.database.DatabaseHelper;
import com.example.musicapp.models.Song;
import com.example.musicapp.services.MusicService;
import com.example.musicapp.utils.NavigationUtils;
import java.util.ArrayList;
import java.util.List;

public class FavoriteActivity extends AppCompatActivity implements SongAdapter.OnSongActionListener {

    private RecyclerView recyclerView;
    private TextView txtNoFavorites;
    private SongAdapter songAdapter;
    private List<Song> favoriteSongs = new ArrayList<>();
    private DatabaseHelper dbHelper;
    private MusicService musicService;
    private boolean isBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        recyclerView = findViewById(R.id.rv_favorite_songs);
        txtNoFavorites = findViewById(R.id.txtNoFavorites);
        dbHelper = new DatabaseHelper(this);

        loadFavorites();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        songAdapter = new SongAdapter(favoriteSongs, this);
        recyclerView.setAdapter(songAdapter);

        NavigationUtils.setupBottomNavigation(this);

        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    private void loadFavorites() {
        favoriteSongs = dbHelper.getFavoriteSongs();
        if (favoriteSongs.isEmpty()) {
            txtNoFavorites.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            txtNoFavorites.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSongClick(Song song) {
        startActivity(new Intent(this, PlayerActivity.class));
    }

    @Override
    public void onSongPlayClick(Song song) {
        if (isBound && musicService != null) {
            int index = -1;
            for (int i = 0; i < favoriteSongs.size(); i++) {
                if (favoriteSongs.get(i).getId() == song.getId()) {
                    index = i;
                    break;
                }
            }
            if (index != -1) {
                musicService.setSongList(new ArrayList<>(favoriteSongs));
                musicService.playSong(index);
                startActivity(new Intent(this, PlayerActivity.class));
            }
        }
    }

    @Override
    public void onSongEdit(Song song, int position) {
        Intent intent = new Intent(this, AddEditSongActivity.class);
        intent.putExtra("song", song);
        intent.putExtra("index", position);
        startActivityForResult(intent, 2);
    }

    @Override
    public void onSongDelete(Song song, int position) {
        dbHelper.deleteSong(song.getId());
        loadFavorites();
        songAdapter.updateList(favoriteSongs);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            loadFavorites();
            songAdapter.updateList(favoriteSongs);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) unbindService(serviceConnection);
    }
}

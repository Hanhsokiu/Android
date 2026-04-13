package com.example.musicapp.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musicapp.R;
import com.example.musicapp.adapters.SongAdapter;
import com.example.musicapp.database.DatabaseHelper;
import com.example.musicapp.models.Album;
import com.example.musicapp.models.Song;
import com.example.musicapp.services.MusicService;
import java.util.ArrayList;
import java.util.List;

public class AlbumDetailActivity extends AppCompatActivity implements SongAdapter.OnSongActionListener {

    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    private List<Song> songList;
    private Album album;
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
        setContentView(R.layout.activity_album_detail);

        Intent intentData = getIntent();
        if (intentData != null && intentData.hasExtra("album")) {
            album = (Album) intentData.getSerializableExtra("album");
        }

        if (album == null) {
            finish();
            return;
        }

        TextView txtTitle = findViewById(R.id.txt_album_name_title);
        txtTitle.setText(album.getName());

        dbHelper = new DatabaseHelper(this);
        loadSongs();

        recyclerView = findViewById(R.id.rv_album_songs);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // QUAN TRỌNG: Truyền tham số false để SongAdapter biết đây KHÔNG PHẢI là MainList
        // Từ đó hiển thị menu "Xóa khỏi danh sách này" cho cả Admin và User
        songAdapter = new SongAdapter(songList, this, false);
        recyclerView.setAdapter(songAdapter);

        ImageButton btnBack = findViewById(R.id.btn_back_detail);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void loadSongs() {
        songList = dbHelper.getSongsInAlbum(album.getId());
    }

    @Override
    public void onSongClick(Song song) {
        startActivity(new Intent(this, PlayerActivity.class));
    }

    @Override
    public void onSongPlayClick(Song song) {
        if (isBound && musicService != null && songList != null) {
            int index = -1;
            for (int i = 0; i < songList.size(); i++) {
                if (songList.get(i).getId() == song.getId()) {
                    index = i;
                    break;
                }
            }
            if (index != -1) {
                musicService.setSongList(new ArrayList<>(songList));
                musicService.playSong(index);
                startActivity(new Intent(this, PlayerActivity.class));
            }
        }
    }

    @Override
    public void onSongEdit(Song song, int position) {
        // Chức năng sửa trong Album nếu cần
    }

    @Override
    public void onSongDelete(Song song, int position) {
        // XÓA LIÊN KẾT BÀI HÁT KHỎI ALBUM
        dbHelper.removeSongFromAlbum(album.getId(), song.getId());
        songList.remove(position);
        songAdapter.updateList(new ArrayList<>(songList));
        Toast.makeText(this, "Đã xóa khỏi album", Toast.LENGTH_SHORT).show();
        
        if (isBound && musicService != null) {
            musicService.setSongList(new ArrayList<>(songList));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}

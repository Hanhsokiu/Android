package com.example.musicapp.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musicapp.R;
import com.example.musicapp.adapters.SongAdapter;
import com.example.musicapp.database.DatabaseHelper;
import com.example.musicapp.models.Song;
import com.example.musicapp.services.MusicService;
import com.example.musicapp.utils.NavigationUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SongAdapter.OnSongActionListener {

    private static final int REQUEST_ADD_SONG = 1;
    private static final int REQUEST_EDIT_SONG = 2;

    private RecyclerView recyclerView;
    private TextView txtNoMusic;
    private FloatingActionButton fabAdd;
    private SongAdapter songAdapter;
    private List<Song> songList = new ArrayList<>();
    
    private DrawerLayout drawerLayout;
    private ImageButton btnMenu, btnSearch;
    private EditText etSearch;
    private NavigationView navigationView;

    private MusicService musicService;
    private boolean isBound = false;
    private DatabaseHelper dbHelper;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            if (musicService != null && songList != null) {
                musicService.setSongList(new ArrayList<>(songList));
            }
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
        setContentView(R.layout.activity_main);

        initViews();
        dbHelper = new DatabaseHelper(this);
        loadSongsFromDb();

        setupRecyclerView();
        setupListeners();
        NavigationUtils.setupBottomNavigation(this);

        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        startService(intent);
        
        updateUI();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewSongs);
        txtNoMusic = findViewById(R.id.txtNoMusic);
        fabAdd = findViewById(R.id.fab_add_song);
        drawerLayout = findViewById(R.id.drawer_layout);
        btnMenu = findViewById(R.id.btn_menu);
        btnSearch = findViewById(R.id.btn_search);
        etSearch = findViewById(R.id.et_search);
        navigationView = findViewById(R.id.nav_view);
    }

    private void loadSongsFromDb() {
        songList = dbHelper.getAllSongs();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        songAdapter = new SongAdapter(new ArrayList<>(songList), this);
        recyclerView.setAdapter(songAdapter);
    }

    private void setupListeners() {
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        
        btnSearch.setOnClickListener(v -> {
            if (etSearch.getVisibility() == View.GONE) {
                etSearch.setVisibility(View.VISIBLE);
            } else {
                etSearch.setVisibility(View.GONE);
                etSearch.setText("");
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (songAdapter != null) songAdapter.filter(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditSongActivity.class);
            startActivityForResult(intent, REQUEST_ADD_SONG);
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_create_album) {
                startActivity(new Intent(MainActivity.this, AddAlbumActivity.class));
            } else if (id == R.id.nav_albums) {
                startActivity(new Intent(MainActivity.this, AlbumActivity.class));
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void updateUI() {
        if (songList.isEmpty()) {
            txtNoMusic.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            txtNoMusic.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
        if (songAdapter != null) {
            songAdapter.updateList(new ArrayList<>(songList));
        }
    }

    @Override
    public void onSongClick(Song song) {
        openPlayer();
    }

    @Override
    public void onSongPlayClick(Song song) {
        if (isBound && musicService != null) {
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
                openPlayer();
            }
        }
    }

    private void openPlayer() {
        Intent intent = new Intent(this, PlayerActivity.class);
        startActivity(intent);
    }

    @Override
    public void onSongEdit(Song song, int position) {
        Intent intent = new Intent(this, AddEditSongActivity.class);
        intent.putExtra("song", song);
        intent.putExtra("index", position);
        startActivityForResult(intent, REQUEST_EDIT_SONG);
    }

    @Override
    public void onSongDelete(Song song, int position) {
        dbHelper.deleteSong(song.getId());
        songList.removeIf(s -> s.getId() == song.getId());
        if (isBound && musicService != null) musicService.setSongList(new ArrayList<>(songList));
        updateUI();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            loadSongsFromDb();
            updateUI();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) unbindService(serviceConnection);
    }
}

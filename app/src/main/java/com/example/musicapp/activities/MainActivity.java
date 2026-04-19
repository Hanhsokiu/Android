package com.example.musicapp.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
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

public class MainActivity extends AppCompatActivity implements SongAdapter.OnSongActionListener, MusicService.MusicServiceListener {

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
    private String userRole;

    // Mini Player Views
    private View miniPlayerContainer;
    private ImageView imgMiniAlbum;
    private TextView txtMiniSongName, txtMiniArtistName;
    private ImageButton btnMiniPlayPause, btnMiniNext, btnMiniPrev;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicService.setListener(MainActivity.this);
            isBound = true;
            updateMiniPlayer();
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userRole = pref.getString("role", null);
        if (userRole == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        initViews();
        loadSongsFromDb();

        setupRecyclerView();
        setupListeners();
        NavigationUtils.setupBottomNavigation(this);

        if ("USER".equals(userRole)) {
            fabAdd.setVisibility(View.GONE);
        }

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

        // Mini Player
        miniPlayerContainer = findViewById(R.id.mini_player_container);
        imgMiniAlbum = findViewById(R.id.img_mini_album);
        txtMiniSongName = findViewById(R.id.txt_mini_song_name);
        txtMiniArtistName = findViewById(R.id.txt_mini_artist_name);
        btnMiniPlayPause = findViewById(R.id.btn_mini_play_pause);
        btnMiniPrev = findViewById(R.id.btn_mini_prev);
        btnMiniNext = findViewById(R.id.btn_mini_next);
        
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            TextView txtUser = headerView.findViewById(R.id.txt_user_name_nav);
            if (txtUser != null) {
                SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                txtUser.setText("Chào, " + pref.getString("username", "User") + " (" + userRole + ")");
            }
        }
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
                if (songAdapter != null) songAdapter.updateList(filter(s.toString()));
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditSongActivity.class);
            startActivityForResult(intent, 1);
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_logout) {
                logout();
            } else if (id == R.id.nav_recent) {
                startActivity(new Intent(MainActivity.this, RecentActivity.class));
            } else if (id == R.id.nav_create_album) {
                startActivity(new Intent(MainActivity.this, AddAlbumActivity.class));
            } else if (id == R.id.nav_albums) {
                startActivity(new Intent(MainActivity.this, AlbumActivity.class));
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        if (miniPlayerContainer != null) {
            miniPlayerContainer.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, PlayerActivity.class));
            });
        }

        if (btnMiniPlayPause != null) {
            btnMiniPlayPause.setOnClickListener(v -> {
                if (isBound && musicService != null) {
                    musicService.pausePlayer();
                    updateMiniPlayer(); // Cập nhật icon ngay lập tức
                }
            });
        }

        if (btnMiniPrev != null) {
            btnMiniPrev.setOnClickListener(v -> {
                if (isBound && musicService != null) {
                    musicService.prevSong();
                    // updateMiniPlayer sẽ được gọi tự động qua onSongChanged
                }
            });
        }

        if (btnMiniNext != null) {
            btnMiniNext.setOnClickListener(v -> {
                if (isBound && musicService != null) {
                    musicService.nextSong();
                    // updateMiniPlayer sẽ được gọi tự động qua onSongChanged
                }
            });
        }
    }

    private void updateMiniPlayer() {
        if (!isBound || musicService == null) return;
        
        Song currentSong = musicService.getCurrentSong();
        if (currentSong != null) {
            miniPlayerContainer.setVisibility(View.VISIBLE);
            txtMiniSongName.setText(currentSong.getTitle());
            txtMiniArtistName.setText(currentSong.getArtist());
            
            try {
                if (currentSong.getImagePath() != null && !currentSong.getImagePath().isEmpty()) {
                    imgMiniAlbum.setImageURI(Uri.parse(currentSong.getImagePath()));
                } else {
                    imgMiniAlbum.setImageResource(R.drawable.ic_music_note);
                }
            } catch (Exception e) {
                imgMiniAlbum.setImageResource(R.drawable.ic_music_note);
            }
            
            btnMiniPlayPause.setImageResource(musicService.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
        } else {
            miniPlayerContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSongChanged(Song newSong) {
        // Khi Service báo bài hát đã đổi, cập nhật Mini Player ngay lập tức trên UI Thread
        runOnUiThread(this::updateMiniPlayer);
    }

    @Override
    public void onPlayStatusChanged(boolean isPlaying) {
        // Khi Service báo trạng thái Play/Pause thay đổi
        runOnUiThread(this::updateMiniPlayer);
    }

    private void loadSongsFromDb() {
        songList = dbHelper.getAllSongs();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        songAdapter = new SongAdapter(new ArrayList<>(songList), this, true);
        recyclerView.setAdapter(songAdapter);
    }

    private List<Song> filter(String text) {
        if (text.isEmpty()) return new ArrayList<>(songList);
        List<Song> filteredList = new ArrayList<>();
        for (Song s : songList) {
            if (s.getTitle().toLowerCase().contains(text.toLowerCase()) || 
                s.getArtist().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(s);
            }
        }
        return filteredList;
    }

    private void logout() {
        if (isBound && musicService != null) {
            musicService.stopMusic();
        }
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        pref.edit().clear().apply();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void updateUI() {
        if (songList.isEmpty()) {
            if (txtNoMusic != null) txtNoMusic.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            if (txtNoMusic != null) txtNoMusic.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
        if (songAdapter != null) {
            songAdapter.updateList(new ArrayList<>(songList));
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
            for (int i = 0; i < songList.size(); i++) {
                if (songList.get(i).getId() == song.getId()) {
                    index = i;
                    break;
                }
            }
            if (index != -1) {
                musicService.setSongList(new ArrayList<>(songList));
                musicService.playSong(index);
                // updateMiniPlayer() sẽ được gọi qua onSongChanged
            }
        }
    }

    @Override
    public void onSongEdit(Song song, int position) {
        if (!"ADMIN".equals(userRole)) return;
        Intent intent = new Intent(this, AddEditSongActivity.class);
        intent.putExtra("song", song);
        intent.putExtra("index", position);
        startActivityForResult(intent, 2);
    }

    @Override
    public void onSongDelete(Song song, int position) {
        if (!"ADMIN".equals(userRole)) return;
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
    protected void onResume() {
        super.onResume();
        updateMiniPlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) unbindService(serviceConnection);
    }
}

package com.example.musicapp.activities;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.musicapp.R;
import com.example.musicapp.database.DatabaseHelper;
import com.example.musicapp.models.Album;
import com.example.musicapp.models.Song;
import com.example.musicapp.services.MusicService;
import com.example.musicapp.utils.MusicUtils;
import java.util.List;

public class PlayerActivity extends AppCompatActivity implements MusicService.MusicServiceListener {

    private ImageView imgAlbum;
    private TextView txtSongName, txtArtistName, txtCurrentTime, txtTotalTime;
    private SeekBar seekBar;
    private ImageButton btnPlayPause, btnPrev, btnNext, btnBackHome, btnFavorite, btnAddAlbum, btnRepeat, btnRestart;

    private MusicService musicService;
    private boolean isBound = false;
    private Handler handler = new Handler();
    private boolean isRepeatMode = false;
    
    // Animation xoay ảnh
    private ObjectAnimator rotateAnimator;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicService.setListener(PlayerActivity.this);
            isBound = true;
            updateUI();
            updateSeekBar();
            handleAnimation();
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        initViews();
        setupAnimation();
        setupListeners();

        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void initViews() {
        imgAlbum = findViewById(R.id.img_album);
        txtSongName = findViewById(R.id.txt_song_name);
        txtArtistName = findViewById(R.id.txt_artist_name);
        txtCurrentTime = findViewById(R.id.txt_current_time);
        txtTotalTime = findViewById(R.id.txt_total_time);
        seekBar = findViewById(R.id.seekBar);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);
        btnBackHome = findViewById(R.id.btn_back_home);
        btnFavorite = findViewById(R.id.btn_favorite_player);
        btnAddAlbum = findViewById(R.id.btn_add_to_album_player);
        btnRepeat = findViewById(R.id.btn_repeat);
        btnRestart = findViewById(R.id.btn_restart);
    }

    private void setupAnimation() {
        rotateAnimator = ObjectAnimator.ofFloat(imgAlbum, "rotation", 0f, 360f);
        rotateAnimator.setDuration(10000); // 10 giây 1 vòng
        rotateAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rotateAnimator.setInterpolator(new LinearInterpolator());
    }

    private void handleAnimation() {
        if (musicService != null && musicService.isPlaying()) {
            if (rotateAnimator.isPaused()) rotateAnimator.resume();
            else if (!rotateAnimator.isRunning()) rotateAnimator.start();
        } else {
            rotateAnimator.pause();
        }
    }

    private void setupListeners() {
        btnBackHome.setOnClickListener(v -> finish());

        btnPlayPause.setOnClickListener(v -> {
            if (isBound) {
                musicService.pausePlayer();
                // Giao diện sẽ được cập nhật thông qua callback onPlayStatusChanged
            }
        });

        btnNext.setOnClickListener(v -> {
            if (isBound) {
                musicService.nextSong();
            }
        });

        btnPrev.setOnClickListener(v -> {
            if (isBound) {
                musicService.prevSong();
            }
        });

        btnRepeat.setOnClickListener(v -> {
            isRepeatMode = !isRepeatMode;
            btnRepeat.setColorFilter(isRepeatMode ? getResources().getColor(android.R.color.holo_blue_light) : getResources().getColor(android.R.color.darker_gray));
            Toast.makeText(this, isRepeatMode ? "Bật phát lại" : "Tắt phát lại", Toast.LENGTH_SHORT).show();
        });

        btnRestart.setOnClickListener(v -> {
            if (isBound) {
                musicService.seekTo(0);
            }
        });

        btnFavorite.setOnClickListener(v -> {
            if (isBound) {
                Song song = musicService.getCurrentSong();
                if (song != null) {
                    boolean newState = !song.isFavorite();
                    song.setFavorite(newState);
                    new DatabaseHelper(this).setFavorite(song.getId(), newState);
                    updateFavoriteIcon(newState);
                    Toast.makeText(this, newState ? "Đã thích ❤️" : "Bỏ thích", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnAddAlbum.setOnClickListener(v -> {
            if (isBound) {
                Song song = musicService.getCurrentSong();
                if (song != null) showAlbumSelectionDialog(song.getId());
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isBound) musicService.seekTo(progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void showAlbumSelectionDialog(long songId) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String username = pref.getString("username", "");
        long userId = dbHelper.getUserId(username);

        if (userId == -1) {
            Toast.makeText(this, "Lỗi xác thực người dùng!", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Album> albums = dbHelper.getAllAlbums(userId);
        if (albums.isEmpty()) {
            Toast.makeText(this, "Bạn chưa có album nào! Hãy tạo album trước.", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[albums.size()];
        for (int i = 0; i < albums.size(); i++) names[i] = albums.get(i).getName();
        new AlertDialog.Builder(this).setTitle("Thêm vào Album")
            .setItems(names, (d, which) -> {
                dbHelper.addSongToAlbum(albums.get(which).getId(), songId);
                Toast.makeText(this, "Đã thêm!", Toast.LENGTH_SHORT).show();
            }).show();
    }

    private void updateUI() {
        if (!isBound) return;
        Song song = musicService.getCurrentSong();
        if (song != null) {
            txtSongName.setText(song.getTitle());
            txtArtistName.setText(song.getArtist());
            updateFavoriteIcon(song.isFavorite());
            try {
                if (song.getImagePath() != null && !song.getImagePath().isEmpty()) {
                    imgAlbum.setImageURI(Uri.parse(song.getImagePath()));
                } else {
                    imgAlbum.setImageResource(R.drawable.ic_music_note);
                }
            } catch (Exception e) { imgAlbum.setImageResource(R.drawable.ic_music_note); }
            int dur = musicService.getDuration();
            txtTotalTime.setText(MusicUtils.formatDuration(dur));
            seekBar.setMax(dur);
            updatePlayPauseIcon();
        }
    }

    @Override
    public void onSongChanged(Song newSong) {
        runOnUiThread(() -> {
            updateUI();
            rotateAnimator.end();
            rotateAnimator.start();
        });
    }

    @Override
    public void onPlayStatusChanged(boolean isPlaying) {
        runOnUiThread(() -> {
            updatePlayPauseIcon();
            handleAnimation();
        });
    }

    private void updateFavoriteIcon(boolean isFav) {
        btnFavorite.setImageResource(isFav ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
        btnFavorite.setColorFilter(isFav ? getResources().getColor(android.R.color.holo_red_light) : getResources().getColor(android.R.color.white));
    }

    private void updatePlayPauseIcon() {
        if (musicService != null) {
            btnPlayPause.setImageResource(musicService.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
        }
    }

    private void updateSeekBar() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isBound) {
                    int pos = musicService.getCurrentPosition();
                    seekBar.setProgress(pos);
                    txtCurrentTime.setText(MusicUtils.formatDuration(pos));
                    if (isRepeatMode && pos >= musicService.getDuration() - 500 && pos > 0) {
                        musicService.seekTo(0);
                    }
                }
                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            musicService.setListener(null);
            unbindService(serviceConnection);
        }
        handler.removeCallbacksAndMessages(null);
        if (rotateAnimator != null) rotateAnimator.cancel();
    }
}

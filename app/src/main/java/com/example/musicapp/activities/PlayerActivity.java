package com.example.musicapp.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.musicapp.R;
import com.example.musicapp.models.Song;
import com.example.musicapp.services.MusicService;
import com.example.musicapp.utils.MusicUtils;

public class PlayerActivity extends AppCompatActivity {

    private ImageView imgAlbum;
    private TextView txtSongName, txtArtistName, txtCurrentTime, txtTotalTime;
    private SeekBar seekBar;
    private ImageButton btnPlayPause, btnPrev, btnNext;

    private MusicService musicService;
    private boolean isBound = false;
    private Handler handler = new Handler();

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isBound = true;
            updateUI();
            updateSeekBar();
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

        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        btnPlayPause.setOnClickListener(v -> {
            if (isBound) {
                musicService.pausePlayer();
                updatePlayPauseIcon();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (isBound) {
                musicService.nextSong();
                updateUI();
            }
        });

        btnPrev.setOnClickListener(v -> {
            if (isBound) {
                musicService.prevSong();
                updateUI();
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isBound) {
                    musicService.seekTo(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
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
    }

    private void updateUI() {
        if (!isBound || musicService == null) return;
        Song currentSong = musicService.getCurrentSong();
        if (currentSong != null) {
            txtSongName.setText(currentSong.getTitle());
            txtArtistName.setText(currentSong.getArtist());
            
            // Xử lý ảnh an toàn tránh crash
            try {
                if (currentSong.getImagePath() != null && !currentSong.getImagePath().isEmpty()) {
                    imgAlbum.setImageURI(Uri.parse(currentSong.getImagePath()));
                    if (imgAlbum.getDrawable() == null) {
                        imgAlbum.setImageResource(R.drawable.ic_music_note);
                    }
                } else {
                    imgAlbum.setImageResource(R.drawable.ic_music_note);
                }
            } catch (Exception e) {
                imgAlbum.setImageResource(R.drawable.ic_music_note);
            }
            
            int duration = musicService.getDuration();
            if (duration > 0) {
                txtTotalTime.setText(MusicUtils.formatDuration(duration));
                seekBar.setMax(duration);
            }
            updatePlayPauseIcon();
        }
    }

    private void updatePlayPauseIcon() {
        if (musicService != null && musicService.isPlaying()) {
            btnPlayPause.setImageResource(R.drawable.ic_pause);
        } else {
            btnPlayPause.setImageResource(R.drawable.ic_play);
        }
    }

    private void updateSeekBar() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isBound && musicService != null) {
                    try {
                        int currentPos = musicService.getCurrentPosition();
                        int duration = musicService.getDuration();
                        
                        seekBar.setProgress(currentPos);
                        txtCurrentTime.setText(MusicUtils.formatDuration(currentPos));
                        
                        if (duration > 0 && seekBar.getMax() != duration) {
                            seekBar.setMax(duration);
                            txtTotalTime.setText(MusicUtils.formatDuration(duration));
                        }
                        
                        Song currentSong = musicService.getCurrentSong();
                        if (currentSong != null && !txtSongName.getText().toString().equals(currentSong.getTitle())) {
                            updateUI();
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        handler.removeCallbacksAndMessages(null);
    }
}

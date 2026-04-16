package com.example.musicapp.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.example.musicapp.R;
import com.example.musicapp.activities.PlayerActivity;
import com.example.musicapp.database.DatabaseHelper;
import com.example.musicapp.models.Song;
import java.io.IOException;
import java.util.List;

public class MusicService extends Service {

    private final IBinder binder = new MusicBinder();
    private MediaPlayer mediaPlayer;
    private List<Song> songList;
    private int songPosition = -1;
    private static final String CHANNEL_ID = "MusicChannel";
    private static final int NOTIFICATION_ID = 1;

    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREV = "action_prev";

    public interface MusicServiceListener {
        void onSongChanged(Song newSong);
        void onPlayStatusChanged(boolean isPlaying);
    }

    private MusicServiceListener listener;

    public void setListener(MusicServiceListener listener) {
        this.listener = listener;
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(
            new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        );
        mediaPlayer.setOnCompletionListener(mp -> nextSong());
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_PAUSE:
                    pausePlayer();
                    break;
                case ACTION_NEXT:
                    nextSong();
                    break;
                case ACTION_PREV:
                    prevSong();
                    break;
            }
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setSongList(List<Song> songs) {
        this.songList = songs;
    }

    public Song getCurrentSong() {
        if (songList != null && songPosition >= 0 && songPosition < songList.size()) {
            return songList.get(songPosition);
        }
        return null;
    }

    public void playSong(int position) {
        if (songList == null || songList.isEmpty() || position < 0 || position >= songList.size()) return;
        
        songPosition = position;
        Song playSong = songList.get(songPosition);
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(playSong.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            showNotification(playSong.getTitle(), playSong.getArtist(), true);
            
            // LƯU VÀO LỊCH SỬ GẦN ĐÂY
            saveToRecent(playSong.getId());

            if (listener != null) listener.onSongChanged(playSong);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void saveToRecent(long songId) {
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String username = pref.getString("username", "");
        DatabaseHelper db = new DatabaseHelper(this);
        long userId = db.getUserId(username);
        if (userId != -1) {
            db.addRecentSong(userId, songId);
        }
    }

    public void pausePlayer() {
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                showNotification(getCurrentSong().getTitle(), getCurrentSong().getArtist(), false);
            } else {
                mediaPlayer.start();
                showNotification(getCurrentSong().getTitle(), getCurrentSong().getArtist(), true);
            }
            if (listener != null) listener.onPlayStatusChanged(mediaPlayer.isPlaying());
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void stopMusic() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.reset();
        }
        stopForeground(true);
        stopSelf();
    }

    public void nextSong() {
        if (songList == null || songList.isEmpty()) return;
        songPosition = (songPosition + 1) % songList.size();
        playSong(songPosition);
    }

    public void prevSong() {
        if (songList == null || songList.isEmpty()) return;
        songPosition = (songPosition - 1 + songList.size()) % songList.size();
        playSong(songPosition);
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getDuration() {
        return (mediaPlayer != null) ? mediaPlayer.getDuration() : 0;
    }

    public int getCurrentPosition() {
        return (mediaPlayer != null) ? mediaPlayer.getCurrentPosition() : 0;
    }

    public void seekTo(int pos) {
        if (mediaPlayer != null) mediaPlayer.seekTo(pos);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Music Service Channel",
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
        }
    }

    private void showNotification(String title, String artist, boolean isPlaying) {
        Intent notificationIntent = new Intent(this, PlayerActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent prevIntent = new Intent(this, MusicService.class).setAction(ACTION_PREV);
        PendingIntent prevPending = PendingIntent.getService(this, 0, prevIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent pauseIntent = new Intent(this, MusicService.class).setAction(ACTION_PAUSE);
        PendingIntent pausePending = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, MusicService.class).setAction(ACTION_NEXT);
        PendingIntent nextPending = PendingIntent.getService(this, 2, nextIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentTitle(title)
                .setContentText(artist)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_prev, "Prev", prevPending)
                .addAction(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play, isPlaying ? "Pause" : "Play", pausePending)
                .addAction(R.drawable.ic_next, "Next", nextPending)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }
}

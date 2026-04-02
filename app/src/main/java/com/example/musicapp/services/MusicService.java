package com.example.musicapp.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
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
        } catch (Exception e) {
            e.printStackTrace();
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
        } catch (Exception e) { e.printStackTrace(); }
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
        try {
            return mediaPlayer != null && mediaPlayer.isPlaying();
        } catch (Exception e) { return false; }
    }

    public int getDuration() {
        try {
            return (mediaPlayer != null) ? mediaPlayer.getDuration() : 0;
        } catch (Exception e) { return 0; }
    }

    public int getCurrentPosition() {
        try {
            return (mediaPlayer != null) ? mediaPlayer.getCurrentPosition() : 0;
        } catch (Exception e) { return 0; }
    }

    public void seekTo(int pos) {
        try {
            if (mediaPlayer != null) mediaPlayer.seekTo(pos);
        } catch (Exception e) { e.printStackTrace(); }
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

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentTitle(title)
                .setContentText(artist)
                .setContentIntent(pendingIntent)
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

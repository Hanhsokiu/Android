package com.example.musicapp.models;

import java.io.Serializable;

public class Song implements Serializable {
    private long id;
    private String title;
    private String artist;
    private String path;
    private long duration;
    private String imagePath;

    public Song(long id, String title, String artist, String path, long duration, String imagePath) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.path = path;
        this.duration = duration;
        this.imagePath = imagePath;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getPath() { return path; }
    public long getDuration() { return duration; }
    public String getImagePath() { return imagePath; }

    public void setDuration(long duration) { this.duration = duration; }
}

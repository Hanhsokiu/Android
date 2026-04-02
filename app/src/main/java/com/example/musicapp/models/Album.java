package com.example.musicapp.models;

import java.io.Serializable;
import java.util.List;

public class Album implements Serializable {
    private long id;
    private String name;
    private List<Song> songs;

    public Album(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Album(long id, String name, List<Song> songs) {
        this.id = id;
        this.name = name;
        this.songs = songs;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public List<Song> getSongs() { return songs; }
    public void setSongs(List<Song> songs) { this.songs = songs; }
}

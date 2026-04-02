package com.example.musicapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.musicapp.models.Album;
import com.example.musicapp.models.Song;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "MusicAppDB.db";
    private static final int DATABASE_VERSION = 6;

    // Bảng Songs
    private static final String TABLE_SONGS = "songs";
    private static final String COL_ID = "id";
    private static final String COL_TITLE = "title";
    private static final String COL_ARTIST = "artist";
    private static final String COL_PATH = "path";
    private static final String COL_DURATION = "duration";
    private static final String COL_IMAGE = "image_path";

    // Bảng Albums
    private static final String TABLE_ALBUMS = "albums";
    private static final String COL_ALB_ID = "album_id";
    private static final String COL_ALB_NAME = "album_name";

    // Bảng Liên kết Album - Bài hát
    private static final String TABLE_ALBUM_SONGS = "album_songs";
    private static final String COL_AS_ALB_ID = "as_album_id";
    private static final String COL_AS_SONG_ID = "as_song_id";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_SONGS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TITLE + " TEXT, " +
                COL_ARTIST + " TEXT, " +
                COL_PATH + " TEXT, " +
                COL_DURATION + " INTEGER, " +
                COL_IMAGE + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_ALBUMS + " (" +
                COL_ALB_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_ALB_NAME + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_ALBUM_SONGS + " (" +
                COL_AS_ALB_ID + " INTEGER, " +
                COL_AS_SONG_ID + " INTEGER, " +
                "PRIMARY KEY (" + COL_AS_ALB_ID + ", " + COL_AS_SONG_ID + "))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ALBUM_SONGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ALBUMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SONGS);
        onCreate(db);
    }

    // --- Bài hát ---
    public long addSong(Song song) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COL_TITLE, song.getTitle());
        v.put(COL_ARTIST, song.getArtist());
        v.put(COL_PATH, song.getPath());
        v.put(COL_DURATION, song.getDuration());
        v.put(COL_IMAGE, song.getImagePath());
        long id = db.insert(TABLE_SONGS, null, v);
        db.close();
        return id;
    }

    public List<Song> getAllSongs() {
        List<Song> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_SONGS + " ORDER BY id DESC", null);
        if (c.moveToFirst()) {
            do {
                list.add(new Song(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4), c.getString(5)));
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    public void updateSong(Song song) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COL_TITLE, song.getTitle());
        v.put(COL_ARTIST, song.getArtist());
        v.put(COL_PATH, song.getPath());
        v.put(COL_IMAGE, song.getImagePath());
        db.update(TABLE_SONGS, v, COL_ID + " = ?", new String[]{String.valueOf(song.getId())});
        db.close();
    }

    public void deleteSong(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SONGS, COL_ID + " = ?", new String[]{String.valueOf(id)});
        db.delete(TABLE_ALBUM_SONGS, COL_AS_SONG_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // --- Album ---
    public long addAlbum(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COL_ALB_NAME, name);
        long id = db.insert(TABLE_ALBUMS, null, v);
        db.close();
        return id;
    }

    public List<Album> getAllAlbums() {
        List<Album> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_ALBUMS, null);
        if (c.moveToFirst()) {
            do {
                list.add(new Album(c.getLong(0), c.getString(1)));
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    public void addSongToAlbum(long albId, long songId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COL_AS_ALB_ID, albId);
        v.put(COL_AS_SONG_ID, songId);
        db.insertWithOnConflict(TABLE_ALBUM_SONGS, null, v, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
    }

    public List<Song> getSongsInAlbum(long albId) {
        List<Song> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String q = "SELECT s.* FROM " + TABLE_SONGS + " s " +
                   "JOIN " + TABLE_ALBUM_SONGS + " as ON s.id = as.as_song_id " +
                   "WHERE as.as_album_id = ?";
        Cursor c = db.rawQuery(q, new String[]{String.valueOf(albId)});
        if (c.moveToFirst()) {
            do {
                list.add(new Song(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4), c.getString(5)));
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }
}

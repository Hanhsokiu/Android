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

    private static final String DATABASE_NAME = "MusicManagerFinal.db";
    private static final int DATABASE_VERSION = 2; // Increment version

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE songs (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, artist TEXT, path TEXT, duration INTEGER, image_path TEXT, is_favorite INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE albums (album_id INTEGER PRIMARY KEY AUTOINCREMENT, album_name TEXT)");
        db.execSQL("CREATE TABLE album_songs (as_album_id INTEGER, as_song_id INTEGER, PRIMARY KEY (as_album_id, as_song_id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE songs ADD COLUMN is_favorite INTEGER DEFAULT 0");
        }
    }

    public long addSong(Song song) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("title", song.getTitle());
        v.put("artist", song.getArtist());
        v.put("path", song.getPath());
        v.put("duration", song.getDuration());
        v.put("image_path", song.getImagePath());
        v.put("is_favorite", song.isFavorite() ? 1 : 0);
        long id = db.insert("songs", null, v);
        db.close();
        return id;
    }

    public List<Song> getAllSongs() {
        List<Song> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM songs ORDER BY id DESC", null);
        if (c != null && c.moveToFirst()) {
            do {
                list.add(new Song(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4), c.getString(5), c.getInt(6) == 1));
            } while (c.moveToNext());
            c.close();
        }
        db.close();
        return list;
    }

    public List<Song> getFavoriteSongs() {
        List<Song> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM songs WHERE is_favorite = 1 ORDER BY id DESC", null);
        if (c != null && c.moveToFirst()) {
            do {
                list.add(new Song(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4), c.getString(5), c.getInt(6) == 1));
            } while (c.moveToNext());
            c.close();
        }
        db.close();
        return list;
    }

    public void updateSong(Song song) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("title", song.getTitle());
        v.put("artist", song.getArtist());
        v.put("path", song.getPath());
        v.put("image_path", song.getImagePath());
        v.put("is_favorite", song.isFavorite() ? 1 : 0);
        db.update("songs", v, "id = ?", new String[]{String.valueOf(song.getId())});
        db.close();
    }

    public void setFavorite(long songId, boolean isFavorite) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("is_favorite", isFavorite ? 1 : 0);
        db.update("songs", v, "id = ?", new String[]{String.valueOf(songId)});
        db.close();
    }

    public void deleteSong(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("songs", "id = ?", new String[]{String.valueOf(id)});
        db.delete("album_songs", "as_song_id = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public long addAlbum(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("album_name", name);
        long id = db.insert("albums", null, v);
        db.close();
        return id;
    }

    public List<Album> getAllAlbums() {
        List<Album> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM albums", null);
        if (c != null && c.moveToFirst()) {
            do {
                list.add(new Album(c.getLong(0), c.getString(1)));
            } while (c.moveToNext());
            c.close();
        }
        db.close();
        return list;
    }

    public void addSongToAlbum(long albId, long songId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("as_album_id", albId);
        v.put("as_song_id", songId);
        db.insertWithOnConflict("album_songs", null, v, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
    }

    public void removeSongFromAlbum(long albId, long songId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("album_songs", "as_album_id = ? AND as_song_id = ?", 
                  new String[]{String.valueOf(albId), String.valueOf(songId)});
        db.close();
    }

    public List<Song> getSongsInAlbum(long albId) {
        List<Song> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String q = "SELECT s.* FROM songs s JOIN album_songs a ON s.id = a.as_song_id WHERE a.as_album_id = ?";
        Cursor c = db.rawQuery(q, new String[]{String.valueOf(albId)});
        if (c != null && c.moveToFirst()) {
            do {
                list.add(new Song(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4), c.getString(5), c.getInt(6) == 1));
            } while (c.moveToNext());
            c.close();
        }
        db.close();
        return list;
    }
}

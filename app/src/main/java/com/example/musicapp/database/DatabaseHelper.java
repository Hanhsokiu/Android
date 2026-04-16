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
    private static final int DATABASE_VERSION = 6; 

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE songs (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, artist TEXT, path TEXT, duration INTEGER, image_path TEXT, is_favorite INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE albums (album_id INTEGER PRIMARY KEY AUTOINCREMENT, album_name TEXT, user_id INTEGER)");
        db.execSQL("CREATE TABLE album_songs (as_album_id INTEGER, as_song_id INTEGER, PRIMARY KEY (as_album_id, as_song_id))");
        db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT, role TEXT)");
        db.execSQL("CREATE TABLE favorite_songs (fav_user_id INTEGER, fav_song_id INTEGER, PRIMARY KEY (fav_user_id, fav_song_id))");
        // Bảng bài hát gần đây
        db.execSQL("CREATE TABLE recent_songs (recent_user_id INTEGER, recent_song_id INTEGER, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (recent_user_id, recent_song_id))");
        
        ContentValues admin = new ContentValues();
        admin.put("username", "admin");
        admin.put("password", "admin123");
        admin.put("role", "ADMIN");
        db.insert("users", null, admin);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 5) {
            db.execSQL("CREATE TABLE IF NOT EXISTS favorite_songs (fav_user_id INTEGER, fav_song_id INTEGER, PRIMARY KEY (fav_user_id, fav_song_id))");
        }
        if (oldVersion < 6) {
            db.execSQL("CREATE TABLE IF NOT EXISTS recent_songs (recent_user_id INTEGER, recent_song_id INTEGER, timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (recent_user_id, recent_song_id))");
        }
    }

    public long getUserId(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id FROM users WHERE username=?", new String[]{username});
        long id = -1;
        if (c != null && c.moveToFirst()) {
            id = c.getLong(0);
            c.close();
        }
        return id;
    }

    // --- Quản lý Yêu thích ---
    public void setFavorite(long userId, long songId, boolean isFav) {
        SQLiteDatabase db = this.getWritableDatabase();
        if (isFav) {
            ContentValues v = new ContentValues();
            v.put("fav_user_id", userId);
            v.put("fav_song_id", songId);
            db.insertWithOnConflict("favorite_songs", null, v, SQLiteDatabase.CONFLICT_IGNORE);
        } else {
            db.delete("favorite_songs", "fav_user_id = ? AND fav_song_id = ?", 
                      new String[]{String.valueOf(userId), String.valueOf(songId)});
        }
    }

    public boolean isFavorite(long userId, long songId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM favorite_songs WHERE fav_user_id = ? AND fav_song_id = ?", 
                               new String[]{String.valueOf(userId), String.valueOf(songId)});
        boolean exists = (c != null && c.getCount() > 0);
        if (c != null) c.close();
        return exists;
    }

    public List<Song> getFavoriteSongs(long userId) {
        List<Song> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String q = "SELECT s.* FROM songs s JOIN favorite_songs f ON s.id = f.fav_song_id WHERE f.fav_user_id = ?";
        Cursor c = db.rawQuery(q, new String[]{String.valueOf(userId)});
        if (c != null && c.moveToFirst()) {
            do {
                list.add(new Song(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4), c.getString(5), true));
            } while (c.moveToNext());
            c.close();
        }
        return list;
    }

    // --- Quản lý Gần đây ---
    public void addRecentSong(long userId, long songId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Xóa bản ghi cũ nếu đã tồn tại để cập nhật timestamp mới nhất
        db.delete("recent_songs", "recent_user_id = ? AND recent_song_id = ?", 
                  new String[]{String.valueOf(userId), String.valueOf(songId)});
        
        ContentValues v = new ContentValues();
        v.put("recent_user_id", userId);
        v.put("recent_song_id", songId);
        db.insert("recent_songs", null, v);
        
        // Giới hạn 20 bài gần đây nhất
        db.execSQL("DELETE FROM recent_songs WHERE (recent_user_id = " + userId + ") AND recent_song_id NOT IN " +
                   "(SELECT recent_song_id FROM recent_songs WHERE recent_user_id = " + userId + " ORDER BY timestamp DESC LIMIT 20)");
    }

    public List<Song> getRecentSongs(long userId) {
        List<Song> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String q = "SELECT s.* FROM songs s JOIN recent_songs r ON s.id = r.recent_song_id WHERE r.recent_user_id = ? ORDER BY r.timestamp DESC";
        Cursor c = db.rawQuery(q, new String[]{String.valueOf(userId)});
        if (c != null && c.moveToFirst()) {
            do {
                list.add(new Song(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4), c.getString(5), false));
            } while (c.moveToNext());
            c.close();
        }
        return list;
    }

    // --- Quản lý bài hát ---
    public long addSong(Song song) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("title", song.getTitle());
        v.put("artist", song.getArtist());
        v.put("path", song.getPath());
        v.put("duration", song.getDuration());
        v.put("image_path", song.getImagePath());
        v.put("is_favorite", 0);
        long id = db.insert("songs", null, v);
        db.close();
        return id;
    }

    public void updateSong(Song song) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("title", song.getTitle());
        v.put("artist", song.getArtist());
        v.put("path", song.getPath());
        v.put("image_path", song.getImagePath());
        db.update("songs", v, "id = ?", new String[]{String.valueOf(song.getId())});
        db.close();
    }

    public List<Song> getAllSongs() {
        List<Song> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM songs ORDER BY id DESC", null);
        if (c != null && c.moveToFirst()) {
            do {
                list.add(new Song(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4), c.getString(5), false));
            } while (c.moveToNext());
            c.close();
        }
        db.close();
        return list;
    }

    public void deleteSong(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("songs", "id = ?", new String[]{String.valueOf(id)});
        db.delete("album_songs", "as_song_id = ?", new String[]{String.valueOf(id)});
        db.delete("favorite_songs", "fav_song_id = ?", new String[]{String.valueOf(id)});
        db.delete("recent_songs", "recent_song_id = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // --- Album ---
    public long addAlbum(String name, long userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("album_name", name);
        v.put("user_id", userId);
        long id = db.insert("albums", null, v);
        db.close();
        return id;
    }

    public List<Album> getAllAlbums(long userId) {
        List<Album> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM albums WHERE user_id = ?", new String[]{String.valueOf(userId)});
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
                list.add(new Song(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4), c.getString(5), false));
            } while (c.moveToNext());
            c.close();
        }
        db.close();
        return list;
    }

    public boolean registerUser(String username, String password, String role) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("username", username);
        v.put("password", password);
        v.put("role", role);
        long res = db.insert("users", null, v);
        return res != -1;
    }

    public String checkLogin(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT role FROM users WHERE username=? AND password=?", new String[]{username, password});
        if (c != null && c.moveToFirst()) {
            String role = c.getString(0);
            c.close();
            return role;
        }
        return null;
    }
}

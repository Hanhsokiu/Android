package com.example.musicapp.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musicapp.R;
import com.example.musicapp.database.DatabaseHelper;
import com.example.musicapp.models.Album;
import com.example.musicapp.models.Song;
import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
    private List<Song> songList;
    private List<Song> songListFull;
    private OnSongActionListener listener;

    public interface OnSongActionListener {
        void onSongClick(Song song);
        void onSongPlayClick(Song song);
        void onSongEdit(Song song, int position);
        void onSongDelete(Song song, int position);
    }

    public SongAdapter(List<Song> songList, OnSongActionListener listener) {
        this.songList = songList;
        this.songListFull = new ArrayList<>(songList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songList.get(position);
        holder.title.setText(song.getTitle());
        holder.artist.setText(song.getArtist());
        
        if (song.getImagePath() != null && !song.getImagePath().isEmpty()) {
            try {
                holder.imgSong.setImageURI(Uri.parse(song.getImagePath()));
            } catch (Exception e) {
                holder.imgSong.setImageResource(R.drawable.ic_music_note);
            }
        } else {
            holder.imgSong.setImageResource(R.drawable.ic_music_note);
        }

        holder.itemView.setOnClickListener(v -> listener.onSongClick(song));
        holder.btnPlay.setOnClickListener(v -> listener.onSongPlayClick(song));
        holder.btnMore.setOnClickListener(v -> showPopupMenu(v, song, holder.getAdapterPosition()));
    }

    private void showPopupMenu(View view, Song song, int position) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(view.getContext(), view);
        popup.getMenu().add("Thêm vào Album");
        popup.getMenu().add("Sửa");
        popup.getMenu().add("Xóa");
        
        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("Thêm vào Album")) {
                showAlbumSelectionDialog(view.getContext(), song.getId());
            } else if (title.equals("Sửa")) {
                listener.onSongEdit(song, position);
            } else if (title.equals("Xóa")) {
                listener.onSongDelete(song, position);
            }
            return true;
        });
        popup.show();
    }

    private void showAlbumSelectionDialog(Context context, long songId) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        List<Album> albums = dbHelper.getAllAlbums();
        if (albums.isEmpty()) {
            Toast.makeText(context, "Chưa có album nào. Hãy tạo album trước!", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] albumNames = new String[albums.size()];
        for (int i = 0; i < albums.size(); i++) {
            albumNames[i] = albums.get(i).getName();
        }

        new AlertDialog.Builder(context)
            .setTitle("Chọn Album")
            .setItems(albumNames, (dialog, which) -> {
                dbHelper.addSongToAlbum(albums.get(which).getId(), songId);
                Toast.makeText(context, "Đã thêm vào album", Toast.LENGTH_SHORT).show();
            })
            .show();
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    public void filter(String text) {
        songList.clear();
        if (text.isEmpty()) {
            songList.addAll(songListFull);
        } else {
            text = text.toLowerCase();
            for (Song song : songListFull) {
                if (song.getTitle().toLowerCase().contains(text) || 
                    song.getArtist().toLowerCase().contains(text)) {
                    songList.add(song);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void updateList(List<Song> newList) {
        this.songList = new ArrayList<>(newList);
        this.songListFull = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView title, artist;
        ImageView imgSong;
        ImageButton btnPlay, btnMore;
        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.txt_song_title);
            artist = itemView.findViewById(R.id.txt_song_artist);
            imgSong = itemView.findViewById(R.id.img_song_item);
            btnPlay = itemView.findViewById(R.id.btn_play_item);
            btnMore = itemView.findViewById(R.id.btn_more_options);
        }
    }
}

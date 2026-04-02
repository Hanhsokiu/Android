package com.example.musicapp.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musicapp.R;
import com.example.musicapp.adapters.AlbumAdapter;
import com.example.musicapp.database.DatabaseHelper;
import com.example.musicapp.models.Album;
import java.util.List;

public class AlbumActivity extends AppCompatActivity implements AlbumAdapter.OnAlbumClickListener {

    private RecyclerView recyclerView;
    private AlbumAdapter albumAdapter;
    private List<Album> albumList;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        dbHelper = new DatabaseHelper(this);
        albumList = dbHelper.getAllAlbums();

        recyclerView = findViewById(R.id.rv_albums);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        albumAdapter = new AlbumAdapter(albumList, this);
        recyclerView.setAdapter(albumAdapter);

        findViewById(R.id.btn_back_album).setOnClickListener(v -> finish());
    }

    @Override
    public void onAlbumClick(int position) {
        Album album = albumList.get(position);
        Intent intent = new Intent(this, AlbumDetailActivity.class);
        intent.putExtra("album", album);
        startActivity(intent);
    }
}

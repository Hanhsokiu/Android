package com.example.musicapp.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.musicapp.R;
import com.example.musicapp.models.Song;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class AddEditSongActivity extends AppCompatActivity {

    private static final int PICK_AUDIO_REQUEST = 101;
    private static final int PICK_IMAGE_REQUEST = 102;

    private EditText etTitle, etArtist, etPath;
    private ImageView ivSongImage;
    private Button btnChooseMusic, btnChooseImage, btnSave;
    private long currentSongId = -1;
    private int songIndex = -1;
    private String selectedMusicPath = "";
    private String selectedImagePath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_song);

        initViews();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("song")) {
            Song song = (Song) intent.getSerializableExtra("song");
            if (song != null) {
                currentSongId = song.getId();
                songIndex = intent.getIntExtra("index", -1);
                etTitle.setText(song.getTitle());
                etArtist.setText(song.getArtist());
                etPath.setText(song.getPath());
                selectedMusicPath = song.getPath();
                selectedImagePath = song.getImagePath();
                
                if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
                    try {
                        ivSongImage.setImageURI(Uri.parse(selectedImagePath));
                    } catch (Exception e) {
                        ivSongImage.setImageResource(R.drawable.ic_music_note);
                    }
                }
            }
        }

        btnChooseMusic.setOnClickListener(v -> openFilePicker("audio/*", PICK_AUDIO_REQUEST));
        btnChooseImage.setOnClickListener(v -> openFilePicker("image/*", PICK_IMAGE_REQUEST));

        btnSave.setOnClickListener(v -> saveSong());
    }

    private void initViews() {
        etTitle = findViewById(R.id.et_title);
        etArtist = findViewById(R.id.et_artist);
        etPath = findViewById(R.id.et_path);
        ivSongImage = findViewById(R.id.iv_song_image);
        btnChooseMusic = findViewById(R.id.btn_choose_music);
        btnChooseImage = findViewById(R.id.btn_choose_image);
        btnSave = findViewById(R.id.btn_save);
    }

    private void saveSong() {
        String title = etTitle.getText().toString().trim();
        String artist = etArtist.getText().toString().trim();

        if (title.isEmpty() || selectedMusicPath == null || selectedMusicPath.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên và chọn nhạc!", Toast.LENGTH_SHORT).show();
            return;
        }

        long id = (currentSongId != -1) ? currentSongId : System.currentTimeMillis();
        Song song = new Song(id, title, artist, selectedMusicPath, 0, selectedImagePath);
        
        Intent resultIntent = new Intent();
        resultIntent.putExtra("song", song);
        resultIntent.putExtra("index", songIndex);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void openFilePicker(String type, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(type);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Chọn tệp"), requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            if (requestCode == PICK_AUDIO_REQUEST) {
                selectedMusicPath = saveFileToInternalStorage(uri, "music");
                etPath.setText(selectedMusicPath);
                if (etTitle.getText().toString().isEmpty()) {
                    etTitle.setText(getFileName(uri));
                }
            } else if (requestCode == PICK_IMAGE_REQUEST) {
                selectedImagePath = saveFileToInternalStorage(uri, "images");
                try {
                    ivSongImage.setImageURI(Uri.parse(selectedImagePath));
                } catch (Exception e) {
                    Toast.makeText(this, "Không thể hiển thị ảnh!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String saveFileToInternalStorage(Uri uri, String folderName) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            String fileName = getFileName(uri);
            File dir = new File(getExternalFilesDir(null), folderName);
            if (!dir.exists()) dir.mkdirs();
            
            File file = new File(dir, fileName);
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.close();
            inputStream.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return uri.toString();
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) result = cursor.getString(index);
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }
}

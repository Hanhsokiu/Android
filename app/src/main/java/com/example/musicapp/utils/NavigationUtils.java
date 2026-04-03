package com.example.musicapp.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ImageButton;
import com.example.musicapp.R;
import com.example.musicapp.activities.AddAlbumActivity;
import com.example.musicapp.activities.AlbumActivity;
import com.example.musicapp.activities.FavoriteActivity;
import com.example.musicapp.activities.MainActivity;

public class NavigationUtils {

    public static void setupBottomNavigation(Activity activity) {
        View bottomNav = activity.findViewById(R.id.bottom_navigation);
        if (bottomNav == null) return;

        ImageButton navHome = bottomNav.findViewById(R.id.nav_home);
        ImageButton navAlbum = bottomNav.findViewById(R.id.nav_album);
        ImageButton navFavorite = bottomNav.findViewById(R.id.nav_favorite);
        ImageButton navAddAlbum = bottomNav.findViewById(R.id.nav_add_album);

        // Highlight the current tab
        int black = activity.getResources().getColor(android.R.color.black);
        if (activity instanceof MainActivity) {
            navHome.setColorFilter(black);
        } else if (activity instanceof AlbumActivity) {
            navAlbum.setColorFilter(black);
        } else if (activity instanceof FavoriteActivity) {
            navFavorite.setColorFilter(black);
        } else if (activity instanceof AddAlbumActivity) {
            navAddAlbum.setColorFilter(black);
        }

        navHome.setOnClickListener(v -> {
            if (!(activity instanceof MainActivity)) {
                Intent intent = new Intent(activity, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);
            }
        });

        navAlbum.setOnClickListener(v -> {
            if (!(activity instanceof AlbumActivity)) {
                activity.startActivity(new Intent(activity, AlbumActivity.class));
            }
        });

        navFavorite.setOnClickListener(v -> {
            if (!(activity instanceof FavoriteActivity)) {
                activity.startActivity(new Intent(activity, FavoriteActivity.class));
            }
        });

        navAddAlbum.setOnClickListener(v -> {
            if (!(activity instanceof AddAlbumActivity)) {
                activity.startActivity(new Intent(activity, AddAlbumActivity.class));
            }
        });
    }
}

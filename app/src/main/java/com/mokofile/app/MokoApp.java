package com.mokofile.app;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.color.DynamicColors;

public class MokoApp extends Application {

    public static final String PREFS = "mokofile";
    public static final String KEY_THEME = "theme_mode";

    @Override
    public void onCreate() {
        super.onCreate();

        // Настоящий Material You: палитра из обоев (Android 12+).
        DynamicColors.applyToActivitiesIfAvailable(this);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int mode = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static int savedTheme(android.content.Context context) {
        return context.getSharedPreferences(PREFS, MODE_PRIVATE)
                .getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static void saveTheme(android.content.Context context, int mode) {
        context.getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit().putInt(KEY_THEME, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }
}

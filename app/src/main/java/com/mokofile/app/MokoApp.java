package com.mokofile.app;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

public class MokoApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(Settings.themeMode(this));
    }
}

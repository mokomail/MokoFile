package com.mokofile.app;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/** Единое место для всех настроек приложения. */
public final class Settings {

    public static final String PREFS = "mokofile";

    public static final String KEY_THEME = "theme_mode";
    public static final String KEY_DYNAMIC = "dynamic_color";
    public static final String KEY_GRID = "grid";
    public static final String KEY_SORT_KEY = "sort_key";
    public static final String KEY_SORT_ASC = "sort_asc";
    public static final String KEY_HIDDEN = "show_hidden";
    public static final String KEY_FOLDERS_FIRST = "folders_first";
    public static final String KEY_CONFIRM_DELETE = "confirm_delete";
    public static final String KEY_LAST_DIR = "last_dir";

    private Settings() {
    }

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static int themeMode(Context context) {
        return prefs(context).getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static void setThemeMode(Context context, int mode) {
        prefs(context).edit().putInt(KEY_THEME, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static boolean dynamicColor(Context context) {
        return prefs(context).getBoolean(KEY_DYNAMIC, true);
    }

    public static void setDynamicColor(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_DYNAMIC, enabled).apply();
    }

    public static boolean grid(Context context) {
        return prefs(context).getBoolean(KEY_GRID, false);
    }

    public static void setGrid(Context context, boolean grid) {
        prefs(context).edit().putBoolean(KEY_GRID, grid).apply();
    }

    public static String sortKey(Context context) {
        return prefs(context).getString(KEY_SORT_KEY, "name");
    }

    public static void setSortKey(Context context, String key) {
        prefs(context).edit().putString(KEY_SORT_KEY, key).apply();
    }

    public static boolean sortAsc(Context context) {
        return prefs(context).getBoolean(KEY_SORT_ASC, true);
    }

    public static void setSortAsc(Context context, boolean asc) {
        prefs(context).edit().putBoolean(KEY_SORT_ASC, asc).apply();
    }

    public static boolean showHidden(Context context) {
        return prefs(context).getBoolean(KEY_HIDDEN, false);
    }

    public static void setShowHidden(Context context, boolean show) {
        prefs(context).edit().putBoolean(KEY_HIDDEN, show).apply();
    }

    public static boolean foldersFirst(Context context) {
        return prefs(context).getBoolean(KEY_FOLDERS_FIRST, true);
    }

    public static void setFoldersFirst(Context context, boolean first) {
        prefs(context).edit().putBoolean(KEY_FOLDERS_FIRST, first).apply();
    }

    public static boolean confirmDelete(Context context) {
        return prefs(context).getBoolean(KEY_CONFIRM_DELETE, true);
    }

    public static void setConfirmDelete(Context context, boolean confirm) {
        prefs(context).edit().putBoolean(KEY_CONFIRM_DELETE, confirm).apply();
    }

    public static String lastDir(Context context, String fallback) {
        return prefs(context).getString(KEY_LAST_DIR, fallback);
    }

    public static void setLastDir(Context context, String path) {
        prefs(context).edit().putString(KEY_LAST_DIR, path).apply();
    }

    /** Сбрасывает всё, кроме темы оформления. */
    public static void reset(Context context) {
        int theme = themeMode(context);
        prefs(context).edit().clear().putInt(KEY_THEME, theme).apply();
    }
}

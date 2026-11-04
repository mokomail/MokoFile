package com.mokofile.app;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.mokofile.app.data.FileRepository;
import com.mokofile.app.util.Formats;

public class SettingsActivity extends AppCompatActivity {

    private TextView themeValue;
    private TextView viewValue;
    private TextView accessStatus;
    private TextView versionValue;
    private TextView storageValue;
    private TextView storageUsed;
    private TextView storageFree;
    private LinearProgressIndicator storageBar;
    private SwitchMaterial swDynamic;
    private SwitchMaterial swHidden;
    private SwitchMaterial swFolders;
    private SwitchMaterial swConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Settings.dynamicColor(this)) {
            DynamicColors.applyToActivityIfAvailable(this);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        View root = findViewById(R.id.settingsRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.settingsToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        bindViews();
        loadValues();
        setupRows();
        loadStorage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAccess();
    }

    private void bindViews() {
        themeValue = findViewById(R.id.themeValue);
        viewValue = findViewById(R.id.viewValue);
        accessStatus = findViewById(R.id.accessStatus);
        versionValue = findViewById(R.id.versionValue);
        storageValue = findViewById(R.id.storageValue);
        storageUsed = findViewById(R.id.storageUsed);
        storageFree = findViewById(R.id.storageFree);
        storageBar = findViewById(R.id.storageBar);
        swDynamic = findViewById(R.id.swDynamic);
        swHidden = findViewById(R.id.swHidden);
        swFolders = findViewById(R.id.swFolders);
        swConfirm = findViewById(R.id.swConfirm);
    }

    private void loadValues() {
        themeValue.setText(themeLabel(Settings.themeMode(this)));
        viewValue.setText(Settings.grid(this) ? R.string.view_value_grid : R.string.view_value_list);

        swDynamic.setChecked(Settings.dynamicColor(this));
        swHidden.setChecked(Settings.showHidden(this));
        swFolders.setChecked(Settings.foldersFirst(this));
        swConfirm.setChecked(Settings.confirmDelete(this));

        versionValue.setText(versionName() + " (beta)");
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "0.1.0";
        }
    }

    private void setupRows() {
        findViewById(R.id.rowTheme).setOnClickListener(v -> showThemeDialog());
        findViewById(R.id.rowView).setOnClickListener(v -> showViewDialog());

        findViewById(R.id.rowDynamic).setOnClickListener(v -> {
            boolean enabled = !swDynamic.isChecked();
            swDynamic.setChecked(enabled);
            Settings.setDynamicColor(this, enabled);
            recreate();
        });

        findViewById(R.id.rowHidden).setOnClickListener(v -> {
            boolean enabled = !swHidden.isChecked();
            swHidden.setChecked(enabled);
            Settings.setShowHidden(this, enabled);
        });

        findViewById(R.id.rowFolders).setOnClickListener(v -> {
            boolean enabled = !swFolders.isChecked();
            swFolders.setChecked(enabled);
            Settings.setFoldersFirst(this, enabled);
        });

        findViewById(R.id.rowConfirm).setOnClickListener(v -> {
            boolean enabled = !swConfirm.isChecked();
            swConfirm.setChecked(enabled);
            Settings.setConfirmDelete(this, enabled);
        });

        findViewById(R.id.rowAccess).setOnClickListener(v -> openAccessSettings());
        ((MaterialButton) findViewById(R.id.accessButton)).setOnClickListener(v -> openAccessSettings());

        findViewById(R.id.rowRepo).setOnClickListener(v -> openRepository());

        findViewById(R.id.rowReset).setOnClickListener(v -> confirmReset());
    }

    private void showThemeDialog() {
        final int[] modes = {
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                AppCompatDelegate.MODE_NIGHT_NO,
                AppCompatDelegate.MODE_NIGHT_YES
        };
        String[] labels = {
                getString(R.string.theme_value_system),
                getString(R.string.theme_value_light),
                getString(R.string.theme_value_dark)
        };

        int checked = 0;
        int current = Settings.themeMode(this);
        for (int i = 0; i < modes.length; i++) {
            if (modes[i] == current) checked = i;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.theme)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    Settings.setThemeMode(this, modes[which]);
                })
                .show();
    }

    private void showViewDialog() {
        String[] labels = {
                getString(R.string.view_value_list),
                getString(R.string.view_value_grid)
        };
        int checked = Settings.grid(this) ? 1 : 0;

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.default_view)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    Settings.setGrid(this, which == 1);
                    viewValue.setText(which == 1 ? R.string.view_value_grid : R.string.view_value_list);
                    dialog.dismiss();
                })
                .show();
    }

    private String themeLabel(int mode) {
        switch (mode) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                return getString(R.string.theme_value_light);
            case AppCompatDelegate.MODE_NIGHT_YES:
                return getString(R.string.theme_value_dark);
            default:
                return getString(R.string.theme_value_system);
        }
    }

    private void loadStorage() {
        String[] stats = FileRepository.storageStats(Environment.getExternalStorageDirectory());
        long used;
        long total;
        try {
            used = Long.parseLong(stats[0]);
            total = Long.parseLong(stats[1]);
        } catch (NumberFormatException e) {
            return;
        }
        if (total <= 0) return;

        int percent = (int) Math.round(used * 100.0 / total);
        storageBar.setProgressCompat(Math.max(1, percent), false);
        storageValue.setText(Formats.sizeGb(used) + " / " + Formats.sizeGb(total));
        storageUsed.setText("Занято " + Formats.size(used));
        storageFree.setText("Свободно " + Formats.size(total - used));
    }

    private boolean hasAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return true;
    }

    private void updateAccess() {
        boolean granted = hasAccess();
        accessStatus.setText(granted ? R.string.access_granted : R.string.access_missing);
        accessStatus.setTextColor(getColor(granted
                ? R.color.access_granted
                : R.color.access_denied));
    }

    private void openAccessSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
            }
        }
    }

    private void openRepository() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/mokomail/mokofiles")));
        } catch (ActivityNotFoundException ignored) {
        }
    }

    private void confirmReset() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.reset_confirm_title)
                .setMessage(R.string.reset_confirm_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.reset, (dialog, which) -> {
                    Settings.reset(this);
                    recreate();
                })
                .show();
    }
}

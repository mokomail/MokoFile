package com.mokofile.app;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.mokofile.app.data.FileItem;
import com.mokofile.app.data.FileRepository;
import com.mokofile.app.ui.FileAdapter;
import com.mokofile.app.util.Colors;
import com.mokofile.app.util.Formats;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements FileAdapter.Listener {

    private static final String ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private File rootDir;
    private File currentDir;
    private final Deque<File> history = new ArrayDeque<>();

    private final List<FileItem> allItems = new ArrayList<>();
    private final List<FileItem> shownItems = new ArrayList<>();
    private final LinkedHashSet<String> selected = new LinkedHashSet<>();
    private final List<File> clipboard = new ArrayList<>();
    private boolean clipboardCut;

    private FileAdapter adapter;
    private boolean gridMode;
    private boolean sortAsc = true;
    private boolean foldersFirst = true;
    private boolean showHidden;
    private boolean dynamicOn;
    private String sortKey = "name";
    private String filterType = "all";
    private String query = "";
    private boolean searching;
    private boolean selectionMode;
    private boolean permissionAsked;
    private boolean accessGrantedAtStart;
    private int allChipId = View.NO_ID;

    private MaterialToolbar toolbar;
    private MaterialCardView searchCard;
    private EditText searchInput;
    private ImageView searchClose;
    private MaterialCardView storageCard;
    private LinearProgressIndicator storageBar;
    private TextView storageValue;
    private TextView storageUsed;
    private TextView storageFree;
    private View quickScroll;
    private ChipGroup quickRow;
    private View crumbScroll;
    private LinearLayout crumbRow;
    private AppBarLayout appBar;
    private boolean headerExpanded = true;
    private TextView folderTitle;
    private TextView countLabel;
    private MaterialButton sortButton;
    private RecyclerView fileList;
    private View emptyView;
    private TextView emptyText;
    private View selectionBar;
    private TextView selectionCount;
    private View selCopy;
    private View selCut;
    private View selDelete;
    private View selMore;
    private ExtendedFloatingActionButton pasteFab;
    private FloatingActionButton fab;
    private View rootLayout;

    private ActivityResultLauncher<String[]> importLauncher;
    private ActivityResultLauncher<String[]> permissionLauncher;

    private interface TextCallback {
        void onText(String text);
    }

    private static class Action {
        final int icon;
        final String label;
        final Runnable run;

        Action(int icon, String label, Runnable run) {
            this.icon = icon;
            this.label = label;
            this.run = run;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Settings.dynamicColor(this)) {
            DynamicColors.applyToActivityIfAvailable(this);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rootDir = new File(ROOT_PATH);
        accessGrantedAtStart = hasAccess();

        rootLayout = findViewById(R.id.rootLayout);
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return insets;
        });

        bindViews();
        setupToolbar();
        setupList();
        setupChips();
        setupActions();
        setupResultLaunchers();

        gridMode = Settings.grid(this);
        sortKey = Settings.sortKey(this);
        sortAsc = Settings.sortAsc(this);
        foldersFirst = Settings.foldersFirst(this);
        showHidden = Settings.showHidden(this);
        dynamicOn = Settings.dynamicColor(this);

        File saved = new File(Settings.lastDir(this, ROOT_PATH));
        currentDir = saved.isDirectory() ? saved : rootDir;

        setGridMode(gridMode);
        updateSortLabel();
        updateHeader();
        updateSelectionUi();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (searching) {
                    closeSearch();
                } else if (selectionMode) {
                    clearSelection();
                } else if (!history.isEmpty()) {
                    currentDir = history.pop();
                    afterNavigate(false);
                } else {
                    finish();
                }
            }
        });

        ensureAccess();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (hasAccess() && !accessGrantedAtStart) {
            accessGrantedAtStart = true;
            openDir(currentDir, false);
            return;
        }

        // Настройки могли измениться на экране «Настройки».
        if (Settings.dynamicColor(this) != dynamicOn) {
            recreate();
            return;
        }

        boolean newGrid = Settings.grid(this);
        if (newGrid != gridMode) setGridMode(newGrid);

        String newSort = Settings.sortKey(this);
        boolean newAsc = Settings.sortAsc(this);
        if (!newSort.equals(sortKey) || newAsc != sortAsc) {
            sortKey = newSort;
            sortAsc = newAsc;
            applyFilter();
            updateSortLabel();
            updateMenuState();
        }

        boolean newHidden = Settings.showHidden(this);
        if (newHidden != showHidden) {
            showHidden = newHidden;
            refresh();
        }

        boolean newFolders = Settings.foldersFirst(this);
        if (newFolders != foldersFirst) {
            foldersFirst = newFolders;
            applyFilter();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Settings.setGrid(this, gridMode);
        Settings.setSortKey(this, sortKey);
        Settings.setSortAsc(this, sortAsc);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }

    // ---------------------------------------------------------------- setup

    private void bindViews() {
        rootLayout = findViewById(R.id.rootLayout);
        toolbar = findViewById(R.id.toolbar);
        searchCard = findViewById(R.id.searchCard);
        searchInput = findViewById(R.id.searchInput);
        searchClose = findViewById(R.id.searchClose);
        appBar = findViewById(R.id.appBar);
        storageCard = findViewById(R.id.storageCard);
        storageBar = findViewById(R.id.storageBar);
        storageValue = findViewById(R.id.storageValue);
        storageUsed = findViewById(R.id.storageUsed);
        storageFree = findViewById(R.id.storageFree);
        quickScroll = findViewById(R.id.quickScroll);
        quickRow = findViewById(R.id.quickRow);
        crumbScroll = findViewById(R.id.crumbScroll);
        crumbRow = findViewById(R.id.crumbRow);
        folderTitle = findViewById(R.id.folderTitle);
        countLabel = findViewById(R.id.countLabel);
        sortButton = findViewById(R.id.sortButton);
        fileList = findViewById(R.id.fileList);
        emptyView = findViewById(R.id.emptyView);
        emptyText = findViewById(R.id.emptyText);
        selectionBar = findViewById(R.id.selectionBar);
        selectionCount = findViewById(R.id.selectionCount);
        selCopy = findViewById(R.id.selCopy);
        selCut = findViewById(R.id.selCut);
        selDelete = findViewById(R.id.selDelete);
        selMore = findViewById(R.id.selMore);
        pasteFab = findViewById(R.id.pasteFab);
        fab = findViewById(R.id.fab);
    }

    private void setupToolbar() {
        toolbar.inflateMenu(R.menu.menu_toolbar);
        toolbar.setOnMenuItemClickListener(this::onToolbarMenu);
        toolbar.setNavigationOnClickListener(v -> goUp());
    }

    private void setupChips() {
        quickRow.setSingleSelection(true);
        quickRow.setSelectionRequired(true);

        String[][] chips = {
                {getString(R.string.quick_all), "all"},
                {getString(R.string.quick_images), "image"},
                {getString(R.string.quick_videos), "video"},
                {getString(R.string.quick_music), "audio"},
                {getString(R.string.quick_apk), "apk"}
        };

        for (String[] spec : chips) {
            Chip chip = (Chip) LayoutInflater.from(this)
                    .inflate(R.layout.chip_quick, quickRow, false);
            chip.setText(spec[0]);
            chip.setTag(spec[1]);
            chip.setId(View.generateViewId());
            quickRow.addView(chip);
            if (allChipId == View.NO_ID) allChipId = chip.getId();
        }

        quickRow.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty() || adapter == null) return;
            Chip chip = group.findViewById(ids.get(0));
            if (chip == null) return;
            filterType = String.valueOf(chip.getTag());
            updateHeader();
            applyFilter();
        });

        if (allChipId != View.NO_ID) quickRow.check(allChipId);
    }

    private void setupList() {
        adapter = new FileAdapter(this);
        fileList.setLayoutManager(new LinearLayoutManager(this));
        fileList.setAdapter(adapter);

        fileList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 && headerExpanded) {
                    setHeaderExpanded(false, true);
                } else if (dy < 0 && !headerExpanded) {
                    setHeaderExpanded(true, true);
                }
            }
        });
    }

    private void setHeaderExpanded(boolean expanded, boolean animate) {
        headerExpanded = expanded;
        appBar.setExpanded(expanded, animate);
    }

    private void setupActions() {
        fab.setOnClickListener(v -> showCreateSheet());
        pasteFab.setOnClickListener(v -> paste());
        storageCard.setOnClickListener(v -> openDir(rootDir, true));
        sortButton.setOnClickListener(v -> showSortMenu());

        searchClose.setOnClickListener(v -> closeSearch());
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                query = s.toString().trim();
                applyFilter();
            }
        });

        selCopy.setOnClickListener(v -> copySelection(false));
        selCut.setOnClickListener(v -> copySelection(true));
        selDelete.setOnClickListener(v -> confirmDelete(selectedItems()));
        selMore.setOnClickListener(v -> {
            List<FileItem> items = selectedItems();
            if (items.size() == 1) showItemSheet(items.get(0));
            else showSelectionSheet(items);
        });
    }

    private void setupResultLaunchers() {
        importLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(),
                uris -> {
                    if (uris != null && !uris.isEmpty()) importUris(uris);
                });

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean granted = result.get(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    if (granted != null && granted) openDir(rootDir, false);
                });
    }

    // ----------------------------------------------------------- permission

    private boolean hasAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void ensureAccess() {
        if (hasAccess()) {
            openDir(currentDir, false);
            return;
        }

        if (permissionAsked) return;
        permissionAsked = true;

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.permission_title)
                .setMessage(R.string.permission_message)
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, (d, w) -> finish())
                .setPositiveButton(R.string.permission_grant, (d, w) -> requestAccess())
                .show();
    }

    private void requestAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
            }
        } else {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            });
        }
    }

    // ----------------------------------------------------------- navigation

    private void openDir(File dir, boolean addHistory) {
        if (dir == null || !dir.isDirectory()) return;

        if (addHistory && currentDir != null && !currentDir.equals(dir)) {
            history.push(currentDir);
        }

        currentDir = dir;
        Settings.setLastDir(this, dir.getAbsolutePath());

        selected.clear();
        selectionMode = false;
        filterType = "all";
        query = "";
        if (quickRow != null && allChipId != View.NO_ID) quickRow.check(allChipId);

        if (searching) {
            searching = false;
            searchCard.setVisibility(View.GONE);
            storageCard.setVisibility(View.VISIBLE);
            quickScroll.setVisibility(View.VISIBLE);
            crumbScroll.setVisibility(View.VISIBLE);
        }
        searchInput.setText("");
        hideKeyboard();

        afterNavigate(true);
    }

    private void afterNavigate(boolean toTop) {
        if (currentDir == null || !currentDir.isDirectory()) {
            currentDir = rootDir;
        }
        updateHeader();
        updateSelectionUi();
        refresh();
        if (toTop) fileList.scrollToPosition(0);
    }

    private void goUp() {
        if (!history.isEmpty()) {
            currentDir = history.pop();
            afterNavigate(true);
        } else {
            finish();
        }
    }

    private void updateHeader() {
        boolean atRoot = currentDir == null || ROOT_PATH.equals(currentDir.getAbsolutePath());

        toolbar.setTitle(R.string.app_name);
        toolbar.setSubtitle(atRoot ? getString(R.string.storage_title) : relativePath(currentDir));
        toolbar.setNavigationIcon(atRoot ? null : ContextCompat.getDrawable(this, R.drawable.ic_back));

        String title;
        if (atRoot) title = getString(R.string.all_files);
        else title = currentDir.getName();

        if (!"all".equals(filterType)) {
            switch (filterType) {
                case "image":
                    title = getString(R.string.quick_images);
                    break;
                case "video":
                    title = getString(R.string.quick_videos);
                    break;
                case "audio":
                    title = getString(R.string.quick_music);
                    break;
                case "apk":
                    title = getString(R.string.quick_apk);
                    break;
                default:
                    break;
            }
        }
        folderTitle.setText(title);

        buildCrumbs();
        updateMenuState();
    }

    private String relativePath(File dir) {
        String path = dir.getAbsolutePath();
        if (path.startsWith(ROOT_PATH)) {
            path = path.substring(ROOT_PATH.length());
            if (path.startsWith("/")) path = path.substring(1);
        }
        return path.isEmpty() ? getString(R.string.storage_title) : path;
    }

    private void buildCrumbs() {
        crumbRow.removeAllViews();
        if (currentDir == null) return;

        List<File> chain = new ArrayList<>();
        File f = currentDir;
        while (f != null) {
            chain.add(0, f);
            if (ROOT_PATH.equals(f.getAbsolutePath())) break;
            f = f.getParentFile();
        }

        int onSurface = MaterialColors.getColor(crumbRow, R.attr.colorOnSurface);
        int onVariant = MaterialColors.getColor(crumbRow, R.attr.colorOnSurfaceVariant);

        for (int i = 0; i < chain.size(); i++) {
            final File target = chain.get(i);
            boolean last = i == chain.size() - 1;

            TextView crumb = new TextView(this);
            crumb.setText(i == 0 ? getString(R.string.storage_title) : target.getName());
            crumb.setTextSize(13);
            crumb.setSingleLine(true);
            crumb.setPadding(dp(10), dp(6), dp(10), dp(6));
            crumb.setTextColor(last ? onSurface : onVariant);
            if (last) crumb.setTypeface(crumb.getTypeface(), android.graphics.Typeface.BOLD);
            crumb.setBackgroundResource(R.drawable.bg_crumb);
            crumb.setOnClickListener(v -> {
                if (!target.equals(currentDir)) openDir(target, true);
            });
            crumbRow.addView(crumb);

            if (!last) {
                ImageView chevron = new ImageView(this);
                chevron.setImageResource(R.drawable.ic_chevron);
                chevron.setColorFilter(onVariant);
                int size = dp(14);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
                lp.setMarginStart(dp(2));
                lp.setMarginEnd(dp(2));
                chevron.setLayoutParams(lp);
                crumbRow.addView(chevron);
            }
        }

        crumbScroll.post(() -> {
            if (crumbScroll instanceof android.widget.HorizontalScrollView) {
                android.widget.HorizontalScrollView scroll = (android.widget.HorizontalScrollView) crumbScroll;
                scroll.fullScroll(View.FOCUS_RIGHT);
            }
        });
    }

    // ---------------------------------------------------------------- data

    private void refresh() {
        if (currentDir == null) return;
        final File dir = currentDir;

        io.execute(() -> {
            List<FileItem> items = FileRepository.list(dir, showHidden);
            String[] stats = FileRepository.storageStats(rootDir);
            main.post(() -> {
                if (currentDir == null || !currentDir.equals(dir)) return;
                allItems.clear();
                allItems.addAll(items);
                applyFilter();
                updateStorage(stats);
            });
        });
    }

    private void applyFilter() {
        if (query.isEmpty() && "all".equals(filterType)) {
            shownItems.clear();
            shownItems.addAll(allItems);
        } else {
            shownItems.clear();
            String q = query.toLowerCase(Locale.getDefault());
            for (FileItem item : allItems) {
                if (!"all".equals(filterType) && !item.type.equals(filterType)) continue;
                if (!q.isEmpty() && !item.name.toLowerCase(Locale.getDefault()).contains(q)) continue;
                shownItems.add(item);
            }
        }

        Collections.sort(shownItems, comparator());

        adapter.setItems(shownItems);
        adapter.setSelected(selected);

        boolean empty = shownItems.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        emptyText.setText(query.isEmpty() ? R.string.empty_folder : R.string.nothing_found);

        int n = shownItems.size();
        countLabel.setText(n + " " + Formats.plural(n, "элемент", "элемента", "элементов"));

        updateStorageCardVisibility();
    }

    private Comparator<FileItem> comparator() {
        return (a, b) -> {
            if (foldersFirst && a.directory != b.directory) return a.directory ? -1 : 1;
            if (foldersFirst && a.directory) return a.name.compareToIgnoreCase(b.name);

            int cmp;
            switch (sortKey) {
                case "size":
                    cmp = Long.compare(a.size, b.size);
                    break;
                case "date":
                    cmp = Long.compare(a.modified, b.modified);
                    break;
                case "type":
                    cmp = a.type.compareTo(b.type);
                    break;
                case "name":
                default:
                    cmp = a.name.compareToIgnoreCase(b.name);
                    break;
            }
            if (cmp == 0) {
                cmp = a.name.compareToIgnoreCase(b.name);
                return cmp;
            }
            return sortAsc ? cmp : -cmp;
        };
    }

    private void updateStorageCardVisibility() {
        if (searching) {
            storageCard.setVisibility(View.GONE);
            quickScroll.setVisibility(View.GONE);
            crumbScroll.setVisibility(View.GONE);
        } else {
            storageCard.setVisibility(View.VISIBLE);
            quickScroll.setVisibility(View.VISIBLE);
            crumbScroll.setVisibility(View.VISIBLE);
        }
    }

    private void updateStorage(String[] stats) {
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
        storageBar.setProgressCompat(Math.max(1, percent), true);
        storageValue.setText(Formats.sizeGb(used) + " / " + Formats.sizeGb(total));
        storageUsed.setText("Занято " + Formats.size(used));
        storageFree.setText("Свободно " + Formats.size(total - used));
    }

    // ------------------------------------------------------------- items

    @Override
    public void onItemClick(FileItem item) {
        if (selectionMode) {
            toggleSelection(item);
            return;
        }
        if (item.directory) {
            openDir(item.file, true);
        } else {
            openFile(item);
        }
    }

    @Override
    public void onItemLongClick(FileItem item) {
        if (!selectionMode) selectionMode = true;
        toggleSelection(item);
    }

    private void openFile(FileItem item) {
        if ("image".equals(item.type)) {
            startActivity(new Intent(this, ImageViewerActivity.class)
                    .putExtra(ImageViewerActivity.EXTRA_PATH, item.file.getAbsolutePath()));
            return;
        }
        if ("audio".equals(item.type) || "video".equals(item.type)) {
            startActivity(new Intent(this, MediaPlayerActivity.class)
                    .putExtra(MediaPlayerActivity.EXTRA_PATH, item.file.getAbsolutePath()));
            return;
        }

        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", item.file);
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, mimeOf(item))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            snackbar(getString(R.string.no_app));
        }
    }

    private String mimeOf(FileItem item) {
        switch (item.type) {
            case "image":
                return "image/*";
            case "video":
                return "video/*";
            case "audio":
                return "audio/*";
            case "apk":
                return "application/vnd.android.package-archive";
            case "archive":
                return "application/zip";
            default:
                break;
        }

        String ext = item.extension();
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        return mime != null ? mime : "*/*";
    }

    // -------------------------------------------------------- selection

    private void toggleSelection(FileItem item) {
        String key = item.file.getAbsolutePath();
        if (selected.contains(key)) selected.remove(key);
        else selected.add(key);

        if (selected.isEmpty()) selectionMode = false;
        updateSelectionUi();
    }

    private void updateSelectionUi() {
        boolean active = selectionMode && !selected.isEmpty();

        selectionBar.setVisibility(active ? View.VISIBLE : View.GONE);
        fab.setVisibility(active ? View.GONE : View.VISIBLE);
        if (active) selectionCount.setText(String.valueOf(selected.size()));

        adapter.setSelected(selected);
        updatePasteFab();
    }

    private void clearSelection() {
        selected.clear();
        selectionMode = false;
        updateSelectionUi();
    }

    private void selectAll() {
        if (shownItems.isEmpty()) return;
        selectionMode = true;
        for (FileItem item : shownItems) selected.add(item.file.getAbsolutePath());
        updateSelectionUi();
    }

    private List<FileItem> selectedItems() {
        List<FileItem> out = new ArrayList<>();
        for (FileItem item : allItems) {
            if (selected.contains(item.file.getAbsolutePath())) out.add(item);
        }
        return out;
    }

    private void updatePasteFab() {
        boolean show = !clipboard.isEmpty() && !selectionMode;
        pasteFab.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // ----------------------------------------------------------- clipboard

    private void copySelection(boolean cut) {
        List<FileItem> items = selectedItems();
        if (items.isEmpty()) return;

        clipboard.clear();
        for (FileItem item : items) clipboard.add(item.file);
        clipboardCut = cut;

        int n = clipboard.size();
        clearSelection();
        snackbar(getString(R.string.clipboard_ready, n));
    }

    private void paste() {
        if (clipboard.isEmpty()) {
            snackbar(getString(R.string.nothing_to_paste));
            return;
        }

        final File dest = currentDir;
        final List<File> source = new ArrayList<>(clipboard);
        final boolean cut = clipboardCut;

        io.execute(() -> {
            int done = 0;
            String error = null;
            for (File file : source) {
                try {
                    if (!file.exists()) continue;
                    if (dest.getAbsolutePath().startsWith(file.getAbsolutePath())) continue;
                    if (cut && dest.equals(file.getParentFile())) continue;
                    if (cut) FileRepository.move(file, dest);
                    else FileRepository.copy(file, dest);
                    done++;
                } catch (Exception e) {
                    error = e.getMessage();
                }
            }

            final int count = done;
            final String message = error;
            main.post(() -> {
                if (cut) {
                    clipboard.clear();
                    clipboardCut = false;
                    updatePasteFab();
                }
                snackbar(cut ? getString(R.string.moved, count) : getString(R.string.copied, count));
                if (message != null) snackbar(getString(R.string.error_generic, message));
                refresh();
            });
        });
    }

    // ------------------------------------------------------------ dialogs

    private void showCreateSheet() {
        List<Action> actions = new ArrayList<>();
        actions.add(new Action(R.drawable.ic_new_folder, getString(R.string.new_folder),
                this::createFolder));
        actions.add(new Action(R.drawable.ic_upload, getString(R.string.import_file),
                () -> importLauncher.launch(new String[]{"*/*"})));
        showSheet(getString(R.string.create_title), relativePath(currentDir), actions);
    }

    private void createFolder() {
        showTextDialog(getString(R.string.new_folder), getString(R.string.create), "",
                getString(R.string.new_folder), text -> {
                    final File dest = currentDir;
                    io.execute(() -> {
                        try {
                            FileRepository.createDir(dest, text);
                            main.post(() -> {
                                snackbar(getString(R.string.created));
                                refresh();
                            });
                        } catch (Exception e) {
                            main.post(() -> snackbar(getString(R.string.error_generic, e.getMessage())));
                        }
                    });
                });
    }

    private void showRenameDialog(FileItem item) {
        showTextDialog(getString(R.string.rename_title), getString(R.string.save), item.name,
                getString(R.string.name_hint), text -> {
                    io.execute(() -> {
                        try {
                            FileRepository.rename(item.file, text);
                            main.post(this::refresh);
                        } catch (Exception e) {
                            main.post(() -> snackbar(getString(R.string.error_generic, e.getMessage())));
                        }
                    });
                });
    }

    private void showTextDialog(String title, String positive, String initial, String hint,
                                TextCallback callback) {
        View view = getLayoutInflater().inflate(R.layout.dialog_text, null);
        TextInputLayout layout = view.findViewById(R.id.inputLayout);
        TextInputEditText input = view.findViewById(R.id.input);

        layout.setHint(hint);
        if (initial != null && !initial.isEmpty()) {
            input.setText(initial);
            input.setSelection(0, initial.length());
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(view)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(positive, null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String text = input.getText() == null ? "" : input.getText().toString().trim();
                if (text.isEmpty()) {
                    layout.setError(getString(R.string.name_empty));
                    return;
                }
                layout.setError(null);
                dialog.dismiss();
                callback.onText(text);
            });
            input.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        });

        dialog.show();
    }

    private void confirmDelete(List<FileItem> items) {
        if (items.isEmpty()) return;

        if (!Settings.confirmDelete(this)) {
            deleteItems(items);
            return;
        }

        int n = items.size();
        String title = n == 1
                ? getString(R.string.delete_title)
                : "Удалить " + n + " " + Formats.plural(n, "элемент", "элемента", "элементов") + "?";

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(R.string.delete_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete_confirm, (d, w) -> deleteItems(items))
                .show();
    }

    private void deleteItems(List<FileItem> items) {
        final List<FileItem> targets = new ArrayList<>(items);
        io.execute(() -> {
            int done = 0;
            for (FileItem item : targets) {
                if (FileRepository.delete(item.file)) done++;
            }
            final int count = done;
            main.post(() -> {
                snackbar(getString(R.string.deleted, count));
                clearSelection();
                refresh();
            });
        });
    }

    // ------------------------------------------------------------ sheets

    private void showItemSheet(FileItem item) {
        List<Action> actions = new ArrayList<>();

        if (item.directory) {
            actions.add(new Action(R.drawable.ic_folder_open, getString(R.string.action_open),
                    () -> openDir(item.file, true)));
        } else {
            actions.add(new Action(R.drawable.ic_file, getString(R.string.action_open),
                    () -> openFile(item)));
            actions.add(new Action(R.drawable.ic_upload, getString(R.string.action_share),
                    () -> shareItem(item)));
        }

        actions.add(new Action(R.drawable.ic_edit, getString(R.string.action_rename),
                () -> showRenameDialog(item)));
        actions.add(new Action(R.drawable.ic_copy, getString(R.string.action_copy),
                () -> {
                    clipboard.clear();
                    clipboard.add(item.file);
                    clipboardCut = false;
                    clearSelection();
                    updatePasteFab();
                    snackbar(getString(R.string.clipboard_ready, 1));
                }));
        actions.add(new Action(R.drawable.ic_cut, getString(R.string.action_move),
                () -> {
                    clipboard.clear();
                    clipboard.add(item.file);
                    clipboardCut = true;
                    clearSelection();
                    updatePasteFab();
                    snackbar(getString(R.string.clipboard_ready, 1));
                }));
        actions.add(new Action(R.drawable.ic_info, getString(R.string.action_details),
                () -> showDetails(item)));
        actions.add(new Action(R.drawable.ic_delete, getString(R.string.action_delete),
                () -> confirmDelete(Collections.singletonList(item))));
        actions.add(new Action(R.drawable.ic_select_all, getString(R.string.action_select),
                () -> {
                    selectionMode = true;
                    selected.add(item.file.getAbsolutePath());
                    updateSelectionUi();
                }));

        showSheet(item.name, Formats.typeLabel(item.type), actions);
    }

    private void showSelectionSheet(List<FileItem> items) {
        List<Action> actions = new ArrayList<>();
        actions.add(new Action(R.drawable.ic_copy, getString(R.string.action_copy),
                () -> copySelection(false)));
        actions.add(new Action(R.drawable.ic_cut, getString(R.string.action_move),
                () -> copySelection(true)));
        actions.add(new Action(R.drawable.ic_select_all, getString(R.string.select_all),
                this::selectAll));
        actions.add(new Action(R.drawable.ic_delete, getString(R.string.action_delete),
                () -> confirmDelete(items)));

        showSheet(getString(R.string.app_name),
                items.size() + " " + Formats.plural(items.size(), "элемент", "элемента", "элементов"),
                actions);
    }

    private void showDetails(FileItem item) {
        io.execute(() -> {
            String contents = null;
            if (item.directory) {
                File[] children = item.file.listFiles();
                int n = children == null ? 0 : children.length;
                contents = n + " " + Formats.plural(n, "элемент", "элемента", "элементов");
            }

            final String contentsValue = contents;
            main.post(() -> {
                BottomSheetDialog sheet = new BottomSheetDialog(this);
                LinearLayout root = new LinearLayout(this);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(0, dp(4), 0, dp(24));

                View header = getLayoutInflater().inflate(R.layout.sheet_header, root, false);
                ((TextView) header.findViewById(R.id.sheetTitle)).setText(R.string.details_title);
                ((TextView) header.findViewById(R.id.sheetSubtitle)).setText(item.name);
                root.addView(header);

                addInfoRow(root, getString(R.string.details_name), item.name);
                addInfoRow(root, getString(R.string.details_type),
                        Formats.typeLabel(item.type));
                if (item.directory) {
                    addInfoRow(root, getString(R.string.details_items), contentsValue);
                } else {
                    addInfoRow(root, getString(R.string.details_size), Formats.size(item.size));
                }
                addInfoRow(root, getString(R.string.details_path), item.file.getAbsolutePath());
                addInfoRow(root, getString(R.string.details_modified), fullDate(item.modified));

                sheet.setContentView(root);
                sheet.show();
            });
        });
    }

    private void addInfoRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(24), dp(9), dp(24), dp(9));

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(12);
        labelView.setTextColor(MaterialColors.getColor(parent, R.attr.colorOnSurfaceVariant));

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextSize(15);
        valueView.setTextColor(MaterialColors.getColor(parent, R.attr.colorOnSurface));
        valueView.setTextIsSelectable(true);

        row.addView(labelView);
        row.addView(valueView);
        parent.addView(row);
    }

    private String fullDate(long millis) {
        if (millis <= 0) return "—";
        return new SimpleDateFormat("d MMMM yyyy, HH:mm", new Locale("ru"))
                .format(new Date(millis));
    }

    private void showSheet(String title, String subtitle, List<Action> actions) {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, dp(4), 0, dp(16));

        View header = getLayoutInflater().inflate(R.layout.sheet_header, root, false);
        ((TextView) header.findViewById(R.id.sheetTitle)).setText(title);
        TextView sub = header.findViewById(R.id.sheetSubtitle);
        if (subtitle == null || subtitle.isEmpty()) {
            sub.setVisibility(View.GONE);
        } else {
            sub.setText(subtitle);
        }
        root.addView(header);

        for (Action action : actions) {
            View row = getLayoutInflater().inflate(R.layout.sheet_action_row, root, false);
            ((ImageView) row.findViewById(R.id.actionIcon)).setImageResource(action.icon);
            ((TextView) row.findViewById(R.id.actionLabel)).setText(action.label);
            row.setOnClickListener(v -> {
                sheet.dismiss();
                action.run.run();
            });
            root.addView(row);
        }

        sheet.setContentView(root);
        sheet.show();
    }

    private void shareItem(FileItem item) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", item.file);
            Intent send = new Intent(Intent.ACTION_SEND)
                    .setType(mimeOf(item))
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, getString(R.string.action_share)));
        } catch (Exception e) {
            snackbar(getString(R.string.cannot_open));
        }
    }

    // ------------------------------------------------------------- import

    private void importUris(List<Uri> uris) {
        final File dest = currentDir;

        io.execute(() -> {
            int done = 0;
            String error = null;

            for (Uri uri : uris) {
                String name = displayName(uri);
                if (name == null || name.trim().isEmpty()) name = "imported_" + System.currentTimeMillis();
                File target = FileRepository.unique(dest, name);

                try (InputStream in = getContentResolver().openInputStream(uri);
                     OutputStream out = new FileOutputStream(target)) {
                    if (in == null) throw new IOException("Нет доступа к файлу");
                    byte[] buffer = new byte[64 * 1024];
                    int read;
                    while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
                    done++;
                } catch (Exception e) {
                    error = e.getMessage();
                }
            }

            final int count = done;
            final String message = error;
            main.post(() -> {
                snackbar(getString(R.string.imported, count));
                if (message != null) snackbar(getString(R.string.error_generic, message));
                refresh();
            });
        });
    }

    private String displayName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) return cursor.getString(index);
            }
        } catch (Exception ignored) {
        }
        String path = uri.getLastPathSegment();
        if (path == null) return null;
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    // --------------------------------------------------------------- menu

    private boolean onToolbarMenu(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_search) {
            toggleSearch();
            return true;
        }
        if (id == R.id.action_view) {
            setGridMode(!gridMode);
            return true;
        }
        if (id == R.id.action_select_all) {
            selectAll();
            return true;
        }
        if (id == R.id.action_refresh) {
            refresh();
            return true;
        }
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return false;
    }

    private void updateMenuState() {
        Menu menu = toolbar.getMenu();
        MenuItem view = menu.findItem(R.id.action_view);
        if (view != null) {
            view.setIcon(gridMode ? R.drawable.ic_list : R.drawable.ic_grid);
            view.setTitle(gridMode ? R.string.view_list : R.string.view_grid);
        }
    }

    private void setGridMode(boolean grid) {
        gridMode = grid;
        Settings.setGrid(this, grid);
        adapter.setMode(grid ? FileAdapter.MODE_GRID : FileAdapter.MODE_LIST);
        fileList.setLayoutManager(grid
                ? new GridLayoutManager(this, 2)
                : new LinearLayoutManager(this));
        updateMenuState();
    }

    private void showSortMenu() {
        PopupMenu popup = new PopupMenu(this, sortButton);
        Menu menu = popup.getMenu();
        menu.add(0, 1, 0, R.string.sort_name).setCheckable(true).setChecked("name".equals(sortKey));
        menu.add(0, 2, 1, R.string.sort_size).setCheckable(true).setChecked("size".equals(sortKey));
        menu.add(0, 3, 2, R.string.sort_date).setCheckable(true).setChecked("date".equals(sortKey));
        menu.add(0, 4, 3, R.string.sort_type).setCheckable(true).setChecked("type".equals(sortKey));
        menu.add(0, 5, 4, sortAsc ? "По возрастанию" : "По убыванию");

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 5) {
                sortAsc = !sortAsc;
                Settings.setSortAsc(this, sortAsc);
                applyFilter();
                updateSortLabel();
            } else if (id >= 1 && id <= 4) {
                String key = id == 1 ? "name" : id == 2 ? "size" : id == 3 ? "date" : "type";
                chooseSort(key);
            }
            return true;
        });
        popup.show();
    }

    private void chooseSort(String key) {
        if (key.equals(sortKey)) {
            sortAsc = !sortAsc;
        } else {
            sortKey = key;
            sortAsc = true;
        }
        Settings.setSortKey(this, sortKey);
        Settings.setSortAsc(this, sortAsc);
        applyFilter();
        updateSortLabel();
        updateMenuState();
    }

    private void updateSortLabel() {
        String label;
        switch (sortKey) {
            case "size":
                label = getString(R.string.sort_size);
                break;
            case "date":
                label = getString(R.string.sort_date);
                break;
            case "type":
                label = getString(R.string.sort_type);
                break;
            default:
                label = getString(R.string.sort_name);
                break;
        }
        sortButton.setText(label + (sortAsc ? " ↑" : " ↓"));
    }

    // -------------------------------------------------------------- search

    private void toggleSearch() {
        if (searching) closeSearch();
        else openSearch();
    }

    private void openSearch() {
        searching = true;
        searchCard.setVisibility(View.VISIBLE);
        storageCard.setVisibility(View.GONE);
        quickScroll.setVisibility(View.GONE);
        crumbScroll.setVisibility(View.GONE);
        searchInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
    }

    private void closeSearch() {
        if (!searching && query.isEmpty()) return;
        searching = false;
        query = "";
        searchInput.setText("");
        searchCard.setVisibility(View.GONE);
        storageCard.setVisibility(View.VISIBLE);
        quickScroll.setVisibility(View.VISIBLE);
        crumbScroll.setVisibility(View.VISIBLE);
        searchInput.clearFocus();
        hideKeyboard();
        applyFilter();
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        if (focus == null) focus = searchInput;
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
    }

    // --------------------------------------------------------------- misc

    private int dp(int value) {
        return Colors.dp(this, value);
    }

    private void snackbar(String text) {
        Snackbar.make(rootLayout, text, Snackbar.LENGTH_SHORT).show();
    }
}

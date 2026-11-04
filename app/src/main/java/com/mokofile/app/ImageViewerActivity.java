package com.mokofile.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.mokofile.app.data.FileItem;
import com.mokofile.app.ui.ZoomImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Полноэкранный просмотр изображений с зумом и свайпом. */
public class ImageViewerActivity extends AppCompatActivity {

    public static final String EXTRA_PATH = "path";

    private final List<File> images = new ArrayList<>();
    private final ExecutorService decoder = Executors.newFixedThreadPool(2);

    private RecyclerView pager;
    private LinearLayoutManager layoutManager;
    private PagerSnapHelper snapHelper;
    private TextView titleView;
    private TextView counter;
    private View topBar;

    private int current;
    private boolean chromeVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        File start = new File(String.valueOf(getIntent().getStringExtra(EXTRA_PATH)));
        collectImages(start);
        if (images.isEmpty()) {
            finish();
            return;
        }

        pager = findViewById(R.id.pager);
        titleView = findViewById(R.id.title);
        counter = findViewById(R.id.counter);
        topBar = findViewById(R.id.topBar);

        ImageButton back = findViewById(R.id.backButton);
        back.setOnClickListener(v -> finish());
        findViewById(R.id.shareButton).setOnClickListener(v -> shareCurrent());

        View root = findViewById(R.id.viewerRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            topBar.setPadding(dp(8), bars.top + dp(6), dp(8), dp(6));
            counter.setTranslationY(-bars.bottom);
            return insets;
        });

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        layoutManager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        pager.setLayoutManager(layoutManager);
        snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(pager);
        pager.setAdapter(new ImageAdapter());
        pager.setItemViewCacheSize(2);
        pager.scrollToPosition(current);

        pager.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    View snapped = snapHelper.findSnapView(layoutManager);
                    if (snapped != null) {
                        int position = layoutManager.getPosition(snapped);
                        if (position != RecyclerView.NO_POSITION) setCurrent(position);
                    }
                }
            }
        });

        setCurrent(current);
    }

    @Override
    protected void onDestroy() {
        decoder.shutdownNow();
        super.onDestroy();
    }

    private void collectImages(File start) {
        File dir = start.getParentFile();
        File[] files = dir != null ? dir.listFiles() : null;
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && "image".equals(FileItem.typeOf(file))) images.add(file);
            }
        }
        images.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        current = images.indexOf(start);
        if (current < 0) {
            current = 0;
            images.clear();
            images.add(start);
        }
    }

    private void setCurrent(int position) {
        current = position;
        titleView.setText(images.get(current).getName());
        counter.setText((current + 1) + " / " + images.size());
    }

    private void toggleChrome() {
        chromeVisible = !chromeVisible;
        int visibility = chromeVisible ? View.VISIBLE : View.GONE;
        topBar.setVisibility(visibility);
        counter.setVisibility(visibility);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (chromeVisible) {
            controller.show(WindowInsetsCompat.Type.systemBars());
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars());
        }
    }

    private void shareCurrent() {
        try {
            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", images.get(current));
            Intent intent = new Intent(Intent.ACTION_SEND)
                    .setType("image/*")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, getString(R.string.share)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.cannot_open, Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ZoomImageView image = (ZoomImageView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_image_page, parent, false);
            image.setOnTapListener(ImageViewerActivity.this::toggleChrome);
            return new Holder(image);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            File file = images.get(position);
            String path = file.getAbsolutePath();
            holder.image.setTag(path);
            holder.image.setImageBitmap(null);

            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;

            decoder.execute(() -> {
                Bitmap bitmap = decode(path, width, height);
                holder.image.post(() -> {
                    if (isDestroyed()) {
                        if (bitmap != null) bitmap.recycle();
                        return;
                    }
                    if (path.equals(holder.image.getTag())) {
                        holder.image.setImageBitmap(bitmap);
                    } else if (bitmap != null) {
                        bitmap.recycle();
                    }
                });
            });
        }

        @Override
        public int getItemCount() {
            return images.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final ZoomImageView image;

            Holder(ZoomImageView image) {
                super(image);
                this.image = image;
            }
        }
    }

    private static Bitmap decode(String path, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        if (options.outWidth <= 0 || options.outHeight <= 0) return null;

        int sample = 1;
        while (options.outWidth / sample > reqWidth * 2
                && options.outHeight / sample > reqHeight * 2) {
            sample *= 2;
        }

        options.inJustDecodeBounds = false;
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        if (bitmap == null) return null;

        int rotation = 0;
        try {
            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotation = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotation = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotation = 270;
                    break;
                default:
                    break;
            }
        } catch (Exception ignored) {
        }

        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotated != bitmap) bitmap.recycle();
            bitmap = rotated;
        }
        return bitmap;
    }
}

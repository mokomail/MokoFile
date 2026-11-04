package com.mokofile.app;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mokofile.app.data.FileItem;

import java.io.File;
import java.util.Locale;

/** Встроенный плеер для музыки и видео. */
public class MediaPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_PATH = "path";

    private final Handler handler = new Handler(Looper.getMainLooper());

    private VideoView videoView;
    private View audioPanel;
    private TextView titleView;
    private TextView audioTitle;
    private TextView positionLabel;
    private TextView durationLabel;
    private SeekBar seek;
    private FloatingActionButton playButton;
    private View topBar;
    private View controlsCard;

    private MediaPlayer audioPlayer;
    private File file;
    private boolean isAudio;
    private boolean prepared;
    private boolean seeking;
    private boolean resumed = true;
    private int durationMs = 1;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!prepared) return;
            if (!seeking) {
                int position = currentPosition();
                seek.setProgress(position);
                positionLabel.setText(formatTime(position));
            }
            if (isPlaying()) {
                handler.postDelayed(this, 400);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        file = new File(String.valueOf(getIntent().getStringExtra(EXTRA_PATH)));
        isAudio = "audio".equals(FileItem.typeOf(file));

        videoView = findViewById(R.id.videoView);
        audioPanel = findViewById(R.id.audioPanel);
        audioTitle = findViewById(R.id.audioTitle);
        titleView = findViewById(R.id.title);
        positionLabel = findViewById(R.id.position);
        durationLabel = findViewById(R.id.duration);
        seek = findViewById(R.id.seek);
        playButton = findViewById(R.id.playButton);
        topBar = findViewById(R.id.topBar);
        controlsCard = findViewById(R.id.controlsCard);

        titleView.setText(file.getName());

        if (isAudio) {
            videoView.setVisibility(View.GONE);
            audioPanel.setVisibility(View.VISIBLE);
            audioTitle.setText(file.getName());
        } else {
            videoView.setVisibility(View.VISIBLE);
            audioPanel.setVisibility(View.GONE);
        }

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        findViewById(R.id.shareButton).setOnClickListener(v -> shareFile());
        playButton.setOnClickListener(v -> togglePlayback());

        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) positionLabel.setText(formatTime(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seeking = false;
                seekTo(seekBar.getProgress());
            }
        });

        View root = findViewById(R.id.mediaRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            topBar.setPadding(dp(8), bars.top + dp(6), dp(8), dp(6));
            controlsCard.setTranslationY(-bars.bottom);
            return insets;
        });

        setPlayIcon(false);
        prepare();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
    }

    @Override
    protected void onPause() {
        resumed = false;
        if (prepared && isPlaying()) pausePlayback();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (audioPlayer != null) {
            try {
                audioPlayer.release();
            } catch (Exception ignored) {
            }
            audioPlayer = null;
        }
        if (videoView != null) videoView.stopPlayback();
        super.onDestroy();
    }

    private void prepare() {
        Uri uri;
        try {
            uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", file);
        } catch (Exception e) {
            cannotPlay();
            return;
        }

        if (isAudio) {
            audioPlayer = new MediaPlayer();
            audioPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            audioPlayer.setOnPreparedListener(mp -> onPrepared(mp.getDuration()));
            audioPlayer.setOnCompletionListener(mp -> onCompleted());
            audioPlayer.setOnErrorListener((mp, what, extra) -> {
                cannotPlay();
                return true;
            });
            try {
                audioPlayer.setDataSource(this, uri);
                audioPlayer.prepareAsync();
            } catch (Exception e) {
                cannotPlay();
            }
        } else {
            videoView.setOnPreparedListener(mp -> onPrepared(mp.getDuration()));
            videoView.setOnCompletionListener(mp -> onCompleted());
            videoView.setOnErrorListener((mp, what, extra) -> {
                cannotPlay();
                return true;
            });
            videoView.setVideoURI(uri);
        }
    }

    private void onPrepared(int duration) {
        prepared = true;
        durationMs = Math.max(1, duration);
        seek.setMax(durationMs);
        durationLabel.setText(formatTime(durationMs));
        positionLabel.setText(formatTime(0));
        if (resumed) startPlayback();
    }

    private void togglePlayback() {
        if (!prepared) return;
        if (isPlaying()) pausePlayback();
        else startPlayback();
    }

    private void startPlayback() {
        if (!prepared) return;
        try {
            if (isAudio) audioPlayer.start();
            else videoView.start();
        } catch (Exception e) {
            cannotPlay();
            return;
        }
        setPlayIcon(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    private void pausePlayback() {
        if (!prepared) return;
        try {
            if (isAudio) audioPlayer.pause();
            else videoView.pause();
        } catch (Exception ignored) {
        }
        setPlayIcon(false);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        handler.removeCallbacks(ticker);
    }

    private void onCompleted() {
        setPlayIcon(false);
        handler.removeCallbacks(ticker);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        seek.setProgress(0);
        positionLabel.setText(formatTime(0));
    }

    private void seekTo(int positionMs) {
        if (!prepared) return;
        try {
            if (isAudio) audioPlayer.seekTo(positionMs);
            else videoView.seekTo(positionMs);
        } catch (Exception ignored) {
        }
        positionLabel.setText(formatTime(positionMs));
    }

    private boolean isPlaying() {
        if (!prepared) return false;
        try {
            return isAudio ? audioPlayer.isPlaying() : videoView.isPlaying();
        } catch (Exception e) {
            return false;
        }
    }

    private int currentPosition() {
        try {
            return isAudio ? audioPlayer.getCurrentPosition() : videoView.getCurrentPosition();
        } catch (Exception e) {
            return 0;
        }
    }

    private void setPlayIcon(boolean playing) {
        playButton.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
        playButton.setContentDescription(getString(playing ? R.string.pause : R.string.play));
    }

    private void shareFile() {
        try {
            Uri uri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND)
                    .setType(isAudio ? "audio/*" : "video/*")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, getString(R.string.share)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.cannot_open, Toast.LENGTH_SHORT).show();
        }
    }

    private void cannotPlay() {
        Toast.makeText(this, R.string.cannot_play, Toast.LENGTH_SHORT).show();
    }

    private String formatTime(int millis) {
        int totalSeconds = Math.max(0, millis / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}

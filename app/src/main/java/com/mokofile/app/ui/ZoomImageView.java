package com.mokofile.app.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

/** Картинка с пинч-зумом, перетаскиванием и двойным тапом. */
public class ZoomImageView extends AppCompatImageView {

    public interface OnTapListener {
        void onTap();
    }

    private static final float MAX_SCALE = 4f;
    private static final float DOUBLE_TAP_SCALE = 2.5f;

    private final Matrix baseMatrix = new Matrix();
    private final Matrix userMatrix = new Matrix();
    private final Matrix drawMatrix = new Matrix();
    private final RectF rect = new RectF();

    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;

    @Nullable
    private OnTapListener tapListener;
    private float userScale = 1f;

    public ZoomImageView(Context context) {
        this(context, null);
    }

    public ZoomImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.MATRIX);

        scaleDetector = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(@NonNull ScaleGestureDetector detector) {
                        float target = userScale * detector.getScaleFactor();
                        target = Math.max(1f, Math.min(MAX_SCALE, target));
                        float factor = target / userScale;
                        if (factor != 1f) {
                            userMatrix.postScale(factor, factor,
                                    detector.getFocusX(), detector.getFocusY());
                            userScale = target;
                            applyMatrix();
                        }
                        return true;
                    }
                });

        gestureDetector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(@NonNull MotionEvent e) {
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(@NonNull MotionEvent e) {
                        if (userScale > 1.05f) {
                            userMatrix.reset();
                            userScale = 1f;
                        } else {
                            float target = Math.min(MAX_SCALE, DOUBLE_TAP_SCALE);
                            float factor = target / userScale;
                            userMatrix.postScale(factor, factor, e.getX(), e.getY());
                            userScale = target;
                        }
                        applyMatrix();
                        return true;
                    }

                    @Override
                    public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2,
                                            float distanceX, float distanceY) {
                        if (userScale > 1.01f) {
                            userMatrix.postTranslate(-distanceX, -distanceY);
                            applyMatrix();
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                        if (tapListener != null) tapListener.onTap();
                        return true;
                    }
                });
    }

    public void setOnTapListener(@Nullable OnTapListener listener) {
        tapListener = listener;
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        post(this::resetToFit);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) resetToFit();
    }

    private void resetToFit() {
        Drawable drawable = getDrawable();
        if (drawable == null || getWidth() == 0 || getHeight() == 0) return;

        int imageWidth = drawable.getIntrinsicWidth();
        int imageHeight = drawable.getIntrinsicHeight();
        if (imageWidth <= 0 || imageHeight <= 0) return;

        float scale = Math.min(getWidth() / (float) imageWidth,
                getHeight() / (float) imageHeight);
        baseMatrix.reset();
        baseMatrix.postScale(scale, scale);
        baseMatrix.postTranslate((getWidth() - imageWidth * scale) / 2f,
                (getHeight() - imageHeight * scale) / 2f);
        userMatrix.reset();
        userScale = 1f;
        applyMatrix();
    }

    private void applyMatrix() {
        Drawable drawable = getDrawable();
        if (drawable == null) return;

        drawMatrix.set(baseMatrix);
        drawMatrix.postConcat(userMatrix);
        rect.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawMatrix.mapRect(rect);

        float dx = 0f;
        float dy = 0f;
        float width = getWidth();
        float height = getHeight();

        if (rect.width() <= width) {
            dx = (width - rect.width()) / 2f - rect.left;
        } else if (rect.left > 0) {
            dx = -rect.left;
        } else if (rect.right < width) {
            dx = width - rect.right;
        }

        if (rect.height() <= height) {
            dy = (height - rect.height()) / 2f - rect.top;
        } else if (rect.top > 0) {
            dy = -rect.top;
        } else if (rect.bottom < height) {
            dy = height - rect.bottom;
        }

        if (dx != 0f || dy != 0f) {
            userMatrix.postTranslate(dx, dy);
            drawMatrix.postTranslate(dx, dy);
        }
        setImageMatrix(drawMatrix);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getDrawable() == null) return false;

        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(userScale > 1.01f);
            }
        } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(false);
            }
        }

        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return true;
    }
}

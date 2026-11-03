package com.mokofile.app.util;

import android.content.Context;
import android.util.TypedValue;

import androidx.core.content.ContextCompat;

public final class Colors {

    private Colors() {}

    /** Достаёт цвет из атрибута текущей темы (учитывает Dynamic Colors). */
    public static int attr(Context context, int attr) {
        TypedValue value = new TypedValue();
        if (context.getTheme().resolveAttribute(attr, value, true)) {
            if (value.resourceId != 0) {
                return ContextCompat.getColor(context, value.resourceId);
            }
            return value.data;
        }
        return 0xFF000000;
    }

    public static int dp(Context context, float dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}

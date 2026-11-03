package com.mokofile.app.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class Formats {

    private Formats() {}

    public static String size(long bytes) {
        if (bytes <= 0) return "0 Б";
        String[] units = {"Б", "КБ", "МБ", "ГБ", "ТБ"};
        double value = bytes;
        int i = 0;
        while (value >= 1024 && i < units.length - 1) {
            value /= 1024;
            i++;
        }
        String formatted = (value < 10 && i > 0)
                ? String.format(Locale.getDefault(), "%.1f", value)
                : String.format(Locale.getDefault(), "%.0f", value);
        return formatted.replace('.', ',') + " " + units[i];
    }

    public static String sizeGb(long bytes) {
        double gb = bytes / 1024d / 1024d / 1024d;
        return String.format(Locale.getDefault(), "%.0f ГБ", gb);
    }

    public static String date(long millis) {
        if (millis <= 0) return "—";
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(millis);

        long dayNow = dayStart(now);
        long dayThen = dayStart(then);
        long diffDays = Math.round((dayNow - dayThen) / 86_400_000d);

        if (diffDays <= 0) return "сегодня";
        if (diffDays == 1) return "вчера";
        if (diffDays < 7) return diffDays + " дн. назад";

        return new SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(new Date(millis));
    }

    private static long dayStart(Calendar c) {
        Calendar copy = (Calendar) c.clone();
        copy.set(Calendar.HOUR_OF_DAY, 0);
        copy.set(Calendar.MINUTE, 0);
        copy.set(Calendar.SECOND, 0);
        copy.set(Calendar.MILLISECOND, 0);
        return copy.getTimeInMillis();
    }

    public static String plural(int n, String one, String few, String many) {
        int mod10 = n % 10;
        int mod100 = n % 100;
        if (mod10 == 1 && mod100 != 11) return one;
        if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return few;
        return many;
    }

    public static String typeLabel(String type) {
        switch (type) {
            case "dir": return "Папка";
            case "image": return "Изображение";
            case "video": return "Видео";
            case "audio": return "Аудио";
            case "apk": return "Пакет приложения";
            case "archive": return "Архив";
            case "document": return "Документ";
            default: return "Файл";
        }
    }
}

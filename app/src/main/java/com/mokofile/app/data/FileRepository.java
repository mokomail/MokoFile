package com.mokofile.app.data;

import android.os.StatFs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/** Операции с файлами. Все методы блокирующие — вызывать в фоновом потоке. */
public final class FileRepository {

    private FileRepository() {}

    public static List<FileItem> list(File dir) {
        List<FileItem> out = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.exists()) out.add(new FileItem(f));
            }
        }
        return out;
    }

    public static boolean delete(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) delete(child);
            }
        }
        return file.delete();
    }

    public static void copy(File src, File destDir) throws IOException {
        File target = unique(destDir, src.getName());
        if (src.isDirectory()) {
            if (!target.mkdirs() && !target.isDirectory()) {
                throw new IOException("Не удалось создать папку " + target.getName());
            }
            File[] children = src.listFiles();
            if (children != null) {
                for (File child : children) copy(child, target);
            }
        } else {
            try (InputStream in = new FileInputStream(src);
                 OutputStream out = new FileOutputStream(target)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
        }
    }

    public static void move(File src, File destDir) throws IOException {
        if (src.getParentFile() != null && src.getParentFile().equals(destDir)) {
            return;
        }
        File target = new File(destDir, src.getName());
        if (target.exists()) {
            target = unique(destDir, src.getName());
        }
        if (src.renameTo(target)) {
            return;
        }
        copy(src, destDir);
        delete(src);
    }

    public static File createDir(File parent, String name) throws IOException {
        File dir = new File(parent, name.trim());
        if (dir.exists()) throw new IOException("Такое имя уже занято");
        if (!dir.mkdir()) throw new IOException("Не удалось создать папку");
        return dir;
    }

    public static File rename(File source, String newName) throws IOException {
        String clean = newName.trim();
        if (clean.isEmpty()) throw new IOException("Имя не может быть пустым");
        File target = new File(source.getParentFile(), clean);
        if (target.exists() && !target.equals(source)) throw new IOException("Такое имя уже занято");
        if (!source.renameTo(target)) throw new IOException("Не удалось переименовать");
        return target;
    }

    public static File unique(File dir, String name) {
        File candidate = new File(dir, name);
        if (!candidate.exists()) return candidate;

        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";

        int i = 2;
        while (true) {
            candidate = new File(dir, base + " (" + i + ")" + ext);
            if (!candidate.exists()) return candidate;
            i++;
        }
    }

    public static String[] storageStats(File rootDir) {
        try {
            StatFs stat = new StatFs(rootDir.getAbsolutePath());
            long total = stat.getTotalBytes();
            long free = stat.getAvailableBytes();
            return new String[]{String.valueOf(total - free), String.valueOf(total)};
        } catch (Exception e) {
            return new String[]{"0", "0"};
        }
    }
}

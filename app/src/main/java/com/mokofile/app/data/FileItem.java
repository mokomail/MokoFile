package com.mokofile.app.data;

import java.io.File;
import java.util.Locale;

/** Один элемент файловой системы. */
public class FileItem {

    public final File file;
    public final String name;
    public final boolean directory;
    public final long size;
    public final long modified;
    public final String type;

    public FileItem(File file) {
        this.file = file;
        this.name = file.getName();
        this.directory = file.isDirectory();
        this.size = directory ? 0 : file.length();
        this.modified = file.lastModified();
        this.type = typeOf(file);
    }

    public String extension() {
        String n = name;
        int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    public static String typeOf(File file) {
        if (file.isDirectory()) return "dir";
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        String ext = dot > 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";

        switch (ext) {
            case "jpg": case "jpeg": case "png": case "webp": case "gif":
            case "bmp": case "heic": case "avif":
                return "image";
            case "mp4": case "mkv": case "mov": case "webm": case "avi": case "3gp":
                return "video";
            case "mp3": case "m4a": case "flac": case "ogg": case "wav": case "opus": case "aac":
                return "audio";
            case "apk": case "apks": case "xapk":
                return "apk";
            case "zip": case "rar": case "7z": case "tar": case "gz":
                return "archive";
            case "pdf": case "doc": case "docx": case "xls": case "xlsx":
            case "ppt": case "pptx": case "txt": case "md": case "json":
            case "xml": case "csv":
                return "document";
            default:
                return "file";
        }
    }
}

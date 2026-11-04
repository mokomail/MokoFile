package com.mokofile.app.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.mokofile.app.R;
import com.mokofile.app.data.FileItem;
import com.mokofile.app.util.Colors;
import com.mokofile.app.util.Formats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.VH> {

    public static final int MODE_LIST = 0;
    public static final int MODE_GRID = 1;

    public interface Listener {
        void onItemClick(FileItem item);

        void onItemLongClick(FileItem item);
    }

    private final Listener listener;
    private List<FileItem> items = new ArrayList<>();
    private final Set<String> selected = new HashSet<>();
    private int mode = MODE_LIST;

    public FileAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<FileItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void setMode(int mode) {
        if (this.mode == mode) return;
        this.mode = mode;
        notifyDataSetChanged();
    }

    public int getMode() {
        return mode;
    }

    public void setSelected(Set<String> paths) {
        selected.clear();
        selected.addAll(paths);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return mode;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == MODE_GRID ? R.layout.item_file_grid : R.layout.item_file_list;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        FileItem item = items.get(position);
        Context context = holder.itemView.getContext();
        boolean isSelected = selected.contains(item.file.getAbsolutePath());

        holder.name.setText(item.name);
        holder.icon.setImageResource(iconFor(item.type));

        int bg = MaterialColors.getColor(holder.iconWrap, R.attr.colorPrimaryContainer);
        int fg = MaterialColors.getColor(holder.iconWrap, R.attr.colorOnPrimaryContainer);
        switch (item.type) {
            case "dir":
                bg = MaterialColors.getColor(holder.iconWrap, R.attr.colorPrimaryContainer);
                fg = MaterialColors.getColor(holder.iconWrap, R.attr.colorOnPrimaryContainer);
                break;
            case "image":
                bg = MaterialColors.getColor(holder.iconWrap, R.attr.colorTertiaryContainer);
                fg = MaterialColors.getColor(holder.iconWrap, R.attr.colorOnTertiaryContainer);
                break;
            case "video":
                bg = MaterialColors.getColor(holder.iconWrap, R.attr.colorPrimaryContainer);
                fg = MaterialColors.getColor(holder.iconWrap, R.attr.colorOnPrimaryContainer);
                break;
            case "audio":
            case "apk":
                bg = MaterialColors.getColor(holder.iconWrap, R.attr.colorSecondaryContainer);
                fg = MaterialColors.getColor(holder.iconWrap, R.attr.colorOnSecondaryContainer);
                break;
            case "archive":
                bg = MaterialColors.getColor(holder.iconWrap, R.attr.colorTertiaryContainer);
                fg = MaterialColors.getColor(holder.iconWrap, R.attr.colorOnTertiaryContainer);
                break;
            default:
                bg = MaterialColors.getColor(holder.iconWrap, R.attr.colorSurfaceVariant);
                fg = MaterialColors.getColor(holder.iconWrap, R.attr.colorOnSurfaceVariant);
                break;
        }

        GradientDrawable badge = new GradientDrawable();
        badge.setCornerRadius(Colors.dp(context, item.type.equals("dir") ? 14 : 15));
        badge.setColor(bg);
        holder.iconWrap.setBackground(badge);
        holder.icon.setColorFilter(fg);

        if (item.directory) {
            holder.subtitle.setText(Formats.typeLabel(item.type) + " · " + Formats.date(item.modified));
            if (holder.meta != null) holder.meta.setText("—");
        } else {
            holder.subtitle.setText(Formats.typeLabel(item.type) + " · " + Formats.size(item.size));
            if (holder.meta != null) holder.meta.setText(Formats.date(item.modified));
        }

        int nameColor = isSelected
                ? MaterialColors.getColor(holder.itemView, R.attr.colorOnPrimaryContainer)
                : MaterialColors.getColor(holder.itemView, R.attr.colorOnSurface);
        int subColor = isSelected
                ? withAlpha(MaterialColors.getColor(holder.itemView, R.attr.colorOnPrimaryContainer), 0.7f)
                : MaterialColors.getColor(holder.itemView, R.attr.colorOnSurfaceVariant);

        holder.name.setTextColor(nameColor);
        holder.subtitle.setTextColor(subColor);
        if (holder.meta != null) holder.meta.setTextColor(subColor);

        GradientDrawable row = new GradientDrawable();
        row.setCornerRadius(Colors.dp(context, mode == MODE_GRID ? 20 : 16));
        row.setColor(isSelected
                ? MaterialColors.getColor(holder.itemView, R.attr.colorPrimaryContainer)
                : ContextCompat.getColor(context, R.color.transparent));
        holder.root.setBackground(row);

        holder.selCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        if (isSelected) {
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(MaterialColors.getColor(holder.itemView, R.attr.colorPrimary));
            holder.selCheck.setBackground(circle);
            holder.selCheck.setColorFilter(MaterialColors.getColor(holder.itemView, R.attr.colorOnPrimary));
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onItemLongClick(item);
            return true;
        });
    }

    private static int withAlpha(int color, float factor) {
        int alpha = Math.round(255 * factor);
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static int iconFor(String type) {
        switch (type) {
            case "dir":
                return R.drawable.ic_folder;
            case "image":
                return R.drawable.ic_image;
            case "video":
                return R.drawable.ic_video;
            case "audio":
                return R.drawable.ic_music;
            case "apk":
                return R.drawable.ic_apk;
            case "archive":
                return R.drawable.ic_archive;
            case "document":
                return R.drawable.ic_document;
            default:
                return R.drawable.ic_file;
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        final View root;
        final FrameLayout iconWrap;
        final ImageView icon;
        final TextView name;
        final TextView subtitle;
        final TextView meta;
        final ImageView selCheck;

        VH(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.itemRoot);
            iconWrap = itemView.findViewById(R.id.iconWrap);
            icon = itemView.findViewById(R.id.fileIcon);
            name = itemView.findViewById(R.id.fileName);
            subtitle = itemView.findViewById(R.id.fileSubtitle);
            meta = itemView.findViewById(R.id.fileMeta);
            selCheck = itemView.findViewById(R.id.selCheck);
        }
    }
}

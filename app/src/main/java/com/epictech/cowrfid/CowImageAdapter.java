package com.epictech.cowrfid;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.api.services.drive.model.File;

import java.util.List;

public class CowImageAdapter extends RecyclerView.Adapter<CowImageAdapter.ViewHolder> {

    private final Context context;
    private final List<File> images;
    private final OnImageClickListener listener;

    public interface OnImageClickListener {
        void onImageClick(File file);
        void onImageLongClick(File file);
    }

    public CowImageAdapter(Context context, List<File> images, OnImageClickListener listener) {
        this.context = context;
        this.images = images;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cow_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = images.get(position);
        
        String link = file.getThumbnailLink();
        if (link == null) link = file.getWebContentLink();

        // High res thumbnail if available (replace s220 with s1000)
        // Drive thumbnail links often end with "=s220". Removing it or changing it gets higher res.
        // But for this small item, standard thumbnail is fine.
        
        Glide.with(context)
                .load(link)
                .placeholder(R.drawable.bg_badge_gray) // Placeholder/Error drawable
                .into(holder.imageView);

        holder.itemView.setOnClickListener(v -> listener.onImageClick(file));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onImageLongClick(file);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_cow_thumbnail);
        }
    }
}

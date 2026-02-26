package com.epictech.treeinfo;

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

public class CowImageSliderAdapter extends RecyclerView.Adapter<CowImageSliderAdapter.SliderViewHolder> {

    private final Context context;
    private final List<File> images;

    public CowImageSliderAdapter(Context context, List<File> images) {
        this.context = context;
        this.images = images;
    }

    @NonNull
    @Override
    public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cow_slider_image, parent, false);
        return new SliderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
        File file = images.get(position);
        
        String link = file.getWebContentLink();
        if (link == null) {
             link = file.getThumbnailLink();
             if (link != null && link.contains("=s")) {
                 link = link.substring(0, link.lastIndexOf("=")) + "=s1000";
             }
        }

        Glide.with(context)
                .load(link)
                .placeholder(R.drawable.bg_badge_gray)
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class SliderViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_slider_image);
        }
    }
}

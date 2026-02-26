package com.epictech.cowrfid;

import android.content.Context;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import java.util.List;

public class FullscreenImageAdapter extends RecyclerView.Adapter<FullscreenImageAdapter.ViewHolder> {

    private final Context context;
    private final List<String> imageUrls;

    public FullscreenImageAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        PhotoView photoView = new PhotoView(context);
        photoView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return new ViewHolder(photoView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String highResUrl = imageUrls.get(position);
        String lowResUrl = highResUrl;
        
        if (highResUrl != null && highResUrl.contains("=s")) {
             String base = highResUrl.substring(0, highResUrl.lastIndexOf("="));
             // Ensure high res is set (redundant if input is already s2000 but safe)
             highResUrl = base + "=s2000"; 
             lowResUrl = base + "=s400";
        }

        Glide.with(context)
                .load(highResUrl)
                .thumbnail(Glide.with(context).load(lowResUrl))
                .into(holder.photoView);
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        PhotoView photoView;

        ViewHolder(PhotoView itemView) {
            super(itemView);
            photoView = itemView;
        }
    }
}

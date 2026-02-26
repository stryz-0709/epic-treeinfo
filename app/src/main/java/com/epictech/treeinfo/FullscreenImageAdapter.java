package com.epictech.treeinfo;

import android.content.Context;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
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

    private GlideUrl getAuthGlideUrl(String url) {
        if (url == null) return null;
        String token = EarthRangerAuth.getCachedToken();
        if (token == null) return new GlideUrl(url);

        String auth = "Bearer " + token;
        return new GlideUrl(url, new LazyHeaders.Builder()
                .addHeader("Authorization", auth)
                .build());
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String highResUrl = imageUrls.get(position);
        String lowResUrl = highResUrl;

        GlideUrl highResGlideUrl = getAuthGlideUrl(highResUrl);
        GlideUrl lowResGlideUrl = getAuthGlideUrl(lowResUrl);

        if (highResGlideUrl != null && lowResGlideUrl != null) {
            Glide.with(context)
                    .load(highResGlideUrl)
                    .thumbnail(Glide.with(context).load(lowResGlideUrl))
                    .into(holder.photoView);
        } else {
            Glide.with(context).clear(holder.photoView);
        }
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

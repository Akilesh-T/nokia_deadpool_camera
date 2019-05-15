package com.hmdglobal.app.camera.app;

import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.widget.ImageView;
import com.hmdglobal.app.camera.R;

public class ListItemViewHolder extends ViewHolder {
    public ImageView imageView;
    public View indicator;
    private int position;
    public View view;

    public interface ItemListener {
        void onItemClicked(int i);
    }

    public ListItemViewHolder(View view) {
        super(view);
        this.imageView = (ImageView) view.findViewById(R.id.item_image);
        this.indicator = view.findViewById(R.id.indicator_shape);
        this.view = view;
    }
}

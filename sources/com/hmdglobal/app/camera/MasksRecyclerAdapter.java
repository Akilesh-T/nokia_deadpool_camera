package com.hmdglobal.app.camera;

import android.content.Context;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.hmdglobal.app.camera.app.ListItemViewHolder;
import com.hmdglobal.app.camera.model.Model;
import com.hmdglobal.app.camera.util.FileUtil;
import java.util.ArrayList;
import java.util.List;

public class MasksRecyclerAdapter extends Adapter {
    private List<Model> mAllModels = new ArrayList();
    private Context mContext;
    private String mSelect = null;
    private onItemClick onItemClick;

    public interface onItemClick {
        void onClick(int i, String str);
    }

    public MasksRecyclerAdapter(Context context, List<Model> models) {
        this.mContext = context;
        this.mAllModels = models;
    }

    public void setCallback(onItemClick click) {
        this.onItemClick = click;
    }

    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ListItemViewHolder(LayoutInflater.from(this.mContext).inflate(R.layout.list_item, parent, false));
    }

    public void onBindViewHolder(ViewHolder holder, final int position) {
        ListItemViewHolder holder1 = (ListItemViewHolder) holder;
        final Model model = (Model) this.mAllModels.get(position);
        holder1.imageView.setImageResource(getIdFromR(model.sample));
        String name = new StringBuilder();
        name.append(FileUtil.mEffectParentPath);
        name.append(model.zipName);
        name = name.toString();
        ImageView imageView;
        StringBuilder stringBuilder;
        if (TextUtils.isEmpty(this.mSelect)) {
            imageView = holder1.imageView;
            stringBuilder = new StringBuilder();
            stringBuilder.append(model.sample);
            stringBuilder.append("_60");
            imageView.setImageResource(getIdFromR(stringBuilder.toString()));
            holder1.indicator.setVisibility(4);
        } else if (this.mSelect.equals(name)) {
            holder1.imageView.setImageResource(getIdFromR(model.sample));
            holder1.indicator.setVisibility(0);
        } else {
            imageView = holder1.imageView;
            stringBuilder = new StringBuilder();
            stringBuilder.append(model.sample);
            stringBuilder.append("_60");
            imageView.setImageResource(getIdFromR(stringBuilder.toString()));
            holder1.indicator.setVisibility(4);
        }
        holder1.view.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                if (MasksRecyclerAdapter.this.onItemClick != null) {
                    String temp = new StringBuilder();
                    temp.append(FileUtil.mEffectParentPath);
                    temp.append(model.zipName);
                    temp = temp.toString();
                    MasksRecyclerAdapter masksRecyclerAdapter;
                    StringBuilder stringBuilder;
                    if (TextUtils.isEmpty(MasksRecyclerAdapter.this.mSelect)) {
                        masksRecyclerAdapter = MasksRecyclerAdapter.this;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(FileUtil.mEffectParentPath);
                        stringBuilder.append(model.zipName);
                        masksRecyclerAdapter.mSelect = stringBuilder.toString();
                    } else if (MasksRecyclerAdapter.this.mSelect.equals(temp)) {
                        MasksRecyclerAdapter.this.mSelect = "";
                    } else {
                        masksRecyclerAdapter = MasksRecyclerAdapter.this;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(FileUtil.mEffectParentPath);
                        stringBuilder.append(model.zipName);
                        masksRecyclerAdapter.mSelect = stringBuilder.toString();
                    }
                    onItemClick access$000 = MasksRecyclerAdapter.this.onItemClick;
                    int i = position;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(FileUtil.mEffectParentPath);
                    stringBuilder2.append(model.zipName);
                    access$000.onClick(i, stringBuilder2.toString());
                }
                MasksRecyclerAdapter.this.notifyDataSetChanged();
            }
        });
    }

    public void setSelect(String select) {
        this.mSelect = select;
        notifyDataSetChanged();
    }

    private int getIdFromR(String strId) {
        return this.mContext.getResources().getIdentifier(strId, "drawable", this.mContext.getPackageName());
    }

    public int getItemCount() {
        return this.mAllModels.size();
    }
}

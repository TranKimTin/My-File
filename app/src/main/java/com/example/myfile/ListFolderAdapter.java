package com.example.myfile;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

public class ListFolderAdapter extends RecyclerView.Adapter<ListFolderAdapter.ViewHolder> {
    private ArrayList<Folder> listFolder;
    private Context mContext;
    private MyInterface mInterface;
    private boolean showSelect;

    public boolean isSelectedAll() {
        for (Folder f : listFolder)
            if (!f.isSelected())
                return false;
        return true;
    }

    public void setShowSelect(boolean showSelect) {
        this.showSelect = showSelect;
    }

    public boolean isShowSelect() {
        return showSelect;
    }

    public void setmInterface(MyInterface mInterface) {
        this.mInterface = mInterface;
    }

    public ListFolderAdapter(Context context, ArrayList<Folder> list) {
        listFolder = list;
        mContext = context;
        showSelect = false;
    }

    public void clearSelected() {
        for (Folder f : listFolder) {
            f.setSelected(false);
        }
    }

    void selectAll() {
        for (Folder f : listFolder) {
            f.setSelected(true);
        }
    }

    boolean empty() {
        for (Folder f : listFolder)
            if (f.isSelected())
                return false;
        return true;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.row_recycleview, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (showSelect) holder.cbxSelected.setVisibility(View.VISIBLE);
        else holder.cbxSelected.setVisibility(View.GONE);
        holder.cbxSelected.setChecked(listFolder.get(position).isSelected());
        Folder folder = listFolder.get(position);
        holder.tvName.setText(folder.getFolderName());
        if (folder.isFolder) holder.tvNumberChild.setText(folder.getNumberChild() + " mục");
        else {
            float capa = folder.getNumberChild();
            String unit = "b";
            if (capa >= 1024) {
                capa /= 1024;
                unit = "kb";
                if (capa >= 1024) {
                    capa /= 1024;
                    unit = "mb";
                    if (capa >= 1024) {
                        capa /= 1024;
                        unit = "gb";
                    }
                }
            }
            holder.tvNumberChild.setText(String.format("%.2f %s", capa, unit));
        }
        holder.tvCreatedDate.setText(new SimpleDateFormat("dd/MM/yyyy hh:mm").format(folder.getCreatedDate()));
        if (folder.isFolder) holder.imvFolder.setImageResource(R.drawable.folder);
        else if (folder.getFolderName().matches(".*mp4"))
            holder.imvFolder.setImageResource(R.drawable.mp4_file);
        else if (folder.getFolderName().matches(".*png"))
            holder.imvFolder.setImageResource(R.drawable.image_file);
        else if (folder.getFolderName().matches(".*jpg"))
            holder.imvFolder.setImageResource(R.drawable.image_file);
        else if (folder.getFolderName().matches(".*jpeg"))
            holder.imvFolder.setImageResource(R.drawable.image_file);
        else if (folder.getFolderName().matches(".*GIF"))
            holder.imvFolder.setImageResource(R.drawable.image_file);
        else holder.imvFolder.setImageResource(R.drawable.text_file);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mInterface.mOnclick(listFolder.get(position));
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mInterface.mOnLongClick(listFolder.get(position));
                return true;
            }
        });
        holder.cbxSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listFolder.get(position).setSelected(!listFolder.get(position).isSelected());
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return listFolder.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName, tvCreatedDate, tvNumberChild;
        private ImageView imvFolder;
        private CheckBox cbxSelected;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvFolderName);
            tvCreatedDate = itemView.findViewById(R.id.tvCreatedDate);
            tvNumberChild = itemView.findViewById(R.id.tvNumberChild);
            imvFolder = itemView.findViewById(R.id.imvFolder);
            cbxSelected = itemView.findViewById(R.id.cbxSelected);
        }

    }

    public interface MyInterface {
        void mOnclick(Folder folder);

        void mOnLongClick(Folder folder);
    }
}

package com.example.myfile;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import static com.example.myfile.MainActivity.TAG;

public class ListFolderAdapter extends RecyclerView.Adapter<ListFolderAdapter.ViewHolder> {
    private ArrayList<Folder> listFolder;
    private Context mContext;
    private MyInterface mInterface;
    private boolean showSelect;
    private ThreadPoolExecutor executor;
    private String search;

    public ListFolderAdapter(Context context, ArrayList<Folder> list, String s) {
        search = s;
        listFolder = list;
        mContext = context;
        showSelect = false;
        int corePoolSize = 50;
        int maximumPoolSize = 100;
        int queueCapacity = 1000;
        executor = new ThreadPoolExecutor(corePoolSize, // Số corePoolSize
                maximumPoolSize, // số maximumPoolSize
                500, // thời gian một thread được sống nếu không làm gì
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity)); // Blocking queue để cho request đợi

    }

    public void setSearch(String search) {
        this.search = search;
    }

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
        Folder folder = listFolder.get(position);
        if(search.length() > 0 && !folder.getFolderName().toLowerCase().contains(search.toLowerCase())){
            holder.itemView.setVisibility(View.GONE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
            return;
        }
        else{
            holder.itemView.setVisibility(View.VISIBLE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        if (showSelect) holder.cbxSelected.setVisibility(View.VISIBLE);
        else holder.cbxSelected.setVisibility(View.GONE);
        holder.cbxSelected.setChecked(listFolder.get(position).isSelected());

        holder.tvName.setText(folder.getFolderName());
        if (folder.isFolder) holder.tvNumberChild.setText(folder.getNumberChild() + " mục");
        else {
            float capa = folder.getNumberChild();
            String unit = "B";
            if (capa >= 1024) {
                capa /= 1024;
                unit = "KB";
                if (capa >= 1024) {
                    capa /= 1024;
                    unit = "MB";
                    if (capa >= 1024) {
                        capa /= 1024;
                        unit = "GB";
                    }
                }
            }
            holder.tvNumberChild.setText(String.format("%.2f %s", capa, unit));
        }
        holder.tvCreatedDate.setText(new SimpleDateFormat("dd/MM/yyyy hh:mm").format(folder.getCreatedDate()));
        if (folder.isFolder) holder.imvFolder.setImageResource(R.drawable.folder);
        else if (folder.getExt().equals("doc") || folder.getExt().equals("docx"))
            holder.imvFolder.setImageResource(R.drawable.word);
        else if (folder.getExt().equals("xls") || folder.getExt().equals("xlsx"))
            holder.imvFolder.setImageResource(R.drawable.excel);
        else if (folder.getExt().equals("ppt") || folder.getExt().equals("pptx"))
            holder.imvFolder.setImageResource(R.drawable.powerpoint);
        else if (folder.getExt().equals("pdf"))
            holder.imvFolder.setImageResource(R.drawable.pdf);
        else if (folder.getExt().equals("mp4"))
            holder.imvFolder.setImageResource(R.drawable.mp4_file);
        else if (folder.getExt().equals("zip") || folder.getExt().equals("7z") || folder.getExt().equals("rar") || folder.getExt().equals("iso") || folder.getExt().equals("apk"))
            holder.imvFolder.setImageResource(R.drawable.zip);
        else if (folder.getExt().equals("gif"))
            holder.imvFolder.setImageResource(R.drawable.gif);
        else if (folder.getExt().equals("png") || folder.getExt().equals("jpg") || folder.getExt().equals("jpeg")) {
            holder.imvFolder.setImageResource(R.drawable.image_file);
        } else holder.imvFolder.setImageResource(R.drawable.text_file);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mInterface.mOnclick(folder);
                if (showSelect) notifyItemChanged(position);
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mInterface.mOnLongClick(folder);
                return true;
            }
        });
        holder.cbxSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                folder.setSelected(!folder.isSelected());
                notifyItemChanged(position);
            }
        });

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (!folder.isFolder() && (folder.getExt().equals("png") || folder.getExt().equals("jpg") || folder.getExt().equals("jpeg"))) {
                String name = holder.tvName.getText().toString();
                Runnable t = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (!name.equals(holder.tvName.getText().toString())) return;
                            Uri uri = FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".provider", folder.getThis());
                            if (!name.equals(holder.tvName.getText().toString())) return;
                            Bitmap thumbnail = mContext.getContentResolver().loadThumbnail(uri, new Size(80, 70), null);
                            if (!name.equals(holder.tvName.getText().toString())) return;
                            ((Activity) mContext).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    holder.imvFolder.setImageBitmap(thumbnail);
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                executor.execute(t);
            }
        }
    }

    @Override
    public int getItemCount() {
        return listFolder.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvName, tvCreatedDate, tvNumberChild;
        public ImageView imvFolder;
        public CheckBox cbxSelected;

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

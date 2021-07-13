package com.example.myfile;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private List<Folder> listFolder;
    private RecyclerView rcvListFolder;
    private ListFolderAdapter listFolderAdapter;
    private String TAG = "MY FILE";
    private File currentFile;
    private TextView tvBreadcrumb;
    private int countBackPress;
    private CheckBox cbxAll;
    private RelativeLayout sidebar, sidebarSave;
    private Button btnCopy, btnDelete, btnMove, btnSave, btnCancel;
    private List<File> listFileSelected;
    private int status;
    private final int FREE = 0, COPY = 1, MOVE = 2, DELETE = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        countBackPress = 0;
        status = FREE;
        listFolder = new ArrayList<Folder>();
        currentFile = new File(Environment.getExternalStorageDirectory().toString());
        listFolderAdapter = new ListFolderAdapter(this, (ArrayList<Folder>) listFolder);
        listFileSelected = new ArrayList<File>();

        rcvListFolder = findViewById(R.id.rcvListFolder);
        tvBreadcrumb = findViewById(R.id.breadcrumb);
        cbxAll = findViewById(R.id.cbxAll);
        sidebar = findViewById(R.id.sidebar);
        sidebarSave = findViewById(R.id.sidebar_save);
        btnMove = findViewById(R.id.btnMove);
        btnCopy = findViewById(R.id.btnCopy);
        btnDelete = findViewById(R.id.btnDelete);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        cbxAll.setOnClickListener(this);
        btnMove.setOnClickListener(this);
        btnCopy.setOnClickListener(this);
        btnDelete.setOnClickListener(this);
        btnSave.setOnClickListener(this);
        btnCancel.setOnClickListener(this);


        listFolderAdapter.setmInterface(new ListFolderAdapter.MyInterface() {
            @Override
            public void mOnclick(Folder folder) {
                if (listFolderAdapter.isShowSelect()) {
                    folder.setSelected(!folder.isSelected());
                    cbxAll.setChecked(listFolderAdapter.isSelectedAll());
                    listFolderAdapter.notifyDataSetChanged();
                } else {
                    countBackPress = 0;
                    if (folder.isFolder) {
                        currentFile = folder.getThis();
                        createList();
                    } else {
                        open_file(folder.getThis());
                    }
                }

            }

            @Override
            public void mOnLongClick(Folder folder) {
                folder.setSelected(!folder.isSelected());
                listFolderAdapter.setShowSelect(true);
                listFolderAdapter.notifyDataSetChanged();
                cbxAll.setVisibility(View.VISIBLE);
                sidebar.setVisibility(View.VISIBLE);
                sidebarSave.setVisibility(View.GONE);
            }
        });
        rcvListFolder.setAdapter(listFolderAdapter);
        rcvListFolder.setLayoutManager(new LinearLayoutManager(this));
        createList();
    }

    public void open_file(File file) {

        // Get URI and MIME type of file
        Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file);
        String mime = getContentResolver().getType(uri);

        // Open file with user selected app
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mime);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    void createList() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.MANAGE_EXTERNAL_STORAGE}, 1);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);

        tvBreadcrumb.setText(currentFile.getAbsolutePath().toString().replace("/storage/emulated/0", "/Home"));
        File f = currentFile;
        File[] files = f.listFiles();
        listFolder.clear();
        if (files != null) {
            for (File inFile : files) {
                if (inFile.isHidden()) continue;
                Folder folder = new Folder();
                folder.setPath(inFile.getAbsolutePath());
                folder.setFolderName(inFile.getName());
                folder.setCreatedDate(new Date(inFile.lastModified()));
                folder.isFolder = inFile.isDirectory();
                folder.setThis(inFile);
                if (folder.isFolder)
                    folder.setNumberChild(inFile.listFiles() == null ? 0 : inFile.listFiles().length);
                else {
                    folder.setNumberChild(inFile.length());
                }
                listFolder.add(folder);
            }
        }

        Collections.sort(listFolder, (a, b) -> a.getFolderName().compareTo(b.getFolderName()));
        listFolderAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        if (listFolderAdapter.isShowSelect()) {
            listFolderAdapter.clearSelected();
            listFolderAdapter.setShowSelect(false);
            listFolderAdapter.notifyDataSetChanged();
            cbxAll.setVisibility(View.GONE);
            sidebar.setVisibility(View.GONE);
        } else {
            if (!currentFile.getAbsolutePath().equals(Environment.getExternalStorageDirectory().toString())) {
                currentFile = currentFile.getParentFile();
                createList();
            } else {
                countBackPress++;
                if (countBackPress == 1) {
                    Toast.makeText(this, "Chạm lần nữa để thoát", Toast.LENGTH_SHORT).show();
                } else {
                    finish();
                }
            }
        }

    }

    void log(String s) {
        Log.d("MY FILE", s);
    }

    boolean handleSidebar() {
        if (!listFolderAdapter.isShowSelect()) return false;
        if (listFolderAdapter.empty()) {
            Toast.makeText(this, "Chưa chọn file nào", Toast.LENGTH_SHORT).show();
            return false;
        }
        listFileSelected.clear();
        for (Folder f : listFolder) {
            if (f.isSelected()) {
                listFileSelected.add(f.getThis());
            }
        }
        listFolderAdapter.clearSelected();
        listFolderAdapter.setShowSelect(false);
        listFolderAdapter.notifyDataSetChanged();
        sidebar.setVisibility(View.GONE);
        sidebarSave.setVisibility(View.VISIBLE);
        cbxAll.setVisibility(View.GONE);
        return true;
    }

    void deleteFile(File file, ThreadPoolExecutor executor) {
        if (file.isDirectory()) {
            File files[] = file.listFiles();
            if (files == null) file.delete();
            else {
                for (File f : files) {
                    deleteFile(f, executor);
                }
            }
        } else {
            executor.execute(new MyThread(file, null, DELETE));
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cbxAll:
                if (listFolderAdapter.isSelectedAll()) {
                    cbxAll.setChecked(false);
                    listFolderAdapter.clearSelected();
                } else {
                    cbxAll.setChecked(true);
                    listFolderAdapter.selectAll();
                }
                listFolderAdapter.notifyDataSetChanged();
                break;
            case R.id.btnCopy:
                if (!handleSidebar()) return;
                status = COPY;
                btnSave.setText("Paste");
                break;
            case R.id.btnMove:
                if (!handleSidebar()) return;
                status = MOVE;
                btnSave.setText("Paste");
                break;
            case R.id.btnDelete:
                if (!handleSidebar()) return;
                status = DELETE;
                btnSave.setText("OK");
                break;
            case R.id.btnCancel:
                listFileSelected.clear();
                sidebarSave.setVisibility(View.GONE);
                sidebar.setVisibility(View.GONE);
                cbxAll.setVisibility(View.GONE);
                status = FREE;
                break;
            case R.id.btnSave:
                int corePoolSize = 100;
                int maximumPoolSize = 500;
                int queueCapacity = listFileSelected.size();
                ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, // Số corePoolSize
                        maximumPoolSize, // số maximumPoolSize
                        60, // thời gian một thread được sống nếu không làm gì
                        TimeUnit.SECONDS,
                        new ArrayBlockingQueue<>(queueCapacity)); // Blocking queue để cho request đợi
                if (status == COPY) {
                    for (File f : listFileSelected) {
                        File dest = new File(currentFile.getAbsolutePath() + "/" + f.getName());
                        executor.execute(new MyThread(f, dest, COPY));
                    }
                    executor.shutdown();
                    Toast.makeText(this, "Copy thành công", Toast.LENGTH_SHORT).show();
                } else if (status == DELETE) {
                    for (File f : listFileSelected) {
                        deleteFile(f, executor);
                    }
                }
                executor.shutdown();
                while (true) {
                    try {
                        if (!!executor.awaitTermination(50, TimeUnit.MILLISECONDS)) break;
                    } catch (InterruptedException e) {
                        e.printStackTrace();

                    }
                    Toast.makeText(this, "Delete thành công", Toast.LENGTH_SHORT).show();
                }

                sidebarSave.setVisibility(View.GONE);
                status = FREE;
                createList();
        }
    }
}
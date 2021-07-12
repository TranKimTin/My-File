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
import android.provider.MediaStore;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private List<Folder> listFolder;
    private RecyclerView rcvListFolder;
    private ListFolderAdapter listFolderAdapter;
    private String TAG = "MY FILE";
    private File currentFile;
    private TextView tvBreadcrumb;
    private int countBackPress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        countBackPress = 0;
        rcvListFolder = findViewById(R.id.rcvListFolder);
        tvBreadcrumb = findViewById(R.id.breadcrumb);
        listFolder = new ArrayList<Folder>();
        currentFile = new File(Environment.getExternalStorageDirectory().toString());
        listFolderAdapter = new ListFolderAdapter(this, (ArrayList<Folder>) listFolder);
        listFolderAdapter.setmInterface(new ListFolderAdapter.MyInterface() {
            @Override
            public void mOnclick(Folder folder) {
                countBackPress = 0;
                if (folder.isFolder) {
                    currentFile = folder.getThis();
                    createList();
                } else {
                    open_file(folder.getThis());
                }
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
        if (!currentFile.getAbsolutePath().equals(Environment.getExternalStorageDirectory().toString())) {
            currentFile = currentFile.getParentFile();
            createList();
        }
        else{
            countBackPress++;
            if(countBackPress == 1){
                Toast.makeText(this, "Chạm lần nữa để thoát", Toast.LENGTH_SHORT).show();
            }
            else{
                finish();
            }
        }
    }

    void log(String s) {
        Log.d("MY FILE", s);
    }
}
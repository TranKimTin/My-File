package com.example.myfile;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rcvListFolder = findViewById(R.id.rcvListFolder);
        listFolder = new ArrayList<Folder>();
        createList();
        listFolderAdapter = new ListFolderAdapter(this, (ArrayList<Folder>) listFolder);
        rcvListFolder.setAdapter(listFolderAdapter);
        rcvListFolder.setLayoutManager(new LinearLayoutManager(this));
    }

    void createList() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.MANAGE_EXTERNAL_STORAGE}, 1);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);

        String path = Environment.getExternalStorageDirectory().toString();
        if (getIntent().getStringExtra("path") != null) {
            path = getIntent().getStringExtra("path");
        }
        File f = new File(path);
        File[] files = f.listFiles();
        for (File inFile : files) {
            Folder folder = new Folder();
            folder.setPath(inFile.getAbsolutePath());
            folder.setFolderName(inFile.getName());
            folder.setCreatedDate(new Date(inFile.lastModified()));
            folder.isFolder = inFile.isDirectory();
            if (folder.isFolder) folder.setNumberChild(inFile.listFiles().length);
            else {
                folder.setNumberChild(inFile.length());
                folder.setThis(inFile);
            }
            listFolder.add(folder);
        }
        Collections.sort(listFolder, (a, b) -> a.getFolderName().compareTo(b.getFolderName()));
    }

    void log(String s) {
        Log.d("MY FILE", s);
    }
}
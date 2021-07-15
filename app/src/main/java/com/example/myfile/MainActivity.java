package com.example.myfile;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.util.Size;
import android.view.Menu;
import android.view.MenuItem;
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
import java.util.stream.Collectors;

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
    private ProgressDialog progress;
    private int fieldSort, option;
    private final int NAME = 0, DATE = 1, TYPE = 2, ASC = 1, DESC = 2;
    private MenuItem itemName, itemDate, itemType, itemASC, itemDESC;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog

        countBackPress = 0;
        status = FREE;
        fieldSort = DATE;
        option = DESC;
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
                } else {
                    countBackPress = 0;
                    if (folder.isFolder) {
                        currentFile = folder.getThis();
                        listFolder.clear();
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
                cbxAll.setChecked(listFolderAdapter.isSelectedAll());
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

    void showLoader() {
        try {
            if (!progress.isShowing()) progress.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void dismisLoader() {
        try {
            if (progress.isShowing()) progress.dismiss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sortList() {
        if (fieldSort == NAME && option == ASC)
            Collections.sort(listFolder, (a, b) -> a.getFolderName().compareTo(b.getFolderName()));
        if (fieldSort == NAME && option == DESC)
            Collections.sort(listFolder, (a, b) -> b.getFolderName().compareTo(a.getFolderName()));

        if (fieldSort == DATE && option == ASC)
            Collections.sort(listFolder, (a, b) -> a.getCreatedDate().compareTo(b.getCreatedDate()));
        if (fieldSort == DATE && option == DESC)
            Collections.sort(listFolder, (a, b) -> b.getCreatedDate().compareTo(a.getCreatedDate()));

        if (fieldSort == TYPE && option == ASC)
            Collections.sort(listFolder, (a, b) -> a.getExt().compareTo(b.getExt()) != 0 ? a.getExt().compareTo(b.getExt()) : a.getFolderName().compareTo(b.getFolderName()));
        if (fieldSort == TYPE && option == DESC)
            Collections.sort(listFolder, (a, b) -> b.getExt().compareTo(a.getExt()) != 0 ? b.getExt().compareTo(a.getExt()) : b.getFolderName().compareTo(a.getFolderName()));
    }

    void createList() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.MANAGE_EXTERNAL_STORAGE}, 1);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        int MyVersion = Build.VERSION.SDK_INT;
        if (MyVersion > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                }
            }
        }
        showLoader();
        new Thread(new Runnable() {
            @Override
            public void run() {
                tvBreadcrumb.setText(currentFile.getAbsolutePath().toString().replace("/storage/emulated/0", "/Home"));
                File f = currentFile;
                File[] files = f.listFiles();
                listFolder.clear();
                if (files != null) {
                    for (File inFile : files) {
                        if (inFile.isHidden()) continue;
                        String path = inFile.getAbsolutePath();
                        Folder folder = new Folder();
                        folder.setPath(path);
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

                sortList();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listFolderAdapter.notifyDataSetChanged();
                    }
                });
                dismisLoader();
            }
        }).start();

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
                listFolder.clear();
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
        Log.d(TAG, s);
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

    void deleteDir(File file) {
        if (file.isDirectory()) {
            File files[] = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteDir(f);
                }
            }
        }
        file.delete();
    }

    void copy(File src, File dest, ThreadPoolExecutor executor) {
        if (src.isDirectory()) {
            if (!dest.exists()) dest.mkdir();
            File[] files = src.listFiles();
            if (files == null) return;
            for (File file : files) {
                File d = new File(dest.getAbsolutePath() + "/" + file.getName());
                copy(file, d, executor);
            }
        } else {
            executor.execute(new MyThread(src, dest, COPY));
        }
    }

    void await(ThreadPoolExecutor executor) {
        try {
            while (!executor.awaitTermination(50, TimeUnit.MILLISECONDS)) ;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void toast(String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
            }
        });
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
                showLoader();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int corePoolSize = 50;
                        int maximumPoolSize = 100;
                        int queueCapacity = 10000;
                        ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, // Số corePoolSize
                                maximumPoolSize, // số maximumPoolSize
                                60, // thời gian một thread được sống nếu không làm gì
                                TimeUnit.SECONDS,
                                new ArrayBlockingQueue<>(queueCapacity)); // Blocking queue để cho request đợi
                        if (status == COPY) {
                            if (currentFile.getAbsolutePath().contains(listFileSelected.get(0).getAbsolutePath())) {
                                toast("Không được cop ty đến thư mục con");
                                dismisLoader();
                                return;
                            }
                            for (File f : listFileSelected) {
                                File dest = new File(currentFile.getAbsolutePath() + "/" + f.getName());
                                copy(f, dest, executor);
                            }
                            executor.shutdown();
                            await(executor);
                            toast("Copy thành công");
                        } else if (status == DELETE) {
                            for (File f : listFileSelected) {
                                deleteFile(f, executor);
                            }
                            executor.shutdown();
                            await(executor);
                            for (File f : listFileSelected) {
                                deleteDir(f);
                            }
                            toast("Delete thành công");
                        } else if (status == MOVE) {
                            //copy sau do delete
                            //copy
                            if (currentFile.getAbsolutePath().contains(listFileSelected.get(0).getAbsolutePath())) {
                                toast("Không được di chuyển đến thư mục con");
                                dismisLoader();
                                return;
                            }

                            for (File f : listFileSelected) {
                                File dest = new File(currentFile.getAbsolutePath() + "/" + f.getName());
                                copy(f, dest, executor);
                            }
                            executor.shutdown();
                            await(executor);

                            //delete
                            executor = new ThreadPoolExecutor(corePoolSize, // Số corePoolSize
                                    maximumPoolSize, // số maximumPoolSize
                                    60, // thời gian một thread được sống nếu không làm gì
                                    TimeUnit.SECONDS,
                                    new ArrayBlockingQueue<>(queueCapacity)); // Blocking queue để cho request đợi
                            for (File f : listFileSelected) {
                                deleteFile(f, executor);
                            }
                            executor.shutdown();
                            await(executor);
                            for (File f : listFileSelected) {
                                deleteDir(f);
                            }
                            toast("Move thành công");
                        }
                        listFileSelected.clear();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                sidebarSave.setVisibility(View.GONE);
                            }
                        });

                        status = FREE;
                        dismisLoader();
                        createList();
                    }
                }).start();
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_sort, menu);
        itemName = menu.getItem(0);
        itemDate = menu.getItem(1);
        itemType = menu.getItem(2);
        itemASC = menu.getItem(3);
        itemDESC = menu.getItem(4);

        itemName.setChecked(fieldSort == NAME);
        itemDate.setChecked(fieldSort == DATE);
        itemType.setChecked(fieldSort == TYPE);
        itemASC.setChecked(option == ASC);
        itemDESC.setChecked(option == DESC);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_name:
                if (fieldSort != NAME) {
                    fieldSort = NAME;
                    sortList();
                    listFolderAdapter.notifyDataSetChanged();
                }
                break;
            case R.id.item_date:
                if (fieldSort != DATE) {
                    fieldSort = DATE;
                    sortList();
                    listFolderAdapter.notifyDataSetChanged();
                }
                break;
            case R.id.item_type:
                if (fieldSort != TYPE) {
                    fieldSort = TYPE;
                    sortList();
                    listFolderAdapter.notifyDataSetChanged();
                }
                break;
            case R.id.item_asc:
                if (option != ASC) {
                    option = ASC;
                    sortList();
                    listFolderAdapter.notifyDataSetChanged();
                }
                break;
            case R.id.item_desc:
                if (option != DESC) {
                    option = DESC;
                    sortList();
                    listFolderAdapter.notifyDataSetChanged();
                }
                break;
            default:
                break;
        }
        itemName.setChecked(fieldSort == NAME);
        itemDate.setChecked(fieldSort == DATE);
        itemType.setChecked(fieldSort == TYPE);
        itemASC.setChecked(option == ASC);
        itemDESC.setChecked(option == DESC);
        return true;
    }
}
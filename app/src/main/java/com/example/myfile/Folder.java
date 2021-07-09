package com.example.myfile;

import android.net.Uri;

import java.io.File;
import java.util.Date;

public class Folder {
    private String folderName;
    private Date createdDate;
    private long numberChild;
    private String path;
    public boolean isFolder;
    private Uri uri;
    private File This;

    Folder(){
        uri = null;
    }
    public File getThis() {
        return This;
    }

    public void setThis(File aThis) {
        This = aThis;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getNumberChild() {
        return numberChild;
    }

    public void setNumberChild(long numberChild) {
        this.numberChild = numberChild;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public void setFolder(boolean folder) {
        isFolder = folder;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
}

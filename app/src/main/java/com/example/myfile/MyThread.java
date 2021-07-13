package com.example.myfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MyThread implements Runnable {
    private File src, dest;
    private int status;
    private final int COPY = 1, MOVE = 2, DELETE = 3;

    MyThread(File src, File dest, int status) {
        this.src = src;
        this.dest = dest;
        this.status = status;
    }

    @Override
    public void run() {
        try {
            if(status == COPY){
                if (src.isDirectory()) {
                    copyDir(src, dest);
                } else {
                    copyFile(src, dest);
                }
            }
            else if(status == DELETE){
                src.delete();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void copyFile(File src, File dest) throws IOException {
        int i = 1;
        String path = dest.getAbsolutePath();
        int indexDot = path.indexOf('.');
        while (dest.exists()) {
            dest = new File(path.substring(0, indexDot) + "(" + i++ + ")" + path.substring(indexDot));
        }
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dest);
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    void copyDir(File src, File dest) {
        if (!dest.exists()) dest.mkdir();
        File[] files = src.listFiles();
        if (files == null) return;
        int corePoolSize = 100;
        int maximumPoolSize = 500;
        int queueCapacity = files.length;
        ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, // Số corePoolSize
                maximumPoolSize, // số maximumPoolSize
                60, // thời gian một thread được sống nếu không làm gì
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity)); // Blocking queue để cho request đợi
        for (File file : files) {
            File d = new File(dest.getAbsolutePath() + "/" + file.getName());
            executor.execute(new MyThread(file, d, status));
        }
        executor.shutdown();
    }
}

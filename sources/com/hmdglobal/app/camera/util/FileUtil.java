package com.hmdglobal.app.camera.util;

import android.content.Context;
import android.content.res.AssetManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hmdglobal.app.camera.app.CameraApp;
import com.hmdglobal.app.camera.model.Model;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {
    public static List<Model> mAllModels = new ArrayList();
    public static String mEffectParentPath = "";

    static class WorkRunnable implements Runnable {
        private String mDestPath;
        private String mPathName;

        public WorkRunnable(String name, String dest) {
            this.mPathName = name;
            this.mDestPath = dest;
        }

        public void run() {
            AssetManager manager = CameraApp.getContext().getAssets();
            FileOutputStream os = null;
            byte[] bs = new byte[1024];
            int len = 0;
            try {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("masks/");
                stringBuilder.append(this.mPathName);
                InputStream inputStream = manager.open(stringBuilder.toString());
                os = new FileOutputStream(this.mDestPath);
                while (true) {
                    int read = inputStream.read(bs);
                    len = read;
                    if (read != -1) {
                        os.write(bs, 0, len);
                    } else {
                        try {
                            os.flush();
                            os.close();
                            return;
                        } catch (Exception e) {
                            return;
                        }
                    }
                }
            } catch (Exception e2) {
                e2.printStackTrace();
                if (os != null) {
                    os.flush();
                    os.close();
                }
            } catch (Throwable th) {
                if (os != null) {
                    try {
                        os.flush();
                        os.close();
                    } catch (Exception e3) {
                    }
                }
            }
        }
    }

    public static boolean deleteDirectoryRecursively(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return false;
        }
        for (File entry : directory.listFiles()) {
            if (entry.isDirectory()) {
                deleteDirectoryRecursively(entry);
            }
            if (!entry.delete()) {
                return false;
            }
        }
        return directory.delete();
    }

    public static byte[] readFileToByteArray(File file) throws IOException {
        int length = (int) file.length();
        byte[] data = new byte[length];
        FileInputStream stream = new FileInputStream(file);
        int offset = 0;
        while (offset < length) {
            try {
                offset += stream.read(data, offset, length - offset);
            } catch (IOException e) {
                throw e;
            } catch (Throwable th) {
                stream.close();
            }
        }
        stream.close();
        return data;
    }

    public static void saveAssetsToSdcard() {
        new Thread(new Runnable() {
            public void run() {
                File file = CameraApp.getContext().getFilesDir();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(file.getAbsolutePath());
                stringBuilder.append(File.separator);
                stringBuilder.append("beauty");
                stringBuilder.append(File.separator);
                FileUtil.mEffectParentPath = stringBuilder.toString();
                File fileDest = new File(FileUtil.mEffectParentPath);
                if (!fileDest.exists()) {
                    fileDest.mkdir();
                }
                FileUtil.initModels(CameraApp.getContext());
                for (Model model : FileUtil.mAllModels) {
                    String name = new StringBuilder();
                    name.append(FileUtil.mEffectParentPath);
                    name.append(model.zipName);
                    name = name.toString();
                    if (!new File(name).exists()) {
                        new WorkRunnable(model.zipName, name).run();
                    }
                }
            }
        }).start();
    }

    private static void initModels(Context context) {
        InputStream inputStream = null;
        ByteArrayOutputStream baos = null;
        try {
            inputStream = context.getResources().getAssets().open("stickerData.json");
            byte[] buffer = new byte[1024];
            baos = new ByteArrayOutputStream();
            int count = 0;
            while (true) {
                int read = inputStream.read(buffer);
                count = read;
                if (read <= 0) {
                    break;
                }
                baos.write(buffer, 0, count);
            }
            mAllModels.addAll((List) new Gson().fromJson(new String(baos.toByteArray()), new TypeToken<List<Model>>() {
            }.getType()));
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                baos.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        } catch (Exception e3) {
            e3.printStackTrace();
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e22) {
                    e22.printStackTrace();
                }
            }
            if (baos != null) {
                baos.close();
            }
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e4) {
                    e4.printStackTrace();
                }
            }
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e42) {
                    e42.printStackTrace();
                }
            }
        }
    }

    public static void saveFile(String filename, byte[] data) throws Exception {
        if (data != null) {
            String filepath = new StringBuilder();
            filepath.append("/sdcard/");
            filepath.append(filename);
            File file = new File(filepath.toString());
            if (file.exists()) {
                file.delete();
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data, 0, data.length);
            fos.flush();
            fos.close();
        }
    }
}

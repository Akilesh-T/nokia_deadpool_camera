package com.hmdglobal.app.camera.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;
import java.io.File;
import java.util.List;

public class StorageUtils {
    public static final String DEFAULT_DIR = "/DCIM";
    private static final String TAG = "HG";

    public static String getFileDir(Context context) {
        return getFileDir(context, DEFAULT_DIR);
    }

    public static String getFileDir(Context context, String defaultDir) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getFileDir : ");
        stringBuilder.append(StorageUtilProxy.getStoragePath(context, true));
        stringBuilder.append(defaultDir);
        Log.i("HG", stringBuilder.toString());
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(StorageUtilProxy.getStoragePath(context, true));
        stringBuilder2.append(defaultDir);
        return stringBuilder2.toString();
    }

    public static void start2CreateDir(Context mContext) {
        ContentResolver cr = mContext.getContentResolver();
        String path = getFileDir(mContext);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("start2CreateDir path: ");
        stringBuilder.append(path);
        Log.i("HG", stringBuilder.toString());
        Uri accessUri = getCurrentAccessUri(path, cr);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("start2CreateDir accessUri: ");
        stringBuilder2.append(accessUri);
        Log.i("HG", stringBuilder2.toString());
        if (!new File(path).exists() && accessUri != null) {
            createDir(cr, accessUri, Environment.DIRECTORY_DCIM);
        }
    }

    public static Uri getCurrentAccessUri(String storagePath, ContentResolver mContentResolver) {
        String exactStorageName = getExactStorageName(storagePath);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("finsh Exact, so we get the StorageName, storagePath = ");
        stringBuilder.append(exactStorageName);
        Log.i("HG", stringBuilder.toString());
        List<UriPermission> uriPermissions = mContentResolver.getPersistedUriPermissions();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getCurrentAccessUri uriPermissions = ");
        stringBuilder2.append(uriPermissions);
        Log.i("HG", stringBuilder2.toString());
        for (UriPermission permission : uriPermissions) {
            if (exactStorageName != null && permission.getUri().toString().contains(exactStorageName)) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("getCurrentAccessUri we get the hope Uri: ");
                stringBuilder3.append(permission.getUri());
                Log.i("HG", stringBuilder3.toString());
                return permission.getUri();
            }
        }
        Log.i("HG", "getCurrentAccessUri return null,there is nothing to find about Uri");
        return null;
    }

    public static String getExactStorageName(String storagePath) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("start to Exact StorageName, storagePath: ");
        stringBuilder.append(storagePath);
        Log.i("HG", stringBuilder.toString());
        String[] pathName = storagePath.split("/");
        return pathName[pathName.length - 2];
    }

    public static Uri createDir(ContentResolver cr, Uri uri, String dirName) {
        Uri doc = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri));
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("createDir docUri : ");
        stringBuilder.append(doc);
        Log.i("HG", stringBuilder.toString());
        try {
            Uri dir = DocumentsContract.createDocument(cr, doc, "vnd.android.document/directory", dirName);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("createDir Success, result : ");
            stringBuilder2.append(dir);
            Log.i("HG", stringBuilder2.toString());
            return dir;
        } catch (Exception e) {
            Log.e("HG", "Create external directory failed.");
            return null;
        }
    }

    public static File getFileDirReturnFile(Context context) {
        return new File(getFileDir(context));
    }

    public static boolean ExternalFileDirIsExist(Context context) {
        boolean result = new File(getFileDir(context)).exists();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("External FileDir is exist, result : ");
        stringBuilder.append(result);
        Log.i("HG", stringBuilder.toString());
        return result;
    }
}

package com.hmdglobal.app.camera.data;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.data.LocalMediaData.PhotoData;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.exif.ExifInterface;
import com.hmdglobal.app.camera.exif.ExifTag;
import com.morphoinc.app.panoramagp3.Camera2ParamsFragment;
import com.morphoinc.utils.multimedia.MediaProviderUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class RotationTask extends AsyncTask<LocalData, Void, LocalData> {
    private static final Tag TAG = new Tag("RotationTask");
    private final LocalDataAdapter mAdapter;
    private final boolean mClockwise;
    private final Context mContext;
    private final int mCurrentDataId;
    private ProgressDialog mProgress;

    public RotationTask(Context context, LocalDataAdapter adapter, int currentDataId, boolean clockwise) {
        this.mContext = context;
        this.mAdapter = adapter;
        this.mCurrentDataId = currentDataId;
        this.mClockwise = clockwise;
    }

    /* Access modifiers changed, original: protected */
    public void onPreExecute() {
        this.mProgress = new ProgressDialog(this.mContext);
        this.mProgress.setTitle(this.mContext.getString(this.mClockwise ? R.string.rotate_right : R.string.rotate_left));
        this.mProgress.setMessage(this.mContext.getString(R.string.please_wait));
        this.mProgress.setCancelable(false);
        this.mProgress.show();
    }

    /* Access modifiers changed, original: protected|varargs */
    public LocalData doInBackground(LocalData... data) {
        return rotateInJpegExif(data[0]);
    }

    private LocalData rotateInJpegExif(LocalData data) {
        Tag tag;
        StringBuilder stringBuilder;
        LocalData localData = data;
        if (localData instanceof PhotoData) {
            int finalRotationDegrees;
            PhotoData imageData = (PhotoData) localData;
            int originRotation = imageData.getRotation();
            if (this.mClockwise) {
                finalRotationDegrees = (originRotation + 90) % 360;
            } else {
                finalRotationDegrees = (originRotation + MediaProviderUtils.ROTATION_270) % 360;
            }
            int finalRotationDegrees2 = finalRotationDegrees;
            String filePath = imageData.getPath();
            ContentValues values = new ContentValues();
            boolean success = false;
            int newOrientation = 0;
            if (imageData.getMimeType().equalsIgnoreCase("image/jpeg")) {
                ExifInterface exifInterface = new ExifInterface();
                ExifTag tag2 = exifInterface.buildTag(ExifInterface.TAG_ORIENTATION, Short.valueOf(ExifInterface.getOrientationValueForRotation(finalRotationDegrees2)));
                if (tag2 != null) {
                    exifInterface.setTag(tag2);
                    try {
                        exifInterface.forceRewriteExif(filePath);
                        values.put("_size", Long.valueOf(new File(filePath).length()));
                        newOrientation = finalRotationDegrees2;
                        success = true;
                    } catch (FileNotFoundException e) {
                        tag = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Cannot find file to set exif: ");
                        stringBuilder.append(filePath);
                        Log.w(tag, stringBuilder.toString());
                    } catch (IOException e2) {
                        tag = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Cannot set exif data: ");
                        stringBuilder.append(filePath);
                        Log.w(tag, stringBuilder.toString());
                    }
                } else {
                    Tag tag3 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Cannot build tag: ");
                    stringBuilder2.append(ExifInterface.TAG_ORIENTATION);
                    Log.w(tag3, stringBuilder2.toString());
                }
            }
            finalRotationDegrees = newOrientation;
            PhotoData result = null;
            if (success) {
                values.put("orientation", Integer.valueOf(finalRotationDegrees2));
                this.mContext.getContentResolver().update(imageData.getUri(), values, null, null);
                double[] latLong = data.getLatLong();
                double latitude = Camera2ParamsFragment.TARGET_EV;
                double longitude = Camera2ParamsFragment.TARGET_EV;
                if (latLong != null) {
                    latitude = latLong[0];
                    longitude = latLong[1];
                }
                int i = finalRotationDegrees;
                result = new PhotoData(data.getContentId(), data.getTitle(), data.getMimeType(), data.getDateTaken(), data.getDateModified(), data.getPath(), i, imageData.getWidth(), imageData.getHeight(), data.getSizeInBytes(), latitude, longitude);
            }
            return result;
        }
        Log.w(TAG, "Rotation can only happen on PhotoData.");
        return null;
    }

    /* Access modifiers changed, original: protected */
    public void onPostExecute(LocalData result) {
        this.mProgress.dismiss();
        if (result != null) {
            this.mAdapter.updateData(this.mCurrentDataId, result);
        }
    }
}

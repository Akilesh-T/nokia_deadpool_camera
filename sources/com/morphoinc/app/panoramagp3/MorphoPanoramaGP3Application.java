package com.morphoinc.app.panoramagp3;

import android.app.Application;
import android.net.Uri;
import android.util.Size;
import com.morphoinc.app.viewer.MorphoPanoramaViewer;
import com.morphoinc.app.viewer.MorphoPanoramaViewer.ViewParam;
import java.io.File;

public class MorphoPanoramaGP3Application extends Application {
    private String mInputFilePath;
    private final LastShotFile mLastShot = new LastShotFile();
    private MorphoPanoramaViewer mMorphoImageStitcher;
    private ViewParam mPostviewDefaultParam;
    private ViewParam mPostviewParam;
    private Size mPreviewSize = new Size(0, 0);
    private int[] mSupportedPictureSizes = new int[0];
    private Size mThumbnailMaxSize = new Size(1, 1);

    public static class LastShotFile {
        private File mFile = null;
        private int mHeight = 0;
        private Uri mUri = null;
        private int mWidth = 0;

        public void setFile(File file) {
            this.mFile = file;
        }

        public String filePath() {
            if (this.mFile == null) {
                return "";
            }
            return this.mFile.getAbsolutePath();
        }

        public Uri uri() {
            return this.mUri;
        }

        public void setUri(Uri uri) {
            this.mUri = uri;
        }

        public int width() {
            return this.mWidth;
        }

        public void setWidth(int width) {
            this.mWidth = width;
        }

        public int height() {
            return this.mHeight;
        }

        public void setHeight(int height) {
            this.mHeight = height;
        }
    }

    public MorphoPanoramaViewer getMorphoImageStitcher() {
        if (this.mMorphoImageStitcher == null || MorphoPanoramaViewer.isFinished()) {
            this.mMorphoImageStitcher = new MorphoPanoramaViewer();
        }
        return this.mMorphoImageStitcher;
    }

    public Size[] getSupportedPictureSizes() {
        int num = this.mSupportedPictureSizes.length >> 1;
        Size[] sizes = new Size[num];
        for (int i = 0; i < num; i++) {
            sizes[i] = new Size(this.mSupportedPictureSizes[i * 2], this.mSupportedPictureSizes[(i * 2) + 1]);
        }
        return sizes;
    }

    public void setSupportedPictureSizes(int[] sizes) {
        this.mSupportedPictureSizes = (int[]) sizes.clone();
    }

    public Size getPreviewSize() {
        return this.mPreviewSize;
    }

    public void setPreviewSize(Size size) {
        this.mPreviewSize = size;
    }

    public void setLastShotFile(String path, int width, int height) {
        this.mLastShot.setFile(new File(path));
        this.mLastShot.setWidth(width);
        this.mLastShot.setHeight(height);
    }

    public LastShotFile getLastShotFile() {
        return this.mLastShot;
    }

    public void clearLastShotFile() {
        this.mLastShot.setFile(null);
        this.mLastShot.setWidth(0);
        this.mLastShot.setHeight(0);
        this.mLastShot.setUri(null);
    }

    public void updateThumbnailMaxSize(int width, int height) {
        if (width * height >= this.mThumbnailMaxSize.getWidth() * this.mThumbnailMaxSize.getHeight()) {
            this.mThumbnailMaxSize = new Size(width, height);
        }
    }

    public final Size getThumbnailMaxSize() {
        return this.mThumbnailMaxSize;
    }

    public void setInputFilePath(String path) {
        this.mInputFilePath = path;
    }

    public String getInputFilePath() {
        return this.mInputFilePath;
    }

    public ViewParam getPostviewParam() {
        return this.mPostviewParam;
    }

    public ViewParam getPostviewDefaultParam() {
        return this.mPostviewDefaultParam;
    }
}

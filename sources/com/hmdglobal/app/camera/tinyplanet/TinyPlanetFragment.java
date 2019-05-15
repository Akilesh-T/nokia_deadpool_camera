package com.hmdglobal.app.camera.tinyplanet;

import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.hmdglobal.app.camera.CameraActivity;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.app.CameraApp;
import com.hmdglobal.app.camera.app.MediaSaver;
import com.hmdglobal.app.camera.app.MediaSaver.OnMediaSavedListener;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.exif.ExifInterface;
import com.hmdglobal.app.camera.tinyplanet.TinyPlanetPreview.PreviewSizeListener;
import com.hmdglobal.app.camera.util.XmpUtil;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TinyPlanetFragment extends DialogFragment implements PreviewSizeListener {
    public static final String ARGUMENT_TITLE = "title";
    public static final String ARGUMENT_URI = "uri";
    public static final String CROPPED_AREA_FULL_PANO_HEIGHT_PIXELS = "FullPanoHeightPixels";
    public static final String CROPPED_AREA_FULL_PANO_WIDTH_PIXELS = "FullPanoWidthPixels";
    public static final String CROPPED_AREA_IMAGE_HEIGHT_PIXELS = "CroppedAreaImageHeightPixels";
    public static final String CROPPED_AREA_IMAGE_WIDTH_PIXELS = "CroppedAreaImageWidthPixels";
    public static final String CROPPED_AREA_LEFT = "CroppedAreaLeftPixels";
    public static final String CROPPED_AREA_TOP = "CroppedAreaTopPixels";
    private static final String FILENAME_PREFIX = "TINYPLANET_";
    public static final String GOOGLE_PANO_NAMESPACE = "http://ns.google.com/photos/1.0/panorama/";
    private static final int RENDER_DELAY_MILLIS = 50;
    private static final Tag TAG = new Tag("TinyPlanetActivity");
    private final Runnable mCreateTinyPlanetRunnable = new Runnable() {
        public void run() {
            synchronized (TinyPlanetFragment.this.mRendering) {
                if (TinyPlanetFragment.this.mRendering.booleanValue()) {
                    TinyPlanetFragment.this.mRenderOneMore = Boolean.valueOf(true);
                    return;
                }
                TinyPlanetFragment.this.mRendering = Boolean.valueOf(true);
                new AsyncTask<Void, Void, Void>() {
                    /* Access modifiers changed, original: protected|varargs */
                    public Void doInBackground(Void... params) {
                        TinyPlanetFragment.this.mResultLock.lock();
                        try {
                            if (TinyPlanetFragment.this.mSourceBitmap != null) {
                                if (TinyPlanetFragment.this.mResultBitmap != null) {
                                    TinyPlanetNative.process(TinyPlanetFragment.this.mSourceBitmap, TinyPlanetFragment.this.mSourceBitmap.getWidth(), TinyPlanetFragment.this.mSourceBitmap.getHeight(), TinyPlanetFragment.this.mResultBitmap, TinyPlanetFragment.this.mPreviewSizePx, TinyPlanetFragment.this.mCurrentZoom, TinyPlanetFragment.this.mCurrentAngle);
                                    TinyPlanetFragment.this.mResultLock.unlock();
                                    return null;
                                }
                            }
                            TinyPlanetFragment.this.mResultLock.unlock();
                            return null;
                        } catch (Throwable th) {
                            TinyPlanetFragment.this.mResultLock.unlock();
                        }
                    }

                    /* Access modifiers changed, original: protected */
                    public void onPostExecute(Void result) {
                        TinyPlanetFragment.this.mPreview.setBitmap(TinyPlanetFragment.this.mResultBitmap, TinyPlanetFragment.this.mResultLock);
                        synchronized (TinyPlanetFragment.this.mRendering) {
                            TinyPlanetFragment.this.mRendering = Boolean.valueOf(false);
                            if (TinyPlanetFragment.this.mRenderOneMore.booleanValue()) {
                                TinyPlanetFragment.this.mRenderOneMore = Boolean.valueOf(false);
                                TinyPlanetFragment.this.scheduleUpdate();
                            }
                        }
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
            }
        }
    };
    private float mCurrentAngle = 0.0f;
    private float mCurrentZoom = 0.5f;
    private ProgressDialog mDialog;
    private final Handler mHandler = new Handler();
    private String mOriginalTitle = "";
    private TinyPlanetPreview mPreview;
    private int mPreviewSizePx = 0;
    private Boolean mRenderOneMore = Boolean.valueOf(false);
    private Boolean mRendering = Boolean.valueOf(false);
    private Bitmap mResultBitmap;
    private final Lock mResultLock = new ReentrantLock();
    private Bitmap mSourceBitmap;
    private Uri mSourceImageUri;

    private static final class TinyPlanetImage {
        public final byte[] mJpegData;
        public final int mSize;

        public TinyPlanetImage(byte[] jpegData, int size) {
            this.mJpegData = jpegData;
            this.mSize = size;
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(0, R.style.f274Theme.Camera);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(1);
        getDialog().setCanceledOnTouchOutside(true);
        View view = inflater.inflate(R.layout.tinyplanet_editor, container, false);
        this.mPreview = (TinyPlanetPreview) view.findViewById(R.id.preview);
        this.mPreview.setPreviewSizeChangeListener(this);
        ((SeekBar) view.findViewById(R.id.zoomSlider)).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                TinyPlanetFragment.this.onZoomChange(progress);
            }
        });
        ((SeekBar) view.findViewById(R.id.angleSlider)).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                TinyPlanetFragment.this.onAngleChange(progress);
            }
        });
        ((Button) view.findViewById(R.id.creatTinyPlanetButton)).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                TinyPlanetFragment.this.onCreateTinyPlanet();
            }
        });
        this.mOriginalTitle = getArguments().getString("title");
        this.mSourceImageUri = Uri.parse(getArguments().getString(ARGUMENT_URI));
        this.mSourceBitmap = createPaddedSourceImage(this.mSourceImageUri, true);
        if (this.mSourceBitmap == null) {
            Log.e(TAG, "Could not decode source image.");
            dismiss();
        }
        return view;
    }

    private Bitmap createPaddedSourceImage(Uri sourceImageUri, boolean previewSize) {
        InputStream is = getInputStream(sourceImageUri);
        if (is == null) {
            Log.e(TAG, "Could not create input stream for image.");
            dismiss();
        }
        Bitmap sourceBitmap = BitmapFactory.decodeStream(is);
        XMPMeta xmp = XmpUtil.extractXMPMeta(getInputStream(sourceImageUri));
        if (xmp == null) {
            return sourceBitmap;
        }
        return createPaddedBitmap(sourceBitmap, xmp, previewSize ? getDisplaySize() : sourceBitmap.getWidth());
    }

    private void onCreateTinyPlanet() {
        synchronized (this.mRendering) {
            this.mRenderOneMore = Boolean.valueOf(false);
        }
        final String savingTinyPlanet = getActivity().getResources().getString(R.string.saving_tiny_planet);
        new AsyncTask<Void, Void, TinyPlanetImage>() {
            /* Access modifiers changed, original: protected */
            public void onPreExecute() {
                TinyPlanetFragment.this.mDialog = ProgressDialog.show(TinyPlanetFragment.this.getActivity(), null, savingTinyPlanet, true, false);
            }

            /* Access modifiers changed, original: protected|varargs */
            public TinyPlanetImage doInBackground(Void... params) {
                return TinyPlanetFragment.this.createFinalTinyPlanet();
            }

            /* Access modifiers changed, original: protected */
            public void onPostExecute(TinyPlanetImage image) {
                TinyPlanetImage tinyPlanetImage = image;
                final CameraActivity activity = (CameraActivity) TinyPlanetFragment.this.getActivity();
                MediaSaver mediaSaver = ((CameraApp) activity.getApplication()).getMediaSaver();
                OnMediaSavedListener doneListener = new OnMediaSavedListener() {
                    public void onMediaSaved(Uri uri) {
                        activity.notifyNewMedia(uri);
                        TinyPlanetFragment.this.mDialog.dismiss();
                        TinyPlanetFragment.this.dismiss();
                    }
                };
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(TinyPlanetFragment.FILENAME_PREFIX);
                stringBuilder.append(TinyPlanetFragment.this.mOriginalTitle);
                String tinyPlanetTitle = stringBuilder.toString();
                mediaSaver.addImage(tinyPlanetImage.mJpegData, tinyPlanetTitle, new Date().getTime(), null, tinyPlanetImage.mSize, tinyPlanetImage.mSize, 0, null, doneListener, TinyPlanetFragment.this.getActivity().getContentResolver());
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    private TinyPlanetImage createFinalTinyPlanet() {
        this.mResultLock.lock();
        try {
            this.mResultBitmap.recycle();
            this.mResultBitmap = null;
            this.mSourceBitmap.recycle();
            this.mSourceBitmap = null;
            Bitmap sourceBitmap = createPaddedSourceImage(this.mSourceImageUri, false);
            int width = sourceBitmap.getWidth();
            int height = sourceBitmap.getHeight();
            int outputSize = width / 2;
            Bitmap resultBitmap = Bitmap.createBitmap(outputSize, outputSize, Config.ARGB_8888);
            TinyPlanetNative.process(sourceBitmap, width, height, resultBitmap, outputSize, this.mCurrentZoom, this.mCurrentAngle);
            sourceBitmap.recycle();
            ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
            resultBitmap.compress(CompressFormat.JPEG, 100, jpeg);
            return new TinyPlanetImage(addExif(jpeg.toByteArray()), outputSize);
        } finally {
            this.mResultLock.unlock();
        }
    }

    private byte[] addExif(byte[] jpeg) {
        ExifInterface exif = new ExifInterface();
        exif.addDateTimeStampTag(ExifInterface.TAG_DATE_TIME, System.currentTimeMillis(), TimeZone.getDefault());
        OutputStream jpegOut = new ByteArrayOutputStream();
        try {
            exif.writeExif(jpeg, jpegOut);
        } catch (IOException e) {
            Log.e(TAG, "Could not write EXIF", e);
        }
        return jpegOut.toByteArray();
    }

    private int getDisplaySize() {
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return Math.min(size.x, size.y);
    }

    public void onSizeChanged(int sizePx) {
        this.mPreviewSizePx = sizePx;
        this.mResultLock.lock();
        try {
            if (!(this.mResultBitmap != null && this.mResultBitmap.getWidth() == sizePx && this.mResultBitmap.getHeight() == sizePx)) {
                if (this.mResultBitmap != null) {
                    this.mResultBitmap.recycle();
                }
                this.mResultBitmap = Bitmap.createBitmap(this.mPreviewSizePx, this.mPreviewSizePx, Config.ARGB_8888);
            }
            this.mResultLock.unlock();
            scheduleUpdate();
        } catch (Throwable th) {
            this.mResultLock.unlock();
        }
    }

    private void onZoomChange(int zoom) {
        this.mCurrentZoom = ((float) zoom) / 1000.0f;
        scheduleUpdate();
    }

    private void onAngleChange(int angle) {
        this.mCurrentAngle = (float) Math.toRadians((double) angle);
        scheduleUpdate();
    }

    private void scheduleUpdate() {
        this.mHandler.removeCallbacks(this.mCreateTinyPlanetRunnable);
        this.mHandler.postDelayed(this.mCreateTinyPlanetRunnable, 50);
    }

    private InputStream getInputStream(Uri uri) {
        try {
            return getActivity().getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Could not load source image.", e);
            return null;
        }
    }

    private static Bitmap createPaddedBitmap(Bitmap bitmapIn, XMPMeta xmp, int intermediateWidth) {
        Bitmap bitmap = bitmapIn;
        XMPMeta xMPMeta = xmp;
        int i;
        try {
            int croppedAreaWidth = getInt(xMPMeta, CROPPED_AREA_IMAGE_WIDTH_PIXELS);
            int croppedAreaHeight = getInt(xMPMeta, CROPPED_AREA_IMAGE_HEIGHT_PIXELS);
            int fullPanoWidth = getInt(xMPMeta, CROPPED_AREA_FULL_PANO_WIDTH_PIXELS);
            int fullPanoHeight = getInt(xMPMeta, CROPPED_AREA_FULL_PANO_HEIGHT_PIXELS);
            int left = getInt(xMPMeta, CROPPED_AREA_LEFT);
            int top = getInt(xMPMeta, CROPPED_AREA_TOP);
            int i2;
            int i3;
            if (fullPanoWidth == 0) {
                i = intermediateWidth;
                i2 = croppedAreaWidth;
                i3 = croppedAreaHeight;
            } else if (fullPanoHeight == 0) {
                i = intermediateWidth;
                i2 = croppedAreaWidth;
                i3 = croppedAreaHeight;
            } else {
                float scale = ((float) intermediateWidth) / ((float) fullPanoWidth);
                Bitmap paddedBitmap = null;
                while (paddedBitmap == null) {
                    try {
                        paddedBitmap = Bitmap.createBitmap((int) (((float) fullPanoWidth) * scale), (int) (((float) fullPanoHeight) * scale), Config.ARGB_8888);
                    } catch (OutOfMemoryError e) {
                        try {
                            System.gc();
                            scale /= 2.0f;
                        } catch (XMPException e2) {
                            return bitmap;
                        }
                    }
                }
                new Canvas(paddedBitmap).drawBitmap(bitmap, null, new RectF(((float) left) * scale, ((float) top) * scale, ((float) (left + croppedAreaWidth)) * scale, ((float) (top + croppedAreaHeight)) * scale), null);
                return paddedBitmap;
            }
            return bitmap;
        } catch (XMPException e3) {
            i = intermediateWidth;
            return bitmap;
        }
    }

    private static int getInt(XMPMeta xmp, String key) throws XMPException {
        if (xmp.doesPropertyExist(GOOGLE_PANO_NAMESPACE, key)) {
            return xmp.getPropertyInteger(GOOGLE_PANO_NAMESPACE, key).intValue();
        }
        return 0;
    }
}

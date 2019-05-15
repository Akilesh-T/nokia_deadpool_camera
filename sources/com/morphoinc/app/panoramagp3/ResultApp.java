package com.morphoinc.app.panoramagp3;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.util.Xml;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ToggleButton;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.app.CameraApp;
import com.hmdglobal.app.camera.tinyplanet.TinyPlanetFragment;
import com.morphoinc.app.LogFilter;
import com.morphoinc.app.panoramagp3.PanoramaTimer.PanoramaTimerListener;
import com.morphoinc.app.viewer.MorphoPanoramaViewer;
import com.morphoinc.app.viewer.MorphoPanoramaViewer.GalleryData;
import com.morphoinc.app.viewer.MorphoPanoramaViewer.PanoramaViewerInitParam;
import com.morphoinc.app.viewer.MorphoPanoramaViewer.ViewParam;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Timer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class ResultApp extends Activity implements OnGestureListener, OnDoubleTapListener, OnTouchListener, OnClickListener, Runnable {
    private static final int DECODE_POST_VIEWDATA_DONE = 4;
    private static String LOG_TAG = null;
    private static final int MSG_POSTVIEW_DATA_DECODE_ERROR = 3;
    private static final int MSG_REREGISTER_TEXTURE_COMP = 0;
    private static final int MSG_SET_POSTVIEW_DATA_COMP = 1;
    private static final int MSG_SET_POSTVIEW_DATA_ERROR = 2;
    private static final String PREFS_NAME = "mp_result_app_setting";
    private static final String SAVE_DATA_KEY_SENSOR_DRIVEN = "sensor_driven";
    private final int DIALOG_ID_DECODE_ERROR = 0;
    private final int DIALOG_ID_DISP_ERROR = 1;
    private final int DIALOG_ID_LOADING = 4;
    private final int DIALOG_ID_OUTPUT_ERROR = 2;
    private final int DIALOG_ID_PROCESSING = 3;
    private final int FULL_VIEW_MODE = 0;
    private final float INTERIA_SCROLL_DECELERATION_RATE = 0.075f;
    private final int INTERIA_SCROLL_INTERVAL = 20;
    private final int SENSOR_DRIVEN_INTERVAL = 20;
    private final int SLIDE_VIEW_MODE = 1;
    private boolean isFileSelect;
    private boolean isFirstSensorDriven = true;
    private boolean isMoveDisable;
    private boolean isRenderLowImage;
    private boolean isTouching = false;
    private boolean isUseSensorDriven;
    private Sensor mAccelerometer = null;
    private ResultApp mActivity;
    private int mAppSensorFusionMode;
    private ImageButton mDeleteButton;
    private float mDiffX;
    private float mDiffY;
    private int mDispMode = 0;
    private int mDispType;
    private int[] mExifOrientation = new int[1];
    private String mFileName;
    private GLSurfaceView mGLPanoramaView;
    private GalleryData mGalleryData;
    private GestureDetector mGestureDetector;
    private Sensor mGyroscope = null;
    private Sensor mGyroscopeUncalibrated = null;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.arg1) {
                case 1:
                    ResultApp.this.mRenderer.setRenderEnable(true);
                    ResultApp.this.mGLPanoramaView.requestRender();
                    ResultApp.this.showDialog(4);
                    return;
                case 2:
                    ResultApp.this.showDialog(1);
                    return;
                case 3:
                    ResultApp.this.showDialog(0);
                    return;
                case 4:
                    ResultApp.this.mRenderer.setRenderEnable(true);
                    ResultApp.this.createGLSurface();
                    return;
                default:
                    return;
            }
        }
    };
    private HorizontalScrollView mHorizontalView;
    private PanoramaTimer mInertiaScrollTimer;
    private ImageButton mInfoButton;
    private Sensor mMagneticField = null;
    private MorphoPanoramaViewer mMorphoImageStitcher;
    private Bitmap mOrgBitmap = null;
    private double[] mPrevSensorMat = new double[9];
    private float mPreviousScale = 1.0f;
    private float mPreviousX;
    private float mPreviousY;
    private ProgressDialog mProgressDialog;
    private Handler mRenderHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.arg1 == 0) {
                ResultApp.this.setPostviewData();
            }
        }
    };
    private Timer mRenderTimer;
    private PostviewRenderer mRenderer;
    private ImageView mResultView;
    private int mRotateCount;
    private Sensor mRotationVector = null;
    private SimpleOnScaleGestureListener mSGListner = new SimpleOnScaleGestureListener() {
        public boolean onScale(ScaleGestureDetector detector) {
            String access$1500 = ResultApp.LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onScale scale:");
            stringBuilder.append(detector.getScaleFactor());
            LogFilter.d(access$1500, stringBuilder.toString());
            float scale = detector.getScaleFactor();
            ResultApp.this.mRenderer.setScale(scale / ResultApp.this.mPreviousScale);
            ResultApp.this.mGLPanoramaView.requestRender();
            ResultApp.this.mPreviousScale = scale;
            return super.onScale(detector);
        }

        public boolean onScaleBegin(ScaleGestureDetector detector) {
            String access$1500 = ResultApp.LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onScaleBegin scale:");
            stringBuilder.append(detector.getScaleFactor());
            LogFilter.d(access$1500, stringBuilder.toString());
            return super.onScaleBegin(detector);
        }

        public void onScaleEnd(ScaleGestureDetector detector) {
            String access$1500 = ResultApp.LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onScaleEnd scale:");
            stringBuilder.append(detector.getScaleFactor());
            LogFilter.d(access$1500, stringBuilder.toString());
            ResultApp.this.mPreviousScale = 1.0f;
            super.onScaleEnd(detector);
        }
    };
    private ScaleGestureDetector mSacleGestureDetector;
    private Bitmap mScaleBitmap = null;
    private double[] mSensorAngle = new double[3];
    private PanoramaTimer mSensorDrivenTimer;
    private SensorFusion mSensorFusion;
    private int mSensorFusionMode;
    private Object mSensorLockObj = new Object();
    private SensorManager mSensorManager;
    private double[] mSensorMat = new double[9];
    private ImageButton mShareButton;
    private ToggleButton mToggleButton_SensorDriven;
    private ImageButton mViewChangeButton;
    private ViewParam[] mViewParam = new ViewParam[2];

    static /* synthetic */ float access$1224(ResultApp x0, float x1) {
        float f = x0.mDiffX - x1;
        x0.mDiffX = f;
        return f;
    }

    static /* synthetic */ float access$1324(ResultApp x0, float x1) {
        float f = x0.mDiffY - x1;
        x0.mDiffY = f;
        return f;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOG_TAG = "Morpho PanoramaViewer";
        requestWindowFeature(1);
        setContentView(R.layout.result_display);
        CameraApp app = (CameraApp) getApplication();
        this.mActivity = this;
        this.mInfoButton = (ImageButton) findViewById(R.id.ButtonInfo);
        this.mInfoButton.setOnClickListener(this);
        this.mShareButton = (ImageButton) findViewById(R.id.ButtonShare);
        this.mShareButton.setOnClickListener(this);
        this.mDeleteButton = (ImageButton) findViewById(R.id.ButtonDelete);
        this.mDeleteButton.setOnClickListener(this);
        this.mGestureDetector = new GestureDetector(this);
        this.mSacleGestureDetector = new ScaleGestureDetector(getApplicationContext(), this.mSGListner);
        this.mHorizontalView = (HorizontalScrollView) findViewById(R.id.HorizontalScrollView01);
        this.mHorizontalView.requestDisallowInterceptTouchEvent(true);
        this.mHorizontalView.setOnTouchListener(this);
        this.mHorizontalView.setScrollBarStyle(0);
        this.mToggleButton_SensorDriven = (ToggleButton) findViewById(R.id.ToggleButton_SensorDriven);
        this.mResultView = (ImageView) findViewById(R.id.ImageView01);
        this.mMorphoImageStitcher = app.getMorphoImageStitcher();
        Intent intent = getIntent();
        this.mFileName = intent.getStringExtra(Camera2App.INTENT_FILENAME);
        this.mDispType = 0;
        this.mViewParam[0] = app.getPostviewParam();
        this.mViewParam[1] = app.getPostviewDefaultParam();
        decodePostViewData(this.mMorphoImageStitcher, this.mFileName, this.mExifOrientation, this.mViewParam);
        createGLSurface();
        this.mAppSensorFusionMode = intent.getIntExtra(SettingActivity.INTENT_KEY_SENSOR_ASPECT, 2);
        this.isUseSensorDriven = getSharedPreferences(PREFS_NAME, 0).getBoolean(SAVE_DATA_KEY_SENSOR_DRIVEN, false);
        this.mSensorManager = (SensorManager) getSystemService("sensor");
        for (Sensor sensor : this.mSensorManager.getSensorList(-1)) {
            if (sensor.getType() == 4) {
                this.mGyroscope = this.mSensorManager.getDefaultSensor(4);
            }
            if (sensor.getType() == 16) {
                this.mGyroscopeUncalibrated = this.mSensorManager.getDefaultSensor(16);
            }
            if (sensor.getType() == 11) {
                this.mRotationVector = this.mSensorManager.getDefaultSensor(11);
            }
            if (sensor.getType() == 1) {
                this.mAccelerometer = this.mSensorManager.getDefaultSensor(1);
            }
            if (sensor.getType() == 2) {
                this.mMagneticField = this.mSensorManager.getDefaultSensor(2);
            }
        }
        String str;
        StringBuilder stringBuilder;
        if (this.mGyroscope == null && this.mGyroscopeUncalibrated == null && this.mRotationVector == null && (this.mAccelerometer == null || this.mMagneticField == null)) {
            this.isUseSensorDriven = false;
            this.mToggleButton_SensorDriven.setChecked(false);
            this.mToggleButton_SensorDriven.setEnabled(false);
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("isUseSensorDriven is false ");
            stringBuilder.append(this.isUseSensorDriven);
            Log.i(str, stringBuilder.toString());
        } else {
            this.mToggleButton_SensorDriven.setChecked(this.isUseSensorDriven);
            str = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("isUseSensorDriven is true ");
            stringBuilder.append(this.isUseSensorDriven);
            Log.i(str, stringBuilder.toString());
        }
        this.mSensorFusion = new SensorFusion(false);
        this.mSensorFusionMode = 2;
        this.mSensorFusion.setMode(this.mSensorFusionMode);
        this.mSensorFusion.setOffsetMode(1);
        this.mSensorFusion.setAppState(1);
        this.mSensorDrivenTimer = new PanoramaTimer(true);
        this.mSensorDrivenTimer.setTimerListener(new PanoramaTimerListener() {
            public void onTimeout() {
                float rot_x = 0.0f;
                float rot_y = 0.0f;
                boolean is_first = false;
                synchronized (ResultApp.this.mSensorLockObj) {
                    ResultApp.this.mSensorFusion.setRotation(ResultApp.this.getWindowManager().getDefaultDisplay().getRotation());
                    ResultApp.this.mSensorFusion.getSensorMatrix(ResultApp.this.mSensorMat, null, null, null);
                    if (ResultApp.this.mPrevSensorMat == null) {
                        is_first = true;
                        ResultApp.this.mPrevSensorMat = (double[]) ResultApp.this.mSensorMat.clone();
                    } else {
                        MathUtil.getAngleDiff(ResultApp.this.mSensorAngle, ResultApp.this.mSensorMat, ResultApp.this.mPrevSensorMat);
                        System.arraycopy(ResultApp.this.mSensorMat, 0, ResultApp.this.mPrevSensorMat, 0, ResultApp.this.mSensorMat.length);
                        rot_x = (float) (-Math.toDegrees(ResultApp.this.mSensorAngle[2]));
                        rot_y = (float) (-Math.toDegrees(ResultApp.this.mSensorAngle[1]));
                    }
                }
                if (!(ResultApp.this.isTouching || !ResultApp.this.isUseSensorDriven || is_first || ResultApp.this.mRenderer == null)) {
                    ResultApp.this.mRenderer.setSwipeAngle(rot_x, rot_y);
                    ResultApp.this.mGLPanoramaView.requestRender();
                }
                ResultApp.this.mSensorDrivenTimer.start(20);
            }
        });
        if (this.isUseSensorDriven) {
            this.mSensorDrivenTimer.start(20);
        }
        this.mInertiaScrollTimer = new PanoramaTimer(true);
        this.mInertiaScrollTimer.setTimerListener(new PanoramaTimerListener() {
            public void onTimeout() {
                ResultApp.access$1224(ResultApp.this, ResultApp.this.mDiffX * 0.075f);
                ResultApp.access$1324(ResultApp.this, ResultApp.this.mDiffY * 0.075f);
                float value_x = Math.abs(ResultApp.this.mDiffX);
                float value_y = Math.abs(ResultApp.this.mDiffY);
                if (value_x >= 1.0f || value_y >= 1.0f) {
                    ResultApp.this.mRenderer.setSwipeAngle((float) (((double) ResultApp.this.mDiffX) * 0.03d), (float) (((double) ResultApp.this.mDiffY) * 0.03d));
                    ResultApp.this.mGLPanoramaView.requestRender();
                    ResultApp.this.mInertiaScrollTimer.start(20);
                    return;
                }
                ResultApp.this.mDiffX = 0.0f;
                ResultApp.this.mDiffY = 0.0f;
            }
        });
    }

    public void run() {
        Message msg = Message.obtain();
        if (decodePostViewData(this.mMorphoImageStitcher, this.mFileName, this.mExifOrientation, this.mViewParam)) {
            msg.arg1 = 4;
        } else {
            msg.arg1 = 3;
        }
        this.mHandler.sendMessage(msg);
    }

    private void createGLSurface() {
        FrameLayout frame_layout = (FrameLayout) findViewById(R.id.FrameLayout01);
        this.mGLPanoramaView = new GLSurfaceView(this);
        this.mGLPanoramaView.setEGLContextClientVersion(1);
        this.mRenderer = new PostviewRenderer(this, this.mRenderHandler, this.mMorphoImageStitcher, this.isFileSelect);
        ViewParam vparam = ((CameraApp) getApplication()).getPostviewParam();
        if (vparam != null) {
            this.mRenderer.setDefaultScale(vparam.scale);
        }
        this.mRenderer.setDefault();
        this.mGLPanoramaView.setRenderer(this.mRenderer);
        this.mGLPanoramaView.setRenderMode(0);
        frame_layout.addView(this.mGLPanoramaView);
        frame_layout.setVisibility(0);
        this.mGLPanoramaView.setVisibility(0);
        this.mGLPanoramaView.setZOrderMediaOverlay(true);
        this.mGLPanoramaView.setOnTouchListener(this);
        this.mGLPanoramaView.setFocusableInTouchMode(true);
        this.mRenderer.setDispType(this.mDispType);
        this.mToggleButton_SensorDriven.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ResultApp.this.isUseSensorDriven = isChecked;
                if (isChecked) {
                    if (ResultApp.this.mSensorDrivenTimer != null && !ResultApp.this.mSensorDrivenTimer.isStarted()) {
                        ResultApp.this.mSensorDrivenTimer.start(20);
                    }
                } else if (ResultApp.this.mSensorDrivenTimer != null && !ResultApp.this.mSensorDrivenTimer.isStarted()) {
                    ResultApp.this.mSensorDrivenTimer.cancel();
                }
            }
        });
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (this.mRenderTimer != null) {
            this.mRenderTimer.cancel();
            this.mRenderTimer = null;
        }
        LogFilter.d(LOG_TAG, "onTouchEvent");
        if (this.mGestureDetector.onTouchEvent(event)) {
            return true;
        }
        this.mSacleGestureDetector.onTouchEvent(event);
        if (event.getPointerCount() > 1) {
            this.isMoveDisable = true;
            return true;
        }
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case 0:
                this.isMoveDisable = false;
                this.isTouching = true;
                if (this.mInertiaScrollTimer.isStarted()) {
                    this.mInertiaScrollTimer.cancel();
                    break;
                }
                break;
            case 1:
                this.isTouching = false;
                if (!this.isMoveDisable) {
                    this.mInertiaScrollTimer.start(20);
                    break;
                }
                break;
            case 2:
                if (!this.isMoveDisable) {
                    this.mDiffX = x - this.mPreviousX;
                    this.mDiffY = y - this.mPreviousY;
                    this.mRenderer.setSwipeDistance(this.mDiffX, this.mDiffY);
                    this.mGLPanoramaView.requestRender();
                    break;
                }
                break;
            case 3:
            case 4:
                this.isTouching = false;
                break;
        }
        this.mPreviousX = x;
        this.mPreviousY = y;
        return true;
    }

    public boolean onDoubleTap(MotionEvent e) {
        if (this.mRenderTimer != null) {
            this.mRenderTimer.cancel();
            this.mRenderTimer = null;
        }
        LogFilter.d(LOG_TAG, "onDoubleTap");
        this.mRenderer.setDefault();
        this.mGLPanoramaView.requestRender();
        return true;
    }

    public boolean onDoubleTapEvent(MotionEvent event) {
        LogFilter.d(LOG_TAG, "onDoubleTapEvent");
        return false;
    }

    public boolean onSingleTapConfirmed(MotionEvent e) {
        LogFilter.d(LOG_TAG, "onSingleTapConfirmed");
        return false;
    }

    public boolean onDown(MotionEvent e) {
        LogFilter.d(LOG_TAG, "onDown");
        return false;
    }

    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        LogFilter.d(LOG_TAG, "onFling");
        return false;
    }

    public void onLongPress(MotionEvent e) {
        LogFilter.d(LOG_TAG, "onLongPress");
    }

    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        LogFilter.d(LOG_TAG, "onScroll");
        return false;
    }

    public void onShowPress(MotionEvent e) {
        LogFilter.d(LOG_TAG, "onShowPress");
    }

    public boolean onSingleTapUp(MotionEvent e) {
        LogFilter.d(LOG_TAG, "onSingleTapUp");
        return false;
    }

    private void setPostviewData() {
        if (this.mGLPanoramaView != null) {
            this.mGLPanoramaView.queueEvent(new Runnable() {
                public void run() {
                    Message msg = Message.obtain();
                    msg.arg1 = 1;
                    if (ResultApp.this.mGalleryData != null) {
                        int ret = ResultApp.this.mMorphoImageStitcher.setGalleryData(ResultApp.this.mGalleryData, 0, 0);
                        String access$1500 = ResultApp.LOG_TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("setGalleryData is OK");
                        stringBuilder.append(ret);
                        Log.i(access$1500, stringBuilder.toString());
                        if (ret != 0) {
                            access$1500 = ResultApp.LOG_TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("mMorphoImageStitche.setGalleryData error ret:");
                            stringBuilder.append(Integer.toHexString(ret));
                            LogFilter.e(access$1500, stringBuilder.toString());
                            msg.arg1 = 2;
                        }
                    }
                    ResultApp.this.mHandler.sendMessage(msg);
                }
            });
        }
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ButtonDelete /*2131230721*/:
                new Builder(this).setTitle("Delete::Are you sure?").setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        File file = new File(ResultApp.this.mFileName);
                        Cursor cursor = null;
                        try {
                            ContentResolver cr = ResultApp.this.getContentResolver();
                            ContentResolver contentResolver = cr;
                            cursor = contentResolver.query(Media.EXTERNAL_CONTENT_URI, new String[]{"_id"}, "_data= ?", new String[]{file.toString()}, null);
                            if (cursor.getCount() != 0) {
                                cursor.moveToFirst();
                                cr.delete(ContentUris.appendId(Media.EXTERNAL_CONTENT_URI.buildUpon(), cursor.getLong(cursor.getColumnIndex("_id"))).build(), null, null);
                            }
                            if (cursor != null) {
                                cursor.close();
                            }
                            file.delete();
                            ResultApp.this.finish();
                        } catch (Throwable th) {
                            if (cursor != null) {
                                cursor.close();
                            }
                        }
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                }).create().show();
                return;
            case R.id.ButtonInfo /*2131230722*/:
                Builder builder = new Builder(this);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Path:");
                stringBuilder.append(this.mFileName.toString());
                AlertDialog dialog = builder.setMessage(stringBuilder.toString()).setPositiveButton(17039370, null).show();
                return;
            case R.id.ButtonShare /*2131230723*/:
                String shareString = new StringBuilder();
                shareString.append("file://");
                shareString.append(this.mFileName);
                shareString = shareString.toString();
                Intent intent_share = new Intent("android.intent.action.SEND");
                intent_share.putExtra("android.intent.extra.STREAM", Uri.parse(shareString));
                intent_share.setType("image/jpg");
                startActivity(intent_share);
                return;
            default:
                return;
        }
    }

    public boolean onTouch(View v, MotionEvent event) {
        LogFilter.d(LOG_TAG, "onTouch");
        return false;
    }

    private void setDispImage(int disp_width, int disp_height) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setDispImage: w:");
        stringBuilder.append(this.mOrgBitmap.getWidth());
        stringBuilder.append(" h:");
        stringBuilder.append(this.mOrgBitmap.getHeight());
        LogFilter.d(str, stringBuilder.toString());
        if (this.mScaleBitmap != null) {
            this.mScaleBitmap.recycle();
        }
        Matrix matrix;
        float ratio;
        if (this.mDispMode == 0) {
            matrix = new Matrix();
            ratio = ((float) disp_width) / ((float) this.mOrgBitmap.getWidth());
            matrix.postScale(ratio, ratio);
            this.mScaleBitmap = Bitmap.createBitmap(this.mOrgBitmap, 0, 0, this.mOrgBitmap.getWidth(), this.mOrgBitmap.getHeight(), matrix, true);
            this.mResultView.setImageBitmap(this.mScaleBitmap);
        } else if (this.mDispMode == 1) {
            matrix = new Matrix();
            float ratio2 = ((float) disp_height) / ((float) this.mOrgBitmap.getHeight());
            if (((float) this.mOrgBitmap.getWidth()) * ratio2 > 4000.0f) {
                ratio2 /= 2.0f;
                LogFilter.d(LOG_TAG, "ratio/harf");
            }
            ratio = ratio2;
            String str2 = LOG_TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("ratio:");
            stringBuilder2.append(ratio);
            LogFilter.d(str2, stringBuilder2.toString());
            matrix.postScale(ratio, ratio);
            this.mScaleBitmap = Bitmap.createBitmap(this.mOrgBitmap, 0, 0, this.mOrgBitmap.getWidth(), this.mOrgBitmap.getHeight(), matrix, true);
            this.mResultView.setImageBitmap(this.mScaleBitmap);
        }
    }

    private boolean decodePostViewData(MorphoPanoramaViewer image_stitcher, String file_path, int[] exif_orientation, ViewParam[] view_param) {
        MorphoPanoramaViewer morphoPanoramaViewer = image_stitcher;
        int[] width = new int[1];
        int[] height = new int[1];
        String str = file_path;
        getJpegImageSize(str, width, height);
        String str2 = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("the image width is ");
        stringBuilder.append(width[0]);
        Log.i(str2, stringBuilder.toString());
        int[] buffer_size = new int[1];
        PanoramaViewerInitParam param = new PanoramaViewerInitParam();
        param.mode = 1;
        param.render_mode = 1;
        param.input_width = width[0];
        param.input_height = height[0];
        param.format = "RGB888";
        int ret = morphoPanoramaViewer.initialize(param, buffer_size);
        if (ret != 0) {
            str2 = LOG_TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Morpho initialize failed");
            stringBuilder.append(ret);
            Log.i(str2, stringBuilder.toString());
            return false;
        }
        int[] gallery_data_size = new int[1];
        int[] gallery_data_size2 = gallery_data_size;
        int ret2 = morphoPanoramaViewer.decodeGalleyData(str, width, height, exif_orientation, gallery_data_size);
        String str3 = LOG_TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("the gallery data size is ");
        stringBuilder2.append(gallery_data_size2[0]);
        Log.i(str3, stringBuilder2.toString());
        if (ret2 != 0 || gallery_data_size2[0] <= 0) {
            image_stitcher.finish();
            return false;
        }
        this.mGalleryData = getGalleryData(morphoPanoramaViewer, width[0], height[0], gallery_data_size2[0]);
        str3 = LOG_TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("GalleryData is ");
        stringBuilder2.append(this.mGalleryData.cropped_area_image_height);
        Log.i(str3, stringBuilder2.toString());
        if (this.mGalleryData != null) {
            return true;
        }
        image_stitcher.finish();
        return false;
    }

    private GalleryData getGalleryData(MorphoPanoramaViewer image_stitcher, int w, int h, int gallery_data_size) {
        IOException e;
        XmlPullParser xmlPullParser;
        String str;
        XmlPullParserException e2;
        String TARGET_TAG = "Description";
        String NAME_FP_WIDTH = TinyPlanetFragment.CROPPED_AREA_FULL_PANO_WIDTH_PIXELS;
        String NAME_FP_HEIGHT = TinyPlanetFragment.CROPPED_AREA_FULL_PANO_HEIGHT_PIXELS;
        String NAME_CA_LEFT = TinyPlanetFragment.CROPPED_AREA_LEFT;
        String NAME_CA_TOP = TinyPlanetFragment.CROPPED_AREA_TOP;
        String NAME_CA_WIDTH = TinyPlanetFragment.CROPPED_AREA_IMAGE_WIDTH_PIXELS;
        String NAME_CA_HEIGHT = TinyPlanetFragment.CROPPED_AREA_IMAGE_HEIGHT_PIXELS;
        GalleryData data = null;
        byte[] gallery_data = new byte[gallery_data_size];
        int ret = image_stitcher.getGalleryDataOfAppSeg(gallery_data);
        String gdata_str = new String(gallery_data);
        int start_ix = gdata_str.indexOf("<");
        int i;
        String str2;
        String str3;
        String str4;
        String str5;
        String str6;
        if (ret != 0) {
            i = h;
            str2 = TARGET_TAG;
            str3 = NAME_FP_WIDTH;
            str4 = NAME_FP_HEIGHT;
            str5 = NAME_CA_LEFT;
            str6 = NAME_CA_TOP;
            NAME_FP_HEIGHT = w;
        } else if (start_ix == -1) {
            i = h;
            str2 = TARGET_TAG;
            str3 = NAME_FP_WIDTH;
            str4 = NAME_FP_HEIGHT;
            str5 = NAME_CA_LEFT;
            str6 = NAME_CA_TOP;
            NAME_FP_HEIGHT = w;
        } else {
            StringReader sr = new StringReader(gdata_str.substring(start_ix));
            XmlPullParser parser = Xml.newPullParser();
            StringReader sr2;
            try {
                double c_w;
                double c_h;
                parser.setInput(sr);
                int event_type = parser.getEventType();
                while (true) {
                    sr2 = sr;
                    int i2;
                    if (event_type == 1) {
                        i2 = event_type;
                        break;
                    }
                    if (event_type == 2) {
                        try {
                            if ("Description".equals(parser.getName())) {
                                data = new GalleryData();
                                data.full_pano_width = getAttrIntValue(parser, TinyPlanetFragment.CROPPED_AREA_FULL_PANO_WIDTH_PIXELS);
                                data.full_pano_height = getAttrIntValue(parser, TinyPlanetFragment.CROPPED_AREA_FULL_PANO_HEIGHT_PIXELS);
                                data.cropped_area_left = getAttrIntValue(parser, TinyPlanetFragment.CROPPED_AREA_LEFT);
                                data.cropped_area_top = getAttrIntValue(parser, TinyPlanetFragment.CROPPED_AREA_TOP);
                                data.cropped_area_image_width = getAttrIntValue(parser, TinyPlanetFragment.CROPPED_AREA_IMAGE_WIDTH_PIXELS);
                                data.cropped_area_image_height = getAttrIntValue(parser, TinyPlanetFragment.CROPPED_AREA_IMAGE_HEIGHT_PIXELS);
                                break;
                            }
                        } catch (IOException e3) {
                            e = e3;
                            i = h;
                            xmlPullParser = parser;
                            str4 = NAME_FP_HEIGHT;
                            str5 = NAME_CA_LEFT;
                            str6 = NAME_CA_TOP;
                            NAME_FP_HEIGHT = w;
                            e.printStackTrace();
                            str = LOG_TAG;
                            TARGET_TAG = new StringBuilder();
                            TARGET_TAG.append("the gallerydata is ");
                            TARGET_TAG.append(data.full_pano_height);
                            TARGET_TAG.append(" ");
                            TARGET_TAG.append(data.full_pano_width);
                            TARGET_TAG.append(" ");
                            TARGET_TAG.append(data.cropped_area_image_height);
                            TARGET_TAG.append(" ");
                            TARGET_TAG.append(data.cropped_area_image_width);
                            TARGET_TAG.append(" ");
                            TARGET_TAG.append(data.cropped_area_left);
                            TARGET_TAG.append(" ");
                            TARGET_TAG.append(data.cropped_area_top);
                            Log.i(str, TARGET_TAG.toString());
                            return data;
                        } catch (XmlPullParserException e4) {
                            e2 = e4;
                            i = h;
                            xmlPullParser = parser;
                            str4 = NAME_FP_HEIGHT;
                            str5 = NAME_CA_LEFT;
                            str6 = NAME_CA_TOP;
                            NAME_FP_HEIGHT = w;
                            e2.printStackTrace();
                            str = LOG_TAG;
                            TARGET_TAG = new StringBuilder();
                            TARGET_TAG.append("the gallerydata is ");
                            TARGET_TAG.append(data.full_pano_height);
                            TARGET_TAG.append(" ");
                            TARGET_TAG.append(data.full_pano_width);
                            TARGET_TAG.append(" ");
                            TARGET_TAG.append(data.cropped_area_image_height);
                            TARGET_TAG.append(" ");
                            TARGET_TAG.append(data.cropped_area_image_width);
                            TARGET_TAG.append(" ");
                            TARGET_TAG.append(data.cropped_area_left);
                            TARGET_TAG.append(" ");
                            TARGET_TAG.append(data.cropped_area_top);
                            Log.i(str, TARGET_TAG.toString());
                            return data;
                        }
                    }
                    i2 = event_type;
                    event_type = parser.next();
                    sr = sr2;
                }
                try {
                    c_w = (double) data.cropped_area_image_width;
                    c_h = (double) data.cropped_area_image_height;
                } catch (IOException e5) {
                    e = e5;
                    i = h;
                    xmlPullParser = parser;
                    str4 = NAME_FP_HEIGHT;
                    str5 = NAME_CA_LEFT;
                    str6 = NAME_CA_TOP;
                    NAME_FP_HEIGHT = w;
                    e.printStackTrace();
                    str = LOG_TAG;
                    TARGET_TAG = new StringBuilder();
                    TARGET_TAG.append("the gallerydata is ");
                    TARGET_TAG.append(data.full_pano_height);
                    TARGET_TAG.append(" ");
                    TARGET_TAG.append(data.full_pano_width);
                    TARGET_TAG.append(" ");
                    TARGET_TAG.append(data.cropped_area_image_height);
                    TARGET_TAG.append(" ");
                    TARGET_TAG.append(data.cropped_area_image_width);
                    TARGET_TAG.append(" ");
                    TARGET_TAG.append(data.cropped_area_left);
                    TARGET_TAG.append(" ");
                    TARGET_TAG.append(data.cropped_area_top);
                    Log.i(str, TARGET_TAG.toString());
                    return data;
                } catch (XmlPullParserException e6) {
                    e2 = e6;
                    i = h;
                    xmlPullParser = parser;
                    str4 = NAME_FP_HEIGHT;
                    str5 = NAME_CA_LEFT;
                    str6 = NAME_CA_TOP;
                    NAME_FP_HEIGHT = w;
                    e2.printStackTrace();
                    str = LOG_TAG;
                    TARGET_TAG = new StringBuilder();
                    TARGET_TAG.append("the gallerydata is ");
                    TARGET_TAG.append(data.full_pano_height);
                    TARGET_TAG.append(" ");
                    TARGET_TAG.append(data.full_pano_width);
                    TARGET_TAG.append(" ");
                    TARGET_TAG.append(data.cropped_area_image_height);
                    TARGET_TAG.append(" ");
                    TARGET_TAG.append(data.cropped_area_image_width);
                    TARGET_TAG.append(" ");
                    TARGET_TAG.append(data.cropped_area_left);
                    TARGET_TAG.append(" ");
                    TARGET_TAG.append(data.cropped_area_top);
                    Log.i(str, TARGET_TAG.toString());
                    return data;
                }
                try {
                    if (Math.abs(((((double) w) * c_h) / c_w) - ((double) h)) > 1.0d) {
                        return null;
                    }
                } catch (IOException e7) {
                    e = e7;
                    e.printStackTrace();
                    str = LOG_TAG;
                    TARGET_TAG = new StringBuilder();
                    TARGET_TAG.append("the gallerydata is ");
                    TARGET_TAG.append(data.full_pano_height);
                    TARGET_TAG.append(" ");
                    TARGET_TAG.append(data.full_pano_width);
                    TARGET_TAG.append(" ");
                    TARGET_TAG.append(data.cropped_area_image_height);
                    TARGET_TAG.append(" ");
                    TARGET_TAG.append(data.cropped_area_image_width);
                    TARGET_TAG.append(" ");
                    TARGET_TAG.append(data.cropped_area_left);
                    TARGET_TAG.append(" ");
                    TARGET_TAG.append(data.cropped_area_top);
                    Log.i(str, TARGET_TAG.toString());
                    return data;
                } catch (XmlPullParserException e8) {
                    e2 = e8;
                    e2.printStackTrace();
                    str = LOG_TAG;
                    TARGET_TAG = new StringBuilder();
                    TARGET_TAG.append("the gallerydata is ");
                    TARGET_TAG.append(data.full_pano_height);
                    TARGET_TAG.append(" ");
                    TARGET_TAG.append(data.full_pano_width);
                    TARGET_TAG.append(" ");
                    TARGET_TAG.append(data.cropped_area_image_height);
                    TARGET_TAG.append(" ");
                    TARGET_TAG.append(data.cropped_area_image_width);
                    TARGET_TAG.append(" ");
                    TARGET_TAG.append(data.cropped_area_left);
                    TARGET_TAG.append(" ");
                    TARGET_TAG.append(data.cropped_area_top);
                    Log.i(str, TARGET_TAG.toString());
                    return data;
                }
            } catch (IOException e9) {
                e = e9;
                i = h;
                sr2 = sr;
                xmlPullParser = parser;
                str4 = NAME_FP_HEIGHT;
                str5 = NAME_CA_LEFT;
                str6 = NAME_CA_TOP;
                NAME_FP_HEIGHT = w;
                e.printStackTrace();
                str = LOG_TAG;
                TARGET_TAG = new StringBuilder();
                TARGET_TAG.append("the gallerydata is ");
                TARGET_TAG.append(data.full_pano_height);
                TARGET_TAG.append(" ");
                TARGET_TAG.append(data.full_pano_width);
                TARGET_TAG.append(" ");
                TARGET_TAG.append(data.cropped_area_image_height);
                TARGET_TAG.append(" ");
                TARGET_TAG.append(data.cropped_area_image_width);
                TARGET_TAG.append(" ");
                TARGET_TAG.append(data.cropped_area_left);
                TARGET_TAG.append(" ");
                TARGET_TAG.append(data.cropped_area_top);
                Log.i(str, TARGET_TAG.toString());
                return data;
            } catch (XmlPullParserException e10) {
                e2 = e10;
                i = h;
                sr2 = sr;
                xmlPullParser = parser;
                str4 = NAME_FP_HEIGHT;
                str5 = NAME_CA_LEFT;
                str6 = NAME_CA_TOP;
                NAME_FP_HEIGHT = w;
                e2.printStackTrace();
                str = LOG_TAG;
                TARGET_TAG = new StringBuilder();
                TARGET_TAG.append("the gallerydata is ");
                TARGET_TAG.append(data.full_pano_height);
                TARGET_TAG.append(" ");
                TARGET_TAG.append(data.full_pano_width);
                TARGET_TAG.append(" ");
                TARGET_TAG.append(data.cropped_area_image_height);
                TARGET_TAG.append(" ");
                TARGET_TAG.append(data.cropped_area_image_width);
                TARGET_TAG.append(" ");
                TARGET_TAG.append(data.cropped_area_left);
                TARGET_TAG.append(" ");
                TARGET_TAG.append(data.cropped_area_top);
                Log.i(str, TARGET_TAG.toString());
                return data;
            }
            str = LOG_TAG;
            TARGET_TAG = new StringBuilder();
            TARGET_TAG.append("the gallerydata is ");
            TARGET_TAG.append(data.full_pano_height);
            TARGET_TAG.append(" ");
            TARGET_TAG.append(data.full_pano_width);
            TARGET_TAG.append(" ");
            TARGET_TAG.append(data.cropped_area_image_height);
            TARGET_TAG.append(" ");
            TARGET_TAG.append(data.cropped_area_image_width);
            TARGET_TAG.append(" ");
            TARGET_TAG.append(data.cropped_area_left);
            TARGET_TAG.append(" ");
            TARGET_TAG.append(data.cropped_area_top);
            Log.i(str, TARGET_TAG.toString());
            return data;
        }
        return null;
    }

    private int getAttrIntValue(XmlPullParser parser, String target_name) {
        String str = parser.getAttributeValue(null, target_name);
        String str2 = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(target_name);
        stringBuilder.append(":");
        stringBuilder.append(str);
        LogFilter.d(str2, stringBuilder.toString());
        return Integer.parseInt(str);
    }

    private static void getJpegImageSize(String filepath, int[] width, int[] height) {
        Options opt = new Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filepath, opt);
        width[0] = opt.outWidth;
        height[0] = opt.outHeight;
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("outWidth");
        stringBuilder.append(opt.outWidth);
        stringBuilder.append("outHeight");
        stringBuilder.append(opt.outHeight);
        Log.i(str, stringBuilder.toString());
    }

    /* Access modifiers changed, original: protected */
    public void onResume() {
        super.onResume();
        if (this.mGLPanoramaView != null) {
            this.mGLPanoramaView.onResume();
        }
        if (this.mGyroscope != null) {
            this.mSensorManager.registerListener(this.mSensorFusion, this.mGyroscope, 1);
        }
        if (this.mGyroscopeUncalibrated != null) {
            this.mSensorManager.registerListener(this.mSensorFusion, this.mGyroscopeUncalibrated, 1);
        }
        if (this.mRotationVector != null) {
            this.mSensorManager.registerListener(this.mSensorFusion, this.mRotationVector, 1);
        }
        if (this.mAccelerometer != null) {
            this.mSensorManager.registerListener(this.mSensorFusion, this.mAccelerometer, 2);
        }
        if (this.mMagneticField != null) {
            this.mSensorManager.registerListener(this.mSensorFusion, this.mMagneticField, 2);
        }
        int orientation = getResources().getConfiguration().orientation;
    }

    /* Access modifiers changed, original: protected */
    public void onPause() {
        super.onPause();
        if (this.mGLPanoramaView != null) {
            this.mGLPanoramaView.onPause();
        }
        if (this.mSensorManager != null) {
            this.mSensorManager.unregisterListener(this.mSensorFusion);
        }
        Editor editor = getSharedPreferences(PREFS_NAME, 0).edit();
        editor.putBoolean(SAVE_DATA_KEY_SENSOR_DRIVEN, this.mToggleButton_SensorDriven.isChecked());
        editor.commit();
    }

    public void onStop() {
        super.onStop();
    }

    public void onDestroy() {
        super.onDestroy();
        if (this.isFileSelect && !MorphoPanoramaViewer.isFinished()) {
            getApplication();
            if (this.mMorphoImageStitcher != null) {
                this.mMorphoImageStitcher.finish();
            }
        }
        if (this.mSensorDrivenTimer != null) {
            this.mSensorDrivenTimer.release();
        }
        if (this.mInertiaScrollTimer != null) {
            this.mInertiaScrollTimer.release();
        }
        if (this.mScaleBitmap != null) {
            this.mScaleBitmap.recycle();
        }
        if (this.mOrgBitmap != null) {
            this.mOrgBitmap.recycle();
        }
        if (this.mSensorFusion != null) {
            this.mSensorFusion.release();
        }
    }

    public void toggleHideBar() {
        getWindow().getDecorView().setSystemUiVisibility(((getWindow().getDecorView().getSystemUiVisibility() ^ 2) ^ 4) ^ 4096);
    }
}

package com.hmdglobal.app.camera.instantcapture;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import com.android.ex.camera2.portability.Size;
import com.android.ex.camera2.portability.debug.Log;
import com.android.ex.camera2.portability.debug.Log.Tag;
import com.android.external.plantform.ExtBuild;
import com.hmdglobal.app.camera.Exif;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.util.CameraUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class InstantViewImageActivity extends Activity implements SurfaceTextureListener {
    private static final int DOWN_SAMPLE_FACTOR = 4;
    private static final Tag TAG = new Tag("InstantActivity");
    private static final String VIEW_BACK = "persist.sys.view_back";
    private final int CHECK_BURST_PICTURE = 1;
    private ActionBar mActionBar;
    private ImageView mBurstImageView;
    private DialogFragment mConfirmAndDeleteFragment;
    private DecodeImageForReview mDecodeTaskForReview = null;
    private boolean mDelayGoToCamera = false;
    private boolean mForgroundActivity;
    private boolean mHomeReceiverRegistered = false;
    private InstantCaptureHelper mInstantCaptureHelper;
    private final BroadcastReceiver mKeyeventReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Tag access$000 = InstantViewImageActivity.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mKeyeventReceiver ");
            stringBuilder.append(action);
            Log.i(access$000, stringBuilder.toString());
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(action)) {
                String reason = intent.getStringExtra("reason");
                Tag access$0002 = InstantViewImageActivity.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("mKeyeventReceiver reason ");
                stringBuilder2.append(reason);
                Log.i(access$0002, stringBuilder2.toString());
                if ("homekey".equals(reason)) {
                    InstantViewImageActivity.this.finishActivity();
                }
            }
        }
    };
    OnUiUpdateListener mOnUiUpdateListener = new OnUiUpdateListener() {
        public void onUiUpdating(final int num) {
            InstantViewImageActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    Tag access$000 = InstantViewImageActivity.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onUiUpdate ");
                    stringBuilder.append(num);
                    Log.i(access$000, stringBuilder.toString());
                    InstantViewImageActivity.this.mActionBar.setTitle(InstantViewImageActivity.this.getResources().getQuantityString(R.plurals.instant_capture_photos, num, new Object[]{Integer.valueOf(num)}));
                    InstantViewImageActivity.this.mBurstImageView.setVisibility(4);
                }
            });
        }

        public void onUiUpdated(final int num) {
            InstantViewImageActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    Tag access$000 = InstantViewImageActivity.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onUiUpdated ");
                    stringBuilder.append(num);
                    stringBuilder.append(", ");
                    stringBuilder.append(InstantViewImageActivity.this.mDelayGoToCamera);
                    Log.i(access$000, stringBuilder.toString());
                    if (!InstantViewImageActivity.this.mInstantCaptureHelper.isSingleShot() && InstantViewImageActivity.this.mInstantCaptureHelper.hasSaveDone()) {
                        InstantViewImageActivity.this.mActionBar.setTitle(InstantViewImageActivity.this.getResources().getQuantityString(R.plurals.instant_capture_photos, num, new Object[]{Integer.valueOf(num)}));
                        InstantViewImageActivity.this.mBurstImageView.setVisibility(0);
                    }
                    if (InstantViewImageActivity.this.mDelayGoToCamera) {
                        InstantViewImageActivity.this.mDelayGoToCamera = false;
                        InstantViewImageActivity.this.startCameraActivity();
                    }
                    if (InstantViewImageActivity.this.mInstantCaptureHelper.isCaptureDone()) {
                        Log.i(InstantViewImageActivity.TAG, "onUiUpdated to force update view");
                        InstantViewImageActivity.this.showResultImageView(true);
                    }
                }
            });
        }
    };
    private int mOrientation;
    private ImageView mResultImageView;
    private TextureView mTextureView;

    public static class ConfirmAndDeleteDialogFragment extends DialogFragment {
        private ConfirmAndDeleteDialogFragment() {
        }

        public static ConfirmAndDeleteDialogFragment newInstance(int deleteNum) {
            ConfirmAndDeleteDialogFragment frag = new ConfirmAndDeleteDialogFragment();
            Bundle args = new Bundle();
            args.putInt("num", deleteNum);
            frag.setArguments(args);
            return frag;
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String info = getResources().getString(R.string.delete_selection);
            if (getArguments().getInt("num") > 1) {
                info = String.format(getActivity().getResources().getString(R.string.delete_selection_burst), new Object[]{Integer.valueOf(deleteNum)});
            }
            AlertDialog alertDialog = new Builder(getActivity()).setMessage(info).setPositiveButton(R.string.delete, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    android.util.Log.d("InstantView", "delete enter option KPI");
                    ((InstantViewImageActivity) ConfirmAndDeleteDialogFragment.this.getActivity()).executeDeletion();
                }
            }).setNegativeButton(17039360, new OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.dismiss();
                }
            }).create();
            alertDialog.show();
            alertDialog.getButton(-1).setTextColor(getActivity().getResources().getColor(R.color.dialog_button_font_color));
            alertDialog.getButton(-2).setTextColor(getActivity().getResources().getColor(R.color.dialog_button_font_color));
            return alertDialog;
        }
    }

    private class DecodeTask extends AsyncTask<Void, Void, Bitmap> {
        private final byte[] mData;
        private final boolean mMirror;
        private final int mOrientation;

        public DecodeTask(byte[] data, int orientation, boolean mirror) {
            this.mData = data;
            this.mOrientation = orientation;
            this.mMirror = mirror;
        }

        /* Access modifiers changed, original: protected|varargs */
        public Bitmap doInBackground(Void... params) {
            Bitmap bitmap = CameraUtil.downSample(this.mData, 4);
            if (this.mOrientation == 0 && !this.mMirror) {
                return bitmap;
            }
            Matrix m = new Matrix();
            if (this.mMirror) {
                m.setScale(-1.0f, 1.0f);
            }
            m.preRotate((float) this.mOrientation);
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, false);
        }
    }

    private class DeletionTask extends AsyncTask<ArrayList, Void, Void> {
        InstantViewImageActivity mContext;

        DeletionTask(InstantViewImageActivity context) {
            this.mContext = context;
        }

        /* Access modifiers changed, original: protected|varargs */
        public Void doInBackground(ArrayList... params) {
            int i = 0;
            ArrayList<Uri> uris = params[0];
            ContentResolver resolver = this.mContext.getContentResolver();
            while (i < uris.size()) {
                InstantViewImageActivity.this.delete((Uri) uris.get(i), resolver);
                i++;
            }
            return null;
        }

        /* Access modifiers changed, original: protected */
        public void onPostExecute(Void v) {
            if (!this.mContext.isFinishing()) {
                this.mContext.finish();
            }
        }
    }

    public interface OnUiUpdateListener {
        void onUiUpdated(int i);

        void onUiUpdating(int i);
    }

    private class DecodeImageForReview extends DecodeTask {
        public DecodeImageForReview(byte[] data, int orientation, boolean mirror) {
            super(data, orientation, mirror);
        }

        /* Access modifiers changed, original: protected */
        public void onPostExecute(Bitmap bitmap) {
            if (!isCancelled()) {
                InstantViewImageActivity.this.mResultImageView.setVisibility(0);
                InstantViewImageActivity.this.mResultImageView.setImageBitmap(bitmap);
                Log.i(InstantViewImageActivity.TAG, "instant capture kpi, bitmap display");
                InstantViewImageActivity.this.mDecodeTaskForReview = null;
            }
        }
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (this.mInstantCaptureHelper.gFirstFrame) {
            this.mInstantCaptureHelper.gFirstFrame = false;
            Log.i(TAG, "instant capture kpi, bitmap display in onSurfaceTextureUpdated");
        }
    }

    /* Access modifiers changed, original: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mForgroundActivity = true;
        this.mInstantCaptureHelper = InstantCaptureHelper.getInstance();
        if (!this.mInstantCaptureHelper.isInitialized() || this.mInstantCaptureHelper.getForbidStartViewImageActivity()) {
            Log.i(TAG, "onCreate should not start,finish");
            super.finish();
            return;
        }
        getWindow().requestFeature(8);
        ExtBuild.init();
        setContentView(R.layout.instant_view_image);
        this.mActionBar = getActionBar();
        this.mActionBar.setDisplayShowHomeEnabled(false);
        this.mActionBar.setTitle("");
        this.mActionBar.setDisplayShowTitleEnabled(true);
        this.mTextureView = (TextureView) findViewById(R.id.textureview);
        this.mTextureView.setSurfaceTextureListener(this);
        this.mBurstImageView = (ImageView) findViewById(R.id.icon_burst);
        this.mResultImageView = (ImageView) findViewById(R.id.result);
        Log.i(TAG, "onCreate");
        registerReceiver(this.mKeyeventReceiver, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
        this.mHomeReceiverRegistered = true;
        this.mInstantCaptureHelper.registerOnUiUpdateListener(this.mOnUiUpdateListener, this);
    }

    /* Access modifiers changed, original: protected */
    public void onNewIntent(Intent intent) {
        Log.i(TAG, "onNewIntent");
        this.mInstantCaptureHelper = InstantCaptureHelper.getInstance();
        this.mForgroundActivity = true;
        if (this.mInstantCaptureHelper.getForbidStartViewImageActivity()) {
            Log.i(TAG, "onNewIntent should not start,finish");
            finish();
            return;
        }
        resetView();
        super.onNewIntent(intent);
    }

    private void resetView() {
        this.mActionBar.setTitle("");
        this.mBurstImageView.setVisibility(4);
        this.mTextureView.setVisibility(4);
        this.mResultImageView.setVisibility(4);
        this.mDelayGoToCamera = false;
    }

    /* Access modifiers changed, original: protected */
    public void onStart() {
        Window window = getWindow();
        window.clearFlags(524288);
        window.clearFlags(6815744);
        window.addFlags(6815744);
        super.onStart();
        this.mOrientation = getWindowManager().getDefaultDisplay().getOrientation();
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onStart orientation = ");
        stringBuilder.append(this.mOrientation);
        Log.i(tag, stringBuilder.toString());
        if (this.mInstantCaptureHelper.isCaptureDone()) {
            this.mInstantCaptureHelper.gFirstFrame = false;
            Log.i(TAG, "onStart capture done");
            showResultImageView(true);
            return;
        }
        Log.i(TAG, "onStart capture not done");
        this.mInstantCaptureHelper.changeDisplayOrientation(this.mOrientation);
        setSurfaceTexture();
    }

    public void setSurfaceTexture() {
        if (!InstantCaptureHelper.USE_JPEG_AS_PICTURE_DISLAY) {
            runOnUiThread(new Runnable() {
                public void run() {
                    Log.i(InstantViewImageActivity.TAG, "setSurfaceTexture in ui thread ");
                    InstantViewImageActivity.this.mTextureView.setVisibility(0);
                    InstantViewImageActivity.this.mResultImageView.setVisibility(4);
                    SurfaceTexture st = InstantViewImageActivity.this.mInstantCaptureHelper.getSurfaceTexture();
                    if (st != null && (InstantViewImageActivity.this.mTextureView.getSurfaceTexture() == null || st != InstantViewImageActivity.this.mTextureView.getSurfaceTexture())) {
                        Log.i(InstantViewImageActivity.TAG, "setSurfaceTexture");
                        InstantViewImageActivity.this.mTextureView.setSurfaceTexture(InstantViewImageActivity.this.mInstantCaptureHelper.getSurfaceTexture());
                        InstantViewImageActivity.this.mInstantCaptureHelper.setSurfaceTextureAttached();
                    } else if (st == null) {
                        Log.i(InstantViewImageActivity.TAG, "setSurfaceTexture st == null");
                    } else {
                        Log.i(InstantViewImageActivity.TAG, "setSurfaceTexture mTextureView.getSurfaceTexture() != null");
                    }
                }
            });
        }
    }

    public void showResultImageView(final boolean done) {
        runOnUiThread(new Runnable() {
            public void run() {
                boolean singleShot = InstantViewImageActivity.this.mInstantCaptureHelper.isSingleShot();
                if (InstantViewImageActivity.this.mResultImageView.getVisibility() == 0 && singleShot) {
                    Log.i(InstantViewImageActivity.TAG, "the image already show, return");
                    return;
                }
                ArrayList<byte[]> pictureDatas = InstantViewImageActivity.this.mInstantCaptureHelper.getPictureDatas();
                int size = pictureDatas.size();
                Tag access$000 = InstantViewImageActivity.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("showResultImageView, ");
                stringBuilder.append(size);
                Log.e(access$000, stringBuilder.toString());
                boolean needShowBurstIcon = false;
                if (size == 0) {
                    InstantViewImageActivity.this.finish();
                    return;
                }
                if (size != 1) {
                    InstantViewImageActivity.this.mActionBar.setTitle(InstantViewImageActivity.this.getResources().getQuantityString(R.plurals.instant_capture_photos, size, new Object[]{Integer.valueOf(size)}));
                    needShowBurstIcon = true;
                } else if (singleShot) {
                    InstantViewImageActivity.this.mActionBar.setTitle("");
                } else {
                    InstantViewImageActivity.this.mActionBar.setTitle(InstantViewImageActivity.this.getResources().getQuantityString(R.plurals.instant_capture_photos, size, new Object[]{Integer.valueOf(size)}));
                    needShowBurstIcon = true;
                }
                InstantViewImageActivity.this.mTextureView.setVisibility(4);
                if (done && needShowBurstIcon) {
                    InstantViewImageActivity.this.mBurstImageView.setVisibility(0);
                }
                if (!singleShot || InstantViewImageActivity.this.mResultImageView.getVisibility() != 0) {
                    if (singleShot) {
                        InstantViewImageActivity.this.mResultImageView.setVisibility(0);
                        Options opts = new Options();
                        opts.inSampleSize = 4;
                        InstantViewImageActivity.this.mResultImageView.setImageBitmap(BitmapFactory.decodeByteArray((byte[]) pictureDatas.get(0), 0, ((byte[]) pictureDatas.get(0)).length, opts));
                        Log.e(InstantViewImageActivity.TAG, "instant capture kpi, bitmap display in showResultImageView");
                        return;
                    }
                    InstantViewImageActivity.this.mDecodeTaskForReview = new DecodeImageForReview((byte[]) pictureDatas.get(size - 1), Exif.getOrientation(Exif.getExif((byte[]) pictureDatas.get(size - 1))), false);
                    InstantViewImageActivity.this.mDecodeTaskForReview.execute(new Void[0]);
                }
            }
        });
    }

    /* Access modifiers changed, original: protected */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("BURSTSHOTACTIVITY RESULT: ");
        stringBuilder.append(requestCode);
        stringBuilder.append(Size.DELIMITER);
        stringBuilder.append(resultCode);
        Log.i(tag, stringBuilder.toString());
        if (this.mDelayGoToCamera) {
            startCameraActivity();
            this.mDelayGoToCamera = false;
        }
    }

    /* Access modifiers changed, original: protected */
    public void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        boolean screenOn = this.mInstantCaptureHelper.isScreenOn();
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onPause: screen:");
        stringBuilder.append(screenOn);
        stringBuilder.append(" capture done:");
        stringBuilder.append(this.mInstantCaptureHelper.isCaptureDone());
        Log.i(tag, stringBuilder.toString());
        if (!screenOn && this.mInstantCaptureHelper.isCaptureDone()) {
            finishActivity();
        }
    }

    /* Access modifiers changed, original: protected */
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        this.mInstantCaptureHelper.unRegisterOnUiUpdateListener();
        if (this.mHomeReceiverRegistered) {
            this.mHomeReceiverRegistered = false;
            unregisterReceiver(this.mKeyeventReceiver);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.rapid_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void onBackPressed() {
        Log.i(TAG, "onBackPressed");
        startCameraActivity();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == 16908332) {
            Log.i(TAG, "home pressed");
            startCameraActivity();
        } else if (itemId == R.id.action_delete) {
            android.util.Log.d("InstantView", "delete option KPI");
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            Fragment prev = getFragmentManager().findFragmentByTag("dialog");
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);
            if (this.mConfirmAndDeleteFragment != null) {
                this.mConfirmAndDeleteFragment.dismiss();
            }
            this.mConfirmAndDeleteFragment = ConfirmAndDeleteDialogFragment.newInstance(this.mInstantCaptureHelper.getBurstCount());
            this.mConfirmAndDeleteFragment.show(getFragmentManager(), "dialog");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startCameraActivity() {
        ArrayList<Uri> uris = this.mInstantCaptureHelper.getPictureUris();
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startCameraActivity ");
        stringBuilder.append(this.mInstantCaptureHelper.getBurstCount());
        stringBuilder.append(", ");
        stringBuilder.append(uris);
        Log.i(tag, stringBuilder.toString());
        if (uris.size() <= 0 || uris.size() < this.mInstantCaptureHelper.getBurstCount()) {
            this.mDelayGoToCamera = true;
            return;
        }
        this.mInstantCaptureHelper.startCameraActivity(this, uris);
        finishActivity();
    }

    public void finish() {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("finish ");
        stringBuilder.append(this.mForgroundActivity);
        Log.i(tag, stringBuilder.toString());
        if (this.mForgroundActivity) {
            this.mForgroundActivity = false;
            this.mDelayGoToCamera = false;
            moveTaskToBack(true);
        }
    }

    public void executeDeletion() {
        new DeletionTask(this).execute(new ArrayList[]{this.mInstantCaptureHelper.getPictureUris()});
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mOrientation = getWindowManager().getDefaultDisplay().getOrientation();
        if (this.mInstantCaptureHelper.isCaptureDone()) {
            showResultImageView(true);
        } else {
            this.mInstantCaptureHelper.changeDisplayOrientation(this.mOrientation);
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("configChange: ");
        stringBuilder.append(this.mOrientation);
        Log.e(tag, stringBuilder.toString());
    }

    public int getOrientation() {
        return this.mOrientation;
    }

    private void delete(Uri uri, ContentResolver resolver) {
        Cursor c = resolver.query(uri, new String[]{"_data"}, null, null, null);
        String path = null;
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    path = c.getString(0);
                }
            } catch (Throwable th) {
                if (c != null) {
                    c.close();
                }
            }
        }
        if (c != null) {
            c.close();
        }
        resolver.delete(uri, null, null);
        if (!TextUtils.isEmpty(path)) {
            new File(path).delete();
        }
    }

    private final void finishActivity() {
        if (this.mConfirmAndDeleteFragment != null && this.mConfirmAndDeleteFragment.isAdded()) {
            this.mConfirmAndDeleteFragment.dismissAllowingStateLoss();
            this.mConfirmAndDeleteFragment = null;
        }
        finish();
    }

    public void openBurst(View view) {
        String GALLERY_PACKAGE_NAME = "com.android.gallery3d";
        String BURSTSHOT_ACTIVITY_CLASS = "com.android.gallery3d.app.BurstShotActivity";
        String BURSTSHOT_ARRAYLIST = "burstshot-arraylist";
        String KEY_LOCKED_CAMERA = "is-camera-review";
        List<Uri> uris = this.mInstantCaptureHelper.getPictureUris();
        ArrayList<String> idArrays = new ArrayList();
        for (Uri uri : uris) {
            idArrays.add(uri.getLastPathSegment());
        }
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.gallery3d", "com.android.gallery3d.app.BurstShotActivity"));
        Bundle data = new Bundle();
        data.putStringArrayList("burstshot-arraylist", idArrays);
        data.putBoolean("is-camera-review", true);
        intent.putExtras(data);
        try {
            startActivityForResult(intent, 1);
            this.mDelayGoToCamera = true;
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            intent = new Intent("android.intent.action.VIEW");
            intent.setDataAndType((Uri) uris.get(0), "image/*");
            startActivity(intent);
            finish();
        }
    }
}

package com.hmdglobal.app.camera.rapidcapture;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import com.hmdglobal.app.camera.CameraActivity;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.util.ImageLoader;
import java.io.File;

public class RapidViewImageActivity extends Activity {
    private static final String TAG = "RapidViewImageActivity";
    public static boolean mIsRunning = false;
    private ImageView image;
    private ActionBar mActionBar;
    private DialogFragment mConfirmAndDeleteFragment;
    private LoadBitmapTask mLoadBitmapTask = null;
    private final BroadcastReceiver mShutdownReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.d(RapidViewImageActivity.TAG, "screen off");
            if (RapidViewImageActivity.this.mConfirmAndDeleteFragment != null && RapidViewImageActivity.this.mConfirmAndDeleteFragment.isVisible()) {
                RapidViewImageActivity.this.mConfirmAndDeleteFragment.dismiss();
                RapidViewImageActivity.this.mConfirmAndDeleteFragment = null;
            }
            RapidViewImageActivity.this.finish();
        }
    };
    private Uri mSourceUri = null;

    public static class ConfirmAndDeleteDialogFragment extends DialogFragment {
        public static ConfirmAndDeleteDialogFragment newInstance() {
            return new ConfirmAndDeleteDialogFragment();
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog alertDialog = new Builder(getActivity()).setMessage(R.string.delete_selection).setPositiveButton(R.string.delete, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    ((RapidViewImageActivity) ConfirmAndDeleteDialogFragment.this.getActivity()).executeDeletion();
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

    private class DeletionTask extends AsyncTask<Uri, Void, Void> {
        RapidViewImageActivity mContext;

        DeletionTask(RapidViewImageActivity context) {
            this.mContext = context;
        }

        /* Access modifiers changed, original: protected|varargs */
        public Void doInBackground(Uri... params) {
            Uri uri = params[0];
            ContentResolver resolver = this.mContext.getContentResolver();
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
            return null;
        }

        /* Access modifiers changed, original: protected */
        public void onPostExecute(Void v) {
            if (!this.mContext.isFinishing()) {
                this.mContext.finish();
            }
        }
    }

    private class LoadBitmapTask extends AsyncTask<Uri, Void, Bitmap> {
        int mBitmapSize;
        Context mContext;
        int mOrientation = 0;
        Rect mOriginalBounds = new Rect();

        public LoadBitmapTask() {
            this.mBitmapSize = RapidViewImageActivity.this.getScreenImageSize();
            this.mContext = RapidViewImageActivity.this.getApplicationContext();
        }

        /* Access modifiers changed, original: protected|varargs */
        public Bitmap doInBackground(Uri... params) {
            Uri uri = params[0];
            Bitmap bmap = ImageLoader.loadConstrainedBitmap(uri, this.mContext, this.mBitmapSize, this.mOriginalBounds, false);
            this.mOrientation = ImageLoader.getMetadataRotation(this.mContext, uri);
            return bmap;
        }

        /* Access modifiers changed, original: protected */
        public void onPostExecute(Bitmap result) {
            RapidViewImageActivity.this.doneLoadBitmap(result, new RectF(this.mOriginalBounds), this.mOrientation);
        }
    }

    /* Access modifiers changed, original: protected */
    public void onCreate(Bundle savedInstanceState) {
        Window window = getWindow();
        window.requestFeature(8);
        LayoutParams params = window.getAttributes();
        params.flags |= 134217728;
        params.flags |= 1024;
        params.flags |= 524288;
        window.setAttributes(params);
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        setContentView(R.layout.rapid_view_image);
        this.image = (ImageView) findViewById(R.id.image);
        this.mActionBar = getActionBar();
        this.mActionBar.setDisplayShowHomeEnabled(false);
        registerReceiver(this.mShutdownReceiver, new IntentFilter("android.intent.action.SCREEN_OFF"));
        if (intent.getData() != null) {
            this.mSourceUri = intent.getData();
            startLoadBitmap(this.mSourceUri);
        }
        RapidCaptureHelper.getInstance().acquireScreenWakeLock(this);
        mIsRunning = true;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.rapid_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /* Access modifiers changed, original: protected */
    public void onDestroy() {
        super.onDestroy();
        mIsRunning = false;
        Log.d(TAG, "onDestroy");
        unregisterReceiver(this.mShutdownReceiver);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == 16908332) {
            Intent intent;
            if (((KeyguardManager) getSystemService("keyguard")).isKeyguardLocked()) {
                intent = new Intent(CameraActivity.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE);
            } else {
                intent = new Intent("android.media.action.STILL_IMAGE_CAMERA");
            }
            intent.setClass(this, CameraActivity.class);
            intent.addFlags(32768);
            try {
                startActivity(intent);
                return true;
            } catch (ActivityNotFoundException e) {
                finish();
            }
        } else if (itemId == R.id.action_delete) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            Fragment prev = getFragmentManager().findFragmentByTag("dialog");
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);
            if (this.mConfirmAndDeleteFragment != null) {
                this.mConfirmAndDeleteFragment.dismiss();
            }
            this.mConfirmAndDeleteFragment = ConfirmAndDeleteDialogFragment.newInstance();
            this.mConfirmAndDeleteFragment.show(getFragmentManager(), "dialog");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void executeDeletion() {
        new DeletionTask(this).execute(new Uri[]{this.mSourceUri});
    }

    private void startLoadBitmap(Uri uri) {
        if (uri != null) {
            this.mLoadBitmapTask = new LoadBitmapTask();
            this.mLoadBitmapTask.execute(new Uri[]{uri});
        }
    }

    private int getScreenImageSize() {
        DisplayMetrics outMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
        return Math.max(outMetrics.heightPixels, outMetrics.widthPixels);
    }

    private void doneLoadBitmap(Bitmap bitmap, RectF bounds, int orientation) {
        if (bitmap != null && bitmap.getWidth() != 0 && bitmap.getHeight() != 0) {
            this.image.setImageBitmap(bitmap);
        }
    }
}

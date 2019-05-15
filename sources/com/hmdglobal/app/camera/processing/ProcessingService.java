package com.hmdglobal.app.camera.processing;

import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.support.v4.content.LocalBroadcastManager;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.app.CameraApp;
import com.hmdglobal.app.camera.app.CameraServices;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.session.CaptureSession;
import com.hmdglobal.app.camera.session.CaptureSession.ProgressListener;
import com.hmdglobal.app.camera.session.CaptureSessionManager;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ProcessingService extends Service implements ProgressListener {
    public static final String ACTION_PAUSE_PROCESSING_SERVICE = "com.hmdglobal.app.camera.processing.PAUSE";
    public static final String ACTION_RESUME_PROCESSING_SERVICE = "com.hmdglobal.app.camera.processing.RESUME";
    private static final int CAMERA_NOTIFICATION_ID = 2;
    private static final Tag TAG = new Tag("ProcessingService");
    private static final int THREAD_PRIORITY = -4;
    private ProcessingTask mCurrentTask;
    private Builder mNotificationBuilder;
    private NotificationManager mNotificationManager;
    private volatile boolean mPaused = false;
    private ProcessingServiceManager mProcessingServiceManager;
    private Thread mProcessingThread;
    private final ServiceController mServiceController = new ServiceController();
    private CaptureSessionManager mSessionManager;
    private final Lock mSuspendStatusLock = new ReentrantLock();
    private WakeLock mWakeLock;

    public class ServiceController extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == ProcessingService.ACTION_PAUSE_PROCESSING_SERVICE) {
                ProcessingService.this.pause();
            } else if (intent.getAction() == ProcessingService.ACTION_RESUME_PROCESSING_SERVICE) {
                ProcessingService.this.resume();
            }
        }
    }

    public void onCreate() {
        this.mProcessingServiceManager = ProcessingServiceManager.getInstance();
        this.mSessionManager = getServices().getCaptureSessionManager();
        this.mWakeLock = ((PowerManager) getSystemService("power")).newWakeLock(1, TAG.toString());
        this.mWakeLock.acquire();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_PAUSE_PROCESSING_SERVICE);
        intentFilter.addAction(ACTION_RESUME_PROCESSING_SERVICE);
        LocalBroadcastManager.getInstance(this).registerReceiver(this.mServiceController, intentFilter);
        this.mNotificationBuilder = createInProgressNotificationBuilder();
        this.mNotificationManager = (NotificationManager) getSystemService("notification");
    }

    public void onDestroy() {
        Log.d(TAG, "Shutting down");
        if (this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(this.mServiceController);
        stopForeground(true);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting in foreground.");
        startForeground(2, this.mNotificationBuilder.build());
        asyncProcessAllTasksAndShutdown();
        return 1;
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    private void pause() {
        Log.d(TAG, "Pausing");
        try {
            this.mSuspendStatusLock.lock();
            this.mPaused = true;
            if (this.mCurrentTask != null) {
                this.mCurrentTask.suspend();
            }
            this.mSuspendStatusLock.unlock();
        } catch (Throwable th) {
            this.mSuspendStatusLock.unlock();
        }
    }

    private void resume() {
        Log.d(TAG, "Resuming");
        try {
            this.mSuspendStatusLock.lock();
            this.mPaused = false;
            if (this.mCurrentTask != null) {
                this.mCurrentTask.resume();
            }
            this.mSuspendStatusLock.unlock();
        } catch (Throwable th) {
            this.mSuspendStatusLock.unlock();
        }
    }

    private void asyncProcessAllTasksAndShutdown() {
        if (this.mProcessingThread == null) {
            this.mProcessingThread = new Thread("CameraProcessingThread") {
                public void run() {
                    Process.setThreadPriority(-4);
                    while (true) {
                        ProcessingTask popNextSession = ProcessingService.this.mProcessingServiceManager.popNextSession();
                        ProcessingTask task = popNextSession;
                        if (popNextSession != null) {
                            ProcessingService.this.mCurrentTask = task;
                            try {
                                ProcessingService.this.mSuspendStatusLock.lock();
                                if (ProcessingService.this.mPaused) {
                                    ProcessingService.this.mCurrentTask.suspend();
                                }
                                ProcessingService.this.mSuspendStatusLock.unlock();
                                ProcessingService.this.processAndNotify(task);
                            } catch (Throwable th) {
                                ProcessingService.this.mSuspendStatusLock.unlock();
                            }
                        } else {
                            ProcessingService.this.stopSelf();
                            return;
                        }
                    }
                }
            };
            this.mProcessingThread.start();
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void processAndNotify(ProcessingTask task) {
        if (task == null) {
            Log.e(TAG, "Reference to ProcessingTask is null");
            return;
        }
        CaptureSession session = task.getSession();
        if (session == null) {
            session = this.mSessionManager.createNewSession(task.getName(), 0, task.getLocation());
        }
        resetNotification();
        session.addProgressListener(this);
        System.gc();
        Log.d(TAG, "Processing start");
        task.process(this, getServices(), session);
        Log.d(TAG, "Processing done");
    }

    private void resetNotification() {
        this.mNotificationBuilder.setContentText("…").setProgress(100, 0, false);
        postNotification();
    }

    private CameraServices getServices() {
        return (CameraApp) getApplication();
    }

    private void postNotification() {
        this.mNotificationManager.notify(2, this.mNotificationBuilder.build());
    }

    private Builder createInProgressNotificationBuilder() {
        return new Builder(this).setSmallIcon(R.drawable.ic_notification).setWhen(System.currentTimeMillis()).setOngoing(true).setContentTitle(getText(R.string.app_name));
    }

    public void onProgressChanged(int progress) {
        this.mNotificationBuilder.setProgress(100, progress, false);
        postNotification();
    }

    public void onStatusMessageChanged(CharSequence message) {
        this.mNotificationBuilder.setContentText(message);
        postNotification();
    }
}

package com.hyper.castwave.data.screenmirror;

import static android.app.PendingIntent.FLAG_IMMUTABLE;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.Icon;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;


import com.hyper.castwave.data.R;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ScreenRecorder extends Service {

    private static final String TAG = "ScreenRecorder";
    private static final int ORIENTATION_0 = 0;
    private static final ReentrantReadWriteLock frameLock = new ReentrantReadWriteLock();
    private static final String appName = "screen.mirroring";
    private static final String NOTIFICATIONS_RECORDING_CHANNEL = "notifications";
    private static final int NOTIFICATION_RECORDING_ID = 7023;
    private static final int NOTIFICATION_RECORDING_FINISHED_ID = 7024;
    public static int fps = 40;
    public static String ACTION_START = appName + ".START_RECORDING";
    public static String ACTION_STOP = appName + ".STOP_RECORDING";
    public static String ACTION_MORE = appName + ".MORE";
    public static String ACTION_ACTIVITY_FINISHED_FILE = appName + ".ACTIVITY_FINISHED_FILE";
    private static ScreenRecorder INSTANCE;
    private static byte[] jpegFrame;
    private final IBinder recordingBinder = new RecordingBinder();
    public boolean runningService = false;
    ImageReader imageReader;
    private int displayWidth, displayHeight = 0;
    private Intent data;
    private int result;
    private long timeStart = 0;
    private NotificationManager recordingNotificationManager;
    private MediaProjection mediaProjection;
    private RecordingCallback recordingCallback = null;
    private int currentOrientation = ORIENTATION_0;
    private long timeUpdate = 0;
    private String oldTime = "";

    public static ScreenRecorder getInstance() {
        return INSTANCE;
    }

    public static byte[] getJpegFrame() {
        try {
            frameLock.readLock().lock();
            return jpegFrame;
        } finally {
            frameLock.readLock().unlock();
        }
    }

    private static void setJpegFrame(ByteArrayOutputStream stream) {
        try {
            frameLock.writeLock().lock();
            jpegFrame = stream.toByteArray();
        } finally {
            frameLock.writeLock().unlock();
        }
    }

    public static String getCurrentTimeStamp() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("HH:mm:ss");
        Date now = new Date();
        String strDate = sdfDate.format(now);
        return strDate;
    }

    public IBinder getRecordingBinder() {
        return recordingBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return recordingBinder;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        INSTANCE = this;
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(ACTION_START)) {
                try {
                    actionStart();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (intent.getAction().equals(ACTION_STOP)) {
                try {
                    screenRecordingStop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (intent.getAction().equals(ACTION_MORE)) {
                Log.e(TAG, "onStartCommand: action_more");
            }
        } else {
            if (!runningService) {
                Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show();
                stopSelf();
            }
        }

        return START_STICKY;
    }

    @SuppressLint("WrongConstant")
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void actionStart() {
        recordingNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (recordingNotificationManager.getNotificationChannel(NOTIFICATIONS_RECORDING_CHANNEL) == null) {
            NotificationChannel recordingNotifications = new NotificationChannel(NOTIFICATIONS_RECORDING_CHANNEL, getString(R.string.notifications_channel), NotificationManager.IMPORTANCE_HIGH);
            recordingNotifications.enableLights(true);
            recordingNotifications.setLightColor(Color.RED);
            recordingNotifications.setShowBadge(true);
            recordingNotifications.enableVibration(true);

            recordingNotificationManager.createNotificationChannel(recordingNotifications);
        }
        runningService = true;
        screenRecordingStart();
    }

    public void actionConnect(RecordingCallback callback) {
        recordingCallback = callback;
    }

    public void actionDisconnect() {
        recordingCallback = null;
    }

    @SuppressLint("WrongConstant")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void screenRecordingStart() {

        if (recordingCallback != null) {
            recordingCallback.onRecordingStart();
        }

        timeStart = SystemClock.elapsedRealtime();
        if (mediaProjection == null) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mediaProjection == null) {
                        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                        mediaProjection = projectionManager.getMediaProjection(result, data);
                        screenRecordingStart();
                    }
                }
            }, 500);
        }
        if (mediaProjection != null) {
            displayWidth = Resources.getSystem().getDisplayMetrics().heightPixels;
            displayHeight = Resources.getSystem().getDisplayMetrics().widthPixels;
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int density = metrics.densityDpi;
            int flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;

            imageReader = ImageReader.newInstance(displayWidth, displayHeight, PixelFormat.RGBA_8888, 2);
            imageReader.setOnImageAvailableListener(new ImageAvailableListener(), null);
            mediaProjection.registerCallback(new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    super.onStop();
                    screenRecordingStop();
                }

                @Override
                public void onCapturedContentResize(int width, int height) {
                    super.onCapturedContentResize(width, height);
                }

                @Override
                public void onCapturedContentVisibilityChanged(boolean isVisible) {
                    super.onCapturedContentVisibilityChanged(isVisible);
                }
            }, null);

            mediaProjection.createVirtualDisplay("ScreenStreamVirtualDisplay", displayWidth, displayHeight, density, flags, imageReader == null ? null : imageReader.getSurface(), null, null);

        }
        Intent stopRecordIntent = new Intent(this, ScreenRecorder.class);
        stopRecordIntent.setAction(ACTION_STOP);
        PendingIntent stopRecordActionIntent = PendingIntent.getService(this, 0, stopRecordIntent, FLAG_IMMUTABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Icon stopIcon = Icon.createWithResource(this, R.drawable.ic_arrange_grid);
            Icon recordingIcon = Icon.createWithResource(this, R.drawable.ic_arrange_grid);
            Icon recordingIconLarge = Icon.createWithResource(this, R.drawable.ic_arrange_grid);
            Notification.Action.Builder stopRecordAction = new Notification.Action.Builder(stopIcon, getString(R.string.notifications_stop), stopRecordActionIntent);
            Intent pauseRecordIntent = new Intent(this, ScreenRecorder.class);
            pauseRecordIntent.setAction(ACTION_MORE);
            Notification.Builder notification = new Notification.Builder(this, NOTIFICATIONS_RECORDING_CHANNEL).setContentTitle(getString(R.string.recording_started_title)).setContentText(getString(R.string.recording_started_text)).setTicker(getString(R.string.recording_started_text)).setSmallIcon(recordingIcon).setLargeIcon(recordingIconLarge).setUsesChronometer(true).setWhen(System.currentTimeMillis() - (SystemClock.elapsedRealtime() - timeStart)).setOngoing(true).addAction(stopRecordAction.build());

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_RECORDING_ID, notification.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
                } else {
                    startForeground(NOTIFICATION_RECORDING_ID, notification.build());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            NotificationCompat.Action.Builder xxx = new NotificationCompat.Action.Builder(R.drawable.ic_play_media_player, getString(R.string.notifications_stop), stopRecordActionIntent);
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_play_media_player);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this).setContentTitle(getString(R.string.recording_started_title)).setContentText(getString(R.string.recording_started_text)).setTicker(getString(R.string.recording_started_text)).setSmallIcon(R.drawable.ic_play_media_player).setLargeIcon(bm).setUsesChronometer(true).setWhen(System.currentTimeMillis() - (SystemClock.elapsedRealtime() - timeStart)).setOngoing(true).addAction(xxx.build());


            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("notify_001", "Channel human readable title", NotificationManager.IMPORTANCE_DEFAULT);
                mNotificationManager.createNotificationChannel(channel);
            }

            mNotificationManager.notify(0, mBuilder.build());
        }

    }

    private void screenRecordingStop() {
        timeStart = 0;
        runningService = false;

        if (recordingCallback != null) {
            recordingCallback.onRecordingStop();
        }

        Intent openFolderIntent = new Intent(this, ScreenRecorder.class);

        openFolderIntent.setAction(ACTION_ACTIVITY_FINISHED_FILE);

        PendingIntent openFolderActionIntent = PendingIntent.getService(this, 0, openFolderIntent, PendingIntent.FLAG_IMMUTABLE);

        Icon finishedIcon = null;
        Icon finishedIconLarge = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            finishedIcon = Icon.createWithResource(this, R.drawable.ic_arrange_grid);
            finishedIconLarge = Icon.createWithResource(this, R.drawable.ic_arrange_grid);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder finishedNotification = new Notification.Builder(this, NOTIFICATIONS_RECORDING_CHANNEL).setContentTitle(getString(R.string.recording_finished_title)).setContentText(getString(R.string.recording_finished_text)).setContentIntent(openFolderActionIntent).setSmallIcon(finishedIcon).setLargeIcon(finishedIconLarge).setAutoCancel(true);

            recordingNotificationManager.notify(NOTIFICATION_RECORDING_FINISHED_ID, finishedNotification.build());
            stopForeground(Service.STOP_FOREGROUND_REMOVE);
        } else {
            NotificationManagerCompat.from(this).cancelAll();
            stopService(new Intent(this, ScreenRecorder.class));
        }

        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }

        stopSelf();
    }

    public interface RecordingCallback {
        void onRecordingStart();

        void onRecordingStop();
    }

    public class RecordingBinder extends Binder {
        public boolean isStarted() {
            return ScreenRecorder.this.runningService;
        }

        public void stopService() {
            try {
                ScreenRecorder.this.screenRecordingStop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void setConnect(RecordingCallback callback) {
            try {
                ScreenRecorder.this.actionConnect(callback);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        public void setStart() {
            try {
                ScreenRecorder.this.actionStart();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void setDisconnect() {
            ScreenRecorder.this.actionDisconnect();
        }

        public void setPreStart(int resultCode, Intent resultData) {
            ScreenRecorder.this.result = resultCode;
            ScreenRecorder.this.data = resultData;
        }

    }

    @SuppressLint("NewApi")
    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @SuppressLint("WrongConstant")
        @Override
        public void onImageAvailable(ImageReader reader) {
            if (!runningService) {
                return;
            }
            Image image = null;
            FileOutputStream fos = null;
            Bitmap bitmap = null;

            ByteArrayOutputStream stream = null;

            try {
                image = imageReader.acquireLatestImage();
                if (image != null) {
                    long currentTimeMillis = System.currentTimeMillis();
                    if (currentTimeMillis - timeUpdate < fps) {
                        image.close();
                        return;
                    }
                    timeUpdate = currentTimeMillis;


                    Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                    int screenOrientation = display.getRotation();


                    if (screenOrientation != currentOrientation) {

                        DisplayMetrics metrics = getResources().getDisplayMetrics();
                        int density = metrics.densityDpi;
                        int flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR;

                        imageReader = ImageReader.newInstance(displayWidth, displayHeight, PixelFormat.RGBA_8888, 2);
                        imageReader.setOnImageAvailableListener(new ImageAvailableListener(), null);

                        mediaProjection.createVirtualDisplay("ScreenStreamVirtualDisplay", displayWidth, displayHeight, density, flags, imageReader == null ? null : imageReader.getSurface(), null, null);

                    }
                    currentOrientation = screenOrientation;


                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * displayWidth;
                    image.close();
                    ScreenMirrorUtils.Companion.getOutputStream(buffer, displayWidth + rowPadding / pixelStride, displayHeight, stream1 -> {
                        setJpegFrame(stream1);
                        String time = getCurrentTimeStamp();
                        if (!oldTime.equals(time)) {
                            oldTime = time;
                        }
                    });

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

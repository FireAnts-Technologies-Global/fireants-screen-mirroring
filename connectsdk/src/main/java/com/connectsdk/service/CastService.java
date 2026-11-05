package com.connectsdk.service;

import android.net.Uri;
import android.os.Bundle;

import com.connectsdk.core.ImageInfo;
import com.connectsdk.core.MediaInfo;
import com.connectsdk.core.SubtitleInfo;
import com.connectsdk.core.Util;
import com.connectsdk.discovery.DiscoveryFilter;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.service.capability.CapabilityMethods;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.VolumeControl;
import com.connectsdk.service.capability.WebAppLauncher;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;
import com.connectsdk.service.command.URLServiceSubscription;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;
import com.connectsdk.service.sessions.CastWebAppSession;
import com.connectsdk.service.sessions.LaunchSession;
import com.connectsdk.service.sessions.WebAppSession;
import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.LaunchOptions;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaTrack;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.images.WebImage;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

public class CastService extends DeviceService implements MediaPlayer, MediaControl, VolumeControl, WebAppLauncher {
    public static final String CAST_SERVICE_MUTE_SUBSCRIPTION_NAME = "mute";
    public static final String CAST_SERVICE_VOLUME_SUBSCRIPTION_NAME = "volume";
    public static final String ID = "Chromecast";
    public static final String PLAY_STATE = "PlayState";
    private static final long MEDIA_TRACK_ID = 1;
    static String applicationID = "CC1AD845";
    CastDevice castDevice;
    CopyOnWriteArraySet<ConnectionListener> commandQueue = new CopyOnWriteArraySet<>();
    String currentAppId;
    boolean currentMuteStatus;
    float currentVolumeLevel;
    String launchingAppId;
    GoogleApiClient mApiClient;
    CastClient mCastClient = new CastClient();
    CastListener mCastClientListener = new CastListener();
    ConnectionCallbacks mConnectionCallbacks = new ConnectionCallbacks();
    ConnectionFailedListener mConnectionFailedListener = new ConnectionFailedListener();
    RemoteMediaPlayer mMediaPlayer;
    boolean mWaitingForReconnect = false;
    Map<String, CastWebAppSession> sessions = new HashMap();
    List<URLServiceSubscription<?>> subscriptions = new ArrayList();

    public CastService(ServiceDescription serviceDescription, ServiceConfig serviceConfig) {
        super(serviceDescription, serviceConfig);
    }

    public static DiscoveryFilter discoveryFilter() {
        return new DiscoveryFilter(ID, ID);
    }

    public static String getApplicationID() {
        return applicationID;
    }

    public static void setApplicationID(String str) {
        applicationID = str;
    }

    public MediaControl getMediaControl() {
        return this;
    }

    public MediaPlayer getMediaPlayer() {
        return this;
    }

    public String getServiceName() {
        return ID;
    }

    public VolumeControl getVolumeControl() {
        return this;
    }

    public WebAppLauncher getWebAppLauncher() {
        return this;
    }

    public boolean isConnectable() {
        return true;
    }

    public CapabilityPriorityLevel getPriorityLevel(Class<? extends CapabilityMethods> cls) {
        if (cls.equals(MediaPlayer.class)) {
            return getMediaPlayerCapabilityLevel();
        }
        if (cls.equals(MediaControl.class)) {
            return getMediaControlCapabilityLevel();
        }
        if (cls.equals(VolumeControl.class)) {
            return getVolumeControlCapabilityLevel();
        }
        if (cls.equals(WebAppLauncher.class)) {
            return getWebAppLauncherCapabilityLevel();
        }
        return CapabilityPriorityLevel.NOT_SUPPORTED;
    }

    public void connect() {
        if (this.castDevice == null) {
            this.castDevice = (CastDevice) getServiceDescription().getDevice();
        }
        if (this.mApiClient == null) {
            this.mApiClient = createApiClient();
        }
        if (!this.mApiClient.isConnecting() && !this.mApiClient.isConnected()) {
            this.mApiClient.connect();
        }
    }

    /* access modifiers changed from: protected */
    public GoogleApiClient createApiClient() {
        return new GoogleApiClient.Builder(DiscoveryManager.getInstance().getContext()).addApi(Cast.API, Cast.CastOptions.builder(this.castDevice, this.mCastClientListener).build()).addConnectionCallbacks(this.mConnectionCallbacks).addOnConnectionFailedListener(this.mConnectionFailedListener).build();
    }

    public void disconnect() {
        this.mWaitingForReconnect = false;
        detachMediaPlayer();
        if (!this.commandQueue.isEmpty()) {
            this.commandQueue.clear();
        }
        GoogleApiClient googleApiClient = this.mApiClient;
        if (googleApiClient != null && googleApiClient.isConnected()) {
            try {
                this.mCastClient.leaveApplication(this.mApiClient);
            } catch (CastClientException unused) {
                String str = Util.T;
            }
            this.mApiClient.disconnect();
        }
        if (this.connected) {
            Util.runOnUI(new Runnable() {
                public void run() {
                    if (CastService.this.getListener() != null) {
                        CastService.this.getListener().onDisconnect(CastService.this, null);
                    }
                }
            });
        }
        this.connected = false;
        this.mApiClient = null;
    }

    public CapabilityPriorityLevel getMediaControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    public void play(final ResponseListener<Object> responseListener) {
        runCommand(new ConnectionListener() {
            public void onConnected() {
                try {
                    CastService castService = CastService.this;
                    castService.mMediaPlayer.play(castService.mApiClient);
                    Util.postSuccess(responseListener, null);
                } catch (Exception unused) {
                    Util.postError(responseListener, new ServiceCommandError(0, "Unable to play", null));
                }
            }
        });
    }

    public void pause(final ResponseListener<Object> responseListener) {
        runCommand(new ConnectionListener() {
            public void onConnected() {
                try {
                    CastService castService = CastService.this;
                    castService.mMediaPlayer.pause(castService.mApiClient);
                    Util.postSuccess(responseListener, null);
                } catch (Exception unused) {
                    Util.postError(responseListener, new ServiceCommandError(0, "Unable to pause", null));
                }
            }
        });
    }

    public void stop(final ResponseListener<Object> responseListener) {
        runCommand(new ConnectionListener() {
            public void onConnected() {
                try {
                    CastService castService = CastService.this;
                    castService.mMediaPlayer.stop(castService.mApiClient);
                    Util.postSuccess(responseListener, null);
                } catch (Exception unused) {
                    Util.postError(responseListener, new ServiceCommandError(0, "Unable to stop", null));
                }
            }
        });
    }

    public void rewind(ResponseListener<Object> responseListener) {
        Util.postError(responseListener, ServiceCommandError.notSupported());
    }

    public void fastForward(ResponseListener<Object> responseListener) {
        Util.postError(responseListener, ServiceCommandError.notSupported());
    }

    public void previous(ResponseListener<Object> responseListener) {
        Util.postError(responseListener, ServiceCommandError.notSupported());
    }

    public void next(ResponseListener<Object> responseListener) {
        Util.postError(responseListener, ServiceCommandError.notSupported());
    }

    public void seek(final long j, final ResponseListener<Object> responseListener) {
        RemoteMediaPlayer remoteMediaPlayer = this.mMediaPlayer;
        if (remoteMediaPlayer == null || remoteMediaPlayer.getMediaStatus() == null) {
            Util.postError(responseListener, new ServiceCommandError(0, "There is no media currently available", null));
        } else {
            runCommand(new ConnectionListener() {
                public void onConnected() {
                    try {
                        CastService castService = CastService.this;
                        castService.mMediaPlayer.seek(castService.mApiClient, j, 0).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                            public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                                Status status = mediaChannelResult.getStatus();
                                if (status.isSuccess()) {
                                    Util.postSuccess(responseListener, null);
                                } else {
                                    Util.postError(responseListener, new ServiceCommandError(status.getStatusCode(), status.getStatusMessage(), status));
                                }
                            }
                        });
                    } catch (Exception unused) {
                        Util.postError(responseListener, new ServiceCommandError(0, "Unable to seek", null));
                    }
                }
            });
        }
    }

    public void getDuration(DurationListener durationListener) {
        RemoteMediaPlayer remoteMediaPlayer = this.mMediaPlayer;
        if (remoteMediaPlayer == null || remoteMediaPlayer.getMediaStatus() == null) {
            Util.postError(durationListener, new ServiceCommandError(0, "There is no media currently available", null));
        } else {
            Util.postSuccess(durationListener, Long.valueOf(this.mMediaPlayer.getStreamDuration()));
        }
    }

    public void getPosition(PositionListener positionListener) {
        RemoteMediaPlayer remoteMediaPlayer = this.mMediaPlayer;
        if (remoteMediaPlayer == null || remoteMediaPlayer.getMediaStatus() == null) {
            Util.postError(positionListener, new ServiceCommandError(0, "There is no media currently available", null));
        } else {
            Util.postSuccess(positionListener, Long.valueOf(this.mMediaPlayer.getApproximateStreamPosition()));
        }
    }

    public CapabilityPriorityLevel getMediaPlayerCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    public void getMediaInfo(MediaInfoListener mediaInfoListener) {
        ArrayList arrayList;
        String str;
        String str2;
        RemoteMediaPlayer remoteMediaPlayer = this.mMediaPlayer;
        if (remoteMediaPlayer != null) {
            ArrayList arrayList2 = null;
            if (remoteMediaPlayer.getMediaInfo() != null) {
                String contentId = this.mMediaPlayer.getMediaInfo().getContentId();
                String contentType = this.mMediaPlayer.getMediaInfo().getContentType();
                MediaMetadata metadata = this.mMediaPlayer.getMediaInfo().getMetadata();
                if (metadata != null) {
                    String string = metadata.getString(MediaMetadata.KEY_TITLE);
                    String string2 = metadata.getString(MediaMetadata.KEY_SUBTITLE);
                    if (metadata.getImages() != null && metadata.getImages().size() > 0) {
                        String uri = metadata.getImages().get(0).getUrl().toString();
                        arrayList2 = new ArrayList();
                        arrayList2.add(new ImageInfo(uri));
                    }
                    arrayList = arrayList2;
                    str = string2;
                    str2 = string;
                } else {
                    str2 = null;
                    str = null;
                    arrayList = null;
                }
                MediaInfo mediaInfo = new MediaInfo(contentId, contentType, str2, str, arrayList);
                Util.postSuccess(mediaInfoListener, mediaInfo);
            } else {
                Util.postError(mediaInfoListener, new ServiceCommandError(0, "Media Info is null", null));
            }
        }
    }

    public ServiceSubscription<MediaInfoListener> subscribeMediaInfo(MediaInfoListener mediaInfoListener) {
        URLServiceSubscription uRLServiceSubscription = new URLServiceSubscription(this, "info", null, null);
        uRLServiceSubscription.addListener(mediaInfoListener);
        addSubscription(uRLServiceSubscription);
        return uRLServiceSubscription;
    }

    public void attachMediaPlayer() {
        if (this.mMediaPlayer == null) {
            mMediaPlayer = createMediaPlayer();
            mMediaPlayer.setOnStatusUpdatedListener(new RemoteMediaPlayer.OnStatusUpdatedListener() {
                public void onStatusUpdated() {
                    if (CastService.this.subscriptions.size() > 0) {
                        for (URLServiceSubscription next : CastService.this.subscriptions) {
                            if (next.getTarget().equalsIgnoreCase(CastService.PLAY_STATE)) {
                                for (int i = 0; i < next.getListeners().size(); i++) {
                                    ResponseListener responseListener = (ResponseListener) next.getListeners().get(i);
                                    RemoteMediaPlayer remoteMediaPlayer = CastService.this.mMediaPlayer;
                                    if (!(remoteMediaPlayer == null || remoteMediaPlayer.getMediaStatus() == null)) {
                                        Util.postSuccess(responseListener, PlayStateStatus.convertPlayerStateToPlayStateStatus(CastService.this.mMediaPlayer.getMediaStatus().getPlayerState()));
                                    }
                                }
                            }
                        }
                    }
                }
            });
            this.mMediaPlayer.setOnMetadataUpdatedListener(new RemoteMediaPlayer.OnMetadataUpdatedListener() {
                public void onMetadataUpdated() {
                    if (CastService.this.subscriptions.size() > 0) {
                        for (URLServiceSubscription next : CastService.this.subscriptions) {
                            if (next.getTarget().equalsIgnoreCase("info")) {
                                for (int i = 0; i < next.getListeners().size(); i++) {
                                    CastService.this.getMediaInfo((MediaInfoListener) next.getListeners().get(i));
                                }
                            }
                        }
                    }
                }
            });
            GoogleApiClient googleApiClient = this.mApiClient;
            if (googleApiClient != null) {
                try {
                    this.mCastClient.setMessageReceivedCallbacks(googleApiClient, this.mMediaPlayer.getNamespace(), this.mMediaPlayer);
                } catch (Exception unused) {
                    String str = Util.T;
                }
            }
        }
    }

    public RemoteMediaPlayer createMediaPlayer() {
        return new RemoteMediaPlayer();
    }

    public void detachMediaPlayer() {
        RemoteMediaPlayer remoteMediaPlayer = this.mMediaPlayer;
        if (remoteMediaPlayer != null) {
            GoogleApiClient googleApiClient = this.mApiClient;
            if (googleApiClient != null) {
                try {
                    this.mCastClient.removeMessageReceivedCallbacks(googleApiClient, remoteMediaPlayer.getNamespace());
                } catch (CastClientException unused) {
                    String str = Util.T;
                }
            }
        }
        this.mMediaPlayer = null;
    }

    public void displayImage(String str, String str2, String str3, String str4, String str5, LaunchListener launchListener) {
        MediaMetadata mediaMetadata = new MediaMetadata(4);
        mediaMetadata.putString(MediaMetadata.KEY_TITLE, str3);
        mediaMetadata.putString(MediaMetadata.KEY_SUBTITLE, str4);
        if (str5 != null) {
            mediaMetadata.addImage(new WebImage(Uri.parse(str5), 100, 100));
        }
        playMedia(new com.google.android.gms.cast.MediaInfo.Builder(str).setContentType(str2).setStreamType(0).setMetadata(mediaMetadata).setStreamDuration(0).setCustomData(null).build(), applicationID, launchListener);
    }

    public void displayImage(MediaInfo mediaInfo, LaunchListener launchListener) {
        String str;
        String str2;
        String str3;
        String str4;
        String str5;
        String str6 = null;
        if (mediaInfo != null) {
            String url = mediaInfo.getUrl();
            String mimeType = mediaInfo.getMimeType();
            String title = mediaInfo.getTitle();
            String description = mediaInfo.getDescription();
            if (mediaInfo.getImages() != null && mediaInfo.getImages().size() > 0) {
                str6 = mediaInfo.getImages().get(0).getUrl();
            }
            str = str6;
            str5 = url;
            str4 = mimeType;
            str3 = title;
            str2 = description;
        } else {
            str5 = null;
            str4 = null;
            str3 = null;
            str2 = null;
            str = null;
        }
        displayImage(str5, str4, str3, str2, str, launchListener);
    }

    private void playMedia(String mediaURL, SubtitleInfo subtitleInfo, String str2, String str3, String str4, String str5, boolean z, LaunchListener launchListener) {
        MediaMetadata mediaMetadata = new MediaMetadata(1);
        mediaMetadata.putString(MediaMetadata.KEY_TITLE, str3);
        mediaMetadata.putString(MediaMetadata.KEY_SUBTITLE, str4);
        if (str5 != null) {
            mediaMetadata.addImage(new WebImage(Uri.parse(str5), 100, 100));
        }
        ArrayList arrayList = new ArrayList();
        if (subtitleInfo != null) {
            arrayList.add(new MediaTrack.Builder(1, 1).setName(subtitleInfo.getLabel()).setSubtype(1).setContentId(subtitleInfo.getUrl()).setContentType(subtitleInfo.getMimeType()).setLanguage(subtitleInfo.getLanguage()).build());
        }
        playMedia(new com.google.android.gms.cast.MediaInfo.Builder(mediaURL).setContentType(str2).setStreamType(1).setMetadata(mediaMetadata).setStreamDuration(1000).setCustomData(null).setMediaTracks(arrayList).build(), applicationID, launchListener);
    }

    public void playMedia(String str, String str2, String str3, String str4, String str5, boolean z, LaunchListener launchListener) {
        playMedia(str, null, str2, str3, str4, str5, z, launchListener);
    }

    public void playMedia(MediaInfo mediaInfo, boolean z, LaunchListener launchListener) {
        String str;
        String str2;
        String str3;
        String str4;
        SubtitleInfo subtitleInfo;
        String str5;
        try {
            this.mCastClient.getApplicationStatus(this.mApiClient);
        } catch (CastClientException e2) {
            Util.postError(launchListener, new ServiceCommandError(e2.getMessage()));
        }
        String str6 = null;
        if (mediaInfo != null) {
            String url = mediaInfo.getUrl();
            SubtitleInfo subtitleInfo2 = mediaInfo.getSubtitleInfo();
            String mimeType = mediaInfo.getMimeType();
            String title = mediaInfo.getTitle();
            String description = mediaInfo.getDescription();
            if (mediaInfo.getImages() != null && mediaInfo.getImages().size() > 0) {
                str6 = mediaInfo.getImages().get(0).getUrl();
            }
            str = str6;
            str2 = description;
            str3 = title;
            str4 = mimeType;
            subtitleInfo = subtitleInfo2;
            str5 = url;
        } else {
            str5 = null;
            subtitleInfo = null;
            str4 = null;
            str3 = null;
            str2 = null;
            str = null;
        }
        playMedia(str5, subtitleInfo, str4, str3, str2, str, z, launchListener);
    }

    private void playMedia(final com.google.android.gms.cast.MediaInfo mediaInfo, final String str, final LaunchListener launchListener) {
        final ApplicationConnectionResultCallback applicationConnectionResultCallback = new ApplicationConnectionResultCallback(new LaunchWebAppListener() {
            public void onSuccess(final WebAppSession webAppSession) {
                CastService.this.runCommand(new ConnectionListener() {
                    public void onConnected() {
                        CastService.this.loadMedia(mediaInfo, webAppSession, launchListener);
                    }
                });
            }

            public void onFailure(ServiceCommandError serviceCommandError) {
                Util.postError(launchListener, serviceCommandError);
            }
        });
        this.launchingAppId = str;
        runCommand(new ConnectionListener() {
            public void onConnected() {
                boolean z;
                try {
                    CastService castService = CastService.this;
                    if (castService.mCastClient.getApplicationStatus(castService.mApiClient) != null) {
                        if (str.equals(CastService.this.currentAppId)) {
                            z = false;
                            LaunchOptions launchOptions = new LaunchOptions();
                            launchOptions.setRelaunchIfRunning(z);
                            CastService castService2 = CastService.this;
                            castService2.mCastClient.launchApplication(castService2.mApiClient, str, launchOptions).setResultCallback(applicationConnectionResultCallback);
                        }
                    }
                    z = true;
                    LaunchOptions launchOptions2 = new LaunchOptions();
                    launchOptions2.setRelaunchIfRunning(z);
                    CastService castService22 = CastService.this;
                    castService22.mCastClient.launchApplication(castService22.mApiClient, str, launchOptions2).setResultCallback(applicationConnectionResultCallback);
                } catch (Exception unused) {
                    Util.postError(launchListener, new ServiceCommandError(0, "Unable to launch", null));
                }
            }
        });
    }

    public void loadMedia(com.google.android.gms.cast.MediaInfo mediaInfo, final WebAppSession webAppSession, final LaunchListener launchListener) {
        try {
            this.mMediaPlayer.load(this.mApiClient, mediaInfo, true).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                    Status status = mediaChannelResult.getStatus();
                    if (status.isSuccess()) {
                        webAppSession.launchSession.setSessionType(LaunchSession.LaunchSessionType.Media);
                        CastService castService = CastService.this;
                        castService.mMediaPlayer.setActiveMediaTracks(castService.mApiClient, new long[]{1});
                        Util.postSuccess(launchListener, new MediaLaunchObject(webAppSession.launchSession, CastService.this));
                        return;
                    }
                    Util.postError(launchListener, new ServiceCommandError(status.getStatusCode(), status.getStatusMessage(), status));
                }
            });
        } catch (Exception unused) {
            Util.postError(launchListener, new ServiceCommandError(0, "Unable to load", null));
        }
    }

    public void closeMedia(final LaunchSession launchSession, final ResponseListener<Object> responseListener) {
        runCommand(new ConnectionListener() {
            public void onConnected() {
                try {
                    CastService castService = CastService.this;
                    castService.mCastClient.stopApplication(castService.mApiClient, launchSession.getSessionId()).setResultCallback(new ResultCallback<Status>() {
                        public void onResult(Status status) {
                            if (status.isSuccess()) {
                                Util.postSuccess(responseListener, status);
                            } else {
                                Util.postError(responseListener, new ServiceCommandError(status.getStatusCode(), status.getStatusMessage(), status));
                            }
                        }
                    });
                } catch (Exception unused) {
                    Util.postError(responseListener, new ServiceCommandError(0, "Unable to stop", null));
                }
            }
        });
    }

    public CapabilityPriorityLevel getWebAppLauncherCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    public void launchWebApp(String str, WebAppSession.LaunchListener launchListener) {
        launchWebApp(str, true, launchListener);
    }

    public void launchWebApp(String str, boolean z, final WebAppSession.LaunchListener launchListener) {
        this.launchingAppId = str;
        final LaunchWebAppListener r5 = new LaunchWebAppListener() {
            public void onSuccess(WebAppSession webAppSession) {
                Util.postSuccess(launchListener, webAppSession);
            }

            public void onFailure(ServiceCommandError serviceCommandError) {
                Util.postError(launchListener, serviceCommandError);
            }
        };
        final boolean z2 = z;
        final String str2 = str;
        final WebAppSession.LaunchListener launchListener2 = launchListener;
        ConnectionListener r0 = new ConnectionListener() {
            public void onConnected() {
                try {
                    if (!z2) {
                        CastService castService = CastService.this;
                        castService.mCastClient.joinApplication(castService.mApiClient).setResultCallback(new ResultCallback<Cast.ApplicationConnectionResult>() {
                            public void onResult(Cast.ApplicationConnectionResult applicationConnectionResult) {
                                if (!applicationConnectionResult.getStatus().isSuccess() || applicationConnectionResult.getApplicationMetadata() == null || applicationConnectionResult.getApplicationMetadata().getName() == null || !applicationConnectionResult.getApplicationMetadata().getApplicationId().equals(str2)) {
                                    LaunchOptions launchOptions = new LaunchOptions();
                                    launchOptions.setRelaunchIfRunning(true);
                                    try {
                                        CastService castService = CastService.this;
                                        PendingResult<Cast.ApplicationConnectionResult> launchApplication = castService.mCastClient.launchApplication(castService.mApiClient, str2, launchOptions);
                                        launchApplication.setResultCallback(new ApplicationConnectionResultCallback(r5));
                                    } catch (Exception unused) {
                                        Util.postError(launchListener2, new ServiceCommandError(0, "Unable to launch", null));
                                    }
                                } else {
                                    ApplicationMetadata applicationMetadata = applicationConnectionResult.getApplicationMetadata();
                                    CastService.this.currentAppId = applicationMetadata.getApplicationId();
                                    LaunchSession launchSessionForAppId = LaunchSession.launchSessionForAppId(applicationMetadata.getApplicationId());
                                    launchSessionForAppId.setAppName(applicationMetadata.getName());
                                    launchSessionForAppId.setSessionId(applicationConnectionResult.getSessionId());
                                    launchSessionForAppId.setSessionType(LaunchSession.LaunchSessionType.WebApp);
                                    launchSessionForAppId.setService(CastService.this);
                                    CastWebAppSession castWebAppSession = new CastWebAppSession(launchSessionForAppId, CastService.this);
                                    castWebAppSession.setMetadata(applicationMetadata);
                                    CastService.this.sessions.put(applicationMetadata.getApplicationId(), castWebAppSession);
                                    Util.postSuccess(launchListener2, castWebAppSession);
                                }
                            }
                        });
                        return;
                    }
                    LaunchOptions launchOptions = new LaunchOptions();
                    launchOptions.setRelaunchIfRunning(z2);
                    CastService castService2 = CastService.this;
                    castService2.mCastClient.launchApplication(castService2.mApiClient, str2, launchOptions).setResultCallback(new ApplicationConnectionResultCallback(r5));
                } catch (Exception unused) {
                    Util.postError(launchListener2, new ServiceCommandError(0, "Unable to launch", null));
                }
            }
        };
        runCommand(r0);
    }

    public void launchWebApp(String str, JSONObject jSONObject, WebAppSession.LaunchListener launchListener) {
        Util.postError(launchListener, ServiceCommandError.notSupported());
    }

    public void launchWebApp(String str, JSONObject jSONObject, boolean z, WebAppSession.LaunchListener launchListener) {
        Util.postError(launchListener, ServiceCommandError.notSupported());
    }

    public void requestStatus(final ResponseListener<Object> responseListener) {
        try {
            this.mMediaPlayer.requestStatus(this.mApiClient).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                    if (mediaChannelResult.getStatus().isSuccess()) {
                        Util.postSuccess(responseListener, mediaChannelResult);
                    } else {
                        Util.postError(responseListener, new ServiceCommandError(0, "Failed to request status", mediaChannelResult));
                    }
                }
            });
        } catch (Exception unused) {
            Util.postError(responseListener, new ServiceCommandError(0, "There is no media currently available", null));
        }
    }

    public void joinApplication(final ResponseListener<Object> responseListener) {
        runCommand(new ConnectionListener() {
            public void onConnected() {
                try {
                    CastService castService = CastService.this;
                    castService.mCastClient.joinApplication(castService.mApiClient).setResultCallback(new ResultCallback<Cast.ApplicationConnectionResult>() {
                        public void onResult(Cast.ApplicationConnectionResult applicationConnectionResult) {
                            if (applicationConnectionResult.getStatus().isSuccess()) {
                                if (!(applicationConnectionResult.getApplicationMetadata() == null || applicationConnectionResult.getApplicationMetadata().getName() == null || applicationConnectionResult.getApplicationMetadata().getName().equals("Backdrop"))) {
                                    CastService castService = CastService.this;
                                    RemoteMediaPlayer remoteMediaPlayer = castService.mMediaPlayer;
                                    if (remoteMediaPlayer != null) {
                                        GoogleApiClient googleApiClient = castService.mApiClient;
                                        if (googleApiClient != null) {
                                            remoteMediaPlayer.requestStatus(googleApiClient).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                                                public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                                                    Util.postSuccess(responseListener, mediaChannelResult);
                                                }
                                            });
                                            return;
                                        }
                                    }
                                }
                                Util.postSuccess(responseListener, applicationConnectionResult);
                                return;
                            }
                            Util.postError(responseListener, new ServiceCommandError(0, "Failed to join application", applicationConnectionResult));
                        }
                    });
                } catch (Exception unused) {
                    Util.postError(responseListener, new ServiceCommandError(0, "Unable to join", null));
                }
            }
        });
    }

    public void joinWebApp(final LaunchSession launchSession, final WebAppSession.LaunchListener launchListener) {
        final ApplicationConnectionResultCallback applicationConnectionResultCallback = new ApplicationConnectionResultCallback(new LaunchWebAppListener() {
            public void onSuccess(final WebAppSession webAppSession) {
                webAppSession.connect(new ResponseListener<Object>() {
                    public void onSuccess(Object obj) {
                        CastService.this.requestStatus(new ResponseListener<Object>() {
                            public void onSuccess(Object obj) {
                                Util.postSuccess(launchListener, webAppSession);
                            }

                            public void onError(ServiceCommandError serviceCommandError) {
                                Util.postSuccess(launchListener, webAppSession);
                            }
                        });
                    }

                    public void onError(ServiceCommandError serviceCommandError) {
                        Util.postError(launchListener, serviceCommandError);
                    }
                });
            }

            public void onFailure(ServiceCommandError serviceCommandError) {
                Util.postError(launchListener, serviceCommandError);
            }
        });
        this.launchingAppId = launchSession.getAppId();
        runCommand(new ConnectionListener() {
            public void onConnected() {
                try {
                    CastService castService = CastService.this;
                    castService.mCastClient.joinApplication(castService.mApiClient, launchSession.getAppId()).setResultCallback(applicationConnectionResultCallback);
                } catch (Exception unused) {
                    Util.postError(launchListener, new ServiceCommandError(0, "Unable to join", null));
                }
            }
        });
    }

    public void joinWebApp(String str, WebAppSession.LaunchListener launchListener) {
        LaunchSession launchSessionForAppId = LaunchSession.launchSessionForAppId(str);
        launchSessionForAppId.setSessionType(LaunchSession.LaunchSessionType.WebApp);
        launchSessionForAppId.setService(this);
        joinWebApp(launchSessionForAppId, launchListener);
    }

    public void closeWebApp(LaunchSession launchSession, final ResponseListener<Object> responseListener) {
        runCommand(new ConnectionListener() {
            public void onConnected() {
                try {
                    CastService castService = CastService.this;
                    castService.mCastClient.stopApplication(castService.mApiClient).setResultCallback(new ResultCallback<Status>() {
                        public void onResult(Status status) {
                            if (status.isSuccess()) {
                                Util.postSuccess(responseListener, null);
                            } else {
                                Util.postError(responseListener, new ServiceCommandError(status.getStatusCode(), status.getStatusMessage(), status));
                            }
                        }
                    });
                } catch (Exception unused) {
                    Util.postError(responseListener, new ServiceCommandError(0, "Unable to stop", null));
                }
            }
        });
    }

    public void pinWebApp(String str, ResponseListener<Object> responseListener) {
        Util.postError(responseListener, ServiceCommandError.notSupported());
    }

    public void unPinWebApp(String str, ResponseListener<Object> responseListener) {
        Util.postError(responseListener, ServiceCommandError.notSupported());
    }

    public void isWebAppPinned(String str, WebAppSession.WebAppPinStatusListener webAppPinStatusListener) {
        Util.postError(webAppPinStatusListener, ServiceCommandError.notSupported());
    }

    public ServiceSubscription<WebAppSession.WebAppPinStatusListener> subscribeIsWebAppPinned(String str, WebAppSession.WebAppPinStatusListener webAppPinStatusListener) {
        Util.postError(webAppPinStatusListener, ServiceCommandError.notSupported());
        return null;
    }

    public CapabilityPriorityLevel getVolumeControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    public void volumeUp(final ResponseListener<Object> responseListener) {
        getVolume(new VolumeListener() {
            public void onSuccess(Float f) {
                if (((double) f.floatValue()) >= 1.0d) {
                    Util.postSuccess(responseListener, null);
                    return;
                }
                float floatValue = (float) (((double) f.floatValue()) + 0.01d);
                if (((double) floatValue) > 1.0d) {
                    floatValue = 1.0f;
                }
                CastService.this.setVolume(floatValue, responseListener);
                Util.postSuccess(responseListener, null);
            }

            public void onError(ServiceCommandError serviceCommandError) {
                Util.postError(responseListener, serviceCommandError);
            }
        });
    }

    public void volumeDown(final ResponseListener<Object> responseListener) {
        getVolume(new VolumeListener() {
            public void onSuccess(Float f) {
                if (((double) f.floatValue()) <= 0.0d) {
                    Util.postSuccess(responseListener, null);
                    return;
                }
                float floatValue = (float) (((double) f.floatValue()) - 0.01d);
                if (((double) floatValue) < 0.0d) {
                    floatValue = 0.0f;
                }
                CastService.this.setVolume(floatValue, responseListener);
                Util.postSuccess(responseListener, null);
            }

            public void onError(ServiceCommandError serviceCommandError) {
                Util.postError(responseListener, serviceCommandError);
            }
        });
    }

    public void setVolume(final float f, final ResponseListener<Object> responseListener) {
        runCommand(new ConnectionListener() {
            public void onConnected() {
                try {
                    CastService castService = CastService.this;
                    castService.mCastClient.setVolume(castService.mApiClient, f);
                    Util.postSuccess(responseListener, null);
                } catch (Exception unused) {
                    Util.postError(responseListener, new ServiceCommandError(0, "setting volume level failed", null));
                }
            }
        });
    }

    public void getVolume(VolumeListener volumeListener) {
        Util.postSuccess(volumeListener, Float.valueOf(this.currentVolumeLevel));
    }

    public void setMute(final boolean z, final ResponseListener<Object> responseListener) {
        runCommand(new ConnectionListener() {
            public void onConnected() {
                try {
                    CastService castService = CastService.this;
                    castService.mCastClient.setMute(castService.mApiClient, z);
                    Util.postSuccess(responseListener, null);
                } catch (Exception unused) {
                    Util.postError(responseListener, new ServiceCommandError(0, "setting mute status failed", null));
                }
            }
        });
    }

    public void getMute(MuteListener muteListener) {
        Util.postSuccess(muteListener, Boolean.valueOf(this.currentMuteStatus));
    }

    public ServiceSubscription<VolumeListener> subscribeVolume(VolumeListener volumeListener) {
        URLServiceSubscription uRLServiceSubscription = new URLServiceSubscription(this, CAST_SERVICE_VOLUME_SUBSCRIPTION_NAME, null, null);
        uRLServiceSubscription.addListener(volumeListener);
        addSubscription(uRLServiceSubscription);
        return uRLServiceSubscription;
    }

    public ServiceSubscription<MuteListener> subscribeMute(MuteListener muteListener) {
        URLServiceSubscription uRLServiceSubscription = new URLServiceSubscription(this, "mute", null, null);
        uRLServiceSubscription.addListener(muteListener);
        addSubscription(uRLServiceSubscription);
        return uRLServiceSubscription;
    }

    public void updateCapabilities() {
        ArrayList arrayList = new ArrayList();
        Collections.addAll(arrayList, MediaPlayer.Capabilities);
        arrayList.add(MediaPlayer.Subtitle_WebVTT);
        Collections.addAll(arrayList, VolumeControl.Capabilities);
        arrayList.add(MediaControl.Play);
        arrayList.add(MediaControl.Pause);
        arrayList.add(MediaControl.Stop);
        arrayList.add(MediaControl.Duration);
        arrayList.add(MediaControl.Seek);
        arrayList.add(MediaControl.Position);
        arrayList.add(MediaControl.PlayState);
        arrayList.add(MediaControl.PlayState_Subscribe);
        arrayList.add(WebAppLauncher.Launch);
        arrayList.add(WebAppLauncher.Message_Send);
        arrayList.add(WebAppLauncher.Message_Receive);
        arrayList.add(WebAppLauncher.Message_Send_JSON);
        arrayList.add(WebAppLauncher.Message_Receive_JSON);
        arrayList.add(WebAppLauncher.Connect);
        arrayList.add(WebAppLauncher.Disconnect);
        arrayList.add(WebAppLauncher.Join);
        arrayList.add(WebAppLauncher.Close);
        setCapabilities(arrayList);
    }

    public void getPlayState(PlayStateListener playStateListener) {
        RemoteMediaPlayer remoteMediaPlayer = this.mMediaPlayer;
        if (remoteMediaPlayer == null || remoteMediaPlayer.getMediaStatus() == null) {
            Util.postError(playStateListener, new ServiceCommandError(0, "There is no media currently available", null));
        } else {
            Util.postSuccess(playStateListener, PlayStateStatus.convertPlayerStateToPlayStateStatus(this.mMediaPlayer.getMediaStatus().getPlayerState()));
        }
    }

    public GoogleApiClient getApiClient() {
        return this.mApiClient;
    }

    public boolean isConnected() {
        return this.connected;
    }

    public ServiceSubscription<PlayStateListener> subscribePlayState(PlayStateListener playStateListener) {
        URLServiceSubscription uRLServiceSubscription = new URLServiceSubscription(this, PLAY_STATE, null, null);
        uRLServiceSubscription.addListener(playStateListener);
        addSubscription(uRLServiceSubscription);
        return uRLServiceSubscription;
    }

    private void addSubscription(URLServiceSubscription<?> uRLServiceSubscription) {
        this.subscriptions.add(uRLServiceSubscription);
    }

    public void unsubscribe(URLServiceSubscription<?> uRLServiceSubscription) {
        this.subscriptions.remove(uRLServiceSubscription);
    }

    public List<URLServiceSubscription<?>> getSubscriptions() {
        return this.subscriptions;
    }

    public void setSubscriptions(List<URLServiceSubscription<?>> list) {
        this.subscriptions = list;
    }

    public void runCommand(ConnectionListener connectionListener) {
        GoogleApiClient googleApiClient = this.mApiClient;
        if (googleApiClient == null || !googleApiClient.isConnected()) {
            connect();
            this.commandQueue.add(connectionListener);
            return;
        }
        connectionListener.onConnected();
    }

    interface ConnectionListener {
        void onConnected();
    }

    public interface LaunchWebAppListener {
        void onFailure(ServiceCommandError serviceCommandError);

        void onSuccess(WebAppSession webAppSession);
    }

    static class CastClient {
        CastClient() {
        }

        public void leaveApplication(GoogleApiClient googleApiClient) throws CastClientException {
            try {
                Cast.CastApi.leaveApplication(googleApiClient);
            } catch (RuntimeException e2) {
                throw createCastClientException(e2);
            }
        }

        public void setMessageReceivedCallbacks(GoogleApiClient googleApiClient, String str, RemoteMediaPlayer remoteMediaPlayer) throws CastClientException {
            try {
                Cast.CastApi.setMessageReceivedCallbacks(googleApiClient, remoteMediaPlayer.getNamespace(), remoteMediaPlayer);
            } catch (RuntimeException e2) {
                throw createCastClientException(e2);
            } catch (IOException e3) {
                throw createCastClientException(e3);
            }
        }

        public void removeMessageReceivedCallbacks(GoogleApiClient googleApiClient, String str) throws CastClientException {
            try {
                Cast.CastApi.removeMessageReceivedCallbacks(googleApiClient, str);
            } catch (IOException e2) {
                throw createCastClientException(e2);
            } catch (RuntimeException e3) {
                throw createCastClientException(e3);
            }
        }

        public Object getApplicationStatus(GoogleApiClient googleApiClient) throws CastClientException {
            try {
                return Cast.CastApi.getApplicationStatus(googleApiClient);
            } catch (RuntimeException e2) {
                throw createCastClientException(e2);
            }
        }

        public PendingResult<Cast.ApplicationConnectionResult> launchApplication(GoogleApiClient googleApiClient, String str, LaunchOptions launchOptions) throws CastClientException {
            try {
                return Cast.CastApi.launchApplication(googleApiClient, str, launchOptions);
            } catch (RuntimeException e2) {
                throw createCastClientException(e2);
            }
        }

        public PendingResult<Status> stopApplication(GoogleApiClient googleApiClient, String str) throws CastClientException {
            try {
                return Cast.CastApi.stopApplication(googleApiClient, str);
            } catch (RuntimeException e2) {
                throw createCastClientException(e2);
            }
        }

        public PendingResult<Cast.ApplicationConnectionResult> joinApplication(GoogleApiClient googleApiClient) throws CastClientException {
            try {
                return Cast.CastApi.joinApplication(googleApiClient);
            } catch (RuntimeException e2) {
                throw createCastClientException(e2);
            }
        }

        public PendingResult<Cast.ApplicationConnectionResult> joinApplication(GoogleApiClient googleApiClient, String str) throws CastClientException {
            try {
                return Cast.CastApi.joinApplication(googleApiClient, str);
            } catch (RuntimeException e2) {
                throw createCastClientException(e2);
            }
        }

        public PendingResult<Status> stopApplication(GoogleApiClient googleApiClient) throws CastClientException {
            try {
                return Cast.CastApi.stopApplication(googleApiClient);
            } catch (RuntimeException e2) {
                throw createCastClientException(e2);
            }
        }

        public void setVolume(GoogleApiClient googleApiClient, float f) throws IOException, CastClientException {
            try {
                Cast.CastApi.setVolume(googleApiClient, (double) f);
            } catch (RuntimeException e2) {
                throw createCastClientException(e2);
            } catch (IOException e3) {
                throw createCastClientException(e3);
            }
        }

        public void setMute(GoogleApiClient googleApiClient, boolean z) throws IOException, CastClientException {
            try {
                Cast.CastApi.setMute(googleApiClient, z);
            } catch (RuntimeException e2) {
                throw createCastClientException(e2);
            } catch (IOException e3) {
                throw createCastClientException(e3);
            }
        }

        public ApplicationMetadata getApplicationMetadata(GoogleApiClient googleApiClient) throws CastClientException {
            try {
                return Cast.CastApi.getApplicationMetadata(googleApiClient);
            } catch (RuntimeException e2) {
                throw createCastClientException(e2);
            }
        }

        public double getVolume(GoogleApiClient googleApiClient) throws CastClientException {
            try {
                return Cast.CastApi.getVolume(googleApiClient);
            } catch (RuntimeException e2) {
                throw createCastClientException(e2);
            }
        }

        public boolean isMute(GoogleApiClient googleApiClient) throws CastClientException {
            try {
                return Cast.CastApi.isMute(googleApiClient);
            } catch (RuntimeException e2) {
                throw createCastClientException(e2);
            }
        }

        private CastClientException createCastClientException(Exception exc) {
            return new CastClientException("CastClient error", exc);
        }
    }

    static class CastClientException extends Exception {
        CastClientException(String str, Exception exc) {
            super(str, exc);
        }
    }

    private class ApplicationConnectionResultCallback implements ResultCallback<Cast.ApplicationConnectionResult> {
        LaunchWebAppListener listener;

        public ApplicationConnectionResultCallback(LaunchWebAppListener launchWebAppListener) {
            this.listener = launchWebAppListener;
        }

        public void onResult(Cast.ApplicationConnectionResult applicationConnectionResult) {
            Status status = applicationConnectionResult.getStatus();
            if (status.isSuccess()) {
                ApplicationMetadata applicationMetadata = applicationConnectionResult.getApplicationMetadata();
                CastService.this.currentAppId = applicationMetadata.getApplicationId();
                LaunchSession launchSessionForAppId = LaunchSession.launchSessionForAppId(applicationMetadata.getApplicationId());
                launchSessionForAppId.setAppName(applicationMetadata.getName());
                launchSessionForAppId.setSessionId(applicationConnectionResult.getSessionId());
                launchSessionForAppId.setSessionType(LaunchSession.LaunchSessionType.WebApp);
                launchSessionForAppId.setService(CastService.this);
                CastWebAppSession castWebAppSession = new CastWebAppSession(launchSessionForAppId, CastService.this);
                castWebAppSession.setMetadata(applicationMetadata);
                CastService.this.sessions.put(applicationMetadata.getApplicationId(), castWebAppSession);
                LaunchWebAppListener launchWebAppListener = this.listener;
                if (launchWebAppListener != null) {
                    launchWebAppListener.onSuccess(castWebAppSession);
                }
                CastService.this.launchingAppId = null;
                return;
            }
            LaunchWebAppListener launchWebAppListener2 = this.listener;
            if (launchWebAppListener2 != null) {
                launchWebAppListener2.onFailure(new ServiceCommandError(status.getStatusCode(), status.getStatusMessage(), status));
            }
        }
    }

    private class CastListener extends Cast.Listener {
        private CastListener() {
        }

        public void onApplicationDisconnected(int i) {
            String str = Util.T;
            StringBuilder sb = new StringBuilder();
            sb.append("Cast.Listener.onApplicationDisconnected: ");
            sb.append(i);
            CastService castService = CastService.this;
            String str2 = castService.currentAppId;
            if (str2 != null) {
                CastWebAppSession castWebAppSession = castService.sessions.get(str2);
                if (castWebAppSession != null) {
                    castWebAppSession.handleAppClose();
                    CastService.this.currentAppId = null;
                }
            }
        }

        public void onApplicationStatusChanged() {
            CastService.this.runCommand(new ConnectionListener() {
                public void onConnected() {
                    CastService castService = CastService.this;
                    GoogleApiClient googleApiClient = castService.mApiClient;
                    if (googleApiClient != null) {
                        try {
                            ApplicationMetadata applicationMetadata = castService.mCastClient.getApplicationMetadata(googleApiClient);
                            if (applicationMetadata != null) {
                                CastService.this.currentAppId = applicationMetadata.getApplicationId();
                            }
                        } catch (CastClientException unused) {
                            String str = Util.T;
                        }
                    }
                }
            });
        }

        public void onVolumeChanged() {
            CastService.this.runCommand(new ConnectionListener() {
                public void onConnected() {
                    try {
                        CastService castService = CastService.this;
                        castService.currentVolumeLevel = (float) castService.mCastClient.getVolume(castService.mApiClient);
                        CastService castService2 = CastService.this;
                        castService2.currentMuteStatus = castService2.mCastClient.isMute(castService2.mApiClient);
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                    if (CastService.this.subscriptions.size() > 0) {
                        for (URLServiceSubscription next : CastService.this.subscriptions) {
                            int i = 0;
                            if (next.getTarget().equals(CastService.CAST_SERVICE_VOLUME_SUBSCRIPTION_NAME)) {
                                while (i < next.getListeners().size()) {
                                    Util.postSuccess((ResponseListener) next.getListeners().get(i), Float.valueOf(CastService.this.currentVolumeLevel));
                                    i++;
                                }
                            } else if (next.getTarget().equals("mute")) {
                                while (i < next.getListeners().size()) {
                                    Util.postSuccess((ResponseListener) next.getListeners().get(i), Boolean.valueOf(CastService.this.currentMuteStatus));
                                    i++;
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
        private ConnectionCallbacks() {
        }

        public void onConnectionSuspended(int i) {
            String str = Util.T;
            CastService castService = CastService.this;
            castService.mWaitingForReconnect = true;
            castService.detachMediaPlayer();
        }

        public void onConnected(Bundle bundle) {
            String str = Util.T;
            StringBuilder sb = new StringBuilder();
            sb.append("ConnectionCallbacks.onConnected, wasWaitingForReconnect: ");
            sb.append(CastService.this.mWaitingForReconnect);
            CastService.this.attachMediaPlayer();
            GoogleApiClient googleApiClient = CastService.this.mApiClient;
            if (googleApiClient != null && googleApiClient.isConnected()) {
                try {
                    CastService castService = CastService.this;
                    castService.mCastClient.joinApplication(castService.mApiClient).setResultCallback(new ResultCallback<Cast.ApplicationConnectionResult>() {
                        public void onResult(Cast.ApplicationConnectionResult applicationConnectionResult) {
                            ConnectionCallbacks.this.onJoinApplicationResult(applicationConnectionResult);
                        }
                    });
                } catch (CastClientException unused) {
                    String str2 = Util.T;
                }
            }
        }

        public void onJoinApplicationResult(Cast.ApplicationConnectionResult applicationConnectionResult) {
            if (applicationConnectionResult.getStatus().isSuccess()) {
                if (!(applicationConnectionResult.getApplicationMetadata() == null || applicationConnectionResult.getApplicationMetadata().getName() == null || applicationConnectionResult.getApplicationMetadata().getName().equals("Backdrop"))) {
                    CastService castService = CastService.this;
                    RemoteMediaPlayer remoteMediaPlayer = castService.mMediaPlayer;
                    if (remoteMediaPlayer != null) {
                        GoogleApiClient googleApiClient = castService.mApiClient;
                        if (googleApiClient != null) {
                            remoteMediaPlayer.requestStatus(googleApiClient).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                                public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                                    ConnectionCallbacks.this.joinFinished();
                                }
                            });
                            return;
                        }
                    }
                }
                joinFinished();
                return;
            }
            joinFinished();
        }

        public void joinFinished() {
            CastService castService = CastService.this;
            if (castService.mWaitingForReconnect) {
                castService.mWaitingForReconnect = false;
            } else {
                castService.connected = true;
                castService.reportConnected(true);
            }
            if (!CastService.this.commandQueue.isEmpty()) {
                Iterator<ConnectionListener> it = CastService.this.commandQueue.iterator();
                while (it.hasNext()) {
                    ConnectionListener next = it.next();
                    next.onConnected();
                    CastService.this.commandQueue.remove(next);
                }
            }
        }
    }

    private class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
        private ConnectionFailedListener() {
        }

        public void onConnectionFailed(final ConnectionResult connectionResult) {
            String str = Util.T;
            StringBuilder sb = new StringBuilder();
            sb.append("ConnectionFailedListener.onConnectionFailed ");
            sb.append(connectionResult != null ? connectionResult : "");
            CastService.this.detachMediaPlayer();
            CastService castService = CastService.this;
            castService.connected = false;
            castService.mWaitingForReconnect = false;
            castService.mApiClient = null;
            Util.runOnUI(new Runnable() {
                public void run() {
                    if (CastService.this.listener != null) {
                        ServiceCommandError serviceCommandError = new ServiceCommandError(connectionResult.getErrorCode(), "Failed to connect to Google Cast device", connectionResult);
                        CastService castService = CastService.this;
                        castService.listener.onConnectionFailure(castService, serviceCommandError);
                    }
                }
            });
        }
    }
}
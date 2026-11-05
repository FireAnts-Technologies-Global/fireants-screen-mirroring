package com.connectsdk.service;

import com.amazon.whisperplay.fling.media.controller.RemoteMediaPlayer;
import com.amazon.whisperplay.fling.media.service.CustomMediaPlayer;
import com.amazon.whisperplay.fling.media.service.MediaPlayerInfo;
import com.amazon.whisperplay.fling.media.service.MediaPlayerStatus;
import com.connectsdk.core.ImageInfo;
import com.connectsdk.core.MediaInfo;
import com.connectsdk.core.Util;
import com.connectsdk.discovery.DiscoveryFilter;
import com.connectsdk.service.capability.CapabilityMethods;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.FireTVServiceError;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;
import com.connectsdk.service.config.ServiceConfig;
import com.connectsdk.service.config.ServiceDescription;
import com.connectsdk.service.sessions.LaunchSession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class FireTVService extends DeviceService implements MediaPlayer, MediaControl {
    public static final String ID = "FireTV";
    private static final String META_DESCRIPTION = "description";
    private static final String META_ICON_IMAGE = "poster";
    private static final String META_KIND = "kind";
    private static final String META_LABEL = "label";
    private static final String META_MIME_TYPE = "type";
    private static final String META_NOREPLAY = "noreplay";
    private static final String META_SRC = "src";
    private static final String META_SRCLANG = "srclang";
    private static final String META_TITLE = "title";
    private static final String META_TRACKS = "tracks";
    public final RemoteMediaPlayer remoteMediaPlayer;
    public PlayStateSubscription playStateSubscription;

    public FireTVService(ServiceDescription serviceDescription, ServiceConfig serviceConfig) {
        super(serviceDescription, serviceConfig);
        if (serviceDescription == null || !(serviceDescription.getDevice() instanceof RemoteMediaPlayer)) {
            this.remoteMediaPlayer = null;
        } else {
            this.remoteMediaPlayer = (RemoteMediaPlayer) serviceDescription.getDevice();
        }
    }

    public static DiscoveryFilter discoveryFilter() {
        return new DiscoveryFilter(ID, ID);
    }

    public MediaControl getMediaControl() {
        return this;
    }

    public MediaPlayer getMediaPlayer() {
        return this;
    }

    public boolean isConnectable() {
        return true;
    }

    public void connect() {
        super.connect();
        if (this.remoteMediaPlayer != null) {
            this.connected = true;
            reportConnected(true);
        }
    }

    public boolean isConnected() {
        return this.connected;
    }

    public void disconnect() {
        super.disconnect();
        PlayStateSubscription playStateSubscription2 = this.playStateSubscription;
        if (playStateSubscription2 != null) {
            playStateSubscription2.unsubscribe();
            this.playStateSubscription = null;
        }
        this.connected = false;
    }

    /* access modifiers changed from: protected */
    public void updateCapabilities() {
        ArrayList arrayList = new ArrayList();
        arrayList.add(MediaPlayer.MediaInfo_Get);
        arrayList.add(MediaPlayer.Display_Image);
        arrayList.add("MediaPlayer.Play.Audio");
        arrayList.add("MediaPlayer.Play.Video");
        arrayList.add(MediaPlayer.Close);
        arrayList.add(MediaPlayer.MetaData_MimeType);
        arrayList.add(MediaPlayer.MetaData_Thumbnail);
        arrayList.add(MediaPlayer.MetaData_Title);
        arrayList.add(MediaPlayer.Subtitle_WebVTT);
        arrayList.add(MediaControl.Play);
        arrayList.add(MediaControl.Pause);
        arrayList.add(MediaControl.Stop);
        arrayList.add(MediaControl.Seek);
        arrayList.add(MediaControl.Duration);
        arrayList.add(MediaControl.Position);
        arrayList.add(MediaControl.PlayState);
        arrayList.add(MediaControl.PlayState_Subscribe);
        setCapabilities(arrayList);
    }

    public CapabilityPriorityLevel getPriorityLevel(Class<? extends CapabilityMethods> cls) {
        if (cls != null) {
            if (cls.equals(MediaPlayer.class)) {
                return getMediaPlayerCapabilityLevel();
            }
            if (cls.equals(MediaControl.class)) {
                return getMediaControlCapabilityLevel();
            }
        }
        return CapabilityPriorityLevel.NOT_SUPPORTED;
    }

    public CapabilityPriorityLevel getMediaPlayerCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    public void getMediaInfo(MediaInfoListener mediaInfoListener) {
        try {
            handleAsyncFutureWithConversion(mediaInfoListener, this.remoteMediaPlayer.getMediaInfo(), new ConvertResult<MediaInfo, MediaPlayerInfo>() {
                public MediaInfo convert(MediaPlayerInfo mediaPlayerInfo) throws JSONException {
                    ArrayList arrayList;
                    JSONObject jSONObject = new JSONObject(mediaPlayerInfo.getMetadata());
                    if (jSONObject.has(FireTVService.META_ICON_IMAGE)) {
                        arrayList = new ArrayList();
                        arrayList.add(new ImageInfo(jSONObject.getString(FireTVService.META_ICON_IMAGE)));
                    } else {
                        arrayList = null;
                    }
                    MediaInfo mediaInfo = new MediaInfo(mediaPlayerInfo.getSource(), jSONObject.getString("type"), jSONObject.getString("title"), jSONObject.getString("description"), arrayList);
                    return mediaInfo;
                }
            }, "Error getting media info");
        } catch (Exception unused) {
            Util.postError(mediaInfoListener, new FireTVServiceError("Error getting media info"));
        }
    }

    public ServiceSubscription<MediaInfoListener> subscribeMediaInfo(MediaInfoListener mediaInfoListener) {
        Util.postError(mediaInfoListener, ServiceCommandError.notSupported());
        return null;
    }

    public void displayImage(String str, String str2, String str3, String str4, String str5, LaunchListener launchListener) {
        setMediaSource(new MediaInfo.Builder(str, str2).setTitle(str3).setDescription(str4).setIcon(str5).build(), launchListener);
    }

    public void playMedia(String str, String str2, String str3, String str4, String str5, boolean z, LaunchListener launchListener) {
        setMediaSource(new MediaInfo.Builder(str, str2).setTitle(str3).setDescription(str4).setIcon(str5).build(), launchListener);
    }

    public void closeMedia(LaunchSession launchSession, ResponseListener<Object> responseListener) {
        stop(responseListener);
    }

    public void displayImage(MediaInfo mediaInfo, LaunchListener launchListener) {
        setMediaSource(mediaInfo, launchListener);
    }

    public void playMedia(MediaInfo mediaInfo, boolean z, LaunchListener launchListener) {
        setMediaSource(mediaInfo, launchListener);
    }

    public CapabilityPriorityLevel getMediaControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    public void play(ResponseListener<Object> responseListener) {
        try {
            handleVoidAsyncFuture(responseListener, this.remoteMediaPlayer.play(), "Error playing");
        } catch (Exception e2) {
            Util.postError(responseListener, new FireTVServiceError("Error playing", e2));
        }
    }

    public void pause(ResponseListener<Object> responseListener) {
        try {
            handleVoidAsyncFuture(responseListener, this.remoteMediaPlayer.pause(), "Error pausing");
        } catch (Exception e2) {
            Util.postError(responseListener, new FireTVServiceError("Error pausing", e2));
        }
    }

    public void stop(ResponseListener<Object> responseListener) {
        try {
            handleVoidAsyncFuture(responseListener, this.remoteMediaPlayer.stop(), "Error stopping");
        } catch (Exception e2) {
            Util.postError(responseListener, new FireTVServiceError("Error stopping", e2));
        }
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

    public void seek(long j, ResponseListener<Object> responseListener) {
        try {
            handleVoidAsyncFuture(responseListener, this.remoteMediaPlayer.seek(CustomMediaPlayer.PlayerSeekMode.Absolute, j), "Error seeking");
        } catch (Exception e2) {
            Util.postError(responseListener, new FireTVServiceError("Error seeking", e2));
        }
    }

    public void getDuration(DurationListener durationListener) {
        try {
            handleAsyncFuture(durationListener, this.remoteMediaPlayer.getDuration(), "Error getting duration");
        } catch (Exception e2) {
            Util.postError(durationListener, new FireTVServiceError("Error getting duration", e2));
        }
    }

    public void getPosition(PositionListener positionListener) {
        try {
            handleAsyncFuture(positionListener, this.remoteMediaPlayer.getPosition(), "Error getting position");
        } catch (Exception e2) {
            Util.postError(positionListener, new FireTVServiceError("Error getting position", e2));
        }
    }

    public void getPlayState(PlayStateListener playStateListener) {
        try {
            handleAsyncFutureWithConversion(playStateListener, this.remoteMediaPlayer.getStatus(), new ConvertResult<PlayStateStatus, MediaPlayerStatus>() {
                public PlayStateStatus convert(MediaPlayerStatus mediaPlayerStatus) {
                    return FireTVService.this.createPlayStateStatusFromFireTVStatus(mediaPlayerStatus);
                }
            }, "Error getting play state");
        } catch (Exception e2) {
            Util.postError(playStateListener, new FireTVServiceError("Error getting play state", e2));
        }
    }

    public ServiceSubscription<PlayStateListener> subscribePlayState(PlayStateListener playStateListener) {
        PlayStateSubscription playStateSubscription2 = this.playStateSubscription;
        if (playStateSubscription2 == null) {
            PlayStateSubscription playStateSubscription3 = new PlayStateSubscription(playStateListener);
            this.playStateSubscription = playStateSubscription3;
            this.remoteMediaPlayer.addStatusListener(playStateSubscription3);
        } else if (!playStateSubscription2.getListeners().contains(playStateListener)) {
            this.playStateSubscription.addListener(playStateListener);
        }
        getPlayState(playStateListener);
        return this.playStateSubscription;
    }

    /* access modifiers changed from: package-private */
    public PlayStateStatus createPlayStateStatusFromFireTVStatus(MediaPlayerStatus mediaPlayerStatus) {
        PlayStateStatus playStateStatus = PlayStateStatus.Unknown;
        if (mediaPlayerStatus == null) {
            return playStateStatus;
        }
        //int i = AnonymousClass7.$SwitchMap$com$amazon$whisperplay$fling$media$service$MediaPlayerStatus$MediaState[mediaPlayerStatus.getState().ordinal()];
        int i = 2;
        if (i == 1) {
            return PlayStateStatus.Buffering;
        }
        if (i == 2) {
            return PlayStateStatus.Playing;
        }
        if (i == 3) {
            return PlayStateStatus.Paused;
        }
        if (i == 4) {
            return PlayStateStatus.Finished;
        }
        if (i != 5) {
            return playStateStatus;
        }
        return PlayStateStatus.Idle;
    }

    private String getMetadata(MediaInfo mediaInfo) throws JSONException {
        JSONObject jSONObject = new JSONObject();
        if (mediaInfo.getTitle() != null && !mediaInfo.getTitle().isEmpty()) {
            jSONObject.put("title", (Object) mediaInfo.getTitle());
        }
        if (mediaInfo.getDescription() != null && !mediaInfo.getDescription().isEmpty()) {
            jSONObject.put("description", (Object) mediaInfo.getDescription());
        }
        jSONObject.put("type", (Object) mediaInfo.getMimeType());
        if (mediaInfo.getImages() != null && mediaInfo.getImages().size() > 0) {
            ImageInfo imageInfo = mediaInfo.getImages().get(0);
            if (!(imageInfo == null || imageInfo.getUrl() == null || imageInfo.getUrl().isEmpty())) {
                jSONObject.put(META_ICON_IMAGE, (Object) imageInfo.getUrl());
            }
        }
        jSONObject.put("noreplay", true);
        if (mediaInfo.getSubtitleInfo() != null) {
            JSONArray jSONArray = new JSONArray();
            JSONObject jSONObject2 = new JSONObject();
            jSONObject2.put("kind", (Object) "subtitles");
            jSONObject2.put("src", (Object) mediaInfo.getSubtitleInfo().getUrl());
            String label = mediaInfo.getSubtitleInfo().getLabel();
            String str = "";
            if (label == null) {
                label = str;
            }
            jSONObject2.put("label", (Object) label);
            String language = mediaInfo.getSubtitleInfo().getLanguage();
            if (language != null) {
                str = language;
            }
            jSONObject2.put("srclang", (Object) str);
            jSONArray.put(jSONObject2);
            jSONObject.put("tracks", (Object) jSONArray);
        }
        return jSONObject.toString();
    }

    public MediaLaunchObject createMediaLaunchObject() {
        LaunchSession launchSession = new LaunchSession();
        launchSession.setService(this);
        launchSession.setSessionType(LaunchSession.LaunchSessionType.Media);
        launchSession.setAppId(this.remoteMediaPlayer.getUniqueIdentifier());
        launchSession.setAppName(this.remoteMediaPlayer.getName());
        return new MediaLaunchObject(launchSession, this);
    }

    private void setMediaSource(MediaInfo mediaInfo, LaunchListener launchListener) {
        try {
            handleAsyncFutureWithConversion(launchListener, this.remoteMediaPlayer.setMediaSource(mediaInfo.getUrl(), getMetadata(mediaInfo), true, false), new ConvertResult<MediaLaunchObject, Void>() {
                public MediaLaunchObject convert(Void voidR) {
                    return FireTVService.this.createMediaLaunchObject();
                }
            }, "Error setting media source");
        } catch (Exception e2) {
            Util.postError(launchListener, new FireTVServiceError("Error setting media source", e2));
        }
    }

    private void handleVoidAsyncFuture(ResponseListener<Object> responseListener, RemoteMediaPlayer.AsyncFuture<Void> asyncFuture, String str) {
        handleAsyncFutureWithConversion(responseListener, asyncFuture, new ConvertResult<Object, Void>() {
            public Object convert(Void voidR) {
                return voidR;
            }
        }, str);
    }

    private <T> void handleAsyncFuture(ResponseListener<T> responseListener, RemoteMediaPlayer.AsyncFuture<T> asyncFuture, String str) {
        handleAsyncFutureWithConversion(responseListener, asyncFuture, new ConvertResult<T, T>() {
            public T convert(T t) {
                return t;
            }
        }, str);
    }

    private <Response, Result> void handleAsyncFutureWithConversion(final ResponseListener<Response> responseListener, RemoteMediaPlayer.AsyncFuture<Result> asyncFuture, final ConvertResult<Response, Result> convertResult, final String str) {
        if (asyncFuture != null) {
            asyncFuture.getAsync(new RemoteMediaPlayer.FutureListener<Result>() {
                public void futureIsNow(Future<Result> future) {
                    try {
                        Util.postSuccess(responseListener, convertResult.convert(future.get()));
                    } catch (ExecutionException e2) {
                        Util.postError(responseListener, new FireTVServiceError(str, e2.getCause()));
                    } catch (Exception e3) {
                        Util.postError(responseListener, new FireTVServiceError(str, e3));
                    }
                }
            });
        } else {
            Util.postError(responseListener, new FireTVServiceError(str));
        }
    }

    private interface ConvertResult<Response, Result> {
        Response convert(Result result) throws Exception;
    }

    private static abstract class Subscription<Status, Listener extends ResponseListener<Status>> implements ServiceSubscription<Listener> {
        List<Listener> listeners;
        Status prevStatus;

        public Subscription(Listener listener) {
            ArrayList arrayList = new ArrayList();
            this.listeners = arrayList;
            if (listener != null) {
                arrayList.add(listener);
            }
        }

        /* access modifiers changed from: package-private */
        public synchronized void notifyListeners(final Status status) {
            if (!status.equals(this.prevStatus)) {
                Util.runOnUI(new Runnable() {
                    public void run() {
                        for (Listener onSuccess : Subscription.this.listeners) {
                            onSuccess.onSuccess(status);
                        }
                    }
                });
                this.prevStatus = status;
            }
        }

        public Listener addListener(Listener listener) {
            if (listener != null) {
                this.listeners.add(listener);
            }
            return listener;
        }

        public void removeListener(Listener listener) {
            this.listeners.remove(listener);
        }

        public List<Listener> getListeners() {
            return this.listeners;
        }
    }

    class PlayStateSubscription extends Subscription<PlayStateStatus, PlayStateListener> implements CustomMediaPlayer.StatusListener {
        public PlayStateSubscription(PlayStateListener playStateListener) {
            super(playStateListener);
        }

        public void onStatusChange(MediaPlayerStatus mediaPlayerStatus, long j) {
            notifyListeners(FireTVService.this.createPlayStateStatusFromFireTVStatus(mediaPlayerStatus));
        }

        public void unsubscribe() {
            FireTVService.this.remoteMediaPlayer.removeStatusListener(this);
            PlayStateSubscription unused = FireTVService.this.playStateSubscription = null;
        }
    }
}
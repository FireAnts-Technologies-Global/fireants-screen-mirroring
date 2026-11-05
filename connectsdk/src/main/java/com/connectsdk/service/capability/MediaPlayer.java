package com.connectsdk.service.capability;

import com.connectsdk.core.MediaInfo;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceSubscription;
import com.connectsdk.service.sessions.LaunchSession;

public interface MediaPlayer extends CapabilityMethods {
    String Any = "MediaPlayer.Any";

    @Deprecated
    String Display_Video = "MediaPlayer.Play.Video";

    @Deprecated
    String Display_Audio = "MediaPlayer.Play.Audio";

    String Display_Image = "MediaPlayer.Display.Image";
    String Play_Video = "MediaPlayer.Play.Video";
    String Play_Audio = "MediaPlayer.Play.Audio";
    String Play_Playlist = "MediaPlayer.Play.Playlist";
    String Close = "MediaPlayer.Close";
    String Loop = "MediaPlayer.Loop";
    String Subtitle_SRT = "MediaPlayer.Subtitle.SRT";
    String Subtitle_WebVTT = "MediaPlayer.Subtitle.WebVTT";

    String MetaData_Title = "MediaPlayer.MetaData.Title";
    String MetaData_Description = "MediaPlayer.MetaData.Description";
    String MetaData_Thumbnail = "MediaPlayer.MetaData.Thumbnail";
    String MetaData_MimeType = "MediaPlayer.MetaData.MimeType";

    String MediaInfo_Get = "MediaPlayer.MediaInfo.Get";
    String MediaInfo_Subscribe = "MediaPlayer.MediaInfo.Subscribe";

    String[] Capabilities = {
            Display_Image,
            Play_Video,
            Play_Audio,
            Close,
            MetaData_Title,
            MetaData_Description,
            MetaData_Thumbnail,
            MetaData_MimeType,
            MediaInfo_Get,
            MediaInfo_Subscribe
    };

    MediaPlayer getMediaPlayer();

    CapabilityPriorityLevel getMediaPlayerCapabilityLevel();

    void getMediaInfo(MediaInfoListener listener);

    ServiceSubscription<MediaInfoListener> subscribeMediaInfo(MediaInfoListener listener);

    void displayImage(MediaInfo mediaInfo, LaunchListener listener);

    void playMedia(MediaInfo mediaInfo, boolean shouldLoop, LaunchListener listener);

    void closeMedia(LaunchSession launchSession, ResponseListener<Object> listener);

    @Deprecated
    void displayImage(String url, String mimeType, String title, String description, String iconSrc, LaunchListener listener);

    @Deprecated
    void playMedia(String url, String mimeType, String title, String description, String iconSrc, boolean shouldLoop, LaunchListener listener);

    interface LaunchListener extends ResponseListener<MediaLaunchObject> {
    }

    interface MediaInfoListener extends ResponseListener<MediaInfo> {
    }

    class MediaLaunchObject {
        public LaunchSession launchSession;
        public MediaControl mediaControl;
        public PlaylistControl playlistControl;

        public MediaLaunchObject(LaunchSession launchSession, MediaControl mediaControl) {
            this.launchSession = launchSession;
            this.mediaControl = mediaControl;
        }

        public MediaLaunchObject(LaunchSession launchSession, MediaControl mediaControl, PlaylistControl playlistControl) {
            this.launchSession = launchSession;
            this.mediaControl = mediaControl;
            this.playlistControl = playlistControl;
        }
    }

}

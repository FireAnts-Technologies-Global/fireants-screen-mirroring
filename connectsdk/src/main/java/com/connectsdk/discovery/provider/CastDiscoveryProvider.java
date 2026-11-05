package com.connectsdk.discovery.provider;

import android.content.Context;
import android.os.Bundle;

import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;

import com.connectsdk.core.Util;
import com.connectsdk.discovery.DiscoveryFilter;
import com.connectsdk.discovery.DiscoveryProvider;
import com.connectsdk.discovery.DiscoveryProviderListener;
import com.connectsdk.service.CastService;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.config.ServiceDescription;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;

import java.net.Inet4Address;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class CastDiscoveryProvider implements DiscoveryProvider {
    private static final long ROUTE_REMOVE_INTERVAL = 3000;
    public MediaRouteSelector mMediaRouteSelector;
    public MediaRouter mMediaRouter;
    public Timer removeRoutesTimer;
    public List<String> removedUUID = new CopyOnWriteArrayList();
    protected ConcurrentHashMap<String, ServiceDescription> foundServices;
    protected MediaRouter.Callback mMediaRouterCallback;
    protected CopyOnWriteArrayList<DiscoveryProviderListener> serviceListeners;
    boolean isRunning = false;

    public CastDiscoveryProvider(Context context) {
        this.mMediaRouter = createMediaRouter(context);
        this.mMediaRouterCallback = new MediaRouterCallback();
        this.foundServices = new ConcurrentHashMap<>(8, 0.75f, 2);
        this.serviceListeners = new CopyOnWriteArrayList<>();
    }

    public void addDeviceFilter(DiscoveryFilter discoveryFilter) {
    }

    public boolean isEmpty() {
        return false;
    }

    public void removeDeviceFilter(DiscoveryFilter discoveryFilter) {
    }

    public void setFilters(List<DiscoveryFilter> list) {
    }

    public MediaRouter createMediaRouter(Context context) {
        return MediaRouter.getInstance(context);
    }

    public void start() {
        if (!this.isRunning) {
            this.isRunning = true;
            if (this.mMediaRouteSelector == null) {
                try {
                    this.mMediaRouteSelector = new MediaRouteSelector.Builder().addControlCategory(CastMediaControlIntent.categoryForCast(CastService.getApplicationID())).build();
                } catch (IllegalArgumentException unused) {
                    String str = Util.T;
                    StringBuilder sb = new StringBuilder();
                    sb.append("Invalid application ID: ");
                    sb.append(CastService.getApplicationID());
                    Iterator<DiscoveryProviderListener> it = this.serviceListeners.iterator();
                    while (it.hasNext()) {
                        it.next().onServiceDiscoveryFailed(this, new ServiceCommandError(0, "Invalid application ID: " + CastService.getApplicationID(), null));
                    }
                    return;
                }
            }
            rescan();
        }
    }

    public void stop() {
        this.isRunning = false;
        Timer timer = this.removeRoutesTimer;
        if (timer != null) {
            timer.cancel();
            this.removeRoutesTimer = null;
        }
        if (this.mMediaRouter != null) {
            Util.runOnUI(new Runnable() {
                public void run() {
                    mMediaRouter.removeCallback(mMediaRouterCallback);
                }
            });
        }
    }

    public void restart() {
        stop();
        start();
    }

    public void reset() {
        stop();
        this.foundServices.clear();
    }

    public void rescan() {
        Util.runOnUI(new Runnable() {
            public void run() {
                mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback, 4);
            }
        });
    }

    public void addListener(DiscoveryProviderListener discoveryProviderListener) {
        this.serviceListeners.add(discoveryProviderListener);
    }

    public void removeListener(DiscoveryProviderListener discoveryProviderListener) {
        this.serviceListeners.remove(discoveryProviderListener);
    }

    private class MediaRouterCallback extends MediaRouter.Callback {
        private MediaRouterCallback() {
        }

        public void onRouteAdded(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo) {
            super.onRouteAdded(mediaRouter, routeInfo);
            CastDevice fromBundle = CastDevice.getFromBundle(routeInfo.getExtras());
            String deviceId = fromBundle.getDeviceId();
            removedUUID.remove(deviceId);
            ServiceDescription serviceDescription = foundServices.get(deviceId);
            boolean z = false;
            boolean z2 = true;
            if (serviceDescription == null) {
                try {
                    ServiceDescription serviceDescription2 = new ServiceDescription(CastService.ID, deviceId, fromBundle.getIpAddress().getHostAddress());
                    try {
                        serviceDescription2.setFriendlyName(fromBundle.getFriendlyName());
                        serviceDescription2.setModelName(fromBundle.getModelName());
                        serviceDescription2.setModelNumber(fromBundle.getDeviceVersion());
                        serviceDescription2.setModelDescription(routeInfo.getDescription());
                        serviceDescription2.setPort(fromBundle.getServicePort());
                        serviceDescription2.setServiceID(CastService.ID);
                        serviceDescription2.setDevice(fromBundle);
                    } catch (Exception unused) {
                        unused.printStackTrace();
                    }
                    serviceDescription = serviceDescription2;
                } catch (Exception unused2) {
                    unused2.printStackTrace();
                }
            } else {
                if (!serviceDescription.getFriendlyName().equals(fromBundle.getFriendlyName())) {
                    serviceDescription.setFriendlyName(fromBundle.getFriendlyName());
                    z = true;
                }
                serviceDescription.setDevice(fromBundle);
                z2 = z;
            }
            if (serviceDescription != null) {
                serviceDescription.setLastDetection(new Date().getTime());
            }

            foundServices.put(deviceId, serviceDescription);
            if (z2) {
                Iterator<DiscoveryProviderListener> it = serviceListeners.iterator();
                while (it.hasNext()) {
                    it.next().onServiceAdded(CastDiscoveryProvider.this, serviceDescription);
                }
            }
        }

        public void onRouteChanged(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo) {
            String nameDevice = "Chromecast";
            super.onRouteChanged(mediaRouter, routeInfo);
            try {
                Bundle bundle = routeInfo.getExtras();
                CastDevice castDevice = CastDevice.getFromBundle(bundle);
                String deviceID = castDevice.getDeviceId();
                Object obj = foundServices.get(deviceID);

                Inet4Address inet4Address = castDevice.getIpAddress();
                String hostAddress = inet4Address.getHostAddress();
                String friendName = castDevice.getFriendlyName();
                String modelName = castDevice.getModelName();
                String deviceVersion = castDevice.getDeviceVersion();
                int servicePort = castDevice.getServicePort();

                ServiceDescription description = new ServiceDescription(nameDevice, deviceID, hostAddress);
                description.setFriendlyName(friendName);
                description.setServiceID(nameDevice);
                description.setModelName(modelName);
                description.setVersion(deviceVersion);
                description.setPort(servicePort);

                Date date = new Date();
                long time = date.getTime();
                description.setLastDetection(time);

                foundServices.put(deviceID, description);
                Iterator iterator = serviceListeners.iterator();
                boolean r10 = iterator.hasNext();
                Object obj2 = iterator.next();
                DiscoveryProviderListener discoveryProviderListener = (DiscoveryProviderListener) serviceListeners;
                discoveryProviderListener.onServiceAdded(CastDiscoveryProvider.this, description);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void onRoutePresentationDisplayChanged(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo) {
            String str = Util.T;
            StringBuilder sb = new StringBuilder();
            sb.append("onRoutePresentationDisplayChanged: [");
            sb.append(routeInfo.getName());
            sb.append("] [");
            sb.append(routeInfo.getDescription());
            sb.append("]");
            super.onRoutePresentationDisplayChanged(mediaRouter, routeInfo);
        }

        public void onRouteRemoved(MediaRouter mediaRouter, final MediaRouter.RouteInfo routeInfo) {
            super.onRouteRemoved(mediaRouter, routeInfo);
            removedUUID.add(CastDevice.getFromBundle(routeInfo.getExtras()).getDeviceId());
            if (removeRoutesTimer == null) {
                removeRoutesTimer = new Timer();
                removeRoutesTimer.schedule(new TimerTask() {
                    public void run() {
                        removeServices(routeInfo);
                    }
                }, 3000);
            }
        }

        public void onRouteVolumeChanged(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo) {
            String str = Util.T;
            StringBuilder sb = new StringBuilder();
            sb.append("onRouteVolumeChanged: [");
            sb.append(routeInfo.getName());
            sb.append("] [");
            sb.append(routeInfo.getDescription());
            sb.append("]");
            super.onRouteVolumeChanged(mediaRouter, routeInfo);
        }

        public void removeServices(MediaRouter.RouteInfo routeInfo) {
            for (String str : removedUUID) {
                final ServiceDescription serviceDescription = foundServices.get(str);
                if (serviceDescription != null) {
                    String str2 = Util.T;
                    StringBuilder sb = new StringBuilder();
                    sb.append("Service [");
                    sb.append(routeInfo.getName());
                    sb.append("] has been removed");
                    Util.runOnUI(new Runnable() {
                        public void run() {
                            Iterator<DiscoveryProviderListener> it = serviceListeners.iterator();
                            while (it.hasNext()) {
                                it.next().onServiceRemoved(CastDiscoveryProvider.this, serviceDescription);
                            }
                        }
                    });
                    foundServices.remove(str);
                }
            }
            removedUUID.clear();
        }
    }
}
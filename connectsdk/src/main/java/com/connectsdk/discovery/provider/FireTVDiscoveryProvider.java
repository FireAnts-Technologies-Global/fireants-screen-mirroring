package com.connectsdk.discovery.provider;

import android.content.Context;

import com.amazon.whisperplay.fling.media.controller.DiscoveryController;
import com.amazon.whisperplay.fling.media.controller.RemoteMediaPlayer;
import com.connectsdk.core.Util;
import com.connectsdk.discovery.DiscoveryFilter;
import com.connectsdk.discovery.DiscoveryProvider;
import com.connectsdk.discovery.DiscoveryProviderListener;
import com.connectsdk.service.FireTVService;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.config.ServiceDescription;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class FireTVDiscoveryProvider implements DiscoveryProvider {
    DiscoveryController.IDiscoveryListener fireTVListener;
    ConcurrentHashMap<String, ServiceDescription> foundServices;
    CopyOnWriteArrayList<DiscoveryProviderListener> serviceListeners;
    private final DiscoveryController discoveryController;
    private boolean isRunning;

    public FireTVDiscoveryProvider(Context context) {
        this(new DiscoveryController(context));
    }

    public FireTVDiscoveryProvider(DiscoveryController discoveryController2) {
        this.foundServices = new ConcurrentHashMap<>();
        this.serviceListeners = new CopyOnWriteArrayList<>();
        this.discoveryController = discoveryController2;
        this.fireTVListener = new FireTVDiscoveryListener();
    }

    public void addDeviceFilter(DiscoveryFilter discoveryFilter) {
    }

    public void removeDeviceFilter(DiscoveryFilter discoveryFilter) {
    }

    public void setFilters(List<DiscoveryFilter> list) {
    }

    public void start() {
        if (!this.isRunning) {
            this.discoveryController.start(this.fireTVListener);
            this.isRunning = true;
        }
    }

    public void stop() {
        if (this.isRunning) {
            this.discoveryController.stop();
            this.isRunning = false;
        }
        for (ServiceDescription notifyListenersThatServiceLost : this.foundServices.values()) {
            notifyListenersThatServiceLost(notifyListenersThatServiceLost);
        }
        this.foundServices.clear();
    }

    public void restart() {
        stop();
        start();
    }

    public void rescan() {
        restart();
    }

    public void reset() {
        this.foundServices.clear();
        stop();
    }

    public void addListener(DiscoveryProviderListener discoveryProviderListener) {
        this.serviceListeners.add(discoveryProviderListener);
    }

    public void removeListener(DiscoveryProviderListener discoveryProviderListener) {
        this.serviceListeners.remove(discoveryProviderListener);
    }

    public boolean isEmpty() {
        return this.foundServices.isEmpty();
    }

    public void notifyListenersThatServiceAdded(final ServiceDescription serviceDescription) {
        Util.runOnUI(new Runnable() {
            public void run() {
                Iterator<DiscoveryProviderListener> it = serviceListeners.iterator();
                while (it.hasNext()) {
                    it.next().onServiceAdded(FireTVDiscoveryProvider.this, serviceDescription);
                }
            }
        });
    }

    public void notifyListenersThatServiceLost(final ServiceDescription serviceDescription) {
        Util.runOnUI(new Runnable() {
            public void run() {
                Iterator<DiscoveryProviderListener> it = serviceListeners.iterator();
                while (it.hasNext()) {
                    it.next().onServiceRemoved(FireTVDiscoveryProvider.this, serviceDescription);
                }
            }
        });
    }

    public void notifyListenersThatDiscoveryFailed(final ServiceCommandError serviceCommandError) {
        Util.runOnUI(new Runnable() {
            public void run() {
                Iterator<DiscoveryProviderListener> it = serviceListeners.iterator();
                while (it.hasNext()) {
                    it.next().onServiceDiscoveryFailed(FireTVDiscoveryProvider.this, serviceCommandError);
                }
            }
        });
    }

    class FireTVDiscoveryListener implements DiscoveryController.IDiscoveryListener {
        FireTVDiscoveryListener() {
        }

        public void playerDiscovered(RemoteMediaPlayer remoteMediaPlayer) {
            if (remoteMediaPlayer != null) {
                String uniqueIdentifier = remoteMediaPlayer.getUniqueIdentifier();
                ServiceDescription serviceDescription = foundServices.get(uniqueIdentifier);
                if (serviceDescription == null) {
                    ServiceDescription serviceDescription2 = new ServiceDescription();
                    updateServiceDescription(serviceDescription2, remoteMediaPlayer);
                    foundServices.put(uniqueIdentifier, serviceDescription2);
                    notifyListenersThatServiceAdded(serviceDescription2);
                } else {
                    updateServiceDescription(serviceDescription, remoteMediaPlayer);
                }
            }
        }

        public void playerLost(RemoteMediaPlayer remoteMediaPlayer) {
            if (remoteMediaPlayer != null) {
                ServiceDescription serviceDescription = foundServices.get(remoteMediaPlayer.getUniqueIdentifier());
                if (serviceDescription != null) {
                    notifyListenersThatServiceLost(serviceDescription);
                    foundServices.remove(remoteMediaPlayer.getUniqueIdentifier());
                }
            }
        }

        public void discoveryFailure() {
            notifyListenersThatDiscoveryFailed(new ServiceCommandError("FireTV discovery failure"));
        }

        private void updateServiceDescription(ServiceDescription serviceDescription, RemoteMediaPlayer remoteMediaPlayer) {
            String uniqueIdentifier = remoteMediaPlayer.getUniqueIdentifier();
            serviceDescription.setDevice(remoteMediaPlayer);
            serviceDescription.setFriendlyName(remoteMediaPlayer.getName());
            serviceDescription.setIpAddress(uniqueIdentifier);
            serviceDescription.setServiceID(FireTVService.ID);
            serviceDescription.setUUID(uniqueIdentifier);
        }
    }
}
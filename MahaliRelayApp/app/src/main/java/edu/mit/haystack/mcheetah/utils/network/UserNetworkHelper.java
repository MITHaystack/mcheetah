package edu.mit.haystack.mcheetah.utils.network;

/* The MIT License (MIT)
 * Copyright (c) 2015 Massachusetts Institute of Technology
 *
 * Author: David Mascharka
 * This software is part of the Mahali Project, PI: V. Pankratius
 * http://mahali.mit.edu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import edu.mit.haystack.mahalirelayapp.R;

/**
 * @author David Mascharka
 *
 * Used for when the user is in control of scans (eg click button -> start scan)
 */
public class UserNetworkHelper implements NetworkSelectionDialogFragment.NetworkSelectionDialogListener {
    /**
     * If the user didn't initiate the WiFi scan, we discard the results
     */
    private boolean userInitiatedScan;

    /**
     * Allows us to start WiFi scans, collect results, etc.
     */
    private WifiManager wifiManager;

    /**
     * The results of a WiFi scan
     */
    private List<ScanResult> scanResults;

    /**
     * Results we're interested in
     */
    private List<ScanResult> goodNetworks;

    /**
     * Allows us to use certain functionality like getting strings from resources
     */
    private AppCompatActivity owner;

    /**
     * Whether we are connected to a network of interest
     */
    private boolean connected;

    BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateScanResults();
        }
    };

    // Listens for a change in the connection state of the WiFi
    // Is notified when the device connects to or disconnects from a network
    private BroadcastReceiver wifiConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            actOnConnectionChanged(intent);
        }
    };

    public interface MahaliConnectionListener {
        void mahaliConnected(String ip, String ssid);
        void mahaliDisconnected();
    }
    private MahaliConnectionListener connectionListener;

    public UserNetworkHelper(AppCompatActivity context) {
        this.owner = context;

        try {
            connectionListener = (MahaliConnectionListener) owner;
        } catch (ClassCastException e) {
            throw new ClassCastException(owner.toString() + " must implement MahaliConnectionListener");
        }

        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        context.registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        context.registerReceiver(wifiConnectionReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));

        userInitiatedScan = false;
        connected = false;

        goodNetworks = new ArrayList<ScanResult>();
    }

    public void startScan() {
        wifiManager.setWifiEnabled(true);
        userInitiatedScan = true;
        wifiManager.startScan();
        Log.wtf("TEST", "STARTED SCAN");
    }

    @SuppressWarnings("deprecation")
    private void actOnConnectionChanged(Intent intent) {
        NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if (info.getState() == NetworkInfo.State.CONNECTED) {
            if (wifiManager.getConnectionInfo() != null) {
                if (wifiManager.getConnectionInfo().getSSID().toUpperCase().contains(owner.getString(
                        R.string.mahali_ssid_prefix))) {
                    if (!connected) {
                        String ip = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
                        String ssid = wifiManager.getConnectionInfo().getSSID();
                        connectionListener.mahaliConnected(ip, ssid);
                    }
                    connected = true;
                }
            }
        } else if (info.getState() == NetworkInfo.State.DISCONNECTED ||
                info.getState() == NetworkInfo.State.DISCONNECTING) {
            if (connected) {
                connectionListener.mahaliDisconnected();
            }
            connected = false;
        }
    }

    private void updateScanResults() {
        Log.wtf("TEST", "FINISHED A SCAN");
        if (wifiManager == null) {
            return;
        }

        // If the user didn't start the scan, we don't care about it
        if (!userInitiatedScan) {
            return;
        }

        userInitiatedScan = false;

        scanResults = wifiManager.getScanResults();
        goodNetworks.clear();

        for (ScanResult result : scanResults) {
            Log.wtf("TEST", "RESULT: " + result.SSID);
            //if (result.SSID.contains(owner.getString(R.string.mahali_ssid_prefix))) {
                goodNetworks.add(result);
            //}
        }

        if (goodNetworks.size() == 1) {
            connectToNetwork(goodNetworks.get(0).BSSID, "\"" + goodNetworks.get(0).SSID + "\"");
        } else if (goodNetworks.size() > 1) {
            NetworkSelectionDialogFragment networkSelector = new NetworkSelectionDialogFragment();
            networkSelector.setSelectionListener(this);
            Bundle arguments = new Bundle();

            String[] ssids = new String[goodNetworks.size()];
            String[] bssids = new String[goodNetworks.size()];
            String[] signalStrengths = new String[goodNetworks.size()];
            ScanResult mahaliBox;
            for (int i = 0; i < goodNetworks.size(); i++) {
                mahaliBox = goodNetworks.get(i);
                ssids[i] = mahaliBox.SSID;
                bssids[i] = mahaliBox.BSSID;
                signalStrengths[i] = Integer.toString(mahaliBox.level);
            }

            arguments.putStringArray("ssids", ssids);
            arguments.putStringArray("bssids", bssids);
            arguments.putStringArray("signal_strengths", signalStrengths);

            networkSelector.setArguments(arguments);
            networkSelector.show(owner.getSupportFragmentManager(), "MahaliNetworkSelect");
        }
    }

    private void connectToNetwork(final String bssid, final String ssid) {
        // If the device is already connected to the network we want to connect to just return now
        // This avoids us doing extra work and stops us from disconnecting down below then reconnecting
        // which would be pointless and interrupt potential data transfers in progress
        String currentBSSID = wifiManager.getConnectionInfo().getBSSID();
        String currentSSID = wifiManager.getConnectionInfo().getSSID();

        Log.wtf("TEST", "CONNECTION: " + bssid + " " + ssid);
        Log.wtf("TEST", "CURRENT: " + currentBSSID + " " + currentSSID);

        if ((currentBSSID != null && currentBSSID.equals(bssid)) ||
                (currentSSID != null && currentSSID.equals(ssid))) {
            Log.wtf("TEST", "ALREADY CONNECTED");
            return;
        }

        // If the network is already configured, connect to it now
        for (WifiConfiguration config : wifiManager.getConfiguredNetworks()) {
            if (config.BSSID != null && config.BSSID.equals(currentBSSID) &&
                    config.SSID != null && config.SSID.equals(currentSSID)) {
                wifiManager.enableNetwork(config.networkId, true);
                wifiManager.reconnect();
                return;
            }
        }

        // Do the WiFi management on a different thread
        // Network connection manipulation is a potentially long-running operation and
        // could block the UI causing the application to crash and the user to be unhappy
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                WifiConfiguration configuration = new WifiConfiguration();

                // Configure the network
                configuration.BSSID = bssid;
                configuration.SSID = ssid;
                configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                configuration.status = WifiConfiguration.Status.ENABLED;

                // set a high priority so this will be the preferred connection over whatever the
                // user may happen to be connected to already
                // TODO this shouldn't matter since we specify we want to connect to it later. Test and possibly remove
                configuration.priority = 50000;
                configuration.status = WifiConfiguration.Status.CURRENT;

                // Add the network to the WifiManager and save
                int netId = wifiManager.addNetwork(configuration);
                wifiManager.saveConfiguration();

                // Disconnect from whatever we're connected to now, if anything
                //wifiManager.disconnect();

                // Enable the Mahali connection and connect
                wifiManager.enableNetwork(netId, true);
                wifiManager.reconnect();
            }
        });

        worker.start();
    }

    @Override
    public void onNetworkSelected(String bssid, String ssid) {
        connectToNetwork(bssid, "\"" + ssid + "\"");
    }
}
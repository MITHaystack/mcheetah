package edu.mit.haystack.mahalirelayapp.heatmap;

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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.List;

import edu.mit.haystack.mahalirelayapp.AboutActivity;
import edu.mit.haystack.mahalirelayapp.DownloadedDataViewActivity;
import edu.mit.haystack.mahalirelayapp.FileManagerActivity;
import edu.mit.haystack.mahalirelayapp.R;
import edu.mit.haystack.mahalirelayapp.SatelliteUpdateActivity;
import edu.mit.haystack.mahalirelayapp.SettingsActivity;
import edu.mit.haystack.mahalirelayapp.UploadedDataViewActivity;
import edu.mit.haystack.mahalirelayapp.computation.dataselection.DataSelectionActivity;
import edu.mit.haystack.mahalirelayapp.position.PositionDialogFragment;

/**
 * @author David Mascharka
 *
 * Generates and draws a heatmap for Mahali WiFi signal strengths
 */
public class HeatmapActivity extends AppCompatActivity {

    private Button toggleButton;
    private boolean collectData;

    // Location members
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location location;

    // WiFi members
    private WifiManager wifiManager;
    private List<ScanResult> scanResults;

    private HeatmapPlotView plotView;

    // Will listen for broadcasts from the WiFi manager. When a scan finishes, onReceive is called which
    // updates the heatmap with a new datapoint
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateWifiScanResults();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heatmap);

        collectData = false;
        plotView = new HeatmapPlotView(this);
        ((LinearLayout) findViewById(R.id.view_heatmap_plot)).addView(plotView);

        toggleButton = (Button) findViewById(R.id.button_toggle);
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                collectData = !collectData;

                if (collectData) {
                    toggleButton.setText("Stop");
                    wifiManager.startScan();
                } else {
                    toggleButton.setText("Start");
                }
            }
        });

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateLocation(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) { }

            @Override
            public void onProviderEnabled(String provider) { }

            @Override
            public void onProviderDisabled(String provider) { }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        wifiManager.startScan();

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);

        registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    private void updateLocation(Location l) {
        location = l;
    }

    private void updateWifiScanResults() {
        if (collectData) {
            scanResults = wifiManager.getScanResults();
            wifiManager.startScan();
            for (ScanResult r : scanResults) {
                if (r.SSID.toUpperCase().contains(getString(R.string.mahali_ssid_prefix))) {
                    addPoint(r.SSID, r.level);
                }
            }
        }
    }

    private void addPoint(final String ssid, final int signalStrength) {
        if (location != null) {
            plotView.addPoint(new DataPoint((float) location.getLongitude(), (float) location.getLatitude(),
                    signalStrength));
        }
        // maybe write to file
    }

    @Override
    protected void onPause() {
        locationManager.removeUpdates(locationListener);
        unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_mahali_relay_app, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_downloaded_list) {
            Intent downloadedDataViewIntent = new Intent(this, DownloadedDataViewActivity.class);
            startActivity(downloadedDataViewIntent);
            return true;
        } else if (id == R.id.action_uploaded_list) {
            Intent uploadedDataViewIntent = new Intent(this, UploadedDataViewActivity.class);
            startActivity(uploadedDataViewIntent);
            return true;
        } else if (id == R.id.action_set_position) {
            PositionDialogFragment positionDialog = new PositionDialogFragment();
            positionDialog.show(getSupportFragmentManager(), "MahaliPositionDialog");
            return true;
        } else if (id == R.id.action_update_satellite_info) {
            Intent updateSatelliteIntent = new Intent(this, SatelliteUpdateActivity.class);
            startActivity(updateSatelliteIntent);
            return true;
        } else if (id == R.id.action_plot_tec) {
            Intent selectDataIntent = new Intent(this, DataSelectionActivity.class);
            startActivity(selectDataIntent);
            return true;
        } else if (id == R.id.action_wifi_heatmap) {
            Intent heatmapIntent = new Intent(this, HeatmapActivity.class);
            startActivity(heatmapIntent);
            return true;
        } else if (id == R.id.action_file_management) {
            Intent fileManagerIntent = new Intent(this, FileManagerActivity.class);
            startActivity(fileManagerIntent);
            return true;
        } else if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        } else if (id == R.id.action_about) {
            Intent aboutIntent = new Intent(this, AboutActivity.class);
            startActivity(aboutIntent);
            return true;
        }


        return super.onOptionsItemSelected(item);
    }
}

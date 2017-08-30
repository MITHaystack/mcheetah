package edu.mit.haystack.mahalirelayapp;

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

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;

import edu.mit.haystack.mahalirelayapp.computation.dataselection.DataEntry;
import edu.mit.haystack.mahalirelayapp.computation.dataselection.DataSelectionActivity;
import edu.mit.haystack.mahalirelayapp.heatmap.HeatmapActivity;
import edu.mit.haystack.mahalirelayapp.position.PositionDialogFragment;

/**
 * @author David Mascharka
 *
 * Displays a list of data downloaded to the device
 */
public class DownloadedDataViewActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_downloaded_data);
        ArrayList<String> dataFileNames = new ArrayList<String>();

        /**
         * The root directory where everything in the Mahali application goes is whatever the sdcard path
         * happens to be /mahali/
         * Create a handle to this directory
         */
        File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/mahali/");

        ArrayDeque<File> directoryQueue = new ArrayDeque<File>();
        directoryQueue.add(directory);

        // If the Mahali directory doesn't exist, make it
        directory.mkdirs();

        while (!directoryQueue.isEmpty()) {
            File dir = directoryQueue.remove();

            if (dir.exists() && dir.isDirectory()) {
                for (File f : dir.listFiles()) {
                    if (f.isDirectory() &&
                            f.getAbsolutePath().contains(getString(R.string.mahali_ssid_prefix).toLowerCase())) {
                        directoryQueue.add(f);
                    } else if (f.getAbsolutePath().contains(getString(R.string.mahali_ssid_prefix).toLowerCase())) {
                        dataFileNames.add(f.getAbsolutePath());
                    }
                }
            }
        }

        TextView files = (TextView) findViewById(R.id.text_downloaded_data);
        files.setText("");
        for (String s : dataFileNames) {
            String boxName = s.substring(s.indexOf(getString(R.string.mahali_ssid_prefix).toLowerCase()),
                    s.indexOf("/",
                            s.indexOf(getString(R.string.mahali_ssid_prefix).toLowerCase())));
            String fileName = s.substring(s.lastIndexOf("/"));

            files.setText(files.getText() + boxName + fileName + "\n");
        }
        files.setFreezesText(true); // handles orientation changes properly
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

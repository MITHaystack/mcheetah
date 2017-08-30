package edu.mit.haystack.mahalirelayapp.computation.dataselection;

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
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;

import edu.mit.haystack.mahalirelayapp.AboutActivity;
import edu.mit.haystack.mahalirelayapp.DownloadedDataViewActivity;
import edu.mit.haystack.mahalirelayapp.FileManagerActivity;
import edu.mit.haystack.mahalirelayapp.R;
import edu.mit.haystack.mahalirelayapp.SatelliteUpdateActivity;
import edu.mit.haystack.mahalirelayapp.SettingsActivity;
import edu.mit.haystack.mahalirelayapp.UploadedDataViewActivity;
import edu.mit.haystack.mahalirelayapp.computation.MahaliDataViewActivity;
import edu.mit.haystack.mahalirelayapp.heatmap.HeatmapActivity;
import edu.mit.haystack.mahalirelayapp.position.PositionDialogFragment;
import edu.mit.haystack.mcheetah.DataProcessFragment;
import edu.mit.haystack.mcheetah.utils.ZipUtils;

/**
 * @author David Mascharka
 *
 * Displays a list of data files that are downloaded on the device
 *
 * Allows the user to select a file or multiple files for processing and visualizing
 */
public class DataSelectionActivity extends AppCompatActivity{

    private DataAdapter dataFileAdapter;
    private ArrayList<DataEntry> dataFileNames;
    private ListView fileList;
    private ArrayList<String> checkedFilePaths;
    private boolean plottingPressed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_data_selection);

        fileList = (ListView) findViewById(R.id.data_list);

        checkedFilePaths = new ArrayList<String>();

        dataFileNames = new ArrayList<DataEntry>();
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
                        dataFileNames.add(new DataEntry(f));
                    }
                }
            }
        }

        dataFileAdapter = new DataAdapter(this, dataFileNames);
        fileList.setAdapter(dataFileAdapter);

        fileList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String filePath = ((TextView) view.getTag(R.id.data_file_path)).getText().toString();
                CheckBox checkBox = (CheckBox) view.getTag(R.id.data_check);
                checkBox.setChecked(!checkBox.isChecked());

                if (checkBox.isChecked()) {
                    checkedFilePaths.add(filePath);
                } else {
                    checkedFilePaths.remove(filePath);
                }
            }
        });

        ((Button) findViewById(R.id.button_plot_data)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (plottingPressed) {
                    return;
                }

                plottingPressed = true;

                (new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Bundle extras = new Bundle();

                        // If there are zip files (from the Trimble receivers), we need to unzip those and add
                        // the actual file (.15o) to the path list
                        ArrayList<String> actualFilePaths = new ArrayList<String>(checkedFilePaths.size());

                        // The navigation file to use for the data
                        String brdcFile;
                        String ionexFile;
                        int dayOfYear = 0;
                        String yearPart = "";

                        for (String filePath : checkedFilePaths) {
                            String fileName = filePath.substring(filePath.lastIndexOf("/"));
                            if (filePath.contains(".zip")) {
                                ZipUtils.unzip(filePath.substring(0, filePath.lastIndexOf("/") + 1), fileName);
                                // Trimble data
                                actualFilePaths.add(filePath.replace(".RINEX.2.11.zip", ".15O"));

                                int year;
                                int month;
                                int day;
                                ;
                                year = Integer.parseInt(fileName.substring(11, 15));
                                yearPart = fileName.substring(13, 15);
                                month = Integer.parseInt(fileName.substring(15, 17));
                                day = Integer.parseInt(fileName.substring(17, 19));

                                Calendar cal = Calendar.getInstance();
                                dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
                            } else {
                                actualFilePaths.add(filePath);
                                String[] fileParts = filePath.split("_");
                                yearPart = fileParts[1].substring(2, 4);
                                dayOfYear = Integer.parseInt(fileParts[2]);
                            }
                        }

                        if (dayOfYear < 10) {
                            brdcFile = Environment.getExternalStorageDirectory().getAbsolutePath() +
                                    "/mahali/nav/brdc00" + dayOfYear + "0." + yearPart + "n";
                            ionexFile = Environment.getExternalStorageDirectory().getAbsolutePath() +
                                    "/mahali/ionex/jplg00" + dayOfYear + "0." + yearPart + "i";
                        } else if (dayOfYear < 100) {
                            brdcFile = Environment.getExternalStorageDirectory().getAbsolutePath() +
                                    "/mahali/nav/brdc0" + dayOfYear + "0." + yearPart + "n";
                            ionexFile = Environment.getExternalStorageDirectory().getAbsolutePath() +
                                    "/mahali/ionex/jplg0" + dayOfYear + "0." + yearPart + "i";
                        } else {
                            brdcFile = Environment.getExternalStorageDirectory().getAbsolutePath() +
                                    "/mahali/nav/brdc" + dayOfYear + "0." + yearPart + "n";
                            ionexFile = Environment.getExternalStorageDirectory().getAbsolutePath() +
                                    "/mahali/ionex/jplg" + dayOfYear + "0." + yearPart + "i";
                        }

                        extras.putString("brdcFilePath", brdcFile);
                        extras.putString("ionexFilePath", ionexFile);

                        extras.putStringArrayList(DataProcessFragment.FILE_PATH_KEY, actualFilePaths);
                        extras.putInt(DataProcessFragment.DATA_DENSITY_KEY, 1);
                        extras.putInt(DataProcessFragment.PARSER_THREAD_KEY, 4);
                        extras.putString(DataProcessFragment.SHARED_PREFERENCES_KEY, "MahaliPrefs");

                        Intent intent = new Intent(getApplicationContext(), MahaliDataViewActivity.class);
                        intent.putExtras(extras);
                        startActivity(intent);
                    }
                })).start();
            }
        });

        plottingPressed = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        plottingPressed = false;
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

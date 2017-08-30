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
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.DownloadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

import edu.mit.haystack.mahalirelayapp.R;
import edu.mit.haystack.mahalirelayapp.computation.dataselection.DataSelectionActivity;
import edu.mit.haystack.mahalirelayapp.heatmap.HeatmapActivity;
import edu.mit.haystack.mahalirelayapp.position.PositionDialogFragment;
import edu.mit.haystack.mcheetah.utils.network.FileDownloadHelper;

/**
 * @author David Mascharka
 *
 * Downloads new navigation files, ionex files, and list of uploaded files from the MIT Haystack Fermi server
 */
public class SatelliteUpdateActivity extends AppCompatActivity implements OnRemoteOperationListener,
        OnDatatransferProgressListener {

    private FileDownloadHelper downloadHelper;
    private Handler dataTransferHandler;
    private TextView statusText;
    private TextView logText;
    private boolean downloadedBRDC;
    private boolean downloadedIonex;
    private PriorityQueue<String> downloadQueue;
    private static final String NAV_DIRECTORY = Environment.getExternalStorageDirectory().getAbsolutePath() +
            "/mahali/nav/";
    private static final String IONEX_DIRECTORY = Environment.getExternalStorageDirectory().getAbsolutePath() +
            "/mahali/ionex/";
    private int numFilesToDownload;
    private int numFilesDownloaded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_update);

        dataTransferHandler = new Handler();

        downloadHelper = new FileDownloadHelper(this, dataTransferHandler,
                getString(R.string.owncloud_fermi_directory_root),
                getString(R.string.owncloud_fermi_username),
                getString(R.string.owncloud_fermi_password));

        statusText = (TextView) findViewById(R.id.update_text_status);
        logText = (TextView) findViewById(R.id.update_text_log);

        downloadedBRDC = false;
        downloadedIonex = false;

        downloadQueue = new PriorityQueue<String>(10, new NavigationComparator());

        ((Button) findViewById(R.id.update_button_download)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadHelper.getFileList(getString(R.string.owncloud_fermi_directory_navigation));
                statusText.setText("Status: getting navigation file list");
            }
        });
    }

    @Override
    public void onRemoteOperationFinish(RemoteOperation remoteOperation, RemoteOperationResult remoteOperationResult) {
        if (remoteOperation instanceof ReadRemoteFolderOperation) {
            if (remoteOperationResult.isSuccess()) {
                if (!downloadedBRDC) {
                    // we haven't already started downloading BRDC files - start that now
                    downloadedBRDC = true;

                    numFilesToDownload = remoteOperationResult.getData().size();
                    numFilesDownloaded = 0;
                    for (Object o : remoteOperationResult.getData()) {
                        RemoteFile file = (RemoteFile) o;

                        if (!file.getRemotePath().contains("brdc")) {
                            // not a navigation file
                            continue;
                        }

                        downloadQueue.add(file.getRemotePath());
                    }

                    statusText.setText("Status: got file list. Downloading navigation files");

                    checkFiles(new File(NAV_DIRECTORY));

                    if (downloadQueue.size() > 0) {
                        downloadHelper.downloadFile(new RemoteFile(downloadQueue.remove()),
                                new File(Environment.getExternalStorageDirectory().getAbsolutePath()));
                    } else {
                        statusText.setText("Status: downloading ionex file list");
                        downloadHelper.getFileList(getString(R.string.owncloud_fermi_directory_ionex));
                    }
                } else if (!downloadedIonex) {
                    downloadQueue = new PriorityQueue<String>(10, new IonexComparator());
                    // we finished downloading BRDC files already but we haven't done the ionex files
                    downloadedIonex = true;
                    numFilesDownloaded = remoteOperationResult.getData().size();
                    numFilesDownloaded = 0;
                    for (Object o : remoteOperationResult.getData()) {
                        RemoteFile file = (RemoteFile) o;

                        if (!file.getRemotePath().contains("jplg")) {
                            // not an ionex file
                            continue;
                        }

                        downloadQueue.add(file.getRemotePath());
                    }

                    statusText.setText("Status: got file list. Downloading ionex files");

                    checkFiles(new File(IONEX_DIRECTORY));

                    if (downloadQueue.size() > 0) {
                        downloadHelper.downloadFile(new RemoteFile(downloadQueue.remove()),
                                new File(Environment.getExternalStorageDirectory().getAbsolutePath()));
                    } else {
                        statusText.setText("Status: downloading rinex file list");
                        downloadHelper.getFileList(getString(R.string.owncloud_fermi_directory_rinex));
                    }
                } else {
                    // we finished with the BRDC files and the ionex files - get file list so we don't later
                    // download files that are already uploaded to the server
                    File uploadDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                            "/mahali/log/");
                    uploadDir.mkdirs();

                    File uploadLog = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                            "/mahali/log/upload_log.txt");
                    try {
                        // for each box that has data - want to go into box/rinex/ and get the file list there
                        FileOutputStream ostream = new FileOutputStream(uploadLog);
                        PrintWriter writer = new PrintWriter(ostream);

                        for (Object o : remoteOperationResult.getData()) {
                            RemoteFile f = (RemoteFile) o;
                            writer.println(f.getRemotePath());
                        }
                        writer.flush();
                        writer.close();

                        ostream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    statusText.setText("Status: finished");
                    // here we could use this result to potentially delete old files off the Mahali boxes
                }
            } else {
                // try again
                if (!downloadedBRDC) {
                    // we haven't started downloading BRDC files so the read that failed was getting that file list
                    downloadHelper.getFileList(getString(R.string.owncloud_fermi_directory_navigation));
                } else if (!downloadedIonex) {
                    // we haven't started downloading ionex files so that was the read that failed
                    downloadHelper.getFileList(getString(R.string.owncloud_fermi_directory_ionex));
                } else {
                    //we've already downloaded the BRDC and ionex files so it was reading the rinex files that failed
                    downloadHelper.getFileList(getString(R.string.owncloud_fermi_directory_rinex));
                }
            }
        } else if (remoteOperation instanceof DownloadRemoteFileOperation) {
            if (remoteOperationResult.isSuccess()) {
                // download succeeded, update user and download the next file
                numFilesDownloaded++;
                statusText.setText("Status: downloaded " + numFilesDownloaded + "/" + numFilesToDownload);
                logText.setText(logText.getText() + "Downloaded " + remoteOperationResult.getFileName() + "\n");

                if (downloadQueue.size() == 0) {
                    if (!downloadedIonex) {
                        statusText.setText("Status: downloading ionex file list");
                        downloadHelper.getFileList(getString(R.string.owncloud_fermi_directory_ionex));
                    } else {
                        statusText.setText("Status: downloading rinex file list");
                        downloadHelper.getFileList(getString(R.string.owncloud_fermi_directory_rinex));
                    }
                } else {
                    downloadHelper.downloadFile(new RemoteFile(downloadQueue.remove()),
                            new File(Environment.getExternalStorageDirectory().getAbsolutePath()));
                }
            } else {
                // Retry
                downloadHelper.retry((DownloadRemoteFileOperation) remoteOperation);
            }
        }
    }

    /**
     * Make sure we're not downloading files we already have
     */
    private void checkFiles(File directory) {
        // This will happen the first time the app is run (and if the user deletes the folders)
        if (!directory.exists()) {
            directory.mkdirs();
        }

        Iterator<String> iter = downloadQueue.iterator();
        while (iter.hasNext()) {
            String fileName = iter.next();
            for (File f : directory.listFiles()) {
                if (fileName.contains(f.getName())) {
                    iter.remove();
                    numFilesDownloaded++;
                    break;
                }
            }
        }
    }

    @Override
    public void onTransferProgress(long l, long l1, long l2, String s) { /* Don't update the user */}

    /**
     * We want to get the latest navigation files first
     */
    private class NavigationComparator implements Comparator<String> {
        /**
         * Compare two BRDC navigation files to download the latest one first
         * The latest one will return a negative value because it's being used in a PriorityQueue.
         * See the comparator in Mahali.java for comments on that
         *
         * @param first the first BRDC file
         * @param second the second BRDC file
         * @return the order, prioritizing the latest file (closest to the current time)
         */
        @Override
        public int compare(String first, String second) {
            // A file looks like brdc1710.15n
            // This is a brdc navigation file from day 171 of the year, year 2015
            // Something like brdc1630.14n would be from day 163 of 2014
            String firstFileYear;
            String secondFileYear;
            try {
                firstFileYear = first.substring(first.lastIndexOf(".") + 1, first.lastIndexOf("n"));
                secondFileYear = second.substring(second.lastIndexOf(".") + 1, second.lastIndexOf("n"));
            } catch (Exception e) {
                // file name is wrong
                e.printStackTrace();
                return 0;
            }

            int firstYear;
            int secondYear;

            try {
                firstYear = Integer.parseInt(firstFileYear);
                secondYear = Integer.parseInt(secondFileYear);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                // don't crash
                return 0;
            }

            if (firstYear < secondYear) {
                return 1;
            } else if (firstYear > secondYear) {
                return -1;
            }

            // If we're here, both files have the same year

            // We should be able to just do substring(4, 8) but just in case there's a file name that's got
            // some garbage at the beginning this should *hopefully* catch that. This will fail if the file
            // doesn't have the day of the year come directly after `brdc' as in brdc1760.15n
            String firstFileDayOfYear;
            String secondFileDayOfYear;
            try {
                firstFileDayOfYear = first.substring(first.lastIndexOf("c") + 1, first.lastIndexOf("."));
                secondFileDayOfYear = second.substring(second.lastIndexOf("c") + 1, second.lastIndexOf("."));
            } catch (Exception e) {
                // file name is wrong
                e.printStackTrace();
                return 0;
            }

            int firstDayOfYear;
            int secondDayOfYear;

            try {
                firstDayOfYear = Integer.parseInt(firstFileDayOfYear);
                secondDayOfYear = Integer.parseInt(secondFileDayOfYear);
            } catch (NumberFormatException e) {
                // oops
                e.printStackTrace();
                return 0;
            }

            if (firstDayOfYear < secondDayOfYear) {
                return 1;
            } else if (firstDayOfYear > secondDayOfYear) {
                return -1;
            }

            // We shouldn't get here because if we do both files are the same day
            return 0;
        }
    }

    /**
     * We want to get the latest ionex files first
     */
    private class IonexComparator implements Comparator<String> {
        /**
         * Compare two ionex bias files to download the latest one first
         * The latest one will return a negative value because it's being used in a PriorityQueue.
         * See the comparator in Mahali.java for comments on that
         *
         * @param first the first ionex file
         * @param second the second ionex file
         * @return the order, prioritizing the latest file (closest to the current time)
         */
        @Override
        public int compare(String first, String second) {
            // An ionex file looks like jplg0770.15i
            // This is an ionex file from the 77th day of 2015

            String firstFileYear;
            String secondFileYear;

            try {
                firstFileYear = first.substring(first.lastIndexOf(".")+1, first.lastIndexOf("i"));
                secondFileYear = second.substring(second.lastIndexOf("."+1), second.lastIndexOf("i"));
            } catch (Exception e) {
                // weird file name
                return 0;
            }

            int firstYear;
            int secondYear;

            try {
                firstYear = Integer.parseInt(firstFileYear);
                secondYear = Integer.parseInt(secondFileYear);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return 0;
            }

            if (firstYear < secondYear) {
                return 1;
            } else if (firstYear > secondYear) {
                return 0;
            }

            // years are the same
            String firstFileDayOfYear;
            String secondFileDayOfYear;

            try {
                firstFileDayOfYear = first.substring(first.lastIndexOf("g")+1, first.lastIndexOf("."));
                secondFileDayOfYear = second.substring(second.lastIndexOf("g")+1, second.lastIndexOf("."));
            } catch (Exception e) {
                // weird file name
                return 0;
            }

            int firstDayOfYear;
            int secondDayOfYear;

            try {
                firstDayOfYear = Integer.parseInt(firstFileDayOfYear);
                secondDayOfYear = Integer.parseInt(secondFileDayOfYear);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return 0;
            }

            if (firstDayOfYear < secondDayOfYear) {
                return 1;
            } else if (firstDayOfYear > secondDayOfYear) {
                return -1;
            }

            // we should never get here because there's only 1 file for each day
            return 0;
        }
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
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

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;

import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.DownloadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

import edu.mit.haystack.mahalirelayapp.computation.dataselection.DataSelectionActivity;
import edu.mit.haystack.mahalirelayapp.heatmap.HeatmapActivity;
import edu.mit.haystack.mahalirelayapp.position.PositionDialogFragment;
import edu.mit.haystack.mcheetah.utils.network.FileDownloadHelper;

public class MahaliRelayApp extends AppCompatActivity implements OnRemoteOperationListener, OnDatatransferProgressListener {

    /**
     * Helper to download files from the Mahali box
     */
    private FileDownloadHelper downloadHelper;

    /**
     * Handler we need to pass to the downloadHelper so the FileDownloadHelper and ownCloud have a handle to
     * the UI thread (this)
     */
    private Handler dataTransferHandler;

    /**
     * Notifies the user of the current status of the application - scanning, connected, downloading files
     */
    private TextView statusText;

    /**
     * The number of files available to download from this Mahali box
     */
    private int fileCount;

    /**
     * The number of files we have download from this Mahali box in this session
     */
    private int numDownloads;

    /**
     * Priority queue for downloading files - prioritized by latest first
     */
    private PriorityQueue<String> downloadQueue;

    /**
     * Queue for fetching file lists from the Mahali box
     */
    private ArrayDeque<String> directoryQueue;

    /**
     * List containing the directories we've already fetched
     */
    private LinkedList<String> fetchedAlreadyList;

    /**
     * Directory to download data into
     */
    private File targetDirectory;

    /**
     * The Mahali box we're currently connected to
     */
    private String currentBox;

    /**
     * Whether the application is paused and should not download files right now
     */
    private boolean pauseDownloads;

    /**
     * Lets us get connection information so we know what box we're connected to, etc
     */
    private WifiManager wifiManager;

    /**
     * Gives access to Dropbox for uploading files
     */
    DropboxAPI<AndroidAuthSession> dropboxAPI;

    /**
     * How many files there are to upload
     */
    private int numFilesToUpload;

    /**
     * How many files we've uploaded already
     */
    private int numFilesUploaded;

    /**
     * Number of megabytes uploaded
     */
    private float numMBUploaded;

    /**
     * Number of megabytes downloaded
     */
    private float numMBDownloaded;

    /**
     * For printing decimals nicely
     */
    private DecimalFormat numberFormatter;

    private PriorityQueue<String> uploadQueue;

    private Thread uploadWorkerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_mahali_relay_app);
    }

    /**
     * Start getting the list of files on the Mahali box
     * <p/>
     * Recursively searches through directories in the ownCloud server starting with /rinex/
     * Adds files in any subdirectory of rinex to the list of files to download
     */
    @SuppressWarnings("deprecation")
    private void fetchFileList() {
        numMBDownloaded = 0;
        numMBUploaded = 0;
        fileCount = 0;
        directoryQueue = new ArrayDeque<String>();
        fetchedAlreadyList = new LinkedList<String>();

        numDownloads = 0;

        String ip = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        String ssid = wifiManager.getConnectionInfo().getSSID();
        setCurrentConnection(ssid);

        downloadHelper = new FileDownloadHelper(this, dataTransferHandler, "http://" +
                ip.substring(0, ip.lastIndexOf(".") + 1) + "1/owncloud",
                getString(R.string.owncloud_mahali_username),
                getString(R.string.owncloud_mahali_password));

        currentBox = ssid.toLowerCase().replace("\"", "");

        // Now get the rinex data
        setStatus("Status: getting file list");
        downloadHelper.getFileList(currentBox + getString(R.string.mahali_rinex_directory));
    }

    private void setStatus(String status) {
        ((TextView) findViewById(R.id.text_status)).setText(status);
    }

    private void setCurrentFileText(String currentFile) {
        ((TextView) findViewById(R.id.text_current_file)).setText("Current file: " + currentFile);
    }

    private void setCurrentConnection(String connection) {
        ((TextView) findViewById(R.id.text_current_connection)).setText("Current connection: " + connection);
    }

    private void setDownloadStats(int numFilesDownloaded, int numFilesToDownload, float numMB) {
        ((TextView) findViewById(R.id.text_download_stats)).setText("Files / MB Downloaded: " +
                                numFilesDownloaded + " / " + numFilesToDownload + " (" +
                                numberFormatter.format(numMB) + "MB)");
    }

    private void setUploadStats(int numFilesUploaded, int numFilesToUpload, float numMB) {
        ((TextView) findViewById(R.id.text_upload_stats)).setText("Files / MB Uploaded: " +
                numFilesUploaded + " / " + numFilesToUpload + " (" + numberFormatter.format(numMB) + "MB)");
    }

    private void updateStorageAvailable() {
        File externalStorage = Environment.getExternalStorageDirectory();
        long freeSpace = externalStorage.getFreeSpace();
        long totalSpace = externalStorage.getTotalSpace();

        // convert to megabytes
        freeSpace = freeSpace/1024/1024;
        totalSpace = totalSpace/1024/1024;

        final long used = totalSpace-freeSpace;
        final long free = freeSpace;
        final long total = totalSpace;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) findViewById(R.id.text_local_storage)).setText("Local storage used (MB used/free/total): " +
                                                                                used + " / " + free + " / " + total);
            }
        });
    }

    /**
     * Once the file list is complete, this should be called to initiate file downloading
     */
    private void downloadFiles() {
        if (downloadQueue.size() > 0) {
            String s = downloadQueue.remove();
            downloadHelper.downloadFile(new RemoteFile(s), targetDirectory);
        }
    }

    private void uploadFiles() {
        updateStorageAvailable();
        uploadWorkerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!uploadQueue.isEmpty()) {
                    if (Thread.interrupted()) {
                        return;
                    }

                    // Get the upload path -> if none set, default to a catchall /mahali-data/alaska/app-phone/
                    String path = getSharedPreferences(getString(R.string.shared_preferences_key),
                                        MODE_PRIVATE).getString(getString(R.string.shared_preferences_dropbbox_dir),
                            getString(R.string.dropbox_target_directory_default));

                    String filePath = uploadQueue.remove();
                    final File fileToUpload = new File(filePath);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setStatus("Uploading");
                            setCurrentFileText(fileToUpload.getName());
                            setUploadStats(numFilesUploaded, numFilesToUpload, numMBUploaded);
                        }
                    });

                    try {
                        DropboxAPI.Entry e = dropboxAPI.metadata(path + filePath.substring(filePath.lastIndexOf(getString(R.string.mahali_ssid_prefix).toLowerCase())),
                                                 1, null, false, null);
                        if (!e.isDeleted) {
                            // if no exception, the file already exists
                            finishedWithFile(filePath);
                            numFilesUploaded++;
                            continue;
                        }
                    } catch (Exception e) {
                        // do nothing -> will fall through and upload the file
                    }

                    try {
                        FileInputStream inputStream = new FileInputStream(fileToUpload);

                        dropboxAPI.putFile(path + filePath.substring(filePath.lastIndexOf(
                                        getString(R.string.mahali_ssid_prefix).toLowerCase())),
                                inputStream, fileToUpload.length(), null, null);

                        numMBUploaded += fileToUpload.length()/1024.0/1024.0;
                        numFilesUploaded++;
                        finishedWithFile(filePath);
                    } catch (Exception e) {
                        e.printStackTrace();
                        uploadQueue.add(filePath);
                    }
                }
            }
        });
        uploadWorkerThread.start();
    }

    private void finishedWithFile(final String filePath) {
        // when we're done uploading a file, update the upload log and delete the file
        (new Thread(new Runnable() {
            @Override
            public void run() {
                File f = new File(filePath);
                File uploadLog = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                                            getString(R.string.local_log_path) + "upload_log.txt");

                File uploadDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                                            getString(R.string.local_log_path));
                uploadDir.mkdirs();

                try {
                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                                                            new FileOutputStream(uploadLog, true)));
                    writer.println(f.getAbsolutePath());
                    writer.flush();
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (f.exists()) {
                    f.delete();
                }
                updateStorageAvailable();
            }
        })).start();
    }

    private void stopUploads() {
        if (uploadWorkerThread != null) {
            uploadWorkerThread.interrupt();
        }
    }

    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, final RemoteOperationResult result) {

        if (operation instanceof ReadRemoteFolderOperation) {
            if (result.isSuccess()) {
                (new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (Object o : result.getData()) {
                            RemoteFile remoteFile = (RemoteFile) o;
                            String remotePath = remoteFile.getRemotePath();

                            // If this is a directory and is NOT a dotfile
                            if (remoteFile.getMimeType().equals("DIR") &&
                                    !fetchedAlreadyList.contains(remotePath) &&
                                    !remotePath.contains(FileUtils.PATH_SEPARATOR + ".")) {
                                // this is a directory
                                directoryQueue.add(remotePath);
                                fetchedAlreadyList.add(remotePath);
                            } else if (!remoteFile.getMimeType().equals("DIR")) {
                                fileCount++;
                                // this is not a directory -> it is a normal file

                                // If we've already downloaded the file, don't download it again
                                if (alreadyDownloaded(remotePath.substring(remotePath.lastIndexOf(FileUtils.PATH_SEPARATOR) + 1))) {
                                    numDownloads++;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            setDownloadStats(numDownloads, fileCount, numMBDownloaded);
                                        }
                                    });
                                } else {
                                    downloadQueue.add(remotePath);
                                }
                            }
                        }

                        if (directoryQueue.isEmpty()) {
                            downloadFiles();
                        } else {
                            downloadHelper.getFileList(directoryQueue.remove());
                        }
                    }

                    /**
                     * Checks whether the file has already been uploaded to the server and whether the file is
                     * on the device already
                     *
                     * @param fileName the file to check
                     * @return whether the file has been uploaded or is already on the device
                     */
                    private boolean alreadyDownloaded(String fileName) {
                        File uploadLog = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                                "/mahali/log/upload_log.txt");

                        if (!uploadLog.exists()) {
                            return false;
                        }

                        // Check all the files that have been uploaded to the server from this box
                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(uploadLog)));

                            String line = "";
                            while ((line = reader.readLine()) != null) {
                                if (line.contains(fileName)) {
                                    return true;
                                }
                            }
                        } catch (Exception e) {
                            // If we can't read the file, do nothing instead of crashing
                            e.printStackTrace();
                        }

                        // Check the files on the device currently from this box
                        // Files are stored at /sdcard/mahali/BOX_NAME/rinex/data_file.txt
                        // Where BOX_NAME is the name of the Mahali box (ie. mahali-0001)
                        // For example, /sdcard/mahali/mahali-0001/dataout_2015_150_0000.txt
                        File onDevice = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/mahali/" +
                                currentBox);
                        Queue<File> directoriesToCheck = new ArrayDeque<File>();
                        directoriesToCheck.add(onDevice);

                        while (directoriesToCheck.size() > 0) {
                            File currentDirectory = directoriesToCheck.remove();

                            if (!currentDirectory.exists() || currentDirectory.listFiles() == null) {
                                return false;
                            }

                            for (File f : currentDirectory.listFiles()) {
                                if (f.isDirectory()) {
                                    directoriesToCheck.add(f);
                                } else {
                                    if (f.getName().contains(fileName)) {
                                        return true;
                                    }
                                }
                            }
                        }
                        return false;
                    }
                })).start();
            } else {
                setStatus("Status: getting file list");
                downloadHelper.retry((ReadRemoteFolderOperation) operation);
            }
        }

        if (operation instanceof DownloadRemoteFileOperation) {
            if (result.isSuccess()) {
                updateStorageAvailable();
                numDownloads++;

                File onDevice = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/mahali/" +
                        currentBox);

                String fileName = result.getFileName().substring(result.getFileName().lastIndexOf(FileUtils.PATH_SEPARATOR)+1);

                Queue<File> directoriesToCheck = new ArrayDeque<File>();
                directoriesToCheck.add(onDevice);

                while (directoriesToCheck.size() > 0) {
                    File currentDirectory = directoriesToCheck.remove();

                    if (currentDirectory.exists() && currentDirectory.listFiles() != null) {
                        for (File f : currentDirectory.listFiles()) {
                            if (f.isDirectory()) {
                                directoriesToCheck.add(f);
                            } else {
                                if (f.getName().contains(fileName)) {
                                    numMBDownloaded += f.length()/1024.0/1024.0;
                                }
                            }
                        }
                    }
                }
                setDownloadStats(numDownloads, fileCount, numMBDownloaded);

                if (numDownloads == fileCount) {
                    // we downloaded all the available files
                    setStatus("Status: finished downloading files");
                }
            } else {
                downloadQueue.add(result.getFileName());
            }

            if (downloadQueue.size() > 0) {
                String file = downloadQueue.remove();
                downloadHelper.downloadFile(new RemoteFile(file), targetDirectory);
                setCurrentFileText(file.substring(file.lastIndexOf(FileUtils.PATH_SEPARATOR)));
            }
        }
    }

    @Override
    public void onTransferProgress(long rate, long transferredSoFar, long totalToTransfer, final String fileName) {
        if (transferredSoFar >= totalToTransfer) {
            // The file finished downloading, update displayed content
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    /* Could notify the user here */
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        numberFormatter = new DecimalFormat();
        numberFormatter.setMinimumFractionDigits(1);
        numberFormatter.setMaximumFractionDigits(1);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Create a new handler associated with the UI thread for the uploader and downloader
        dataTransferHandler = new Handler();

        // Initialize the network helper
        //networkHelper = new UserNetworkHelper(this);

        targetDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/mahali");

        downloadQueue = new PriorityQueue<String>(10, new FileComparator());

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        AppKeyPair appKeyPair = new AppKeyPair(getString(R.string.dropbox_app_key),
                getString(R.string.dropbox_app_secret));
        AndroidAuthSession session = new AndroidAuthSession(appKeyPair, getString(R.string.dropbox_access_token));
        dropboxAPI = new DropboxAPI<AndroidAuthSession>(session);

        ((Button) findViewById(R.id.button_download_data)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchFileList();
                setStatus("Status: getting file list");
            }
        });

        File rootDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/mahali/");
        ArrayDeque<File> directoryQueue = new ArrayDeque<File>();
        directoryQueue.add(rootDirectory);

        final ArrayList<File> filesToUpload = new ArrayList<File>();
        while (!directoryQueue.isEmpty()) {
            File dir = directoryQueue.remove();

            if (dir.exists() && dir.isDirectory()) {
                for (File f : dir.listFiles()) {
                    if (f.isDirectory() &&
                            f.getAbsolutePath().toUpperCase().contains(getString(R.string.mahali_ssid_prefix))) {
                        directoryQueue.add(f);
                    } else if (!f.isDirectory() &&
                            f.getAbsolutePath().toUpperCase().contains(getString(R.string.mahali_ssid_prefix))) {
                        filesToUpload.add(f);
                    }
                }
            }
        }

        numFilesToUpload = filesToUpload.size();

        // Freaks out if there's a capacity of 0 so if there's nothing to upload just make capacity 1
        uploadQueue = new PriorityQueue<String>(Math.max(1, numFilesToUpload), new FileComparator());

        ((Button) findViewById(R.id.button_upload_data)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (File f : filesToUpload) {
                    uploadQueue.add(f.getAbsolutePath());
                }

                uploadFiles();
            }
        });

        ((Button) findViewById(R.id.button_pause_activities)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseFileOperations();
            }
        });

        String ssid = wifiManager.getConnectionInfo().getSSID();
        if (ssid != null) {
            setCurrentConnection(ssid);
        }

        setCurrentFileText("");
        setStatus("");
        setDownloadStats(0, 0, 0);
        setUploadStats(0, 0, 0);
        updateStorageAvailable();
    }

    @Override
    protected void onPause() {
        super.onPause();

        pauseFileOperations();
    }

    private void pauseFileOperations() {
        stopUploads();
        pauseDownloads = true;
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

    private class FileComparator implements Comparator<String> {

        /**
         * Compares 2 file names to determine which is the latest one
         * We want the latest data so we can check tha the Mahali box is behaving well *now*
         * <p/>
         * This will be used in a PriorityQueue so the latest thing (closest to the present time)
         * should be least-order, not highest order so if an element x has a timestamp before an
         * element y, then we should return 1 since x comes before y. It's a bit backwards
         *
         * @param x first file name
         * @param y second file name
         * @return priority order for file downloads, lowest value is the most recent file
         */
        @Override
        public int compare(String x, String y) {
            // todo fix the file names in the trimbles - the zip files have different conventions

            // Until the naming conventions are sorted out, don't prioritize one file over another
            return 0;
            /*String[] xDates = x.split("_");
            String[] yDates = y.split("_");

            int xYear = Integer.parseInt(xDates[1]);
            int yYear = Integer.parseInt(yDates[1]);

            if (xYear < yYear) {
                return 1;
            } else if (xYear > yYear) {
                return -1;
            }

            // If we're here, both files have the same year
            int xDayOfYear = Integer.parseInt(xDates[2]);
            int yDayOfYear = Integer.parseInt(yDates[2]);

            if (xDayOfYear < yDayOfYear) {
                return 1;
            } else if (xDayOfYear > yDayOfYear) {
                return -1;
            }

            // If we're here, both files have the same day of the year
            int xTimeOfDay = Integer.parseInt(xDates[3].substring(0, xDates[3].indexOf(".")-1));
            int yTimeOfDay = Integer.parseInt(yDates[3].substring(0, yDates[3].indexOf(".")-1));

            if (xTimeOfDay < yTimeOfDay) {
                return 1;
            } else if (xTimeOfDay > yTimeOfDay) {
                return -1;
            }

            // If we're here, that's really strange and should never happen because both files are the same time
            return 0;*/
        }
    }
}
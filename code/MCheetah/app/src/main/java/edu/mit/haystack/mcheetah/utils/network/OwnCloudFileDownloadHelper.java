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

import android.content.Context;
import android.net.Uri;
import android.os.Handler;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.resources.files.DownloadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;

import java.io.File;

/**
 * @author David Mascharka
 *
 * Helper class to download files and fetch a list of files from a server
 *
 * A class using this must implement OnRemoteOperationListener to get file operation results and must
 * implement OnDatatransferProgressListener to see progress updates
 */
public class OwnCloudFileDownloadHelper {

    /**
     * The ownCloud instance for performing the data transfer
     */
    private OwnCloudClient dataTransferClient;

    /**
     * This is passed in from the parent. The owner Context lets this helper class communicate data
     * transfer events back to its parent for displaying updates to the user
     */
    private Context owner;

    /**
     * The Handler gives this helper a Handler tied to the UI thread for event updates
     */
    private Handler dataTransferHandler;

    /**
     * Constructor
     *
     * @param context the owner MUST implement OnRemoteOperationListener AND OnDatatransferProgressListener
     * @param handler a Handler instance so this has a Handler back to the UI thread
     * @param server the server's root URL
     * @param username the username to log in
     * @param password the user's password
     */
    public OwnCloudFileDownloadHelper(Context context, Handler handler, String server, String username, String password) {
        // Set up the client
        Uri serverURL = Uri.parse(server);

        // Tell the client to connect to the server using the owner as a listener and following redirects
        dataTransferClient = OwnCloudClientFactory.createOwnCloudClient(serverURL, context, true);
        dataTransferClient.setCredentials(OwnCloudCredentialsFactory.newBasicCredentials(username, password));

        owner = context;

        // Results will be communicated back to owner so make sure that owner is an OnRemoteOperationListener
        try {
            OnRemoteOperationListener listener = (OnRemoteOperationListener) owner;
        } catch (ClassCastException e) {
            throw new ClassCastException(owner.toString() + " must implement OnRemoteOperationListener " +
                    "to get results back from the FileDownloadHelper");
        }

        // Make sure the owner is an OnDataTransferProgressListener
        try {
            OnDatatransferProgressListener listener = (OnDatatransferProgressListener) owner;
        } catch (ClassCastException e) {
            throw new ClassCastException(owner.toString() + " must implement OnDatatransferProgressListener " +
                    "to get proress updates from the FileDownloadHelper");
        }

        dataTransferHandler = handler;
    }

    /**
     * Downloads a file from the server
     *
     * @param fileToDownload the file to download
     * @param targetDirectory the directory to store the file to on the device
     */
    public void downloadFile(RemoteFile fileToDownload, File targetDirectory) {
        DownloadRemoteFileOperation downloadOperation = new DownloadRemoteFileOperation(fileToDownload.getRemotePath(),
                targetDirectory.getAbsolutePath());
        downloadOperation.addDatatransferProgressListener((OnDatatransferProgressListener) owner);
        downloadOperation.execute(dataTransferClient, (OnRemoteOperationListener) owner, dataTransferHandler);
    }

    /**
     * Gets a list of files in the given directory
     *
     * @param directory the directory to read from
     */
    public void getFileList(String directory) {
        ReadRemoteFolderOperation getFileListOperation = new ReadRemoteFolderOperation(directory);
        getFileListOperation.execute(dataTransferClient, (OnRemoteOperationListener) owner, dataTransferHandler);
    }

    public void retry(DownloadRemoteFileOperation operation) {
        operation.execute(dataTransferClient, (OnRemoteOperationListener) owner, dataTransferHandler);
    }

    public void retry(ReadRemoteFolderOperation operation) {
        operation.execute(dataTransferClient, (OnRemoteOperationListener) owner, dataTransferHandler);
    }
}

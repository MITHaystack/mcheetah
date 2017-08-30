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
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author David Mascharka
 *
 * Helper for uploading and downloading files to/from DropBox
 */
public class DropBoxHelper {

    /**
     * Gives access to DropBox for uploading files
     */
    private DropboxAPI<AndroidAuthSession> dropboxAPI;

    /**
     * Runs file transfer to avoid long-running operations on the UI thread
     */
    private ExecutorService fileTransferService;

    /**
     * The directory to upload the file to on DropBox
     */
    private String dropboxDestinationDir;

    private Context context;

    /**
     * Create a helper for uploading and downloading files to/from DropBox using an access token
     *
     * @param appKey the app key provided when creating the application on DropBox
     * @param appSecret the app secret provided when creating the application on DropBox
     * @param accessToken the access token given by DropBox
     */
    public DropBoxHelper(String appKey, String appSecret, String accessToken) {
        fileTransferService = Executors.newFixedThreadPool(1);

        AppKeyPair appKeyPair = new AppKeyPair(appKey, appSecret);
        AndroidAuthSession session = new AndroidAuthSession(appKeyPair, accessToken);
        dropboxAPI = new DropboxAPI<>(session);
    }

    /**
     * Create a helper for uploading and downloading files to/from DropBox by having the user log in
     *
     * @param appKey the app key provided when creating the application on DropBox
     * @param appSecret the app secret provided when creating the application on DropBox
     */
    public DropBoxHelper(Context context, String appKey, String appSecret) {
        this.context = context;
        fileTransferService = Executors.newFixedThreadPool(1);

        AppKeyPair appKeyPair = new AppKeyPair(appKey, appSecret);
        AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
        dropboxAPI = new DropboxAPI<AndroidAuthSession>(session);
        dropboxAPI.getSession().startOAuth2Authentication(context);
    }

    /**
     * Called by the parent activity when the authentication is complete
     *
     * NOTE: You MUST call this method in onResume of your activity if you want to authenticate using
     * a username/password combination instead of an app key/secret
     */
    public void sessionInitiated() {
        if (dropboxAPI.getSession().authenticationSuccessful()) {
            try {
                dropboxAPI.getSession().finishAuthentication();

                String accessToken = dropboxAPI.getSession().getOAuth2AccessToken();
            } catch (IllegalStateException e) {
                Toast.makeText(context, "Error authenticating", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Uploads a given file
     *
     * First checks to see if the file exists
     *      If it does, this returns
     *      If it does not, it uploads the file
     *
     * @param file file to upload
     * @param dropboxFileName the name of the file on DropBox
     * @return whether the upload was successful
     */
    private Future<Boolean> upload(final File file, final String dropboxFileName) {
        return fileTransferService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                try {
                    DropboxAPI.Entry entry = dropboxAPI.metadata(dropboxDestinationDir + file.getName(), 1, null, false, null);

                    // If the file exists and isn't deleted, the file already exists - don't upload again
                    if (!entry.isDeleted) {
                        return true;
                    }
                } catch (Exception e) {
                    // do nothing -  falls through and uploads file
                }

                try {
                    FileInputStream inputStream = new FileInputStream(file);
                    dropboxAPI.putFile(dropboxDestinationDir + dropboxFileName, inputStream, file.length(), null, null);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        });
    }

    /**
     * Downloads a given file
     *
     * @param dropboxFile file to download
     * @param dropboxFileName name of the file on DropBox
     * @param localFile where to download the file
     * @return whether the download was successful
     */
    private Future<Boolean> download(final File dropboxFile, final String dropboxFileName, final File localFile) {
        return fileTransferService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                try {
                    FileOutputStream outputStream = new FileOutputStream(localFile);
                    DropboxAPI.DropboxFileInfo info = dropboxAPI.getFile(dropboxFileName, null, outputStream, null);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        });
    }
}

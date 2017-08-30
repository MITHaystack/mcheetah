package edu.mit.haystack.magnetometerapp;

import android.Manifest;
import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.util.Calendar;

/**
 * @author David Mascharka
 *
 * This class handles all the data collection. It is the background service that collects data
 * and writes it to a file on the device
 */
public class LoggerService extends IntentService {

    public LoggerService() {
        super("Logger Service");
    }

    /**
     * This is called from the worker thread, so we can just do work auatomatically from here
     * @param intent the intent that is passed to this class - will be used to collect data
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        DataLogger logger = new DataLogger(this, prefs.getFloat("t_dataSample", 1000), prefs.getFloat("t_gpsSample", 10*60*1000));

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 55);
        long endTime = cal.getTimeInMillis();

        File dataFile = logger.createFile(cal.get(Calendar.YEAR) + "_" +
                            (cal.get(Calendar.MONTH)+1) + "_" + cal.get(Calendar.DAY_OF_MONTH));

        // Start recording magnetometer and gps data, then sleep until it's time to end for the day
        logger.startMagnetometer(dataFile);
        logger.startGPS(dataFile);
        try { Thread.sleep(endTime - System.currentTimeMillis()); } catch (Exception e) { e.printStackTrace(); }
        logger.stopGPS();
        logger.recordMagnetometer(dataFile);
    }
}
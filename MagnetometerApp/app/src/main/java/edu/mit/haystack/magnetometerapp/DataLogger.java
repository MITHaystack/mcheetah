package edu.mit.haystack.magnetometerapp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.security.Security;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @author David Mascharka
 *
 * Logs magnetometer data into a file
 */
public class DataLogger implements SensorEventListener {

    private Context context;

    private static Calendar calendar;

    private float t_dataSample;
    private boolean collectingData;
    private float magNumSamples;
    float x;
    private float magX;
    private float magXAvg;
    private float magXMin;
    private float magXMax;
    float y;
    private float magY;
    private float magYAvg;
    private float magYMin;
    private float magYMax;
    float z;
    private float magZ;
    private float magZAvg;
    private float magZMin;
    private float magZMax;

    private float t_gpsSample;
    private double gpsLat;
    private double gpsLong;
    private double gpsAlt;

    private LocationManager manager;
    private LocationListener listener;
    private Location location;

    private SensorManager sensorManager;

    private Thread magnetometerThread = null;
    private Thread gpsThread = null;

    // used to jitter sleep times for data and GPS so they both end up writing
    // in case GPS and data both try collecting data at the same interval, adds a random amount to
    // offset them a little bit
    private Random jitter;

    public DataLogger(Context context, float dataInterval, float gpsInterval) {
        this.context = context;

        calendar = Calendar.getInstance();

        jitter = new Random();

        manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateLocation(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) { /* Do nothing */ }

            @Override
            public void onProviderEnabled(String provider) { /* Do nothing */ }

            @Override
            public void onProviderDisabled(String provider) { /* Do nothing */ }
        };

        try {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
            manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
        } catch (SecurityException e) {
            Toast.makeText(context, "Requires Location Permission", Toast.LENGTH_LONG).show();
        }

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(this,
                                        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                                        SensorManager.SENSOR_DELAY_FASTEST);
        collectingData = false;

        t_dataSample = dataInterval;
        t_gpsSample = gpsInterval;
    }

    @Override
    public void onSensorChanged (SensorEvent event){
        if (collectingData && event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];
            if (x > magXMax) {
                magXMax = x;
            }
            if (y > magYMax) {
                magYMax = y;
            }
            if (z > magZMax) {
                magZMax = z;
            }
            if (x < magXMin) {
                magXMin = x;
            }
            if (y < magYMin) {
                magYMin = y;
            }
            if (z < magZMin) {
                magZMin = z;
            }
            magNumSamples++;
            magX += x;
            magY += y;
            magZ += z;
        }
    }

    @Override
    public void onAccuracyChanged (Sensor sensor,int accuracy){ /* Do nothing */ }

    private void updateLocation(Location loc) {
        location = loc;
    }

    public File createFile(String name) {
        File root = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + context.getString(R.string.file_directory));
        root.mkdirs();
        File dataFile = new File(Environment.getExternalStorageDirectory() + context.getString(R.string.file_directory)
                                + name + ".txt");

        SensorManager manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        String device = Build.MANUFACTURER + " " + Build.MODEL;
        String magnetometerName;

        if (manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) {
            Sensor magnetometer = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            magnetometerName = magnetometer.getName() + " vendor: " + magnetometer.getVendor() + " version: " +
                    magnetometer.getVersion() + " power: " + magnetometer.getPower() + "mA resolution: " +
                    magnetometer.getResolution() + " uT max range: " + magnetometer.getMaximumRange();
        } else {
            magnetometerName = "null";
        }

        try {
            FileOutputStream outputStream = new FileOutputStream(dataFile, true);
            PrintWriter writer = new PrintWriter(outputStream);
            writer.write(device);
            writer.write("\n");
            writer.write(magnetometerName);
            writer.write("\n");
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return dataFile;
    }

    public void startMagnetometer(final File dataFile) {
        magnetometerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // The thread will be interrupted in recordMagnetometer()
                // Will be called by the service when it's time to reset
                while (!Thread.interrupted()) {
                    collectingData = true;
                    try {
                        Thread.sleep((long) t_dataSample + jitter.nextInt(100));
                        recordMagnetometer(dataFile);
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        });
        magnetometerThread.start();
    }

    public void recordMagnetometer(File dataFile) {
        magnetometerThread.interrupt();
        magnetometerThread = null;
        collectingData = false;

        magXAvg = magX / magNumSamples;
        magYAvg = magY / magNumSamples;
        magZAvg = magZ / magZAvg;

        synchronized (this) {
            try {
                FileWriter writer = new FileWriter(dataFile, true);
                calendar.setTimeInMillis(System.currentTimeMillis());
                String theMagnetometer = "D, " + calendar.getTime().toString() + ", " + t_dataSample +
                                            ", " + magNumSamples + ", " + magXAvg + ", " + magXMin +
                                            ", " + magXMax + ", " + + magYAvg + ", " + magYMin + ", " +
                                            magYMax + ", " + magZAvg + ", " + magZMin + ", " + magZMax;
                writer.write(theMagnetometer);
                writer.write("\n");
                writer.flush();
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        magNumSamples = 0;
        magX = 0;
        magXAvg = 0;
        magXMin = 0;
        magXMax = 0;
        magY = 0;
        magYAvg = 0;
        magYMin = 0;
        magYMax = 0;
        magZ = 0;
        magZAvg = 0;
        magZMin = 0;
        magZMax = 0;

        // restart the data collection
        startMagnetometer(dataFile);
    }

    public void startGPS(final File dataFile) {
        gpsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    if (location == null) {
                        continue;
                    }
                    gpsLat = location.getLatitude();
                    gpsLong = location.getLongitude();
                    gpsAlt = location.getAltitude();

                    synchronized (this) {
                        try {
                            FileWriter writer = new FileWriter(dataFile, true);
                            calendar.setTimeInMillis(System.currentTimeMillis());
                            String theGPS = "G, " + calendar.getTime().toString() + ", " + t_gpsSample + gpsLat +
                                    ", " + gpsLong + ", " + gpsAlt + "\n";
                            writer.write(theGPS);
                            writer.flush();
                            writer.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    try {
                        Thread.sleep((long) t_gpsSample + jitter.nextInt(100));
                    } catch (Exception e) {
                        e.printStackTrace();
                    };
                }
            }
        });
        gpsThread.start();
    }

    public void stopGPS() {
        gpsThread.interrupt();
        try {
            manager.removeUpdates(listener);
        } catch (SecurityException e) {
            // do nothing - we don't have permission to record GPS
        }
        gpsThread = null;
    }
}

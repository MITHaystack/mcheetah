package edu.mit.haystack.mahalirelayapp.computation;

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
import android.provider.ContactsContract;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import edu.mit.haystack.mahalirelayapp.MahaliData;
import edu.mit.haystack.mahalirelayapp.MahaliRelayApp;
import edu.mit.haystack.mahalirelayapp.R;
import edu.mit.haystack.mahalirelayapp.computation.dataselection.DataSelectionActivity;
import edu.mit.haystack.mahalirelayapp.rinex.GPSObservation;
import edu.mit.haystack.mahalirelayapp.rinex.RinexObservationParser;
import edu.mit.haystack.mcheetah.DataProcessFragment;
import edu.mit.haystack.mcheetah.parsing.PointSkipSelectionDialogFragment;
import edu.mit.haystack.mcheetah.visualization.Renderer;

/**
 * @author David Mascharka
 *
 * Displays a TEC plot of slant TEC from a given RINEX observation observationFile
 */
public class MahaliDataViewActivity extends AppCompatActivity implements DataProcessFragment.ActivityEndRequestedListener,
        DataProcessFragment.DisplayPointDensityListener, Renderer.BadDataListener {

    private DataProcessFragment<GPSObservation> datafragment;
    private TECRenderer renderer;
    private TECComputer computer;
    private List<GPSObservation> mahaliData;
    private boolean plotFinished;

    private Thread biasUpdater;
    private boolean updatingBias;
    private double receiverBias;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        plotFinished = false;

        setContentView(R.layout.activity_tec_plot);

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            // this should never happen - this shouldn't start without an intent, which should
            // always have a bundle with it
            Log.wtf("Mahali", "The extras bundle is null in MahaliDataViewActivity");
            Toast.makeText(this, "Encountered an error", Toast.LENGTH_LONG).show();
            endSelf();
        }

        // Find the retained fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        datafragment = (DataProcessFragment<GPSObservation>) fragmentManager.findFragmentByTag("TECPlot");

        if (datafragment == null) {
            TECRenderer renderer = new TECRenderer(this);

            datafragment = new DataProcessFragment<GPSObservation>();
            datafragment.setParserClass(RinexObservationParser.class);

            TECComputer computer = new TECComputer();

            // 2 is a reasonable number for most devices
            // Could add a setting to change the number of threads
            computer.setComputeThreads(2);

            File brdcFile = new File(extras.getString("brdcFilePath"));
            if (brdcFile.exists()) {
                computer.setEphemerides(brdcFile);
                renderer.plotVertical = true;
            } else {
                Toast.makeText(this, "No nav file: plotting slant TEC", Toast.LENGTH_SHORT).show();
                renderer.plotVertical = false;
            }
            File ioenxFile = new File(extras.getString("ionexFilePath"));
            computer.setIonexFile(ioenxFile);

            datafragment.setComputer(computer);
            ArrayList<GPSObservation> myData = new ArrayList<>();
            datafragment.setDataObject(myData);
            datafragment.setRenderer(renderer);
            getSupportFragmentManager().beginTransaction().replace(R.id.tec_plot_root, datafragment, "TECPlot").commit();
        } else {
            getSupportFragmentManager().beginTransaction().replace(R.id.tec_plot_root, datafragment, "TECPlot").commit();
            datafragment.redisplay();
        }

        (new Thread(new Runnable() {
            @Override
            public void run() {
                while (!datafragment.isFinished()) {
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (datafragment.isFinished()) {
                    plotFinished();
                }
            }
        })).start();

        ((SeekBar) findViewById(R.id.plot_bar_bias)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (plotFinished) {
                    updateBias(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private double newBias = 0;
    private double originalBias = 0;
    private void updateBias(float bias) {
        if (!updatingBias) {
            updatingBias = true;
            originalBias = newBias;
            newBias = receiverBias + (bias-100);
            try {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        DecimalFormat formatter = new DecimalFormat();
                        formatter.setMaximumFractionDigits(2);
                        formatter.setMinimumFractionDigits(2);
                        ((TextView) findViewById(R.id.plot_text_bias)).setText("Bias: " + formatter.format(newBias));
                    }
                });
            } catch (NullPointerException e) {
                e.printStackTrace();
            }

            biasUpdater = new Thread(new Runnable() {
                @Override
                public void run() {
                    int size = mahaliData.size();
                    GPSObservation o;
                    for (int i = size-1; i >= 0; i--) {
                        o = mahaliData.get(i);
                        o.slantTEC = o.slantTEC - newBias + originalBias;
                        TECComputer.applyMappingFunction(o);
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            renderer.update(mahaliData);
                            updatingBias = false;
                        }
                    });
                }
            });

            biasUpdater.start();
        }
    }

    /**
     * When plotting is finished, get the data objects here to allow bias udpates
     */
    private void plotFinished() {
        renderer = (TECRenderer) datafragment.getRenderer();
        computer = (TECComputer) datafragment.getComputer();
        mahaliData = (List<GPSObservation>) datafragment.getDataObject();
        receiverBias = MahaliData.mahaliReceiverBias;

        plotFinished = true;
    }

    /**
     * This morbid-sounding function kills this activity and returns to the main activity
     *
     * Called in case all data is bad or there is an error reading a file so that we don't crash
     * Because ending yourself is better than crashing
     */
    private void endSelf() {
        Intent intent = new Intent(this, DataSelectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onEndRequested() {
        endSelf();
    }

    @Override
    public void displayPointDensityDialog() {
        PointSkipSelectionDialogFragment f = new PointSkipSelectionDialogFragment();
        f.setListener(datafragment);
        f.show(getSupportFragmentManager(), "Point Density");
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

        return super.onOptionsItemSelected(item);
    }

    /**
     * Called if all the data is bad - end the activity and return to MahaliActivity
     */
    @Override
    public void badData() {
        Toast.makeText(this, "Data was bad", Toast.LENGTH_LONG).show();
        endSelf();
    }
}
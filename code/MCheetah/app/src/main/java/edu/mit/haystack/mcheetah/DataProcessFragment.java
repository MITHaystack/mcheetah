package edu.mit.haystack.mcheetah;

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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.mit.haystack.mcheetah.parsing.Parser;
import edu.mit.haystack.mcheetah.parsing.PointSkipSelectionDialogFragment;
import edu.mit.haystack.mcheetah.visualization.DataView;
import edu.mit.haystack.mcheetah.visualization.OutlierRemovalSelectionDialogFragment;
import edu.mit.haystack.mcheetah.visualization.Renderer;

/**
 * @author David Mascharka
 *
 * This is where the magic happens
 * This class combines the Parser, Computer, and Renderer for processing and displaying scientific data
 *
 * To use, set the parser, computer, and renderer to your implementations, then the DataProcessFragment
 * to a blank activity. Pass data to this and parsing, computation, and rendering will be handled automatically
 * on a background thread.
 *
 * Override this class or modify it to provide custom details for your specific use case
 *
 * The type D is the data type this will process, and must be the same across the computer and parser
 */
public class DataProcessFragment<D> extends Fragment implements
        PointSkipSelectionDialogFragment.PointSkipSelectionDialogListener,
        OutlierRemovalSelectionDialogFragment.OutlierRemovalDialogListener {

    /**
     * Constant name for the Bundle key containing an ArrayList<String> of file paths to process
     */
    public static final String FILE_PATH_KEY = "file_names";

    /**
     * Constant name for the Bundle key containing an int specifying the data density for parsing
     */
    public static final String DATA_DENSITY_KEY = "data_density";

    /**
     * Constant name for the Bundle key containing an int specifying the number of threads to use for parsing
     */
    public static final String PARSER_THREAD_KEY = "parser_thread_count";

    /**
     * Constant name for the Bundle key containing the SharedPreferences object name
     */
    public static final String SHARED_PREFERENCES_KEY = "shared_prefs";

    /**
     * The name of the SharedPreferences object so this can read stored data
     */
    private String sharedPrefsName;

    /**
     * Handle to the SharedPreferences object
     */
    private SharedPreferences prefs;

    /**
     * Allows for writing to the SharedPreferences object
     */
    private SharedPreferences.Editor prefsEditor;

    /**
     * Parser object class
     *
     * Must be set in setParserClass(Parser)
     *
     * A Class object is passed because we create the actual parsers in the parse method with a threadpool
     */
    private Class parserClass;

    /**
     * Compute object
     *
     * Must be set in setComputer(Computer)
     */
    private Computer<D> computer;

    /**
     * Renderer object
     *
     * Must be set in setRenderer(Renderer)
     */
    private Renderer<D> renderer;

    /**
     * The data object that will be processed
     */
    private List<D> dataObject;

    /**
     * Holds the view that will house the plot
     */
    private DataView<D> dataView;

    /**
     * The data files to be processed
     */
    private File[] dataFiles;

    /**
     * The density of the data the user wants to parse (every point, every other point, every tenth point, ...)
     */
    private int dataDensity;

    /**
     * If the application is restarting, we've already computed all the values
     * This prevents us from recalculating everything on screen orientation changes
     */
    private boolean restarting;

    /**
     * ThreadPool for parsing data files
     */
    private ExecutorService parserPool;

    /**
     * Whether the computation and plot display is finished
     */
    private boolean finished;

    /**
     * When the user selects a data density, compute and plot that data
     *
     * @param density the density of data to use
     */
    @Override
    public void onSkipSelected(int density) {
        dataDensity = density;

        (new Thread(new Runnable() {
            @Override
            public void run() {
                parse();
                compute();
                plot();
            }
        })).start();
    }

    /**
     * Clip the plot bounds to the user-provided min and max y value
     *
     * @param minValue the minimum value to display
     * @param maxValue the maximum value to display
     */
    @Override
    public void onOutlierValuesSelected(float minValue, float maxValue) {
        dataView.setMinMax(minValue, maxValue);
    }

    /**
     * Called when the application should display a PointDensityDialog to let the user pick a data density
     */
    public interface DisplayPointDensityListener {
        public void displayPointDensityDialog();
    }

    /**
     * Listener for when the point density has been set
     */
    private DisplayPointDensityListener pointDensityListener;

    /**
     * Called when the application should end due to something like no good data
     */
    public interface ActivityEndRequestedListener {
        public void onEndRequested();
    }

    /**
     * Listener for when the application should end
     */
    private ActivityEndRequestedListener activityEndRequestedListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        // When this view is created, it is not restarting
        restarting = false;

        finished = false;

        // We should allow the user to select a data density when we start
        // When this is clicked, computation begins
        pointDensityListener.displayPointDensityDialog();

        Bundle extra = getActivity().getIntent().getExtras();
        dataDensity = extra.getInt(DATA_DENSITY_KEY);
        ArrayList<String> filePaths = extra.getStringArrayList(FILE_PATH_KEY);

        if (filePaths == null) {
            // this should never happen
            Log.wtf("Mahali", "File paths is null in DataProcessFragment");
            Toast.makeText(getActivity(), "Encountered an error", Toast.LENGTH_SHORT).show();
            activityEndRequestedListener.onEndRequested();
        }

        int numDataFiles = filePaths.size();
        // No data, quit
        if (numDataFiles <= 0) {
            activityEndRequestedListener.onEndRequested();
        }

        dataFiles = new File[numDataFiles];
        for (int i = 0; i < numDataFiles; i++) {
            dataFiles[i] = new File(filePaths.get(i));
        }

        sharedPrefsName = extra.getString(SHARED_PREFERENCES_KEY);
        prefs = getActivity().getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE);

        parserPool = Executors.newFixedThreadPool(extra.getInt(PARSER_THREAD_KEY));
    }

    /**
     * Called when the activity is recreated due to an orientation change
     * Avoids the work of calculating everything again - just save state and restore
     */
    public void redisplay() {
        restarting = true;
        finished = false;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            pointDensityListener = (DisplayPointDensityListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.getLocalClassName() + " must implement DisplayPointDensityListener");
        }

        try {
            activityEndRequestedListener = (ActivityEndRequestedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.getLocalClassName() + " must implement ActivityEndRequestedListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_data_process, container, false);

        if (restarting) {
            dataView = new DataView<D>(getActivity());
            dataView.setMyRenderer(renderer);
            renderer.addData(dataObject);

            if (view != null) {
                LinearLayout plot = (LinearLayout) view.findViewById(R.id.data_plot);
                if (plot != null) {
                    plot.addView(dataView);
                }
            } else {
                Toast.makeText(getActivity(), "Error making plot", Toast.LENGTH_LONG).show();
                activityEndRequestedListener.onEndRequested();
            }
        }

        return view;
    }

    /**
     * Set the parsing class, which will be used in the parse() method to create Parsers for each file
     * @param p the class of the Parser
     */
    public void setParserClass(Class p) {
        parserClass = p;
    }

    /**
     * Set the computing object, which will be used in the compute() method to perform the main
     * computation we're interested in
     *
     * @param c the Computer object
     */
    public void setComputer(Computer<D> c) {
        computer = c;
    }

    /**
     * Get the computing object, which may hold data an application wants to access
     *
     * @return the Computer object
     */
    public Computer<D> getComputer() {
        return computer;
    }

    /**
     * Set the renderer, which will be used to display the computed values to the user
     *
     * @param r the renderer to use
     */
    public void setRenderer(Renderer<D> r) {
        renderer = r;

    }

    /**
     * Get the renderer object, which may hold data an application wants
     *
     * @return the renderer object
     */
    public Renderer<D> getRenderer() {
        return renderer;
    }

    /**
     * Set the list of data objects we're interested in - used to store all results from parsing
     * in a fork-join pattern (lists are joined into dataObject), passed to the Computer for
     * computation, then passed to the Renderer to display to the user
     *
     * @param d the data object, a list of type D, data
     */
    public void setDataObject(List<D> d) {
        dataObject = d;
    }

    /**
     * Get the list of data, which an application may want
     *
     * @return the list of data objects
     */
    public List<D> getDataObject() {
        return dataObject;
    }

    public boolean isFinished() {
        return finished;
    }

    /**
     * Parses all given data files
     */
    @SuppressWarnings("unchecked")
    private void parse() {
        // Get the number of data files
        int size = dataFiles.length;

        // Create an array of Futures for however many data files are given, which will hold the results
        // of parsing each file
        Future[] futures = new Future[size];

        // Create an array of booleans indicating whether each future has finished already
        boolean[] finishedAlready = new boolean[size];

        // Are we done with all the files?
        boolean doneParsing = false;

        // Loop through all the data files, add a Future to the array for each data files, and
        // initialize the boolean array, since nothing has finished yet
        for (int i = 0; i < size; i++) {
            futures[i] = addFileToParse(dataFiles[i]);
            finishedAlready[i] = false;
        }

        // Until all the files have been parsed
        while (!doneParsing) {
            doneParsing = true;

            // Loop through all the files
            for (int i = 0; i < size; i++) {
                // If a Future is finished and wasn't before (it just finished parsing)
                if (futures[i].isDone() && !finishedAlready[i]) {
                    // it's finished now
                    finishedAlready[i] = true;
                    try {
                        // This parser just finished, add all its data to dataObject
                        dataObject.addAll((List<D>) futures[i].get());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    // Hint to the system that this might be a good time to collect garbage to
                    // clear any objects the parser might have created
                    System.gc();
                }
                if (!futures[i].isDone()) {
                    doneParsing = false;
                }
            }

            try {
                Thread.sleep(10);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Add a data file for parsing and submit it to the parser pool to begin parsing
     *
     * @param f the file to parse
     * @return a Future object which will return a list of data objects
     */
    @SuppressWarnings("unchecked")
    private Future<List<D>> addFileToParse(final File f) {
        return parserPool.submit(new Callable<List<D>>() {
            @Override
            public List<D> call() {
                try {
                    // Use reflection here because we just have a Class object and need to cast
                    // it to a Parser<D>, which needs to be initialized with the application context
                    Parser<D> parser = (Parser<D>) parserClass.getDeclaredConstructor(Context.class)
                            .newInstance(getActivity());

                    return parser.parse(f, dataDensity);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });
    }

    /**
     * Performs the main computation
     */
    private void compute() {
        computer.compute(dataObject);
    }

    /**
     * Displays a plot of the data
     */
    private void plot() {
        if (getActivity() != null) {
            // We need to add the plot on the UI thread because we're touching Views, which is
            // only allowed on the UI thread
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dataView = new DataView<D>(getActivity());
                    dataView.setMyRenderer(renderer);
                    renderer.addData(dataObject);
                    View thisView = getView();
                    if (thisView != null) {
                        LinearLayout plot = (LinearLayout) thisView.findViewById(R.id.data_plot);
                        if (plot != null) {
                            plot.addView(dataView);
                            finished = true;
                        }
                    } else {
                        Toast.makeText(getActivity(), "Error making plot", Toast.LENGTH_LONG).show();
                        activityEndRequestedListener.onEndRequested();
                    }
                }
            });
        }
    }
}
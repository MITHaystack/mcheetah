package edu.mit.haystack.mcheetah.autotune;

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

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.mit.haystack.mcheetah.parsing.Parser;

/**
 * @author David Mascharka
 *
 * This class encapsulates the computation logic that is the same as DataProcessFragment
 * Runs computation on different thread configurations to find the best configuration for performance
 */
public class Autotuner<D> {

    private Activity parentActivity;

    private ExecutorService parserPool;

    public Autotuner(Activity parent) {
        parentActivity = parent;
    }

    public static int getNumCores() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static long currentStackSize() {
        return Runtime.getRuntime().freeMemory();
    }

    public static long maxStackSize() {
        return Runtime.getRuntime().maxMemory();
    }

    /**
     * Performs parsing to get the optimal number of threads
     *
     * @param dataFiles the files to parse
     * @param parserClass the custom Parser for the data type
     * @param dataObject the data type list
     * @param minCores the minimum number of cores to try
     * @param maxCores the maximum number of cores to try
     * @param repetitions the number of repetitions to perform parsing
     * @return the optimal number of cores for parsing
     */
    @SuppressWarnings("unchecked")
    public int getBestPerformanceThreadsParsing(File[] dataFiles, Class parserClass, List<D> dataObject,
                                         int minCores, int maxCores, int repetitions) {
        // Indicate it would be a good time to perform garbage collection to free some space before
        // the benchmark
        System.gc();

        if (maxCores < minCores || repetitions == 0 || dataFiles.length == 0) {
            return 1;
        }

        int bestCores = -1;
        long bestAvgTime = Long.MAX_VALUE;

        long startTime;
        long finishTime;

        for (int i = minCores; i <= maxCores; i++) {
            dataObject.clear();
            long avgTime = 0;
            parserPool = Executors.newFixedThreadPool(i);

            startTime = System.currentTimeMillis();
            for (int rep = 0; rep < repetitions; rep++) {
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
                for (int k = 0; k < size; k++) {
                    futures[k] = addFileToParse(dataFiles[k], parserClass);
                    finishedAlready[k] = false;
                }

                // Until all the files have been parsed
                while (!doneParsing) {
                    doneParsing = true;

                    // Loop through all the files
                    for (int k = 0; k < size; k++) {
                        // If a Future is finished and wasn't before (it just finished parsing)
                        if (futures[k].isDone() && !finishedAlready[k]) {
                            // it's finished now
                            finishedAlready[k] = true;
                            try {
                                // This parser just finished, add all its data to dataObject
                                dataObject.addAll((List<D>) futures[k].get());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            // Hint to the system that this might be a good time to collect garbage to
                            // clear any objects the parser might have created
                            System.gc();
                        }
                        if (!futures[k].isDone()) {
                            doneParsing = false;
                        }
                    }

                    try {
                        Thread.sleep(10);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                finishTime = System.currentTimeMillis();
                avgTime += (finishTime-startTime);
            }

            avgTime /= repetitions;
            if (avgTime < bestAvgTime) {
                bestAvgTime = avgTime;
                bestCores = i;
            }
            parserPool.shutdown();
            while (!parserPool.isTerminated()) {
                try { Thread.sleep(10); } catch (Exception e) { e.printStackTrace(); }
            }
        }

        return bestCores;
    }

    /**
     * Add a data file for parsing and submit it to the parser pool to begin parsing
     *
     * @param f the file to parse
     * @return a Future object which will return a list of data objects
     */
    @SuppressWarnings("unchecked")
    private Future<List<D>> addFileToParse(final File f, final Class parserClass) {
        return parserPool.submit(new Callable<List<D>>() {
            @Override
            public List<D> call() {
                try {
                    // Use reflection here because we just have a Class object and need to cast
                    // it to a Parser<D>, which needs to be initialized with the application context
                    Parser<D> parser = (Parser<D>) parserClass.getDeclaredConstructor(Context.class)
                            .newInstance(parentActivity);

                    return parser.parse(f, 1);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });
    }
}
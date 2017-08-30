package edu.mit.haystack.example.mcheetah;

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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.mit.haystack.mcheetah.Computer;

/**
 * @author David Mascharka
 *
 * Simple Computer class that squares the y coordinate and subtracts 1 from the x coordinate
 *
 * Parallelized to show the ease of parallelism in MCheetah
 */
public class ExampleComputer implements Computer<ExampleData> {

    /**
     * We'll perform computation in batches of 20 ExampleData objects
     */
    private static final int BATCH_SIZE = 20;
    /**
     * Pool for running computation on batches of ExampleData objects
     */
    private ExecutorService computePool;

    @Override
    public boolean compute(List<ExampleData> data) {
        // If the number of compute threads wasn't set, default to 2 threads
        if (computePool == null) {
            computePool = Executors.newFixedThreadPool(2);
        }

        // Loop through all the data, pull batches of points, and submit them for computation
        int size = data.size();
        for (int i = 0; i < size; i += BATCH_SIZE) {
            ExampleData[] theDataPoints = new ExampleData[BATCH_SIZE];
            for (int j = 0; j < BATCH_SIZE; j++) {
                if (i + j >= size) {
                    break;
                }

                theDataPoints[j] = data.get(i+j);
            }
            performComputationOnBatch(theDataPoints);
        }

        // Calling shutdown stops jobs from being submitted
        computePool.shutdown();
        // Until all batches are finished, just wait
        while (!computePool.isTerminated()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    /**
     * Take a batch of ExampleData points and perform the computation on them
     *
     * Square the y coordinate and subtract 1 from the x coordinate of each point
     *
     * @param points the batch of points
     */
    private void performComputationOnBatch(final ExampleData[] points) {
        computePool.submit(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < points.length; i++) {
                    if (points[i] == null) {
                        break;
                    }

                    points[i].theYCoordinate = points[i].theYCoordinate*points[i].theYCoordinate;
                    points[i].theXCoordinate -= 1;
                }
            }
        });
    }

    /**
     * Set the number of threads to use in computation
     *
     * @param threads the number of threads to use for computation
     */
    public void setComputeThreads(int threads) {
        computePool = Executors.newFixedThreadPool(threads);
    }
}

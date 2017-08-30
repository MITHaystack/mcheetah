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

import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.mit.haystack.mahalirelayapp.MahaliData;
import edu.mit.haystack.mahalirelayapp.rinex.GPSEphemeris;
import edu.mit.haystack.mahalirelayapp.rinex.GPSObservation;
import edu.mit.haystack.mahalirelayapp.rinex.IonexParser;
import edu.mit.haystack.mahalirelayapp.rinex.MahaliObservation;
import edu.mit.haystack.mahalirelayapp.rinex.RinexNavigationParser;

import edu.mit.haystack.mcheetah.Computer;
import edu.mit.haystack.mcheetah.utils.ParserUtils;

/**
 * @author David Mascharka
 *
 * Handles the computation of TEC from GPS observation data
 *
 * NOTE: WHEN THIS IS FIRST CREATED IN YOUR ACTIVITY, CALL setEphemerides AND setReceiverPosition
 * BEFORE ADDING THE DataProcessFragment THAT USES THIS CLASS
 */
public class TECComputer implements Computer<GPSObservation> {

    /**
     * The coefficient of the time gap - a difference of more than the median timestep * GAP indicates a gap
     */
    private static final byte GAP = 3;

    /**
     * The max difference between consecutive TEC values to indicate a gap, probably due to a loss of lock
     */
    private static final byte MAX_DIFFERENCE_TEC_VALUE = 1;

    private static double[] kVector = new double[3];

    private static int computeThreads = 1;

    /**
     * ThreadPool for converting batches of GPSObservations from slant to vertical TEC in parallel
     */
    private static ExecutorService pool;

    private List<GPSEphemeris> ephemerides;

    private IonexParser ionexParser;

    public void setEphemerides(List<GPSEphemeris> e) {
        ephemerides = e;
    }

    public void setEphemerides(final File navigationFile) {
        (new Thread(new Runnable() {
            @Override
            public void run() {
                RinexNavigationParser p = new RinexNavigationParser();
                setEphemerides(p.parse(navigationFile, 1));
            }
        })).start();
    }

    public void parseIonexFile(File file) {
        ionexParser = new IonexParser();
        ionexParser.parse(file);
    }

    public void setIonexFile(File ionexFile) {
        parseIonexFile(ionexFile);
    }

    public void setComputeThreads(int numThreads) {
        computeThreads = numThreads;
    }

    @Override
    public boolean compute(List<GPSObservation> data) {
        MahaliObservation observation = new MahaliObservation();

        // TODO get rid of these allocations, or make them not hardcoded at least
        parseIonexFile(new File("/sdcard/mahali/ionex/jplg1380.15i"));

        observation.observations.addAll(data);
        observation.receiverX = MahaliData.mahaliX;
        observation.receiverY = MahaliData.mahaliY;
        observation.receiverZ = MahaliData.mahaliZ;

        calculateEverythingAndConvert(observation, ephemerides, ionexParser, computeThreads);
        return true;
    }

    /**
     * Very handy if you just want to do everything all at once
     *
     * Sets the slant TEC of the input observation to the correct TEC, adjusted for satellite and receiver bias
     *
     * Note: Do NOT call this on the UI thread. Performs way too much computation and will crash the app
     *
     * @param mahaliObservation contains the set of observations
     * @param ionex contains satellite biases
     */
    public static void calculateEverything(MahaliObservation mahaliObservation, IonexParser ionex) {
        // First, calculate line-of-sight TEC
        calculateTEC(mahaliObservation.observations);

        // Next, remove satellite biases
        removeSatelliteBiases(ionex, mahaliObservation.observations);

        // Get the receiver bias
        double bias = estimateReceiverBiasZeroTEC(mahaliObservation.observations);
        MahaliData.mahaliReceiverBias = bias;

        // Subtract the bias from the slant TEC
        int size = mahaliObservation.observations.size();
        for (int i = 0; i < size; i++) {
            mahaliObservation.observations.get(i).slantTEC -= bias;
        }
    }

    /**
     * Very handy for calculating absolutely everything at once
     *
     * Sets the vertical TEC of the input observation to the correct value
     *
     * Note: Do NOT call this on the UI thread. Performs way too much computation and will crash the app
     *
     * @param mahaliObservation contains the set of observations
     * @param ephemerides satellite ephemerides for the day the observations were taken
     * @param ionex contains satellite biases
     */
    public static void calculateEverythingAndConvert(MahaliObservation mahaliObservation, List<GPSEphemeris> ephemerides,
                                                     IonexParser ionex, int numThreads) {
        calculateEverything(mahaliObservation, ionex);

        if (ephemerides != null) {
            convertSlantToVerticalTEC(mahaliObservation, ephemerides, 4, numThreads);
        }
    }

    /**
     * Converts line-of-sight TEC to vertical TEC
     *
     * @param mahaliObservation set of GPS observation data
     * @param ephemerides set of satellite ephemeris data
     */
    public static void convertSlantToVerticalTEC(MahaliObservation mahaliObservation, List<GPSEphemeris> ephemerides,
                                                 int batchSize, int poolSize) {
        long end;
        long start = System.currentTimeMillis();
        pool = Executors.newFixedThreadPool(1);
        Collections.sort(ephemerides);

        double[] receiverGeodetic = GPSEphemeris.getLatLongAltFromXYZ(mahaliObservation.receiverX,
                mahaliObservation.receiverY, mahaliObservation.receiverZ);
        double latGeo = ParserUtils.DEGREES_TO_RADIANS*(receiverGeodetic[0]);
        double longGeo = ParserUtils.DEGREES_TO_RADIANS*(receiverGeodetic[1]);

        // Compute k vector in local North-East-Up system
        kVector = new double[] {Math.cos(latGeo)*Math.cos(longGeo),
                Math.cos(latGeo)*Math.sin(longGeo),
                Math.sin(latGeo)};

        System.gc();
        int size = mahaliObservation.observations.size();
        for (int i = 0; i < size; i += batchSize) {
            /*observation = mahaliObservation.observations.get(i);
            ephemeris = getClosestEphemeris(observation, ephemerides);

            computeElevation(observation, ephemeris, mahaliObservation.receiverX, mahaliObservation.receiverY,
                    mahaliObservation.receiverZ);

            observation.verticalTEC = (observation.slantTEC * getOneOverMappingFunction(observation.elevation));*/
            //convertSingleObservation(mahaliObservation.observations.get(i), ephemerides, mahaliObservation.receiverX,
            //            mahaliObservation.receiverY, mahaliObservation.receiverZ);
            GPSObservation[] obs = new GPSObservation[batchSize];
            for (int j = 0; j < obs.length; j++) {
                if (i+j >= size) {
                    break;
                }
                obs[j] = mahaliObservation.observations.get(i+j);
            }
            convertObservations(obs, ephemerides, mahaliObservation.receiverX,
                    mahaliObservation.receiverY, mahaliObservation.receiverZ);
        }

        pool.shutdown();
        while (!pool.isTerminated()) {
            try {
                Thread.sleep(10);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        end = System.currentTimeMillis();
        Log.wtf("TEST", "PERFORMANCE convert: " + (end - start));
    }

    private static void convertObservations(final GPSObservation[] obs, final List<GPSEphemeris> ephemerides,
                                            final double x, final double y, final double z) {
        pool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < obs.length; i++) {
                        if (obs[i] == null) {
                            break;
                        }
                        GPSEphemeris e = getClosestEphemeris(obs[i], ephemerides);
                        computeElevation(obs[i], e, x, y, z);
                        obs[i].verticalTEC = obs[i].slantTEC*getOneOverMappingFunction(obs[i].elevation);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    for (byte i = 0; i < obs.length; i++) {
                        // throw away the batch
                        obs[i].verticalTEC = Integer.MAX_VALUE;
                    }
                }
            }
        });
    }

    /**
     * Applies the mapping function for a given observation
     * Make sure that the slant TEC and elevation are already set before calling this function
     * If elevation isn't set, call convertSlantToVerticalTEC instead
     *
     * @param observation the observation to apply the mapping function to
     */
    public static void applyMappingFunction(GPSObservation observation) {
        observation.verticalTEC = observation.slantTEC * getOneOverMappingFunction(observation.elevation);
    }

    /**
     * Removes satellite biases from the data
     *
     * @param ionexParser contains satellite bias information
     * @param observations set of observations
     */
    public static void removeSatelliteBiases(IonexParser ionexParser, List<GPSObservation> observations) {
        GPSObservation o;
        int size = observations.size();
        for (int i = 0; i < size; i++) {
            o = observations.get(i);
            double satelliteBias = ionexParser.getBias(o.prn);
            o.slantTEC = o.slantTEC - satelliteBias;
        }
    }

    /**
     * Estimates receiver bias assuming that at some point the TEC = 0
     *
     * Checks for outliers by insisting the lowest point is within 1 TECu of 99% lowest point
     *
     * Returns estimated receiver bias in TEC units
     */
    public static double estimateReceiverBiasZeroTEC(List<GPSObservation> observations) {
        int size = observations.size();
        double[] tecList = new double[size];
        for (int i = 0; i < size; i++) {
            tecList[i] = observations.get(i).slantTEC;
        }

        Arrays.sort(tecList);
        double lowestTEC = tecList[0];
        double ninetyNineTEC = tecList[(int) ((size-1)*0.01)];
        if (ninetyNineTEC - lowestTEC > 1.0) {
            return ninetyNineTEC;
        } else {
            return lowestTEC;
        }
    }

    /**
     * Calculates the elevation from a receiver to a satellite
     *
     * Adapted from GPSTk from the Applied Research Laboratory at the University of Texas at Austin
     * http://www.gpstk.org/bin/view/Documentation/WebHome
     *
     * @param observation the observation
     * @param ephemeris the satellite ephemeris data
     * @param receiverX receiver x coordinate in ECEF
     * @param receiverY receiver y coordinate in ECEF
     * @param receiverZ receiver z coordinate in ECEF
     */
    private static void computeElevation(final GPSObservation observation, final GPSEphemeris ephemeris, double receiverX,
                                         double receiverY, double receiverZ) {
        // Compute the satellite's position in ECEF
        double[] satelliteXYZ = ephemeris.getSatelliteXYZ(observation.time);

        double[] vector = new double[3];
        // Get the vector from the satellite to the receiver
        vector[0] = satelliteXYZ[0] - receiverX;
        vector[1] = satelliteXYZ[1] - receiverY;
        vector[2] = satelliteXYZ[2] - receiverZ;

        // get the up coordinate in local north-east-up coordinate system
        double localUp = vector[0]*kVector[0]+vector[1]*kVector[1]+vector[2]*kVector[2];

        // cos(z), z is angle with respect to local vertical
        double cosUp = localUp/Math.sqrt(vector[0]*vector[0]+vector[1]*vector[1]+vector[2]*vector[2]);

        observation.elevation = 90.0 - ParserUtils.RADIANS_TO_DEGREES*Math.acos(cosUp);
    }

    /**
     * Finds the closest satellite ephemeris broadcast to this observation
     * Ensure when calling this that the ephemerides list is sorted
     * This assumes a sorted list to save work
     *
     * @param observation the observation
     * @param ephemerides list of satellite ephemerides
     */
    public static GPSEphemeris getClosestEphemeris(final GPSObservation observation, final List<GPSEphemeris> ephemerides) {
        GPSEphemeris ephemeris = null;
        long closestDifference = Long.MAX_VALUE;
        long thisDifference;

        GPSEphemeris e;
        // We'll never get more than 32,000 ephemeris readings per day. At most we'll have ~1500
        // They broadcast once every half hour for each day from 32 satellites at most
        short size = (short) ephemerides.size();
        for (short i = 0; i < size; i++) {
            e = ephemerides.get(i);
            // If this ephemeris is for a different satellite, keep going
            if (e.prn != observation.prn) {
                // If we've already found an ephemeris that's close for this satellite, we're done because the list is sorted
                // so all PRNs after this will not match
                if (ephemeris != null) {
                    break;
                }
                continue;
            }

            thisDifference = Math.abs(observation.time.getTime() - e.time.getTime());
            if (thisDifference < closestDifference) {
                closestDifference = thisDifference;
                ephemeris = e;
            } else {
                // sorted by time and prn so if we're getting farther away we've found the best one already
                break;
            }
        }

        return ephemeris;
    }

    /**
     * Returns 1/(the mapping function) given elevation in degrees and fitting parameter
     *
     * Mapping function:
     *                     1
     *     z = ---------------------------
     *         sqrt(1.0 - (fit*cos(el))^2)
     *
     * We return 1/z because this way we just multiply and save some work dividing
     *
     * @param elevation the angle of elevation from the receiver to the satellite, in degrees
     * @return the function to map slant to vertical TEC
     */
    private static double getOneOverMappingFunction(double elevation) {
        return Math.sqrt(1.0 - Math.pow(0.95 * Math.cos(ParserUtils.DEGREES_TO_RADIANS*elevation), 2.0));
    }

    /**
     * Computes the total electron content for the set of GPS observations
     *
     * @param observations the observation data
     */
    public static void calculateTEC(List<GPSObservation> observations) {
        Collections.sort(observations);

        List<Integer> timePeriods;
        // At this point, the observations are sorted by PRN and time
        for (byte i = 1; i <= 32; i++) { // there are 32 PRNs, definitely fits in a byte
            timePeriods = analyzeData(i, observations);

            if (timePeriods == null) {
                // no data for this satellite
                continue;
            }

            for (int j = 0; j < timePeriods.size(); j += 2) {
                getRawTEC(timePeriods.get(j), timePeriods.get(j + 1), observations);
            }
        }

        // Remove single points here
        // This is significantly faster than using an iterator
        for (int i = observations.size()-1; i > 0; i--) {
            if (observations.get(i).slantTEC == Integer.MAX_VALUE) {
                observations.remove(i);
            }
        }
    }

    /**
     * Calculates the raw TEC value, ignoring satellite bias
     *
     * @param startIndex the first point in a contiguous time period
     * @param endIndex the end point in a contiguous time period
     * @param observations the GPS observations
     */
    private static void getRawTEC(int startIndex, int endIndex, List<GPSObservation> observations) {
        if (startIndex+1 >= endIndex) { // if there's only 1 point, just return
            return;
        }

        GPSObservation o;
        double[] differentialList = new double[endIndex-startIndex];

        // Find the median difference between the phase and the differential range
        for (int i = startIndex; i < endIndex; i++) {
            o = observations.get(i);
            differentialList[i-startIndex] = (o.phase - o.differentialRange);
        }

        Arrays.sort(differentialList);
        double medianDifference = differentialList[(differentialList.length/2)];
        double distributionWidth = differentialList[((int) (differentialList.length*0.75))] -
                differentialList[((int) (differentialList.length*0.25))];

        // If the sample is too small, set a minimum distribution width
        if (differentialList.length < 6) {
            if (distributionWidth < 20.0) {
                distributionWidth = 20.0;
            }
        }

        double medianError = distributionWidth / Math.sqrt(endIndex - startIndex);

        // TEC is phase - medianDifference
        for (int i = startIndex; i < endIndex; i++) {
            o = observations.get(i);
            o.slantTEC = o.phase - medianDifference;
            o.tecError = medianError;
        }
    }

    /**
     * Returns a list of timesteps that are good (don't have gaps)
     * A gap is a timestep greater than 3 times the median
     * A gap is also a change in phase TEC of more than one TECu (possibly due to a phase slip)
     *
     * @param prn the PRN of the satellite to look at
     * @param data the GPS data
     * @return a list of time series
     */
    private static List<Integer> analyzeData(byte prn, List<GPSObservation> data) {
        long prevTime = -1;
        long thisTime = -1;
        int firstIndex = -1;
        int lastIndex;
        int i;
        GPSObservation thisObs;

        int size = data.size();
        for (i = 0; i < size; i++) {
            thisObs = data.get(i);
            if (thisObs.prn != prn) {
                if (firstIndex != -1) {
                    break;
                }
            } else {
                if (firstIndex == -1) {
                    firstIndex = i;
                }
            }
        }
        lastIndex = i;

        if (firstIndex == -1) {
            return null; // no data
        }

        long[] timeStepList = new long[lastIndex-firstIndex];
        for (i = firstIndex; i < lastIndex; i++) {
            if (prevTime == -1) {
                firstIndex = i;
                prevTime = data.get(i).time.getTime();
                continue;
            }

            thisTime = data.get(i).time.getTime();
            timeStepList[i-firstIndex] = (thisTime - prevTime);

            prevTime = thisTime;
        }

        if (timeStepList.length < 3) {
            return null; // not enough data
        }

        // sort the timesteps
        Arrays.sort(timeStepList);

        long medianTimeStep = timeStepList[(timeStepList.length / 2)];

        // break into time periods - the list here stores startIndex, endIndex, startIndex, endIndex, startIndex, ...
        // Reduces memory consumption from having ArrayList<ArrayList<Integer>> and having start/end timePeriod ArrayLists
        ArrayList<Integer> timePeriodList = new ArrayList<Integer>();
        timePeriodList.add(firstIndex);

        GPSObservation first;
        GPSObservation second;
        for (i = firstIndex; i < lastIndex - 1; i++) {
            first = data.get(i+1);
            second = data.get(i);
            if (first.time.getTime() - second.time.getTime() > medianTimeStep * GAP) {
                // gap found
                timePeriodList.add(i+1);
                timePeriodList.add(i+1);
            } else if (Math.abs(first.phase - second.phase) > MAX_DIFFERENCE_TEC_VALUE) {
                // gap found
                timePeriodList.add(i+1);
                timePeriodList.add(i+1);
            }
        }

        timePeriodList.add(lastIndex);

        return timePeriodList;
    }
}

package edu.mit.haystack.mahalirelayapp.rinex;

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
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import edu.mit.haystack.mahalirelayapp.MahaliData;
import edu.mit.haystack.mcheetah.parsing.Parser;
import edu.mit.haystack.mcheetah.utils.ParserUtils;

/**
 * @author David Mascharka
 *
 * Reads in and parses a RINEX observation file
 */
public class RinexObservationParser implements Parser<GPSObservation> {

    private Context context; // used for communicating results back to the parent

    private static final double L1_VALUE_TO_METERS = 3.0E8/(154.0*10.23E6);
    private static final double L2_VALUE_TO_METERS = 3.0E8/(120.0*10.23E6);
    private static final double F2_F1_FACTOR = 1.545727;
    private static final double METERS_TO_TEC = 6.158;

    public RinexObservationParser(Context context) {
        this.context = context;
    }

    /**
     * Reads in and parses a RINEX observation file
     *
     * @param obsFile the observation file to parse
     * @return whether the file successfully parsed
     */
    public List<GPSObservation> parse(File obsFile, int density) {
        List<GPSObservation> mahaliObservation = new ArrayList<>();

        BufferedReader fileReader = null;
        FileInputStream inputStream = null;
        int observationNumber = 0;

        // Try to read the file. Notify the user if there is an error
        try {
            inputStream = new FileInputStream(obsFile);
        } catch (FileNotFoundException e) {
            Toast.makeText(context.getApplicationContext(), "File not found: " + obsFile.getName(),
                                Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return null;
        }

        // inputStream should never be null here
        try {
            fileReader = new BufferedReader(new InputStreamReader(inputStream));
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        }

        String line = ""; // the contents of the line in the file
        boolean inHeader = true; // are we in the RINEX file header?

        // how many observation types are in the obs file (won't be > 255, ever)
        byte numObservationTypes = 0;

        GPSObservation observation; // an observation
        // Save some space on the date - we know these will fit into these datatypes
        short year;
        byte month;
        byte day;
        byte hour;
        byte minute;
        byte second;
        String[] items; // the items in the current observation
        String prnString; // the string of all the PRNs for the epoch

        byte[] prns;
        byte numObservationsInEpoch; // there are only 32 GPS satellites so this will be 1-32 (really less but 32 is a max)

        Calendar cal = Calendar.getInstance(); // for setting date
        Date observationTime; // cache this so we don't call getTime() so many times

        // Holds the items of the observation for easier indexing (ie using add() instead of a more complex
        // computation when there are multiple lines needed for an observation)
        ArrayList<String> observationItems = new ArrayList<String>();

        // Cache these so we're not re-creating them hundreds of thousands of times
        byte indexL1 = -1;
        byte indexL2 = -1;
        double diffRange;
        double phase;
        boolean diffRangeSet;
        List<String> obsList = null;

        // Cache these to save a bunch of lookups
        boolean hasP1 = false;
        boolean hasP2 = false;

        double l1;
        double l2;

        // Lines are at most 80 characters so we'll ignore anything that goes past that - it'll just be
        // whitespace
        // Using a character array is significantly better on memory than a lot of String manipulation
        // because Java Strings are immutable so there's a lot of object creation when using Strings
        //char[] theLine = new char[80];
        //char[] helper;

        try {
            // Read in the file line-by-line
            while ((line = fileReader.readLine()) != null) {
                if (inHeader) {
                    // contains is fine, doesn't make a new object
                    if (line.contains("APPROX POSITION XYZ")) {
                        // Read in the position of the receiver
                        // If the receiver does not record position information, this will be (0,0,0) and
                        // will be computed
                        String[] positionStr = ParserUtils.splitSpace(line);

                        // In ECEF coordinates
                        MahaliData.mahaliX = ParserUtils.parseDouble(positionStr[0]);
                        MahaliData.mahaliY = ParserUtils.parseDouble(positionStr[1]);
                        MahaliData.mahaliZ = ParserUtils.parseDouble(positionStr[2]);
                    } else if (line.contains("TYPES OF OBSERV")) {
                        items = ParserUtils.splitSpace(line);

                        // find how many observation types there are
                        numObservationTypes = ParserUtils.parseByte(items[0]);

                        obsList = new ArrayList<String>(numObservationTypes);

                        for (byte i = 1; i <= numObservationTypes && i < items.length; i++) {
                            if (items[i].contains("#")) {
                                obsList.add(items[i].substring(0, items[i].indexOf("#")));
                                break;
                            }
                            obsList.add(items[i]);
                        }

                        if (numObservationTypes > 9) {
                            // there's another line of observation stuff
                            line = fileReader.readLine();
                            items = ParserUtils.splitSpace(line);

                            for (byte i = 0; obsList.size() < numObservationTypes && i < items.length; i++) {
                                obsList.add(items[i]);
                            }
                        }

                        // Make sure the required observations are found
                        if (!obsList.contains("L1") || !obsList.contains("L2")) {
                            Toast.makeText(context, "Missing L1 or L2 value: exiting", Toast.LENGTH_SHORT).show();
                            return null;
                        }
                        indexL1 = (byte) obsList.indexOf("L1");
                        indexL2 = (byte) obsList.indexOf("L2");
                        hasP1 = obsList.contains("P1");
                        hasP2 = obsList.contains("P2");

                        if (!obsList.contains("P1") && !obsList.contains("C1")) {
                            Toast.makeText(context, "Missing P1 and C1: exiting", Toast.LENGTH_SHORT).show();
                            return null;
                        }
                        if (!obsList.contains("P2") && !obsList.contains("C2")) {
                            Toast.makeText(context, "Missing P2 and C2: exiting", Toast.LENGTH_SHORT).show();
                            return null;
                        }
                    } else if (line.contains("END OF HEADER")) {
                        inHeader = false;
                    }
                } else {
                    items = ParserUtils.splitSpace(line);
                    year = ParserUtils.parseShort(items[0]);
                    if (year >= 0 && items[7].contains("G")) {
                        // If the year is valid (RINEX uses 80-99 for 1980-1999 and 00-79 for 2000-2079)
                        // and the number of observed satellites is greater than 0 (if == 0 there will be
                        // no index G because G starts a satellite PRN) then we want to store this
                        year += year < 80 ? 2000 : 1900;
                        month = ParserUtils.parseByte(items[1]);
                        day = ParserUtils.parseByte(items[2]);
                        hour = ParserUtils.parseByte(items[3]);
                        minute = ParserUtils.parseByte(items[4]);
                        second = (byte) ParserUtils.parseDouble(items[5]); // seconds is a decimal, we use a byte
                        cal.set(year, month-1, day, hour, minute, second); // Java says January is month 0
                        cal.set(Calendar.MILLISECOND, 0);
                        observationTime = cal.getTime();

                        // Get the number of satellites by pulling a substring of the PRN string giving
                        // the number of satellites. This is followed immediately by the PRNs of the
                        // satellites. For example, 4G12G06G22G17 has 4 satellites followed by the PRNs
                        prnString = items[7];
                        numObservationsInEpoch = ParserUtils.parseByte(prnString.substring(0, prnString.indexOf("G")));

                        observationNumber++;
                        // Skip this observation epoch according to the data density
                        if (observationNumber % density != 0) {
                            if (numObservationsInEpoch > 12) {
                                fileReader.readLine();
                            }

                            // read lines until we're past the epoch
                            // 5 observations fit on a line so we read (numberOfObservationTypes / 5) lines for
                            // however many observations there are in the epoch
                            byte linesToSkip = (byte) (Math.ceil((float) numObservationTypes / 5) * numObservationsInEpoch);
                            for (byte i = 0; i < linesToSkip; i++) {
                                fileReader.readLine();
                            }

                            continue;
                        }

                        if (numObservationsInEpoch > 12) {
                            // 2 lines for the PRN string
                            prnString += fileReader.readLine().trim();
                        }
                        prns = ParserUtils.splitPRNs(prnString, numObservationsInEpoch);

                        for (byte i = 0; i < numObservationsInEpoch; i++) {
                            observationItems.clear();

                            // Read in each observation
                            while (observationItems.size() < numObservationTypes) {
                                line = fileReader.readLine();

                                // Each piece of data in the RINEX file is contained in a 16-character
                                // subsequence of the line
                                // Length is at most 80, cache it here because we'll use it a few times
                                byte length = (byte) line.length();
                                // dataIdx will always be in the range (0, 80) so we can make it a byte
                                for (byte dataIdx = 0; dataIdx < length; dataIdx += 16) {
                                    // Get 14-character substring of the line at 16-character intervals
                                    // Reading in one observation type at a time
                                    // Reading in 14 character substrings lets us avoid collecting
                                    // loss of lock indicator and signal strength indicator for L1 and L2
                                    // and avoids going psat the end of the line
                                    observationItems.add(line.substring(dataIdx, Math.min(dataIdx + 14, line.length())));
                                }

                                if (length < 80) {
                                    int howManyMissing = (80 - length) / 16;
                                    int j = 0;
                                    while (observationItems.size() < numObservationTypes && j++ < howManyMissing) {
                                        observationItems.add(" ");
                                    }
                                }
                            }

                            l1 = ParserUtils.parseDouble(observationItems.get(indexL1));
                            if (l1 == 0) {
                                // Data is bad
                                continue;
                            }

                            l2 = ParserUtils.parseDouble(observationItems.get(indexL2));
                            if (l2 == 0) {
                                // Data is bad
                                continue;
                            }

                            // Set the differential range
                            diffRange = Integer.MAX_VALUE;
                            diffRangeSet = false;
                            try {
                                if (hasP1) {
                                    double p2Pseudorange = ParserUtils.parseDouble(observationItems.get(obsList.indexOf("P2")));
                                    double p1Pseudorange = ParserUtils.parseDouble(observationItems.get(obsList.indexOf("P1")));
                                    diffRange = p2Pseudorange -p1Pseudorange;

                                    diffRangeSet = p2Pseudorange != 0 && p1Pseudorange != 0;
                                }
                            }catch (Exception e) {
                                // here because something didn't parse
                                e.printStackTrace();
                            }

                            if (!diffRangeSet) {
                                try {
                                    if (hasP2) {
                                        double p2Pseudorange = ParserUtils.parseDouble(observationItems.get(obsList.indexOf("P2")));
                                        double c1Pseudorange = ParserUtils.parseDouble(observationItems.get(obsList.indexOf("C1")));
                                        diffRange = p2Pseudorange - c1Pseudorange;

                                        diffRangeSet = p2Pseudorange != 0 && c1Pseudorange != 0;
                                    }
                                } catch (Exception e) {
                                    // something didn't parse
                                    e.printStackTrace();
                                }
                            }

                            if (!diffRangeSet) {
                                try {
                                    double c2Pseudorange = ParserUtils.parseDouble(observationItems.get(obsList.indexOf("C2")));
                                    double c1Pseudorange = ParserUtils.parseDouble(observationItems.get(obsList.indexOf("C1")));
                                    diffRange = c2Pseudorange - c1Pseudorange;

                                    diffRangeSet = c2Pseudorange != 0 && c1Pseudorange != 0;
                                } catch (Exception e) {
                                    // something didn't parse
                                    e.printStackTrace();
                                }
                            }

                            if (diffRangeSet) {
                                // Convert diffRange to TEC
                                diffRange = diffRange * METERS_TO_TEC * F2_F1_FACTOR;

                                try {
                                    phase = (l1 * L1_VALUE_TO_METERS - l2 * L2_VALUE_TO_METERS)
                                            * F2_F1_FACTOR * METERS_TO_TEC;

                                    observation = new GPSObservation();
                                    observation.time = observationTime;
                                    observation.prn = prns[i];
                                    observation.differentialRange = diffRange;
                                    observation.phase = phase;

                                    mahaliObservation.add(observation);

                                } catch (NumberFormatException e) {
                                    // Gracefully notice something is missing and don't crash
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            Toast.makeText(context, "Error reading file " + obsFile.getName(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return null;
        } catch (NullPointerException e) {
            // This should never happen
            e.printStackTrace();
            return null;
        }

        return mahaliObservation;
    }
}
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
import android.util.SparseArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import edu.mit.haystack.mcheetah.utils.ParserUtils;

/**
 * @author David Mascharka
 *
 * Class to parse Ionex files to get satelllite biases in TEC units
 *
 * Biases are loaded then retrieved using a hashmap
 *
 * Adapted from Bill Rideout's (MIT Haystack) Python module
 */
public class IonexParser {

    private static SparseArray<Double> biases;

    public IonexParser() {
        biases = new SparseArray<Double>();
    }

    public void parse(File ionexFile) {
        // conversion factor in TECu
        double conversionFactor = -0.463*6.158; // diff ns -> meters -> TEC

        boolean lineFound = false; // on the right line

        String line;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(ionexFile));

            while ((line = reader.readLine()) != null) {
                if (line.contains("DIFFERENTIAL CODE BIASES")) {
                    lineFound = true;
                    continue;
                }

                if (lineFound) {
                    String[] items = ParserUtils.splitSpace(line);

                    // See if we're done
                    // We're not using the ParserUtils parse methods because we want the exception - it tells us we're done
                    int id = 0;
                    try {
                        try {
                            id = Integer.parseInt(items[0]);
                        } catch (NumberFormatException e) {
                            // see if the last two characters are ints and the first is G
                            if (items[0].charAt(0) == 'G') {
                                id = Integer.parseInt(items[0].substring(1, 3));
                            }
                        }

                        double bias = ParserUtils.parseDouble(items[1]) * conversionFactor;

                        biases.put(id, bias);
                    } catch (Exception e) {
                        // We'll end up here after we finish reading data
                        return;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieve the satellite bias
     *
     * @param prn the PRN number of the satellite
     * @return the satellite's bias in TECu
     */
    public double getBias(byte prn) {
        if (biases.indexOfKey(prn) == -1) {
            return 0;
        }

        return biases.get(prn);
    }
}
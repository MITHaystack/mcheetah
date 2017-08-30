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

import java.util.ArrayList;
import java.util.List;

/**
 * @author David Mascharka
 *
 * Class holding the data from a single Mahali observation
 *
 * This class allows reduced memory usage by eliminating many values from the GPSObservation class
 * Since the box is static over an observation period, this class holds the receiver XYZ position, eliminating
 * 3 doubles from all observations (which may be well over 1,000,000 - equates to large memory savings)
 *
 * This is essentially a struct holding receiver XYZ and observation data
 */
public class MahaliObservation {
    public double receiverX;
    public double receiverY;
    public double receiverZ;

    public List<GPSObservation> observations;

    public MahaliObservation() {
        observations = new ArrayList<GPSObservation>(); //Collections.synchronizedList(new ArrayList<GPSObservation>());
    }
}
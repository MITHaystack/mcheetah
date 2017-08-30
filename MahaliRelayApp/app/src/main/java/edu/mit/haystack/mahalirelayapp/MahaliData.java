package edu.mit.haystack.mahalirelayapp;

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

/**
 * @author David Mascharka
 *
 * Basically a struct for some information that may be used in different parts of the app and change
 * on each run of the app so we're storing the values here instead of in SharedPreferences
 *
 * This avoids some of the complexity of communicating values between activities, simplifying the app some
 */
public class MahaliData {
    /**
     * Latitude of the Mahali box - used to compute angle of elevation to a satellite
     * if the position is not given by the receiver
     */
    public static double mahaliLatitude = Double.MAX_VALUE;

    /**
     * Longitude of the Mahali box - used to compute angle of elevation to a satellite
     * if the position is not given by the receiver
     */
    public static double mahaliLongitude = Double.MAX_VALUE;

    /**
     * Height above the reference ellipsoid - used to compute angle of elevation to a satellite
     * if the position is not given by the receiver
     */
    public static double mahaliElevation = Double.MAX_VALUE;

    /**
     * X position of the Mahali box in ECEF coordinates
     */
    public static double mahaliX = Double.MAX_VALUE;

    /**
     * Y position of the Mahali box in ECEF coordinates
     */
    public static double mahaliY = Double.MAX_VALUE;

    /**
     * Z position of the Mahali box in ECEF coordinates
     */
    public static double mahaliZ = Double.MAX_VALUE;

    /**
     * The receiver bias in TECu
     */
    public static double mahaliReceiverBias = 0;
}

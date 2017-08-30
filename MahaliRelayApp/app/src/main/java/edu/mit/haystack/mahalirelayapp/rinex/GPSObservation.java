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

import java.util.Date;

/**
 * @author David Mascharka
 *
 * Contains all the information found in a single datapoint from a RINEX observation file
 *
 * Notice the lack of getters and setters (other than for the time)
 * This is because performance is improved by abandoning getters/setters
 * Decreased runtime by 10 seconds without other changes
 */
public class GPSObservation implements Comparable<GPSObservation> {

    /**
     * Time of the GPS observation
     */
    public Date time;

    /**
     * The pseudo-random noise ID of the satellite this reading came from
     *
     * Save some space by making it a byte since we know it'll be 1-32 so we don't a full int
     * Seems insignificant but when you have over 100,000 of them you're saving a few tens of kilobytes
     * which makes a difference on phones/tablets with limited memory
     */
    public byte prn;

    /**
     * Angle of elevation from the receiver to the satellite
     */
    public double elevation;

    /**
     * In TECu - noisy data but correct absolute average value for line-of-sight (slant) TEC
     */
    public double differentialRange;

    /**
     * In TECu - smooth but wrong absolute value line-of-sight (slant) TEC
     */
    public double phase;

    /**
     * In TECu - corrected line-of-sight TEC
     */
    public double slantTEC;

    /**
     * Estimated error in TECu
     */
    public double tecError;

    /**
     * In TECu - line-of-sight TEC converted to vertical by a mapping function based on elevation
     */
    public double verticalTEC;

    public GPSObservation() {
        prn = -1;
        elevation = Integer.MAX_VALUE;
        differentialRange = Integer.MAX_VALUE;
        phase = Integer.MAX_VALUE;
        slantTEC = Integer.MAX_VALUE;
        tecError = Integer.MAX_VALUE;
    }

    /**
     * Useful for sorting by observations
     *
     * Comparison goes by PRN, then by time
     *
     * @param another the GPSObservation to compare to
     * @return the order of observations, by PRN then time
     */
    @Override
    public int compareTo(GPSObservation another) {
        if (this.prn < another.prn) {
            return -1;
        } else if (this.prn > another.prn) {
            return 1;
        }

        return this.time.compareTo(another.time);
    }
}
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import edu.mit.haystack.mcheetah.parsing.Parser;
import edu.mit.haystack.mcheetah.utils.ParserUtils;

public class RinexNavigationParser implements Parser<GPSEphemeris> {
    public ArrayList<GPSEphemeris> parse(File rinexFile, int density) {
        ArrayList<GPSEphemeris> satelliteEphemerides = new ArrayList<>();

        FileInputStream inputStream;

        try {
            inputStream = new FileInputStream(rinexFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }


        InputStreamReader inputReader;
        BufferedReader fileReader;

        inputReader = new InputStreamReader(inputStream);
        fileReader = new BufferedReader(inputReader);

        String line = "";
        byte lineNumber = -1; // only goes to 8
        byte prn = 0;       // range 1-32
        short year = 0;     // 1981-2079
        byte month = 0;     // 1-12
        byte day = 0;       // 1-31
        byte hour = 0;      // 0-23
        byte minute = 0;    // 0-59
        byte second = 0;    // 0-59

        // See the GPSEphemeris class for descriptions of these variables
        double crs = 0;
        double deltaN = 0;
        double m0 = 0;
        double cuc = 0;
        double e = 0;
        double cus = 0;
        double sqrtA = 0;
        double toe = 0;
        double cic = 0;
        double OMEGA = 0;
        double cis = 0;
        double i0 = 0;
        double crc = 0;
        double omega = 0;
        double OMEGA_DOT = 0;
        double IDOT = 0;

        // Straightforward parsing
        // All the ephemeris information is in the same order and is always the same information
        // File format specified at: ftp://igscb.jpl.nasa.gov/igscb/data/format/rinex210.txt
        try {
            while ((line = fileReader.readLine()) != null) {
                if (lineNumber == -1) {
                    if (line.contains("END OF HEADER")) {
                        lineNumber = 0;
                    }
                } else if (lineNumber == 0) {
                    prn = ParserUtils.parseByte(line.substring(0, 2));
                    year = ParserUtils.parseShort(line.substring(2, 5));
                    year += year < 80 ? 2000 : 1900;
                    month = ParserUtils.parseByte(line.substring(5, 8));
                    day = ParserUtils.parseByte(line.substring(8, 11));
                    hour = ParserUtils.parseByte(line.substring(11, 14));
                    minute = ParserUtils.parseByte(line.substring(14, 17));
                    second = (byte) ParserUtils.parseDouble(line.substring(17, 22));
                    lineNumber = 1;
                } else if (lineNumber == 1) {
                    crs = ParserUtils.parseDouble(line.substring(22, 41));
                    deltaN = ParserUtils.parseDouble(line.substring(41, 60));
                    m0 = ParserUtils.parseDouble(line.substring(60));
                    lineNumber = 2;
                } else if (lineNumber == 2) {
                    cuc = ParserUtils.parseDouble(line.substring(0, 22));
                    e = ParserUtils.parseDouble(line.substring(22, 41));
                    cus = ParserUtils.parseDouble(line.substring(41, 60));
                    sqrtA = ParserUtils.parseDouble(line.substring(60));
                    lineNumber = 3;
                } else if (lineNumber == 3) {
                    toe = ParserUtils.parseDouble(line.substring(0, 22));
                    cic = ParserUtils.parseDouble(line.substring(22, 41));
                    OMEGA = ParserUtils.parseDouble(line.substring(41, 60));
                    cis = ParserUtils.parseDouble(line.substring(60));
                    lineNumber  = 4;
                } else if (lineNumber == 4) {
                    i0 = ParserUtils.parseDouble(line.substring(0, 22));
                    crc = ParserUtils.parseDouble(line.substring(22, 41));
                    omega = ParserUtils.parseDouble(line.substring(41, 60));
                    OMEGA_DOT = ParserUtils.parseDouble(line.substring(60));
                    lineNumber = 5;
                } else if (lineNumber == 5) {
                    IDOT = ParserUtils.parseDouble(line.substring(0, 22));
                    lineNumber = 6;
                } else {
                    lineNumber++;
                    if (lineNumber > 7) {
                        lineNumber = 0;

                        GPSEphemeris ephemeris = new GPSEphemeris(year, month, day, hour, minute, second,
                                prn, toe, sqrtA, e, i0, OMEGA, omega, m0, IDOT, OMEGA_DOT, deltaN, cuc, cus,
                                crc, crs, cic, cis);
                        satelliteEphemerides.add(ephemeris);
                    }
                }
            }

            // Add the last ephemeris
            GPSEphemeris ephemeris = new GPSEphemeris(year, month, day, hour, minute, second,
                    prn, toe, sqrtA, e, i0, OMEGA, omega, m0, IDOT, OMEGA_DOT, deltaN, cuc, cus,
                    crc, crs, cic, cis);
            satelliteEphemerides.add(ephemeris);
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return satelliteEphemerides;
    }
}
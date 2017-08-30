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

import java.util.Calendar;
import java.util.Date;

import edu.mit.haystack.mcheetah.utils.ParserUtils;

/**
 * @author David Mascharka
 *
 * Class that calculates GPS satellite location from a given ephemeris, as specified in a rinex navigation file
 *
 * Adapted from Bill Rideout's python moduele
 */
public class GPSEphemeris implements Comparable<GPSEphemeris> {
    /**
     * The time this ephemeris was sent
     */
    public Date time;

    /**
     * The satellite PRN for identification
     *
     * Save some space by making it a byte since we know it'll be 1-32 so we don't a full int
     * Not as big an impact as making the GPSObservation PRN a byte but good to save memory anyway
     */
    public byte prn;

    /**
     * Time of ephemeris
     */
    private double toe;

    /**
     * semimajor axis of the satellite's orbit
     *
     * Given as the square root of the semimajor axis but used often as the full axis so we store that instead to
     * save some computation
     */
    private double a;

    /**
     * Eccentricity of the satellite's orbit
     */
    private double e;

    /**
     * Initial inclination
     */
    private double i0;

    /**
     * Longitude of the ascending node (at weekly epoch)
     */
    private double OMEGA;

    /**
     * Argument of perigee at time toe
     */
    private double omega;

    /**
     * Mean anomaly at time toe
     */
    private double m0;

    /**
     * Inclination rate of change
     */
    private double idot;

    /**
     * Rate of change of longitude of the ascending node
     */
    private double omegaDot;

    /**
     * Mean motion correction
     */
    private double deltaN;

    /**
     * Latitude correction cosinus component
     */
    private double cuc;

    /**
     * Latitude correction sinus component
     */
    private double cus;

    /**
     * Radius correction cosinus component
     */
    private double crc;

    /**
     * Radius correction sinus component
     */
    private double crs;

    /**
     * Inclination correction cosinus component
     */
    private double cic;

    /**
     * Angular velocity
     */
    private double cis;

    private static Calendar cal;
    private static double[] xyz;

    private static final double mu = 3986005.0E8; // universal gravitational constant
    private static final double OeDOT = 7.2921151467E-5;

    /**
     * Earth's semimajor axis length
     */
    private static final double EARTH_MAJOR_SEMI = 6378137.0;

    /**
     * Earth's semiminor axis length
     */
    private static final double EARTH_MINOR_SEMI = 6356752.3142;

    /**
     * The square of earth's eccentricity
     */
    private static final double EARTH_ECCENTRICITY_SQ = 0.00669437999014;

    /**
     * Create a new GPSEphemeris object holding satellite ephemeris information for one point in time
     *
     * Note there is no millisecond option - we assume only 1-second resolution so we don't keep track
     * of the millisecond
     * Instead, all milliseconds are set to 0
     *
     * @param year the year corresponding to this ephemeris
     * @param month the month corresponding to this ephemeris
     * @param day the day corresponding to this ephemeris
     * @param hour the hour corresponding to this ephemeris
     * @param minute the minute corresponding to this ephemeris
     * @param second the second corresponding to this ephemeris
     * @param prn the pseudo-random noise ID for this satellite
     * @param toe the time of ephemeris
     * @param sqrtA square root of the semimajor axis
     * @param e eccentricity
     * @param i0 initial inclination
     * @param OMEGA longitude of the ascending node (at weekly epoch)
     * @param omega argument of perigee at time of ephemeris
     * @param m0 mean anomaly at time of ephemeris
     * @param IDOT inclination rate of change
     * @param OMEGA_DOT rate of change of longitude of the ascending node
     * @param deltaN mean motion correction
     * @param cuc latitude correction cosinus component
     * @param cus latitude correction sinus component
     * @param crc radius correction cosinus component
     * @param crs radius correction sinus component
     * @param cic inclination correction cosinus component
     * @param cis angular velocity
     */
    public GPSEphemeris(short year, byte month, byte day, byte hour, byte minute, byte second, byte prn, double toe,
                        double sqrtA, double e, double i0, double OMEGA, double omega, double m0, double IDOT,
                        double OMEGA_DOT, double deltaN, double cuc, double cus, double crc, double crs,
                        double cic, double cis) {
        this.prn = prn;
        cal = Calendar.getInstance();
        cal.set(year, month-1, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0);
        this.time = cal.getTime();
        this.toe = toe;
        this.a = sqrtA*sqrtA;
        this.e = e;
        this.i0 = i0;
        this.OMEGA = OMEGA;
        this.omega = omega;
        this.m0 = m0;
        idot = IDOT;
        omegaDot = OMEGA_DOT;
        this.deltaN = deltaN;
        this.cuc = cuc;
        this.cus = cus;
        this.crc = crc;
        this.crs = crs;
        this.cic = cic;
        this.cis = cis;

        xyz = new double[3];
    }

    /**
     * Returns the time delay or advance as a result of relativistic effects
     *
     * Compensation from "Understanding GPS Principles and Applications" Elliott Kaplan, Editor pg 244
     * "When the satellite is at perigee, the satellite velocity is higher and the gravitational potential
     * is lower -- both cause the satellite clock to run slower. When the satellite is at apogee, the
     * satellite velocity is lower and the gravitational potential is higher -- both cause the satellite
     * clock to run faster"
     *
     * deltaTr = F*e*sqrt(A)*sin(Ek)
     *
     * @param gpsTime the time of the observation
     * @return the delay or advance of the satellite clock due to relativistic effects
     */
    public double getRelativisticDelay(float  gpsTime) {
        double n = Math.sqrt(mu/Math.pow(a, 3)) + deltaN;

        double tk = gpsTime - toe;

        double Mk = m0 + n*tk;

        double Ek = solveIter(Mk, e);

        double F = -4.442807633E-10;

        return F*e*Math.sqrt(a)*Math.sin(Ek);
    }

    /**
     * Gets the satellize xyz as a tuple at GPS time of week
     * @param time number of seconds since midnight of Saturday/Sunday
     * @return tuple of satellite position at gpsTime in ECEF coordinates
     *
     * Algorithm based on http://web.ics.purdue.edu/~ecalais/teaching/geodesy/EAS_591T_2003_lab_4.htm
     */
    public double[] getSatelliteXYZ(Date time) {
        int gpsTime = getGpsTime(time);

        double n = Math.sqrt(mu/(a*a*a)) + deltaN;

        double tk = gpsTime - toe;

        double Mk = m0 + n*tk;

        double Ek = solveIter(Mk, e);

        double numOpp = Math.sqrt(1.0 - e * e)*Math.sin(Ek);
        double numAdj = Math.cos(Ek) - e;
        double Vk = Math.atan2(numOpp, numAdj);

        double Phik = Vk + omega;

        // correct for orbital perturbations
        double omega = this.omega + cus*Math.sin(2.0 * Phik) + cuc*Math.cos(2.0 * Phik);

        double r = a*(1.0 - e*Math.cos(Ek)) + crs*Math.sin(2.0 * Phik) +
                crc*Math.cos(2.0 * Phik);
        double i = i0 + idot*tk + cis*Math.sin(2.0 * Phik) +
                cic*Math.cos(2.0 * Phik);

        // compute right ascension
        double Omega = OMEGA + (omegaDot - OeDOT)*tk - (OeDOT*toe);

        // convert satellite position from orbital frame to ECEF frame
        double cosOmega = Math.cos(Omega);
        double sinOmega = Math.sin(Omega);
        double cosomega = Math.cos(omega);
        double sinomega = Math.sin(omega);
        double cosi = Math.cos(i);
        double sini = Math.sin(i);
        double cosVk = Math.cos(Vk);
        double sinVk = Math.sin(Vk);

        double R11 = cosOmega*cosomega - sinOmega*sinomega*cosi;
        double R12 = -1.0*cosOmega*sinomega - sinOmega*cosomega*cosi;


        double R21 = sinOmega*cosomega + cosOmega*sinomega*cosi;
        double R22 = -1.0*sinOmega*sinomega + cosOmega*cosomega*cosi;

        double R31 = sinomega*sini;
        double R32 = cosomega*sini;

        xyz[0] = R11*r*cosVk + R12*r*sinVk; // x
        xyz[1] = R21*r*cosVk + R22*r*sinVk; // y
        xyz[2] = R31*r*cosVk + R32*r*sinVk; // z

        return xyz;
    }

    /**
     * Returns a tuple of (lat, long, alt) given ECEF coordinates (x, y, z)
     * @param x ECEF x coordinate
     * @param y ECEF y coordinate
     * @param z ECEF z coordinate
     * @return a tuple of (lat, long, alt in km) - long from 0 to 360
     */
    public static double[] getLatLongAltFromXYZ(double x, double y, double z) {
        double e2 = 1-EARTH_MINOR_SEMI*EARTH_MINOR_SEMI/(EARTH_MAJOR_SEMI * EARTH_MAJOR_SEMI);
        double r = Math.sqrt(x*x+y*y);
        double E2 = EARTH_MAJOR_SEMI * EARTH_MAJOR_SEMI -EARTH_MINOR_SEMI*EARTH_MINOR_SEMI;
        double Ff = 54.0*EARTH_MINOR_SEMI*EARTH_MINOR_SEMI*z*z;
        double Gg = r*r+(1-e2)*z*z-e2*E2;
        double c = e2*e2*Ff*r*r/(Gg*Gg*Gg);
        double ss = Math.pow(1 + c + Math.sqrt(c * c + 2.0 * c), 1.0 / 3.0);
        double P = Ff/(3.0*Math.pow(ss + 1/ss + 1, 2.0)*Gg*Gg);
        double Q = Math.sqrt(1.0 + 2.0*e2*e2*P);
        double r0 = -P*e2*r/(1.0+Q)+Math.sqrt(0.5* EARTH_MAJOR_SEMI * EARTH_MAJOR_SEMI *(1.0+1.0/Q)-P*(1-e2)*z*z/(Q+Q*Q)-0.5*P*r*r);
        double U = Math.sqrt(Math.pow(r-e2*r0, 2.0)+z*z);
        double V = Math.sqrt(Math.pow(r-e2*r0, 2.0)+(1-e2)*z*z);
        double z0 = EARTH_MINOR_SEMI*EARTH_MINOR_SEMI*z/(EARTH_MAJOR_SEMI *V);
        double h = U*(1-EARTH_MINOR_SEMI*EARTH_MINOR_SEMI/(EARTH_MAJOR_SEMI *V));
        double latitude = Math.atan2(z + EARTH_MAJOR_SEMI * EARTH_MAJOR_SEMI * e2 * z0 / (EARTH_MINOR_SEMI * EARTH_MINOR_SEMI), r);
        double longitude = Math.atan2(y, x);

        latitude = ParserUtils.RADIANS_TO_DEGREES*(latitude);
        longitude = ParserUtils.RADIANS_TO_DEGREES*(longitude);

        // be sure longitude goes from 0 to 360
        if (longitude < 0.0) {
            longitude += 360;
        }

        return new double[] {latitude, longitude, h/1000.0};
    }

    /**
     * Converts geodetic latitude, longitude, height to ECEF XYZ
     * Geodetic coordinates should be using the WGS-84 reference ellipsoid
     *
     * @param latitude latitude
     * @param longitude longitude
     * @param elevation height above the ellipsoid
     * @return a tuple of (x, y, z) in ECEF coordinates
     */
    public static double[] geodeticToECEF(double latitude, double longitude, double elevation) {
        double sLat = Math.sin(ParserUtils.DEGREES_TO_RADIANS * latitude);
        double cLat = Math.cos(ParserUtils.DEGREES_TO_RADIANS * latitude);
        double N = EARTH_MAJOR_SEMI/Math.sqrt(1.0-EARTH_ECCENTRICITY_SQ*sLat*sLat);

        return new double[] {(N + elevation) * cLat * Math.cos(ParserUtils.DEGREES_TO_RADIANS * (longitude)),
                (N+elevation)*cLat*Math.sin(ParserUtils.DEGREES_TO_RADIANS * (longitude)),
                (N*(1.0-EARTH_ECCENTRICITY_SQ)+elevation)*sLat };
    }

    private static double solveIter(double Mk, double e) {
        double start = Mk - 1.01*e;
        double end = Mk + 1.01*e;
        double bestGuess = 0;
        double minErr;
        double guess;
        double term1;
        double err;
        double range;

        for (int i = 0; i < 5; i++) {
            minErr = Float.MAX_VALUE;
            bestGuess = 0;
            for (int j = 0; j < 5; j++) {
                guess = start + j*(end-start)/10.0;
                term1 = e*Math.sin(guess);
                err = Math.abs(Mk-guess+term1);
                if (err < minErr) {
                    minErr = err;
                    bestGuess = guess;
                }
            }
            range = end-start;
            start = bestGuess - range/10.0;
            end = bestGuess + range/10.0;
        }

        return bestGuess;
    }

    /**
     * Convert a date to GPS time
     *
     * @param time the date
     * @return GPS time for the given date (Sunday midnight is 0)
     */
    private static int getGpsTime(Date time) {
        cal.setTime(time);
        int total = 0;
        total += (cal.get(Calendar.DAY_OF_WEEK)-1)*3600*24;
        total += cal.get(Calendar.HOUR_OF_DAY)*3600;
        total += cal.get(Calendar.MINUTE)*60;
        total += cal.get(Calendar.SECOND);

        return total;
    }

    /**
     * Useful for sorting satellite ephemerides
     *
     * Compare satellite ephemerides by PRN, then by time
     *
     * @param another the ephemeris to compare to
     * @return the order sorted by PRN and time
     */
    @Override
    public int compareTo(GPSEphemeris another) {
        // sort by PRN
        if (this.prn < another.prn) {
            return -1;
        } else if (this.prn > another.prn) {
            return 1;
        }

        // and by time
        return this.time.compareTo(another.time);
    }
}
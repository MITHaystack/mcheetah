package edu.mit.haystack.mcheetah.utils;

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

/**
 * @author David Mascharka
 *
 * Contains handy functions for parsing files that create significantly less
 * garbage than Java's methods
 *
 * Note that this class is NOT thread safe and should not be called from multiple threads
 */
public class ParserUtils {
    public static final double DEGREES_TO_RADIANS = 0.01745329251;

    public static final double RADIANS_TO_DEGREES = 57.2957795;

    private ParserUtils() {} // static class, don't let other classes instantiate this

    /**
     * Java's parseDouble creates too many objects so do this character-by-character and
     * don't create any new objects
     *
     * @param num the string representation of the double we want to parse
     * @return the double represented by the input string
     */
    public static double parseDouble(String num) {
        double doubleRetVal = Integer.MAX_VALUE;
        boolean pastDecimal = false;
        boolean negative = false;
        byte digitsPastDecimal = 0;
        boolean inExponent = false;
        boolean exponentNegative = false;
        byte exponent = 0;
        char c;
        byte digit;

        // No number we have to deal with should ever be longer than 255 digits
        byte size = (byte) num.length();
        for (byte i = 0; i < size; i++) {
            c = num.charAt(i);

            // if the character is a digit
            if ('0' <= c && c <= '9') {
                // Get the integer representation of the character by subtracting the ASCII value for 0
                digit = (byte) (c - '0');

                // If this is not a digit in the exponent
                if (!inExponent) {
                    // If the value has not been initialized
                    if (doubleRetVal == Integer.MAX_VALUE) {
                        // Set the return value to this digit
                        doubleRetVal = digit;
                    } else {
                        // If we're here, doubleRetVal already has a value and we're appending a digit
                        // Multiply doubleRetVal by 10 and add the last digit to it, effectively
                        // appending digit to the end of doubleRetVal
                        doubleRetVal = doubleRetVal*10 + digit;
                    }

                    // If we're in the decimal, mark another decimal digit down
                    if (pastDecimal) {
                        digitsPastDecimal++;
                    }
                } else { // we are in the exponent
                    // If the exponent is currently 0
                    if (exponent == 0) {
                        // set exponent
                        exponent = digit;
                    } else {
                        // otherwise append digit to exponent
                        exponent *= 10;
                        exponent += digit;
                    }
                }
            } else if (c == '-') { // if the character isn't a digit but is a negative sign
                // if we're not in the exponent, negate negative
                // Note that if a number has negative signs in the middle of it this will just flip signs
                // So if you pass in -12-12 the value returned by this method will be 1212
                if (!inExponent) {
                    negative = !negative;
                } else {
                    // Negate the exponent
                    exponentNegative = !exponentNegative;
                }
            } else if (c == '.') { // if the character isn't a digit but is a decimal
                // we're past the decimal
                // Note that this will parse things like 123.123.123 just fine as 123.123123
                pastDecimal = true;
            } else if (c == 'D' || c == 'E') { // 1.2E7 and 1.2D7 are both 1.2*10^7
                inExponent = true;
            }
        }

        // Divide by 10 until we've got the correct number of decimal digits
        while (digitsPastDecimal > 0) {
            digitsPastDecimal--;
            doubleRetVal /= 10;
        }

        // If the number is negative, make it so
        if (negative) {
            doubleRetVal *= -1;
        }

        // While we have exponent digits, multiply or divide by 10 depending on whether
        // the exponent is positive or negative
        while (exponent > 0) {
            if (exponentNegative) {
                doubleRetVal /= 10;
            } else {
                doubleRetVal *= 10;
            }
            exponent--;
        }

        return doubleRetVal;
    }

    /**
     * Java's Byte.parseByte method calls Integer.parseInt, which creates some objects
     * Don't create a bunch of objects. Just take the number directly
     * Experimentally, takes a little bit less time and memory than calling Byte.parseByte
     *
     * For a detailed walkthrough of this function, see the parseDouble method in the class
     * They are very similar, except this does not handle exponents or decimal digits
     *
     * @param num the string representation of the number we want to parse
     * @return the byte representation of the number
     */
    public static byte parseByte(String num) {
        byte byteRetVal = Byte.MAX_VALUE;
        boolean negative = false;

        char c;
        byte digit;
        byte size = (byte) num.length();
        for (byte i = 0; i < size; i++) {
            c = num.charAt(i);

            if ('0' <= c && c <= '9') {
                // the character is a diigt
                digit = (byte) (c - '0');
                if (byteRetVal == Byte.MAX_VALUE) {
                    byteRetVal = digit;
                } else {
                    byteRetVal *= 10;
                    byteRetVal += digit;
                }
            } else if (c == '-') {
                negative = !negative;
            }
        }

        if (negative) {
            byteRetVal *= -1;
        }

        return byteRetVal;
    }

    /**
     * Java's Short.parseShort method calls Integer.parseInt, which creates objects
     * Don't create lots of objects. Just take the number directly
     *
     * For a detailed walkthrough of this method, see the parseDouble method in this class
     * They're very similar, except this doesn't handle exponents or decimals
     *
     * @param num the string representation of the number to parse
     * @return the short integer (2 byte) representation of the number
     */
    public static short parseShort(String num) {
        short shortRetVal = Short.MAX_VALUE;
        boolean negative = false;

        byte size = (byte) num.length();
        char c;
        byte digit;
        for (byte i = 0; i < size; i++) {
            c = num.charAt(i);

            if ('0' <= c && c <= '9') {
                // digit
                digit = (byte) (c - '0');
                if (shortRetVal == Short.MAX_VALUE) {
                    shortRetVal = digit;
                } else {
                    shortRetVal *= 10;
                    shortRetVal += digit;
                }
            } else if (c == '-') {
                negative = !negative;
            }
        }

        if (negative) {
            shortRetVal *= -1;
        }

        return shortRetVal;
    }

    /**
     * Java's Integer.parseInt method generates a lot of garbage
     * Use this instead for performance
     *
     * For a detailed walkthrough of this method, see the parseDouble method in this class
     * They're very similar, except this doesn't handle exponents or decimals
     *
     * @param num the string representation of the number to parse
     * @return the integer representation of the number
     */
    public static int parseInt(String num) {
        int intRetVal = Integer.MAX_VALUE;
        boolean negative = false;

        byte size = (byte) num.length();
        char c;
        byte digit;
        for (byte i = 0; i < size; i++) {
            c = num.charAt(i);

            if ('0' <= c && c <= '9') {
                // digit
                digit = (byte) (c - '0');
                if (intRetVal == Integer.MAX_VALUE) {
                    intRetVal = digit;
                } else {
                    intRetVal *= 10;
                    intRetVal += digit;
                }
            } else if (c == '-') {
                negative = !negative;
            }
        }

        if (negative) {
            intRetVal *= -1;
        }

        return intRetVal;
    }

    /**
     * Java's String.split is awful for performance and Pattern.split also creates too many objects
     * Splits on at least one space
     * Equivalent to the regular expression /\s+/
     *
     * NOTE this assumes a line length no longer than 127 characters
     *
     * @param input the string to split on a space
     * @return array of tokens
     */
    public static String[] splitSpace(String input) {
        ArrayList<String> splitHelper = new ArrayList<>();

        byte startIndex = -1;

        byte size = (byte) input.length();
        char c;
        for (byte i = 0; i < size; i++) {
            c = input.charAt(i);
            if (c == ' ' && startIndex != -1) {
                splitHelper.add(input.substring(startIndex, i));
                startIndex = -1;
            } else if (c != ' ' && startIndex == -1) {
                startIndex = i;
            }
        }

        if (startIndex != -1) {
            // we're at the end of the string and there's something else to add
            splitHelper.add(input.substring(startIndex, size));
        }

        size = (byte) splitHelper.size();
        String[] result = new String[size];
        for (byte i = 0; i < size; i++) {
            result[i] = splitHelper.get(i);
        }

        return result;
    }

    /**
     * Again, Java's split methods create a lot of garbage
     * Optimize splitting on a G for reading PRNs
     *
     * @param input a prn string
     * @param numPRNs the number of PRNs we're reading
     * @return a byte array of PRNs for the observation epoch
     */
    public static byte[] splitPRNs(String input, byte numPRNs) {
        byte[] PRNs = new byte[numPRNs];
        byte prnIndex = 0;
        boolean inPRN = false;

        // Here we allocate a new byte for size because we call parseByte in here, which would modify
        // the global size, which would be really bad and cause a lot of ephemerides to be null -- not good!
        byte size = (byte) input.length();
        char c;

        for (byte i = 0; i < size; i++) {
            c = input.charAt(i);
            if (c == 'G' && inPRN) {
                // all PRNs will be 2 characters
                PRNs[prnIndex] = parseByte(input.substring(i - 2, i));
                prnIndex++;
            } else if (c == 'G') { // the first number indicates numPRNs, skip over it
                inPRN = true;
            }
        }
        // get the last one
        PRNs[prnIndex] = parseByte(input.substring(size - 2, size));

        return PRNs;
    }
}
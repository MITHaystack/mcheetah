package edu.mit.haystack.mcheetah.parsing;

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

import java.io.File;
import java.util.List;

/**
 * @author David Mascharka
 *
 * Interface for parsing files to help with pipeline reusability
 *
 * NOTE: this class should be made thread-safe, unless you explicity specify that only 1 thread will
 * be used to parse files, otherwise multiple threads WILL be creating instances of this class. The
 * use of static variables in this class should be done with caution
 * For more details on why this is, see how a DataProcessFragment utilizes this class
 *
 * Commonly, a parser should only need to have a parse function which returns a list of objects this parses
 * A class implementing this Parser needs to specify what type of object will be returned, D
 */
public interface Parser<D> {
    /**
     * Parse function
     *
     * Reads in a file and returns a list of data objects
     *
     * @param dataFile the file to parse
     * @param dataDensity the density of data to process (every point, every third point, every n points)
     * @return the object the file is parsing
     */
    List<D> parse(File dataFile, int dataDensity);
}
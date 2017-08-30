package edu.mit.haystack.mcheetah;

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

import java.util.List;

/**
 * @author David Mascharka
 *
 * This class will hold the logic for the computation a scientist is interested in
 *
 * In the case of the Mahali project, this will be implemented to compute TEC data
 *
 * The interface name is actually very descriptive because this is where computation happens but
 * does, I admit, sound a bit silly
 *
 * The data type here should be the same as that of the Parser you implement.
 *
 * NOTE: this class should define any parallelism of the computation if parallelism is desired,
 * otherwise this will automatically run on a single worker thread
 */
public interface Computer<D> {
    /**
     * Performs the main computation
     *
     * @param data list of data objects
     * @return boolean indicating success
     */
    boolean compute(List<D> data);
}
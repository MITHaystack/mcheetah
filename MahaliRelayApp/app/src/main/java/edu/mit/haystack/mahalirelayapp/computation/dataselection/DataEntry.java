package edu.mit.haystack.mahalirelayapp.computation.dataselection;

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

/**
 * @author David Mascharka
 *
 * Entry in the list of data objects -> contains a checkbox and string
 */
public class DataEntry {

    private String fileName;
    private File dataFile;
    private boolean checked;

    public DataEntry(File file) {
        dataFile = file;
        fileName = dataFile.getAbsolutePath().substring(dataFile.getAbsolutePath().indexOf("mahali-"),
                        dataFile.getAbsolutePath().indexOf("/", dataFile.getAbsolutePath().indexOf("mahali-")))
                        + "/" + dataFile.getName();

        checked = false;
    }

    public String getFilePath() {
        return dataFile.getAbsolutePath();
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
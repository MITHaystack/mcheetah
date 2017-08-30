package edu.mit.haystack.example.mcheetah;

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

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import edu.mit.haystack.mcheetah.parsing.Parser;

/**
 * @author David Mascharka
 *
 * Simple parser type that takes in a file containing pairs of integers separated by newlines
 * Takes these pairs and creates a list of ExampleData type holding them
 *
 * A data file may look like:
 *     1    2
 *     3    4
 *     19   27
 *     33   3
 *     7    22
 *     ....
 */
public class ExampleParser implements Parser<ExampleData> {

    /**
     * The parent holding this parser
     */
    private Context context;

    public ExampleParser(Context context) {
        this.context = context;
    }

    /**
     * Takes a file that contains a list of integer pairs, separated by newlines
     * Very simple data type to parse
     *
     * @param dataFile the file to parse
     * @param dataDensity the density of data to process (every point, every third point, every n points)
     * @return a list of ExampleData type
     */
    public List<ExampleData> parse(File dataFile, int dataDensity) {
        ArrayList<ExampleData> theDataList = new ArrayList<ExampleData>();

        BufferedReader fileReader = null;
        FileInputStream inputStream = null;
        int observationNumber = 0;

        // Create a FileInputStream from the given data file
        try {
            inputStream = new FileInputStream(dataFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(context, "The input file was not found", Toast.LENGTH_SHORT).show();
            return null;
        }

        // Create a reader to go through the file
        try {
            fileReader = new BufferedReader(new InputStreamReader(inputStream));
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        }

        String line = "";
        try {
            // Read in the file line-by-line
            while ((line = fileReader.readLine()) != null) {
                if (observationNumber % dataDensity != 0) {
                    continue;
                }

                // Split on whitespace
                String[] parts = line.split("\\s+");
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);

                // Make a new ExampleData point and add it to the list
                theDataList.add(new ExampleData(x, y));
                observationNumber++;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return theDataList;
    }
}

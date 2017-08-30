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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import edu.mit.haystack.mcheetah.visualization.Renderer;

/**
 * @author David Mascharka
 *
 * Simple data renderer to draw pairs of (x,y) points stored in a list of ExampleData objects
 */
public class ExampleRenderer extends Renderer<ExampleData> {

    public ExampleRenderer(Context context) {
        super(context);
    }

    @Override
    public void addData(List<ExampleData> data) {
        // If there is no data, don't try drawing. Notify the listener
        numPoints = data.size();
        if (numPoints == 0) {
            badDataListener.badData();
        }

        // Each point is 2 floats, each of which is 4 bytes
        dataPoints = ByteBuffer.allocateDirect(4 * 2 * numPoints).order(ByteOrder.nativeOrder()).asFloatBuffer();

        for (int i = 0; i < data.size(); i++) {
            dataPoints.put(data.get(i).theXCoordinate);
            dataPoints.put(data.get(i).theYCoordinate);
        }
        dataPoints.rewind();

        setPlotBounds();
    }

    @Override
    public void update(List<ExampleData> data) {
        for (int i = 0; i < data.size(); i++) {
            dataPoints.put(data.get(i).theXCoordinate);
            dataPoints.put(data.get(i).theYCoordinate);
        }
        dataPoints.rewind();
    }

    @Override
    public void setAxisTicks(int xTicks, int yTicks) {
        // Don't actually set anything here
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Reset the matrix
        gl.glLoadIdentity();

        // Clear everything to black
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

        // Keep a copy of the matrix
        gl.glPushMatrix();
            // Manipulate the matrix
            gl.glScalef(zoom, zoom, 1);
            gl.glTranslatef(translateX, translateY, 0);

            // Draw our points in green
            gl.glColor4f(0.0f, 1.0f, 0.0f, 1.0f);
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glVertexPointer(2, GL10.GL_FLOAT, 0, dataPoints);
            gl.glDrawArrays(GL10.GL_POINTS, 0, numPoints);
            gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glPopMatrix();
    }
}

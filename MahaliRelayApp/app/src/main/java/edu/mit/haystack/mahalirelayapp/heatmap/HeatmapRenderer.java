package edu.mit.haystack.mahalirelayapp.heatmap;

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
import android.opengl.GLSurfaceView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author David Mascharka
 *
 * Draws a heatmap of WiFi coverage
 */
public class HeatmapRenderer implements GLSurfaceView.Renderer {

    // Struct holding rgb values for use in OpenGL's ColorArray
    private class Color {
        public float r;
        public float g;
        public float b;

        public Color(float red, float green, float blue) {
            r = red;
            g = green;
            b = blue;
        }
    }

    // Used for drawing points
    private FloatBuffer pointsBuffer;

    // Used for passing color values to points
    private FloatBuffer colorsBuffer;

    // Holds the colors we're using
    private ArrayList<Color> colors;

    // Holds the values in a heated colormap
    private ArrayList<Color> heatedColorMap;

    // The signal strength for each point and the lat/long coordinates
    private ArrayList<Integer> values;
    private ArrayList<Float> coordinates;

    // Values holding the signal strength min and max for interpolation to determine color and the
    // min and max lat/long values to size the display
    private float signalMin;
    private float signalMax;
    private float xMax;
    private float yMax;
    private float xMin;
    private float yMin;

    private Context context;

    public HeatmapRenderer(Context context) {
        this.context = context;

        colors = new ArrayList<Color>();
        heatedColorMap = new ArrayList<Color>();
        values = new ArrayList<Integer>();
        coordinates = new ArrayList<Float>();

        loadHeatedObjectColorMap();
        processData();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glPointSize(10.0f);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);

        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();

        // This will change as soon as a point gets added
        gl.glOrthof(0.0f, 1.0f, 0.0f, 1.0f, -1.0f, 1.0f);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();

        if (xMin == Integer.MAX_VALUE) {
            // this will happen until a point gets added
            gl.glOrthof(-1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f);
        } else {
            gl.glOrthof(xMin, xMax, yMin, yMax, -1.0f, 1.0f);
        }
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

        gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glColorPointer(4, GL10.GL_FLOAT, 0, colorsBuffer);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, pointsBuffer);
        gl.glDrawArrays(GL10.GL_POINTS, 0, coordinates.size() / 2);

        gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }

    public void addPoint(DataPoint point) {
        coordinates.add(point.getLongitude());
        coordinates.add(point.getLatitude());
        values.add(point.getLeve());

        processData();
    }

    private void loadHeatedObjectColorMap() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets()
                                                        .open("heatedColorMap.txt")));

            String line = reader.readLine();
            int numElements = Integer.parseInt(line);

            for (int i = 0; i < numElements; i++) {
                Color c = new Color(0, 0, 0);
                line = reader.readLine();
                String[] values = line.split("\\s+");
                c.r = Float.parseFloat(values[0]);
                c.g = Float.parseFloat(values[1]);
                c.b = Float.parseFloat(values[2]);

                heatedColorMap.add(c);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processData() {
        setPlotBounds();

        colors.clear();

        float norm;
        for (int value : values) {
            norm = (value - signalMin) / (signalMax - signalMin);
            pushColor(norm);
        }

        colorsBuffer = ByteBuffer.allocateDirect(colors.size() * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        for (Color c : colors) {
            colorsBuffer.put(c.r);
            colorsBuffer.put(c.g);
            colorsBuffer.put(c.b);
            colorsBuffer.put(1.0f); // alpha value
        }
        colorsBuffer.rewind();

        pointsBuffer = ByteBuffer.allocateDirect(coordinates.size()*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        for (float f : coordinates) {
            pointsBuffer.put(f);
        }
        pointsBuffer.rewind();
    }

    private void pushColor(float normalizedValue) {
        int value = (int) (normalizedValue * (heatedColorMap.size()-1));
        colors.add(heatedColorMap.get(value));
    }

    private void setPlotBounds() {
        signalMax = Integer.MIN_VALUE;
        signalMin = Integer.MAX_VALUE;
        xMax = Integer.MIN_VALUE;
        xMin = Integer.MAX_VALUE;
        yMax = Integer.MIN_VALUE;
        yMin = Integer.MAX_VALUE;

        float value;

        // Get the minimum and maximum x values
        for (int i = 0; i < coordinates.size(); i += 2) {
            value = coordinates.get(i);
            xMin = Math.min(xMin, value);
            xMax = Math.max(xMax, value);
        }

        // Get the minimum and maximum y values
        for (int i = 1; i < coordinates.size(); i += 2) {
            value = coordinates.get(i);
            yMin = Math.min(yMin, value);
            yMax = Math.max(yMax, value);
        }

        // Since the heatmap is probably going to be a very small area lat/long, adjust the coordinates if
        // the double to float conversion makes it look like min and max are the same
        // Also fixes the issue where there's only a single point being plotted
        // Offsets max from min by about 100 meters
        if (xMax <= xMin) {
            xMax = xMin + 0.0001f;
        }

        if (yMax <= yMin) {
            yMax = yMin + 0.0001f;
        }

        // Get minimum and maximum signal strength (color value)
        for (float val : values) {
            signalMin = Math.min(signalMin, val);
            signalMax = Math.max(signalMax, val);
        }
    }
}

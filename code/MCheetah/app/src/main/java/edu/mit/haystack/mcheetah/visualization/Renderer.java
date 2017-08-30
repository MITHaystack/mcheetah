package edu.mit.haystack.mcheetah.visualization;

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

import java.nio.FloatBuffer;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author David Mascharka
 *
 * Class for plotting using OpenGL ES
 *
 * This will be held in a GLSurfaceView of some sort
 *
 * The data passed in is of type D
 */
public abstract class Renderer<D> implements GLSurfaceView.Renderer {

    /**
     * The owner of this class will be notified via this listener if all the data is bad
     */
    public interface BadDataListener {
        void badData();
    }

    /**
     * Will be notified if the data is bad
     */
    protected BadDataListener badDataListener;

    /**
     * This is used to load the glyphs used for drawing text
     */
    protected Context context;

    /**
     * Used for drawing text on-screen for axis labels, etc
     */
    protected Glyphs glyphs;

    /**
     * The zoom level of the plot
     */
    protected float zoom;

    /**
     * Used for drawing data
     */
    protected FloatBuffer dataPoints;

    // Members for holding the minimum and maximum values on the x and y axes
    protected float xMin;
    protected float yMin;
    protected float xMax;
    protected float yMax;

    /**
     * Width of the viewport - the width of whatever View is holding this
     *
     * Nobody should be changing this - automatically computed in onSurfaceChanged
     */
    protected int viewportWidth;

    /**
     * Height of the viewport - the height of whatever View is holding this
     *
     * Nobody should be changing this - automatically computed in onSurfaceChanged
     */
    protected int viewportHeight;

    /**
     * The width of the graph - will probably be less than viewportWidth to accommodate axis labels and such
     *
     * Computed automatically based on viewportWidth and xSpacing
     */
    protected int graphWidth;

    /**
     * The height of the graph - will probably be less than viewportHeight to accommodate axis labels and such
     *
     * Computed automatically based on viewportHeight and ySpacing
     */
    protected int graphHeight;

    /**
     * The x spacing to use
     */
    protected float xSpacing;

    /**
     * The y spacing to use
     */
    protected float ySpacing;

    /**
     * The number of ticks on the x axis
     */
    protected int xAxisTicks;

    /**
     * The number of ticks on the y axis
     */
    protected int yAxisTicks;

    /**
     * The translation on the x axis
     */
    protected float translateX;

    /**
     * The translation on the y axis
     */
    protected float translateY;

    /**
     * The number of points being drawn
     */
    protected int numPoints;

    /**
     * The context passed in MUST implement BadDataListener#badData
     *
     * @param context the context this renderer belongs to, to be notified of bad data
     */
    public Renderer(Context context) {
        this.context = context;
        try {
            badDataListener = (BadDataListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.getClass().getCanonicalName() + " must implement BadDataListener");
        }
    }

    /**
     * Add a given list of data
     *
     * Specify what to do (e.g. what values to pull for rendernig
     *
     * @param data list of data to add
     */
    public abstract void addData(List<D> data);

    /**
     * Update the list of data points
     *
     * Specify what to do with the data (e.g. add, replace, remove values)
     *
     * @param data data list to update with
     */
    public abstract void update(List<D> data);

    /**
     * Set the x and y axis ticks and labels
     *
     * Here you would specify how to label the data and with how many labels per axis
     * For example, label x axis by 10s, y axis by 5s
     *
     * @param xTicks number of x ticks
     * @param yTicks number of y ticks
     */
    public abstract void setAxisTicks(int xTicks, int yTicks);

    /**
     * Initialize the plot
     *
     * Set points to size 5.0
     * Set the zoom level to 1
     * Compute and set the bounds of the plot
     * Load text glyphs for displaying axis labels
     */
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glPointSize(5.0f);

        zoom = 1;

        setPlotBounds();

        glyphs = new Glyphs();
        glyphs.loadGlyphs(gl, context);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        viewportWidth = width;
        viewportHeight = height;
        xSpacing = width / 8;
        ySpacing = height / 8;

        graphWidth = (int) ((viewportWidth - xSpacing / 4) - (3 * xSpacing / 4));
        graphHeight = (int) ((viewportHeight - ySpacing / 2) - (ySpacing / 2));

        gl.glViewport((int) (3 * xSpacing / 4), (int) (ySpacing / 2), graphWidth, graphHeight);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrthof(xMin, xMax, yMin, yMax, -1.0f, 1.0f);
        gl.glMatrixMode(GL10.GL_MODELVIEW);

        setAxisTicks(xAxisTicks, yAxisTicks);
    }

    /**
     * Set the minimum value in the data for rendering
     *
     * @param min the minimum value
     */
    public void setMinValue(float min) {
        yMin = min;
    }

    /**
     * Set the maximum value in the data for rendering
     *
     * @param max the maximum value
     */
    public void setMaxValue(float max) {
        yMax = max;
    }

    /**
     * Set the x spacing for axis ticks
     */
    public void setXSpacing(float spacing) {
        xSpacing = spacing;
    }

    /**
     * Set the y spacing for axis ticks
     */
    public void setYSpacing(float spacing) {
        ySpacing = spacing;
    }

    /**
     * Display the data to the user
     */
    @Override
    public abstract void onDrawFrame(GL10 gl);

    /**
     * Handle translating the plot
     * @param x x translation
     * @param y y translation
     */
    public void translate(float x, float y) {
        translateX += x/viewportWidth*(xMax-xMin);
        translateY -= y/viewportHeight*(yMax-yMin);
    }

    /**
     * Zet the zoom level for the data
     */
    public void setZoom(float zoomLevel) {
        zoom = zoomLevel;
    }

    /**
     * Set the bounds of the plot
     */
    protected void setPlotBounds() {
        float value;

        xMax = Float.MIN_VALUE;
        yMax = Float.MIN_VALUE;
        xMin = Float.MAX_VALUE;
        yMin = Float.MAX_VALUE;

        // Get the min and max x values
        for (int i = 0; i < 2*numPoints; i += 2) {
            value = dataPoints.get(i);
            xMax = Math.max(xMax, value);
            xMin = Math.min(xMin, value);
        }

        // Get min and max y values
        for (int i = 1; i < 2*numPoints; i += 2) {
            value = dataPoints.get(i);
            yMax = Math.max(yMax, value);
            yMin = Math.min(yMin, value);
        }
    }
}
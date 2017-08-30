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
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import java.util.List;

/**
 * @author David Mascharka
 *
 * Holds the logic for manipulating a displayed plot
 */
public class DataView<D> extends GLSurfaceView {

    /**
     * The plot this view holds
     */
    private edu.mit.haystack.mcheetah.visualization.Renderer<D> renderer;

    /**
     * The last x coordinate that was touched by the user
     */
    private float lastX;

    /**
     * The last y coordinate that was touched by the user
     */
    private float lastY;

    /**
     * Handles scaling the plot via a pinch-zoom gesture
     */
    private ScaleGestureDetector scaleDetector;

    /**
     * The amount to scale the plot by
     */
    private float scaleFactor;

    /**
     * Is the user currently zooming?
     */
    private boolean zooming;

    /**
     * Create an OpenGL SurfaceView to hold the renderer
     *
     * @param context the application that will display the plot
     */
    public DataView(Context context) {
        super(context);
        scaleFactor = 1.0f;
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    /**
     * Set the renderer class, which will display the plot that this holds
     *
     * @param renderer the renderer, which will display the plot
     */
    public void setMyRenderer(edu.mit.haystack.mcheetah.visualization.Renderer<D> renderer) {
        this.renderer = renderer;
        setRenderer(renderer);
    }

    /**
     * When the user touches the screen, handle zooming and translation
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        if (event.getPointerCount() == 1 && event.getAction() == MotionEvent.ACTION_UP) {
            zooming = false;
            return true; // don't let this action translate
        }

        if (event.getPointerCount() > 1 || zooming) {
            zooming = true;
            return true; // don't translate if we're zooming
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            lastX = event.getX();
            lastY = event.getY();
            return true;
        }

        renderer.translate(event.getX() - lastX, event.getY() - lastY);
        lastX = event.getX();
        lastY = event.getY();

        return true;
    }

    /**
     * Update the values of the points the renderer is holding
     *
     * Useful for changing values in the data if, for example, the user can interact with them
     *
     * @param data the list of data to pass in with updated values
     */
    public void updateValues(final List<D> data) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.update(data);
            }
        });
    }

    /**
     * Listens for gestures to scale the plot, scales accordingly
     */
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();

            // Constrain the zooming
            // Minimum 0.1x zoom, maximum 100x zoom
            scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 100.0f));
            renderer.setZoom(scaleFactor);

            return true;
        }
    }

    /**
     * Set the minimum and maximum values to display
     *
     * @param min minimum y value to display
     * @param max maximum y value to display
     */
    public void setMinMax(float min, float max) {
        renderer.setMinValue(min);
        renderer.setMaxValue(max);
    }
}
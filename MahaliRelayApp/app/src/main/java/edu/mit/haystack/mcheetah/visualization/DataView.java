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
 * Holds
 */
public class DataView<D> extends GLSurfaceView {

    private edu.mit.haystack.mcheetah.visualization.Renderer<D> renderer;
    private float lastX;
    private float lastY;
    private ScaleGestureDetector scaleDetector;
    private float scaleFactor;
    private boolean zooming;

    public DataView(Context context, List<D> data) {
        super(context);
        scaleFactor = 1.0f;
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    public void setMyRenderer(edu.mit.haystack.mcheetah.visualization.Renderer<D> renderer) {
        this.renderer = renderer;
        setRenderer(renderer);
    }

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

    public void updateValues(final List<D> data) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.update(data);
            }
        });
    }

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

    public void setMinMax(float min, float max) {
        renderer.setMinValue(min);
        renderer.setMaxValue(max);
    }
}
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

/**
 * @author David Mascharka
 *
 * Basic GLSurfaceView to hold a HeatmapRenderer
 */
public class HeatmapPlotView extends GLSurfaceView{

    private HeatmapRenderer renderer;

    public HeatmapPlotView(Context context) {
        super(context);

        renderer = new HeatmapRenderer(context);
        setRenderer(renderer);
    }

    /**
     * Adds a new DataPoint to the plot
     * @param point the point to add
     */
    public void addPoint(final DataPoint point) {
        // Queue the addition of the new point so that it runs on the renderer's thread
        // This is necessary so we're not manipulating the contents of the FloatBuffer in the
        // renderer while it's trying to draw, which would lead to a crash
        queueEvent(new Runnable() {
            @Override
            public void run() {
                renderer.addPoint(point);
            }
        });
    }
}

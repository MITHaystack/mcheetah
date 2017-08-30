package edu.mit.haystack.mahalirelayapp.computation;

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
import java.nio.FloatBuffer;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import edu.mit.haystack.mahalirelayapp.rinex.GPSObservation;
import edu.mit.haystack.mcheetah.visualization.Glyphs;
import edu.mit.haystack.mcheetah.visualization.Renderer;

/**
 * @author David Mascharka
 */
public class TECRenderer extends Renderer<GPSObservation> {

    public TECRenderer(Context context) {
        super(context);
        plotVertical = false;
    }

    /**
     * Holds the coordinates to draw axis lines
     */
    private FloatBuffer axesBuffer;

    /**
     * Whether to plot vertical (true) or slant (false) TEC
     */
    public boolean plotVertical;

    private DecimalFormat format;
    private long startTime;

    @Override
    public void addData(List<GPSObservation> observations) {
        // This is much faster than using an iterator
        GPSObservation o;
        int cutoff = (int) (Integer.MAX_VALUE*0.8);
        for (int i = observations.size() - 1; i > 0; i--) {
            o = observations.get(i);
            if (o.slantTEC >= cutoff) {
                observations.remove(i);
            }
        }

        numPoints = observations.size();

        if (numPoints == 0) {
            badDataListener.badData();
        }

        // Each point is 2 floats, each of which is 4 bytes
        dataPoints = ByteBuffer.allocateDirect(4 * 2 * numPoints).order(ByteOrder.nativeOrder()).asFloatBuffer();

        Comparator<GPSObservation> timeOrder = new Comparator<GPSObservation>() {
            @Override
            public int compare(GPSObservation o1, GPSObservation o2) {
                return o1.time.compareTo(o2.time);
            }
        };

        Collections.sort(observations, timeOrder);
        format = new DecimalFormat();
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);

        if (observations.size() == 0) {
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(observations.get(0).time.getTime());
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        startTime = cal.getTimeInMillis();
        for (int i = 0; i < observations.size(); i++) {
            dataPoints.put((float) (observations.get(i).time.getTime() - cal.getTimeInMillis()));

            if (plotVertical) {
                dataPoints.put((float) (observations.get(i).verticalTEC));
            } else {
                dataPoints.put((float) (observations.get(i).slantTEC));
            }
        }
        dataPoints.rewind();

        setPlotBounds();
    }

    @Override
    public void update(List<GPSObservation> observations) {
        for (int i = 0; i < observations.size(); i++) {
            dataPoints.put(2*i+1, plotVertical ? (float) observations.get(i).verticalTEC :
                                                    (float) observations.get(i).slantTEC);
        }
        dataPoints.rewind();
        setPlotBounds();
        setMinValue(0);
        setMaxValue(50);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);

        gl.glPointSize(5.0f);

        zoom = 1;

        setPlotBounds();

        glyphs = new Glyphs();
        glyphs.loadGlyphs(gl, context);
        setAxisTicks(5, 10);
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

        if (width > height) {
            setAxisTicks(xAxisTicks, 5);
        } else {
            setAxisTicks(5, 10);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        gl.glLoadIdentity();
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

        gl.glPushMatrix();
            gl.glScalef(zoom, zoom, 1);
            gl.glTranslatef(translateX, translateY, 0);
            gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glVertexPointer(2, GL10.GL_FLOAT, 0, dataPoints);
            gl.glDrawArrays(GL10.GL_POINTS, 0, numPoints);
            gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glPopMatrix();

        // Set the matrix for the entire window to draw the axes
        gl.glViewport(0, 0, viewportWidth, viewportHeight);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrthof(0, viewportWidth, 0, viewportHeight, -1.0f, 1.0f);
        gl.glMatrixMode(GL10.GL_MODELVIEW);

        // Draw axes
        gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, axesBuffer);
        gl.glDrawArrays(GL10.GL_LINES, 0, 2 * (xAxisTicks + yAxisTicks + 2));
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);

        drawAxisLabels(gl);

        // Reset the viewport to draw the graph
        gl.glViewport((int) (3 * xSpacing / 4), (int) (ySpacing / 2), graphWidth, graphHeight);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrthof(xMin, xMax, yMin, yMax, -1.0f, 1.0f);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
    }

    public void setAxisTicks(int xTicks, int yTicks) {
        xAxisTicks = xTicks;
        yAxisTicks = yTicks;

        axesBuffer = ByteBuffer.allocateDirect(4*4*(xTicks+yTicks+2)).order(ByteOrder.nativeOrder()).asFloatBuffer();

        float xStep = (viewportWidth-xSpacing)/xTicks;
        for (int i = 0; i <= xTicks; i++) {
            axesBuffer.put(3*xSpacing/4+xStep*i);
            axesBuffer.put(ySpacing/2);
            axesBuffer.put(3*xSpacing/4+xStep*i);
            axesBuffer.put(viewportHeight-ySpacing/2);
        }
        float yStep = (viewportHeight-ySpacing)/yTicks;
        for (int i = 0; i <= yTicks; i++) {
            axesBuffer.put(3*xSpacing/4);
            axesBuffer.put(ySpacing/2+yStep*i);
            axesBuffer.put(viewportWidth-xSpacing/4);
            axesBuffer.put(ySpacing/2+yStep*i);
        }
        axesBuffer.rewind();
    }

    private void drawAxisLabels(GL10 gl) {
        float xStep = (viewportWidth-xSpacing)/xAxisTicks;
        float yStep = (viewportHeight-ySpacing)/yAxisTicks;

        gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        // Draw x axis title centered
        glyphs.drawText(gl, "TIME", viewportWidth/2 - glyphs.getStringLength("TIME", Glyphs.SUBTEXT_MODE)/2,
                ySpacing/4, Glyphs.SUBTEXT_MODE);

        // Draw y axis title centered
        glyphs.drawText(gl, "TEC", viewportHeight/2 - glyphs.getStringLength("TEC", Glyphs.SUBTEXT_MODE) / 2,
                -glyphs.getHeight(Glyphs.SUBTEXT_MODE), Glyphs.SUBTEXT_MODE, 90);

        // Draw axis labels
        float point;
        String str;
        for (int i = 0; i <= xAxisTicks; i++) {
            point = ((xMax-xMin) / xAxisTicks*i + xMin - translateX) / zoom;
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis((long) point + startTime);
            str = cal.get(Calendar.HOUR_OF_DAY) + ":" + (cal.get(Calendar.MINUTE) < 10 ? "0" : "") +
                    cal.get(Calendar.MINUTE)  + ":" + cal.get(Calendar.SECOND);
            glyphs.drawText(gl, str, 3*xSpacing/4+xStep*i - glyphs.getStringLength(str, Glyphs.SUBTEXT_MODE)/2,
                    ySpacing/2 - glyphs.getHeight(Glyphs.SUBTEXT_MODE), Glyphs.SUBTEXT_MODE);
        }

        for (int i = 0; i <= yAxisTicks; i++) {
            point = ((yMax-yMin)/yAxisTicks*i + yMin - translateY) / zoom;
            str = format.format(point);
            glyphs.drawText(gl, str, 3*xSpacing/4 - glyphs.getStringLength(str, Glyphs.SUBTEXT_MODE),
                    ySpacing/2+yStep*i, Glyphs.SUBTEXT_MODE);
        }
    }
}

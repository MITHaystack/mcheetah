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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.util.ArrayList;

import edu.mit.haystack.mcheetah.DataProcessFragment;
import edu.mit.haystack.mcheetah.R;
import edu.mit.haystack.mcheetah.parsing.PointSkipSelectionDialogFragment;
import edu.mit.haystack.mcheetah.visualization.Renderer;

/**
 * @author David Mascharka
 */
public class ExampleDataViewActivity extends AppCompatActivity implements DataProcessFragment.ActivityEndRequestedListener,
        DataProcessFragment.DisplayPointDensityListener, Renderer.BadDataListener {

    private DataProcessFragment<ExampleData> dataFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment_data_process);

        // If we're resuming then we already have a plot being held - get that now
        FragmentManager fragmentManager = getSupportFragmentManager();
        dataFragment = (DataProcessFragment<ExampleData>) fragmentManager.findFragmentByTag("ExamplePlot");

        if (dataFragment == null) {
            // If the fragment does't exist, this is the first time creating the activity so we should
            // do all the computation now
            ExampleRenderer renderer = new ExampleRenderer(this);
            dataFragment = new DataProcessFragment<ExampleData>();
            dataFragment.setParserClass(ExampleParser.class);
            ExampleComputer computer = new ExampleComputer();

            computer.setComputeThreads(2);

            dataFragment.setComputer(computer);
            ArrayList<ExampleData> theData = new ArrayList<ExampleData>();
            dataFragment.setDataObject(theData);
            dataFragment.setRenderer(renderer);

            getSupportFragmentManager().beginTransaction().replace(R.id.data_plot, dataFragment, "ExamplePlot").commit();
        } else {
            getSupportFragmentManager().beginTransaction().replace(R.id.data_plot, dataFragment, "ExamplePlot").commit();
            dataFragment.redisplay();
        }
    }

    /**
     * Goes back to the parent activity
     */
    private void endSelf() {
        Intent intent = new Intent(this, ExampleActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onEndRequested() {
        endSelf();
    }

    @Override
    public void badData() {
        Toast.makeText(this, "Data was bad", Toast.LENGTH_LONG).show();
        endSelf();
    }

    @Override
    public void displayPointDensityDialog() {
        PointSkipSelectionDialogFragment f = new PointSkipSelectionDialogFragment();
        f.setListener(dataFragment);
        f.show(getSupportFragmentManager(), "Point Density");
    }
}

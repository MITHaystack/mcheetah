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
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;

import edu.mit.haystack.mcheetah.DataProcessFragment;
import edu.mit.haystack.mcheetah.R;

/**
 * @author David Mascharka
 *
 * Simple activity that just holds a button
 * When the user clicks the button, computation starts on 3 files in parallel
 */
public class ExampleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_example);

        ((Button) findViewById(R.id.activity_example_plot_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle extras = new Bundle();

                ArrayList<String> filePaths = new ArrayList<String>(3);
                filePaths.add("/sdcard/mcheetah_example_1.txt");
                filePaths.add("/sdcard/mcheetah_example_2.txt");
                filePaths.add("/sdcard/mcheetah_example_3.txt");

                extras.putStringArrayList(DataProcessFragment.FILE_PATH_KEY, filePaths);
                extras.putInt(DataProcessFragment.DATA_DENSITY_KEY, 1);
                extras.putInt(DataProcessFragment.PARSER_THREAD_KEY, 3);
                extras.putString(DataProcessFragment.SHARED_PREFERENCES_KEY, "ExamplePrefs");
                Intent intent = new Intent(getApplicationContext(), ExampleDataViewActivity.class);
                intent.putExtras(extras);
                startActivity(intent);
            }
        });

        ((Button) findViewById(R.id.activity_example_setup_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Setup.createDataFiles();
            }
        });
    }
}

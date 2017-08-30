package edu.mit.haystack.mcheetah.parsing;

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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import edu.mit.haystack.mcheetah.R;

/**
 * @author David Mascharka
 *
 * Class to display a dialog letting the user specify is they want to plot every point or parts of
 * the data
 *
 * Very useful for lower-powered devices if you want to plot a full day
 * Eliminating no points a day can be ~600,000-800,000 datapoints, a lot of computation
 * By taking every other observation time or every third observation time we can save a lot of
 * computation and memory
 */
public class PointSkipSelectionDialogFragment extends DialogFragment {
    /**
     * This passes back the data density the user wants to compute (every other point, every third point)
     */
    public interface PointSkipSelectionDialogListener {
        public void onSkipSelected(int density);
    }

    PointSkipSelectionDialogListener listener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();

        final View view = inflater.inflate(R.layout.dialog_select_point_skip, null);

        ((SeekBar) view.findViewById(R.id.point_skip_dialog_skip_bar)).setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        TextView text = (TextView) view.findViewById(R.id.point_skip_dialog_text_skip_amount);
                        progress++; // make it go 1-10
                        String update = "Use every ";
                        if (progress == 2) {
                            update += "2nd ";
                        } else if (progress == 3) {
                            update += "3rd ";
                        } else if (progress > 1) {
                            update += progress + "th ";
                        }

                        update += "point";

                        text.setText(update);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });

        builder.setTitle(R.string.point_skip_dialog_title);
        builder.setView(view);
        builder.setCancelable(false);
        builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    // back button pressed - don't cancel this - do nothing
                    return true;
                }

                return false;
            }
        });
        builder.setPositiveButton("Plot", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listener.onSkipSelected(((SeekBar) view.findViewById(R.id.point_skip_dialog_skip_bar)).getProgress() + 1);
            }
        });

        return builder.create();
    }

    public void setListener(PointSkipSelectionDialogListener l) {
        listener = l;
    }
}
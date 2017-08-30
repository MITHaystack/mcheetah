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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import edu.mit.haystack.mahalirelayapp.R;

/**
 * @author David Mascharka
 *
 * Displays a dialog with two EditTexts allowing the user to select to remove points from their plot that
 * have a TEC value less than the minimum specified or greater than the maximum specified
 */
public class OutlierRemovalSelectionDialogFragment extends DialogFragment {
    /**
     * Passes back the minimum and maximum values to display to a listener
     */
    public interface OutlierRemovalDialogListener {
        public void onOutlierValuesSelected(float minValue, float maxValue);
    }

    OutlierRemovalDialogListener listener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();

        final View view = inflater.inflate(R.layout.dialog_select_outliers, null);

        builder.setTitle(R.string.outlier_selection_dialog_title)
                .setView(view)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText minimum = (EditText) view.findViewById(R.id.outlier_dialog_minimum);
                        EditText maximum = (EditText) view.findViewById(R.id.outlier_dialog_maximum);

                        listener.onOutlierValuesSelected(Float.parseFloat(minimum.getText().toString()),
                                Float.parseFloat(maximum.getText().toString()));
                    }
                });

        return builder.create();
    }

    public void setListener(OutlierRemovalDialogListener l) {
        listener = l;
    }
}
package edu.mit.haystack.mahalirelayapp.position;

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
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import edu.mit.haystack.mahalirelayapp.MahaliData;
import edu.mit.haystack.mahalirelayapp.R;

/**
 * @author David Mascharka
 *
 * Allows the user to type in their position manually using 3 dialog boxes
 */
public class ManualPositionDialogFragment extends DialogFragment {

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final LayoutInflater inflater = getActivity().getLayoutInflater();

        final View view = inflater.inflate(R.layout.dialog_manual_position, null);

        builder.setTitle(R.string.dialog_position_title)
                .setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText latitude = (EditText) view.findViewById(R.id.position_dialog_latitude);
                        EditText longitude = (EditText) view.findViewById(R.id.position_dialog_longitude);
                        EditText elevation = (EditText) view.findViewById(R.id.position_dialog_elevation);

                        try {
                            MahaliData.mahaliLatitude = Float.parseFloat(latitude.getText().toString());
                        } catch (Exception e) {
                            // set to min value if there's an error - user will be forced to re-input later
                            MahaliData.mahaliLatitude = Float.MIN_VALUE;
                        }

                        try {
                            MahaliData.mahaliLongitude = Float.parseFloat(longitude.getText().toString());
                        } catch (Exception e) {
                            MahaliData.mahaliLongitude = Float.MIN_VALUE;
                        }

                        try {
                            MahaliData.mahaliElevation = Float.parseFloat(elevation.getText().toString());
                        } catch (Exception e) {
                            MahaliData.mahaliElevation = Float.MIN_VALUE;
                        }
                    }
                });

        return builder.create();
    }
}
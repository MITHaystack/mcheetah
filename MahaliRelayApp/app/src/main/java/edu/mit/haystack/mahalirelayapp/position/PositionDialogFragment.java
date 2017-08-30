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
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import edu.mit.haystack.mahalirelayapp.R;

/**
 * @author David Mascharka
 *
 * Allows the user to set a position by either typing it out or selecting it from Google Maps
 * (if Google Maps is supported on the device)
 *
 * Performs a check to see if Google Play Services is available
 * If not, does not allow the option of selecting position on a map, only typing it because Google Maps
 *      is not supported without Google Play Services
 */
public class PositionDialogFragment extends DialogFragment {

    boolean playServicesSupported;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Check if Google Play Services is supported
        // We have to do this in onAttach instead of in the constructor because when it's constructed it doesn't
        // have an activity
        int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());
        if (result != ConnectionResult.SUCCESS) {
            ManualPositionDialogFragment f = new ManualPositionDialogFragment();
            f.show(getFragmentManager(), "MahaliPositionDialog");
            this.dismiss();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.dialog_position_title)
                .setItems(new CharSequence[]{"Select On Map", "Type Position"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            Intent mapsIntent = new Intent(getActivity(), MapsActivity.class);
                            getActivity().startActivity(mapsIntent);
                        } else {
                            ManualPositionDialogFragment f = new ManualPositionDialogFragment();
                            f.show(getFragmentManager(), "MahaliPositionDialog");
                        }
                    }
                });

        return builder.create();
    }
}
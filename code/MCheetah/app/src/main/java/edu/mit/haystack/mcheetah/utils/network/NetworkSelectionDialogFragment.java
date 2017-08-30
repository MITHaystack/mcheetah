package edu.mit.haystack.mcheetah.utils.network;

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
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import edu.mit.haystack.mcheetah.R;

/**
 * @author David Mascharka
 *
 * Creates a dialog asking the user to select which Mahali box they want to connect to
 * Needed in the event there are multiple Mahali boxes in range
 *
 * A class using this MUST implement NetworkSelectionDialogListener
 */
public class NetworkSelectionDialogFragment extends DialogFragment {

    // Used to pass results back to the owner
    // The class using this dialog HAS TO implement this, otherwise an Exception will be thrown
    public interface NetworkSelectionDialogListener {
        /**
         * Which network the user decided to connect to
         *
         * WARNING: Do NOT try to simplify this and just pass in the BSSID. I did and Android freaked
         * out trying to set a WifiConfiguration object to have a BSSID and no SSID. I ended up getting
         * a network configuration with no SSID or BSSID with a high priority and had to clear it in code
         * Just don't do it.
         *
         * @param bssid the network BSSID
         * @param ssid the network SSID
         */
        public void onNetworkSelected(String bssid, String ssid);
    }

    private Context context;

    private NetworkSelectionDialogListener selectionListener;
    private AlertDialog dialog;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        context = (Context) activity;
    }

    public void setSelectionListener(NetworkSelectionDialogListener listener) {
        selectionListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        dialog = null;

        // Arguments specifying the SSIDs and signal strengths of all the Mahali boxes in range will
        // be passed to the dialog
        Bundle bundle = getArguments();

        // Bundle should never be null
        if (bundle != null) {
            final String[] ssids = bundle.getStringArray("ssids");
            final String[] bssids = bundle.getStringArray("bssids");
            final String[] signalStrengths = bundle.getStringArray("signal_strengths");

            // Create the dialog displaying the SSID and signal strength for each Mahali box in range
            final String[] options = new String[ssids.length];
            for (int i = 0; i < ssids.length; i++) {
                options[i] = ssids[i] + " strength " + signalStrengths[i];
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Choose a network");
            builder.setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    selectionListener.onNetworkSelected(bssids[which], ssids[which]);
                }
            });

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = inflater.inflate(R.layout.dialog_network_selection, null);
            builder.setView(v);

            dialog = builder.create();
        } else {
            Log.wtf("Mahali", "Arguments was null in NetworkSelectionDialogFragment");
        }

        return dialog;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Stop the dialog if the box is currently displayed because a new scan will happen as soon
        // as the activity is recreated (such as on a screen rotation) and this may get created again
        // Without this, if you rotate the screen through a few orientation changes you can get a lot
        // of dialogs on a backstack that you have to click through to dismiss
        if (dialog != null) {
            dialog.cancel();
        }
    }
}

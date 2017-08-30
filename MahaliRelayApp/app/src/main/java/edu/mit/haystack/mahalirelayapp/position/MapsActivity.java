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

import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import edu.mit.haystack.mahalirelayapp.MahaliData;
import edu.mit.haystack.mahalirelayapp.R;

/**
 * @author David Mascharka
 *
 * Displays a map of the user's position so they can indicate the location of the Mahali box
 * they are connected to
 */
public class MapsActivity extends AppCompatActivity implements GoogleMap.OnMyLocationChangeListener,
        GoogleMap.OnMapClickListener, GoogleMap.OnMarkerDragListener {

    private GoogleMap map; // Might be null if Google Play services APK is not available.

    private float latitude;
    private float longitude;
    private float elevation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();

        ((Button) findViewById(R.id.button_select_position)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If the user didn't select a position, make them
                // Hopefully we never have a Mahali box at (0,0)
                // I doubt we will since it's in the middle of the ocean off Africa
                if (latitude == 0 && longitude == 0) {
                    Toast.makeText(getApplicationContext(), "Please select the Mahali position", Toast.LENGTH_SHORT).show();
                    return;
                }

                MahaliData.mahaliLatitude = latitude;
                MahaliData.mahaliLongitude = longitude;
                MahaliData.mahaliElevation = elevation;
                finish();
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // do nothing - prevent user from leaving without selecting a position
            return true;
        }

        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #map} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (map == null) {
            // Try to obtain the map from the SupportMapFragment.
            map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
            // Check if we were successful in obtaining the map.
            if (map != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #map} is not null.
     */
    private void setUpMap() {
        map.setMyLocationEnabled(true);
        map.setOnMyLocationChangeListener(this);
        map.setOnMapClickListener(this);

        UiSettings uiSettings = map.getUiSettings();
        uiSettings.setMyLocationButtonEnabled(false);
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setCompassEnabled(true);
    }

    /**
     * We need latitude, longitude, and height. Since the user marker doesn't report height but only
     * latitude and longitude, we record height any time the user location changes
     *
     * @param location the user's new location
     */
    @Override
    public void onMyLocationChange(Location location) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 20));
        elevation = (float) location.getAltitude();
    }

    /**
     * When the user clicks on the map, plop down a marker to indicate the position of the Mahali box
     *
     * @param latLng the latitude and longitude of the Mahali box
     */
    @Override
    public void onMapClick(LatLng latLng) {
        map.clear();

        map.addMarker(new MarkerOptions().position(latLng).title("Mahali Box").draggable(true));
        map.setOnMarkerDragListener(this);
        latitude = (float) latLng.latitude;
        longitude = (float) latLng.longitude;
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    /**
     * Keep track of the position the user marks down to pass back to the activity
     *
     * @param marker the marker being dragged indicating Mahali position
     */
    @Override
    public void onMarkerDragEnd(Marker marker) {
        latitude = (float) marker.getPosition().latitude;
        longitude = (float) marker.getPosition().longitude;
    }
}
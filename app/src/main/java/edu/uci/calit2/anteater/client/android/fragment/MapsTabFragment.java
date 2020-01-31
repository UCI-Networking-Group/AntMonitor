/*
 *  This file is part of AntMonitor <https://athinagroup.eng.uci.edu/projects/antmonitor/>.
 *  Copyright (C) 2018 Anastasia Shuba and the UCI Networking Group
 *  <https://athinagroup.eng.uci.edu>, University of California, Irvine.
 *
 *  AntMonitor is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 2 of the License.
 *
 *  AntMonitor is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with AntMonitor. If not, see <http://www.gnu.org/licenses/>.
 */
package edu.uci.calit2.anteater.client.android.fragment;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import edu.uci.calit2.anteater.R;

/**
 * @author Emmanouil Alimpertis
 */
public class MapsTabFragment extends Fragment implements OnInfoWindowClickListener {

    //TODO: fix zoom in user's current location does not work
    //TODO: pop up/bubble with user's signals
    //TODO: neighboring towers (?)
    private LatLng defaultLatLng = new LatLng(33.644785, -117.8299873);
    private GoogleMap map;
    private int zoomLevel = 10;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.signals_tab4_maps, container, false);


        try {
            map = ((SupportMapFragment) getChildFragmentManager().findFragmentById(R.id. map)).getMap();
            if (map!=null){
                map.getUiSettings().setCompassEnabled(true);
                map.setTrafficEnabled(true);
                map.setMyLocationEnabled(true);


                // Move the camera instantly to defaultLatLng.
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, zoomLevel));

                // Getting LocationManager object from System Service LOCATION_SERVICE
                LocationManager locationManager = (LocationManager) this.getActivity().getSystemService(Context.LOCATION_SERVICE);

                // Creating a criteria object to retrieve provider
                Criteria criteria = new Criteria();

                // Getting the name of the best provider
                String provider = locationManager.getBestProvider(criteria, true);

                // Getting Current Location
                Location location = locationManager.getLastKnownLocation(provider);

                if(location!=null) {
                    // Getting latitude of the current location
                    double latitude = location.getLatitude();

                    // Getting longitude of the current location
                    double longitude = location.getLongitude();

                    // Creating a LatLng object for the current location
                    LatLng latLng = new LatLng(latitude, longitude);

                    LatLng myPosition = new LatLng(latitude, longitude);

                    map.addMarker(new MarkerOptions().position(myPosition).title("Start"));


                    map.addMarker(new MarkerOptions().position(defaultLatLng)
                            .title("This is the title")
                            .snippet("This is the snippet within the InfoWindow")
                            .icon(BitmapDescriptorFactory
                                    .fromFile("check_mark.png")));


                    map.setOnInfoWindowClickListener(this);

                }
            }


        }catch (NullPointerException e) {
            e.printStackTrace();
        }

        return v;
    }


    @Override
    public void onPause() {
        if (map != null){
            map.setMyLocationEnabled(false);
            map.setTrafficEnabled(false);
        }
        super.onPause();
    }



    @Override
    public void onInfoWindowClick(Marker marker) {
        //Intent intent = new Intent(this, NewActivity.class);
        //intent.putExtra("snippet", marker.getSnippet());
        //intent.putExtra("title", marker.getTitle());
        //intent.putExtra("position", marker.getPosition());
        //startActivity(intent);
    }


}

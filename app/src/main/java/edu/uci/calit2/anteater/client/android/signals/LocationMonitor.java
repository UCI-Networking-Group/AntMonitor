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
package edu.uci.calit2.anteater.client.android.signals;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import edu.uci.calit2.anteater.client.android.analysis.AnalysisHelper;

/**
 * @author Emmanouil Alimpertis
 */
//TODO smarter handling of the connection state with google api client
// (see that returns null if connected)
public class LocationMonitor implements
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private static final String TAG = LocationMonitor.class.getSimpleName();
    private Activity parentActivity;
    Context mCtx;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private Location mLastLocation;
    private StringBuffer myLocSummary;
    private String myLocLatitude;
    private String myLocLongitude;
    private long lastConnectionAttempted;

    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;
    //the Request for receiving the location
    private LocationRequest mLocationRequest;
    // boolean flag to toggle periodic location updates.
    private boolean mRequestingLocationUpdates = true;
    //boolean flag to mark if Google Play Services & Fused Location API are available
    private boolean isFusedLocationServicesAvailable=false;


    private static final long ONE_MIN= 1000 * 60; //in msec
    private static final long UPDATE_INTERVAL = ONE_MIN *30; // 30 min.
    private static final long UPDATE_FASTEST_INTERVAL = ONE_MIN; // 1 min.

    /** Formatter for location values */
    private final NumberFormat locFormat;

    //Singleton
    private static LocationMonitor singleton;

    public static LocationMonitor getInstance(Context c){
        if(singleton==null)
            singleton = new LocationMonitor(c);
        return singleton;
    }

    private LocationMonitor(Context c){

        //this.parentActivity=callingParentActivity;
        this.mCtx=c;
        this.createLocationRequest();
        this.myLocSummary=new StringBuffer("Unknown|Unknown");
        mLastLocation=new Location("dumpyProvider");//mLastLocation can't be null

        if (checkPlayServices()) {// First we need to check availability of play services
            buildGoogleApiClient();// Building the GoogleApi client
        }
        if(mGoogleApiClient!=null) {
            lastConnectionAttempted=System.currentTimeMillis();
            mGoogleApiClient.connect();
        }
        else
            this.isFusedLocationServicesAvailable=false;

        // Prepare location formatter: be consistent and use '.' for decimal separation,
        // regardless of where we are in the world
        locFormat = NumberFormat.getInstance(Locale.US);
        locFormat.setMaximumFractionDigits(1);
        locFormat.setRoundingMode(RoundingMode.DOWN);
    }

    /**
     * Creating location request object
     * */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL); //30 min update window, low power impact
        mLocationRequest.setFastestInterval(UPDATE_FASTEST_INTERVAL); //1 min update if a Location update is available by other applications
    }

    /**
     * Creating google API client object
     * */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(mCtx)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    @Override
    public void onConnected(Bundle arg0) {

        // Once connected with google api, get the location
        //printLocation();
        Location tempLocObj;
        Log.i(TAG,"CONNECTED");
        this.isFusedLocationServicesAvailable=true;
        synchronized (this.mLastLocation){
            tempLocObj=LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if(tempLocObj==null) {//no location available
                this.myLocSummary.setLength(0);
                this.myLocSummary.append("Unknown|Unknown");
                myLocLatitude = null;
                myLocLongitude = null;
            }
            else{
                setLocationStrings(tempLocObj);
            }
        }
        if (mRequestingLocationUpdates) {
            this.startLocationUpdates();
        }
    }


    /**
     * Method to verify google play services on the device
     * */
    private boolean checkPlayServices() {

        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(mCtx);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                //TODO enable user resolution
               // GooglePlayServicesUtil.getErrorDialog(resultCode, this.parentActivity,
               //         PLAY_SERVICES_RESOLUTION_REQUEST).show();
                Log.e(TAG, "GooglePlayServices Problem Occured");
            } else {
                Log.e(TAG, "This device is not supported");
                //finish();
            }
            this.isFusedLocationServicesAvailable=false;
            return false;
        }

        return true;
    }

    /**
     * Method to print the location in console, used mainly for debug
     * */
    private void printLocation() {

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLastLocation != null) {
            double latitude = mLastLocation.getLatitude();
            double longitude = mLastLocation.getLongitude();

            Log.i(TAG,"location is: "+ latitude + ", " + longitude + " Boolean var: "+this.isFusedLocationServicesAvailable);
            Log.i(TAG, "provider is: " + mLastLocation.getProvider() + " accuracy is: " + mLastLocation.getAccuracy() + " Time/age is: " + mLastLocation.getTime());

        } else {
            Log.i(TAG, "(Couldn't get the location. Make sure location is enabled on the device)");
        }
    }

    /**
     * Google api callback methods
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {

        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
        /*TODO finish the implementation for the mechanism of failure resolution (2nd argument of )
        the second argument of  startResolutionForResult must be determined in order to proceed
        */
        /*if (!result.hasResolution()){
            // Show the localized error dialog
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this.parentActivity, 0).show();
        }
        else{
            try {
                Log.i(TAG, "Attempting to resolve failed connection");
                result.startResolutionForResult(this.parentActivity, result.getErrorCode());
            }
            catch (IntentSender.SendIntentException e) {
            Log.e(TAG,"Exception while starting resolution activity", e);
            }
        }
        //mConnectionResult = result;
        */
        this.isFusedLocationServicesAvailable=false;
    }

    public void onConnectionSuspended(int arg0) {
        //GoogleApiClient will automatically try to reconnect according to the Google API documentation
        this.isFusedLocationServicesAvailable=false;
        Log.i(TAG,"CONNECTion SUSPENDED");

    }


    /**
     * Starting the location updates
     * */
    protected void startLocationUpdates() {

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * Stopping location updates
     */
    protected void stopLocationUpdates() {

        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onLocationChanged(Location location) {

        //printLocation();
        synchronized (this.mLastLocation){
            if(location==null) {//no location available
                this.myLocSummary.setLength(0);
                this.myLocSummary.append("Unknown|Unknown");
            }
            else{
                setLocationStrings(location);
            }

        }
    }

    private void setLocationStrings(Location location) {
        this.mLastLocation=location;
        this.myLocSummary.setLength(0);
        this.myLocSummary.append(mLastLocation.getLatitude()).append("|").append(mLastLocation.getLongitude());

        // Truncate the location to an integer, and add a period at the end so that we only catch
        // leaks with the lat and lon with decimal values. Otherwise we get many false alarms.
        String latTemp = locFormat.format(mLastLocation.getLatitude());
        String lonTemp = locFormat.format(mLastLocation.getLongitude());

        // If the formatted location is the same, don't bother rebuilding search tree
        if (latTemp.equals(myLocLatitude) && lonTemp.equals(myLocLongitude))
            return;

        myLocLatitude = latTemp;
        myLocLongitude = lonTemp;

        // Update search tree
        AnalysisHelper.startRebuildSearchTree(mCtx);
    }

    public String getLocationStr() {

        if(!isFusedLocationServicesAvailable){
            //*5(number of minutes) in msec=300000
            if (Math.abs(this.lastConnectionAttempted-System.currentTimeMillis())>300000){
                if(mGoogleApiClient!=null) {
                    this.lastConnectionAttempted=System.currentTimeMillis();
                    mGoogleApiClient.connect();
                    Log.i(TAG,"TRYING TO RECONNECT");
                }
            }
        }

        synchronized (this.mLastLocation){
            if(!isFusedLocationServicesAvailable)
                return "Unknown";
            else
                return myLocSummary.toString();
        }
    }

    /**
     * @return the current latitude, or {@code null} if location is unavailable
     */
    public String getLatitude() {
        synchronized (this.mLastLocation){
            if(!isFusedLocationServicesAvailable)
                return null;
            else
                return myLocLatitude;
        }
    }

    /**
     * @return the current longitude, or {@code null} if location is unavailable
     */
    public String getLongitude() {
        synchronized (this.mLastLocation){
            if(!isFusedLocationServicesAvailable)
                return null;
            else
                return myLocLongitude;
        }
    }

    /**
     * @return for latitude: a list of strings that is +/- 0.1 of the value including the current value
     */
    public List<String> getLatitudeValues() {
        return getRoundedLocationValues(this.getLatitude());
    }

    /**
     * @return for longitude: a list of strings that is +/- 0.1 of the value including the current value
     */
    public List<String> getLongitudeValues() {
        return getRoundedLocationValues(this.getLongitude());
    }

    private List<String> getRoundedLocationValues(String value) {
        List<String> values = new ArrayList<>();
        if (value != null) {
            values.add(value);

            double valueD = 0;
            try {
                valueD = locFormat.parse(value).doubleValue();
            } catch (ParseException e) {
                Log.w(TAG, "Could not parse location value to a double: " + value);

                // Return with just the original value
                return values;
            }

            String upperValue = locFormat.format(valueD + 0.1);
            values.add(upperValue);

            String lowerValue = locFormat.format(valueD - 0.1);
            values.add(lowerValue);
        }
        return values;
    }

}

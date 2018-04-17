package com.marius.pathmap.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.model.LatLng;
import com.marius.pathmap.model.User;
import com.marius.pathmap.utils.PathMapSharedPreferences;

public class TrackingService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final String TAG = TrackingService.class.getSimpleName();

    private static final long INTERVAL = 1000;
    private static final long FASTEST_INTERVAL = 1000;
    private static final float SMALLEST_DISPLACEMENT = 0.25F;

    public static final String ACTION = "com.marius.pathmap.service.TrackingService";
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private double latitudeValue = 0.0;
    private double longitudeValue = 0.0;
    private long startTimeInMilliSeconds = 0L;
    private boolean isServiceRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        if(isRouteTrackingOn()){
            startTimeInMilliSeconds = System.currentTimeMillis();
            Log.d(TAG, "Current time " + startTimeInMilliSeconds);
            Log.d(TAG, "Service is running");
        }
        mLocationRequest = createLocationRequest();
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            mGoogleApiClient.connect();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isServiceRunning = true;
        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Connection method has been called");
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                            if (mLastLocation != null) {
                                latitudeValue = mLastLocation.getLatitude();
                                longitudeValue = mLastLocation.getLongitude();
                                Log.d(TAG, "Latitude 1: " + latitudeValue + " Longitude 1: " + longitudeValue);
                                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, TrackingService.this);
                            }
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    protected LocationRequest createLocationRequest() {
        @SuppressLint("RestrictedApi") LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setSmallestDisplacement(SMALLEST_DISPLACEMENT);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Latitude " + location.getLatitude() + " Longitude " + location.getLongitude());
        Log.d(TAG, "SERVICE RUNNING " + isServiceRunning);
        if(isRouteTrackingOn() && startTimeInMilliSeconds == 0){
            startTimeInMilliSeconds = System.currentTimeMillis();
        }
        if(isRouteTrackingOn() && startTimeInMilliSeconds > 0){
            latitudeValue = location.getLatitude();
            longitudeValue = location.getLongitude();
            Log.d(TAG, "Latitude " + latitudeValue + " Longitude " + longitudeValue);
            // insert values to local sqlite database
            User.getInstance().addPoint(new LatLng(latitudeValue,longitudeValue));
            // send local broadcast receiver to application components
            Intent localBroadcastIntent = new Intent(ACTION);
            localBroadcastIntent.putExtra("RESULT_CODE", "LOCAL");
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localBroadcastIntent);
            long timeoutTracking = 2 * 60 * 60 * 1000;
            if(System.currentTimeMillis() >= startTimeInMilliSeconds + timeoutTracking){
                //turn of the tracking
                Log.d(TAG, "SERVICE HAS BEEN STOPPED");
                this.stopSelf();
            }
        }
        if(!isRouteTrackingOn()){
            Log.d(TAG, "SERVICE HAS BEEN STOPPED 1");
            isServiceRunning = false;
            Log.d(TAG, "SERVICE STOPPED " + isServiceRunning);
            this.stopSelf();
        }
    }
    private boolean isRouteTrackingOn(){
        Log.d(TAG, "SERVICE STATE " + PathMapSharedPreferences.getInstance(getApplicationContext()).getTrackingState());
        return PathMapSharedPreferences.getInstance(getApplicationContext()).getTrackingState();
    }

    @Override
    public void onDestroy() {
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }
}
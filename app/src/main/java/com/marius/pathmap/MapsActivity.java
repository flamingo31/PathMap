package com.marius.pathmap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Geocoder;
import android.location.Location;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.marius.pathmap.gps.GPSTracker;
import com.marius.pathmap.model.User;
import com.marius.pathmap.service.TrackingService;
import com.marius.pathmap.utils.PathMapSharedPreferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMarkerClickListener {

    private static final String TAG = MapsActivity.class.getSimpleName();

    private static final String SAVING_STATE_POINTS = "savingStatePoints";

    private GoogleMap mMap;
    private Switch trackOnOff;

    private static final long INTERVAL = 1000;
    private static final long FASTEST_INTERVAL = 1000;
    private static final float SMALLEST_DISPLACEMENT = 0.25F;

    private static final int PERMISSION_LOCATION_REQUEST_CODE = 101;

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private double latitudeValue = 0.0;
    private double longitudeValue = 0.0;
    private RouteBroadCastReceiver routeReceiver;
    private List<LatLng> startToPresentLocations;
    private List<LatLng> mlocationPoints;
    private List<LatLng> gpslocationPoints;
    private GPSTracker gps;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_LOCATION_REQUEST_CODE);
        gps = new GPSTracker(this);
        startToPresentLocations = User.getInstance().getPoints();
        mlocationPoints = new ArrayList<>();
        gpslocationPoints = new ArrayList<>();
        mLocationRequest = createLocationRequest();
        routeReceiver = new RouteBroadCastReceiver();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        trackOnOff = (Switch) findViewById(R.id.trackOnOff);

        trackOnOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    PathMapSharedPreferences.getInstance(getApplicationContext()).saveTrackingState(true);
                    Intent intent = new Intent(getApplicationContext(), TrackingService.class);
                    startService(intent);
                    if (!User.getInstance().isPointsEmpty()) {
                        User.getInstance().addStartTime(Calendar.getInstance().getTime());
                    } else {
                        if (gps.canGetLocation()) {
                            User.getInstance().addStartTime(Calendar.getInstance().getTime());
                            double latitude = gps.getLatitude();
                            double longitude = gps.getLongitude();
                            gpslocationPoints.add(new LatLng(latitude, longitude));
                            markDynamicLocationOnMap(mMap, gpslocationPoints);
                            drawRouteOnMap(mMap, gpslocationPoints);
                        } else {
                            Toast.makeText(getApplicationContext(), "Tracking service(on network/gps) is not working.", Toast.LENGTH_LONG).show();
                        }
                    }
                } else if (!isChecked) {
                    if (!User.getInstance().isPointsEmpty()) {
                        User.getInstance().addEndTime(Calendar.getInstance().getTime());
                    }
                    PathMapSharedPreferences.getInstance(getApplicationContext()).removeTrackingState();
                    List<LatLng> locationPoints = getPoints(User.getInstance().getPoints());
                    if (locationPoints.size() > 0) {
                        markDynamicLocationOnMap(mMap, locationPoints);
                    } else {
                        gpslocationPoints.clear();
                        if (gps.canGetLocation()) {
                            User.getInstance().addEndTime(Calendar.getInstance().getTime());
                            double latitude = gps.getLatitude();
                            double longitude = gps.getLongitude();
                            gpslocationPoints.add(new LatLng(latitude, longitude));
                            markDynamicLocationOnMap(mMap, gpslocationPoints);
                        } else {
                            Toast.makeText(getApplicationContext(), "There is a service error. Try to reload the app.", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        });

    }

    private void initListeners() {
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnInfoWindowClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.showJourneys:
                showJourneysList();
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        if (!User.getInstance().isPointsEmpty()) {
            outState.putParcelableArrayList(SAVING_STATE_POINTS, User.getInstance().getPoints());
        }
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (User.getInstance().isPointsEmpty()) {
            User.getInstance().setPoints(savedInstanceState.getParcelableArrayList(SAVING_STATE_POINTS));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_LOCATION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (gps.canGetLocation()) {
                        double latitude = gps.getLatitude();
                        double longitude = gps.getLongitude();
                        gpslocationPoints.add(new LatLng(latitude, longitude));
                        markDynamicLocationOnMap(mMap, gpslocationPoints);
                    }
                } else
                    Toast.makeText(this, "Location Permission Denied", Toast.LENGTH_SHORT).show();
                break;
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        initListeners();
    }

    private void markStartingLocationOnMap(GoogleMap mapObject, LatLng location) {
        mapObject.addMarker(new MarkerOptions().position(location).title("Current location"));
        initCamera(mapObject,location);
    }

    private void markDynamicLocationOnMap(GoogleMap mapObject, List<LatLng> locations) {
        for (LatLng location : locations) {
            refreshMap(mMap);
            mapObject.addMarker(new MarkerOptions().position(location).title("Current location"));
            initCamera(mapObject,location);
        }
    }

    private void initCamera(GoogleMap mapObject, LatLng location) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        }
        CameraPosition position = CameraPosition.builder()
                .target(location)
                .zoom(16f)
                .bearing(0.0f)
                .tilt(0.0f)
                .build();

        mapObject.animateCamera(CameraUpdateFactory
                .newCameraPosition(position), null);

        mapObject.setTrafficEnabled(true);
        mapObject.setMyLocationEnabled(true);
        mapObject.getUiSettings().setZoomControlsEnabled( true );
    }

    private String getAddressFromLatLng(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this);
        String address = "";
        try {
            address = geocoder
                    .getFromLocation( latLng.latitude, latLng.longitude, 1 )
                    .get( 0 ).getAddressLine( 0 );
        } catch (IOException e ) {
            Toast.makeText(getApplicationContext(), "No marker information at this moment.", Toast.LENGTH_LONG).show();
        }
        return address;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        marker.showInfoWindow();
        return true;
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
                                Log.d(TAG, "Latitude 4: " + latitudeValue + " Longitude 4: " + longitudeValue);
                                refreshMap(mMap);
                                if(!User.getInstance().isPointsEmpty()){
                                    mlocationPoints.add(new LatLng(latitudeValue, longitudeValue));
                                    markDynamicLocationOnMap(mMap, mlocationPoints);
                                } else {
                                    markStartingLocationOnMap(mMap, new LatLng(latitudeValue, longitudeValue));
                                }
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
        Log.d(TAG, "Connection method call failed");
    }

    @Override
    public void onInfoWindowClick(Marker marker) {

    }

    @Override
    public void onMapLongClick(LatLng latLng) {

    }

    private class RouteBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String local = intent.getExtras().getString("RESULT_CODE");
            assert local != null;
            if(local.equals("LOCAL")){
                //get all data from database
                startToPresentLocations = User.getInstance().getPoints();
                if(startToPresentLocations.size() > 0){
                    //prepare map drawing.
                    List<LatLng> locationPoints = getPoints(startToPresentLocations);
                    refreshMap(mMap);
                    markDynamicLocationOnMap(mMap, locationPoints);
                    drawRouteOnMap(mMap, locationPoints);
                }
            }
        }
    }

    private List<LatLng> getPoints(List<LatLng> mLocations){
        List<LatLng> points = new ArrayList<LatLng>();
        for(LatLng mLocation : mLocations){
            points.add(new LatLng(mLocation.latitude, mLocation.longitude));
        }
        return points;
    }

    private void drawRouteOnMap(GoogleMap map, List<LatLng> positions){
        PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
        options.addAll(positions);
        Polyline polyline = map.addPolyline(options);
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(positions.get(0).latitude, positions.get(0).longitude))
                .zoom(17)
                .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void refreshMap(GoogleMap mapInstance){
        mapInstance.clear();
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
    protected void onResume() {
        super.onResume();
        if(routeReceiver == null){
            routeReceiver = new RouteBroadCastReceiver();
        }
        IntentFilter filter = new IntentFilter(TrackingService.ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(routeReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(routeReceiver);
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    private void showJourneysList(){
        Intent intent = new Intent(this, JourneysActivity.class);
        startActivity(intent);
    }

}

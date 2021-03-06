package com.marius.pathmap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.marius.pathmap.model.User;
import com.marius.pathmap.service.TrackingService;
import com.marius.pathmap.utils.PathMapSharedPreferences;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMarkerClickListener, SensorEventListener {

    private static final String TAG = MapsActivity.class.getSimpleName();

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
    private List<LatLng> mLocationPoints;
    private boolean isAutoDetectEnabled;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private float[] mGravity;
    private double mAccel;
    private double mAccelCurrent;
    private double mAccelLast;

    private boolean sensorRegistered = false;

    private int hitCount = 0;
    private double hitSum = 0;
    private double hitResult = 0;

    private final int sampleSize = 50; //  higher is more precise but slow measure.
    private final double threshold = 0.2; //  higher is more spike movement


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
        startToPresentLocations = User.getInstance().getPoints();
        mLocationPoints = new Vector<>();
        mLocationRequest = createLocationRequest();
        routeReceiver = new RouteBroadCastReceiver();
        isAutoDetectEnabled = false;

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
                    Toast.makeText(getApplicationContext(), getString(R.string.route_tracking_on), Toast.LENGTH_SHORT).show();
                    User.getInstance().addStartTime(Calendar.getInstance().getTime());
                } else if (!isChecked) {
                    Toast.makeText(getApplicationContext(), getString(R.string.route_tracking_off), Toast.LENGTH_SHORT).show();
                    User.getInstance().addEndTime(Calendar.getInstance().getTime());
                    try {
                        secureDataRouteTracking(User.getInstance().getAddressStart(), User.getInstance().getAddressEnd(),
                                User.getInstance().getStartTime(), User.getInstance().getEndTime(),
                                User.getInstance().getPoints());
                    } catch (Exception ex){
                        Log.d(TAG, ex.getLocalizedMessage());
                    }
                    PathMapSharedPreferences.getInstance(getApplicationContext()).removeTrackingState();
                    PathMapSharedPreferences.getInstance(getApplicationContext()).saveTrackingState(false);
                    initCamera(mMap, new LatLng(latitudeValue,longitudeValue));
                    User.getInstance().clearPoints();
                }
            }
        });

        sensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        assert sensorManager != null;
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorRegistered = true;
    }

    // for detecting the user's movements and turning the tracking service ON or OFF if is not moving
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mGravity = event.values.clone();
            // Shake detection
            double x = mGravity[0];
            double y = mGravity[1];
            double z = mGravity[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = Math.sqrt(x * x + y * y + z * z);
            double delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;

            if (hitCount <= sampleSize) {
                hitCount++;
                hitSum += Math.abs(mAccel);
            } else {
                hitResult = hitSum / sampleSize;

                Log.d(TAG, String.valueOf(hitResult));

                if (hitResult > threshold) {
                    if(trackOnOff.isEnabled() && isAutoDetectEnabled) {
                        trackOnOff.setChecked(true);
                        Log.d(TAG, "Walking");
                    }
                } else {
                    if(trackOnOff.isEnabled() && isAutoDetectEnabled) {
                        trackOnOff.setChecked(false);
                        Log.d(TAG, "Stop Walking");
                    }
                }

                hitCount = 0;
                hitSum = 0;
                hitResult = 0;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

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
                disableTrackingSwitch();
                showJourneysList();
                break;
            case R.id.enableAutoDetect:
                enableOrDisableAutoDetect(true);
                break;
            case R.id.disableAutoDetect:
                enableOrDisableAutoDetect(false);
                break;
            default:
                break;
        }
        return true;
    }

    private void enableOrDisableAutoDetect(boolean isEnableOrDisable){
        this.isAutoDetectEnabled = isEnableOrDisable;
        if(isEnableOrDisable){
            Toast.makeText(getApplicationContext(),getString(R.string.auto_detect_enabled), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(),getString(R.string.auto_detect_disabled), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_LOCATION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else
                    Toast.makeText(this, getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        initListeners();
    }

    private void initCamera(GoogleMap mapObject, LatLng location) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_LOCATION_REQUEST_CODE);
        }
        CameraPosition position = CameraPosition.builder()
                .target(location)
                .zoom(16f)
                .bearing(0.0f)
                .tilt(0.0f)
                .build();

        mapObject.animateCamera(CameraUpdateFactory
                .newCameraPosition(position), null);

        //mapObject.setTrafficEnabled(true); // this method could enable the traffic on the map, but will slow the app
        mapObject.setMyLocationEnabled(true);
        mapObject.getUiSettings().setZoomControlsEnabled( true );
    }

    // getting address from the current position
    private String getAddressFromLatLng(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this);
        List<Address> addresses = null;
        String errorMessage = "";
        String address = "";
        try {
            addresses = geocoder
                    .getFromLocation(latLng.latitude, latLng.longitude, 1);
        } catch (IOException ioException) {
            errorMessage = getString(R.string.service_not_available);
            Log.e(TAG, errorMessage, ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            errorMessage = getString(R.string.invalid_lat_long_used);
            Log.e(TAG, errorMessage + ". " + "Latitude = " + latLng.latitude +
                    ", Longitude = " +
                    latLng.longitude, illegalArgumentException);
        }

        if (addresses == null || addresses.size()  == 0) {
            if (errorMessage.isEmpty()) {
                errorMessage = getString(R.string.no_address_found);
                Log.e(TAG, errorMessage);
            }
            return "Current location";
        } else {
            Address addressItem = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<String>();

            // Fetch the address lines using getAddressLine
            for(int i = 0; i <= addressItem.getMaxAddressLineIndex(); i++) {
                addressFragments.add(addressItem.getAddressLine(i));
            }
            Log.i(TAG, getString(R.string.address_found));

            for(String addressPoint : addressFragments){
                address = addressPoint;
            }
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
                                initCamera(mMap, new LatLng(latitudeValue,longitudeValue));
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
       // Here we can add a marker that displays some location information
    }


    // when receiving coordinates from the tracking service
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
                    List<LatLng> locationPoints = startToPresentLocations;
                    refreshMap(mMap);
                    drawRouteOnMap(mMap, locationPoints);
                }
            }
        }
    }

    // draws in real time a route on the map when the tracking service is running
    private void drawRouteOnMap(GoogleMap map, List<LatLng> positions){
        PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
        options.addAll(positions);
        Polyline polyline = map.addPolyline(options);
        User.getInstance().addAddressStart(getAddressFromLatLng(positions.get(0)));
        for(LatLng location : positions) {
            User.getInstance().addAddressEnd(getAddressFromLatLng(location));
            initCamera(map, location);
        }
    }

    // securing the data from the user into a file, and for retaining the data after uninstalling the app I could implement a cache method to save the data in a temporary file
    private void secureDataRouteTracking(String addressStart, String addressEnd,
                                         Vector<Date> startTime, Vector<Date> endTime,
                                         Vector<LatLng> routeTracks){
        final String FILE_NAME = "user_route_tracking.txt";
        StringBuilder buildDataString = new StringBuilder();
        for(int i = 0; i < routeTracks.size(); i++){
            buildDataString.append(addressStart);
            buildDataString.append(" ");
            buildDataString.append(startTime.get(i).toString());
            buildDataString.append(" - ");
            buildDataString.append(addressEnd);
            buildDataString.append(" ");
            buildDataString.append(endTime.get(i).toString());
            buildDataString.append(": { ");
            for(int k = 0; k < routeTracks.size(); k++) {
                buildDataString.append(routeTracks.get(k).toString());
                buildDataString.append(" ; ");
            }
            buildDataString.append(" }");
            buildDataString.append(System.getProperty("line.separator"));
        }
        String fileContents = buildDataString.toString();

        FileOutputStream fos = null;
        try {
            fos = openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            fos.write(fileContents.getBytes());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
                Log.d(TAG, "DATA IS SECURED");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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

    private void enableTrackingSwitch(){
        trackOnOff.setEnabled(true);
        Toast.makeText(getApplicationContext(), getString(R.string.enable_tracking), Toast.LENGTH_SHORT).show();
    }

    private void disableTrackingSwitch(){
        trackOnOff.setChecked(false);
        trackOnOff.setEnabled(false);
        Toast.makeText(getApplicationContext(), getString(R.string.disable_tracking), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableTrackingSwitch();
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

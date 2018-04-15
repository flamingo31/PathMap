package com.marius.pathmap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.PersistableBundle;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.marius.pathmap.model.User;

import java.util.ArrayList;
import java.util.Calendar;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = MapsActivity.class.getSimpleName();

    private static final String SAVING_STATE_POINTS = "savingStatePoints";

    public static final float LOCATION_UPDATE_MIN_DISTANCE = 0.10F;
    public static final int LOCATION_UPDATE_MIN_TIME = 100;

    private GoogleMap mMap;
    private LocationManager locationManager;
    private Switch trackOnOff;
    private boolean isTracking = false;
    private static final int LOCATION_REQUEST_CODE = 101;

    private LocationRequest locationRequest;
    private static final long INTERVAL = 100;
    private static final long FASTEST_INTERVAL = 100;
    private static final float SMALLEST_DISPLACEMENT = 0.10F;

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                Log.d(TAG,String.format("%f, %f", location.getLatitude(), location.getLongitude()));
                User.getInstance().addPoint(new LatLng(location.getLatitude(), location.getLongitude()));
                if(!isTracking) {
                    drawMarker(location);
                }
            } else {
                Log.d(TAG,"Location is null");
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    @SuppressLint("RestrictedApi")
    protected void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setSmallestDisplacement(SMALLEST_DISPLACEMENT); //added
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        createLocationRequest();

        trackOnOff = (Switch) findViewById(R.id.trackOnOff);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        trackOnOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked){
                    isTracking = true;
                    if (User.getInstance() != null) {
                        User.getInstance().addStartTime(Calendar.getInstance().getTime());
                        drawLine(User.getInstance().getPoints());
                    } else {
                        Toast.makeText(getApplicationContext(), "Tracking is not working.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    isTracking = false;
                    if(User.getInstance() != null){
                        User.getInstance().addEndTime(Calendar.getInstance().getTime());
                    }
                    getCurrentLocation();
                }
            }
        });

        if(!isTracking){
           getCurrentLocation();
        }
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
        if(User.getInstance() != null){
            outState.putParcelableArrayList(SAVING_STATE_POINTS, User.getInstance().getPoints());
        }
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(User.getInstance().getPoints() == null){
            User.getInstance().setPoints(savedInstanceState.getParcelableArrayList(SAVING_STATE_POINTS));
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getCurrentLocation();
                } else
                    Toast.makeText(this, "Location Permission Denied", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void getCurrentLocation() {
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        Location location = null;
        if (!(isGPSEnabled || isNetworkEnabled))
            Toast.makeText(getApplicationContext(), R.string.error_location_provider, Toast.LENGTH_LONG).show();
        else {
            if (isNetworkEnabled) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
                    return;
                }
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        LOCATION_UPDATE_MIN_TIME, LOCATION_UPDATE_MIN_DISTANCE, locationListener);
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (isGPSEnabled) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
                    return;
                }
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        LOCATION_UPDATE_MIN_TIME, LOCATION_UPDATE_MIN_DISTANCE, locationListener);
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
        }
        if (location != null) {
            Log.d(TAG,String.format("getCurrentLocation(%f, %f)", location.getLatitude(),
                    location.getLongitude()));
            drawMarker(location);
        }
    }

    private void drawMarker(Location location) {
        if (mMap != null) {
            mMap.clear();
            LatLng gps = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker))
                    .position(gps)
                    .title("Current Position"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(gps, 16));
        }
    }

    private void drawLine(ArrayList<LatLng> points){
        if(mMap != null) {
            mMap.clear();  //clears all Markers and Polylines

            if(points.size() == 0){
                return;
            }
            LatLng current = null;
            PolylineOptions options = new PolylineOptions().width(10).color(Color.BLACK).geodesic(true);
            for (int i = 0; i < points.size(); i++) {
                LatLng point = points.get(i);
                current = point;
                options.add(point);
            }

            mMap.addPolyline(options);
            mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker))
                    .position(current)
                    .title("Current Position"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(points.get(0), 16));
        }
    }

    private void showJourneysList(){
        Intent intent = new Intent(this, JourneysActivity.class);
        startActivity(intent);
    }

}

package com.marius.pathmap.model;

import android.arch.lifecycle.ViewModel;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class User extends ViewModel {

    private double latitude;
    private double longitude;
    private ArrayList<LatLng> points;

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public ArrayList<LatLng> getPoints() {
        return points;
    }

    public void setPoints(ArrayList<LatLng> points) {
        this.points = points;
    }
}

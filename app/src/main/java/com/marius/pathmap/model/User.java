package com.marius.pathmap.model;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

public class User {

    private static User instance;
    private ArrayList<LatLng> points;
    private ArrayList<Date> startTime;
    private ArrayList<Date> endTime;
    private HashMap<Integer, ArrayList<LatLng>> routeTracks;
    private int keyItem;
    private ArrayList<String> addressStart;
    private ArrayList<String> addressEnd;



    private User() {
        points = new ArrayList<>();
        startTime = new ArrayList<>();
        endTime = new ArrayList<>();
        routeTracks = new HashMap<>();
        addressStart = new ArrayList<>();
        addressEnd = new ArrayList<>();
        keyItem = 0;
    }

    public static synchronized User getInstance() {
        if (instance == null) {
            instance = new User();
        }
        return instance;
    }

    public void addPoints(Collection<LatLng> points) {
        for (LatLng point : points) {
            addPoint(point);
        }
    }


    public boolean isPointsEmpty() {
        return points.size() == 0;
    }

    public boolean isStartTimeEmpty() {
        return startTime.size() == 0;
    }

    public boolean isEndTimeEmpty() {
        return endTime.size() == 0;
    }

    public void clearPoints() {
        points.clear();
    }

    public void clearStartTimes() {
        startTime.clear();
    }

    public void clearEndTimes() {
        endTime.clear();
    }

    public void clearAddressStart(){ addressStart.clear(); }

    public void clearAddressEnd(){ addressEnd.clear(); }

    public void addPoint(LatLng point) {
        points.add(point);
    }

    public void addStartTime(Date start) {
        startTime.add(start);
    }

    public void addEndTime(Date end) {
        endTime.add(end);
    }

    public void addAddressStart(String address){
        this.addressStart.add(address);
    }

    public void addAddressEnd(String address){
        this.addressEnd.add(address);
    }

    public ArrayList<LatLng> getPoints() {
        return points;
    }

    public void setPoints(ArrayList<LatLng> points) {
        this.points = points;
    }

    public ArrayList<Date> getStartTime() {
        return startTime;
    }

    public ArrayList<Date> getEndTime() {
        return endTime;
    }

    public ArrayList<String> getAddressStart() {
        return addressStart;
    }

    public void setAddressStart(ArrayList<String> addressStart) {
        this.addressStart = addressStart;
    }

    public ArrayList<String> getAddressEnd() {
        return addressEnd;
    }

    public void setAddressEnd(ArrayList<String> addressEnd) {
        this.addressEnd = addressEnd;
    }

    public void saveRouteTracks(ArrayList<LatLng> points){
        if(points.size() == 0){
            return;
        }
        routeTracks.put(keyItem, points);
        ++keyItem;
    }

    public HashMap<Integer, ArrayList<LatLng>> getRouteTracks() {
        return routeTracks;
    }

}

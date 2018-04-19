package com.marius.pathmap.model;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
/*
 I have choose to make a singleton model because I could manage much better the data for the user.
* */
public class User {

    private static User instance;
    private ArrayList<LatLng> points;
    private ArrayList<LatLng> pointsForRecordOff;
    private ArrayList<Date> startTime;
    private ArrayList<Date> endTime;
    private HashMap<Integer, ArrayList<LatLng>> routeTracks;
    private ArrayList<String> addressStart;
    private ArrayList<String> addressEnd;
    private int keyItem;



    private User() {
        points = new ArrayList<>();
        pointsForRecordOff = new ArrayList<>();
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

    public boolean isPointsForRecordOffEmpty() {
        return pointsForRecordOff.size() == 0;
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

    public void clearPointsForRecordOff() {
        pointsForRecordOff.clear();
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

    public void addPointForRecordOff(LatLng point) {
        pointsForRecordOff.add(point);
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

    public ArrayList<LatLng> getPointsForRecordOff() {
        return pointsForRecordOff;
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

    public void saveRouteTracks(Integer keyItem, LatLng point){
        points.add(point);
        if(points.size() == 0){
            return;
        }
        routeTracks.put(keyItem, points);
    }

    public HashMap<Integer, ArrayList<LatLng>> getRouteTracks() {
        return routeTracks;
    }

    public void incrementKeyItem(){
        ++keyItem;
    }

    public int getKeyItem(){
        return keyItem;
    }


}

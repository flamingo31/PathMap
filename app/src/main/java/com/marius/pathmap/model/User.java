package com.marius.pathmap.model;

import com.google.android.gms.maps.model.LatLng;

import java.util.Collection;
import java.util.Date;
import java.util.Vector;

/*
 I have choose to make a singleton model because I could manage much better the data for the user.
* */
public class User {

    private static User instance;
    private Vector<LatLng> points;
    private Vector<LatLng> pointsForRecordOff;
    private Vector<Date> startTime;
    private Vector<Date> endTime;
    private String addressStart;
    private String addressEnd;



    private User() {
        points = new Vector<>();
        pointsForRecordOff = new Vector<>();
        startTime = new Vector<>();
        endTime = new Vector<>();
        addressStart = "";
        addressEnd = "";
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
        this.addressStart = address;
    }

    public void addAddressEnd(String address){
        this.addressEnd= address;
    }

    public Vector<LatLng> getPoints() {
        return points;
    }

    public Vector<LatLng> getPointsForRecordOff() {
        return pointsForRecordOff;
    }

    public void setPoints(Vector<LatLng> points) {
        this.points = points;
    }

    public Vector<Date> getStartTime() {
        return startTime;
    }

    public Vector<Date> getEndTime() {
        return endTime;
    }

    public String getAddressStart() {
        return addressStart;
    }

    public String getAddressEnd() {
        return addressEnd;
    }

}

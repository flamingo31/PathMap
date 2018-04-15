package com.marius.pathmap.model;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class User {

    private static User instance;
    private ArrayList<LatLng> points;
    private ArrayList<Date> startTime;
    private ArrayList<Date> endTime;


    private User() {
        points = new ArrayList<>();
        startTime = new ArrayList<>();
        endTime = new ArrayList<>();
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


    public boolean isEmpty() {
        return points.size() == 0;
    }

    public void clear() {
        points.clear();
    }

    public void addPoint(LatLng point) {
        points.add(point);
    }

    public LatLng getPointByIndex(int id) {
        return points.get(id);
    }

    public void addStartTime(Date start) {
        startTime.add(start);
    }

    public Date getStartTimetByIndex(int id) {
        return startTime.get(id);
    }

    public void addEndTime(Date end) {
        endTime.add(end);
    }

    public Date getEndTimetByIndex(int id) {
        return endTime.get(id);
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
}

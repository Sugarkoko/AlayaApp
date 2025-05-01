package com.example.alayaapp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects; // Added for equals/hashCode

public class ItineraryItem {
    private long id;
    private Calendar time;
    private String activity;
    private String rating;
    private String bestTimeToVisit;
    private double latitude;
    private double longitude;

    public ItineraryItem(long id, Calendar time, String activity, String rating,
                         String bestTimeToVisit, double latitude, double longitude) {
        this.id = id;
        this.time = time;
        this.activity = activity;
        this.rating = rating;
        this.bestTimeToVisit = bestTimeToVisit;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public ItineraryItem() {}

    public long getId() { return id; }
    public Calendar getTime() { return time; }
    public String getActivity() { return activity; }
    public String getRating() { return rating; }
    public String getBestTimeToVisit() { return bestTimeToVisit; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    public void setId(long id) { this.id = id; }
    public void setTime(Calendar time) { this.time = time; }
    public void setActivity(String activity) { this.activity = activity; }
    public void setRating(String rating) { this.rating = rating; }
    public void setBestTimeToVisit(String bestTimeToVisit) { this.bestTimeToVisit = bestTimeToVisit; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getFormattedTime() {
        if (time == null) return "N/A";
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return sdf.format(time.getTime());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItineraryItem that = (ItineraryItem) o;
        return id == that.id &&
                Double.compare(that.latitude, latitude) == 0 &&
                Double.compare(that.longitude, longitude) == 0 &&
                Objects.equals(time, that.time) &&
                Objects.equals(activity, that.activity) &&
                Objects.equals(rating, that.rating) &&
                Objects.equals(bestTimeToVisit, that.bestTimeToVisit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, time, activity, rating, bestTimeToVisit, latitude, longitude);
    }
}
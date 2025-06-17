package com.example.alayaapp;

import com.google.firebase.firestore.GeoPoint;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ItineraryItem {
    private long id;
    // NEW: Replaced single time with a start and end time window
    private transient Calendar startTime;
    private transient Calendar endTime;
    private long startTimeMillis;
    private long endTimeMillis;

    private String activity;
    private String rating;
    private String imageUrl;
    private transient GeoPoint coordinates;
    private double latitude;
    private double longitude;
    private String placeDocumentId;

    // Default constructor for deserialization
    public ItineraryItem() {}

    // NEW: Updated constructor for time windows
    public ItineraryItem(long id, Calendar startTime, Calendar endTime, String activity, String rating, String imageUrl, GeoPoint coordinates, String placeDocumentId) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.activity = activity;
        this.rating = rating;
        this.imageUrl = imageUrl;
        this.coordinates = coordinates;
        this.placeDocumentId = placeDocumentId;

        // For serialization
        if (startTime != null) this.startTimeMillis = startTime.getTimeInMillis();
        if (endTime != null) this.endTimeMillis = endTime.getTimeInMillis();
        if (coordinates != null) {
            this.latitude = coordinates.getLatitude();
            this.longitude = coordinates.getLongitude();
        }
    }

    // --- Getters and Setters ---
    public long getId() { return id; }

    public Calendar getStartTime() {
        if (startTime == null && startTimeMillis > 0) {
            startTime = Calendar.getInstance();
            startTime.setTimeInMillis(startTimeMillis);
        }
        return startTime;
    }

    public Calendar getEndTime() {
        if (endTime == null && endTimeMillis > 0) {
            endTime = Calendar.getInstance();
            endTime.setTimeInMillis(endTimeMillis);
        }
        return endTime;
    }

    public String getActivity() { return activity; }
    public String getRating() { return rating; }
    public String getImageUrl() { return imageUrl; }
    public String getPlaceDocumentId() { return placeDocumentId; }

    public GeoPoint getCoordinates() {
        if (coordinates == null && (latitude != 0.0 || longitude != 0.0)) {
            coordinates = new GeoPoint(latitude, longitude);
        }
        return coordinates;
    }

    // For backward compatibility, returns the start time.
    public Calendar getTime() {
        return getStartTime();
    }

    // NEW: Updated method to format the time window
    public String getFormattedTime() {
        if (getStartTime() == null || getEndTime() == null) {
            return "Time not set";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String start = sdf.format(getStartTime().getTime());
        String end = sdf.format(getEndTime().getTime());

        // Avoids showing "9:00 AM - 9:00 AM" for very short/punctual events
        if (start.equals(end)) {
            return start;
        }
        return start + " - " + end;
    }
}
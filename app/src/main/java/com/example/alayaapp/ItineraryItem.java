package com.example.alayaapp;

import com.google.firebase.firestore.GeoPoint;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ItineraryItem {
    private long id;
    private transient Calendar time; // Marked as transient to be ignored by Gson
    private long timeMillis; // Gson will serialize this field
    private String activity;
    private String rating;
    private String imageUrl;
    private transient GeoPoint coordinates; // Marked as transient
    private double latitude; // Gson will serialize this
    private double longitude; // Gson will serialize this
    private String placeDocumentId;

    public ItineraryItem(long id, Calendar time, String activity, String rating, String imageUrl, GeoPoint coordinates, String placeDocumentId) {
        this.id = id;
        this.time = time;
        this.activity = activity;
        this.rating = rating;
        this.imageUrl = imageUrl;
        this.coordinates = coordinates;
        this.timeMillis = time.getTimeInMillis();
        if (coordinates != null) {
            this.latitude = coordinates.getLatitude();
            this.longitude = coordinates.getLongitude();
        }
        this.placeDocumentId = placeDocumentId;
    }

    public long getId() { return id; }

    public Calendar getTime() {
        // Re-create Calendar object on demand after deserialization
        if (time == null && timeMillis > 0) {
            time = Calendar.getInstance();
            time.setTimeInMillis(timeMillis);
        }
        return time;
    }

    public String getActivity() { return activity; }
    public String getRating() { return rating; }
    public String getImageUrl() { return imageUrl; }

    public GeoPoint getCoordinates() {
        // Re-create GeoPoint object on demand after deserialization
        if (coordinates == null && (latitude != 0.0 || longitude != 0.0)) {
            coordinates = new GeoPoint(latitude, longitude);
        }
        return coordinates;
    }

    public String getPlaceDocumentId() { return placeDocumentId; }

    public void setTime(Calendar time) {
        this.time = time;
        this.timeMillis = time.getTimeInMillis();
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a");
        return sdf.format(getTime().getTime());
    }
}
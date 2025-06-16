// app/src/main/java/com/example/alayaapp/ItineraryItem.java
package com.example.alayaapp;

import com.google.firebase.firestore.GeoPoint;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ItineraryItem {
    private long id;
    private Calendar time;
    private String activity;
    private String rating;
    private String imageUrl;
    private GeoPoint coordinates;
    private String placeDocumentId;

    public ItineraryItem(long id, Calendar time, String activity, String rating, String imageUrl, GeoPoint coordinates, String placeDocumentId) {
        this.id = id;
        this.time = time;
        this.activity = activity;
        this.rating = rating;
        this.imageUrl = imageUrl;
        this.coordinates = coordinates;
        this.placeDocumentId = placeDocumentId;
    }

    public long getId() {
        return id;
    }

    public Calendar getTime() {
        return time;
    }

    public String getActivity() {
        return activity;
    }

    public String getRating() {
        return rating;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public GeoPoint getCoordinates() {
        return coordinates;
    }

    public String getPlaceDocumentId() {
        return placeDocumentId;
    }

    public void setTime(Calendar time) {
        this.time = time;
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a");
        return sdf.format(time.getTime());
    }
}
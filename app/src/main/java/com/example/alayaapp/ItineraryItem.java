// app/src/main/java/com/example/alayaapp/ItineraryItem.java
package com.example.alayaapp;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ItineraryItem {
    private long id;
    private Calendar time;
    private String activity;
    private String rating;
    private String imageUrl; // +++ ADDED +++

    // +++ MODIFIED CONSTRUCTOR +++
    public ItineraryItem(long id, Calendar time, String activity, String rating, String imageUrl) {
        this.id = id;
        this.time = time;
        this.activity = activity;
        this.rating = rating;
        this.imageUrl = imageUrl; // +++ ADDED +++
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

    // +++ ADDED GETTER +++
    public String getImageUrl() {
        return imageUrl;
    }

    public void setTime(Calendar time) {
        this.time = time;
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a");
        return sdf.format(time.getTime());
    }
}
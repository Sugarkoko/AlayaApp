package com.example.alayaapp;

import java.text.SimpleDateFormat;
import java.util.Calendar; // Or use java.time for API 26+

public class ItineraryItem {
    private long id; // For stable IDs in adapter
    private Calendar time; // Store time properly
    private String activity;
    private String rating;




    public ItineraryItem(long id, Calendar time, String activity, String rating) {
        this.id = id;
        this.time = time;
        this.activity = activity;
        this.rating = rating;
    }


    public long getId() { return id; }
    public Calendar getTime() { return time; }
    public String getActivity() { return activity; }
    public String getRating() { return rating; }


    public void setTime(Calendar time) { this.time = time; }


    public String getFormattedTime() {
        // Example using SimpleDateFormat (add import java.text.SimpleDateFormat)
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a"); // e.g., 9:00 AM
        return sdf.format(time.getTime());
    }
}
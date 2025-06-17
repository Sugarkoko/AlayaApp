package com.example.alayaapp;

import com.google.firebase.firestore.GeoPoint;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ItineraryItem {
    private long id;
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

    public ItineraryItem() {}

    public ItineraryItem(long id, Calendar startTime, Calendar endTime, String activity, String rating, String imageUrl, GeoPoint coordinates, String placeDocumentId) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.activity = activity;
        this.rating = rating;
        this.imageUrl = imageUrl;
        this.coordinates = coordinates;
        this.placeDocumentId = placeDocumentId;
        if (startTime != null) this.startTimeMillis = startTime.getTimeInMillis();
        if (endTime != null) this.endTimeMillis = endTime.getTimeInMillis();
        if (coordinates != null) {
            this.latitude = coordinates.getLatitude();
            this.longitude = coordinates.getLongitude();
        }
    }

    // --- Getters and Setters (No changes here) ---
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
    public Calendar getTime() { return getStartTime(); }

    // --- NEW: Helper method to round a Calendar object to the nearest 5 minutes ---
    private Calendar roundToNearestFiveMinutes(Calendar originalCal) {
        if (originalCal == null) {
            return null;
        }

        // Create a copy to avoid modifying the original, precise time
        Calendar roundedCal = (Calendar) originalCal.clone();

        int minutes = roundedCal.get(Calendar.MINUTE);
        // Use Math.round to find the nearest multiple of 5
        int roundedMinutes = (int) (Math.round((double) minutes / 5.0) * 5);

        // Handle the case where rounding up goes to 60 (e.g., 58 minutes -> 60)
        int hoursToAdd = roundedMinutes / 60;
        int finalMinutes = roundedMinutes % 60;

        roundedCal.set(Calendar.MINUTE, finalMinutes);
        // For cleanliness, reset seconds and milliseconds
        roundedCal.set(Calendar.SECOND, 0);
        roundedCal.set(Calendar.MILLISECOND, 0);

        if (hoursToAdd > 0) {
            roundedCal.add(Calendar.HOUR_OF_DAY, hoursToAdd);
        }

        return roundedCal;
    }


    // --- MODIFIED: This method now uses the rounding helper for display ---
    public String getFormattedTime() {
        if (getStartTime() == null || getEndTime() == null) {
            return "Time not set";
        }

        // Get the new, rounded times for display purposes ONLY
        Calendar roundedStartTime = roundToNearestFiveMinutes(getStartTime());
        Calendar roundedEndTime = roundToNearestFiveMinutes(getEndTime());

        // This check is important in case rounding fails for some reason
        if (roundedStartTime == null || roundedEndTime == null) {
            return "Time not set";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String start = sdf.format(roundedStartTime.getTime());
        String end = sdf.format(roundedEndTime.getTime());

        if (start.equals(end)) {
            return start;
        }
        return start + " - " + end;
    }
}
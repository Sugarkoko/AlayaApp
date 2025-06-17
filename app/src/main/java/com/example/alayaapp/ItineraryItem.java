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
    private String category;
    private transient GeoPoint coordinates;
    private double latitude;
    private double longitude;
    private String placeDocumentId;
    private boolean isTimeLocked = false; // NEW: To mark this item as fixed

    public ItineraryItem() {}

    public ItineraryItem(long id, Calendar startTime, Calendar endTime, String activity, String rating, String imageUrl, GeoPoint coordinates, String placeDocumentId, String category) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.activity = activity;
        this.rating = rating;
        this.imageUrl = imageUrl;
        this.coordinates = coordinates;
        this.placeDocumentId = placeDocumentId;
        this.category = category;
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
    public String getCategory() { return category; }
    public GeoPoint getCoordinates() {
        if (coordinates == null && (latitude != 0.0 || longitude != 0.0)) {
            coordinates = new GeoPoint(latitude, longitude);
        }
        return coordinates;
    }
    public Calendar getTime() { return getStartTime(); }

    // NEW: Getter and Setter for isTimeLocked
    public boolean isTimeLocked() {
        return isTimeLocked;
    }
    public void setTimeLocked(boolean timeLocked) {
        isTimeLocked = timeLocked;
    }

    private Calendar roundToNearestFiveMinutes(Calendar originalCal) {
        if (originalCal == null) { return null; }
        Calendar roundedCal = (Calendar) originalCal.clone();
        int minutes = roundedCal.get(Calendar.MINUTE);
        int roundedMinutes = (int) (Math.round((double) minutes / 5.0) * 5);
        int hoursToAdd = roundedMinutes / 60;
        int finalMinutes = roundedMinutes % 60;
        roundedCal.set(Calendar.MINUTE, finalMinutes);
        roundedCal.set(Calendar.SECOND, 0);
        roundedCal.set(Calendar.MILLISECOND, 0);
        if (hoursToAdd > 0) {
            roundedCal.add(Calendar.HOUR_OF_DAY, hoursToAdd);
        }
        return roundedCal;
    }

    public String getFormattedTime() {
        if (getStartTime() == null || getEndTime() == null) {
            return "Time not set";
        }
        Calendar roundedStartTime = roundToNearestFiveMinutes(getStartTime());
        Calendar roundedEndTime = roundToNearestFiveMinutes(getEndTime());
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
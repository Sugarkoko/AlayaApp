package com.example.alayaapp;

import java.util.List;
import java.util.Objects;

/**
 * A data class representing the complete state of a generated itinerary.
 * This object is serialized to JSON and stored in SharedPreferences.
 */
public class ItineraryState {

    // --- Generation Parameters ---
    private final double startLat;
    private final double startLon;
    private final long startTimeMillis;
    private final long endTimeMillis;

    // --- Generated Data ---
    private final List<ItineraryItem> itineraryItems;
    private final List<Place> topRatedPlaces;
    private final String headerMessage;
    private final String locationName;

    public ItineraryState(double startLat, double startLon, long startTimeMillis, long endTimeMillis,
                          List<ItineraryItem> itineraryItems, List<Place> topRatedPlaces,
                          String headerMessage, String locationName) {
        this.startLat = startLat;
        this.startLon = startLon;
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
        this.itineraryItems = itineraryItems;
        this.topRatedPlaces = topRatedPlaces;
        this.headerMessage = headerMessage;
        this.locationName = locationName;
    }

    public List<ItineraryItem> getItineraryItems() {
        return itineraryItems;
    }

    public List<Place> getTopRatedPlaces() {
        return topRatedPlaces;
    }

    public String getHeaderMessage() {
        return headerMessage;
    }

    public String getLocationName() {
        return locationName;
    }

    /**
     * Checks if the parameters used to generate this itinerary state
     * are still valid compared to the current user preferences.
     * A small tolerance is used for location comparison.
     */
    public boolean isStillValid(double currentLat, double currentLon, long currentStartMillis, long currentEndMillis) {
        final double LAT_LON_TOLERANCE = 0.0001; // Approx 11 meters
        boolean isLocationSame = Math.abs(this.startLat - currentLat) < LAT_LON_TOLERANCE &&
                Math.abs(this.startLon - currentLon) < LAT_LON_TOLERANCE;
        boolean areTimesSame = this.startTimeMillis == currentStartMillis &&
                this.endTimeMillis == currentEndMillis;

        return isLocationSame && areTimesSame;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItineraryState that = (ItineraryState) o;
        return Double.compare(that.startLat, startLat) == 0 &&
                Double.compare(that.startLon, startLon) == 0 &&
                startTimeMillis == that.startTimeMillis &&
                endTimeMillis == that.endTimeMillis;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startLat, startLon, startTimeMillis, endTimeMillis);
    }
}
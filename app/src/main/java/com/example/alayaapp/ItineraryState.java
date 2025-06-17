// In: app/src/main/java/com/example/alayaapp/ItineraryState.java
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
    private final List<String> categoryPreferences;
    private final int lockedItemIndex; // NEW: To remember which item is locked (-1 if none)

    // --- Generated Data ---
    private final List<ItineraryItem> itineraryItems;
    private final List<Place> topRatedPlaces;
    private final String headerMessage;
    private final String locationName;
    private final boolean isUserModified;

    public ItineraryState(double startLat, double startLon, long startTimeMillis, long endTimeMillis, List<String> categoryPreferences, int lockedItemIndex, List<ItineraryItem> itineraryItems, List<Place> topRatedPlaces, String headerMessage, String locationName, boolean isUserModified) {
        this.startLat = startLat;
        this.startLon = startLon;
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
        this.categoryPreferences = categoryPreferences;
        this.lockedItemIndex = lockedItemIndex; // NEW
        this.itineraryItems = itineraryItems;
        this.topRatedPlaces = topRatedPlaces;
        this.headerMessage = headerMessage;
        this.locationName = locationName;
        this.isUserModified = isUserModified;
    }

    public double getStartLat() { return startLat; }
    public double getStartLon() { return startLon; }
    public List<ItineraryItem> getItineraryItems() { return itineraryItems; }
    public List<Place> getTopRatedPlaces() { return topRatedPlaces; }
    public String getHeaderMessage() { return headerMessage; }
    public String getLocationName() { return locationName; }
    public boolean isUserModified() { return isUserModified; }
    public long getStartTimeMillis() { return startTimeMillis; }
    public long getEndTimeMillis() { return endTimeMillis; }
    public List<String> getCategoryPreferences() { return categoryPreferences; }
    public int getLockedItemIndex() { return lockedItemIndex; } // NEW

    /**
     * Checks if the parameters used to generate this itinerary state
     * are still valid compared to the current user preferences.
     * A small tolerance is used for location comparison.
     * MODIFIED: If the state was user-modified, it's always considered valid
     * to prevent overwriting the user's changes.
     */
    public boolean isStillValid(double currentLat, double currentLon, long currentStartMillis, long currentEndMillis, List<String> currentPreferences) {
        // If user made changes, don't auto-regenerate. Keep their version.
        if (this.isUserModified) {
            return true;
        }
        final double LAT_LON_TOLERANCE = 0.0001; // Approx 11 meters
        boolean isLocationSame = Math.abs(this.startLat - currentLat) < LAT_LON_TOLERANCE && Math.abs(this.startLon - currentLon) < LAT_LON_TOLERANCE;
        boolean areTimesSame = this.startTimeMillis == currentStartMillis && this.endTimeMillis == currentEndMillis;
        boolean arePrefsSame = Objects.equals(this.categoryPreferences, currentPreferences);
        return isLocationSame && areTimesSame && arePrefsSame;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItineraryState that = (ItineraryState) o;
        return Double.compare(that.startLat, startLat) == 0 &&
                Double.compare(that.startLon, startLon) == 0 &&
                startTimeMillis == that.startTimeMillis &&
                endTimeMillis == that.endTimeMillis &&
                isUserModified == that.isUserModified &&
                lockedItemIndex == that.lockedItemIndex && // NEW
                Objects.equals(categoryPreferences, that.categoryPreferences);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startLat, startLon, startTimeMillis, endTimeMillis, categoryPreferences, isUserModified, lockedItemIndex); // NEW
    }
}
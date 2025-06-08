package com.example.alayaapp.logic;

import android.util.Log;

import com.example.alayaapp.ItineraryItem;
import com.example.alayaapp.Place;
import com.google.firebase.firestore.GeoPoint;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Generates a daily itinerary based on a greedy nearest-neighbor algorithm with constraints.
 */
public class ItineraryGenerator {

    private static final String TAG = "ItineraryGenerator";
    private static final String ITINERARY_START_TIME = "09:00";
    private static final String ITINERARY_END_TIME = "18:00";
    private static final double AVERAGE_SPEED_KMH = 15.0; // Average travel speed in Baguio

    /**
     * Main method to generate the itinerary.
     * @param userStartLocation The starting point for the day.
     * @param allPlaces A list of all potential places to visit.
     * @param tripDate The specific date for the itinerary.
     * @return A list of ItineraryItem objects representing the generated schedule.
     */
    public List<ItineraryItem> generate(GeoPoint userStartLocation, List<Place> allPlaces, Calendar tripDate) {
        List<ItineraryItem> generatedItinerary = new ArrayList<>();
        List<Place> availablePlaces = new ArrayList<>(allPlaces);

        // 1. Initialization
        Calendar currentTime = (Calendar) tripDate.clone();
        setCalendarTime(currentTime, ITINERARY_START_TIME);

        Calendar endTime = (Calendar) tripDate.clone();
        setCalendarTime(endTime, ITINERARY_END_TIME);

        GeoPoint currentLocation = userStartLocation;
        String lastCategory = null;

        // 2. Pre-filtering: Remove places that are closed for the entire day.
        String dayOfWeek = tripDate.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.US).toLowerCase();
        availablePlaces.removeIf(place -> {
            if (place.getOpeningHours() == null || !place.getOpeningHours().containsKey(dayOfWeek)) {
                Log.d(TAG, "Pre-filtering: Removing " + place.getName() + " as it's closed on " + dayOfWeek);
                return true;
            }
            return false;
        });
        Log.d(TAG, "Pre-filtered places. Remaining: " + availablePlaces.size());

        // 3. Generation Loop
        long itineraryItemIdCounter = 1;
        while (currentTime.before(endTime) && !availablePlaces.isEmpty()) {
            Place bestNextPlace = null;
            double minDistance = Double.MAX_VALUE;

            // A. Find Best Next Place (avoiding same category if possible)
            for (Place place : availablePlaces) {
                if (place.getCategory() != null && place.getCategory().equals(lastCategory)) {
                    continue; // Skip if it's the same category as the last one
                }
                double distance = calculateDistance(currentLocation, place.getCoordinates());
                if (distance < minDistance) {
                    minDistance = distance;
                    bestNextPlace = place;
                }
            }

            // Fallback: If all remaining places are of the same category, just find the closest one.
            if (bestNextPlace == null) {
                minDistance = Double.MAX_VALUE;
                for (Place place : availablePlaces) {
                    double distance = calculateDistance(currentLocation, place.getCoordinates());
                    if (distance < minDistance) {
                        minDistance = distance;
                        bestNextPlace = place;
                    }
                }
            }

            if (bestNextPlace == null) {
                Log.d(TAG, "No suitable places found. Ending generation.");
                break; // No places left to visit
            }

            // B. Check Feasibility of the Closest Place
            int travelTimeMinutes = (int) ((minDistance / AVERAGE_SPEED_KMH) * 60);
            Calendar arrivalTime = (Calendar) currentTime.clone();
            arrivalTime.add(Calendar.MINUTE, travelTimeMinutes);

            if (!isPlaceOpenAtTime(bestNextPlace, arrivalTime, dayOfWeek)) {
                Log.d(TAG, bestNextPlace.getName() + " is not open upon estimated arrival. Removing and continuing.");
                availablePlaces.remove(bestNextPlace);
                continue;
            }

            // C. Add to Itinerary and Update State
            Log.d(TAG, "Adding " + bestNextPlace.getName() + " to itinerary.");
            Calendar itemTime = (Calendar) arrivalTime.clone();
            String rating = String.format(Locale.getDefault(), "%.1f", bestNextPlace.getRating());
            generatedItinerary.add(new ItineraryItem(itineraryItemIdCounter++, itemTime, bestNextPlace.getName(), rating));

            // Update algorithm state for the next iteration
            currentTime.setTime(arrivalTime.getTime());
            currentTime.add(Calendar.MINUTE, bestNextPlace.getAverageVisitDuration());

            currentLocation = bestNextPlace.getCoordinates();
            lastCategory = bestNextPlace.getCategory();
            availablePlaces.remove(bestNextPlace);
        }

        return generatedItinerary;
    }

    /**
     * Helper to set a Calendar object's time from a "HH:mm" string.
     */
    private void setCalendarTime(Calendar calendar, String timeStr) {
        String[] parts = timeStr.split(":");
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
        calendar.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    /**
     * Checks if a place is open at a given arrival time.
     */
    private boolean isPlaceOpenAtTime(Place place, Calendar arrivalTime, String dayOfWeek) {
        Map<String, String> hours = place.getOpeningHours().get(dayOfWeek);
        if (hours == null || hours.get("open") == null || hours.get("close") == null) {
            return false; // No opening hours data for this day
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
            Calendar openTimeCal = Calendar.getInstance();
            openTimeCal.setTime(sdf.parse(hours.get("open")));

            Calendar closeTimeCal = Calendar.getInstance();
            closeTimeCal.setTime(sdf.parse(hours.get("close")));

            // Compare only the time part (hour and minute)
            int arrivalTimeInMinutes = arrivalTime.get(Calendar.HOUR_OF_DAY) * 60 + arrivalTime.get(Calendar.MINUTE);
            int openTimeInMinutes = openTimeCal.get(Calendar.HOUR_OF_DAY) * 60 + openTimeCal.get(Calendar.MINUTE);
            int closeTimeInMinutes = closeTimeCal.get(Calendar.HOUR_OF_DAY) * 60 + closeTimeCal.get(Calendar.MINUTE);

            return arrivalTimeInMinutes >= openTimeInMinutes && arrivalTimeInMinutes < closeTimeInMinutes;

        } catch (ParseException e) {
            Log.e(TAG, "Failed to parse opening hours for " + place.getName(), e);
            return false;
        }
    }

    /**
     * Calculates the distance between two GeoPoints using the Haversine formula.
     * @return Distance in kilometers.
     */
    private double calculateDistance(GeoPoint start, GeoPoint end) {
        if (start == null || end == null) return Double.MAX_VALUE;
        final int R = 6371; // Radius of the earth in km

        double latDistance = Math.toRadians(end.getLatitude() - start.getLatitude());
        double lonDistance = Math.toRadians(end.getLongitude() - start.getLongitude());

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(start.getLatitude())) * Math.cos(Math.toRadians(end.getLatitude()))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}
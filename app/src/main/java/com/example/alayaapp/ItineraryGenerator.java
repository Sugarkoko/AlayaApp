package com.example.alayaapp;

import android.util.Log;
import com.google.firebase.firestore.GeoPoint;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Generates a daily itinerary based on user-defined constraints or a greedy nearest-neighbor algorithm.
 */
public class ItineraryGenerator {
    private static final String TAG = "ItineraryGenerator";
    private static final double AVERAGE_SPEED_KMH = 15.0; // Average travel speed in Baguio
    private static final double CATEGORY_REPETITION_PENALTY_KM = 50.0; // Penalty for repeating categories in default mode
    private static final int DEFAULT_VISIT_DURATION_MINUTES = 90; // Fallback duration

    /**
     * Overloaded main method to handle customized itinerary generation.
     * @param userStartLocation The starting point for the day.
     * @param allPlaces A list of all potential places to visit.
     * @param tripStartCalendar The desired start time for the itinerary.
     * @param tripEndCalendar The desired end time for the itinerary.
     * @param categoryPreferences A list of strings representing the user's category choice for each stop.
     * @return A list of ItineraryItem objects representing the generated schedule.
     */
    public List<ItineraryItem> generate(GeoPoint userStartLocation, List<Place> allPlaces, Calendar tripStartCalendar, Calendar tripEndCalendar, List<String> categoryPreferences) {
        // If preferences are empty or null, fall back to the default generation logic.
        if (categoryPreferences == null || categoryPreferences.isEmpty()) {
            Log.d(TAG, "No category preferences provided. Using default itinerary generation.");
            return generate(userStartLocation, allPlaces, tripStartCalendar, tripEndCalendar);
        }

        Log.d(TAG, "Starting customized itinerary generation with preferences: " + categoryPreferences);

        List<ItineraryItem> generatedItinerary = new ArrayList<>();
        List<Place> availablePlaces = new ArrayList<>(allPlaces);

        // 1. Initialization
        Calendar currentTime = (Calendar) tripStartCalendar.clone();
        Calendar endTime = (Calendar) tripEndCalendar.clone();
        GeoPoint currentLocation = userStartLocation;
        String dayOfWeek = tripStartCalendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.US).toLowerCase();

        // 2. Pre-filter places that are closed for the entire day.
        availablePlaces.removeIf(place -> {
            if (place.getOpeningHours() == null || !place.getOpeningHours().containsKey(dayOfWeek)) {
                Log.d(TAG, "Pre-filtering: Removing " + place.getName() + " as it's closed on " + dayOfWeek);
                return true;
            }
            return false;
        });
        Log.d(TAG, "Pre-filtered places. Remaining: " + availablePlaces.size());

        // 3. Constraint-Driven Generation Loop
        long itineraryItemIdCounter = 1;
        for (String preferredCategory : categoryPreferences) {
            if (currentTime.after(endTime) || availablePlaces.isEmpty()) {
                Log.d(TAG, "Ending generation early: out of time or places.");
                break;
            }

            // A. Filter candidates for the current stop based on the preferred category
            List<Place> candidatePlaces = new ArrayList<>();
            if ("Any".equalsIgnoreCase(preferredCategory)) {
                candidatePlaces.addAll(availablePlaces);
            } else {
                for (Place p : availablePlaces) {
                    if (preferredCategory.equalsIgnoreCase(p.getCategory())) {
                        candidatePlaces.add(p);
                    }
                }
            }

            if (candidatePlaces.isEmpty()) {
                Log.w(TAG, "No available places found for category: " + preferredCategory + ". Skipping this stop.");
                continue; // Move to the next preference
            }

            // B. Find the best candidate from the filtered list
            Place bestNextPlace = null;
            double bestScore = Double.MAX_VALUE;
            for (Place candidatePlace : candidatePlaces) {
                double distance = calculateDistance(currentLocation, candidatePlace.getCoordinates());
                // In custom mode, the primary factor is just distance. No category penalty.
                if (distance < bestScore) {
                    bestScore = distance;
                    bestNextPlace = candidatePlace;
                }
            }

            if (bestNextPlace == null) {
                Log.w(TAG, "Could not determine a best next place for category: " + preferredCategory);
                continue;
            }

            // C. Check Feasibility of the Best Place
            double actualTravelDistance = calculateDistance(currentLocation, bestNextPlace.getCoordinates());
            int travelTimeMinutes = (int) ((actualTravelDistance / AVERAGE_SPEED_KMH) * 60);
            Calendar arrivalTime = (Calendar) currentTime.clone();
            arrivalTime.add(Calendar.MINUTE, travelTimeMinutes);

            int visitDuration = bestNextPlace.getAverageVisitDuration() > 0 ? bestNextPlace.getAverageVisitDuration() : DEFAULT_VISIT_DURATION_MINUTES;
            Calendar departureTime = (Calendar) arrivalTime.clone();
            departureTime.add(Calendar.MINUTE, visitDuration);

            if (departureTime.after(endTime)) {
                Log.d(TAG, "Not enough time to visit " + bestNextPlace.getName() + " for preference '" + preferredCategory + "'. Trying other candidates.");
                // This was the best option, but it doesn't fit. We should ideally try the next best.
                // For simplicity, we'll just skip this preference slot. A more advanced implementation
                // could re-run the loop for this preference with the failed place removed.
                continue;
            }

            if (!isPlaceOpenAtTime(bestNextPlace, arrivalTime, dayOfWeek)) {
                Log.d(TAG, bestNextPlace.getName() + " is not open upon estimated arrival for preference '" + preferredCategory + "'.");
                // Similar to the time issue, we skip this slot.
                continue;
            }

            // D. Add to Itinerary and Update State
            Log.d(TAG, "Adding " + bestNextPlace.getName() + " to itinerary for preference '" + preferredCategory + "'.");
            Calendar itemTime = (Calendar) arrivalTime.clone();
            String rating = String.format(Locale.getDefault(), "%.1f", bestNextPlace.getRating());
            generatedItinerary.add(new ItineraryItem(
                    itineraryItemIdCounter++,
                    itemTime,
                    bestNextPlace.getName(),
                    rating,
                    bestNextPlace.getImage_url(),
                    bestNextPlace.getCoordinates(),
                    bestNextPlace.getDocumentId()
            ));

            // Update state for the next iteration
            currentTime.setTime(arrivalTime.getTime());
            currentTime.add(Calendar.MINUTE, visitDuration);
            currentLocation = bestNextPlace.getCoordinates();
            availablePlaces.remove(bestNextPlace); // Remove from pool so it can't be picked again
        }

        return generatedItinerary;
    }


    /**
     * Default method to generate the itinerary using a greedy nearest-neighbor algorithm.
     * @param userStartLocation The starting point for the day.
     * @param allPlaces A list of all potential places to visit.
     * @param tripStartCalendar The desired start time for the itinerary.
     * @param tripEndCalendar The desired end time for the itinerary.
     * @return A list of ItineraryItem objects representing the generated schedule.
     */
    public List<ItineraryItem> generate(GeoPoint userStartLocation, List<Place> allPlaces, Calendar tripStartCalendar, Calendar tripEndCalendar) {
        List<ItineraryItem> generatedItinerary = new ArrayList<>();
        List<Place> availablePlaces = new ArrayList<>(allPlaces);

        // 1. Initialization
        Calendar currentTime = (Calendar) tripStartCalendar.clone();
        Calendar endTime = (Calendar) tripEndCalendar.clone();
        GeoPoint currentLocation = userStartLocation;
        String lastCategory = null;

        // 2. Pre-filtering: Remove places that are closed for the entire day.
        String dayOfWeek = tripStartCalendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.US).toLowerCase();
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
            double bestScore = Double.MAX_VALUE;

            // A. Find the best place to visit next
            for (Place candidatePlace : availablePlaces) {
                double distance = calculateDistance(currentLocation, candidatePlace.getCoordinates());
                double score = distance;
                // Add penalty for repeating the same category consecutively
                if (candidatePlace.getCategory() != null && candidatePlace.getCategory().equals(lastCategory)) {
                    score += CATEGORY_REPETITION_PENALTY_KM;
                }
                if (score < bestScore) {
                    bestScore = score;
                    bestNextPlace = candidatePlace;
                }
            }

            if (bestNextPlace == null) {
                Log.d(TAG, "No suitable places found. Ending generation.");
                break;
            }

            // B. Check Feasibility of the Best Place
            double actualTravelDistance = calculateDistance(currentLocation, bestNextPlace.getCoordinates());
            int travelTimeMinutes = (int) ((actualTravelDistance / AVERAGE_SPEED_KMH) * 60);
            Calendar arrivalTime = (Calendar) currentTime.clone();
            arrivalTime.add(Calendar.MINUTE, travelTimeMinutes);

            // Use dynamic visit duration from database, with a fallback
            int visitDuration = bestNextPlace.getAverageVisitDuration() > 0 ? bestNextPlace.getAverageVisitDuration() : DEFAULT_VISIT_DURATION_MINUTES;
            Calendar departureTime = (Calendar) arrivalTime.clone();
            departureTime.add(Calendar.MINUTE, visitDuration);

            if (departureTime.after(endTime)) {
                Log.d(TAG, "Not enough time to visit " + bestNextPlace.getName() + ". Removing and continuing.");
                availablePlaces.remove(bestNextPlace);
                continue;
            }

            if (!isPlaceOpenAtTime(bestNextPlace, arrivalTime, dayOfWeek)) {
                Log.d(TAG, bestNextPlace.getName() + " is not open upon estimated arrival. Removing and continuing.");
                availablePlaces.remove(bestNextPlace);
                continue;
            }

            // C. Add to Itinerary and Update State
            Log.d(TAG, "Adding " + bestNextPlace.getName() + " to itinerary. Visit duration: " + visitDuration + " mins.");
            Calendar itemTime = (Calendar) arrivalTime.clone();
            String rating = String.format(Locale.getDefault(), "%.1f", bestNextPlace.getRating());
            generatedItinerary.add(new ItineraryItem(
                    itineraryItemIdCounter++,
                    itemTime,
                    bestNextPlace.getName(),
                    rating,
                    bestNextPlace.getImage_url(),
                    bestNextPlace.getCoordinates(),
                    bestNextPlace.getDocumentId()
            ));

            // Update algorithm state for the next iteration
            currentTime.setTime(arrivalTime.getTime());
            currentTime.add(Calendar.MINUTE, visitDuration);
            currentLocation = bestNextPlace.getCoordinates();
            lastCategory = bestNextPlace.getCategory();
            availablePlaces.remove(bestNextPlace);
        }
        return generatedItinerary;
    }

    /**
     * Checks if a place is open at a given arrival time.
     */
    private boolean isPlaceOpenAtTime(Place place, Calendar arrivalTime, String dayOfWeek) {
        if (place.getOpeningHours() == null) return false;
        Map<String, String> hours = place.getOpeningHours().get(dayOfWeek);
        if (hours == null || hours.get("open") == null || hours.get("close") == null) {
            return false; // No opening hours data for this day
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
            Calendar openTimeCal = Calendar.getInstance();
            openTimeCal.setTime(Objects.requireNonNull(sdf.parse(hours.get("open"))));
            Calendar closeTimeCal = Calendar.getInstance();
            closeTimeCal.setTime(Objects.requireNonNull(sdf.parse(hours.get("close"))));

            // Handle 24-hour case (e.g., 00:00 to 23:59)
            if (hours.get("open").equals("00:00") && (hours.get("close").equals("23:59") || hours.get("close").equals("24:00"))) {
                return true;
            }

            // Compare only the time part (hour and minute)
            int arrivalTimeInMinutes = arrivalTime.get(Calendar.HOUR_OF_DAY) * 60 + arrivalTime.get(Calendar.MINUTE);
            int openTimeInMinutes = openTimeCal.get(Calendar.HOUR_OF_DAY) * 60 + openTimeCal.get(Calendar.MINUTE);
            int closeTimeInMinutes = closeTimeCal.get(Calendar.HOUR_OF_DAY) * 60 + closeTimeCal.get(Calendar.MINUTE);

            // Handle overnight case (e.g., 18:00 to 02:00)
            if (closeTimeInMinutes < openTimeInMinutes) {
                return arrivalTimeInMinutes >= openTimeInMinutes || arrivalTimeInMinutes < closeTimeInMinutes;
            } else {
                return arrivalTimeInMinutes >= openTimeInMinutes && arrivalTimeInMinutes < closeTimeInMinutes;
            }
        } catch (ParseException | NullPointerException e) {
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
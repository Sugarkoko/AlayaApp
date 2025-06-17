package com.example.alayaapp;

import android.util.Log;
import androidx.annotation.Nullable;
import com.google.firebase.firestore.GeoPoint;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Date;

public class ItineraryGenerator {
    private static final String TAG = "ItineraryGenerator";
    private static final double AVERAGE_SPEED_KMH = 15.0;
    private static final int DEFAULT_VISIT_DURATION_MINUTES = 90;
    private static final int MIN_VISIT_DURATION_MINUTES = 30;
    private static final double CATEGORY_REPETITION_PENALTY_KM = 50.0;

    public static class GenerationResult {
        public final List<ItineraryItem> itinerary;
        public final List<String> unmetPreferences;

        public GenerationResult(List<ItineraryItem> itinerary, List<String> unmetPreferences) {
            this.itinerary = itinerary;
            this.unmetPreferences = unmetPreferences;
        }
    }

    public GenerationResult generateWithTimeWindows(GeoPoint userStartLocation, List<Place> itineraryPlaces, Calendar tripStartCalendar, Calendar tripEndCalendar, List<String> categoryPreferences, @Nullable ItineraryItem lockedItem) {
        if (lockedItem != null) {
            Log.d(TAG, "Starting generation around a locked item: " + lockedItem.getActivity());
            return generateAroundLockedItem(userStartLocation, itineraryPlaces, tripStartCalendar, tripEndCalendar, lockedItem);
        } else {
            Log.d(TAG, "Starting dynamic window itinerary generation (no locked item).");
            return generateNewItinerary(userStartLocation, itineraryPlaces, tripStartCalendar, tripEndCalendar, categoryPreferences);
        }
    }

    private GenerationResult generateNewItinerary(GeoPoint userStartLocation, List<Place> allPlaces, Calendar tripStartCalendar, Calendar tripEndCalendar, List<String> categoryPreferences) {
        List<ItineraryItem> generatedItinerary = new ArrayList<>();
        List<Place> availablePlaces = new ArrayList<>(allPlaces);
        String dayOfWeek = tripStartCalendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.US).toLowerCase();
        List<String> unmetPreferences = new ArrayList<>();

        availablePlaces.removeIf(place -> !isPlaceOpenOnDay(place, dayOfWeek));
        if (availablePlaces.isEmpty()) {
            Log.w(TAG, "No places are open on " + dayOfWeek);
            return new GenerationResult(generatedItinerary, unmetPreferences);
        }

        List<Place> sequence = selectPlaceSequence(userStartLocation, availablePlaces, categoryPreferences, unmetPreferences);
        if (sequence.isEmpty()) {
            Log.w(TAG, "Could not determine a valid sequence of places.");
            return new GenerationResult(generatedItinerary, unmetPreferences);
        }

        Log.d(TAG, "Determined sequence: " + sequence.stream().map(Place::getName).collect(Collectors.joining(" -> ")));

        long totalMinVisitMinutes = 0;
        for (Place p : sequence) {
            totalMinVisitMinutes += p.getAverageVisitDuration() > 0 ? p.getAverageVisitDuration() : DEFAULT_VISIT_DURATION_MINUTES;
        }

        long totalTravelMinutes = 0;
        GeoPoint lastLocation = userStartLocation;
        for (Place p : sequence) {
            totalTravelMinutes += calculateTravelTime(lastLocation, p.getCoordinates());
            lastLocation = p.getCoordinates();
        }

        long minTotalTimeMinutes = totalMinVisitMinutes + totalTravelMinutes;
        long userTripDurationMinutes = (tripEndCalendar.getTimeInMillis() - tripStartCalendar.getTimeInMillis()) / (60 * 1000);
        long slackMinutes = userTripDurationMinutes - minTotalTimeMinutes;

        Log.d(TAG, "User Trip: " + userTripDurationMinutes + " mins. Min Required: " + minTotalTimeMinutes + " mins. Slack: " + slackMinutes + " mins.");
        if (slackMinutes < 0) {
            Log.w(TAG, "Not enough time for the planned itinerary. Required time exceeds user's trip window.");
            return new GenerationResult(generatedItinerary, unmetPreferences);
        }

        double perStopSlackMinutes = sequence.isEmpty() ? 0 : (double) slackMinutes / sequence.size();
        Calendar currentTime = (Calendar) tripStartCalendar.clone();
        GeoPoint currentGeoLocation = userStartLocation;
        long itineraryItemIdCounter = 1;

        for (Place place : sequence) {
            int travelTime = calculateTravelTime(currentGeoLocation, place.getCoordinates());
            Calendar arrivalTime = (Calendar) currentTime.clone();
            arrivalTime.add(Calendar.MINUTE, travelTime);

            Calendar placeOpenTime = getPlaceTime(place, dayOfWeek, "open", tripStartCalendar);
            Calendar placeCloseTime = getPlaceTime(place, dayOfWeek, "close", tripStartCalendar);

            Calendar windowStart = (Calendar) arrivalTime.clone();
            if (windowStart.before(placeOpenTime)) {
                windowStart.setTime(placeOpenTime.getTime());
            }

            int baseVisitDuration = place.getAverageVisitDuration() > 0 ? place.getAverageVisitDuration() : DEFAULT_VISIT_DURATION_MINUTES;
            int flexibleVisitDuration = (int) (baseVisitDuration + perStopSlackMinutes);
            Calendar windowEnd = (Calendar) windowStart.clone();
            windowEnd.add(Calendar.MINUTE, flexibleVisitDuration);

            if (windowEnd.after(placeCloseTime)) {
                windowEnd.setTime(placeCloseTime.getTime());
            }
            if (windowEnd.after(tripEndCalendar)) {
                windowEnd.setTime(tripEndCalendar.getTime());
            }

            long finalVisitMinutes = (windowEnd.getTimeInMillis() - windowStart.getTimeInMillis()) / (60 * 1000);
            if (finalVisitMinutes < MIN_VISIT_DURATION_MINUTES || windowStart.after(windowEnd) || windowStart.after(tripEndCalendar)) {
                Log.w(TAG, "Skipping " + place.getName() + " because the available time window is invalid or too short.");
                continue;
            }

            String rating = String.format(Locale.getDefault(), "%.1f", place.getRating());
            generatedItinerary.add(new ItineraryItem(
                    itineraryItemIdCounter++, windowStart, windowEnd, place.getName(), rating,
                    place.getImage_url(), place.getCoordinates(), place.getDocumentId(), place.getCategory()
            ));
            currentTime = (Calendar) windowEnd.clone();
            currentGeoLocation = place.getCoordinates();
        }
        return new GenerationResult(generatedItinerary, unmetPreferences);
    }

    private GenerationResult generateAroundLockedItem(GeoPoint userStartLocation, List<Place> originalSequence, Calendar tripStartCalendar, Calendar tripEndCalendar, ItineraryItem lockedItem) {
        List<ItineraryItem> finalItinerary = new ArrayList<>();
        finalItinerary.add(lockedItem); // The locked item is the anchor

        String dayOfWeek = tripStartCalendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.US).toLowerCase();

        int lockedPlaceIndex = -1;
        for (int i = 0; i < originalSequence.size(); i++) {
            if (originalSequence.get(i).getDocumentId().equals(lockedItem.getPlaceDocumentId())) {
                lockedPlaceIndex = i;
                break;
            }
        }

        if (lockedPlaceIndex == -1) {
            Log.e(TAG, "Could not find locked place in the provided sequence. Aborting recalculation.");
            return new GenerationResult(Collections.singletonList(lockedItem), Collections.emptyList());
        }

        // --- BACKWARD PASS ---
        Calendar latestDepartureTime = (Calendar) lockedItem.getStartTime().clone();
        GeoPoint nextStopLocation = lockedItem.getCoordinates();
        for (int i = lockedPlaceIndex - 1; i >= 0; i--) {
            Place currentPlace = originalSequence.get(i);
            int travelToNext = calculateTravelTime(currentPlace.getCoordinates(), nextStopLocation);
            Calendar mustDepartBy = (Calendar) latestDepartureTime.clone();
            mustDepartBy.add(Calendar.MINUTE, -travelToNext);

            Calendar placeCloseTime = getPlaceTime(currentPlace, dayOfWeek, "close", tripStartCalendar);
            if (mustDepartBy.after(placeCloseTime)) {
                Log.w(TAG, "Conflict (Backward): " + currentPlace.getName() + " would need to be left after it closes. Dropping stop.");
                continue;
            }

            int visitDuration = currentPlace.getAverageVisitDuration() > 0 ? currentPlace.getAverageVisitDuration() : DEFAULT_VISIT_DURATION_MINUTES;
            Calendar mustArriveBy = (Calendar) mustDepartBy.clone();
            mustArriveBy.add(Calendar.MINUTE, -visitDuration);

            Calendar placeOpenTime = getPlaceTime(currentPlace, dayOfWeek, "open", tripStartCalendar);
            if (mustArriveBy.before(placeOpenTime) || mustArriveBy.before(tripStartCalendar)) {
                Log.w(TAG, "Conflict (Backward): " + currentPlace.getName() + " cannot be visited in time. Dropping stop.");
                continue;
            }

            finalItinerary.add(new ItineraryItem(currentPlace.getId(), mustArriveBy, mustDepartBy, currentPlace.getName(), String.valueOf(currentPlace.getRating()), currentPlace.getImage_url(), currentPlace.getCoordinates(), currentPlace.getDocumentId(), currentPlace.getCategory()));
            latestDepartureTime = (Calendar) mustArriveBy.clone();
            nextStopLocation = currentPlace.getCoordinates();
        }

        // --- FORWARD PASS ---
        Calendar earliestStartTime = (Calendar) lockedItem.getEndTime().clone();
        GeoPoint prevStopLocation = lockedItem.getCoordinates();
        for (int i = lockedPlaceIndex + 1; i < originalSequence.size(); i++) {
            Place currentPlace = originalSequence.get(i);
            int travelFromPrev = calculateTravelTime(prevStopLocation, currentPlace.getCoordinates());
            Calendar canArriveAt = (Calendar) earliestStartTime.clone();
            canArriveAt.add(Calendar.MINUTE, travelFromPrev);

            Calendar placeOpenTime = getPlaceTime(currentPlace, dayOfWeek, "open", tripStartCalendar);
            if (canArriveAt.before(placeOpenTime)) {
                canArriveAt = (Calendar) placeOpenTime.clone();
            }

            int visitDuration = currentPlace.getAverageVisitDuration() > 0 ? currentPlace.getAverageVisitDuration() : DEFAULT_VISIT_DURATION_MINUTES;
            Calendar departureTime = (Calendar) canArriveAt.clone();
            departureTime.add(Calendar.MINUTE, visitDuration);

            Calendar placeCloseTime = getPlaceTime(currentPlace, dayOfWeek, "close", tripStartCalendar);
            if (departureTime.after(placeCloseTime) || departureTime.after(tripEndCalendar)) {
                Log.w(TAG, "Conflict (Forward): " + currentPlace.getName() + " cannot be visited in time. Dropping stop.");
                continue;
            }

            finalItinerary.add(new ItineraryItem(currentPlace.getId(), canArriveAt, departureTime, currentPlace.getName(), String.valueOf(currentPlace.getRating()), currentPlace.getImage_url(), currentPlace.getCoordinates(), currentPlace.getDocumentId(), currentPlace.getCategory()));
            earliestStartTime = (Calendar) departureTime.clone();
            prevStopLocation = currentPlace.getCoordinates();
        }

        finalItinerary.sort(Comparator.comparing(ItineraryItem::getStartTime));
        return new GenerationResult(finalItinerary, Collections.emptyList());
    }


    private List<Place> selectPlaceSequence(GeoPoint startLocation, List<Place> availablePlaces, List<String> categoryPreferences, List<String> unmetPreferences) {
        if (categoryPreferences == null || categoryPreferences.isEmpty()) {
            return selectGreedySequence(startLocation, availablePlaces);
        } else {
            if (availablePlaces.size() == categoryPreferences.size()) {
                Log.d(TAG, "Forced sequence detected. Using the provided place list directly.");
                return availablePlaces;
            }
            return selectCustomSequence(startLocation, availablePlaces, categoryPreferences, unmetPreferences);
        }
    }

    private List<Place> selectGreedySequence(GeoPoint startLocation, List<Place> availablePlaces) {
        List<Place> sequence = new ArrayList<>();
        List<Place> pool = new ArrayList<>(availablePlaces);
        GeoPoint currentLocation = startLocation;
        String lastCategory = null;
        int maxStops = 5;

        while (sequence.size() < maxStops && !pool.isEmpty()) {
            Place bestNextPlace = null;
            double bestScore = Double.MAX_VALUE;
            for (Place candidate : pool) {
                double distance = calculateDistance(currentLocation, candidate.getCoordinates());
                double score = distance;
                if (candidate.getCategory() != null && candidate.getCategory().equals(lastCategory)) {
                    score += CATEGORY_REPETITION_PENALTY_KM;
                }
                if (score < bestScore) {
                    bestScore = score;
                    bestNextPlace = candidate;
                }
            }
            if (bestNextPlace != null) {
                sequence.add(bestNextPlace);
                pool.remove(bestNextPlace);
                currentLocation = bestNextPlace.getCoordinates();
                lastCategory = bestNextPlace.getCategory();
            } else {
                break;
            }
        }
        return sequence;
    }

    private List<Place> selectCustomSequence(GeoPoint startLocation, List<Place> availablePlaces, List<String> categoryPreferences, List<String> unmetPreferences) {
        List<Place> sequence = new ArrayList<>();
        List<Place> pool = new ArrayList<>(availablePlaces);
        GeoPoint currentLocation = startLocation;

        for (int i = 0; i < categoryPreferences.size(); i++) {
            String preferredCategory = categoryPreferences.get(i);
            if (pool.isEmpty()) {
                unmetPreferences.add("'" + preferredCategory + "' for Stop " + (i + 1));
                continue;
            }
            List<Place> candidatesForStop = new ArrayList<>();
            if ("Any".equalsIgnoreCase(preferredCategory)) {
                candidatesForStop.addAll(pool);
            } else {
                for (Place p : pool) {
                    if (preferredCategory.equalsIgnoreCase(p.getCategory())) {
                        candidatesForStop.add(p);
                    }
                }
            }
            if (candidatesForStop.isEmpty()) {
                Log.w(TAG, "No available places in pool for category: " + preferredCategory);
                unmetPreferences.add("'" + preferredCategory + "' for Stop " + (i + 1));
                continue;
            }
            Place bestNextPlace = null;
            double bestScore = Double.MAX_VALUE;
            for (Place candidate : candidatesForStop) {
                double distance = calculateDistance(currentLocation, candidate.getCoordinates());
                if (distance < bestScore) {
                    bestScore = distance;
                    bestNextPlace = candidate;
                }
            }
            if (bestNextPlace != null) {
                sequence.add(bestNextPlace);
                pool.remove(bestNextPlace);
                currentLocation = bestNextPlace.getCoordinates();
            }
        }
        return sequence;
    }

    private boolean isPlaceOpenOnDay(Place place, String dayOfWeek) {
        if (place.getOpeningHours() == null) return false;
        return place.getOpeningHours().containsKey(dayOfWeek);
    }

    private int calculateTravelTime(GeoPoint start, GeoPoint end) {
        if (start == null || end == null) return 0;
        double distance = calculateDistance(start, end);
        return (int) ((distance / AVERAGE_SPEED_KMH) * 60);
    }

    private Calendar getPlaceTime(Place place, String dayOfWeek, String type, Calendar referenceDate) {
        Calendar timeCal = (Calendar) referenceDate.clone();
        try {
            Map<String, String> hours = place.getOpeningHours().get(dayOfWeek);
            if (hours == null || hours.get(type) == null) {
                throw new ParseException("Hours not available for " + type, 0);
            }
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
            Date parsedTime = sdf.parse(hours.get(type));
            Calendar parsedCal = Calendar.getInstance();
            parsedCal.setTime(Objects.requireNonNull(parsedTime));
            timeCal.set(Calendar.HOUR_OF_DAY, parsedCal.get(Calendar.HOUR_OF_DAY));
            timeCal.set(Calendar.MINUTE, parsedCal.get(Calendar.MINUTE));
            timeCal.set(Calendar.SECOND, 0);
            timeCal.set(Calendar.MILLISECOND, 0);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse '" + type + "' time for " + place.getName() + ". Using default.", e);
            if ("open".equals(type)) {
                timeCal.set(Calendar.HOUR_OF_DAY, 0);
                timeCal.set(Calendar.MINUTE, 0);
            } else {
                timeCal.set(Calendar.HOUR_OF_DAY, 23);
                timeCal.set(Calendar.MINUTE, 59);
            }
        }
        return timeCal;
    }

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
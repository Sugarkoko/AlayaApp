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
    private static final int DEFAULT_VISIT_DURATION_MINUTES = 60;
    private static final int MIN_VISIT_DURATION_MINUTES = 30;
    private static final int MAX_STOPS = 5;
    private static final int MIN_STOPS = 1;


    private static final double RATING_WEIGHT = 2.0;


    private static final double CATEGORY_REPETITION_PENALTY_KM = 50.0;


    private Calendar roundToNearestFiveMinutes(Calendar originalCal) {
        if (originalCal == null) {
            return null;
        }
        Calendar roundedCal = (Calendar) originalCal.clone();
        int minutes = roundedCal.get(Calendar.MINUTE);
        int roundedMinutes = (int) (Math.round((double) minutes / 5.0) * 5);
        if (roundedMinutes == 60) {
            roundedCal.add(Calendar.HOUR_OF_DAY, 1);
            roundedCal.set(Calendar.MINUTE, 0);
        } else {
            roundedCal.set(Calendar.MINUTE, roundedMinutes);
        }
        roundedCal.set(Calendar.SECOND, 0);
        roundedCal.set(Calendar.MILLISECOND, 0);
        return roundedCal;
    }

    public static class GenerationResult {
        public final List<ItineraryItem> itinerary;
        public final List<String> unmetPreferences;

        public GenerationResult(List<ItineraryItem> itinerary, List<String> unmetPreferences) {
            this.itinerary = itinerary;
            this.unmetPreferences = unmetPreferences;
        }
    }

    public GenerationResult generateWithTimeWindows(GeoPoint userStartLocation, List<Place> places, Calendar tripStartCalendar, Calendar tripEndCalendar, List<String> categoryPreferences, @Nullable ItineraryItem lockedItem) {
        if (lockedItem != null) {
            Log.d(TAG, "Recalculating itinerary around a locked item: " + lockedItem.getActivity());
            return generateAroundLockedItem(userStartLocation, places, tripStartCalendar, tripEndCalendar, lockedItem);
        } else {
            Log.d(TAG, "Generating a fresh itinerary (no locked item).");
            return generateNewItinerary(userStartLocation, places, tripStartCalendar, tripEndCalendar, categoryPreferences);
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
            return new GenerationResult(new ArrayList<>(), unmetPreferences);
        }

        final boolean isCustomPlan = categoryPreferences != null && !categoryPreferences.isEmpty();
        int startNumStops = isCustomPlan ? categoryPreferences.size() : MAX_STOPS;

        for (int numStops = startNumStops; numStops >= MIN_STOPS; numStops--) {
            Log.i(TAG, "Attempting to generate an itinerary with " + numStops + " stops.");
            List<String> prefsForThisAttempt = isCustomPlan ? categoryPreferences.subList(0, numStops) : Collections.emptyList();
            List<Place> sequence = selectPlaceSequence(userStartLocation, availablePlaces, prefsForThisAttempt, unmetPreferences, numStops);

            if (sequence.size() < numStops) {
                Log.w(TAG, "Could not find " + numStops + " suitable places. Trying with " + (numStops - 1) + ".");
                continue;
            }

            Log.d(TAG, "Determined sequence for " + numStops + " stops: " + sequence.stream().map(Place::getName).collect(Collectors.joining(" -> ")));

            long totalMinVisitMinutes = 0;
            for (Place p : sequence) {
                int visitDuration = p.getAverageVisitDuration();
                totalMinVisitMinutes += (visitDuration > 0) ? visitDuration : DEFAULT_VISIT_DURATION_MINUTES;
            }

            long totalTravelMinutes = 0;
            GeoPoint lastLocation = userStartLocation;
            for (Place p : sequence) {
                totalTravelMinutes += calculateTravelTime(lastLocation, p.getCoordinates());
                lastLocation = p.getCoordinates();
            }

            long minTotalTimeMinutes = totalMinVisitMinutes + totalTravelMinutes;
            long userTripDurationMinutes = (tripEndCalendar.getTimeInMillis() - tripStartCalendar.getTimeInMillis()) / (60 * 1000);

            if (minTotalTimeMinutes <= userTripDurationMinutes) {
                Log.i(TAG, "SUCCESS: A " + numStops + "-stop plan fits! Required: " + minTotalTimeMinutes + " mins, Available: " + userTripDurationMinutes + " mins.");
                long slackMinutes = userTripDurationMinutes - minTotalTimeMinutes;
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

                    Calendar earliestPossibleStart = (Calendar) arrivalTime.clone();
                    if (earliestPossibleStart.before(placeOpenTime)) {
                        earliestPossibleStart.setTime(placeOpenTime.getTime());
                    }

                    Calendar effectiveStartTime = roundToNearestFiveMinutes(earliestPossibleStart);
                    if (effectiveStartTime.before(earliestPossibleStart)) {
                        effectiveStartTime.add(Calendar.MINUTE, 5);
                    }

                    int baseVisitDuration = place.getAverageVisitDuration();
                    if (baseVisitDuration <= 0) {
                        baseVisitDuration = DEFAULT_VISIT_DURATION_MINUTES;
                    }
                    int flexibleVisitDuration = (int) (baseVisitDuration + perStopSlackMinutes);

                    Calendar tentativeEndTime = (Calendar) effectiveStartTime.clone();
                    tentativeEndTime.add(Calendar.MINUTE, flexibleVisitDuration);

                    Calendar effectiveEndTime = roundToNearestFiveMinutes(tentativeEndTime);

                    if (effectiveEndTime.after(placeCloseTime)) {
                        effectiveEndTime.setTime(placeCloseTime.getTime());
                    }
                    if (effectiveEndTime.after(tripEndCalendar)) {
                        effectiveEndTime.setTime(tripEndCalendar.getTime());
                    }

                    long finalVisitMinutes = (effectiveEndTime.getTimeInMillis() - effectiveStartTime.getTimeInMillis()) / (60 * 1000);
                    if (finalVisitMinutes < MIN_VISIT_DURATION_MINUTES || effectiveStartTime.after(effectiveEndTime) || effectiveStartTime.after(tripEndCalendar)) {
                        Log.w(TAG, "Skipping " + place.getName() + " because the available time window after rounding is invalid or too short.");
                        continue;
                    }

                    String rating = String.format(Locale.getDefault(), "%.1f", place.getRating());
                    generatedItinerary.add(new ItineraryItem(
                            itineraryItemIdCounter++,
                            effectiveStartTime,
                            effectiveEndTime,
                            place.getName(),
                            rating,
                            place.getImage_url(),
                            place.getCoordinates(),
                            place.getDocumentId(),
                            place.getCategory()
                    ));

                    currentTime = (Calendar) effectiveEndTime.clone();
                    currentGeoLocation = place.getCoordinates();
                }
                return new GenerationResult(generatedItinerary, unmetPreferences);
            } else {
                Log.w(TAG, "A " + numStops + "-stop plan is too long. Required: " + minTotalTimeMinutes + " mins, Available: " + userTripDurationMinutes + " mins. Trying fewer stops.");
            }
        }

        Log.w(TAG, "Even an itinerary with " + MIN_STOPS + " stops could not fit in the given time.");
        return new GenerationResult(new ArrayList<>(), unmetPreferences);
    }


    private double calculateScore(Place place, GeoPoint referencePoint, @Nullable String lastCategory) {
        double distance = calculateDistance(referencePoint, place.getCoordinates());
        // A bonus for higher-rated places (subtracts from the score).
        double ratingBonus = (place.getRating() > 0) ? place.getRating() * RATING_WEIGHT : 0;
        // A penalty for repeating the same category (adds to the score).
        double categoryPenalty = 0;
        if (lastCategory != null && lastCategory.equalsIgnoreCase(place.getCategory())) {
            categoryPenalty = CATEGORY_REPETITION_PENALTY_KM;
        }
        // Final score: lower is better.
        return distance - ratingBonus + categoryPenalty;
    }


    private List<Place> selectPlaceSequence(GeoPoint startLocation, List<Place> allAvailablePlaces, List<String> categoryPreferences, List<String> unmetPreferences, int numStops) {
        List<Place> finalSequence = new ArrayList<>();
        List<Place> remainingPlacesPool = new ArrayList<>(allAvailablePlaces);
        String lastCategory = null;

        for (int i = 0; i < numStops; i++) {
            List<Place> candidatesForThisStop = new ArrayList<>();
            String preferredCategory = (categoryPreferences != null && !categoryPreferences.isEmpty()) ? categoryPreferences.get(i) : "Any";

            if ("Any".equalsIgnoreCase(preferredCategory)) {
                candidatesForThisStop.addAll(remainingPlacesPool);
            } else {
                for (Place p : remainingPlacesPool) {
                    if (preferredCategory.equalsIgnoreCase(p.getCategory())) {
                        candidatesForThisStop.add(p);
                    }
                }
            }

            if (candidatesForThisStop.isEmpty()) {
                Log.w(TAG, "No available places in pool for stop " + (i + 1) + " with category: " + preferredCategory);
                if (!"Any".equalsIgnoreCase(preferredCategory)) {
                    unmetPreferences.add(preferredCategory + " for Stop " + (i + 1));
                }
                break;
            }

            Place bestNextPlace = null;
            double bestScore = Double.MAX_VALUE;

            for (Place candidate : candidatesForThisStop) {

                double score = calculateScore(candidate, startLocation, lastCategory);

                if (score < bestScore) {
                    bestScore = score;
                    bestNextPlace = candidate;
                }
            }

            if (bestNextPlace != null) {
                finalSequence.add(bestNextPlace);
                remainingPlacesPool.remove(bestNextPlace);
                lastCategory = bestNextPlace.getCategory();
            } else {
                break;
            }
        }

        if (finalSequence.size() > 1) {
            return optimizeSequenceWith2Opt(finalSequence, startLocation);
        } else {
            return finalSequence;
        }
    }

    private List<Place> optimizeSequenceWith2Opt(List<Place> route, GeoPoint startLocation) {
        if (route.size() <= 2) {
            return route;
        }
        List<Place> bestRoute = new ArrayList<>(route);
        boolean improvementFound = true;
        while (improvementFound) {
            improvementFound = false;
            double bestDistance = calculateTotalDistance(bestRoute, startLocation);
            for (int i = 0; i < bestRoute.size() - 1; i++) {
                for (int k = i + 1; k < bestRoute.size(); k++) {
                    List<Place> newRoute = perform2OptSwap(bestRoute, i, k);
                    double newDistance = calculateTotalDistance(newRoute, startLocation);
                    if (newDistance < bestDistance) {
                        bestRoute = newRoute;
                        bestDistance = newDistance;
                        improvementFound = true;
                    }
                }
            }
        }
        Log.d(TAG, "2-Opt optimization complete.");
        return bestRoute;
    }


    private GenerationResult generateAroundLockedItem(GeoPoint userStartLocation, List<Place> originalSequence, Calendar tripStartCalendar, Calendar tripEndCalendar, ItineraryItem lockedItem) {
        List<ItineraryItem> finalItinerary = new ArrayList<>();
        finalItinerary.add(lockedItem);
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

            int visitDuration = currentPlace.getAverageVisitDuration();
            if (visitDuration <= 0) {
                visitDuration = DEFAULT_VISIT_DURATION_MINUTES;
            }
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

            int visitDuration = currentPlace.getAverageVisitDuration();
            if (visitDuration <= 0) {
                visitDuration = DEFAULT_VISIT_DURATION_MINUTES;
            }
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

    private List<Place> perform2OptSwap(List<Place> route, int i, int k) {
        List<Place> newRoute = new ArrayList<>();
        for (int c = 0; c <= i; c++) {
            newRoute.add(route.get(c));
        }
        for (int c = k; c > i; c--) {
            newRoute.add(route.get(c));
        }
        for (int c = k + 1; c < route.size(); c++) {
            newRoute.add(route.get(c));
        }
        return newRoute;
    }

    private double calculateTotalDistance(List<Place> route, GeoPoint startLocation) {
        if (route == null || route.isEmpty()) {
            return 0.0;
        }
        double totalDistance = 0.0;
        GeoPoint currentLocation = startLocation;
        for (Place place : route) {
            totalDistance += calculateDistance(currentLocation, place.getCoordinates());
            currentLocation = place.getCoordinates();
        }
        return totalDistance;
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
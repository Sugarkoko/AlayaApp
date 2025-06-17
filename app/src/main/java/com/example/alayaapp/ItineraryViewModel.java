package com.example.alayaapp;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import android.os.Looper;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ItineraryViewModel extends AndroidViewModel {
    private static final String TAG = "ItineraryViewModel";
    private static final String PREFS_NAME = "AlayaAppPrefs";
    private static final String KEY_ACTIVE_ITINERARY_STATE = "active_itinerary_state";

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<Boolean> _isItinerarySaved = new MutableLiveData<>(false);
    public final LiveData<Boolean> isItinerarySaved = _isItinerarySaved;

    private final MutableLiveData<String> _currentLocationName = new MutableLiveData<>("Tap to get current location");
    public final LiveData<String> currentLocationName = _currentLocationName;

    private final MutableLiveData<String> _currentLocationStatus = new MutableLiveData<>("Set your location to begin");
    public final LiveData<String> currentLocationStatus = _currentLocationStatus;

    private final MutableLiveData<ItineraryState> _itineraryState = new MutableLiveData<>();
    public final LiveData<ItineraryState> itineraryState = _itineraryState;

    private final SharedPreferences sharedPreferences;
    private final Gson gson;
    private final FirebaseFirestore db;
    private final ItineraryGenerator itineraryGenerator;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public ItineraryViewModel(@NonNull Application application) {
        super(application);
        sharedPreferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new GsonBuilder().create();
        db = FirebaseFirestore.getInstance();
        itineraryGenerator = new ItineraryGenerator();
    }

    public void initializeState() {
        executorService.execute(() -> {
            ItineraryState savedState = loadStateFromPrefs();
            if (savedState != null) {
                Log.d(TAG, "ViewModel initialized with a saved state.");
                _itineraryState.postValue(savedState);
                _currentLocationName.postValue(savedState.getLocationName());
                _currentLocationStatus.postValue("Loaded saved plan for: " + savedState.getLocationName());
                if (savedState.getItineraryItems() != null && !savedState.getItineraryItems().isEmpty()) {
                    Calendar tripStart = savedState.getItineraryItems().get(0).getStartTime();
                    if (tripStart != null) {
                        checkIfTripIsAlreadySaved(savedState.getItineraryItems(), savedState.getLocationName(), tripStart);
                    } else {
                        Log.w(TAG, "Loaded itinerary item has an invalid start time. Cannot check history.");
                        _isItinerarySaved.postValue(false);
                    }
                } else {
                    _isItinerarySaved.postValue(false);
                }
            } else {
                Log.d(TAG, "ViewModel initialized with no saved state.");
                _itineraryState.postValue(null);
            }
        });
    }

    public void loadOrGenerateItinerary(GeoPoint startLocation, String locationName, Calendar tripStart, Calendar tripEnd, List<Place> allPlaces, boolean forceRegenerate, List<String> categoryPreferences) {
        _isLoading.setValue(true);
        executorService.execute(() -> {
            ItineraryState savedState = loadStateFromPrefs();
            if (savedState != null && !forceRegenerate && savedState.isStillValid(startLocation.getLatitude(), startLocation.getLongitude(), tripStart.getTimeInMillis(), tripEnd.getTimeInMillis(), categoryPreferences)) {
                Log.d(TAG, "Valid saved itinerary found. Loading from SharedPreferences.");
                _itineraryState.postValue(savedState);
                _currentLocationName.postValue(savedState.getLocationName());
                _currentLocationStatus.postValue("Loaded saved plan for: " + savedState.getLocationName());
                checkIfTripIsAlreadySaved(savedState.getItineraryItems(), savedState.getLocationName(), tripStart);
                _isLoading.postValue(false);
            } else {
                Log.d(TAG, forceRegenerate ? "Forcing regeneration." : "No valid saved itinerary. Generating new one.");
                ItineraryGenerator.GenerationResult result = itineraryGenerator.generateWithTimeWindows(startLocation, allPlaces, tripStart, tripEnd, categoryPreferences, null);
                List<ItineraryItem> generatedItems = result.itinerary;
                StringBuilder messageBuilder = new StringBuilder();
                if (generatedItems.isEmpty()) {
                    messageBuilder.append("We couldn't find any open attractions for your selected time and categories, or the schedule was too tight. Try extending your trip time or changing preferences.");
                } else if (categoryPreferences != null && !categoryPreferences.isEmpty() && generatedItems.size() < categoryPreferences.size()) {
                    messageBuilder.append("We couldn't find matches for all your preferences, but here's what we found!");
                } else {
                    messageBuilder.append("This is a suggested plan. Times are flexible and based on your trip window and location hours.");
                }
                if (result.unmetPreferences != null && !result.unmetPreferences.isEmpty()) {
                    messageBuilder.append("\n\nNote: We could not find a nearby location for the following:\n");
                    for (int i = 0; i < result.unmetPreferences.size(); i++) {
                        messageBuilder.append("• ").append(result.unmetPreferences.get(i));
                        if (i < result.unmetPreferences.size() - 1) {
                            messageBuilder.append("\n");
                        }
                    }
                }
                fetchRecommendationsAndBuildState(generatedItems, messageBuilder.toString(), startLocation, locationName, tripStart, tripEnd, categoryPreferences, false, -1);
            }
        });
    }

    public void lockAndRecalculateItinerary(int indexToLock, Calendar newStartTime, Calendar newEndTime, List<Place> allPlaces) {
        _isLoading.setValue(true);
        executorService.execute(() -> {
            ItineraryState currentState = _itineraryState.getValue();
            if (currentState == null || currentState.getItineraryItems() == null) {
                Log.e(TAG, "Cannot lock item, current state is null.");
                _isLoading.postValue(false);
                return;
            }

            List<ItineraryItem> currentItems = currentState.getItineraryItems();
            if (indexToLock >= currentItems.size()) {
                Log.e(TAG, "Cannot lock item, index out of bounds.");
                _isLoading.postValue(false);
                return;
            }

            // --- FIX START ---
            // Create the list of places IN THE CORRECT ORDER from the current itinerary.
            List<Place> currentSequenceOfPlaces = new ArrayList<>();
            for (ItineraryItem item : currentItems) {
                allPlaces.stream()
                        .filter(p -> p.getDocumentId().equals(item.getPlaceDocumentId()))
                        .findFirst()
                        .ifPresent(currentSequenceOfPlaces::add);
            }

            if (currentSequenceOfPlaces.size() != currentItems.size()) {
                Log.e(TAG, "Mismatch between itinerary items and found places. Aborting recalculation.");
                new android.os.Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(getApplication(), "Error: Could not find all places for recalculation.", Toast.LENGTH_LONG).show());
                _isLoading.postValue(false);
                return;
            }
            // --- FIX END ---

            ItineraryItem itemToLock = currentItems.get(indexToLock);
            ItineraryItem lockedItem = new ItineraryItem(
                    itemToLock.getId(), newStartTime, newEndTime, itemToLock.getActivity(),
                    itemToLock.getRating(), itemToLock.getImageUrl(), itemToLock.getCoordinates(),
                    itemToLock.getPlaceDocumentId(), itemToLock.getCategory()
            );
            lockedItem.setTimeLocked(true);

            GeoPoint startLocation = new GeoPoint(currentState.getStartLat(), currentState.getStartLon());
            Calendar tripStart = Calendar.getInstance();
            tripStart.setTimeInMillis(currentState.getStartTimeMillis());
            Calendar tripEnd = Calendar.getInstance();
            tripEnd.setTimeInMillis(currentState.getEndTimeMillis());

            // Pass the CORRECT sequence of places to the generator
            ItineraryGenerator.GenerationResult result = itineraryGenerator.generateWithTimeWindows(
                    startLocation, currentSequenceOfPlaces, tripStart, tripEnd, currentState.getCategoryPreferences(), lockedItem
            );

            String message;
            if (result.itinerary.isEmpty() || result.itinerary.size() < currentItems.size()) {
                message = "Could not fit all stops with the new time for '" + lockedItem.getActivity() + "'. Some stops were removed.";
                new android.os.Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(getApplication(), "Conflict: Could not fit all stops.", Toast.LENGTH_LONG).show());
            } else {
                message = "Schedule updated around your new time for '" + lockedItem.getActivity() + "'.";
            }

            fetchRecommendationsAndBuildState(result.itinerary, message, startLocation, currentState.getLocationName(),
                    tripStart, tripEnd, currentState.getCategoryPreferences(), true, indexToLock);
        });
    }


    public void replaceItineraryItem(int indexToReplace, Place newPlace, List<Place> allPlaces) {
        _isLoading.setValue(true);
        executorService.execute(() -> {
            ItineraryState currentState = _itineraryState.getValue();
            if (currentState == null) {
                Log.e(TAG, "Cannot replace item, current state is null.");
                _isLoading.postValue(false);
                return;
            }
            List<Place> newSequenceOfPlaces = new ArrayList<>();
            List<ItineraryItem> oldItinerary = currentState.getItineraryItems();
            for (int i = 0; i < oldItinerary.size(); i++) {
                if (i == indexToReplace) {
                    newSequenceOfPlaces.add(newPlace);
                } else {
                    String placeId = oldItinerary.get(i).getPlaceDocumentId();
                    allPlaces.stream()
                            .filter(p -> p.getDocumentId().equals(placeId))
                            .findFirst()
                            .ifPresent(newSequenceOfPlaces::add);
                }
            }

            GeoPoint startLocation = new GeoPoint(currentState.getStartLat(), currentState.getStartLon());
            Calendar tripStart = Calendar.getInstance();
            tripStart.setTimeInMillis(currentState.getStartTimeMillis());
            Calendar tripEnd = Calendar.getInstance();
            tripEnd.setTimeInMillis(currentState.getEndTimeMillis());

            List<String> forcedSequenceCategories = newSequenceOfPlaces.stream()
                    .map(Place::getCategory)
                    .collect(Collectors.toList());

            ItineraryGenerator.GenerationResult result = itineraryGenerator.generateWithTimeWindows(startLocation, newSequenceOfPlaces, tripStart, tripEnd, forcedSequenceCategories, null);
            String message = "Your itinerary has been updated with your changes. Times have been recalculated.";
            fetchRecommendationsAndBuildState(result.itinerary, message, startLocation, currentState.getLocationName(), tripStart, tripEnd, currentState.getCategoryPreferences(), true, -1);
        });
    }

    public void deleteItineraryItem(int indexToDelete, List<Place> allPlaces) {
        _isLoading.setValue(true);
        executorService.execute(() -> {
            ItineraryState currentState = _itineraryState.getValue();
            if (currentState == null || currentState.getItineraryItems() == null || indexToDelete >= currentState.getItineraryItems().size()) {
                Log.e(TAG, "Cannot delete item, invalid state or index.");
                _isLoading.postValue(false);
                return;
            }
            List<ItineraryItem> oldItinerary = currentState.getItineraryItems();
            List<ItineraryItem> tempItinerary = new ArrayList<>(oldItinerary);
            tempItinerary.remove(indexToDelete);

            if (tempItinerary.isEmpty()) {
                clearItinerary();
                _isLoading.postValue(false);
                return;
            }

            List<Place> newSequenceOfPlaces = new ArrayList<>();
            for (ItineraryItem item : tempItinerary) {
                String placeId = item.getPlaceDocumentId();
                allPlaces.stream()
                        .filter(p -> p.getDocumentId().equals(placeId))
                        .findFirst()
                        .ifPresent(newSequenceOfPlaces::add);
            }

            GeoPoint startLocation = new GeoPoint(currentState.getStartLat(), currentState.getStartLon());
            Calendar tripStart = Calendar.getInstance();
            tripStart.setTimeInMillis(currentState.getStartTimeMillis());
            Calendar tripEnd = Calendar.getInstance();
            tripEnd.setTimeInMillis(currentState.getEndTimeMillis());

            List<String> forcedSequenceCategories = newSequenceOfPlaces.stream()
                    .map(Place::getCategory)
                    .collect(Collectors.toList());

            ItineraryGenerator.GenerationResult result = itineraryGenerator.generateWithTimeWindows(startLocation, newSequenceOfPlaces, tripStart, tripEnd, forcedSequenceCategories, null);
            String message = "Item removed. Your schedule has been recalculated.";
            fetchRecommendationsAndBuildState(result.itinerary, message, startLocation, currentState.getLocationName(), tripStart, tripEnd, currentState.getCategoryPreferences(), true, -1);
        });
    }

    public void clearItinerary() {
        sharedPreferences.edit().remove(KEY_ACTIVE_ITINERARY_STATE).apply();
        _itineraryState.postValue(null);
        _isItinerarySaved.postValue(false);
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new android.os.Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getApplication(), "Itinerary cleared.", Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(getApplication(), "Itinerary cleared.", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchRecommendationsAndBuildState(List<ItineraryItem> mainItinerary, String message, GeoPoint startLocation, String locationName, Calendar tripStart, Calendar tripEnd, List<String> categoryPreferences, boolean isUserModified, int lockedItemIndex) {
        List<String> excludedIds = new ArrayList<>();
        if (mainItinerary != null) {
            for (ItineraryItem item : mainItinerary) {
                if (item.getPlaceDocumentId() != null) excludedIds.add(item.getPlaceDocumentId());
            }
        }

        Query topRatedQuery = db.collection("places").orderBy("rating", Query.Direction.DESCENDING).limit(10);
        if (!excludedIds.isEmpty() && excludedIds.size() < 10) {
            topRatedQuery = topRatedQuery.whereNotIn(com.google.firebase.firestore.FieldPath.documentId(), excludedIds);
        }

        topRatedQuery.get().addOnCompleteListener(task -> {
            List<Place> topRatedPlaces = new ArrayList<>();
            if (task.isSuccessful() && task.getResult() != null) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Place place = document.toObject(Place.class);
                    place.setDocumentId(document.getId());
                    topRatedPlaces.add(place);
                }
            } else {
                Log.w(TAG, "Error getting recommended places.", task.getException());
            }

            ItineraryState newState = new ItineraryState(
                    startLocation.getLatitude(), startLocation.getLongitude(),
                    tripStart.getTimeInMillis(), tripEnd.getTimeInMillis(),
                    categoryPreferences, lockedItemIndex, mainItinerary, topRatedPlaces,
                    message, locationName, isUserModified
            );

            saveStateToPrefs(newState);
            _itineraryState.postValue(newState);
            _currentLocationName.postValue(locationName);
            _currentLocationStatus.postValue("Generated new plan for: " + locationName);
            checkIfTripIsAlreadySaved(mainItinerary, locationName, tripStart);
            _isLoading.postValue(false);
        });
    }

    private void saveStateToPrefs(ItineraryState state) {
        if (state == null) return;
        String json = gson.toJson(state);
        sharedPreferences.edit().putString(KEY_ACTIVE_ITINERARY_STATE, json).apply();
        Log.d(TAG, "Itinerary state saved to SharedPreferences.");
    }

    private ItineraryState loadStateFromPrefs() {
        String json = sharedPreferences.getString(KEY_ACTIVE_ITINERARY_STATE, null);
        if (json == null) return null;
        try {
            return gson.fromJson(json, ItineraryState.class);
        } catch (Exception e) {
            Log.e(TAG, "Error deserializing ItineraryState from JSON", e);
            return null;
        }
    }

    public void saveCurrentTripToHistory(Calendar tripStartCalendar) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getApplication(), "You must be signed in to save a trip.", Toast.LENGTH_SHORT).show();
            return;
        }
        ItineraryState currentState = _itineraryState.getValue();
        if (currentState == null || currentState.getItineraryItems() == null || currentState.getItineraryItems().isEmpty()) {
            Toast.makeText(getApplication(), "No itinerary to save.", Toast.LENGTH_SHORT).show();
            return;
        }
        _isItinerarySaved.setValue(true);
        Toast.makeText(getApplication(), "Saving trip...", Toast.LENGTH_SHORT).show();

        List<ItineraryItem> currentItinerary = currentState.getItineraryItems();
        String tripTitle = "Trip to " + currentState.getLocationName();
        String tripDate = DateFormat.getDateInstance(DateFormat.MEDIUM).format(tripStartCalendar.getTime());
        String signature = generateTripSignature(currentItinerary, currentState.getLocationName(), tripStartCalendar);

        List<Map<String, String>> itineraryForDb = new ArrayList<>();
        for (ItineraryItem item : currentItinerary) {
            Map<String, String> itemMap = new HashMap<>();
            itemMap.put("activity", item.getActivity());
            itemMap.put("time", item.getFormattedTime());
            itemMap.put("rating", item.getRating());
            itineraryForDb.add(itemMap);
        }

        Trip tripToSave = new Trip(tripTitle, tripDate, signature, itineraryForDb);
        db.collection("users").document(currentUser.getUid())
                .collection("tripHistory")
                .add(tripToSave)
                .addOnSuccessListener(documentReference -> Toast.makeText(getApplication(), "Trip saved successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    Toast.makeText(getApplication(), "Error saving trip. It will be saved when you're online.", Toast.LENGTH_LONG).show();
                    Log.w(TAG, "Error adding document, will be retried by Firestore offline persistence.", e);
                });
    }

    private void checkIfTripIsAlreadySaved(List<ItineraryItem> itinerary, String locationName, Calendar tripStartCalendar) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || itinerary == null || itinerary.isEmpty()) {
            _isItinerarySaved.postValue(false);
            return;
        }
        String signature = generateTripSignature(itinerary, locationName, tripStartCalendar);
        db.collection("users").document(currentUser.getUid())
                .collection("tripHistory")
                .whereEqualTo("tripSignature", signature)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    _isItinerarySaved.postValue(task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty());
                });
    }

    private String generateTripSignature(List<ItineraryItem> itinerary, String locationName, Calendar tripStartCalendar) {
        if (itinerary == null || itinerary.isEmpty() || tripStartCalendar == null) {
            return "";
        }
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(tripStartCalendar.getTime());
        String firstActivity = itinerary.get(0).getActivity();
        return locationName + "_" + date + "_" + firstActivity;
    }

    public void updateLocationStatus(String name, String status) {
        _currentLocationName.postValue(name);
        _currentLocationStatus.postValue(status);
    }
}
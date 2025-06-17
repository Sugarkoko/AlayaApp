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

import java.text.DateFormat;
import java.text.SimpleDateFormat; // <-- THIS IS THE FIX
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ItineraryViewModel extends AndroidViewModel {

    private static final String TAG = "ItineraryViewModel";
    private static final String PREFS_NAME = "AlayaAppPrefs";
    private static final String KEY_ACTIVE_ITINERARY_STATE = "active_itinerary_state";

    private final MutableLiveData<List<Object>> _displayList = new MutableLiveData<>();
    public final LiveData<List<Object>> displayList = _displayList;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<Boolean> _isItinerarySaved = new MutableLiveData<>(false);
    public final LiveData<Boolean> isItinerarySaved = _isItinerarySaved;

    private final MutableLiveData<String> _currentLocationName = new MutableLiveData<>("Tap to get current location");
    public final LiveData<String> currentLocationName = _currentLocationName;

    private final MutableLiveData<String> _currentLocationStatus = new MutableLiveData<>("Set your location to begin");
    public final LiveData<String> currentLocationStatus = _currentLocationStatus;

    private final SharedPreferences sharedPreferences;
    private final Gson gson;
    private final FirebaseFirestore db;
    private final ItineraryGenerator itineraryGenerator;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private ItineraryState activeItineraryState;

    public ItineraryViewModel(@NonNull Application application) {
        super(application);
        sharedPreferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new GsonBuilder().create();
        db = FirebaseFirestore.getInstance();
        itineraryGenerator = new ItineraryGenerator();
    }

    public void loadOrGenerateItinerary(GeoPoint startLocation, String locationName, Calendar tripStart, Calendar tripEnd, List<Place> allPlaces, boolean forceRegenerate) {
        _isLoading.setValue(true);
        executorService.execute(() -> {
            ItineraryState savedState = loadStateFromPrefs();

            if (savedState != null && !forceRegenerate &&
                    savedState.isStillValid(startLocation.getLatitude(), startLocation.getLongitude(), tripStart.getTimeInMillis(), tripEnd.getTimeInMillis())) {
                Log.d(TAG, "Valid saved itinerary found. Loading from SharedPreferences.");
                activeItineraryState = savedState;
                _currentLocationName.postValue(savedState.getLocationName());
                _currentLocationStatus.postValue("Loaded saved plan for: " + savedState.getLocationName());
                checkIfTripIsAlreadySaved(activeItineraryState.getItineraryItems(), activeItineraryState.getLocationName(), tripStart);
            } else {
                Log.d(TAG, forceRegenerate ? "Forcing regeneration." : "No valid saved itinerary. Generating new one.");
                List<ItineraryItem> generatedItems = itineraryGenerator.generate(startLocation, allPlaces, tripStart, tripEnd);
                String message = "";
                if (generatedItems.size() < 2) {
                    message = "We couldn't find many open attractions for your selected time.";
                }
                fetchRecommendationsAndBuildState(generatedItems, message, startLocation, locationName, tripStart, tripEnd);
                return; // The async recommendation fetch will handle the rest
            }

            // If we loaded from cache, we still need to build the display list
            buildAndPostDisplayList(activeItineraryState);
            _isLoading.postValue(false);
        });
    }

    public void clearItinerary() {
        sharedPreferences.edit().remove(KEY_ACTIVE_ITINERARY_STATE).apply();
        activeItineraryState = null;
        _displayList.postValue(new ArrayList<>());
        _isItinerarySaved.postValue(false);
        Toast.makeText(getApplication(), "Itinerary cleared.", Toast.LENGTH_SHORT).show();
    }

    private void fetchRecommendationsAndBuildState(List<ItineraryItem> mainItinerary, String message, GeoPoint startLocation, String locationName, Calendar tripStart, Calendar tripEnd) {
        List<String> excludedIds = new ArrayList<>();
        if (mainItinerary != null) {
            for (ItineraryItem item : mainItinerary) {
                if (item.getPlaceDocumentId() != null) excludedIds.add(item.getPlaceDocumentId());
            }
        }

        Query topRatedQuery = db.collection("places").orderBy("rating", Query.Direction.DESCENDING).limit(10);
        if (!excludedIds.isEmpty()) {
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

            activeItineraryState = new ItineraryState(
                    startLocation.getLatitude(), startLocation.getLongitude(),
                    tripStart.getTimeInMillis(), tripEnd.getTimeInMillis(),
                    mainItinerary, topRatedPlaces, message, locationName
            );

            saveStateToPrefs(activeItineraryState);
            _currentLocationName.postValue(locationName);
            _currentLocationStatus.postValue("Generated new plan for: " + locationName);
            checkIfTripIsAlreadySaved(mainItinerary, locationName, tripStart);
            buildAndPostDisplayList(activeItineraryState);
            _isLoading.postValue(false);
        });
    }

    private void buildAndPostDisplayList(ItineraryState state) {
        if (state == null) {
            _displayList.postValue(new ArrayList<>());
            return;
        }
        List<Object> items = new ArrayList<>();
        items.add(new ItineraryAdapter.LocationHeaderData(state.getHeaderMessage()));

        if (state.getItineraryItems() != null && !state.getItineraryItems().isEmpty()) {
            items.add("Suggested Itinerary");
            items.addAll(state.getItineraryItems());
        }

        if (state.getTopRatedPlaces() != null && !state.getTopRatedPlaces().isEmpty()) {
            items.add(new ItineraryAdapter.HorizontalListContainer("Top Rated", state.getTopRatedPlaces()));
        }

        if (state.getItineraryItems() != null && !state.getItineraryItems().isEmpty()) {
            items.add("Hours are based on standard schedules. We recommend checking ahead for holidays or special events.");
        }
        _displayList.postValue(items);
    }

    private void saveStateToPrefs(ItineraryState state) {
        if (state == null) return;
        String json = gson.toJson(state);
        sharedPreferences.edit().putString(KEY_ACTIVE_ITINERARY_STATE, json).apply();
        Log.d(TAG, "Itinerary state saved to SharedPreferences.");
    }

    private ItineraryState loadStateFromPrefs() {
        String json = sharedPreferences.getString(KEY_ACTIVE_ITINERARY_STATE, null);
        if (json == null) {
            return null;
        }
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
        if (activeItineraryState == null || activeItineraryState.getItineraryItems().isEmpty()) {
            Toast.makeText(getApplication(), "No itinerary to save.", Toast.LENGTH_SHORT).show();
            return;
        }

        _isItinerarySaved.setValue(true);
        Toast.makeText(getApplication(), "Saving trip...", Toast.LENGTH_SHORT).show();

        List<ItineraryItem> currentItinerary = activeItineraryState.getItineraryItems();
        String tripTitle = "Trip to " + activeItineraryState.getLocationName();
        String tripDate = DateFormat.getDateInstance(DateFormat.MEDIUM).format(tripStartCalendar.getTime());
        String signature = generateTripSignature(currentItinerary, activeItineraryState.getLocationName(), tripStartCalendar);

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
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        _isItinerarySaved.postValue(true);
                    } else {
                        _isItinerarySaved.postValue(false);
                    }
                });
    }

    private String generateTripSignature(List<ItineraryItem> itinerary, String locationName, Calendar tripStartCalendar) {
        if (itinerary == null || itinerary.isEmpty()) return "";
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(tripStartCalendar.getTime());
        String firstActivity = itinerary.get(0).getActivity();
        String secondActivity = itinerary.size() > 1 ? itinerary.get(1).getActivity() : "";
        return locationName + "_" + date + "_" + firstActivity + "_" + secondActivity;
    }

    public void updateLocationStatus(String name, String status) {
        _currentLocationName.postValue(name);
        _currentLocationStatus.postValue(status);
    }
}
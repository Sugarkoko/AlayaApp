package com.example.alayaapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager; // Removed ItemTouchHelper import
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.alayaapp.databinding.ActivityItinerariesBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ItinerariesActivity extends AppCompatActivity
        implements ItineraryAdapter.OnItemClickListener { // Removed OnStartDragListener

    private ActivityItinerariesBinding binding;
    private ItineraryAdapter itineraryAdapter;
    // Removed ItemTouchHelper and Callback variables

    private List<ItineraryItem> suggestedList = new ArrayList<>();
    private boolean isEditMode = false;
    final int CURRENT_ITEM_ID = R.id.navigation_itineraries;

    private static final String PREFS_NAME = "AlayaAppPrefs";
    private static final String KEY_CURRENT_LOCATION = "currentLocation";
    private static final String KEY_SUGGESTED_ITINERARY = "suggestedItineraryJson";
    private static final String DEFAULT_LOCATION = "Baguio City";

    private Gson gson = new Gson();
    private Map<String, List<ItineraryItem>> sampleItineraryData;
    private Map<String, List<ItineraryItem>> locationChoicesData;
    private String currentLocation;
    private final String[] availableLocations = {"Baguio City", "Cubao", "BGC"};
    private int selectedLocationIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityItinerariesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        createSampleData();
        createLocationChoicesData();
        loadCurrentLocation();
        loadOrGenerateItineraryForLocation(currentLocation, false);
        setupRecyclerView(); // No longer sets up ItemTouchHelper
        setupBottomNavListener();
        setupClickListeners();
    }

    private void setupRecyclerView() {
        itineraryAdapter = new ItineraryAdapter(suggestedList, this); // Removed drag listener
        binding.rvSuggestedItinerary.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSuggestedItinerary.setAdapter(itineraryAdapter);
        // ItemTouchHelper setup removed
    }

    private void setupClickListeners() {
        binding.ivEditItinerary.setOnClickListener(v -> enterEditMode());
        binding.tvSaveChanges.setOnClickListener(v -> exitEditModeAndSave());
        binding.tvLocationCity.setOnClickListener(v -> showChangeLocationDialog());
        setupRecommendedClickListeners();
    }

    private void loadCurrentLocation() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        currentLocation = prefs.getString(KEY_CURRENT_LOCATION, DEFAULT_LOCATION);
        binding.tvLocationCity.setText(currentLocation);
    }

    private void saveCurrentLocation(String location) {
        if (location == null || location.trim().isEmpty()) return;
        currentLocation = location.trim();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_CURRENT_LOCATION, currentLocation);
        editor.apply();
        binding.tvLocationCity.setText(currentLocation);
        Log.d("ItineraryActivity", "Saved current location: " + currentLocation);
    }

    private void showChangeLocationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Location");

        int currentSelectionIndex = -1;
        for (int i = 0; i < availableLocations.length; i++) {
            if (availableLocations[i].equalsIgnoreCase(currentLocation)) {
                currentSelectionIndex = i;
                break;
            }
        }
        if (currentSelectionIndex == -1) currentSelectionIndex = 0;
        selectedLocationIndex = currentSelectionIndex;

        builder.setSingleChoiceItems(availableLocations, currentSelectionIndex, (dialog, which) -> {
            selectedLocationIndex = which;
        });

        builder.setPositiveButton("OK", (dialog, which) -> {
            if (selectedLocationIndex != -1) {
                String newLocation = availableLocations[selectedLocationIndex];
                if (!newLocation.equalsIgnoreCase(currentLocation)) {
                    saveCurrentLocation(newLocation);
                    loadOrGenerateItineraryForLocation(currentLocation, true);
                } else {
                    dialog.dismiss();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void loadOrGenerateItineraryForLocation(String location, boolean isLocationChange) {
        List<ItineraryItem> loadedList = loadItineraryFromPrefs();
        suggestedList.clear();
        boolean generatedNew = false;

        if (loadedList != null && !loadedList.isEmpty()) {
            suggestedList.addAll(loadedList);
            Log.d("ItineraryActivity", "Loaded saved itinerary for: " + location);
        } else {
            List<ItineraryItem> sampleItems = findSampleData(location);

            if (sampleItems != null) {
                List<ItineraryItem> itemsToUse = new ArrayList<>();
                for(ItineraryItem item : sampleItems) { // Deep copy might be safer depending on future use
                    itemsToUse.add(new ItineraryItem(item.getId(), item.getTime() != null ? (Calendar)item.getTime().clone() : null,
                            item.getActivity(), item.getRating(), item.getBestTimeToVisit(),
                            item.getLatitude(), item.getLongitude()));
                }
                suggestedList.addAll(itemsToUse);

                Log.d("ItineraryActivity", "No saved data, generated sample itinerary for: " + location);
                if (itineraryAdapter != null) {
                    itineraryAdapter.recalculateTimes();
                } else {
                    Log.w("ItineraryActivity", "Adapter not ready during initial sample data time calculation");
                }
                saveItineraryToPrefs(suggestedList);
                generatedNew = true;
            } else {
                Log.d("ItineraryActivity", "No saved or sample data found for: " + location);
                clearSavedItinerary();
            }
        }

        if (itineraryAdapter != null) {
            itineraryAdapter.notifyDataSetChanged();
        } else {
            setupRecyclerView();
        }


        if (isLocationChange || generatedNew) {
            binding.rvSuggestedItinerary.setVisibility(suggestedList.isEmpty() ? View.GONE : View.VISIBLE);
            if (isEditMode) {
                forceExitEditMode();
            }
            if (generatedNew && !isLocationChange) {
                Toast.makeText(this, "Loaded sample itinerary for " + location, Toast.LENGTH_SHORT).show();
            } else if (isLocationChange && generatedNew) {
                Toast.makeText(this, "Generated sample itinerary for " + location, Toast.LENGTH_SHORT).show();
            } else if (isLocationChange && !generatedNew && loadedList != null && !loadedList.isEmpty()) {
                Toast.makeText(this, "Loaded previously saved itinerary for " + location, Toast.LENGTH_SHORT).show();
            }
        }
        binding.rvSuggestedItinerary.setVisibility(suggestedList.isEmpty() ? View.GONE : View.VISIBLE);
    }


    private List<ItineraryItem> findSampleData(String location) {
        for (Map.Entry<String, List<ItineraryItem>> entry : sampleItineraryData.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(location)) {
                return entry.getValue();
            }
        }
        return null;
    }


    private void clearSavedItinerary() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String key = KEY_SUGGESTED_ITINERARY + "_" + currentLocation;
        prefs.edit().remove(key).apply();
        Log.d("ItineraryActivity", "Cleared saved itinerary from SharedPreferences for " + currentLocation);
    }


    private List<ItineraryItem> loadItineraryFromPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String key = KEY_SUGGESTED_ITINERARY + "_" + currentLocation;
        String json = prefs.getString(key, null);
        if (TextUtils.isEmpty(json)) {
            return null;
        }

        try {
            Type listType = new TypeToken<ArrayList<ItineraryItem>>() {}.getType();
            return gson.fromJson(json, listType);
        } catch (Exception e) {
            Log.e("ItineraryActivity", "Error parsing itinerary JSON from SharedPreferences", e);
            prefs.edit().remove(key).apply();
            return null;
        }
    }

    private void saveItineraryToPrefs(List<ItineraryItem> listToSave) {
        if (listToSave == null || currentLocation == null) return;

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        try {
            String json = gson.toJson(listToSave);
            editor.putString(KEY_SUGGESTED_ITINERARY + "_" + currentLocation, json);
            editor.apply();
            Log.d("ItineraryActivity", "Saved itinerary to SharedPreferences for " + currentLocation);
        } catch (Exception e) {
            Log.e("ItineraryActivity", "Error converting itinerary to JSON for SharedPreferences", e);
            Toast.makeText(this, "Error saving changes", Toast.LENGTH_SHORT).show();
        }
    }

    private void createSampleData() {
        sampleItineraryData = new HashMap<>();
        Calendar cal = Calendar.getInstance();
        long idCounter = 1;

        List<ItineraryItem> baguioList = new ArrayList<>();
        cal.set(Calendar.HOUR_OF_DAY, 9); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
        baguioList.add(new ItineraryItem(idCounter++, (Calendar)cal.clone(), "Burnham Park", "4.4", "Morning/Afternoon", 16.4123, 120.5950));
        baguioList.add(new ItineraryItem(idCounter++, null, "Baguio Cathedral", "4.5", "Anytime", 16.4137, 120.5987));
        baguioList.add(new ItineraryItem(idCounter++, null, "Mines View Park", "4.3", "Morning (for view)", 16.4188, 120.6286));
        baguioList.add(new ItineraryItem(idCounter++, null, "Camp John Hay", "4.7", "Daytime", 16.4000, 120.6167));
        sampleItineraryData.put("Baguio City", baguioList);

        List<ItineraryItem> cubaoList = new ArrayList<>();
        idCounter = 101;
        cal.set(Calendar.HOUR_OF_DAY, 10); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
        cubaoList.add(new ItineraryItem(idCounter++, (Calendar)cal.clone(), "Gateway Mall", "4.5", "Anytime", 14.6199, 121.0535));
        cubaoList.add(new ItineraryItem(idCounter++, null, "Art in Island", "4.6", "Afternoon", 14.6214, 121.0553));
        cubaoList.add(new ItineraryItem(idCounter++, null, "Araneta Coliseum", "4.4", "Event Dependent", 14.6208, 121.0545));
        cubaoList.add(new ItineraryItem(idCounter++, null, "Farmers Market/Plaza", "4.3", "Morning (Market)", 14.6188, 121.0530));
        sampleItineraryData.put("Cubao", cubaoList);

        List<ItineraryItem> bgcList = new ArrayList<>();
        idCounter = 201;
        cal.set(Calendar.HOUR_OF_DAY, 11); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
        bgcList.add(new ItineraryItem(idCounter++, (Calendar)cal.clone(), "Bonifacio High Street", "4.6", "Afternoon/Evening", 14.5515, 121.0506));
        bgcList.add(new ItineraryItem(idCounter++, null, "The Mind Museum", "4.5", "Daytime (check hours)", 14.5538, 121.0467));
        bgcList.add(new ItineraryItem(idCounter++, null, "Venice Grand Canal Mall", "4.4", "Evening (lights)", 14.5368, 121.0534));
        bgcList.add(new ItineraryItem(idCounter++, null, "Burgos Circle", "4.5", "Evening (Dining)", 14.5544, 121.0492));
        sampleItineraryData.put("BGC", bgcList);
        sampleItineraryData.put("Bonifacio Global City", bgcList);
    }

    private void createLocationChoicesData() {
        locationChoicesData = new HashMap<>();
        long choiceIdCounter = -1;

        List<ItineraryItem> baguioChoices = new ArrayList<>();
        baguioChoices.add(new ItineraryItem(choiceIdCounter--, null, "Session Road", "4.2", "Anytime", 16.4128, 120.5978));
        baguioChoices.add(new ItineraryItem(choiceIdCounter--, null, "Good Shepherd Convent", "4.6", "Daytime (Shopping)", 16.4278, 120.6183));
        baguioChoices.add(new ItineraryItem(choiceIdCounter--, null, "Tam-awan Village", "4.1", "Daytime", 16.4293, 120.5803));
        baguioChoices.add(new ItineraryItem(choiceIdCounter--, null, "Wright Park", "4.0", "Daytime", 16.4155, 120.6139));
        baguioChoices.add(new ItineraryItem(choiceIdCounter--, null, "The Mansion", "4.2", "Daytime (Outside View)", 16.4146, 120.6149));
        baguioChoices.add(new ItineraryItem(choiceIdCounter--, null, "Laperal White House", "3.9", "Daytime (Spooky!)", 16.4108, 120.6077));
        locationChoicesData.put("Baguio City", baguioChoices);

    }

    private void enterEditMode() {
        if (suggestedList.isEmpty()) {
            Toast.makeText(this, "Itinerary is empty.", Toast.LENGTH_SHORT).show();
            return;
        }
        isEditMode = true;
        binding.ivEditItinerary.setVisibility(View.GONE);
        binding.tvSaveChanges.setVisibility(View.VISIBLE);
        if (itineraryAdapter != null) itineraryAdapter.setEditMode(true);
        Toast.makeText(this, "Edit mode enabled. Click item to replace.", Toast.LENGTH_SHORT).show();
    }

    private void exitEditModeAndSave() {
        forceExitEditMode();
        List<ItineraryItem> updatedList = itineraryAdapter.getCurrentList();
        saveItineraryToPrefs(updatedList);
        Toast.makeText(this, "Changes Saved", Toast.LENGTH_SHORT).show();
    }

    private void forceExitEditMode() {
        isEditMode = false;
        binding.ivEditItinerary.setVisibility(View.VISIBLE);
        binding.tvSaveChanges.setVisibility(View.GONE);
        if (itineraryAdapter != null) itineraryAdapter.setEditMode(false);
    }

    @Override
    public void onItemClick(int position) {
        if (position < 0 || position >= suggestedList.size()) return;

        if (isEditMode) {
            if (currentLocation.equalsIgnoreCase("Baguio City")) {
                showReplaceItemDialog(position);
            } else {
                Toast.makeText(this, "Item replacement only available for Baguio City in this prototype.", Toast.LENGTH_SHORT).show();
            }
        } else {
            ItineraryItem clickedItem = suggestedList.get(position);
            if (clickedItem != null) {
                showItemOverviewDialog(clickedItem);
            }
        }
    }

    private void showReplaceItemDialog(final int positionToReplace) {
        List<ItineraryItem> choices = locationChoicesData.get("Baguio City");
        if (choices == null || choices.isEmpty()) {
            Toast.makeText(this, "No replacement choices available.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<ItineraryItem> filteredChoices = new ArrayList<>();
        List<String> currentActivities = suggestedList.stream()
                .map(ItineraryItem::getActivity)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        for (ItineraryItem choice : choices) {
            if (!currentActivities.contains(choice.getActivity())) {
                filteredChoices.add(choice);
            }
        }

        if (filteredChoices.isEmpty()) {
            Toast.makeText(this, "All available choices are already in the itinerary.", Toast.LENGTH_SHORT).show();
            return;
        }

        final CharSequence[] choiceNames = new CharSequence[filteredChoices.size()];
        for (int i = 0; i < filteredChoices.size(); i++) {
            choiceNames[i] = filteredChoices.get(i).getActivity();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Replace with:");
        builder.setItems(choiceNames, (dialog, which) -> {
            ItineraryItem chosenItemData = filteredChoices.get(which);
            ItineraryItem originalItem = suggestedList.get(positionToReplace);

            if (originalItem != null && chosenItemData != null) {
                ItineraryItem newItem = new ItineraryItem(
                        originalItem.getId(),
                        originalItem.getTime(),
                        chosenItemData.getActivity(),
                        chosenItemData.getRating(),
                        chosenItemData.getBestTimeToVisit(),
                        chosenItemData.getLatitude(),
                        chosenItemData.getLongitude()
                );

                suggestedList.set(positionToReplace, newItem);
                itineraryAdapter.notifyItemChanged(positionToReplace);
                Log.d("ItineraryActivity", "Replaced item at position " + positionToReplace + " with " + newItem.getActivity());
            }
            dialog.dismiss();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }


    private void showItemOverviewDialog(ItineraryItem item) {
        if (item == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(item.getActivity() != null ? item.getActivity() : "Details");
        String message = "Rating: " + (item.getRating() != null ? item.getRating() : "N/A") + "\n" +
                "Best time to visit: " + (item.getBestTimeToVisit() != null ? item.getBestTimeToVisit() : "Anytime");
        builder.setMessage(message);
        builder.setPositiveButton("Navigate", (dialog, which) -> startNavigation(item));
        builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void startNavigation(ItineraryItem item) {
        if (item == null) {
            Toast.makeText(this, "Cannot navigate: Item data missing", Toast.LENGTH_SHORT).show();
            return;
        }
        String uriString;
        if (item.getLatitude() != 0 || item.getLongitude() != 0) {
            uriString = String.format(Locale.US, "geo:0,0?q=%f,%f(%s)",
                    item.getLatitude(), item.getLongitude(), Uri.encode(item.getActivity()));
        } else if (!TextUtils.isEmpty(item.getActivity())){
            uriString = "geo:0,0?q=" + Uri.encode(item.getActivity());
        } else {
            Toast.makeText(this, "Cannot navigate: Location name or coordinates missing", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri gmmIntentUri = Uri.parse(uriString);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Toast.makeText(this, "No map application found", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e("ItineraryActivity", "Error starting map intent: " + uriString, e);
            Toast.makeText(this, "Could not launch map application", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupBottomNavListener() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int destinationItemId = item.getItemId();
            if (destinationItemId == CURRENT_ITEM_ID) return true;
            Class<?> destinationActivity = null;
            if (destinationItemId == R.id.navigation_home) destinationActivity = HomeActivity.class;
            else if (destinationItemId == R.id.navigation_map) { Toast.makeText(this, "Map Clicked (No Activity)", Toast.LENGTH_SHORT).show(); return true; }
            else if (destinationItemId == R.id.navigation_profile) { Toast.makeText(this, "Profile Clicked (No Activity)", Toast.LENGTH_SHORT).show(); return true; }

            if (destinationActivity != null) {
                boolean slideRightToLeft = getItemIndex(destinationItemId) > getItemIndex(CURRENT_ITEM_ID);
                navigateTo(destinationActivity, slideRightToLeft);
                return true;
            }
            return false;
        });
    }

    private void navigateTo(Class<?> destinationActivity, boolean slideRight) {
        Intent intent = new Intent(getApplicationContext(), destinationActivity);
        startActivity(intent);
        if (slideRight) overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        else overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }

    private int getItemIndex(int itemId) {
        if (itemId == R.id.navigation_home) return 0;
        if (itemId == R.id.navigation_itineraries) return 1;
        if (itemId == R.id.navigation_map) return 2;
        if (itemId == R.id.navigation_profile) return 3;
        return -1;
    }

    private void setupRecommendedClickListeners() {
        binding.cardRecommended1.setOnClickListener(v -> handleRecommendedClick("Arca's Yard Cafe"));
        binding.cardRecommended2.setOnClickListener(v -> handleRecommendedClick("Wright Park Riding Center"));
        binding.cardRecommended3.setOnClickListener(v -> handleRecommendedClick("Baguio Orchidarium"));
        binding.cardRecommended4.setOnClickListener(v -> handleRecommendedClick("Camp John Hay Picnic Area"));
    }

    private void handleRecommendedClick(String itemName) {
        Toast.makeText(this, itemName + " clicked (Implement Action)", Toast.LENGTH_SHORT).show();
    }
}
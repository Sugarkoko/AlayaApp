package com.example.alayaapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context; // Added for SharedPreferences
import android.content.Intent;
import android.content.SharedPreferences; // Added for SharedPreferences
import android.os.Bundle;
import android.text.TextUtils; // Added for checking empty strings
import android.util.Log; // Added for logging potential errors
import android.view.View;
import android.widget.Toast;

import com.example.alayaapp.databinding.ActivityItinerariesBinding;
import com.google.gson.Gson; // Added for JSON conversion
import com.google.gson.reflect.TypeToken; // Added for List deserialization

import java.lang.reflect.Type; // Added for List deserialization
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ItinerariesActivity extends AppCompatActivity implements ItineraryAdapter.OnStartDragListener {

    private ActivityItinerariesBinding binding;
    private ItineraryAdapter itineraryAdapter;
    private ItemTouchHelper itemTouchHelper;
    private ItineraryItemTouchHelperCallback touchHelperCallback;

    private List<ItineraryItem> suggestedList = new ArrayList<>();
    private boolean isEditMode = false;
    final int CURRENT_ITEM_ID = R.id.navigation_itineraries;

    // Constants for SharedPreferences
    private static final String PREFS_NAME = "AlayaAppPrefs";
    private static final String KEY_SUGGESTED_ITINERARY = "suggestedItineraryJson";
    private Gson gson = new Gson(); // Gson instance for JSON handling

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityItinerariesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Load data (tries SharedPreferences first, then defaults)
        loadItineraryData();

        // Setup RecyclerView
        setupRecyclerView(); // Setup adapter *after* suggestedList is potentially loaded

        // Setup Bottom Nav
        binding.bottomNavigation.setSelectedItemId(CURRENT_ITEM_ID);
        setupBottomNavListener();

        // Setup Click Listeners for Edit/Save
        binding.ivEditItinerary.setOnClickListener(v -> enterEditMode());
        binding.tvSaveChanges.setOnClickListener(v -> exitEditModeAndSave());

        // Setup Click Listener for Location Box
        binding.tvLocationCity.setOnClickListener(v -> {
            // TODO: Implement location change feature (Dialog, Activity, etc.)
            Toast.makeText(this, "Change Location Clicked (Implement Feature)", Toast.LENGTH_SHORT).show();
        });

        // Setup Click Listeners for Recommended Itineraries
        setupRecommendedClickListeners();
    }

    private void setupRecyclerView() {
        itineraryAdapter = new ItineraryAdapter(suggestedList, this);
        binding.rvSuggestedItinerary.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSuggestedItinerary.setAdapter(itineraryAdapter);

        // Setup ItemTouchHelper
        touchHelperCallback = new ItineraryItemTouchHelperCallback(itineraryAdapter);
        itemTouchHelper = new ItemTouchHelper(touchHelperCallback);
        itemTouchHelper.attachToRecyclerView(binding.rvSuggestedItinerary);
    }

    // --- Data Loading ---
    private void loadItineraryData() {
        List<ItineraryItem> loadedList = loadItineraryFromPrefs();
        if (loadedList != null && !loadedList.isEmpty()) {
            suggestedList.clear();
            suggestedList.addAll(loadedList);
            Log.d("ItineraryActivity", "Loaded itinerary from SharedPreferences.");
        } else {
            Log.d("ItineraryActivity", "No saved itinerary found, loading default placeholders.");
            loadDefaultPlaceholderData();
            // Optional: Save the default data immediately so it's there next time
            // saveItineraryToPrefs(suggestedList);
        }
        // Adapter will be notified after setupRecyclerView is called
    }

    private List<ItineraryItem> loadItineraryFromPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_SUGGESTED_ITINERARY, null);
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        try {
            // Define the type of the list for Gson deserialization
            Type listType = new TypeToken<ArrayList<ItineraryItem>>() {}.getType();
            return gson.fromJson(json, listType);
        } catch (Exception e) {
            Log.e("ItineraryActivity", "Error parsing itinerary JSON from SharedPreferences", e);
            return null; // Return null if parsing fails
        }
    }

    private void loadDefaultPlaceholderData() {
        // Ensure list is clear before adding defaults
        suggestedList.clear();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 9); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
        suggestedList.add(new ItineraryItem(1, (Calendar)cal.clone(), "Breakfast at Café by the Ruins", "4.5"));
        cal.add(Calendar.HOUR_OF_DAY, 1); // 10 AM
        suggestedList.add(new ItineraryItem(2, (Calendar)cal.clone(), "Burnham Park", "4.4"));
        cal.add(Calendar.HOUR_OF_DAY, 2); // 12 PM
        suggestedList.add(new ItineraryItem(3, (Calendar)cal.clone(), "Lunch at Choco-late de Batirol", "4.5"));
        cal.add(Calendar.HOUR_OF_DAY, 2); // 2 PM
        suggestedList.add(new ItineraryItem(4, (Calendar)cal.clone(), "Mines View Park", "4.3"));
        cal.add(Calendar.HOUR_OF_DAY, 1); // 3 PM
        suggestedList.add(new ItineraryItem(5, (Calendar)cal.clone(), "Baguio Cathedral", "4.5"));
        cal.add(Calendar.HOUR_OF_DAY, 1); // 4 PM
        suggestedList.add(new ItineraryItem(6, (Calendar)cal.clone(), "Lemon and Olives", "4.5"));
        cal.add(Calendar.HOUR_OF_DAY, 1); // 5 PM
        suggestedList.add(new ItineraryItem(7, (Calendar)cal.clone(), "Dinner at Café Yagam", "4.5"));
        // No need to notify adapter here, it will be done after RecyclerView setup
    }

    // --- Data Saving ---
    private void saveItineraryToPrefs(List<ItineraryItem> listToSave) {
        if (listToSave == null) return;

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        try {
            String json = gson.toJson(listToSave);
            editor.putString(KEY_SUGGESTED_ITINERARY, json);
            editor.apply(); // Use apply() for asynchronous saving
            Log.d("ItineraryActivity", "Itinerary saved to SharedPreferences.");
        } catch (Exception e) {
            Log.e("ItineraryActivity", "Error converting itinerary to JSON for SharedPreferences", e);
            Toast.makeText(this, "Error saving changes", Toast.LENGTH_SHORT).show(); // Inform user
        }
    }


    // --- Edit Mode Handling ---
    private void enterEditMode() {
        isEditMode = true;
        binding.ivEditItinerary.setVisibility(View.GONE);
        binding.tvSaveChanges.setVisibility(View.VISIBLE);
        itineraryAdapter.setEditMode(true);
        if(touchHelperCallback != null) {
            touchHelperCallback.setEditMode(true);
        }
        Toast.makeText(this, "Edit mode enabled. Drag handles to reorder.", Toast.LENGTH_SHORT).show();
    }

    private void exitEditModeAndSave() {
        isEditMode = false;
        binding.ivEditItinerary.setVisibility(View.VISIBLE);
        binding.tvSaveChanges.setVisibility(View.GONE);
        itineraryAdapter.setEditMode(false);
        if(touchHelperCallback != null) {
            touchHelperCallback.setEditMode(false);
        }

        // Get the potentially reordered and time-updated list from the adapter
        List<ItineraryItem> updatedList = itineraryAdapter.getCurrentList();

        // Save the updated list using SharedPreferences
        saveItineraryToPrefs(updatedList);

        Toast.makeText(this, "Changes Saved", Toast.LENGTH_SHORT).show();
    }

    // --- Implementation of OnStartDragListener ---
    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        if (itemTouchHelper != null && isEditMode) {
            itemTouchHelper.startDrag(viewHolder);
        }
    }

    // --- Bottom Nav Logic ---
    private void setupBottomNavListener() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int destinationItemId = item.getItemId();

            if (destinationItemId == CURRENT_ITEM_ID) {
                return true; // Already here
            }

            Class<?> destinationActivity = null;
            if (destinationItemId == R.id.navigation_home) {
                destinationActivity = HomeActivity.class;
            } else if (destinationItemId == R.id.navigation_map) {
                // TODO: destinationActivity = MapActivity.class;
                Toast.makeText(ItinerariesActivity.this, "Map Clicked (No Activity)", Toast.LENGTH_SHORT).show();
                return true;
            } else if (destinationItemId == R.id.navigation_profile) {
                // TODO: destinationActivity = ProfileActivity.class;
                Toast.makeText(ItinerariesActivity.this, "Profile Clicked (No Activity)", Toast.LENGTH_SHORT).show();
                return true;
            }

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
        if (slideRight) {
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } else {
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
        finish();
    }

    private int getItemIndex(int itemId) {
        if (itemId == R.id.navigation_home) return 0;
        if (itemId == R.id.navigation_itineraries) return 1;
        if (itemId == R.id.navigation_map) return 2;
        if (itemId == R.id.navigation_profile) return 3;
        return -1;
    }

    // --- Recommended Itineraries Click Handling ---
    private void setupRecommendedClickListeners() {
        binding.cardRecommended1.setOnClickListener(v -> handleRecommendedClick("Arca's Yard Cafe"));
        binding.cardRecommended2.setOnClickListener(v -> handleRecommendedClick("Wright Park Riding Center"));
        binding.cardRecommended3.setOnClickListener(v -> handleRecommendedClick("Baguio Orchidarium"));
        binding.cardRecommended4.setOnClickListener(v -> handleRecommendedClick("Camp John Hay Picnic Area"));
        // Add listeners for any other cards here
    }

    private void handleRecommendedClick(String itemName) {
        // TODO: Implement actual navigation or detail view for recommended items
        Toast.makeText(this, itemName + " clicked (Implement Action)", Toast.LENGTH_SHORT).show();
    }
}
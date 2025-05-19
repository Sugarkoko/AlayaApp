package com.example.alayaapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper; // Import
import androidx.recyclerview.widget.LinearLayoutManager; // Import
import androidx.recyclerview.widget.RecyclerView; // Import

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View; // Import
import android.widget.ImageView;
import android.widget.TextView; // Import
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList; // Import
import java.util.Calendar; // Import
import java.util.List; // Import

public class ItinerariesActivity extends AppCompatActivity implements ItineraryAdapter.OnStartDragListener { // Implement listener

    BottomNavigationView bottomNavigationView;
    // ImageView ivEditItinerary; // We find both edit and save now
    View ivEditItinerary; // Use View for easier toggling if needed
    TextView tvSaveChanges;
    RecyclerView rvSuggestedItinerary;
    ItineraryAdapter itineraryAdapter;
    ItemTouchHelper itemTouchHelper;
    ItineraryItemTouchHelperCallback touchHelperCallback; // Keep reference to update edit mode


    List<ItineraryItem> suggestedList = new ArrayList<>(); // Your data list
    private boolean isEditMode = false; // State tracking

    final int CURRENT_ITEM_ID = R.id.navigation_itineraries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itineraries);

        // Find Views
        ivEditItinerary = findViewById(R.id.iv_edit_itinerary);
        tvSaveChanges = findViewById(R.id.tv_save_changes);
        rvSuggestedItinerary = findViewById(R.id.rv_suggested_itinerary);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Setup RecyclerView
        setupRecyclerView();

        // Load Initial Data (Placeholder)
        loadPlaceholderData();

        // Setup Bottom Nav
        bottomNavigationView.setSelectedItemId(CURRENT_ITEM_ID);
        setupBottomNavListener();


        // Setup Click Listeners for Edit/Save
        ivEditItinerary.setOnClickListener(v -> enterEditMode());
        tvSaveChanges.setOnClickListener(v -> exitEditModeAndSave());

    }

    private void setupRecyclerView() {
        itineraryAdapter = new ItineraryAdapter(suggestedList, this); // Pass listener
        rvSuggestedItinerary.setLayoutManager(new LinearLayoutManager(this));
        rvSuggestedItinerary.setAdapter(itineraryAdapter);

        // Setup ItemTouchHelper
        touchHelperCallback = new ItineraryItemTouchHelperCallback(itineraryAdapter);
        itemTouchHelper = new ItemTouchHelper(touchHelperCallback);
        itemTouchHelper.attachToRecyclerView(rvSuggestedItinerary);
    }

    private void loadPlaceholderData() {
        // Create some dummy data with initial times
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 9); cal.set(Calendar.MINUTE, 0);
        suggestedList.add(new ItineraryItem(1, (Calendar)cal.clone(), "Breakfast at Café by the Ruins", "4.5"));
        cal.add(Calendar.HOUR_OF_DAY, 1);
        suggestedList.add(new ItineraryItem(2, (Calendar)cal.clone(), "Burnham Park", "4.5")); // Changed to Burnham
        cal.add(Calendar.HOUR_OF_DAY, 2); // 12 PM
        suggestedList.add(new ItineraryItem(3, (Calendar)cal.clone(), "Lunch at Choco-late de Batirol", "4.5"));
        cal.add(Calendar.HOUR_OF_DAY, 2); // 2 PM
        suggestedList.add(new ItineraryItem(4, (Calendar)cal.clone(), "Mines View Park", "4.5"));
        cal.add(Calendar.HOUR_OF_DAY, 1); // 3 PM
        suggestedList.add(new ItineraryItem(5, (Calendar)cal.clone(), "Baguio Cathedral", "4.5"));
        cal.add(Calendar.HOUR_OF_DAY, 1); // 4 PM
        suggestedList.add(new ItineraryItem(6, (Calendar)cal.clone(), "Lemon and Olives", "4.5"));
        cal.add(Calendar.HOUR_OF_DAY, 1); // 5 PM
        suggestedList.add(new ItineraryItem(7, (Calendar)cal.clone(), "Dinner at Café Yagam", "4.5"));

        itineraryAdapter.notifyDataSetChanged();
    }

    private void enterEditMode() {
        isEditMode = true;
        ivEditItinerary.setVisibility(View.GONE);
        tvSaveChanges.setVisibility(View.VISIBLE);
        itineraryAdapter.setEditMode(true);
        if(touchHelperCallback != null) {
            touchHelperCallback.setEditMode(true); // Enable dragging in callback
        }
        Toast.makeText(this, "Edit mode enabled. Drag handles to reorder.", Toast.LENGTH_SHORT).show();

    }

    private void exitEditModeAndSave() {
        isEditMode = false;
        ivEditItinerary.setVisibility(View.VISIBLE);
        tvSaveChanges.setVisibility(View.GONE);
        itineraryAdapter.setEditMode(false);
        if(touchHelperCallback != null) {
            touchHelperCallback.setEditMode(false); // Disable dragging in callback
        }

        // ** TODO: Implement actual saving logic here! **
        // For now, just get the potentially reordered list
        List<ItineraryItem> updatedList = itineraryAdapter.getCurrentList();
        // In a real app, save 'updatedList' to database/sharedprefs/API

        Toast.makeText(this, "Changes Saved (Placeholder)", Toast.LENGTH_SHORT).show();

        // Optional: Refresh adapter completely if saving involves complex changes,
        // but usually hiding handles is enough.
        // itineraryAdapter.notifyDataSetChanged();
    }


    // --- Implementation of OnStartDragListener ---
    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        if (itemTouchHelper != null && isEditMode) { // Only drag in edit mode
            itemTouchHelper.startDrag(viewHolder);
        }
    }

    // --- Bottom Nav Logic ---
    private void setupBottomNavListener() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int destinationItemId = item.getItemId();

            if (destinationItemId == CURRENT_ITEM_ID) { // CURRENT_ITEM_ID is R.id.navigation_itineraries
                return true; // Already on Itineraries screen
            }

            Class<?> destinationActivityClass = null; // Use a class variable for clarity
            // boolean slideRightToLeft = true; // We'll determine this based on index

            if (destinationItemId == R.id.navigation_home) {
                destinationActivityClass = HomeActivity.class;
            } else if (destinationItemId == R.id.navigation_map) {
                Toast.makeText(ItinerariesActivity.this, "Map Clicked (No Activity)", Toast.LENGTH_SHORT).show();
                // If you had a MapActivity, you might do:
                // destinationActivityClass = MapActivity.class;
                return true; // Return true to show selection change visually
            } else if (destinationItemId == R.id.navigation_profile) { // <<< --- THIS IS THE MODIFIED PART ---
                destinationActivityClass = ProfileActivity.class; // Set the destination to ProfileActivity
            }
            // No 'else if' for R.id.navigation_itineraries as it's handled by CURRENT_ITEM_ID check

            if (destinationActivityClass != null) {
                Intent intent = new Intent(getApplicationContext(), destinationActivityClass);
                startActivity(intent);

                // Determine slide direction based on item index
                boolean slideRightToLeft = getItemIndex(destinationItemId) > getItemIndex(CURRENT_ITEM_ID);

                if (slideRightToLeft) {
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } else {
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                }
                finish(); // Close ItinerariesActivity
                return true;
            }
            return false; // Item not handled
        });
    }


    private int getItemIndex(int itemId) {
        if (itemId == R.id.navigation_home) return 0;
        if (itemId == R.id.navigation_itineraries) return 1;
        if (itemId == R.id.navigation_map) return 2;
        if (itemId == R.id.navigation_profile) return 3;
        return -1;
    }

}
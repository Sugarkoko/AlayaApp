package com.example.alayaapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // For logging potential issues
import android.widget.Toast; // For user feedback or placeholders

import androidx.activity.EdgeToEdge; // Keeping this as it was in your original
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.alayaapp.databinding.ActivityMapsBinding; // Import ViewBinding
// Removed map-specific imports if not directly used for navigation setup
// import com.google.android.gms.maps.OnMapReadyCallback;
// import com.google.android.gms.maps.SupportMapFragment;
// import com.google.android.gms.maps.GoogleMap;
// etc.

public class MapsActivity extends AppCompatActivity /* Remove "implements OnMapReadyCallback" if map logic is deferred */ {

    private static final String TAG = "MapsActivity";
    private ActivityMapsBinding binding; // Use ViewBinding

    // Define the current item ID for MapsActivity
    final int CURRENT_ITEM_ID = R.id.navigation_map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // From your original code

        // Inflate layout using ViewBinding
        try {
            binding = ActivityMapsBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
        } catch (Exception e) {
            Log.e(TAG, "Error inflating layout: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading page.", Toast.LENGTH_LONG).show();
            finish(); // Can't proceed if layout is broken
            return;
        }

        // Apply Window Insets for EdgeToEdge
        // Ensure your root view in activity_maps.xml has android:id="@+id/main_maps_root_container" or similar
        // I'll assume binding.getRoot() is the correct view for applying insets here.
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Adjust padding for the root view. The bottom padding is set to 0
            // to avoid double padding if BottomNavigationView handles its own insets
            // or if it's outside this padded root.
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // --- Setup Bottom Navigation ---
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        if (binding.bottomNavigationMapsPage == null) {
            Log.e(TAG, "BottomNavigationView (bottom_navigation_maps_page) not found in layout!");
            // Consider adding a fallback or ensuring the ID is correct in activity_maps.xml
            return;
        }

        binding.bottomNavigationMapsPage.setSelectedItemId(CURRENT_ITEM_ID);

        binding.bottomNavigationMapsPage.setOnItemSelectedListener(item -> {
            int destinationItemId = item.getItemId();

            if (destinationItemId == CURRENT_ITEM_ID) {
                return true; // Already on the Maps screen
            }

            Class<?> destinationActivityClass = null;
            // Determine if the current activity should be finished when navigating
            boolean finishCurrent = true; // Usually true for main tab navigation

            if (destinationItemId == R.id.navigation_home) {
                destinationActivityClass = HomeActivity.class;
            } else if (destinationItemId == R.id.navigation_itineraries) {
                destinationActivityClass = ItinerariesActivity.class;
            } else if (destinationItemId == R.id.navigation_profile) {
                destinationActivityClass = ProfileActivity.class;
            }
            // No case for R.id.navigation_map as it's handled by CURRENT_ITEM_ID check

            if (destinationActivityClass != null) {
                Intent intent = new Intent(MapsActivity.this, destinationActivityClass);
                // Clear previous activities in the stack if navigating to a main tab
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);

                // Determine animation based on item index
                boolean slideRightToLeft = getItemIndex(destinationItemId) > getItemIndex(CURRENT_ITEM_ID);
                if (slideRightToLeft) {
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } else {
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                }

                if (finishCurrent) {
                    finish(); // Close MapsActivity
                }
                return true;
            }
            return false; // Item ID not handled
        });
    }

    // Helper to get the order/index of bottom navigation items
    // This helps determine slide animation direction
    private int getItemIndex(int itemId) {
        if (itemId == R.id.navigation_home) return 0;
        if (itemId == R.id.navigation_itineraries) return 1;
        if (itemId == R.id.navigation_map) return 2;
        if (itemId == R.id.navigation_profile) return 3;
        return -1; // Should not happen if IDs are correct
    }

    // Removed map-specific methods like onMapReady, enableMyLocation, etc.
    // Removed FusedLocationProviderClient and LatLng constants as they are map-specific.
    // Removed BottomSheetBehavior related code.
    // Removed FAB click listener if it's map-specific.
}
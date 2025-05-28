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
// Removed map-specific imports

public class MapsActivity extends AppCompatActivity {

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
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // --- Setup Bottom Navigation ---
        setupBottomNavigation();

        // --- Placeholder for map functionality ---
        // Accessing views directly from the binding object
        if (binding.tvDirectionText != null) {
            binding.tvDirectionText.setText("Map will be displayed here.");
        } else {
            Log.w(TAG, "tvDirectionText is null in binding.");
        }

        // If you need to manipulate content inside the bottom sheet that is NOT map dependent:
        if (binding.llMapInfoContainer != null) {
            // Example: if there was a title or static text in llMapInfoContainer
            // binding.someTextViewInBottomSheet.setText("Details will appear here.");
            // For now, we'll assume tvRouteDuration, tvRouteDistance, tvNextDestinationBottom
            // are part of llMapInfoContainer and will show their XML default "N/A"
            if (binding.tvRouteDuration != null) binding.tvRouteDuration.setText("N/A");
            if (binding.tvRouteDistance != null) binding.tvRouteDistance.setText("N/A");
            if (binding.tvNextDestinationBottom != null) binding.tvNextDestinationBottom.setText("Select Destination");

        } else {
            Log.w(TAG, "llMapInfoContainer is null in binding.");
        }


        // FAB click listener
        if (binding.fabMyLocation != null) {
            binding.fabMyLocation.setOnClickListener(v -> {
                Toast.makeText(MapsActivity.this, "Map interaction to be implemented.", Toast.LENGTH_SHORT).show();
            });
        } else {
            Log.w(TAG, "fabMyLocation is null in binding.");
        }

    }

    private void setupBottomNavigation() {
        if (binding.bottomNavigationMapsPage == null) {
            Log.e(TAG, "BottomNavigationView (bottom_navigation_maps_page) not found in layout!");
            return;
        }

        binding.bottomNavigationMapsPage.setSelectedItemId(CURRENT_ITEM_ID);

        binding.bottomNavigationMapsPage.setOnItemSelectedListener(item -> {
            int destinationItemId = item.getItemId();

            if (destinationItemId == CURRENT_ITEM_ID) {
                return true; // Already on the Maps screen
            }

            Class<?> destinationActivityClass = null;
            boolean finishCurrent = true;

            if (destinationItemId == R.id.navigation_home) {
                destinationActivityClass = HomeActivity.class;
            } else if (destinationItemId == R.id.navigation_itineraries) {
                destinationActivityClass = ItinerariesActivity.class;
            } else if (destinationItemId == R.id.navigation_profile) {
                destinationActivityClass = ProfileActivity.class;
            }

            if (destinationActivityClass != null) {
                Intent intent = new Intent(MapsActivity.this, destinationActivityClass);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);

                boolean slideRightToLeft = getItemIndex(destinationItemId) > getItemIndex(CURRENT_ITEM_ID);
                if (slideRightToLeft) {
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } else {
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                }

                if (finishCurrent) {
                    finish();
                }
                return true;
            }
            return false;
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
package com.example.alayaapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View; // <-- Added for View.OnClickListener
import android.widget.Toast;
// Removed unused imports like Button, TextView, MenuItem, NavigationBarView etc.
// Keep NonNull import if used elsewhere, though not strictly needed for this code now
// import androidx.annotation.NonNull;

import com.example.alayaapp.databinding.ActivityHomeBinding; // Import ViewBinding class

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding; // Declare binding variable
    final int CURRENT_ITEM_ID = R.id.navigation_home; // Define constant for clarity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate the layout using ViewBinding
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        // Set the content view from the binding's root
        setContentView(binding.getRoot());

        // --- Set Initial State for Bottom Nav ---
        binding.bottomNavigation.setSelectedItemId(CURRENT_ITEM_ID);

        // --- Set Listeners ---

        // ** --- Add the Click Listener for the Burnham Park Card --- **
        // Make sure you have a CardView with id "card_burnham_park" in your activity_home.xml
        if (binding.cardBurnhamPark != null) { // Good practice to check if the view exists
            binding.cardBurnhamPark.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Create an Intent to start BurnhamDetailsActivity
                    // Make sure you have created BurnhamDetailsActivity.java and its layout,
                    // and declared it in AndroidManifest.xml
                    Intent intent = new Intent(HomeActivity.this, BurnhamDetailsActivity.class);
                    startActivity(intent); // Launch the activity

                    // Optional: Add a transition animation if you like.
                    // Note: The navigateTo helper method also applies transitions and finishes the current activity.
                    // If you don't want to finish HomeActivity, use overridePendingTransition directly here.
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left); // Example animation
                }
            });
        } else {
            // Optional: Log an error or show a toast if the CardView is not found,
            // which would indicate an issue with your layout XML file.
            // Toast.makeText(this, "Burnham Park CardView not found in layout!", Toast.LENGTH_LONG).show();
        }


        // Example listener for trip date button
        if (binding.btnTripDate != null) { // Check if this view exists in your layout
            binding.btnTripDate.setOnClickListener(v -> {
                // Handle trip date click
                // TODO: Implement Date Picker Dialog or similar
                Toast.makeText(HomeActivity.this, "Trip Date Clicked (Implement Date Picker)", Toast.LENGTH_SHORT).show();
            });
        }


        // Example Listener for location change
        if (binding.tvLocationCity2 != null) { // Check if this view exists in your layout
            binding.tvLocationCity2.setOnClickListener(v -> {
                // TODO: Implement Location Change Dialog/Screen
                Toast.makeText(HomeActivity.this, "Change Location Clicked (Implement Feature)", Toast.LENGTH_SHORT).show();
            });
        }


        // ** --- Bottom Navigation Item Selection Logic --- **
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == CURRENT_ITEM_ID) {
                // Already on Home screen, do nothing or maybe refresh data
                return true; // Indicate item selection was handled
            } else if (itemId == R.id.navigation_itineraries) {
                // Navigate to Itineraries Activity
                navigateTo(ItinerariesActivity.class, true); // Use helper method
                return true;
            } else if (itemId == R.id.navigation_map) {
                // TODO: Navigate to Map Activity (Create MapActivity first)
                // navigateTo(MapActivity.class, true);
                Toast.makeText(HomeActivity.this, "Map Clicked (No Activity)", Toast.LENGTH_SHORT).show();
                // Return true even if not navigating yet to show selection change visually
                return true;
            } else if (itemId == R.id.navigation_profile) {
                // TODO: Navigate to Profile Activity (Create ProfileActivity first)
                // navigateTo(ProfileActivity.class, true);
                Toast.makeText(HomeActivity.this, "Profile Clicked (No Activity)", Toast.LENGTH_SHORT).show();
                // Return true even if not navigating yet to show selection change visually
                return true;
            }
            return false; // Return false if the item ID is not handled
        });
    }

    // Helper method for navigation to avoid repetition
    private void navigateTo(Class<?> destinationActivity, boolean slideRight) {
        Intent intent = new Intent(getApplicationContext(), destinationActivity);
        startActivity(intent);
        if (slideRight) {
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } else {
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
        // Only finish if not navigating to home, or if it's a distinct new instance of home
        if (destinationActivity != HomeActivity.class) { // Avoid finishing if navigating to Home itself
            finish(); // Close current activity
        }
    }


    // Optional: Re-select item onResume if needed, but usually not necessary
    // if navigation always finishes the current activity.
    /*
    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null) { // Check binding exists
             binding.bottomNavigation.setSelectedItemId(CURRENT_ITEM_ID);
        }
    }
    */
}
package com.example.alayaapp;

import androidx.annotation.NonNull; // Import this
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent; // Import this
import android.os.Bundle;
import android.view.MenuItem; // Import this
import android.widget.Button; // If you have other buttons
import android.widget.TextView;
import android.widget.Toast; // Import this

import com.google.android.material.bottomnavigation.BottomNavigationView; // Import this
import com.google.android.material.navigation.NavigationBarView; // Import this

public class HomeActivity extends AppCompatActivity {

    // Declare the BottomNavigationView
    BottomNavigationView bottomNavigationView;

    // Declare other views if needed (like the trip date button)
    // Button btnTripDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home); // Your home layout file

        // --- Find Views ---
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        // btnTripDate = findViewById(R.id.btn_trip_date); // Example

        // --- Set Initial State for Bottom Nav ---
        // Set Home selected since we are in HomeActivity
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);

        // --- Set Listeners ---

        // Example listener for another button (if you have one)
        /*
        btnTripDate.setOnClickListener(v -> {
            // Handle trip date click
            Toast.makeText(HomeActivity.this, "Trip Date Clicked (No Action)", Toast.LENGTH_SHORT).show();
        });
        */

        // ** --- Add the Bottom Navigation Item Selection Logic --- **
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                // Already on Home screen, do nothing or maybe refresh data
                return true; // Indicate item selection was handled
            } else if (itemId == R.id.navigation_itineraries) {
                // Navigate to Itineraries Activity
                startActivity(new Intent(getApplicationContext(), ItinerariesActivity.class));
                // Optional: Add transition animation
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); // Simple fade
                finish(); // Close HomeActivity so back button doesn't return here
                return true;
            } else if (itemId == R.id.navigation_map) {
                // TODO: Navigate to Map Activity (Create MapActivity first)
                // startActivity(new Intent(getApplicationContext(), MapActivity.class));
                // overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                // finish();
                Toast.makeText(HomeActivity.this, "Map Clicked (No Activity)", Toast.LENGTH_SHORT).show();
                // Return true even if not navigating yet to show selection change
                return true;
            } else if (itemId == R.id.navigation_profile) {
                // TODO: Navigate to Profile Activity (Create ProfileActivity first)
                // startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                // overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                // finish();
                Toast.makeText(HomeActivity.this, "Profile Clicked (No Activity)", Toast.LENGTH_SHORT).show();
                // Return true even if not navigating yet to show selection change
                return true;
            }

            return false; // Return false if the item ID is not handled
        });
    }

    // Optional: Ensure the correct item is selected when resuming the activity
    // This helps if you navigate away and come back via means other than the nav bar
    /*
    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
    }
    */
}
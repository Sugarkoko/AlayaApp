package com.example.alayaapp;

import androidx.annotation.NonNull; // Keep if used, though not strictly for this specific change
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
// import android.view.MenuItem; // Unused in this version
import android.view.View;
// import android.widget.Button; // Unused
// import android.widget.TextView; // Unused
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
// import com.google.android.material.navigation.NavigationBarView; // Not directly used

public class HomeActivity extends AppCompatActivity {

    // Declare the BottomNavigationView
    BottomNavigationView bottomNavigationView;

    // Declare the CardView for Burnham Park
    CardView burnhamCard;

    // Define the current item ID for HomeActivity
    final int CURRENT_ITEM_ID = R.id.navigation_home; // Added for consistency if you add more logic

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home); // Your home layout file

        // --- Find Views ---
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        burnhamCard = findViewById(R.id.card_burnham_park);

        // --- Set Initial State for Bottom Nav ---
        bottomNavigationView.setSelectedItemId(CURRENT_ITEM_ID);

        // --- Set Listeners ---

        // ** --- Add the Click Listener for the Burnham Park Card --- **
        if (burnhamCard != null) { // Good practice to check if view was found
            burnhamCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(HomeActivity.this, BurnhamDetailsActivity.class);
                    startActivity(intent);
                    // Optional: Add a transition animation if you like
                    // Consider defining R.anim.slide_in_right and R.anim.slide_out_left
                    // overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                }
            });
        }


        // ** --- Updated Bottom Navigation Item Selection Logic --- **
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == CURRENT_ITEM_ID) { // If already on home
                return true;
            } else if (itemId == R.id.navigation_itineraries) {
                Intent intent = new Intent(getApplicationContext(), ItinerariesActivity.class);
                startActivity(intent);
                // Apply transition (ensure these anim files exist or use android.R.anim)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish(); // Close HomeActivity
                return true;
            } else if (itemId == R.id.navigation_map) {
                Toast.makeText(HomeActivity.this, "Map Clicked (No Activity)", Toast.LENGTH_SHORT).show();
                // If you had a MapActivity, you'd navigate similarly:
                // Intent intent = new Intent(getApplicationContext(), MapActivity.class);
                // startActivity(intent);
                // overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                // finish();
                return true; // Even if not navigating, return true to show selection
            } else if (itemId == R.id.navigation_profile) { // <<< --- THIS IS THE MODIFIED PART ---
                Intent intent = new Intent(getApplicationContext(), ProfileActivity.class);
                startActivity(intent);
                // Apply transition (ensure these anim files exist or use android.R.anim)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish(); // Close HomeActivity
                return true;
            }

            return false; // Item not handled
        });
    }

    // Optional onResume method (keep if you have it)
    /*
    @Override
    protected void onResume() {
        super.onResume();
        // This ensures the "Home" icon is selected if the user navigates back
        // to HomeActivity using the system back button without HomeActivity having been finished.
        // However, if you always finish() HomeActivity when navigating away, this might not be strictly necessary
        // unless you have other ways to return to it.
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(CURRENT_ITEM_ID);
        }
    }
    */
}
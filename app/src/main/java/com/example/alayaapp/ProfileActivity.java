package com.example.alayaapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.alayaapp.databinding.ActivityProfileBinding; // Import ViewBinding class

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    final int CURRENT_ITEM_ID = R.id.navigation_profile; // From your bottom_navigation_menu.xml

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // --- Set Initial State for Bottom Nav ---
        binding.bottomNavigationProfilePage.setSelectedItemId(CURRENT_ITEM_ID);

        // --- Setup Listeners ---
        setupBottomNavListener();
        setupActionListeners(); // Consolidate action listeners

        // Load profile data (if not hardcoded in XML)
        loadProfileData();
    }

    private void setupBottomNavListener() {
        binding.bottomNavigationProfilePage.setOnItemSelectedListener(item -> {
            int destinationItemId = item.getItemId();

            if (destinationItemId == CURRENT_ITEM_ID) {
                return true; // Already on Profile screen
            }

            Class<?> destinationActivityClass = null;
            if (destinationItemId == R.id.navigation_home) {
                destinationActivityClass = HomeActivity.class;
            } else if (destinationItemId == R.id.navigation_itineraries) {
                destinationActivityClass = ItinerariesActivity.class;
            } else if (destinationItemId == R.id.navigation_map) {
                Toast.makeText(ProfileActivity.this, "Map Clicked (No Activity)", Toast.LENGTH_SHORT).show();
                // destinationActivityClass = MapActivity.class; // If you have one
                return true; // Return true to show selection change visually
            }

            if (destinationActivityClass != null) {
                navigateTo(destinationActivityClass, destinationItemId, true);
                return true;
            }
            return false;
        });
    }

    private void setupActionListeners() {
        binding.ivLogout.setOnClickListener(v -> {
            // Handle logout logic
            Toast.makeText(this, "Logout Clicked", Toast.LENGTH_SHORT).show();
            // Example: Intent to LoginActivity, clear task, etc.
            // Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            // startActivity(intent);
            // finish();
        });

        binding.tvEditEmail.setOnClickListener(v -> {
            // Handle edit email logic
            Toast.makeText(this, "Edit Email Clicked", Toast.LENGTH_SHORT).show();
            // Example:
            // Intent intent = new Intent(ProfileActivity.this, EditEmailActivity.class);
            // startActivity(intent);
        });

        binding.tvEditPhone.setOnClickListener(v -> {
            // Handle edit phone logic
            Toast.makeText(this, "Edit Phone Clicked", Toast.LENGTH_SHORT).show();
            // Example:
            // Intent intent = new Intent(ProfileActivity.this, EditPhoneActivity.class);
            // startActivity(intent);
        });

        binding.layoutChangePassword.setOnClickListener(v -> {
            // Navigate to ChangePasswordActivity
            Intent intent = new Intent(ProfileActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
            // Optional: Add a specific transition for this action
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        binding.layoutHistory.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, TripHistoryActivity.class);
            startActivity(intent);
        });
    }

    private void loadProfileData() {
        // In a real app, fetch this from SharedPreferences, database, or API
        binding.tvProfileNameHeader.setText("Alice Go");
        binding.tvProfileNameDetail.setText("Alice Go");
        // if (binding.flProfileImageContainer.ivProfileIcon != null) { // Check if using FrameLayout structure
        //    binding.flProfileImageContainer.ivProfileIcon.setImageResource(R.drawable.ic_profile_placeholder);
        // }
        binding.tvProfileBirthday.setText("January 1, 2000");
        binding.tvProfileEmail.setText("alicego@gmail.com");
        binding.tvProfilePhone.setText("09215687102");
        if (binding.tvProfilePassword != null) { // Check if this TextView exists
            binding.tvProfilePassword.setText("************");
        }
    }

    // Helper method for bottom navigation
    private void navigateTo(Class<?> destinationActivityClass, int destinationItemId, boolean finishCurrent) {
        Intent intent = new Intent(getApplicationContext(), destinationActivityClass);
        startActivity(intent);

        // Determine slide direction based on item index for bottom nav items
        boolean slideRightToLeft = getItemIndex(destinationItemId) > getItemIndex(CURRENT_ITEM_ID);

        if (slideRightToLeft) {
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } else {
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }

        if (finishCurrent) {
            // finishAffinity(); // Use if you want to clear more of the back stack for main tab navigation
            finish(); // Finishes only the current activity
        }
    }

    // Helper to get the order/index of bottom navigation items
    private int getItemIndex(int itemId) {
        if (itemId == R.id.navigation_home) return 0;
        if (itemId == R.id.navigation_itineraries) return 1;
        if (itemId == R.id.navigation_map) return 2;
        if (itemId == R.id.navigation_profile) return 3;
        return -1;
    }

    // Optional: onResume
    /*
    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null) {
            binding.bottomNavigationProfilePage.setSelectedItemId(CURRENT_ITEM_ID);
        }
    }
    */
}
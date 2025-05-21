package com.example.alayaapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.alayaapp.databinding.ActivityProfileBinding;
import com.google.firebase.auth.FirebaseAuth; // Import FirebaseAuth

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    final int CURRENT_ITEM_ID = R.id.navigation_profile;

    private FirebaseAuth mAuth; // Declare Firebase Auth instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // --- Set Initial State for Bottom Nav ---
        binding.bottomNavigationProfilePage.setSelectedItemId(CURRENT_ITEM_ID);

        // --- Setup Listeners ---
        setupBottomNavListener();
        setupActionListeners();

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
                destinationActivityClass = MapsActivity.class; // Assuming MapsActivity exists
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
            // --- Firebase Logout Logic ---
            mAuth.signOut();
            Toast.makeText(ProfileActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();

            // Navigate to LoginActivity and clear the back stack
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); // Close ProfileActivity
        });

        binding.tvEditEmail.setOnClickListener(v -> {
            Toast.makeText(this, "Edit Email Clicked (Placeholder)", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(ProfileActivity.this, EditEmailActivity.class);
            // startActivity(intent);
        });

        binding.tvEditPhone.setOnClickListener(v -> {
            Toast.makeText(this, "Edit Phone Clicked (Placeholder)", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(ProfileActivity.this, EditPhoneActivity.class);
            // startActivity(intent);
        });

        binding.layoutChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        binding.layoutHistory.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, TripHistoryActivity.class);
            startActivity(intent);
            // Optional: overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    private void loadProfileData() {
        // In a real app, fetch this from SharedPreferences, database, or API
        // If user is logged in, you could fetch their email from mAuth.getCurrentUser().getEmail()
        // and name from your Realtime Database
        if (mAuth.getCurrentUser() != null) {
            binding.tvProfileEmail.setText(mAuth.getCurrentUser().getEmail());
            // You'd need to add a listener to Firebase Realtime Database to get the name here
            // For now, keeping placeholders or hardcoded values for name and birthday.
            binding.tvProfileNameHeader.setText("Alice Go"); // Placeholder
            binding.tvProfileNameDetail.setText("Alice Go"); // Placeholder
        } else {
            // Handle case where user is somehow null (shouldn't happen if they reached profile)
            binding.tvProfileNameHeader.setText("User");
            binding.tvProfileNameDetail.setText("User");
            binding.tvProfileEmail.setText("user@example.com");
        }

        binding.tvProfileBirthday.setText("January 1, 2000"); // Placeholder
        binding.tvProfilePhone.setText("09215687102"); // Placeholder
        if (binding.tvProfilePassword != null) {
            binding.tvProfilePassword.setText("************");
        }
    }

    private void navigateTo(Class<?> destinationActivityClass, int destinationItemId, boolean finishCurrent) {
        Intent intent = new Intent(getApplicationContext(), destinationActivityClass);
        // Clear previous activities in the stack if navigating to a main tab
        // This is important to prevent a deep back stack when switching main tabs
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
    }

    private int getItemIndex(int itemId) {
        if (itemId == R.id.navigation_home) return 0;
        if (itemId == R.id.navigation_itineraries) return 1;
        if (itemId == R.id.navigation_map) return 2;
        if (itemId == R.id.navigation_profile) return 3;
        return -1;
    }

    /*
    // Optional: Re-select current item if navigating back to ProfileActivity
    // without it being finished. However, with FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP
    // and finish() in navigateTo, this might not be strictly necessary.
    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null && binding.bottomNavigationProfilePage != null) {
            binding.bottomNavigationProfilePage.setSelectedItemId(CURRENT_ITEM_ID);
        }
    }
    */
}
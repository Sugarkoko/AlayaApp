package com.example.alayaapp;

import androidx.annotation.NonNull; // Added for DataSnapshot
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // Added for logging
import android.widget.Toast;

import com.example.alayaapp.databinding.ActivityProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser; // Added for FirebaseUser
import com.google.firebase.database.DataSnapshot; // Added
import com.google.firebase.database.DatabaseError; // Added
import com.google.firebase.database.DatabaseReference; // Added
import com.google.firebase.database.FirebaseDatabase; // Added
import com.google.firebase.database.ValueEventListener; // Added

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    final int CURRENT_ITEM_ID = R.id.navigation_profile;

    private FirebaseAuth mAuth;
    private DatabaseReference userDatabaseReference; // For fetching user data

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Initialize database reference to the specific user's node
            userDatabaseReference = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
        }


        binding.bottomNavigationProfilePage.setSelectedItemId(CURRENT_ITEM_ID);
        setupBottomNavListener();
        setupActionListeners();
        loadProfileData();
    }

    private void setupBottomNavListener() {
        binding.bottomNavigationProfilePage.setOnItemSelectedListener(item -> {
            int destinationItemId = item.getItemId();

            if (destinationItemId == CURRENT_ITEM_ID) {
                return true;
            }

            Class<?> destinationActivityClass = null;
            if (destinationItemId == R.id.navigation_home) {
                destinationActivityClass = HomeActivity.class;
            } else if (destinationItemId == R.id.navigation_itineraries) {
                destinationActivityClass = ItinerariesActivity.class;
            } else if (destinationItemId == R.id.navigation_map) {
                destinationActivityClass = MapsActivity.class;
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
            mAuth.signOut();
            Toast.makeText(ProfileActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
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
        });
    }

    private void loadProfileData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && userDatabaseReference != null) {
            // Set email from Auth directly as it's authoritative
            binding.tvProfileEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "N/A");

            userDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String name = dataSnapshot.child("name").getValue(String.class);
                        String contactNumber = dataSnapshot.child("contactNumber").getValue(String.class);
                        String birthday = dataSnapshot.child("birthday").getValue(String.class);
                        // Email is already set from Auth, but you could read it here too if needed for consistency check

                        binding.tvProfileNameHeader.setText(name != null && !name.isEmpty() ? name : "Set your name");
                        binding.tvProfileNameDetail.setText(name != null && !name.isEmpty() ? name : "Set your name");
                        binding.tvProfilePhone.setText(contactNumber != null && !contactNumber.isEmpty() ? contactNumber : "Set contact no.");
                        binding.tvProfileBirthday.setText(birthday != null && !birthday.isEmpty() ? birthday : "Set birthday");
                    } else {
                        Log.w(TAG, "User data not found in database for UID: " + currentUser.getUid());
                        // Set defaults or placeholders if no data found
                        binding.tvProfileNameHeader.setText("User Name");
                        binding.tvProfileNameDetail.setText("User Name");
                        binding.tvProfilePhone.setText("Not Set");
                        binding.tvProfileBirthday.setText("Not Set");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("ProfileActivity", "Failed to load profile data.", databaseError.toException());
                    Toast.makeText(ProfileActivity.this, "Failed to load profile details.", Toast.LENGTH_SHORT).show();
                    // Fallback UI if load fails
                    binding.tvProfileNameHeader.setText("User");
                    binding.tvProfileNameDetail.setText("User");
                    binding.tvProfileEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "user@example.com");
                    binding.tvProfilePhone.setText("Error loading");
                    binding.tvProfileBirthday.setText("Error loading");
                }
            });
        } else {
            // Handle case where user is null or database reference couldn't be initialized
            binding.tvProfileNameHeader.setText("User");
            binding.tvProfileNameDetail.setText("User");
            binding.tvProfileEmail.setText("user@example.com");
            binding.tvProfilePhone.setText("N/A");
            binding.tvProfileBirthday.setText("N/A");
            if (currentUser == null) {
                Log.e("ProfileActivity", "Cannot load profile data: current user is null.");
            } else {
                Log.e("ProfileActivity", "Cannot load profile data: userDatabaseReference is null.");
            }
        }

        // Password display is just a placeholder
        if (binding.tvProfilePassword != null) {
            binding.tvProfilePassword.setText("************");
        }
    }
    private static final String TAG = "ProfileActivity"; // Added TAG for logging


    private void navigateTo(Class<?> destinationActivityClass, int destinationItemId, boolean finishCurrent) {
        Intent intent = new Intent(getApplicationContext(), destinationActivityClass);
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
}
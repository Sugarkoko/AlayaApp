package com.example.alayaapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog; // Import AlertDialog
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog; // For birthday picker
import android.content.DialogInterface; // For AlertDialog buttons
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;       // For setting EditText input type
import android.text.TextUtils;    // For checking empty strings
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;    // For DatePickerDialog listener
import android.widget.EditText;      // For input dialogs
import android.widget.Toast;

import com.example.alayaapp.databinding.ActivityProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;          // For DatePickerDialog
import java.text.SimpleDateFormat;  // For formatting date
import java.util.Locale;            // For date formatting

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    final int CURRENT_ITEM_ID = R.id.navigation_profile;
    private static final String TAG = "ProfileActivity";

    private FirebaseAuth mAuth;
    private DatabaseReference userDatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userDatabaseReference = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
        }

        binding.bottomNavigationProfilePage.setSelectedItemId(CURRENT_ITEM_ID);
        setupBottomNavListener();
        setupActionListeners(); // This will now include edit listeners
        loadProfileData();
    }

    private void setupBottomNavListener() {
        binding.bottomNavigationProfilePage.setOnItemSelectedListener(item -> {
            int destinationItemId = item.getItemId();
            if (destinationItemId == CURRENT_ITEM_ID) return true;

            Class<?> destinationActivityClass = null;
            if (destinationItemId == R.id.navigation_home) destinationActivityClass = HomeActivity.class;
            else if (destinationItemId == R.id.navigation_itineraries) destinationActivityClass = ItinerariesActivity.class;
            else if (destinationItemId == R.id.navigation_map) destinationActivityClass = MapsActivity.class;

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

        // Remove or repurpose tvEditEmail and tvEditPhone if making main TextViews clickable
        // binding.tvEditEmail.setVisibility(View.GONE); // Example: Hide if not used
        // binding.tvEditPhone.setVisibility(View.GONE); // Example: Hide if not used


        // --- Make TextViews clickable for editing ---
        binding.tvProfileNameDetail.setOnClickListener(v -> showEditTextDialog("name", "Edit Name", binding.tvProfileNameDetail.getText().toString()));
        binding.tvProfileBirthday.setOnClickListener(v -> showBirthdayPickerDialog());
        binding.tvProfilePhone.setOnClickListener(v -> showEditTextDialog("contactNumber", "Edit Contact Number", binding.tvProfilePhone.getText().toString()));
        // For email, editing typically requires re-authentication for security.
        // For simplicity, I'm not implementing email editing here, but you could add it.
        // binding.tvProfileEmail.setOnClickListener(v -> showEditTextDialog("email", "Edit Email", binding.tvProfileEmail.getText().toString()));


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

    private void showEditTextDialog(final String fieldKey, String title, String currentValue) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        final EditText input = new EditText(this);
        if (fieldKey.equals("contactNumber")) {
            input.setInputType(InputType.TYPE_CLASS_PHONE);
        } else {
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        }

        // Pre-fill with current value, but only if it's not the placeholder
        if (!currentValue.startsWith("Set ") && !currentValue.equals("N/A") && !currentValue.equals("Not Set")) {
            input.setText(currentValue);
            input.setSelection(currentValue.length()); // Move cursor to end
        } else {
            input.setHint("Enter " + fieldKey.toLowerCase().replace("number", " no."));
        }

        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newValue = input.getText().toString().trim();
            if (!TextUtils.isEmpty(newValue)) {
                updateFirebaseField(fieldKey, newValue);
            } else {
                // Optionally allow saving an empty string to clear a field,
                // or enforce that a value must be entered.
                // For now, let's allow clearing by saving an empty string.
                updateFirebaseField(fieldKey, ""); // Or show a Toast "Field cannot be empty"
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showBirthdayPickerDialog() {
        Calendar calendar = Calendar.getInstance();
        // Try to parse current birthday text if it exists and is valid
        String currentBirthdayText = binding.tvProfileBirthday.getText().toString();
        if (!currentBirthdayText.startsWith("Set ") && !currentBirthdayText.equals("N/A") && !currentBirthdayText.equals("Not Set")) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // Or your display format
                calendar.setTime(sdf.parse(currentBirthdayText));
            } catch (Exception e) {
                // Could not parse, use current date as default for picker
                Log.w(TAG, "Could not parse existing birthday: " + currentBirthdayText, e);
            }
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, yearSelected, monthOfYear, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(yearSelected, monthOfYear, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // Store in a standard format
                    String formattedDate = sdf.format(selectedDate.getTime());
                    updateFirebaseField("birthday", formattedDate);
                }, year, month, day);
        datePickerDialog.show();
    }


    private void updateFirebaseField(final String fieldKey, final String value) {
        if (userDatabaseReference != null) {
            userDatabaseReference.child(fieldKey).setValue(value)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(ProfileActivity.this, fieldKey + " updated successfully", Toast.LENGTH_SHORT).show();
                        // Update UI locally
                        if (fieldKey.equals("name")) {
                            binding.tvProfileNameHeader.setText(!value.isEmpty() ? value : "Set your name");
                            binding.tvProfileNameDetail.setText(!value.isEmpty() ? value : "Set your name");
                        } else if (fieldKey.equals("contactNumber")) {
                            binding.tvProfilePhone.setText(!value.isEmpty() ? value : "Set contact no.");
                        } else if (fieldKey.equals("birthday")) {
                            binding.tvProfileBirthday.setText(!value.isEmpty() ? value : "Set birthday");
                        }
                        // If you add email editing, handle it here too
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ProfileActivity.this, "Failed to update " + fieldKey, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Failed to update " + fieldKey, e);
                    });
        } else {
            Toast.makeText(this, "Error: Not connected to database.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProfileData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && userDatabaseReference != null) {
            binding.tvProfileEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "N/A");

            userDatabaseReference.addValueEventListener(new ValueEventListener() { // Changed to addValueEventListener for real-time updates
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String name = dataSnapshot.child("name").getValue(String.class);
                        String contactNumber = dataSnapshot.child("contactNumber").getValue(String.class);
                        String birthday = dataSnapshot.child("birthday").getValue(String.class);

                        binding.tvProfileNameHeader.setText(name != null && !name.isEmpty() ? name : "Set your name");
                        binding.tvProfileNameDetail.setText(name != null && !name.isEmpty() ? name : "Set your name");
                        binding.tvProfilePhone.setText(contactNumber != null && !contactNumber.isEmpty() ? contactNumber : "Set contact no.");
                        binding.tvProfileBirthday.setText(birthday != null && !birthday.isEmpty() ? birthday : "Set birthday");
                    } else {
                        Log.w(TAG, "User data not found in database for UID: " + currentUser.getUid());
                        binding.tvProfileNameHeader.setText("Set your name");
                        binding.tvProfileNameDetail.setText("Set your name");
                        binding.tvProfilePhone.setText("Set contact no.");
                        binding.tvProfileBirthday.setText("Set birthday");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Failed to load profile data.", databaseError.toException());
                    Toast.makeText(ProfileActivity.this, "Failed to load profile details.", Toast.LENGTH_SHORT).show();
                    binding.tvProfileNameHeader.setText("User");
                    binding.tvProfileNameDetail.setText("User");
                    binding.tvProfileEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "user@example.com");
                    binding.tvProfilePhone.setText("Error loading");
                    binding.tvProfileBirthday.setText("Error loading");
                }
            });
        } else {
            binding.tvProfileNameHeader.setText("User");
            binding.tvProfileNameDetail.setText("User");
            binding.tvProfileEmail.setText("user@example.com");
            binding.tvProfilePhone.setText("N/A");
            binding.tvProfileBirthday.setText("N/A");
            if (currentUser == null) Log.e(TAG, "Cannot load profile data: current user is null.");
            else Log.e(TAG, "Cannot load profile data: userDatabaseReference is null.");
        }

        if (binding.tvProfilePassword != null) {
            binding.tvProfilePassword.setText("************");
        }
    }

    private void navigateTo(Class<?> destinationActivityClass, int destinationItemId, boolean finishCurrent) {
        Intent intent = new Intent(getApplicationContext(), destinationActivityClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        boolean slideRightToLeft = getItemIndex(destinationItemId) > getItemIndex(CURRENT_ITEM_ID);
        if (slideRightToLeft) overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        else overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        if (finishCurrent) finish();
    }

    private int getItemIndex(int itemId) {
        if (itemId == R.id.navigation_home) return 0;
        if (itemId == R.id.navigation_itineraries) return 1;
        if (itemId == R.id.navigation_map) return 2;
        if (itemId == R.id.navigation_profile) return 3;
        return -1;
    }
}
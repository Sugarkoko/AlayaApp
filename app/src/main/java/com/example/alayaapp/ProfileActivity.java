package com.example.alayaapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.alayaapp.databinding.ActivityProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {
    private ActivityProfileBinding binding;
    final int CURRENT_ITEM_ID = R.id.navigation_profile;
    private static final String TAG = "ProfileActivity";

    private FirebaseAuth mAuth;
    private DatabaseReference userDatabaseReference;
    private ValueEventListener userProfileListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tvProfileNameHeader.setText("User");
        binding.tvProfileNameDetail.setText("User");

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userDatabaseReference = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
        }

        binding.bottomNavigationProfilePage.setSelectedItemId(CURRENT_ITEM_ID);
        setupBottomNavListener();
        setupActionListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadProfileData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (userDatabaseReference != null && userProfileListener != null) {
            userDatabaseReference.removeEventListener(userProfileListener);
        }
    }

    private void setupBottomNavListener() {
        binding.bottomNavigationProfilePage.setOnItemSelectedListener(item -> {
            int destinationItemId = item.getItemId();
            if (destinationItemId == CURRENT_ITEM_ID) return true;

            Class<?> destinationActivityClass = null;
            if (destinationItemId == R.id.navigation_home)
                destinationActivityClass = HomeActivity.class;
            else if (destinationItemId == R.id.navigation_itineraries)
                destinationActivityClass = ItinerariesActivity.class;
            else if (destinationItemId == R.id.navigation_map)
                destinationActivityClass = MapsActivity.class;

            if (destinationActivityClass != null) {
                navigateTo(destinationActivityClass, destinationItemId, true);
                return true;
            }
            return false;
        });
    }

    private void setupActionListeners() {
        binding.ivLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Log Out", (dialog, which) -> {
                        if (userDatabaseReference != null && userProfileListener != null) {
                            userDatabaseReference.removeEventListener(userProfileListener);
                        }
                        mAuth.signOut();
                        Toast.makeText(ProfileActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        binding.tvProfileNameDetail.setOnClickListener(v -> showEditTextDialog("name", "Edit Name", binding.tvProfileNameDetail.getText().toString()));
        binding.tvProfileBirthday.setOnClickListener(v -> showBirthdayPickerDialog());
        binding.tvProfilePhone.setOnClickListener(v -> showEditTextDialog("contactNumber", "Edit Contact Number", binding.tvProfilePhone.getText().toString()));

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
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.TransparentDialog);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_text, null);
        builder.setView(dialogView);

        final TextView tvDialogTitle = dialogView.findViewById(R.id.tv_dialog_title);
        final EditText etDialogInput = dialogView.findViewById(R.id.et_dialog_input);
        Button btnSave = dialogView.findViewById(R.id.btn_dialog_save);
        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);

        tvDialogTitle.setText(title);

        if (fieldKey.equals("contactNumber")) {
            etDialogInput.setInputType(InputType.TYPE_CLASS_PHONE);
            etDialogInput.setHint("Enter contact no.");
        } else {
            etDialogInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            etDialogInput.setHint("Enter name");
        }

        if (!currentValue.startsWith("Set ") && !currentValue.equals("N/A") && !currentValue.equals("Not Set") && !currentValue.equals("User")) {
            etDialogInput.setText(currentValue);
            etDialogInput.setSelection(currentValue.length());
        }

        final AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String newValue = etDialogInput.getText().toString().trim();
            if (fieldKey.equals("name") && TextUtils.isEmpty(newValue)) {
                etDialogInput.setError("Name cannot be empty");
                return;
            }
            updateFirebaseField(fieldKey, newValue);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showBirthdayPickerDialog() {
        Calendar calendar = Calendar.getInstance();
        String currentBirthdayText = binding.tvProfileBirthday.getText().toString();
        if (!currentBirthdayText.startsWith("Set ") && !currentBirthdayText.equals("N/A") && !currentBirthdayText.equals("Not Set")) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                calendar.setTime(sdf.parse(currentBirthdayText));
            } catch (Exception e) {
                Log.w(TAG, "Could not parse existing birthday: " + currentBirthdayText, e);
            }
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, R.style.GreenDatePickerDialog,(view, yearSelected, monthOfYear, dayOfMonth) -> {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(yearSelected, monthOfYear, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
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

            if (userProfileListener != null) {
                userDatabaseReference.removeEventListener(userProfileListener);
            }
            userProfileListener = new ValueEventListener() {
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

                        boolean isProfileIncomplete = TextUtils.isEmpty(name) || TextUtils.isEmpty(contactNumber) || TextUtils.isEmpty(birthday);
                        binding.cardCompleteProfilePrompt.setVisibility(isProfileIncomplete ? View.VISIBLE : View.GONE);
                    } else {
                        binding.tvProfileNameHeader.setText("Set your name");
                        binding.tvProfileNameDetail.setText("Set your name");
                        binding.cardCompleteProfilePrompt.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(ProfileActivity.this, "Failed to load profile details.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to load profile data.", databaseError.toException());
                }
            };
            userDatabaseReference.addValueEventListener(userProfileListener);
        } else {
            if (!isFinishing()) {
                Intent intent = new Intent(this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
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
        if (slideRightToLeft)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        else
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
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
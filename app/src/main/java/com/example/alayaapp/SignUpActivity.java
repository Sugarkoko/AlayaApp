package com.example.alayaapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils; // For checking empty name
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button; // For location dialog
import android.widget.EditText; // For name dialog
import android.widget.Toast;

import com.example.alayaapp.databinding.ActivitySignUpBinding;
import com.example.alayaapp.databinding.ActivityWelcomePageBinding;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class SignUpActivity extends AppCompatActivity {
    private ActivitySignUpBinding signUpFormBinding;
    private ActivityWelcomePageBinding welcomeScreenBinding;
    private AlertDialog locationDialog;
    private AlertDialog nameEntryDialog; // Dialog for entering name
    private AlertDialog birthdayPromptDialog;
    private AlertDialog phoneEntryDialog;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference; // Points to "users" node
    private static final String TAG = "SignUpActivity";

    // Request codes for permission and settings
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int REQUEST_CHECK_SETTINGS = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");
        showSignUpForm();
    }


    private void showSignUpForm() {
        signUpFormBinding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(signUpFormBinding.getRoot());

        signUpFormBinding.signupButton.setOnClickListener(v -> {
            String email = signUpFormBinding.emailEditText.getText().toString().trim();
            String password = signUpFormBinding.passwordEditText.getText().toString().trim();
            String confirmPassword = signUpFormBinding.confirmPasswordEditText.getText().toString().trim();


            boolean valid = true;
            if (email.isEmpty()) {
                signUpFormBinding.emailLayout.setError("Email required");
                valid = false;
            } else {
                signUpFormBinding.emailLayout.setError(null);
            }
            if (password.isEmpty()) {
                signUpFormBinding.passwordLayout.setError("Password required");
                valid = false;
            } else {
                signUpFormBinding.passwordLayout.setError(null);
            }
            if (confirmPassword.isEmpty()) {
                signUpFormBinding.confirmPasswordLayout.setError("Confirmation required");
                valid = false;
            } else {
                signUpFormBinding.confirmPasswordLayout.setError(null);
            }
            if (!valid) return;

            if (!password.equals(confirmPassword)) {
                signUpFormBinding.confirmPasswordLayout.setError("Passwords do not match");
                return;
            } else {
                signUpFormBinding.confirmPasswordLayout.setError(null);
            }


            signUpFormBinding.signupButton.setEnabled(false);
            Toast.makeText(SignUpActivity.this, "Creating account...", Toast.LENGTH_SHORT).show();

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        signUpFormBinding.signupButton.setEnabled(true);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                String userId = firebaseUser.getUid();
                                HashMap<String, Object> userInfo = new HashMap<>();
                                userInfo.put("email", email);
                                userInfo.put("name", ""); // Name initially blank
                                userInfo.put("contactNumber", "");
                                userInfo.put("birthday", "");

                                databaseReference.child(userId).setValue(userInfo)
                                        .addOnSuccessListener(aVoid -> Log.d(TAG, "User info (empty name) successfully written for UID: " + userId))
                                        .addOnFailureListener(e -> Log.w(TAG, "Error writing user info to DB for UID: " + userId, e));

                                Toast.makeText(SignUpActivity.this, "Sign Up Successful!", Toast.LENGTH_SHORT).show();
                                showWelcomeScreen(firebaseUser); // Pass user to next step
                            } else {
                                // Should not happen if task is successful, but handle defensively
                                Toast.makeText(SignUpActivity.this, "User creation error. Please try again.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(SignUpActivity.this, "Authentication failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        signUpFormBinding.switchToLoginLayout.setOnClickListener(v -> {
            finish();
        });
    }

    private void showWelcomeScreen(FirebaseUser firebaseUser) {
        welcomeScreenBinding = ActivityWelcomePageBinding.inflate(getLayoutInflater());
        setContentView(welcomeScreenBinding.getRoot());
        welcomeScreenBinding.getStartedButton.setOnClickListener(v_welcome -> {
            showEnterNameDialog(firebaseUser); // Show name dialog first
        });
    }

    private void showEnterNameDialog(FirebaseUser firebaseUser) {
        if (nameEntryDialog != null && nameEntryDialog.isShowing()) {
            return; // Prevent multiple dialogs
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.TransparentDialog);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_enter_name, null);
        builder.setView(dialogView);

        final EditText nameEditText = dialogView.findViewById(R.id.dialog_name_edit_text);
        Button nextButton = dialogView.findViewById(R.id.dialog_next_button);

        nameEntryDialog = builder.create();
        nameEntryDialog.setCancelable(false);

        nextButton.setOnClickListener(v_name_dialog -> {
            String name = nameEditText.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                nameEditText.setError("Name cannot be empty");
                return;
            }
            // Save the name to Firebase
            databaseReference.child(firebaseUser.getUid()).child("name").setValue(name)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(SignUpActivity.this, "Name saved!", Toast.LENGTH_SHORT).show();
                        if (nameEntryDialog != null) nameEntryDialog.dismiss();
                        showEnterBirthdayDialog(firebaseUser); // Proceed to next step
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(SignUpActivity.this,"Failed to save name: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Failed to save name to Firebase", e);
                    });
        });

        nameEntryDialog.show();
    }

    private void showEnterBirthdayDialog(final FirebaseUser firebaseUser) {
        if (birthdayPromptDialog != null && birthdayPromptDialog.isShowing()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.TransparentDialog);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_prompt_birthday, null);
        builder.setView(dialogView);
        birthdayPromptDialog = builder.create();
        birthdayPromptDialog.setCancelable(false);

        Button btnSelectDate = dialogView.findViewById(R.id.btn_select_date);
        Button btnSkip = dialogView.findViewById(R.id.btn_skip_birthday);

        btnSkip.setOnClickListener(v -> {
            birthdayPromptDialog.dismiss();
            showEnterPhoneDialog(firebaseUser);
        });

        btnSelectDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR) - 25; // Default to a reasonable age
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this,R.style.GreenDatePickerDialog,  (view, yearSelected, monthOfYear, dayOfMonth) -> {
                Calendar selectedDate = Calendar.getInstance();
                selectedDate.set(yearSelected, monthOfYear, dayOfMonth);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String formattedDate = sdf.format(selectedDate.getTime());
                databaseReference.child(firebaseUser.getUid()).child("birthday").setValue(formattedDate)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "Birthday saved!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to save birthday", e));

                birthdayPromptDialog.dismiss();
                showEnterPhoneDialog(firebaseUser); // Proceed to next step regardless of save success
            }, year, month, day);

            datePickerDialog.setTitle("Select Your Birthday");
            datePickerDialog.show();
        });

        birthdayPromptDialog.show();
    }


    private void showEnterPhoneDialog(final FirebaseUser firebaseUser) {
        if (phoneEntryDialog != null && phoneEntryDialog.isShowing()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.TransparentDialog);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_enter_phone, null);
        builder.setView(dialogView);
        phoneEntryDialog = builder.create();
        phoneEntryDialog.setCancelable(false);

        final EditText phoneEditText = dialogView.findViewById(R.id.dialog_phone_edit_text);
        Button btnSave = dialogView.findViewById(R.id.btn_save_phone);
        Button btnSkip = dialogView.findViewById(R.id.btn_skip_phone);

        btnSkip.setOnClickListener(v -> {
            phoneEntryDialog.dismiss();
            showLocationPermissionDialog();
        });

        btnSave.setOnClickListener(v -> {
            String phoneNumber = phoneEditText.getText().toString().trim();
            if (!TextUtils.isEmpty(phoneNumber)) {
                databaseReference.child(firebaseUser.getUid()).child("contactNumber").setValue(phoneNumber)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "Phone number saved!", Toast.LENGTH_SHORT).show());
            }
            phoneEntryDialog.dismiss();
            showLocationPermissionDialog(); // Proceed to next step
        });

        phoneEntryDialog.show();
    }

    private void showLocationPermissionDialog() {
        if (locationDialog != null && locationDialog.isShowing()) {
            return; // Prevent multiple dialogs
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.TransparentDialog);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_location_notification, null);
        builder.setView(dialogView);

        Button btnTurnOn = dialogView.findViewById(R.id.btn_turn_on);
        Button btnSkip = dialogView.findViewById(R.id.btn_skip_location); // Get the new skip button
        locationDialog = builder.create();

        if (locationDialog.getWindow() != null) {
            locationDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        locationDialog.setCancelable(false);

        btnTurnOn.setOnClickListener(v_dialog_button -> {
            checkAndRequestLocationPermissions();
        });

        btnSkip.setOnClickListener(v -> navigateToHome());

        locationDialog.show();
    }

    private void checkAndRequestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permission has already been granted, check if location is enabled.
            checkDeviceLocationSettings();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, now check if location services are on.
                checkDeviceLocationSettings();
            } else {
                // Permission was denied. Navigate to home anyway.
                Toast.makeText(this, "Location permission denied. Features will be limited.", Toast.LENGTH_LONG).show();
                navigateToHome();
            }
        }
    }

    private void checkDeviceLocationSettings() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> {
            // All location settings are satisfied. Navigate to home.
            Toast.makeText(this, "Location is on. You're all set!", Toast.LENGTH_SHORT).show();
            navigateToHome();
        });

        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult()
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(SignUpActivity.this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    // Ignore the error.
                    Log.e(TAG, "Error resolving location settings", sendEx);
                    navigateToHome();
                }
            } else {
                // Non-resolvable error.
                navigateToHome();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                // The user agreed to change location settings.
                Toast.makeText(this, "Location services enabled!", Toast.LENGTH_SHORT).show();
            } else {
                // The user did not agree to change location settings.
                Toast.makeText(this, "Location services are required for the best experience.", Toast.LENGTH_LONG).show();
            }
            // In either case, proceed to the home screen.
            navigateToHome();
        }
    }

    private void navigateToHome() {
        if (locationDialog != null && locationDialog.isShowing()) {
            locationDialog.dismiss();
        }
        Intent intent = new Intent(SignUpActivity.this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationDialog != null && locationDialog.isShowing()) {
            locationDialog.dismiss();
        }
        if (nameEntryDialog != null && nameEntryDialog.isShowing()) {
            nameEntryDialog.dismiss();
        }
        if (birthdayPromptDialog != null && birthdayPromptDialog.isShowing()) {
            birthdayPromptDialog.dismiss();
        }
        if (phoneEntryDialog != null && phoneEntryDialog.isShowing()) {
            phoneEntryDialog.dismiss();
        }
    }
}
package com.example.alayaapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils; // For checking empty name
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;   // For location dialog
import android.widget.EditText; // For name dialog
import android.widget.Toast;

import com.example.alayaapp.databinding.ActivitySignUpBinding;
import com.example.alayaapp.databinding.ActivityWelcomePageBinding;
import com.google.android.material.button.MaterialButton; // For name dialog button
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding signUpFormBinding;
    private ActivityWelcomePageBinding welcomeScreenBinding;
    private AlertDialog locationDialog;
    private AlertDialog nameEntryDialog; // Dialog for entering name

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference; // Points to "users" node
    private static final String TAG = "SignUpActivity";

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

            // --- Your existing validation logic ---
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
            // --- End of validation logic ---

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
                            Toast.makeText(SignUpActivity.this, "Authentication failed: " +
                                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                    Toast.LENGTH_LONG).show();
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

        MaterialButton nextButton = dialogView.findViewById(R.id.dialog_next_button);

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
                        showLocationPermissionDialog();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(SignUpActivity.this, "Failed to save name: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Failed to save name to Firebase", e);

                    });
        });

        nameEntryDialog.show();
    }

    private void showLocationPermissionDialog() {
        if (locationDialog != null && locationDialog.isShowing()) {
            return; // Prevent multiple dialogs
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.dialog_location_notification, null);
        builder.setView(dialogView);


        Button btnTurnOn = dialogView.findViewById(R.id.btn_turn_on);
        locationDialog = builder.create();

        if (locationDialog.getWindow() != null) {
            locationDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        locationDialog.setCancelable(false);

        btnTurnOn.setOnClickListener(v_dialog_button -> {
            if (locationDialog != null) locationDialog.dismiss();
            Intent intent = new Intent(SignUpActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
        locationDialog.show();
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
    }
}
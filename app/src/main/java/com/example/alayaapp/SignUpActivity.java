package com.example.alayaapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.alayaapp.databinding.ActivitySignUpBinding;
import com.example.alayaapp.databinding.ActivityWelcomePageBinding;
import com.google.firebase.auth.FirebaseAuth; // Firebase Import
import com.google.firebase.auth.FirebaseUser; // Firebase Import
import com.google.firebase.database.DatabaseReference; // Firebase Import
import com.google.firebase.database.FirebaseDatabase;  // Firebase Import
import java.util.HashMap; // For user data

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding signUpFormBinding;
    private ActivityWelcomePageBinding welcomeScreenBinding; // Assuming you still use this
    private AlertDialog locationDialog;

    private FirebaseAuth mAuth; // Firebase Auth instance
    private DatabaseReference databaseReference; // Firebase Realtime Database reference
    private static final String TAG = "SignUpActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize Firebase Auth and Database
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users"); // "users" node

        showSignUpForm(); // Initial call to display the form
    }

    private void showSignUpForm() {
        signUpFormBinding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(signUpFormBinding.getRoot());

        signUpFormBinding.signupButton.setOnClickListener(v -> {
            String email = signUpFormBinding.emailEditText.getText().toString().trim();
            String password = signUpFormBinding.passwordEditText.getText().toString().trim();
            String confirmPassword = signUpFormBinding.confirmPasswordEditText.getText().toString().trim();
            // If you add a name field in activity_sign_up.xml:
            // String name = signUpFormBinding.nameEditText.getText().toString().trim();


            boolean valid = true;
            if (email.isEmpty()) {
                signUpFormBinding.emailLayout.setError("Email required");
                valid = false;
            } else {
                signUpFormBinding.emailLayout.setError(null);
            }

            if (password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "Password fields cannot be empty", Toast.LENGTH_SHORT).show();
                if (password.isEmpty()) signUpFormBinding.passwordLayout.setError("Password required"); else signUpFormBinding.passwordLayout.setError(null);
                if (confirmPassword.isEmpty()) signUpFormBinding.confirmPasswordLayout.setError("Confirmation required"); else signUpFormBinding.confirmPasswordLayout.setError(null);
                valid = false;
            } else {
                signUpFormBinding.passwordLayout.setError(null);
                signUpFormBinding.confirmPasswordLayout.setError(null);
            }

            if (!valid) return;

            if (!password.equals(confirmPassword)) {
                signUpFormBinding.confirmPasswordLayout.setError("Passwords do not match");
                return;
            } else {
                signUpFormBinding.confirmPasswordLayout.setError(null);
            }

            // --- Firebase Sign Up ---
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
                                // Store additional user information (e.g., email, name if you add it)
                                HashMap<String, Object> userInfo = new HashMap<>();
                                userInfo.put("email", email);
                                // userInfo.put("name", name); // if you added a name field
                                // userInfo.put("createdAt", System.currentTimeMillis()); // Example timestamp

                                databaseReference.child(userId).setValue(userInfo)
                                        .addOnSuccessListener(aVoid -> Log.d(TAG, "User info successfully written to DB for UID: " + userId))
                                        .addOnFailureListener(e -> Log.w(TAG, "Error writing user info to DB for UID: " + userId, e));

                                // Optional: Send email verification
                                // firebaseUser.sendEmailVerification().addOnCompleteListener(...);
                            }

                            Toast.makeText(SignUpActivity.this, "Sign Up Successful!", Toast.LENGTH_SHORT).show();
                            showWelcomeScreen(); // Proceed to welcome screen or location dialog
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(SignUpActivity.this, "Authentication failed: " +
                                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        signUpFormBinding.switchToLoginLayout.setOnClickListener(v -> {
            finish(); // Go back to LoginActivity
        });
    }

    private void showWelcomeScreen() {
        // This part remains the same as your original logic
        welcomeScreenBinding = ActivityWelcomePageBinding.inflate(getLayoutInflater());
        setContentView(welcomeScreenBinding.getRoot());

        welcomeScreenBinding.getStartedButton.setOnClickListener(v_welcome -> {
            showLocationPermissionDialog();
        });
    }

    private void showLocationPermissionDialog() {
        // This part remains the same as your original logic
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_location_notification, null);
        builder.setView(dialogView);

        Button btnTurnOn = dialogView.findViewById(R.id.btn_turn_on);
        locationDialog = builder.create();

        if (locationDialog.getWindow() != null) {
            locationDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnTurnOn.setOnClickListener(v_dialog_button -> {
            locationDialog.dismiss();
            Intent intent = new Intent(SignUpActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
        locationDialog.show();
    }
}
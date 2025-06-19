package com.example.alayaapp;

import androidx.annotation.NonNull; // Import NonNull
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
// import android.text.method.PasswordTransformationMethod; // Already there, keep if using custom toggle
import android.util.Log; // For logging
import android.view.View;
import android.widget.Toast;

import com.example.alayaapp.databinding.ActivityChangePasswordBinding;
import com.google.android.gms.tasks.OnCompleteListener; // For Firebase tasks
import com.google.android.gms.tasks.Task; // For Firebase tasks
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth; // Firebase Auth
import com.google.firebase.auth.FirebaseUser; // Firebase User

public class ChangePasswordActivity extends AppCompatActivity {

    private ActivityChangePasswordBinding binding;
    private FirebaseAuth mAuth; // Firebase Auth instance
    private static final String TAG = "ChangePasswordActivity"; // For logging

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance(); // Initialize Firebase Auth

        // Back arrow listener
        binding.ivBackArrowChangePassword.setOnClickListener(v -> {
            finish(); // Go back to the previous activity
        });

        // Submit button listener
        binding.btnChangePasswordSubmit.setOnClickListener(v -> {
            handleChangePassword();
        });
    }

    private void handleChangePassword() {
        String newPassword = binding.etNewPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        // Basic Validation
        if (newPassword.isEmpty()) {
            binding.tilNewPassword.setError("New password cannot be empty");
            binding.etNewPassword.requestFocus();
            return;
        } else {
            binding.tilNewPassword.setError(null);
        }

        // Firebase requires passwords to be at least 6 characters.
        if (newPassword.length() < 6) {
            binding.tilNewPassword.setError("Password must be at least 6 characters");
            binding.etNewPassword.requestFocus();
            return;
        } else {
            binding.tilNewPassword.setError(null);
        }

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.setError("Please confirm your password");
            binding.etConfirmPassword.requestFocus();
            return;
        } else {
            binding.tilConfirmPassword.setError(null);
        }

        if (!newPassword.equals(confirmPassword)) {
            binding.tilConfirmPassword.setError("Passwords do not match");
            binding.etConfirmPassword.requestFocus();
            binding.tilNewPassword.setError("Passwords do not match");
            return;
        } else {
            binding.tilConfirmPassword.setError(null);
            binding.tilNewPassword.setError(null); // Clear error on new password if they match
        }


        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            binding.btnChangePasswordSubmit.setEnabled(false); // Disable button during operation
            Toast.makeText(this, "Updating password...", Toast.LENGTH_SHORT).show();

            user.updatePassword(newPassword)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            binding.btnChangePasswordSubmit.setEnabled(true); // Re-enable button
                            if (task.isSuccessful()) {
                                Log.d(TAG, "User password updated.");
                                Toast.makeText(ChangePasswordActivity.this, "Password updated successfully!", Toast.LENGTH_LONG).show();

                                finish(); // Close this activity and return to profile
                            } else {
                                Log.e(TAG, "Error updating password", task.getException());
                                String errorMessage = "Failed to update password.";
                                if (task.getException() != null) {

                                    errorMessage += " " + task.getException().getMessage();
                                    if (task.getException().getMessage().contains("CREDENTIAL_TOO_OLD_LOGIN_AGAIN")) {
                                        errorMessage = "For security, please sign out and sign back in before changing your password.";
                                    }
                                }
                                Toast.makeText(ChangePasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                binding.tilNewPassword.setError(errorMessage); // Show error on a field
                            }
                        }
                    });
        } else {

            Toast.makeText(this, "No user signed in. Please sign in again.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Cannot change password: No user is currently signed in.");

        }
    }
}
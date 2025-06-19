package com.example.alayaapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.alayaapp.databinding.ActivityResetPasswordBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
// NEW: Import for checking email existence
import com.google.firebase.auth.SignInMethodQueryResult;

public class ResetPasswordActivity extends AppCompatActivity {
    private ActivityResetPasswordBinding binding;
    private AlertDialog successDialog;
    private FirebaseAuth mAuth;
    private static final String TAG = "ResetPasswordActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        binding.ivBackArrow.setOnClickListener(v -> {
            finish();
        });

        binding.resetButton.setOnClickListener(v -> {
            handlePasswordReset();
        });
    }


    private void handlePasswordReset() {
        String email = binding.emailEditText.getText().toString().trim();

        if (email.isEmpty()) {
            binding.emailLayout.setError("Email required");
            return;
        } else {
            binding.emailLayout.setError(null); // Clear previous errors
        }

        // Disable button and show progress
        binding.resetButton.setEnabled(false);
        Toast.makeText(this, "Checking email...", Toast.LENGTH_SHORT).show();

        // Check if the email exists in Firebase Auth
        mAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        SignInMethodQueryResult result = task.getResult();
                        boolean emailExists = result.getSignInMethods() != null && !result.getSignInMethods().isEmpty();

                        if (!emailExists) {
                            // Email does not exist, notify the user
                            binding.resetButton.setEnabled(true); // Re-enable button
                            binding.emailLayout.setError("Email address not found"); // Show error on the input field
                            Toast.makeText(ResetPasswordActivity.this, "This email is not registered with an account.", Toast.LENGTH_LONG).show();
                        } else {
                            // Email exists, proceed to send the reset email
                            Toast.makeText(this, "Sending reset email...", Toast.LENGTH_SHORT).show();
                            mAuth.sendPasswordResetEmail(email)
                                    .addOnCompleteListener(sendEmailTask -> {
                                        // Re-enable the button regardless of outcome
                                        binding.resetButton.setEnabled(true);
                                        if (sendEmailTask.isSuccessful()) {
                                            Log.d(TAG, "Password reset email sent successfully.");
                                            showPasswordResetSuccessDialog();
                                        } else {
                                            Log.w(TAG, "sendPasswordResetEmail:failure", sendEmailTask.getException());
                                            Toast.makeText(ResetPasswordActivity.this, "Failed to send reset email. Please try again.", Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }
                    } else {
                        // The task to check the email failed (e.g., no network)
                        binding.resetButton.setEnabled(true); // Re-enable button
                        Log.w(TAG, "fetchSignInMethodsForEmail:failure", task.getException());
                        Toast.makeText(ResetPasswordActivity.this, "Failed to check email. Please check your connection and try again.", Toast.LENGTH_LONG).show();
                    }
                });
    }


    private void showPasswordResetSuccessDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_success_password_change, null);
        builder.setView(dialogView);
        Button btnContinue = dialogView.findViewById(R.id.btn_dialog_continue);
        successDialog = builder.create();

        if (successDialog.getWindow() != null) {
            successDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnContinue.setOnClickListener(v_dialog_button -> {
            successDialog.dismiss();
            // Navigate back to the Login screen
            Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        successDialog.show();
    }
}
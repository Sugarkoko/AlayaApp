package com.example.alayaapp;

import androidx.appcompat.app.AlertDialog; // Import AlertDialog
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater; // Import LayoutInflater
import android.view.View; // Import View
import android.widget.Button; // Import Button
import android.widget.Toast;

import com.example.alayaapp.databinding.ActivitySignUpBinding;
import com.example.alayaapp.databinding.ActivityWelcomePageBinding;
// No need to import binding for the dialog layout if we inflate it manually for AlertDialog

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding signUpFormBinding;
    private ActivityWelcomePageBinding welcomeScreenBinding;
    private AlertDialog locationDialog; // To keep a reference if needed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

            boolean signUpSuccess = true; // Placeholder

            if (signUpSuccess) {
                showWelcomeScreen();
            } else {
                Toast.makeText(SignUpActivity.this, "Sign Up Failed (Placeholder - Check Logs/API)", Toast.LENGTH_SHORT).show();
            }
        });

        signUpFormBinding.switchToLoginLayout.setOnClickListener(v -> {
            finish();
        });
    }

    private void showWelcomeScreen() {
        welcomeScreenBinding = ActivityWelcomePageBinding.inflate(getLayoutInflater());
        setContentView(welcomeScreenBinding.getRoot());

        welcomeScreenBinding.getStartedButton.setOnClickListener(v_welcome -> {
            // Instead of going directly to HomeActivity, show the location permission dialog
            showLocationPermissionDialog();
        });
    }

    private void showLocationPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_location_notification, null);
        builder.setView(dialogView);

        // Prevent dialog dismissal on outside touch or back press if desired
        // builder.setCancelable(false);

        Button btnTurnOn = dialogView.findViewById(R.id.btn_turn_on);

        // Create the dialog
        locationDialog = builder.create();

        // Ensure the dialog has a transparent background if your dialog_background_gradient_rounded has rounded corners
        if (locationDialog.getWindow() != null) {
            locationDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }


        btnTurnOn.setOnClickListener(v_dialog_button -> {
            // Here, you would typically add logic to:
            // 1. Request location permissions.
            // 2. Or open location settings:
            //    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            //    startActivity(intent);
            // For now, we'll just dismiss the dialog and proceed to HomeActivity.

            locationDialog.dismiss(); // Dismiss the dialog

            // Navigate to HomeActivity
            Intent intent = new Intent(SignUpActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); // Close SignUpActivity
        });

        locationDialog.show();
    }
}
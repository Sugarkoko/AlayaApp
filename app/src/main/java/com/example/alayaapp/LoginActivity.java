package com.example.alayaapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
// Removed unused imports like LinearLayout, TextView, TextInputLayout, TextInputEditText
import com.example.alayaapp.databinding.ActivityLoginBinding; // Import ViewBinding class

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding; // Declare binding variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate the layout using ViewBinding
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        // Set the content view from the binding's root
        setContentView(binding.getRoot());

        // Access views using binding
        binding.loginButton.setOnClickListener(v -> {
            // Get text using binding. No need for null checks on getText() before toString()
            String email = binding.emailEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();

            // Basic validation example
            boolean valid = true;
            if (email.isEmpty()) {
                binding.emailLayout.setError("Email required"); // Use binding to set error
                valid = false;
            } else {
                binding.emailLayout.setError(null); // Clear error
            }

            if (password.isEmpty()) {
                binding.passwordLayout.setError("Password required"); // Use binding to set error
                valid = false;
            } else {
                binding.passwordLayout.setError(null); // Clear error
            }

            if (!valid) {
                return; // Stop if validation fails
            }

            // --- Replace with your actual authentication ---
            // Example: Call your authentication service/API here
            boolean loginSuccess = true; // Placeholder - Set this based on auth result
            // --- ---

            if (loginSuccess) {
                // Optional Toast
                // Toast.makeText(LoginActivity.this, "Login Successful (Placeholder)", Toast.LENGTH_SHORT).show();

                // *** NAVIGATE TO OTP VERIFICATION INSTEAD OF HOME ***
                Intent intent = new Intent(LoginActivity.this, OtpVerificationActivity.class);
                // Optional: Pass user email/ID if needed for verification display/logic
                // intent.putExtra("USER_EMAIL", email);
                startActivity(intent);
                finish(); // Close LoginActivity so back press on OTP doesn't return here
            } else {
                // TODO: Provide more specific error message based on auth failure
                Toast.makeText(LoginActivity.this, "Login Failed (Placeholder)", Toast.LENGTH_SHORT).show();
                binding.passwordLayout.setError("Incorrect email or password"); // Example error
            }
        });

        // Navigate to Sign Up using binding
        binding.switchToSignupLayout.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
            // Don't finish LoginActivity here, user might want to come back from Signup
        });

        // Alternative using just the link text (binding.linkSignUpText)
        /*
        binding.linkSignUpText.setOnClickListener(v -> {
             Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
             startActivity(intent);
        });
        */
    }
}
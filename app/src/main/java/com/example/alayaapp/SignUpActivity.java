package com.example.alayaapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
// Removed unused imports
import com.example.alayaapp.databinding.ActivitySignUpBinding; // Import ViewBinding class

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding; // Declare binding variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate the layout using ViewBinding
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        // Set the content view from the binding's root
        setContentView(binding.getRoot());

        // Access views using binding
        binding.signupButton.setOnClickListener(v -> {
            // Get text using binding
            String email = binding.emailEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();
            String confirmPassword = binding.confirmPasswordEditText.getText().toString().trim();

            // Basic validation example
            boolean valid = true;
            if (email.isEmpty()) {
                binding.emailLayout.setError("Email required");
                valid = false;
            } else {
                binding.emailLayout.setError(null);
            }
            // Add validation for password fields as well if desired

            if (password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "Password fields cannot be empty", Toast.LENGTH_SHORT).show();
                // Optionally set errors on password fields
                if (password.isEmpty()) binding.passwordLayout.setError("Password required"); else binding.passwordLayout.setError(null);
                if (confirmPassword.isEmpty()) binding.confirmPasswordLayout.setError("Confirmation required"); else binding.confirmPasswordLayout.setError(null);
                valid = false;
            } else {
                binding.passwordLayout.setError(null);
                binding.confirmPasswordLayout.setError(null);
            }


            if (!valid) return; // Exit if initial checks fail

            if (!password.equals(confirmPassword)) {
                binding.confirmPasswordLayout.setError("Passwords do not match");
                // Optionally clear error on the first password field if it had one
                // binding.passwordLayout.setError(null);
                // Toast.makeText(SignUpActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show(); // Redundant with setError
                return; // Stop if passwords don't match
            } else {
                binding.confirmPasswordLayout.setError(null); // Clear error if they match now
            }

            // --- Replace with your actual registration logic ---
            boolean signUpSuccess = true; // Placeholder
            // --- ---

            if (signUpSuccess) {
                // Toast.makeText(SignUpActivity.this, "Sign Up Successful (Placeholder)", Toast.LENGTH_SHORT).show();

                // Decide flow: Go to Home or back to Login? Going to Home is common after signup.
                Intent intent = new Intent(SignUpActivity.this, HomeActivity.class);
                // Clear back stack
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish(); // Close SignUpActivity
            } else {
                Toast.makeText(SignUpActivity.this, "Sign Up Failed (Placeholder - Check Logs/API)", Toast.LENGTH_SHORT).show();
                // Provide more specific feedback if possible, e.g., email already exists
            }
        });

        // Navigate back to Login using binding
        binding.switchToLoginLayout.setOnClickListener(v -> {
            // Simply finish this activity to go back to LoginActivity in the stack
            finish();
        });

        // Alternative using link text
        /*
        binding.linkSignInText.setOnClickListener(v -> {
            finish();
        });
        */
    }
}
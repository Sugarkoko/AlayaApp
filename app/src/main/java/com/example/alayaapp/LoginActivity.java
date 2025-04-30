package com.example.alayaapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout; // Or just the TextView link
// import android.widget.TextView; // Only needed for alternative link click
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    TextInputLayout emailLayout;
    TextInputEditText emailEditText;
    TextInputLayout passwordLayout;
    TextInputEditText passwordEditText;
    Button loginButton;
    LinearLayout switchToSignupLayout;
    // TextView linkSignUpText; // Alternative if clicking only the link

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailLayout = findViewById(R.id.email_layout);
        emailEditText = findViewById(R.id.email_edit_text);
        passwordLayout = findViewById(R.id.password_layout);
        passwordEditText = findViewById(R.id.password_edit_text);
        loginButton = findViewById(R.id.login_button);
        switchToSignupLayout = findViewById(R.id.switch_to_signup_layout);
        // linkSignUpText = findViewById(R.id.link_sign_up_text);

        loginButton.setOnClickListener(v -> {
            // TODO: Implement actual login logic
            String email = ""; // Initialize to avoid potential NPE warning on getText()
            if (emailEditText.getText() != null) {
                email = emailEditText.getText().toString().trim();
            }
            String password = "";
            if (passwordEditText.getText() != null) {
                password = passwordEditText.getText().toString().trim();
            }


            // Basic validation example
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Email and password cannot be empty", Toast.LENGTH_SHORT).show();
                // You might want to set errors on the TextInputLayouts too
                // emailLayout.setError("Email required");
                // passwordLayout.setError("Password required");
                return;
            } else {
                // Clear potential previous errors
                // emailLayout.setError(null);
                // passwordLayout.setError(null);
            }

            // --- Replace with your actual authentication ---
            // Example: Call your authentication service/API here
            boolean loginSuccess = true; // Placeholder - Set this based on auth result
            // --- ---

            if (loginSuccess) {
                // Toast.makeText(LoginActivity.this, "Login Successful (Placeholder)", Toast.LENGTH_SHORT).show(); // Optional Toast

                // *** NAVIGATE TO OTP VERIFICATION INSTEAD OF HOME ***
                Intent intent = new Intent(LoginActivity.this, OtpVerificationActivity.class);

                // Optional: Pass user email/ID if needed for verification display/logic
                // intent.putExtra("USER_EMAIL", email);

                // We don't clear the task here yet. OtpVerificationActivity will clear it before going to Home.
                // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // REMOVED from here

                startActivity(intent);
                finish(); // Close LoginActivity so back press on OTP doesn't return here

            } else {
                // TODO: Provide more specific error message based on auth failure
                Toast.makeText(LoginActivity.this, "Login Failed (Placeholder)", Toast.LENGTH_SHORT).show();
                // passwordLayout.setError("Incorrect email or password"); // Example error
            }
        });

        // Navigate to Sign Up
        switchToSignupLayout.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
            // Don't finish LoginActivity here, user might want to come back from Signup
        });
         /* // Alternative: Clicking only the link text
         linkSignUpText.setOnClickListener(v -> {
             Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
             startActivity(intent);
         });
         */
    }
}
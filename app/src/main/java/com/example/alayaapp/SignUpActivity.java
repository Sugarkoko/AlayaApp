package com.example.alayaapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout; // Or just TextView
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class SignUpActivity extends AppCompatActivity {

    TextInputLayout emailLayout, passwordLayout, confirmPasswordLayout;
    TextInputEditText emailEditText, passwordEditText, confirmPasswordEditText;
    Button signUpButton;
    LinearLayout switchToLoginLayout; // Or TextView link_sign_in_text
    // TextView linkSignInText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        emailLayout = findViewById(R.id.email_layout);
        emailEditText = findViewById(R.id.email_edit_text);
        passwordLayout = findViewById(R.id.password_layout);
        passwordEditText = findViewById(R.id.password_edit_text);
        confirmPasswordLayout = findViewById(R.id.confirm_password_layout);
        confirmPasswordEditText = findViewById(R.id.confirm_password_edit_text);
        signUpButton = findViewById(R.id.signup_button);
        switchToLoginLayout = findViewById(R.id.switch_to_login_layout);
        // linkSignInText = findViewById(R.id.link_sign_in_text);

        signUpButton.setOnClickListener(v -> {
            // TODO: Implement actual sign up logic
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();

            // Basic validation example
            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirmPassword)) {
                confirmPasswordLayout.setError("Passwords do not match"); // Show error on the layout
                // passwordLayout.setError(null); // Clear potential previous error
                Toast.makeText(SignUpActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            } else {
                confirmPasswordLayout.setError(null); // Clear error if they match now
            }

            // --- Replace with your actual registration logic ---
            boolean signUpSuccess = true; // Placeholder
            // --- ---

            if(signUpSuccess) {
                Toast.makeText(SignUpActivity.this, "Sign Up Successful (Placeholder)", Toast.LENGTH_SHORT).show();
                // Decide flow: Go to Home or back to Login? Going to Home is common.
                Intent intent = new Intent(SignUpActivity.this, HomeActivity.class);
                // Clear back stack
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish(); // Close SignUpActivity
            } else {
                Toast.makeText(SignUpActivity.this, "Sign Up Failed (Placeholder)", Toast.LENGTH_SHORT).show();
            }

        });

        // Navigate back to Login
        switchToLoginLayout.setOnClickListener(v -> {
            // Simply finish this activity to go back to LoginActivity in the stack
            finish();
        });
         /* // Alternative: Clicking only the link text
         linkSignInText.setOnClickListener(v -> {
             finish();
         });
         */
    }
}
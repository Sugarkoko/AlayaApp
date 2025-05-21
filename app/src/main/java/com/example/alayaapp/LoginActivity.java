package com.example.alayaapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.example.alayaapp.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth; // Firebase Import
import com.google.firebase.auth.FirebaseUser; // Firebase Import

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth; // Firebase Auth instance
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        binding.loginButton.setOnClickListener(v -> {
            String email = binding.emailEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();

            boolean valid = true;
            if (email.isEmpty()) {
                binding.emailLayout.setError("Email required");
                valid = false;
            } else {
                binding.emailLayout.setError(null);
            }

            if (password.isEmpty()) {
                binding.passwordLayout.setError("Password required");
                valid = false;
            } else {
                binding.passwordLayout.setError(null);
            }

            if (!valid) {
                return;
            }

            // --- Firebase Sign In ---
            binding.loginButton.setEnabled(false);
            Toast.makeText(LoginActivity.this, "Logging in...", Toast.LENGTH_SHORT).show();

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        binding.loginButton.setEnabled(true);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();

                            // Navigate to OtpVerificationActivity (as per your original flow)
                            // You might want to check user.isEmailVerified() here in a real app
                            // and if verified, go directly to HomeActivity.
                            Intent intent = new Intent(LoginActivity.this, OtpVerificationActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed: " +
                                            (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                    Toast.LENGTH_LONG).show();
                            binding.passwordLayout.setError("Incorrect email or password");
                        }
                    });
        });

        binding.switchToSignupLayout.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        binding.tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ResetPasswordActivity.class);
            startActivity(intent);
        });
    }
}
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
                            // FirebaseUser user = mAuth.getCurrentUser(); // You can get user if needed
                            Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();

                            // Navigate directly to HomeActivity
                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
                            startActivity(intent);
                            finish(); // Close LoginActivity
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
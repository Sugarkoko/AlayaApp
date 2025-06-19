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
    private FirebaseAuth mAuth;
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


            binding.loginButton.setEnabled(false);
            Toast.makeText(LoginActivity.this, "Logging in...", Toast.LENGTH_SHORT).show();

            // Inside the binding.loginButton.setOnClickListener in LoginActivity.java

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        binding.loginButton.setEnabled(true);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);

                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

                            finish();
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
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
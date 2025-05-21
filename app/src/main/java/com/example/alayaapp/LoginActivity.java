package com.example.alayaapp;


import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import com.example.alayaapp.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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

            boolean loginSuccess = true; // Placeholder
            if (loginSuccess) {
                Intent intent = new Intent(LoginActivity.this, OtpVerificationActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(LoginActivity.this, "Login Failed (Placeholder)", Toast.LENGTH_SHORT).show();
                binding.passwordLayout.setError("Incorrect email or password");
            }
        });

        binding.switchToSignupLayout.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        // *** ADDED: Forgot Password Click Listener ***
        binding.tvForgotPassword.setOnClickListener(v -> { // Assumes your TextView ID is tv_forgot_password
            Intent intent = new Intent(LoginActivity.this, ResetPasswordActivity.class);
            startActivity(intent);
            // Optionally, you might not want to finish LoginActivity here,
            // so user can come back if they change their mind.
        });
    }
}
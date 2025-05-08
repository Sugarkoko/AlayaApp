package com.example.alayaapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.alayaapp.databinding.ActivityResetPasswordBinding;

public class ResetPasswordActivity extends AppCompatActivity {

    private ActivityResetPasswordBinding binding;
    private AlertDialog successDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.ivBackArrow.setOnClickListener(v -> {
            finish();
        });

        binding.resetButton.setOnClickListener(v -> {
            String email = binding.emailEditText.getText().toString().trim();
            String newPassword = binding.passwordEditText.getText().toString().trim();
            String confirmNewPassword = binding.confirmPasswordEditText.getText().toString().trim();

            boolean valid = true;
            if (email.isEmpty()) {
                binding.emailLayout.setError("Email required");
                valid = false;
            } else {
                binding.emailLayout.setError(null);
            }

            if (newPassword.isEmpty()) {
                binding.passwordLayout.setError("New password required");
                valid = false;
            } else {
                binding.passwordLayout.setError(null);
            }

            if (confirmNewPassword.isEmpty()) {
                binding.confirmPasswordLayout.setError("Confirm new password");
                valid = false;
            } else {
                binding.confirmPasswordLayout.setError(null);
            }

            if (!valid) {
                return;
            }

            if (!newPassword.equals(confirmNewPassword)) {
                binding.confirmPasswordLayout.setError("Passwords do not match");
                binding.passwordLayout.setError("Passwords do not match");
                return;
            } else {
                binding.confirmPasswordLayout.setError(null);
                binding.passwordLayout.setError(null);
            }

            boolean resetSuccess = true; // Placeholder for actual reset logic

            if (resetSuccess) {
                showPasswordResetSuccessDialog();
            } else {
                Toast.makeText(ResetPasswordActivity.this, "Password reset failed (Placeholder)", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showPasswordResetSuccessDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        // **** UPDATED XML FILENAME FOR INFLATION ****
        View dialogView = inflater.inflate(R.layout.dialog_success_password_change, null);
        builder.setView(dialogView);
        // builder.setCancelable(false);

        Button btnContinue = dialogView.findViewById(R.id.btn_dialog_continue);

        successDialog = builder.create();

        if (successDialog.getWindow() != null) {
            successDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnContinue.setOnClickListener(v_dialog_button -> {
            successDialog.dismiss();

            Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        successDialog.show();
    }
}
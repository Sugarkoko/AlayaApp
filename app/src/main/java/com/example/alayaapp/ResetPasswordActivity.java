package com.example.alayaapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.alayaapp.databinding.ActivityResetPasswordBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {

    private ActivityResetPasswordBinding binding;
    private AlertDialog successDialog;
    private FirebaseAuth mAuth; // Firebase Auth instance
    private static final String TAG = "ResetPasswordActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        binding.ivBackArrow.setOnClickListener(v -> {
            finish();
        });

        binding.resetButton.setOnClickListener(v -> {
            handlePasswordReset();
        });
    }

    private void handlePasswordReset() {
        String email = binding.emailEditText.getText().toString().trim();

        if (email.isEmpty()) {
            binding.emailLayout.setError("Email required");
            return;
        } else {
            binding.emailLayout.setError(null);
        }

        // Disable button and show progress
        binding.resetButton.setEnabled(false);
        Toast.makeText(this, "Sending reset email...", Toast.LENGTH_SHORT).show();

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // Re-enable the button regardless of outcome
                        binding.resetButton.setEnabled(true);

                        if (task.isSuccessful()) {
                            Log.d(TAG, "Password reset email sent successfully.");
                            showPasswordResetSuccessDialog();
                        } else {
                            Log.w(TAG, "sendPasswordResetEmail:failure", task.getException());
                            Toast.makeText(ResetPasswordActivity.this, "Failed to send reset email. Please check the address and try again.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void showPasswordResetSuccessDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_success_password_change, null);
        builder.setView(dialogView);

        Button btnContinue = dialogView.findViewById(R.id.btn_dialog_continue);
        successDialog = builder.create();

        if (successDialog.getWindow() != null) {
            successDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnContinue.setOnClickListener(v_dialog_button -> {
            successDialog.dismiss();
            // Navigate back to the Login screen
            Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        successDialog.show();
    }
}
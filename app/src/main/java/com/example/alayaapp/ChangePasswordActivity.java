package com.example.alayaapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.method.PasswordTransformationMethod; // For password visibility
import android.view.View;
import android.widget.Toast;

import com.example.alayaapp.databinding.ActivityChangePasswordBinding;
import com.google.android.material.textfield.TextInputLayout;

public class ChangePasswordActivity extends AppCompatActivity {

    private ActivityChangePasswordBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Back arrow listener
        binding.ivBackArrowChangePassword.setOnClickListener(v -> {
            finish(); // Go back to the previous activity
        });

        // Submit button listener
        binding.btnChangePasswordSubmit.setOnClickListener(v -> {
            handleChangePassword();
        });

        // Optional: Setup password visibility toggle manually if needed for custom icons
        // setupPasswordToggleListener(binding.tilNewPassword);
        // setupPasswordToggleListener(binding.tilConfirmPassword);
    }

    private void handleChangePassword() {
        String newPassword = binding.etNewPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        // Basic Validation
        if (newPassword.isEmpty()) {
            binding.tilNewPassword.setError("New password cannot be empty");
            return;
        } else {
            binding.tilNewPassword.setError(null); // Clear error
        }

        if (newPassword.length() < 6) { // Example: minimum length
            binding.tilNewPassword.setError("Password must be at least 6 characters");
            return;
        } else {
            binding.tilNewPassword.setError(null);
        }

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.setError("Please confirm your password");
            return;
        } else {
            binding.tilConfirmPassword.setError(null);
        }

        if (!newPassword.equals(confirmPassword)) {
            binding.tilConfirmPassword.setError("Passwords do not match");
            return;
        } else {
            binding.tilConfirmPassword.setError(null);
        }

        // --- TODO: Implement actual password change logic here ---
        // This would typically involve:
        // 1. Getting the current user's ID or token.
        // 2. Making an API call to your backend server to update the password.
        // 3. Handling success and error responses from the server.

        Toast.makeText(this, "Password change request sent (Placeholder)", Toast.LENGTH_SHORT).show();
        // Example: On success, you might navigate back or to a success screen
        // finish();
    }

    // Optional: If you want to use custom drawables for password toggle and handle state
    // private void setupPasswordToggleListener(final TextInputLayout textInputLayout) {
    //    textInputLayout.setEndIconOnClickListener(v -> {
    //        if (textInputLayout.getEditText().getTransformationMethod() == null) {
    //            // If password visible, hide it
    //            textInputLayout.getEditText().setTransformationMethod(PasswordTransformationMethod.getInstance());
    //            textInputLayout.setEndIconDrawable(R.drawable.ic_visibility_on); // Set to "eye open"
    //        } else {
    //            // If password hidden, show it
    //            textInputLayout.getEditText().setTransformationMethod(null);
    //            textInputLayout.setEndIconDrawable(R.drawable.ic_visibility_off); // Set to "eye crossed"
    //        }
    //        // Move cursor to the end
    //        textInputLayout.getEditText().setSelection(textInputLayout.getEditText().getText().length());
    //    });
    // }
}
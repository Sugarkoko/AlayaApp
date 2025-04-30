package com.example.alayaapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
// Removed unused imports like EditText, TextView, Button
import android.widget.Toast;

import com.example.alayaapp.databinding.ActivityOtpVerificationBinding; // Import Activity Binding
import com.example.alayaapp.databinding.DialogSuccessBinding; // Import Dialog Binding

public class OtpVerificationActivity extends AppCompatActivity {

    private ActivityOtpVerificationBinding binding; // Declare activity binding variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate the layout using ViewBinding
        binding = ActivityOtpVerificationBinding.inflate(getLayoutInflater());
        // Set the content view from the binding's root
        setContentView(binding.getRoot());

        // --- Basic Focus Handling (Improve later if needed) ---
        // TODO: Add TextWatchers for more robust OTP input handling (auto-focus next, combine OTP)

        binding.btnOtpSubmit.setOnClickListener(v -> {
            // Get OTP using binding
            String otp = binding.etOtp1.getText().toString() +
                    binding.etOtp2.getText().toString() +
                    binding.etOtp3.getText().toString() +
                    binding.etOtp4.getText().toString();

            // TODO: Add actual OTP validation logic here
            if (otp.length() != 4) {
                Toast.makeText(this, "Please enter all 4 digits", Toast.LENGTH_SHORT).show();
                return;
            }

            // Placeholder: Assume OTP is correct for now
            boolean isOtpCorrect = true; // Replace with real validation (e.g., "1234")
            // boolean isOtpCorrect = otp.equals("1234"); // Example validation

            if (isOtpCorrect) {
                showSuccessDialog();
            } else {
                Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show();
                // Optionally clear OTP fields or set error states
            }
        });

        binding.btnOtpCancel.setOnClickListener(v -> {
            // Go back to the previous screen (likely Login)
            finish();
        });

        binding.tvResendOtp.setOnClickListener(v -> {
            // TODO: Add logic to actually resend the OTP
            Toast.makeText(this, "Resend OTP (Placeholder)", Toast.LENGTH_SHORT).show();
            // Optionally disable the resend button for a cooldown period
        });
    }

    private void showSuccessDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();

        // Inflate the dialog layout using its specific ViewBinding class
        DialogSuccessBinding dialogBinding = DialogSuccessBinding.inflate(inflater);

        // Set the inflated view (the root of the binding) to the dialog builder
        builder.setView(dialogBinding.getRoot());

        // Make dialog non-cancelable by touching outside or back press (optional)
        // builder.setCancelable(false);

        final AlertDialog dialog = builder.create();

        // Make the dialog background transparent (needs to be done after create())
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Access the button inside the dialog via the dialog's binding object
        dialogBinding.btnDialogContinue.setOnClickListener(v -> {
            dialog.dismiss();
            // Navigate to Home Activity and clear the back stack
            Intent intent = new Intent(OtpVerificationActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); // Finish OTP activity
        });

        dialog.show();
    }
}
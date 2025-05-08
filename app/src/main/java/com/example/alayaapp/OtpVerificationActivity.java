package com.example.alayaapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.Toast;

import com.example.alayaapp.databinding.ActivityOtpVerificationBinding;
import com.example.alayaapp.databinding.DialogSuccessBinding; // For your existing success dialog
import com.example.alayaapp.databinding.DialogNewOtpBinding; // *** IMPORT FOR THE NEW OTP RESENT DIALOG ***

public class OtpVerificationActivity extends AppCompatActivity {

    private ActivityOtpVerificationBinding binding;
    private AlertDialog otpResendDialogInstance; // To keep a reference to the resend dialog

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtpVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Request focus on the first OTP box on activity start
        binding.etOtp1.requestFocus();

        // TODO: Consider adding TextWatchers for auto-focusing next OTP box
        // and handling backspace for a smoother UX.

        binding.btnOtpSubmit.setOnClickListener(v -> {
            String otp = binding.etOtp1.getText().toString() +
                    binding.etOtp2.getText().toString() +
                    binding.etOtp3.getText().toString() +
                    binding.etOtp4.getText().toString();

            if (otp.length() != 4) {
                Toast.makeText(this, "Please enter all 4 digits", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean isOtpCorrect = true; // Replace with real validation (e.g., "1234".equals(otp))

            if (isOtpCorrect) {
                showOtpSubmissionSuccessDialog(); // Renamed for clarity
            } else {
                Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show();
                // Optionally clear OTP fields
                clearOtpFields();
                binding.etOtp1.requestFocus();
            }
        });

        binding.btnOtpCancel.setOnClickListener(v -> {
            finish();
        });

        binding.tvResendOtp.setOnClickListener(v -> {
            // --- Replace with your actual logic to resend the OTP via API ---
            boolean resendApiCallSuccess = true; // Placeholder for API call result
            // --- ---

            if (resendApiCallSuccess) {
                showOtpResentConfirmationDialog();
            } else {
                Toast.makeText(this, "Failed to resend OTP. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // This dialog is for when the OTP is successfully SUBMITTED
    private void showOtpSubmissionSuccessDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        DialogSuccessBinding dialogSuccessBinding = DialogSuccessBinding.inflate(inflater);
        builder.setView(dialogSuccessBinding.getRoot());

        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialogSuccessBinding.btnDialogContinue.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(OtpVerificationActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
        dialog.show();
    }

    // *** NEW METHOD: This dialog is for confirming OTP has been RESENT ***
    private void showOtpResentConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();

        // Inflate the dialog layout using its specific ViewBinding class
        DialogNewOtpBinding dialogNewOtpBinding = DialogNewOtpBinding.inflate(inflater);
        builder.setView(dialogNewOtpBinding.getRoot());
        builder.setCancelable(false); // User must click "Continue"

        // Store the dialog instance so we can dismiss it in onDestroy
        otpResendDialogInstance = builder.create();

        if (otpResendDialogInstance.getWindow() != null) {
            otpResendDialogInstance.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialogNewOtpBinding.btnDialogContinue.setOnClickListener(v -> {
            if (otpResendDialogInstance != null) {
                otpResendDialogInstance.dismiss();
            }
            refreshOtpScreen(); // Refresh the main OTP screen
        });

        otpResendDialogInstance.show();
    }

    private void clearOtpFields() {
        binding.etOtp1.setText("");
        binding.etOtp2.setText("");
        binding.etOtp3.setText("");
        binding.etOtp4.setText("");
    }

    private void refreshOtpScreen() {
        clearOtpFields();
        binding.etOtp1.requestFocus(); // Focus on the first OTP box
        Toast.makeText(this, "Please enter the new OTP", Toast.LENGTH_SHORT).show();
        // If you have a countdown timer for OTP, you might want to reset it here.
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Dismiss the dialog if it's showing to prevent window leaks
        if (otpResendDialogInstance != null && otpResendDialogInstance.isShowing()) {
            otpResendDialogInstance.dismiss();
        }
        // You might also want to dismiss the showOtpSubmissionSuccessDialog if you store its instance
    }
}
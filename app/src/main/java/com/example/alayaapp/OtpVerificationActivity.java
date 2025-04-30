package com.example.alayaapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast; // For Resend placeholder

public class OtpVerificationActivity extends AppCompatActivity {

    EditText etOtp1, etOtp2, etOtp3, etOtp4;
    TextView tvResendOtp;
    Button btnOtpSubmit, btnOtpCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        etOtp1 = findViewById(R.id.et_otp_1);
        etOtp2 = findViewById(R.id.et_otp_2);
        etOtp3 = findViewById(R.id.et_otp_3);
        etOtp4 = findViewById(R.id.et_otp_4);
        tvResendOtp = findViewById(R.id.tv_resend_otp);
        btnOtpSubmit = findViewById(R.id.btn_otp_submit);
        btnOtpCancel = findViewById(R.id.btn_otp_cancel);

        // --- Basic Focus Handling (Improve later if needed) ---
        // TODO: Add TextWatchers for more robust OTP input handling (auto-focus next)

        btnOtpSubmit.setOnClickListener(v -> {
            // TODO: Add actual OTP validation logic here
            String otp = etOtp1.getText().toString() +
                    etOtp2.getText().toString() +
                    etOtp3.getText().toString() +
                    etOtp4.getText().toString();

            // Placeholder: Assume OTP is correct for now
            boolean isOtpCorrect = true; // Replace with real validation

            if (isOtpCorrect) {
                showSuccessDialog();
            } else {
                Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show();
            }
        });

        btnOtpCancel.setOnClickListener(v -> {
            // Go back to the previous screen (likely Login)
            finish();
        });

        tvResendOtp.setOnClickListener(v -> {
            // TODO: Add logic to actually resend the OTP
            Toast.makeText(this, "Resend OTP (Placeholder)", Toast.LENGTH_SHORT).show();
        });
    }

    private void showSuccessDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_success, null);
        builder.setView(dialogView);
        // Make dialog non-cancelable by touching outside or back press (optional)
        // builder.setCancelable(false);

        Button btnContinue = dialogView.findViewById(R.id.btn_dialog_continue);

        AlertDialog dialog = builder.create();

        // Make the dialog background transparent
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnContinue.setOnClickListener(v -> {
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